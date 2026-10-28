package software.quare.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class NamedArgumentsOnSeparateLinesRuleTest {
    private val namedArgumentsOnSeparateLinesRuleAssertThat = assertThatRule { NamedArgumentsOnSeparateLinesRule() }

    @Test
    fun `GIVEN two named arguments on the same line WHEN linting THEN reports and wraps each argument`() {
        // Given
        val code =
            """
            val user = User(name = "Ana", age = 30)
            """.trimIndent()
        val formattedCode =
            """
            val user = User(
                name = "Ana",
                age = 30
            )
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted
            .hasLintViolation(1, 17, VIOLATION_MESSAGE)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `GIVEN a named argument sharing a line with a positional one WHEN linting THEN wraps every argument`() {
        // Given
        val code =
            """
            fun main() {
                render(
                    title, subtitle = "Sub", isVisible = true,
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                render(
                    title,
                    subtitle = "Sub",
                    isVisible = true,
                )
            }
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted
            .hasLintViolation(3, 16, VIOLATION_MESSAGE)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `GIVEN nested calls with named arguments on one line WHEN linting THEN wraps both calls`() {
        // Given
        val code =
            """
            val order = Order(item = Item(id = 1, price = 2), quantity = 3)
            """.trimIndent()
        val formattedCode =
            """
            val order = Order(
                item = Item(
                    id = 1,
                    price = 2
                ),
                quantity = 3
            )
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted
            .hasLintViolations(
                LintViolation(1, 19, VIOLATION_MESSAGE),
                LintViolation(1, 31, VIOLATION_MESSAGE),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `GIVEN a comment between named arguments on one line WHEN linting THEN reports without autocorrect`() {
        // Given
        val code =
            """
            val user = User(name = "Ana", /* years */ age = 30)
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(1, 17, VIOLATION_MESSAGE)
    }

    @Test
    fun `GIVEN each named argument on its own line WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val user = User(
                name = "Ana",
                age = 30,
            )
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN a single named argument next to a positional one WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val user = User("Ana", age = 30)
            """.trimIndent()

        // When
        val linted = namedArgumentsOnSeparateLinesRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    private companion object {
        const val VIOLATION_MESSAGE =
            "A call with two or more named arguments puts each argument on its own line"
    }
}
