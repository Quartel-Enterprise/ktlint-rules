package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

class WhenEntrySingleStatementBracesRule :
    QuareRule(
        id = "when-entry-single-statement-braces",
        usesEditorConfigProperties = setOf(
            WHEN_ENTRY_BRACING_EXECUTION_PROPERTY,
            STANDARD_RULE_SET_EXECUTION_PROPERTY,
        ),
    ) {
    private var isWhenEntryBracingEnabled = true

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        isWhenEntryBracingEnabled = listOf(
            WHEN_ENTRY_BRACING_EXECUTION_PROPERTY,
            STANDARD_RULE_SET_EXECUTION_PROPERTY,
        ).none { property ->
            editorConfig.getEditorConfigValueOrNull(
                propertyType = RULE_EXECUTION_PROPERTY_TYPE,
                propertyName = property.name,
            ) == RuleExecution.disabled
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != WHEN_ENTRY) return
        val block = node.findCollapsibleBlockOrNull() ?: return
        if (isWhenEntryBracingEnabled && node.hasSiblingEntryThatKeepsBraces()) return

        val bracingHint = if (isWhenEntryBracingEnabled) BRACING_HINT else ""
        emit(
            block.startOffset,
            "when entry with a single-line body should not be wrapped in braces$bracingHint",
            false,
        )
    }

    private fun ASTNode.findCollapsibleBlockOrNull(): ASTNode? {
        val block = findChildByType(BLOCK) ?: return null
        val statements =
            block
                .children20
                .filterNot { it.elementType == LBRACE || it.elementType == RBRACE || it.isWhiteSpace20 }
                .toList()
        val statement = statements.singleOrNull() ?: return null
        if (statement.text.contains('\n')) return null
        if (statement.isReturnedLambda()) return null
        return block
    }

    /*
     * Why: standard:when-entry-bracing braces every entry of a 'when' once one entry keeps a block or has a
     * multi-line body, so removing the braces of the others there would only make --format add them back.
     */
    private fun ASTNode.hasSiblingEntryThatKeepsBraces(): Boolean = treeParent
        .children20
        .filter { entry -> entry.elementType == WHEN_ENTRY }
        .any { entry -> entry.shouldKeepBraces() }

    private fun ASTNode.shouldKeepBraces(): Boolean {
        if (findChildByType(BLOCK) != null) return findCollapsibleBlockOrNull() == null
        val arrow = findChildByType(ARROW) ?: return false
        return arrow.siblings().any { sibling -> sibling.isWhiteSpaceWithNewline20 }
    }

    /*
     * Why: a lambda literal as the sole statement is the block's return value; collapsing the
     * braces would execute it eagerly instead of returning a deferred lambda.
     */
    private fun ASTNode.isReturnedLambda(): Boolean = elementType == LAMBDA_EXPRESSION

    companion object {
        // Why: ktlint_official runs standard:when-entry-bracing unless it is disabled; the other code styles don't.
        val WHEN_ENTRY_BRACING_EXECUTION_PROPERTY: EditorConfigProperty<RuleExecution> =
            EditorConfigProperty(
                name = "ktlint_standard_when-entry-bracing",
                type = RULE_EXECUTION_PROPERTY_TYPE,
                defaultValue = RuleExecution.enabled,
                intellijIdeaCodeStyleDefaultValue = RuleExecution.disabled,
                androidStudioCodeStyleDefaultValue = RuleExecution.disabled,
            )
        val STANDARD_RULE_SET_EXECUTION_PROPERTY: EditorConfigProperty<RuleExecution> =
            EditorConfigProperty(
                name = "ktlint_standard",
                type = RULE_EXECUTION_PROPERTY_TYPE,
                defaultValue = RuleExecution.enabled,
            )
        private const val BRACING_HINT =
            " (unbrace every such entry of the when together: standard:when-entry-bracing re-adds braces while " +
                "one remains)"
    }
}
