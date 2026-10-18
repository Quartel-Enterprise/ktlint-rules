package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComposableNamingSuffixRule :
    QuareRule(
        id = "composable-naming-suffix",
        usesEditorConfigProperties = setOf(EXTRA_SUFFIXES_PROPERTY),
    ) {
    private var allowedSuffixes = ALLOWED_SUFFIXES.split(' ')

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        val extraSuffixes = editorConfig[EXTRA_SUFFIXES_PROPERTY]
            .split(' ', ',')
            .filter(String::isNotEmpty)
        allowedSuffixes = ALLOWED_SUFFIXES.split(' ') + extraSuffixes
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != FUN) return
        val function = node.psi as? KtNamedFunction ?: return
        val name = function.name ?: return
        if (!name.first().isUpperCase()) return
        if (!function.hasAnnotationContaining("Composable") || function.hasAnnotationContaining("Preview")) return
        if (allowedSuffixes.any { suffix -> name.endsWith(suffix) }) return

        val identifier = node.findChildByType(IDENTIFIER) ?: return
        emit(
            identifier.startOffset,
            "Composable '$name' must end with one of the allowed suffixes: ${allowedSuffixes.joinToString()}. " +
                "When none describes it, use 'Component'",
            false,
        )
    }

    private fun KtNamedFunction.hasAnnotationContaining(name: String): Boolean =
        annotationEntries.any { annotation -> annotation.shortName?.asString()?.contains(name) == true }

    companion object {
        // Why: each project adds the UI kinds of its own domain (a music app's Artwork) without a ruleset release
        val EXTRA_SUFFIXES_PROPERTY: EditorConfigProperty<String> =
            EditorConfigProperty(
                type = PropertyType(
                    "ktlint_quare_composable_extra_suffixes",
                    "Composable name suffixes allowed on top of the built-in ones, separated by spaces or commas",
                    PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                    emptySet(),
                ),
                defaultValue = "",
            )

        private const val ALLOWED_SUFFIXES =
            "Action Badge Banner Bar Box Bubble Button Card Cell Chip Collector Column Component Content Dialog " +
                "Display Drawer Effect Fab Field Footer Grid Handle Header Heading Hero Icon Image Indicator Item " +
                "Label Layout Line List Menu Message Option Overlay Pane Panel Picker Pill Rail Root Row Scaffold " +
                "Screen Section Sheet Sidebar Skeleton Slider Snackbar Spacer Spinner Surface Switch Text Theme " +
                "Tile Title Toggle"
    }
}
