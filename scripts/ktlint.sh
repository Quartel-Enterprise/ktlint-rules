#!/bin/bash

# Runs ktlint with the Quare ruleset over the git repository the caller is in.
#
#   ktlint.sh <ruleset-version> [--format]
#   ktlint.sh <ruleset-version> --print-versions   prints "<ruleset-version> <ktlint-version>" and exits
#
# Consumers don't copy this file: their own scripts/ktlint.sh is a small wrapper that downloads this
# runner from the tag of the ruleset version they pin, so the runner and the rules always match.
# The ktlint version comes from the ruleset's POM (<ktlint.version>), never from the consumer.
#
# Environment:
#   KTLINT_RULESET_JAR  a local ruleset jar to use instead of the released one (to try a rule change)
#   KTLINT_VERSION      the ktlint CLI version, for a ruleset version that isn't released yet
#   KTLINT_PROJECT_ROOT the repository to lint (the wrapper sets it; defaults to the current git repository)
#   KTLINT_CACHE_DIR    where the CLI, jars and POMs are cached (default ~/.cache/ktlint)
#   KTLINT_REQUIRE_SIGNATURE=true  fail instead of falling back to the SHA-1 when gpg isn't installed (CI)

set -e

RULESET_VERSION="$1"
shift || true

if [ -z "$RULESET_VERSION" ]; then
    echo "❌ Usage: ktlint.sh <ruleset-version> [--format]"
    exit 1
fi

FORMAT_MODE=false
PRINT_VERSIONS=false

if [ "$1" = "--format" ]; then
    FORMAT_MODE=true
elif [ "$1" = "--print-versions" ]; then
    PRINT_VERSIONS=true
fi

# KTLINT_RULESET_JAR is relative to where the script was called from, not to the project root
if [ -n "$KTLINT_RULESET_JAR" ]; then
    if [ ! -f "$KTLINT_RULESET_JAR" ]; then
        echo "❌ KTLINT_RULESET_JAR does not point at a file: $KTLINT_RULESET_JAR"
        exit 1
    fi
    KTLINT_RULESET_JAR="$(CDPATH='' cd -- "$(dirname -- "$KTLINT_RULESET_JAR")" > /dev/null && pwd)/$(basename -- "$KTLINT_RULESET_JAR")"
fi

# The wrapper passes its own repository; called directly, the runner lints the repository it is called from
PROJECT_ROOT="${KTLINT_PROJECT_ROOT:-$(git rev-parse --show-toplevel 2> /dev/null || pwd)}"
cd "$PROJECT_ROOT"

KTLINT_CACHE_DIR="${KTLINT_CACHE_DIR:-$HOME/.cache/ktlint}"
MAVEN_BASE="https://repo1.maven.org/maven2/software/quare/ktlint-rules/$RULESET_VERSION"
RULESET_JAR="$KTLINT_CACHE_DIR/ktlint-rules-$RULESET_VERSION.jar"
RULESET_POM="$KTLINT_CACHE_DIR/ktlint-rules-$RULESET_VERSION.pom"
REPORT_PATH="build/ktlint-report.xml"

# The key the Publish workflow signs every release with. A jar that isn't signed by it never runs.
SIGNING_KEY_FINGERPRINT="E1018ADBFDB95137A0E24A7C43A5E7143D9905D1"
SIGNING_KEY='
-----BEGIN PGP PUBLIC KEY BLOCK-----

mDMEatQKNxYJKwYBBAHaRw8BAQdAs/Uk+5xWtN+hHs2E8tYDZTRNwzWFQZUefRGV
7Yf5i4i0LVF1YXJ0ZWwgRW50ZXJwcmlzZSA8cXVhcmUuc29mdHdhcmVAZ21haWwu
Y29tPoivBBMWCgBXFiEE4QGK2/25UTeg4kp8Q6XnFD2ZBdEFAmrUCjcbFIAAAAAA
BAAObWFudTIsMi41KzEuMTIsMCwzAhsDBQsJCAcCAiICBhUKCQgLAgQWAgMBAh4H
AheAAAoJEEOl5xQ9mQXRwi4A/jYCqhOJJFDKgFZ3EAmAJ+faUzGaq6d9GH3/FnA6
U/NnAQCo2GqN+cDp0UuJX5/U9yYRDZC2rzE8owPWLgMV7juNDQ==
=ogMM
-----END PGP PUBLIC KEY BLOCK-----
'

# Downloads $2 to $1 once. Each run writes its own temporary file and moves it into place only when
# complete (and, with $4, verified), so a concurrent or interrupted run never caches a partial file.
download_once() {
    local target="$1"
    local url="$2"
    local label="$3"
    local verify="$4"

    if [ -f "$target" ]; then
        return
    fi

    echo "⬇️  Downloading $label..." >&2
    mkdir -p "$KTLINT_CACHE_DIR"
    local tmp
    tmp="$(mktemp "$target.XXXXXX")"

    if ! curl -fsSLo "$tmp" "$url"; then
        rm -f "$tmp"
        echo "❌ Failed to download $label from $url" >&2
        exit 1
    fi

    if [ -n "$verify" ] && ! "$verify" "$tmp" "$url" "$label"; then
        rm -f "$tmp"
        exit 1
    fi

    mv "$tmp" "$target"
    echo "" >&2
}

