# Quare ktlint rules

Custom [ktlint](https://pinterest.github.io/ktlint/) rule set shared by the Quare Kotlin projects.

```
software.quare:ktlint-rules
```

## Compatibility

Each release is built against one ktlint version and records it in its POM (`<ktlint.version>`). The runner
reads it from there, so a consuming project only pins the ruleset version and never the ktlint one.

| ktlint-rules | ktlint |
| --- | --- |
| 1.0.1 | 1.8.0 |
| 1.0.0 | 1.8.0 (no runner, no `ktlint.version` in the POM) |

## Usage

### In a project

A project pins the ruleset version and keeps a small wrapper that never changes:

```bash
mkdir -p scripts
curl -fsSLo scripts/ktlint.sh \
  https://raw.githubusercontent.com/Quartel-Enterprise/ktlint-rules/1.0.1/consumer/ktlint.sh
chmod +x scripts/ktlint.sh
```

```toml
# gradle/libs.versions.toml (or the bare version in a .ktlint-rules-version file)
[versions]
quare-ktlint-rules = "1.0.1"

[libraries]
# Unused by Gradle; it lets Dependabot / Renovate propose new releases
quare-ktlint-rules = { module = "software.quare:ktlint-rules", version.ref = "quare-ktlint-rules" }
```

```bash
./scripts/ktlint.sh            # check, writes build/ktlint-report.xml
./scripts/ktlint.sh --format   # autocorrect what can be autocorrected
```

The wrapper downloads the runner ([`scripts/ktlint.sh`](scripts/ktlint.sh)) from the tag of the pinned version.
The runner downloads the ktlint CLI and the ruleset jar into `~/.cache/ktlint`, checks the jar's signature,
and lints `**/src/**/*.kt` and `**/*.kts` of the repository. Bumping the version updates the rules, the
runner and ktlint together.

### In CI

```yaml
- uses: actions/checkout@v6
- uses: Quartel-Enterprise/ktlint-rules/.github/actions/ktlint@1.0.1
```

The action sets up a JDK, caches the CLI and the jar, runs the wrapper, annotates the violations and fails
on any of them. Its tag only versions the action itself; the rules come from the version the project pins.

### In the IDE

The [Ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) for Android Studio / IntelliJ shows the
violations while typing: pick the ktlint version from the table above and add
`~/.cache/ktlint/ktlint-rules-<version>.jar` (downloaded by the first `./scripts/ktlint.sh` run) as an
external ruleset.

### With Gradle

[ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle) takes the jar as any other ruleset; its ktlint
version has to match the table above by hand:

```kotlin
dependencies {
    ktlintRuleset("software.quare:ktlint-rules:1.0.1")
}
```

## Verification

Releases are signed with the key `E1018ADBFDB95137A0E24A7C43A5E7143D9905D1`, which the runner embeds: it
refuses a jar or POM whose `.asc` signature isn't from that key. Without `gpg` installed it falls back to
Maven Central's SHA-1 and warns; `KTLINT_REQUIRE_SIGNATURE=true` (set by the CI action) makes gpg mandatory.

## Configuration

The rule set id is `quare`. Every rule is enabled by default; turn one off, or scope it to some files,
in `.editorconfig`:

```editorconfig
[*.{kt,kts}]
ktlint_quare_injected-realtime = disabled
# Composable suffixes this project allows on top of the built-in ones
ktlint_quare_composable_extra_suffixes = Artwork, Waveform

[**/src/*Test/**]
ktlint_quare_comments-say-why = disabled
```

## Rules

| Rule id | |
| --- | --- |
| `comments-say-why` | Production comments only explain why (`// Why:`) |
| `companion-object-constants` | A companion object holds constants and public API, not the class's own private vals |
| `companion-object-duration` | `Duration` constants are private class properties, not companion members |
| `composable-naming-suffix` | Composable names end with an allowed suffix (`Button`, `Card`, `Content`, …, or `Component`), extended by `ktlint_quare_composable_extra_suffixes` |
| `constructor-property-order` | Constructor properties come before plain parameters |
| `data-class-default-value` | Data class properties have no default values |
| `data-class-own-file` | Each data class lives in its own file |
| `dto-serial-name` | DTO fields declare `@SerialName` and have no default value |
| `explicit-backing-field` | Use an explicit backing field (`field = ...`) instead of a `_name` + `name` pair |
| `fun-interface-required` | Single abstract method interfaces are `fun interface` |
| `injected-realtime` | Supabase Realtime is injected, not read from the client |
| `interface-implementation-separate-files` | An interface and its implementation live in separate files |
| `manual-time-unit-conversion` | Use `Duration` instead of hand-written time unit conversions |
| `max-file-length` | Files stay under 600 lines |
| `prefer-method-reference` | Use `::fn` instead of a forwarding lambda |
| `prepare-scenario-in-given` | Tests call `prepareScenario` in the Given section |
| `prepare-scenario-last-member` | `prepareScenario` is the last member of a test class |
| `prepare-scenario-returns-unit` | `prepareScenario` returns `Unit` |
| `private-top-level-val-naming` | Private top-level vals use lowerCamelCase |
| `redundant-private-constructor-property` | Constructor properties only used in init are plain parameters |
| `redundant-sam-constructor-argument` | No SAM constructor where a lambda is enough |
| `suspend-run-catching` | Use `suspendRunCatching` instead of catching `CancellationException` by hand |
| `test-body-sections` | Tests are split into Given / When / Then sections |
| `test-name-given-when-then` | Test names follow `given ..., when ..., then ...` |
| `top-level-val-blank-line` | Consecutive private top-level vals have no blank line between them |
| `top-level-val-ownership` | A private top-level val read by a single class is declared in that class |
| `top-level-val-position` | Private top-level vals come before the first function, class or object |
| `two-branch-when` | A `when` with a single condition + `else` is an `if`/`else` |
| `unit-function-block-body` | `Unit` functions use a block body; expression bodies declare their return type |
| `unused-function-parameter` | No unused function parameters |
| `value-returning-function-naming` | Functions that return a value are named with a verb phrase, not a noun |
| `when-entry-single-statement-braces` | No braces around a single-line `when` entry body |

## Development

```bash
./gradlew test
./gradlew jar   # build/libs/ktlint-rules-<version>.jar
```

To try a change in a project before releasing it, point its wrapper at the jar:

```bash
KTLINT_RULESET_JAR=~/StudioProjects/ktlint-rules/build/libs/ktlint-rules-<version>.jar ./scripts/ktlint.sh
```

CI lints this repository with the runner and the jar it just built, so a rule or runner change that breaks
must be fixed here first.

## Releasing

1. On `main`, bump `version` in `build.gradle.kts` (and `ktlint` in the catalog, plus the table above, when
   moving to another ktlint).
2. Create a GitHub release tagged with that version (`1.2.3`). The tag is also what consumers' wrappers
   download the runner from, so never move or delete it.

The `Publish` workflow checks that the tag matches, runs the tests, publishes and releases to Maven Central,
and attaches the jar to the release. It needs these repository secrets: `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD` (a Central Portal user token), `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and
`GPG_KEY_CONTENTS`. Signing with another key also needs the fingerprint and public key in `scripts/ktlint.sh`
updated, or every consumer refuses the new release.
