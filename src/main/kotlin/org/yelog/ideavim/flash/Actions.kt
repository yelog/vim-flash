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

    abstract fun getMode(): Mode

    override fun getActionUpdateThread(): ActionUpdateThread {
        // Select the appropriate thread type as needed; EDT is typically used for UI-related operations.
        return ActionUpdateThread.EDT
    }
}

class SearchAction : BaseAction() {
    override fun getMode() = Mode.SEARCH
}

class VimFAction : BaseAction() {
    override fun getMode() = Mode.VIM_F
}

class VimFBackwardAction : BaseAction() {
    override fun getMode() = Mode.VIM_F_BACKWARD
}

class VimTAction : BaseAction() {
    override fun getMode() = Mode.VIM_T
}

class VimTBackwardAction : BaseAction() {
    override fun getMode() = Mode.VIM_T_BACKWARD
}