# Checks $1 against the release signature ($2.asc), or against Maven Central's SHA-1 when gpg is missing
verify_artifact() {
    local file="$1"
    local url="$2"
    local label="$3"

    if command -v gpg &> /dev/null; then
        local gnupg_home
        gnupg_home="$(mktemp -d)"
        local signature="$gnupg_home/artifact.asc"

        if ! curl -fsSLo "$signature" "$url.asc"; then
            rm -rf "$gnupg_home"
            echo "❌ Failed to download the signature of $label from $url.asc" >&2
            return 1
        fi

        printf '%s' "$SIGNING_KEY" | gpg --homedir "$gnupg_home" --batch --quiet --import 2> /dev/null
        local status
        status="$(gpg --homedir "$gnupg_home" --batch --status-fd 1 --verify "$signature" "$file" 2> /dev/null)"
        rm -rf "$gnupg_home"

        if ! grep -q "^\[GNUPG:\] VALIDSIG $SIGNING_KEY_FINGERPRINT " <<< "$status"; then
            echo "❌ $label is not signed by the Quare release key $SIGNING_KEY_FINGERPRINT" >&2
            return 1
        fi
        return 0
    fi

    if [ "$KTLINT_REQUIRE_SIGNATURE" = true ]; then
        echo "❌ gpg is not installed, and KTLINT_REQUIRE_SIGNATURE=true needs it to verify $label" >&2
        return 1
    fi

    local expected
    expected="$(curl -fsSL "$url.sha1" | cut -c1-40)"
    local actual
    actual="$(shasum -a 1 "$file" | cut -c1-40)"

    if [ -z "$expected" ] || [ "$expected" != "$actual" ]; then
        echo "❌ $label does not match its SHA-1 on Maven Central" >&2
        return 1
    fi
    echo "⚠️  gpg is not installed: $label was checked against its SHA-1 only" >&2
}

if [ -z "$KTLINT_VERSION" ]; then
    download_once "$RULESET_POM" "$MAVEN_BASE/ktlint-rules-$RULESET_VERSION.pom" "the ruleset $RULESET_VERSION POM" \
        verify_artifact
    KTLINT_VERSION=$(sed -nE 's/.*<ktlint\.version>([^<]+)<\/ktlint\.version>.*/\1/p' "$RULESET_POM" | head -1)

    if [ -z "$KTLINT_VERSION" ]; then
        echo "❌ The ruleset $RULESET_VERSION POM declares no ktlint.version (it is published from 1.0.1 on)"
        exit 1
    fi
fi

if [ "$PRINT_VERSIONS" = true ]; then
    echo "$RULESET_VERSION $KTLINT_VERSION"
    exit 0
fi

echo "🔍 Running ktlint..."
echo "📁 Working directory: $PROJECT_ROOT"
echo ""

# The ktlint CLI is a self-executing jar
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install a JDK (17 or newer)."
    exit 1
fi

KTLINT_BIN="$KTLINT_CACHE_DIR/ktlint-$KTLINT_VERSION"

download_once "$KTLINT_BIN" \
    "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint" \
    "the ktlint $KTLINT_VERSION CLI"
chmod +x "$KTLINT_BIN"

if [ -n "$KTLINT_RULESET_JAR" ]; then
    echo "⚠️  Using the local ruleset $KTLINT_RULESET_JAR instead of $RULESET_VERSION (KTLINT_RULESET_JAR is set)"
    echo ""
    RULESET_JAR="$KTLINT_RULESET_JAR"
else
    download_once "$RULESET_JAR" "$MAVEN_BASE/ktlint-rules-$RULESET_VERSION.jar" "the ruleset $RULESET_VERSION" \
        verify_artifact
fi

KTLINT_ARGS=(
    --ruleset="$RULESET_JAR"
    --relative
    --reporter=plain
)

if [ "$FORMAT_MODE" = true ]; then
    echo "🛠️  Running ktlint $KTLINT_VERSION format with the ruleset $RULESET_VERSION..."
    KTLINT_ARGS+=(--format)
else
    echo "📋 Running ktlint $KTLINT_VERSION check with the ruleset $RULESET_VERSION..."
    mkdir -p build
    KTLINT_ARGS+=(--reporter=checkstyle,output="$REPORT_PATH")
fi
echo ""

set +e
"$KTLINT_BIN" "${KTLINT_ARGS[@]}" \
    "**/src/**/*.kt" \
    "**/*.kts" \
    "!**/build/**" \
    "!**/generated/**"
KTLINT_EXIT_CODE=$?
set -e

echo ""
if [ $KTLINT_EXIT_CODE -ne 0 ]; then
    if [ "$FORMAT_MODE" = true ]; then
        echo "❌ ktlint found errors that cannot be autocorrected. Fix them by hand."
    else
        echo "❌ ktlint found formatting errors."
        echo ""
        echo "To fix them, run:"
        echo "  ./scripts/ktlint.sh --format"
    fi
    echo ""
    echo "✨ ktlint finished (with errors)!"
    exit $KTLINT_EXIT_CODE
fi

if [ "$FORMAT_MODE" = false ]; then
    echo "✅ Report saved to: $REPORT_PATH"
fi
echo "✨ ktlint finished!"
