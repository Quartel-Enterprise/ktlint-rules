# Quare ktlint rules

A set of 33 extra [ktlint](https://ktlint.github.io/ktlint/) rules for Kotlin code, plus a small runner script
and a GitHub Action that run them next to ktlint's standard rules. The rules enforce structure and naming
conventions that the standard ruleset doesn't cover: one data class per file, no default values in data
classes, `// Why:`-only comments, Given/When/Then tests, and more (see [Rules](#rules)). Most of them are
opinionated, so read the list before you adopt it and turn off what doesn't fit your project.

It works with any Kotlin project (JVM, Android, Multiplatform), with or without Gradle. The ruleset is
published on Maven Central:

```
software.quare:ktlint-rules
```

The rule set id is `quare`, so violations show up as `quare:<rule-id>`.

## Requirements

- macOS or Linux with `bash` and `curl`. Windows isn't tested.
- A JDK 17 or newer on the `PATH` (`java`): the ktlint CLI is a Java program.
- Optional: `gpg`, to check the ruleset's signature. Without it, the runner falls back to the SHA-1 from Maven
  Central and prints a warning. `shasum` is then needed.
- Network access on the first run, to GitHub and Maven Central. Everything is cached in `~/.cache/ktlint`
  afterwards.

## Compatibility

Each release is built against one ktlint version and records it in its POM (`<ktlint.version>`). The runner
reads it from there, so a project only pins the ruleset version, never the ktlint one.

| ktlint-rules | ktlint |
| --- | --- |
| 1.1.0 | 1.8.0 |
| 1.0.2 | 1.8.0 |
| 1.0.1 | 1.8.0 |
| 1.0.0 | 1.8.0 (no runner, no `ktlint.version` in the POM) |

## Setup

### 1. Add the wrapper script

From the root of your repository:

```bash
mkdir -p scripts
curl -fsSLo scripts/ktlint.sh \
  https://raw.githubusercontent.com/Quartel-Enterprise/ktlint-rules/refs/tags/1.1.0/consumer/ktlint.sh
chmod +x scripts/ktlint.sh
```

Keep it at `scripts/ktlint.sh`: the wrapper treats its parent folder as the project root. Commit it with its
executable bit. The wrapper never changes between releases, so you download it once.

### 2. Pin the ruleset version

The wrapper reads the version from one of two places.

With a Gradle version catalog, add it to `gradle/libs.versions.toml`. The key must be exactly
`quare-ktlint-rules`:

```toml
[versions]
quare-ktlint-rules = "1.1.0"

[libraries]
# Unused by Gradle; it lets Dependabot / Renovate propose new releases
quare-ktlint-rules = { module = "software.quare:ktlint-rules", version.ref = "quare-ktlint-rules" }
```

Without a catalog, write the bare version to `.ktlint-rules-version` at the root:

```bash
echo 1.1.0 > .ktlint-rules-version
```

The version must be a release (`X.Y.Z`); the wrapper refuses anything else.

### 3. Run it

```bash
./scripts/ktlint.sh            # check, writes build/ktlint-report.xml
./scripts/ktlint.sh --format   # autocorrect what can be autocorrected
```

The wrapper downloads the runner ([`scripts/ktlint.sh`](scripts/ktlint.sh)) from the tag of the pinned
version. The runner downloads the ktlint CLI and the ruleset jar, checks the jar's signature, and lints every
`**/src/**/*.kt` and `**/*.kts` file of the project, except under `build/` and `generated/` folders. Kotlin
files outside a `src/` folder aren't linted. Bumping the version updates the rules, the runner and ktlint
together.

### 4. Turn off what you don't want

Expect many violations on the first run on an existing codebase. Both the `quare` rules and ktlint's
[standard rules](https://ktlint.github.io/ktlint/latest/rules/standard/) run, with the `ktlint_official`
code style unless your `.editorconfig` sets another `ktlint_code_style`. See
[Configuration](#configuration) for how to disable rules, and which ones to look at first.

## What you see

On success:

```
✅ Report saved to: build/ktlint-report.xml
✨ ktlint finished!
```

On a violation, ktlint prints one line per problem with the file, line, column and rule id, then a summary by
rule, and the script exits with a non-zero code:

```
src/main/kotlin/Point.kt:4:22: Data class property 'x' must not have a default value: pass it explicitly wherever the class is built (quare:data-class-default-value)
...
❌ ktlint found formatting errors.

To fix them, run:
  ./scripts/ktlint.sh --format
```

Run `--format` first. What it can't fix is printed with `(cannot be auto-corrected)` and has to be fixed by
hand, or the rule disabled. `--format` doesn't write the report.

## In CI (GitHub Actions)

Set up steps 1 and 2 above first, then add a workflow:

```yaml
name: ktlint

on:
  pull_request:
  push:
    branches: [main]

jobs:
  ktlint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: Quartel-Enterprise/ktlint-rules/.github/actions/ktlint@1.1.0
```

The action sets up a JDK 21, caches the CLI and the jar, runs the wrapper, adds the violations as annotations
on the pull request files, and fails the job on any of them. It requires `gpg` to verify the jar (preinstalled on
GitHub's Ubuntu runners).

| Input | Default | What it is |
| --- | --- | --- |
| `script` | `./scripts/ktlint.sh` | Path to the wrapper, when you keep it somewhere else |

The rules always come from the version the project pins. The action's tag only versions the CI steps, so bump
it together with the pinned version: the action prints a warning when they differ.

## In the IDE

The [Ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) for Android Studio / IntelliJ shows the
violations while you type. Pick the ktlint version from the [Compatibility](#compatibility) table and add
`~/.cache/ktlint/ktlint-rules-<version>.jar` as an external ruleset. The jar is there after the first
`./scripts/ktlint.sh` run.

## With Gradle

If you use [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle) instead of the script, add the jar as
any other ruleset. Set its ktlint version by hand to the one in the [Compatibility](#compatibility) table:

```kotlin
dependencies {
    ktlintRuleset("software.quare:ktlint-rules:1.1.0")
}
```

## Configuration

Every `quare` rule is enabled by default. Configure them in `.editorconfig`, with ktlint's
`ktlint_<rule set>_<rule id>` properties:

```editorconfig
root = true

[*.{kt,kts}]
# Turn one rule off
ktlint_quare_injected-realtime = disabled
# Composable name suffixes this project allows on top of the built-in ones (spaces or commas)
ktlint_quare_composable_extra_suffixes = Artwork, Waveform
# Conflicts with quare:when-entry-single-statement-braces (see below)
ktlint_standard_when-entry-bracing = disabled

# Test sources of JVM, Android and Multiplatform modules, at the root or in a subfolder
[{src/test/**,**/src/test/**,src/*Test/**,**/src/*Test/**}]
ktlint_quare_comments-say-why = disabled
```

`ktlint_quare = disabled` turns off the whole set (for one folder, say). Settings in a later section override
earlier ones for the files they match.

Rules to review first:

- `comments-say-why` flags every comment that doesn't start with `Why:`, KDoc included. It also flags the
  `// Given` / `// When` / `// Then` comments that `test-body-sections` asks for, so disable it in tests as
  above. A library that documents its public API with KDoc will want it off.
- `test-name-given-when-then` and `test-body-sections` apply to every function annotated `@Test`.
- `injected-realtime` and `suspend-run-catching` assume helpers your project has to provide (see below).
- `standard:when-entry-bracing` must be disabled, as in the example: `--format` adds braces to every `when`
  branch once one needs them, and `when-entry-single-statement-braces` then rejects them.

This repository's own [`.editorconfig`](.editorconfig) is a working example.

## Rules

Autocorrect: "Yes" means `--format` fixes it; "No" means you fix it by hand.

| Rule id | What it checks | Autocorrect |
| --- | --- | --- |
| `comments-say-why` | Every comment (`//`, `/* */`, KDoc) starts with `Why:`. A `// Why:` that runs over several lines becomes a `/* */` block with ` * ` before each line; a one-line block becomes `// Why:` | Reshapes `Why:` comments only; other comments are reported |
| `companion-object-constants` | A private, non-`const` property in a companion object is declared in the class body instead | No |
| `companion-object-duration` | A non-private `Duration` property in a companion object is a private property of the class instead | No |
| `composable-naming-suffix` | A `@Composable` function with a capitalized name (not a `@Preview`) ends with a built-in suffix (`Button`, `Card`, `Screen`, `Content`, …) or one from `ktlint_quare_composable_extra_suffixes`; `Component` is the fallback | No |
| `constructor-property-order` | In a primary constructor, `val` / `var` properties come before plain parameters | No |
| `data-class-default-value` | Data class constructor properties have no default values (except in `@Serializable` classes named `*Dto`, see `dto-serial-name`) | No |
| `data-class-own-file` | A data class or enum doesn't share its file with another top-level class or object | No |
| `dto-serial-name` | In a `@Serializable` class named `*Dto`, every property has `@SerialName` and no default value | No |
| `explicit-backing-field` | Flags a `private val _name` + `val name = _name` (or `_name.asStateFlow()` / `.asSharedFlow()`) pair: use Kotlin's explicit backing field (`field = ...`) instead | No |
| `fun-interface-required` | An interface with exactly one abstract function and no abstract property is a `fun interface` | No |
| `injected-realtime` | For supabase-kt: flags `SupabaseClient.realtime` and `getPlugin(Realtime)`, asking for an injected `Realtime` instead | No |
| `interface-implementation-separate-files` | A class that implements an interface declared in the same file (not sealed, not private) moves to another file | No |
| `manual-time-unit-conversion` | Products of literals that convert time units, like `60 * 1000` or `24 * 60 * 60`, use `kotlin.time.Duration` | No |
| `max-file-length` | A file has at most 600 lines (fixed) | No |
| `named-arguments-on-separate-lines` | A call with two or more named arguments puts each argument, and the closing parenthesis, on its own line | Yes, unless the arguments contain a comment |
| `prefer-method-reference` | A lambda that only passes its one parameter to a function of the same file is a reference (`::fn`). Suspend and `@Composable` functions are skipped | No |
| `prepare-scenario-in-given` | In a `@Test`, calls to `prepareScenario` sit under the `// Given` comment | No |
| `prepare-scenario-last-member` | A function named `prepareScenario` is the last member of its class; only a companion object may follow | No |
| `prepare-scenario-returns-unit` | `prepareScenario` has a block body and no declared return type | No |
| `private-top-level-val-naming` | Private top-level properties that aren't `const` use lowerCamelCase | No |
| `redundant-private-constructor-property` | A `private val` / `var` constructor property read only during initialization is a plain parameter | No |
| `redundant-sam-constructor-argument` | An argument `Foo { ... }`, where `Foo` is a `fun interface` of the same file, is passed as the lambda alone | No |
| `suspend-run-catching` | A `try` that rethrows `CancellationException` and catches `Exception` / `Throwable` uses `suspendRunCatching`, a helper your project provides (it isn't in the standard library) | No |
| `test-body-sections` | A `@Test` has `// Given`, `// When` and `// Then` comments, once each and in that order. `// Given` may be left out when the class has a `@BeforeTest` / `@Before` / `@BeforeEach` function | No |
| `test-name-given-when-then` | A `@Test` is named `` `GIVEN ... WHEN ... THEN ...` `` and uses only letters, digits, spaces, `-` and `_` | No |
| `top-level-val-blank-line` | Consecutive private top-level properties have no blank line between them | No |
| `top-level-val-ownership` | A private top-level property read by a single class or object moves into it (a `const val` into its companion object) | No |
| `top-level-val-position` | Private top-level properties come before the first top-level function, class or object | No |
| `two-branch-when` | A `when` with one branch plus `else` is an `if` / `else` | No |
| `unit-function-block-body` | A function with an expression body declares its return type, and a `Unit` function uses a block body | No |
| `unused-function-parameter` | A parameter the body never reads is removed. Overrides, `open`/`abstract`/`operator`/`expect`/`actual`/`external` functions, interface members and `@Suppress("UNUSED_PARAMETER")` are skipped | No |
| `value-returning-function-naming` | A function with a declared non-`Unit` return type starts with a verb from a built-in list (`get`, `load`, `is`, `to`, …) or ends in `OrNull` / `Of` / `For`. Overrides, composables, `Modifier` extensions and `@Dao` / `@Database` members are skipped | No |
| `when-entry-single-statement-braces` | A `when` branch whose body is a single one-line statement has no braces | No |

## Environment variables

| Variable | What it does |
| --- | --- |
| `KTLINT_CACHE_DIR` | Where the runner, the CLI, the jars and the POMs are cached. Default `~/.cache/ktlint`. The CI action caches only the default path |
| `KTLINT_REQUIRE_SIGNATURE` | `true` fails instead of falling back to the SHA-1 when `gpg` is missing. The CI action sets it |
| `KTLINT_RULESET_JAR` | A local ruleset jar to use instead of the released one, to try a rule change |
| `KTLINT_VERSION` | The ktlint CLI version to use instead of the one in the POM, for a ruleset version that isn't released yet |

## Trust model

- **The runner and the action** come from the release tag of this repository on GitHub, the same way any
  GitHub Action pinned to a tag does. They are as trustworthy as this repository. Releases are immutable
  (repository setting), so a published tag can't be moved to other code.
- **The jar and the POM** come from Maven Central and are signed with the key
  `E1018ADBFDB95137A0E24A7C43A5E7143D9905D1`, which the runner embeds. It refuses any file whose `.asc`
  signature isn't from that key, which covers Maven Central, a mirror or a proxy serving something else.
  Without `gpg` it falls back to Maven Central's SHA-1 and warns; `KTLINT_REQUIRE_SIGNATURE=true` makes `gpg`
  mandatory.
- **The ktlint CLI** comes from ktlint's GitHub releases over HTTPS. The runner doesn't check its signature.

## Development

```bash
./gradlew test
./gradlew jar   # build/libs/ktlint-rules-<version>.jar
```

To try a change in a project before releasing it, point its wrapper at the jar you built:

```bash
KTLINT_RULESET_JAR=/path/to/ktlint-rules/build/libs/ktlint-rules-<version>.jar ./scripts/ktlint.sh
```

CI lints this repository with the runner and the jar it just built, so a rule or runner change that breaks
must be fixed here first.

## Releasing

1. On `main`, bump `version` in `build.gradle.kts` (and `ktlint` in the catalog, plus the table above, when
   moving to another ktlint).
2. Create a GitHub release tagged with that version (`1.2.3`). The tag is also what consumers' wrappers
   download the runner from, so never move or delete it.

The `Publish` workflow checks that the tag matches, runs the tests, and publishes and releases to Maven
Central. Releases are immutable, so a mistake is fixed with a new version, never by editing a release. It
needs these repository secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (a Central Portal user
token), `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and `GPG_KEY_CONTENTS`. Signing with another key also needs the
fingerprint and public key in `scripts/ktlint.sh` updated, or every consumer refuses the new release.
