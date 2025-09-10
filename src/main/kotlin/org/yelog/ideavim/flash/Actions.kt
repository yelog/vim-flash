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

class FindAction : BaseAction() {
    override fun getMode() = Mode.VIM_F
}

class FindBackwardAction : BaseAction() {
    override fun getMode() = Mode.VIM_F_BACKWARD
}

class TillAction : BaseAction() {
    override fun getMode() = Mode.VIM_T
}

class TillBackwardAction : BaseAction() {
    override fun getMode() = Mode.VIM_T_BACKWARD
}

class RepeatAction : BaseAction() {
    override fun getMode() = Mode.VIM_REPEAT
}

class RepeatBackwardAction : BaseAction() {
    override fun getMode() = Mode.VIM_REPEAT_BACKWARD
}

class TreesitterAction : BaseAction() {
    override fun getMode() = Mode.TREESITTER
}

class RemoteAction : BaseAction() {
    override fun getMode() = Mode.REMOTE
}
