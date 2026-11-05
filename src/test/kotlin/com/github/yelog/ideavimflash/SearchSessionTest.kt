package com.github.yelog.vimflash

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import org.yelog.ideavim.flash.JumpHandler
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.Mode
import org.yelog.ideavim.flash.UserConfig

class SearchSessionTest : BasePlatformTestCase() {
    private lateinit var config: UserConfig.DataBean
    private val originalConfig = UserConfig.DataBean()
    private lateinit var dataContext: DataContext

    override fun setUp() {
        super.setUp()
        config = UserConfig.getDataBean()
        XmlSerializerUtil.copyBean(config, originalConfig)
        config.characters = "ab"
        config.searchAcrossSplits = false
        config.autoJumpWhenSingle = false
        config.enterConfirmsNearestMatch = true
        config.vimModeTimeoutMillis = -1
        myFixture.configureByText(PlainTextFileType.INSTANCE, "_a _a")
        dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.EDITOR, myFixture.editor)
            .add(CommonDataKeys.PROJECT, project)
            .build()
    }

    override fun tearDown() {
        try {
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
            XmlSerializerUtil.copyBean(originalConfig, config)
        } finally {
            super.tearDown()
        }
    }

    private fun start(mode: Mode) {
        JumpHandler.start(mode, AnActionEvent.createFromDataContext("test", null, dataContext))
    }

    private fun type(character: Char) {
        TypedAction.getInstance().rawHandler.execute(myFixture.editor, character, dataContext)
    }

    fun testEnterSelectsNearestMatchWithoutLabelsAndRemovesCanvas() {
        myFixture.editor.caretModel.moveToOffset(4)
        start(Mode.SEARCH)
        UIUtil.dispatchAllInvocationEvents()
        type('_')

        assertEquals(4, myFixture.editor.caretModel.offset)
        assertTrue(myFixture.editor.contentComponent.components.any { it is MarksCanvas })
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        assertEquals(3, myFixture.editor.caretModel.offset)
        assertEquals("_a _a", myFixture.editor.document.text)
        assertFalse(myFixture.editor.contentComponent.components.any { it is MarksCanvas })
        assertFalse(TypedAction.getInstance().rawHandler === JumpHandler)
    }

    fun testVimFindKeepsItsOwnHighlightersWithoutSearchCanvas() {
        myFixture.editor.caretModel.moveToOffset(0)
        start(Mode.VIM_F)
        type('a')

        assertEquals(1, myFixture.editor.caretModel.offset)
        assertTrue(myFixture.editor.markupModel.allHighlighters.isNotEmpty())
        assertFalse(myFixture.editor.contentComponent.components.any { it is MarksCanvas })
        type('f')
        assertEquals(4, myFixture.editor.caretModel.offset)
        assertFalse(myFixture.editor.contentComponent.components.any { it is MarksCanvas })
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
        assertFalse(TypedAction.getInstance().rawHandler === JumpHandler)
    }
}
