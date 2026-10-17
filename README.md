# Quare ktlint rules

Custom [ktlint](https://pinterest.github.io/ktlint/) rule set shared by the Quare Kotlin projects.

```
software.quare:ktlint-rules
```

## Compatibility

ktlint's rule API changes between minor versions, so each release is built against one ktlint version.
Upgrade the ruleset and ktlint together.

| ktlint-rules | ktlint |
| --- | --- |
| 1.0.0 | 1.8.0 |

## Usage

### Gradle ([ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle))

```kotlin
dependencies {
    ktlintRuleset("software.quare:ktlint-rules:1.0.0")
}
```

### ktlint CLI

The jar is on Maven Central (and attached to each GitHub release), so it can be passed straight to the CLI:

```bash
curl -fsSLo ktlint-rules.jar \
  https://repo1.maven.org/maven2/software/quare/ktlint-rules/1.0.0/ktlint-rules-1.0.0.jar
ktlint --ruleset=ktlint-rules.jar "**/src/**/*.kt"
```

## Configuration

The rule set id is `quare`. Every rule is enabled by default; turn one off, or scope it to some files,
in `.editorconfig`:

```editorconfig
[*.{kt,kts}]
ktlint_quare_injected-realtime = disabled

[**/src/*Test/**]
ktlint_quare_comments-say-why = disabled
```

## Rules

| Rule id | |
| --- | --- |
| `comments-say-why` | Production comments only explain why (`// Why:`) |
| `companion-object-constants` | A companion object holds constants and public API, not the class's own private vals |
| `companion-object-duration` | `Duration` constants are private class properties, not companion members |
| `composable-naming-suffix` | Composable names end with an allowed suffix (`Button`, `Card`, `Content`, …, or `Component`) |
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
./gradlew test               # rule tests
./gradlew publishToMavenLocal  # try a change in another project through mavenLocal()
```

Or point a project's ktlint CLI at `build/libs/ktlint-rules-<version>.jar` directly.

## Releasing

1. Bump `version` in `build.gradle.kts` (and the compatibility table when ktlint changes) on `main`.
2. Create a GitHub release tagged with that version (`1.2.3` or `v1.2.3`).

The `Publish` workflow checks that the tag matches, runs the tests, publishes and releases to Maven Central,
and attaches the jar to the release. It needs these repository secrets: `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD` (a Central Portal user token), `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and
`GPG_KEY_CONTENTS`.
