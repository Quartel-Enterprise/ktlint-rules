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
#   KTLINT_REQUIRE_SIGNATURE=true  fail instead of falling back (SHA-1 for the jar, nothing for the CLI) when
#                       gpg isn't installed (CI)

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

# The key ktlint signs its CLI releases with (published on keyserver.ubuntu.com, see ktlint's install docs)
KTLINT_SIGNING_KEY_FINGERPRINT="ADBC987D1A7B91DB6B0AAA81995EFBF4A3D20BEB"
KTLINT_SIGNING_KEY='
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
RUjO/QWG6CIej/nl7O0g91NtlEY=
=Ixqm
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

# Checks $1 against the signature at $2, which must be made by the key $4 (fingerprint $5, a primary key)
verify_signature() {
    local file="$1"
    local signature_url="$2"
    local label="$3"
    local key="$4"
    local fingerprint="$5"
    local key_name="$6"

    local gnupg_home
    gnupg_home="$(mktemp -d)"
    local signature="$gnupg_home/artifact.asc"

    if ! curl -fsSLo "$signature" "$signature_url"; then
        rm -rf "$gnupg_home"
        echo "❌ Failed to download the signature of $label from $signature_url" >&2
        return 1
    fi

    printf '%s' "$key" | gpg --homedir "$gnupg_home" --batch --quiet --import 2> /dev/null
    local status
    status="$(gpg --homedir "$gnupg_home" --batch --status-fd 1 --verify "$signature" "$file" 2> /dev/null)"
    rm -rf "$gnupg_home"

    # The last field of VALIDSIG is the primary key's fingerprint, whichever of its subkeys signed
    if ! grep -Eq "^\[GNUPG:\] VALIDSIG .* $fingerprint\$" <<< "$status"; then
        echo "❌ $label is not signed by the $key_name $fingerprint" >&2
        return 1
    fi
}

# Checks $1 against the release signature ($2.asc), or against Maven Central's SHA-1 when gpg is missing
verify_artifact() {
    local file="$1"
    local url="$2"
    local label="$3"

    if command -v gpg &> /dev/null; then
        verify_signature "$file" "$url.asc" "$label" "$SIGNING_KEY" "$SIGNING_KEY_FINGERPRINT" "Quare release key"
        return
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

# Checks the ktlint CLI against its release signature ($2.asc). ktlint publishes no checksum, so without gpg
# the CLI runs unchecked (HTTPS only), unless KTLINT_REQUIRE_SIGNATURE=true
verify_ktlint_cli() {
    local file="$1"
    local url="$2"
    local label="$3"

    if command -v gpg &> /dev/null; then
        verify_signature "$file" "$url.asc" "$label" "$KTLINT_SIGNING_KEY" "$KTLINT_SIGNING_KEY_FINGERPRINT" \
            "ktlint signing key"
        return
    fi

    if [ "$KTLINT_REQUIRE_SIGNATURE" = true ]; then
        echo "❌ gpg is not installed, and KTLINT_REQUIRE_SIGNATURE=true needs it to verify $label" >&2
        return 1
    fi
    echo "⚠️  gpg is not installed: $label was not verified (ktlint publishes no checksum)" >&2
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
    "https://github.com/ktlint/ktlint/releases/download/$KTLINT_VERSION/ktlint" \
    "the ktlint $KTLINT_VERSION CLI" \
    verify_ktlint_cli
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
