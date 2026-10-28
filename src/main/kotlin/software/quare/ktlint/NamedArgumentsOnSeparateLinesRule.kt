package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indentWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtValueArgument

class NamedArgumentsOnSeparateLinesRule :
    QuareRule(
        id = "named-arguments-on-separate-lines",
        usesEditorConfigProperties = setOf(INDENT_SIZE_PROPERTY, INDENT_STYLE_PROPERTY),
    ) {
    private val commentTypes = setOf(EOL_COMMENT, BLOCK_COMMENT)
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig = IndentConfig(
            indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
            tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
        )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != VALUE_ARGUMENT_LIST) return

        val arguments = node.children20.filter { it.elementType == VALUE_ARGUMENT }.toList()
        val namedArgumentCount = arguments.count { (it.psi as? KtValueArgument)?.isNamed() == true }
        if (namedArgumentCount < MIN_NAMED_ARGUMENTS) return

        val closingParenthesis = node.findChildByType(RPAR) ?: return
        val misplaced = (arguments + closingParenthesis).filterNot { it.isOnOwnLine() }
        if (misplaced.isEmpty()) return

        // Why: rewriting the whitespace around a comment could move it onto another argument or out of the call.
        val canBeAutoCorrected = node.children20.none { it.elementType in commentTypes }

        emit(misplaced.first().startOffset, VIOLATION_MESSAGE, canBeAutoCorrected).ifAutocorrectAllowed {
            val lineIndent = node.indentWithoutNewlinePrefix
            arguments.forEach { it.upsertWhitespaceBeforeMe("\n$lineIndent${indentConfig.indent}") }
            closingParenthesis.upsertWhitespaceBeforeMe("\n$lineIndent")
        }
    }

    private fun ASTNode.isOnOwnLine(): Boolean = prevLeaf?.isWhiteSpaceWithNewline20 == true

    private companion object {
        const val MIN_NAMED_ARGUMENTS = 2
        const val VIOLATION_MESSAGE =
            "A call with two or more named arguments puts each argument on its own line"
    }
}
