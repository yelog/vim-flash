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
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import org.yelog.ideavim.flash.action.Finder
import org.yelog.ideavim.flash.action.Search
import org.yelog.ideavim.flash.utils.getVisibleRangeOffset
import java.awt.Color


object JumpHandler : TypedActionHandler {
    const val MODE_CHAR1 = 0
    const val MODE_CHAR2 = 1

    private var mOldTypedHandler: TypedActionHandler? = null
    private var mOldEscActionHandler: EditorActionHandler? = null
    private var mOldBackSpaceActionHandler: EditorActionHandler? = null
    private var mOldEnterActionHandler: EditorActionHandler? = null
    private val mMarksCanvas = MarksCanvas()
    private var isStart = false
    private lateinit var finder: Finder
    private var onJump: (() -> Unit)? = null // Runnable that is called after jump
    private var lastMarks: List<MarksCanvas.Mark> = emptyList()
    private var isCanvasAdded = false
    // Record the string being currently searched
    private var searchString = ""

    // List to keep track of the added highlighters
    private val highlighters = mutableListOf<RangeHighlighter>()

    override fun execute(e: Editor, c: Char, dc: DataContext) {
        this.searchString += c
        val marks = finder.input(e, c, lastMarks, searchString)
        if (marks != null) {
            lastMarks = marks
            jumpOrShowCanvas(e, lastMarks)
        }
    }

    // When the Escape key is pressed
    private val escActionHandler: EditorActionHandler = object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            stop(editor)
        }
    }

    // When the Delete key is pressed
    private val backSpaceActionHandler: EditorActionHandler = object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            stop(editor)
        }
    }

    // When the Enter key is pressed
    private val enterActionHandler: EditorActionHandler = object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            editor.caretModel.currentCaret.moveToOffset(lastMarks[0].offset)
            stop(editor)
            onJump?.invoke()
        }
    }


    private fun jumpOrShowCanvas(editor: Editor,marks: List<MarksCanvas.Mark>) {
        when {
            marks.isEmpty() -> {
                stop(editor)
            }

            marks.size == 1 && marks[0].hintMark -> {
                // only one found, just jump to it
                val caret = editor.caretModel.currentCaret
                if (caret.hasSelection()) {
                    val downOffset =
                        if (caret.selectionStart == caret.offset)
                            caret.selectionEnd
                        else
                            caret.selectionStart
                    caret.setSelection(downOffset, marks[0].offset)
                }
                // Shamelessly robbed from AceJump: https://github.com/acejump/AceJump/blob/99e0a5/src/main/kotlin/org/acejump/action/TagJumper.kt#L87
                with(editor) {
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

                stop(editor)
                onJump?.invoke()
            }

            else -> {
                if (!isCanvasAdded) {
                    mMarksCanvas.sync(editor)
                    editor.contentComponent.add(mMarksCanvas)
                    editor.contentComponent.repaint()
                    isCanvasAdded = true
                }
                mMarksCanvas.setData(marks, this.searchString)
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
        this.searchString = ""
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
        mOldEnterActionHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_ENTER)
        manager.setActionHandler(IdeActions.ACTION_EDITOR_ENTER, enterActionHandler)

        setGrayColor(editor, true);

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

    private fun stop(editor: Editor) {
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
            if (mOldEnterActionHandler != null) {
                manager.setActionHandler(IdeActions.ACTION_EDITOR_ENTER, mOldEnterActionHandler!!)
            }
            val parent = mMarksCanvas.parent
            if (parent != null) {
                parent.remove(mMarksCanvas)
                parent.repaint()
            }
            isCanvasAdded = false
            // get editor and remove the highlighters
            setGrayColor(editor, false)
        }
    }

    private fun setGrayColor(editor: Editor, setGray: Boolean) {
        if (setGray) {
            // Set gray color of all editor text
            val visibleArea = editor.scrollingModel.visibleArea
            val startLogicalPosition = editor.xyToLogicalPosition(visibleArea.location)
            val endLogicalPosition = editor.xyToLogicalPosition(
                visibleArea.location.apply {
                    this.x += visibleArea.width
                    this.y += visibleArea.height
                }
            )

            val startOffset = editor.logicalPositionToOffset(startLogicalPosition)
            val endOffset = editor.logicalPositionToOffset(endLogicalPosition)

            val grayAttributes = TextAttributes().apply {
                foregroundColor = Color.GRAY
            }

            val markupModel = editor.markupModel
            val highlighter = markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                grayAttributes,
                HighlighterTargetArea.EXACT_RANGE
            )

            // Store the highlighter for later removal
            highlighters.add(highlighter)

        } else {
            val markupModel = editor.markupModel
            // Remove each highlighter that was previously added
            highlighters.forEach { markupModel.removeHighlighter(it) }
            // Clear the list of highlighters after removal
            highlighters.clear()
        }
    }
}
