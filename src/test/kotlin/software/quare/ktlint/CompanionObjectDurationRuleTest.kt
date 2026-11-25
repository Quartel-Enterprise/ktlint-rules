package software.quare.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class CompanionObjectDurationRuleTest {
    private val companionObjectDurationRuleAssertThat = assertThatRule { CompanionObjectDurationRule() }

    @Test
    fun `GIVEN a public companion Duration built from a number WHEN linting THEN reports the property`() {
        // Given
        val code =
            """
            class DevicesSynchronizer {
                companion object {
                    val INITIAL_BACKOFF = 2.seconds
                }
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 13,
            detail = buildViolationMessage("INITIAL_BACKOFF"),
        )
    }

    @Test
    fun `GIVEN a companion property typed as Duration WHEN linting THEN reports the property`() {
        // Given
        val code =
            """
            class Poller {
                companion object {
                    val pollInterval: Duration = readInterval()
                }
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 13,
            detail = buildViolationMessage("pollInterval"),
        )
    }

    @Test
    fun `GIVEN companion Durations built from factories WHEN linting THEN reports each property`() {
        // Given
        val code =
            """
            class Poller {
                companion object {
                    internal val timeout = Duration.parse("PT1M")
                    val idle = 5.toDuration(DurationUnit.SECONDS)
                }
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 22,
                detail = buildViolationMessage("timeout"),
            ),
            LintViolation(
                line = 4,
                col = 13,
                detail = buildViolationMessage("idle"),
            ),
        )
    }

    @Test
    fun `GIVEN a Duration in a private companion object WHEN linting THEN leaves it to the constants rule`() {
        // Given
        val code =
            """
            class Poller {
                private companion object {
                    val pollInterval = 3.seconds
                }
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN a Duration as a private val in the class body WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            class DevicesSynchronizer {
                private val initialBackoff: Duration = 2.seconds
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }

    @Test
    fun `GIVEN other constants in a companion object WHEN linting THEN reports nothing`() {
        // Given
        val code =
            """
            class RewardedAd {
                companion object {
                    const val LOG_TAG = "RewardedAd"
                    val Empty = RewardedAd()
                }
            }
            """.trimIndent()

        // When
        val linted = companionObjectDurationRuleAssertThat(code)

        // Then
        linted.hasNoLintViolations()
    }
}

private fun buildViolationMessage(name: String): String =
    "Duration '$name' should be a private val in the class body, not a companion object member"
