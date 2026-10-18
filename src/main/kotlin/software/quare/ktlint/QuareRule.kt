package software.quare.ktlint

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.About
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty

private val quareAbout = About(
    maintainer = "Quartel Enterprise",
    repositoryUrl = "https://github.com/Quartel-Enterprise/ktlint-rules",
    issueTrackerUrl = "https://github.com/Quartel-Enterprise/ktlint-rules/issues",
)

abstract class QuareRule(
    id: String,
    usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : Rule(
        ruleId = RuleId("$RULE_SET_ID:$id"),
        about = quareAbout,
        usesEditorConfigProperties = usesEditorConfigProperties,
    ),
    RuleAutocorrectApproveHandler
