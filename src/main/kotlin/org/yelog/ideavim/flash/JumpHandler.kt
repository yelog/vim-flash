package org.yelog.ideavim.flash

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import org.yelog.ideavim.flash.action.Finder
import org.yelog.ideavim.flash.action.Search
import org.yelog.ideavim.flash.utils.getVisibleRangeOffset


object JumpHandler : TypedActionHandler {
    const val MODE_CHAR1 = 0
    const val MODE_CHAR2 = 1
    const val MODE_WORD0 = 2
    const val MODE_WORD1 = 3
    const val MODE_LINE = 4
    const val MODE_WORD1_DECLARATION = 5

    private var mOldTypedHandler: TypedActionHandler? = null
    private var mOldEscActionHandler: EditorActionHandler? = null
    private var mOldBackSpaceActionHandler: EditorActionHandler? = null
    private val mMarksCanvas = MarksCanvas()
    private var isStart = false
    private lateinit var finder: Finder
    private var onJump: (() -> Unit)? = null // Runnable that is called after jump
    private var lastMarks: List<MarksCanvas.Mark> = emptyList()
    private var isCanvasAdded = false

    override fun execute(e: Editor, c: Char, dc: DataContext) {
        val marks = finder.input(e, c, lastMarks)
        if (marks != null) {
            lastMarks = marks
            jumpOrShowCanvas(e, lastMarks)
        }
    }

    // 当按下 esc 时
    private val escActionHandler: EditorActionHandler = object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            stop()
        }
    }

    // 当按下删除键时
    private val backSpaceActionHandler: EditorActionHandler = object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            stop()
        }
    }


    private fun jumpOrShowCanvas(e: Editor, marks: List<MarksCanvas.Mark>) {
        when {
            marks.isEmpty() -> {
                stop()
            }

            marks.size == 1 && marks[0].hintMark -> {
                // only one found, just jump to it
                val caret = e.caretModel.currentCaret
                if (caret.hasSelection()) {
                    val downOffset =
                        if (caret.selectionStart == caret.offset)
                            caret.selectionEnd
                        else
                            caret.selectionStart
                    caret.setSelection(downOffset, marks[0].offset)
                }
                // Shamelessly robbed from AceJump: https://github.com/acejump/AceJump/blob/99e0a5/src/main/kotlin/org/acejump/action/TagJumper.kt#L87
                with(e) {
                    project?.let { project ->
                        CommandProcessor.getInstance().executeCommand(
                            project, {
                                with(IdeDocumentHistory.getInstance(project)) {
                                    setCurrentCommandHasMoves()
                                    includeCurrentCommandAsNavigation()
                                    includeCurrentPlaceAsChangePlace()
                                }
                            }, "IdeaVimFlashHistoryAppender", DocCommandGroupId.noneGroupId(document),
                            UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION, document
                        )
                    }
                }
                caret.moveToOffset(marks[0].offset)

                stop()
                onJump?.invoke()
            }

            else -> {
                if (!isCanvasAdded) {
                    mMarksCanvas.sync(e)
                    e.contentComponent.add(mMarksCanvas)
                    e.contentComponent.repaint()
                    isCanvasAdded = true
                }
                mMarksCanvas.setData(marks)
            }
        }
    }

    /**
     * start search mode
     *
     * @param mode mode enum, see [.MODE_CHAR1] [.MODE_CHAR2] etc
     */
    fun start(mode: Int, anActionEvent: AnActionEvent) {
        if (isStart) return
        isStart = true
        val editor = anActionEvent.getData(CommonDataKeys.EDITOR) ?: return
        val manager = EditorActionManager.getInstance()
        val typedAction = TypedAction.getInstance()
        mOldTypedHandler = typedAction.rawHandler
        typedAction.setupRawHandler(this)
        mOldEscActionHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_ESCAPE)
        manager.setActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, escActionHandler)
        mOldBackSpaceActionHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE)
        manager.setActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE, backSpaceActionHandler)

        onJump = null
        when (mode) {
            MODE_CHAR1 -> finder = Search()
            MODE_CHAR2 -> finder = Search()
            else -> throw RuntimeException("Invalid start mode: $mode")
        }
        val visibleBorderOffset = editor.getVisibleRangeOffset()
        val visibleString = editor.document.getText(visibleBorderOffset)
        val marks = finder.start(editor, visibleString, visibleBorderOffset)
        if (marks != null) {
            lastMarks = marks
            jumpOrShowCanvas(editor, lastMarks)
        } else {
            lastMarks = emptyList()
        }
    }

    private fun stop() {
        if (isStart) {
            isStart = false
            val manager = EditorActionManager.getInstance()
            TypedAction.getInstance().setupRawHandler(mOldTypedHandler!!)
            if (mOldEscActionHandler != null) {
                manager.setActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, mOldEscActionHandler!!)
            }
            if (mOldBackSpaceActionHandler != null) {
                manager.setActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE, mOldBackSpaceActionHandler!!)
            }
            val parent = mMarksCanvas.parent
            if (parent != null) {
                parent.remove(mMarksCanvas)
                parent.repaint()
            }
            isCanvasAdded = false
        }
    }
}
