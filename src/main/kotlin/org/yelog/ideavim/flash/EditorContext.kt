package org.yelog.ideavim.flash

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor

object EditorContext {
    fun getEditor(dataContext: DataContext): Editor? {
        return CommonDataKeys.EDITOR.getData(dataContext)
            ?: CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(dataContext)
    }
}
