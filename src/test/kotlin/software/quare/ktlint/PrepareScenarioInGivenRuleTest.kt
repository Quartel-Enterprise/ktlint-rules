package software.quare.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class PrepareScenarioInGivenRuleTest {
    private val prepareScenarioInGivenRuleAssertThat = assertThatRule { PrepareScenarioInGivenRule() }

    @Test
    fun `GIVEN prepareScenario called under When WHEN linting THEN reports the call`() {
        // Given
        val code =
            """
            class RepositoryTest {
                @Test
                fun `GIVEN a theme WHEN saving THEN stores it`() = runTest {
                    // When
                    prepareScenario(theme = Theme.DARK)
                    repository.save()

                    // Then
                    assertEquals(Theme.DARK, repository.theme)
                }
            }
            """.trimIndent()

        // When
        val linted = prepareScenarioInGivenRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 5,
            col = 9,
            detail = buildViolationMessage("// When"),
        )
    }

    @Test
    fun `GIVEN prepareScenario called under Then WHEN linting THEN reports the call`() {
        // Given
        val code =
            """
            class RepositoryTest {
                @Test
                fun `GIVEN a theme WHEN saving THEN stores it`() {
                    // Given
                    val theme = Theme.DARK

                    // When
                    val result = save(theme)

                    // Then
                    prepareScenario(theme = theme)
                    assertEquals(theme, result)
                }
            }
            """.trimIndent()

        // When
        val linted = prepareScenarioInGivenRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 11,
            col = 9,
            detail = buildViolationMessage("// Then"),
        )
    }

    @Test
    fun `GIVEN prepareScenario called before any section WHEN linting THEN reports the call`() {
        // Given
        val code =
            """
            class RepositoryTest {
                @BeforeTest
                fun setUp() {
                    seedDatabase()
                }

                @Test
                fun `GIVEN a theme WHEN saving THEN stores it`() = runTest {
                    prepareScenario(theme = Theme.DARK)

                    // When
                    repository.save()

                    // Then
                    assertEquals(Theme.DARK, repository.theme)
                }
            }
            """.trimIndent()

        // When
        val linted = prepareScenarioInGivenRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 9,
            col = 9,
            detail = "'prepareScenario' builds the scenario, so it is called under '// Given', not before any section",
        )
    }

    @Test
    fun `GIVEN prepareScenario called under Given WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            class RepositoryTest {
                @Test
                fun `GIVEN a theme WHEN saving THEN stores it`() = runTest {
                    // Given
                    prepareScenario(theme = Theme.DARK)

                    // When
                    repository.save()

                    // Then
                    assertEquals(Theme.DARK, repository.theme)
                }
            }
            """.trimIndent()

        // When
        val linted = prepareScenarioInGivenRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN prepareScenario called outside a test WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            class RepositoryTest {
                @BeforeTest
                fun setUp() {
                    // When
                    prepareScenario(theme = Theme.DARK)
                }
            }
            """.trimIndent()

        // When
        val linted = prepareScenarioInGivenRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    private fun buildViolationMessage(section: String): String =
        "'prepareScenario' builds the scenario, so it is called under '// Given', not under '$section'"
}
