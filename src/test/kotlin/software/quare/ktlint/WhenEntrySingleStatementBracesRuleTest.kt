package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class WhenEntrySingleStatementBracesRuleTest {
    private val whenEntrySingleStatementBracesRuleAssertThat = assertThatRule { WhenEntrySingleStatementBracesRule() }

    @Test
    fun `GIVEN a when entry with a braced single-line statement WHEN linting THEN reports the braces`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else -> "some"
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(2, 10, BRACING_VIOLATION_MESSAGE)
    }

    @Test
    fun `GIVEN a braced entry whose statement spans several lines WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> {
                    format(
                        "none",
                    )
                }
                else -> "some"
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN a braced entry that returns a lambda WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            fun action(count: Int): () -> Unit = when (count) {
                0 -> { { reset() } }
                else -> ::increment
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN bracing runs and another entry has several statements WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else -> {
                    log(count)
                    "some"
                }
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN bracing runs and another entry has a multi-line body WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else ->
                    "some"
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN bracing is disabled and another entry has several statements WHEN linting THEN reports`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else -> {
                    log(count)
                    "some"
                }
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)
            .withEditorConfigOverride(
                WhenEntrySingleStatementBracesRule.WHEN_ENTRY_BRACING_EXECUTION_PROPERTY to RuleExecution.disabled,
            )

        // Then
        linted.hasLintViolationWithoutAutoCorrect(2, 10, VIOLATION_MESSAGE)
    }

    @Test
    fun `GIVEN the standard rule set is disabled WHEN linting THEN reports beside a multi-statement entry`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else -> {
                    log(count)
                    "some"
                }
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)
            .withEditorConfigOverride(
                WhenEntrySingleStatementBracesRule.STANDARD_RULE_SET_EXECUTION_PROPERTY to RuleExecution.disabled,
            )

        // Then
        linted.hasLintViolationWithoutAutoCorrect(2, 10, VIOLATION_MESSAGE)
    }

    @Test
    fun `GIVEN the intellij_idea code style WHEN linting THEN reports beside a multi-statement entry`() {
        // Given
        val code =
            """
            fun describe(count: Int): String = when (count) {
                0 -> { "none" }
                else -> {
                    log(count)
                    "some"
                }
            }
            """.trimIndent()

        // When
        val linted = whenEntrySingleStatementBracesRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.intellij_idea)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(2, 10, VIOLATION_MESSAGE)
    }

    private companion object {
        const val VIOLATION_MESSAGE = "when entry with a single-line body should not be wrapped in braces"
        const val BRACING_VIOLATION_MESSAGE =
            "$VIOLATION_MESSAGE (unbrace every such entry of the when together: standard:when-entry-bracing " +
                "re-adds braces while one remains)"
    }
}
