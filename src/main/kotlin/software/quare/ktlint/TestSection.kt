package software.quare.ktlint

internal enum class TestSection(
    val comment: String,
) {
    GIVEN("// Given"),
    WHEN("// When"),
    THEN("// Then"),
    ;

    companion object {
        fun findByCommentOrNull(text: String): TestSection? =
            entries.firstOrNull { section -> section.comment == text.trimEnd() }
    }
}
