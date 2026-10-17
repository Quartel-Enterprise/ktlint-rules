package software.quare.ktlint

internal enum class TestSection(
    val comment: String,
) {
    GIVEN("// Given"),
    WHEN("// When"),
    THEN("// Then"),
}
