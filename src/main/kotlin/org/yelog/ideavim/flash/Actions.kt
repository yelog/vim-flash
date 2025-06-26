package org.yelog.ideavim.flash

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

abstract class BaseAction : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        JumpHandler.start(getMode(), e)
    }

    abstract fun getMode(): Int

    override fun getActionUpdateThread(): ActionUpdateThread {
        // Select the appropriate thread type as needed; EDT is typically used for UI-related operations.
        return ActionUpdateThread.EDT
    }
}

class SearchAction : BaseAction() {
    override fun getMode() = JumpHandler.MODE_CHAR1
}

class VimFAction : BaseAction() {
    override fun getMode() = JumpHandler.MODE_VIM_F
}

class GotoRecent : BaseAction() {
    override fun getMode() = JumpHandler.MODE_CHAR2
}

class VimFBackwardAction : BaseAction() {
    override fun getMode() = JumpHandler.MODE_VIM_F_BACKWARD
}
