package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.children20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.parents

class NamedArgumentsRequiredRule : QuareRule(id = "named-arguments-required") {
    // Why: their values (keys, flows, format arguments, items) fill one vararg, which a name would only wrap.
    private val varargFunctions = setOf(
        "arrayListOf",
        "arrayOf",
        "booleanArrayOf",
        "byteArrayOf",
        "charArrayOf",
        "combine",
        "doubleArrayOf",
        "floatArrayOf",
        "flowOf",
        "format",
        "getPluralString",
        "getString",
        "hasLintViolations",
        "hasLintViolationsWithoutAutoCorrect",
        "hashMapOf",
        "hashSetOf",
        "intArrayOf",
        "linkedMapOf",
        "linkedSetOf",
        "listOf",
        "listOfNotNull",
        "longArrayOf",
        "mapOf",
        "maxOf",
        "merge",
        "minOf",
        "mutableListOf",
        "mutableMapOf",
        "mutableSetOf",
        "mutableStateListOf",
        "mutableStateMapOf",
        "persistentListOf",
        "persistentMapOf",
        "persistentSetOf",
        "pluralStringResource",
        "remember",
        "sequenceOf",
        "setOf",
        "setOfNotNull",
        "shortArrayOf",
        "sortedMapOf",
        "sortedSetOf",
        "stringResource",
        "trim",
        "trimEnd",
        "trimStart",
    )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != VALUE_ARGUMENT_LIST) return
        val call = node.treeParent?.takeIf { it.elementType == CALL_EXPRESSION }?.psi as? KtCallExpression ?: return
        val calleeName = call.calleeExpression?.text ?: return
        if (calleeName in varargFunctions) return

        val arguments = node.children20
            .filter { it.elementType == VALUE_ARGUMENT }
            .mapNotNull { it.psi as? KtValueArgument }
            .toList()
        if (arguments.size < MIN_ARGUMENTS) return
        // Why: a spread argument fills a vararg, which only takes a name through an extra `*arrayOf(...)`.
        if (arguments.any { it.getSpreadElement() != null }) return

        val firstPositional = arguments.firstOrNull { !it.isNamed() } ?: return
        // Why: a value of function type, like a callback parameter, can't be called with named arguments.
        if (call.isFunctionTypeValueCall(calleeName)) return
        emit(firstPositional.node.startOffset, VIOLATION_MESSAGE, false)
    }

    private fun KtCallExpression.isFunctionTypeValueCall(calleeName: String): Boolean {
        val valueName = if (calleeName == "invoke") {
            (parent as? KtQualifiedExpression)?.takeIf { it.selectorExpression == this }?.receiverExpression?.text
        } else {
            calleeName
        } ?: return false
        return parents.any { scope ->
            scope.findDeclaredValues().any { it.name == valueName && it.hasFunctionType() }
        }
    }

    private fun PsiElement.findDeclaredValues(): List<KtCallableDeclaration> = when (this) {
        is KtFunction -> valueParameters
        is KtClassOrObject -> primaryConstructorParameters + declarations.filterIsInstance<KtProperty>()
        else -> emptyList()
    }

    private fun KtCallableDeclaration.hasFunctionType(): Boolean {
        val typeElement = typeReference?.typeElement
        val unwrapped = (typeElement as? KtNullableType)?.innerType ?: typeElement
        return unwrapped is KtFunctionType
    }

    private companion object {
        const val MIN_ARGUMENTS = 3
        const val VIOLATION_MESSAGE = "A call with three or more arguments names each of them"
    }
}
