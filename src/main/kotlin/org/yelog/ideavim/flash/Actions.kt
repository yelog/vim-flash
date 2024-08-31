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
        // 根据需要选择合适的线程类型，EDT 通常用于更新 UI 相关的操作
        return ActionUpdateThread.EDT
    }
}

class SearchAction : BaseAction() {
    override fun getMode() = JumpHandler.MODE_CHAR1
}

class GotoRecent : BaseAction() {
    override fun getMode() = JumpHandler.MODE_CHAR2
}
