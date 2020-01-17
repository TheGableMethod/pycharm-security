package security.fixes

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.psi.*

class UseCompareDigestFixer : LocalQuickFix, IntentionAction, HighPriorityAction {
    override fun getText(): String {
        return name
    }

    override fun getFamilyName(): String {
        return "Use compare_digest()"
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return true
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val el = getBinaryExpressionElementAtCaret(file, editor) ?: return
        ApplicationManager.getApplication().runWriteAction {
            var newEl = getNewExpressionAtCaret(file, project, el) ?: return@runWriteAction
            el.replace(newEl)
        }
    }

    fun getNewExpressionAtCaret(file: PsiFile, project: Project, oldElement: PyBinaryExpression): PyCallExpression? {
        val elementGenerator = PyElementGenerator.getInstance(project)
        if (file !is PyFile) return null
        val languageLevel = file.languageLevel
        var compareDigestModule = "hmac"
        if (languageLevel.isAtLeast(LanguageLevel.PYTHON37))
            compareDigestModule = "secrets"
        importFrom(file, project, compareDigestModule, "compare_digest")
        val el = elementGenerator.createCallExpression(languageLevel, "compare_digest")
        el.argumentList?.addArgument(oldElement.leftExpression)
        oldElement.rightExpression?.let { el.argumentList?.addArgument(it) }
        return el
    }

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        return
    }
}