package software.quare.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NamedArgumentsRequiredRuleTest {
    private val namedArgumentsRequiredRuleAssertThat = assertThatRule { NamedArgumentsRequiredRule() }

    @Test
    fun `GIVEN a call with four positional arguments WHEN linting THEN reports the first one`() {
        // Given
        val code =
            """
            val image = encodeCrop(photo.jpeg, crop.toTransform(side), side, side)
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 1,
            col = 24,
            detail = VIOLATION_MESSAGE,
        )
    }

    @Test
    fun `GIVEN a call nested in a named argument WHEN linting THEN reports the nested call`() {
        // Given
        val code =
            """
            val photo = PreparedBillPhoto(
                image = encodeCrop(photo.jpeg, crop.toWholeTransform(), crop.turnedWidth, crop.turnedHeight),
                frame = crop.toFrameInTurnedPhoto(),
            )
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 2,
            col = 24,
            detail = VIOLATION_MESSAGE,
        )
    }

    @Test
    fun `GIVEN three arguments with only the last one named WHEN linting THEN reports the first positional one`() {
        // Given
        val code =
            """
            val user = User("Ana", 30, isAdmin = true)
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 1,
            col = 17,
            detail = VIOLATION_MESSAGE,
        )
    }

    @Test
    fun `GIVEN three named arguments WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val user = User(
                name = "Ana",
                age = 30,
                isAdmin = true,
            )
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN two positional arguments and a trailing lambda WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val clamped = value.coerceIn(0, 10).also { println(it) }
            val sum = numbers.fold(0, Int::plus)
            val mapped = items.mapIndexed(start, step) { index, item -> item }
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN vararg builders with three values WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val names = listOf("Ana", "Bia", "Caio")
            val totals = combine(first, second, third) { a, b, c -> a + b + c }
            val label = stringResource(Res.string.owes, debtor, creditor)
            val state = remember(first, second, third) { State() }
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN a call to a function-type parameter or property WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            class Row(
                private val onMove: ((from: Int, to: Int, animated: Boolean) -> Unit)?,
            ) {
                fun check(emit: (offset: Int, message: String, fix: Boolean) -> Unit) {
                    emit(0, "message", false)
                    onMove?.invoke(1, 2, true)
                }
            }
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN a spread argument WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            val all = collect(first, second, *rest)
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN an annotation with three values WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            @Suppress("A", "B", "C")
            fun main() = Unit
            """.trimIndent()

        // When
        val linted = namedArgumentsRequiredRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    private companion object {
        const val VIOLATION_MESSAGE = "A call with three or more arguments names each of them"
    }
}
