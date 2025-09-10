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
import org.yelog.ideavim.flash.action.VimF
import org.yelog.ideavim.flash.utils.notify
import java.awt.Color
import kotlin.math.abs


object JumpHandler : TypedActionHandler {
    private val config: UserConfig.DataBean by lazy { UserConfig.getDataBean() }
    private var mOldTypedHandler: TypedActionHandler? = null
    private var mOldEscActionHandler: EditorActionHandler? = null
    private var mOldBackSpaceActionHandler: EditorActionHandler? = null
    private var mOldEnterActionHandler: EditorActionHandler? = null
    private val mMarksCanvas = MarksCanvas()
    private var isStart = false
    private lateinit var finder: Finder
    private var currentMode = Mode.SEARCH
    private var onJump: (() -> Unit)? = null // Runnable that is called after jump
    private var lastMarks: List<MarksCanvas.Mark> = emptyList()
    private var isCanvasAdded = false
    // 记录最近一次 DataContext，用于 stopAndDispatch 透传按键
    private var lastDataContext: DataContext? = null

    // Record the string being currently searched
    private var searchString = ""

    // List to keep track of the added highlighters
    private val highlighters = mutableListOf<RangeHighlighter>()

    override fun execute(e: Editor, c: Char, dc: DataContext) {
        // 保存 DataContext，便于后续透传
        lastDataContext = dc
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
            moveToOffset(editor, lastMarks[0].offset)
            stop(editor)
            onJump?.invoke()
        }
    }


    private fun jumpOrShowCanvas(editor: Editor, marks: List<MarksCanvas.Mark>) {
        when {
            marks.isEmpty() -> {
                stop(editor)
            }

            (marks.size == 1 && (config.autoJumpWhenSingle || marks[0].hintMark)) -> {
                // For VimF mode, don't stop automatically - keep waiting for 'f' key
                if (currentMode.isVimMode()) {
                    // The VimF already jumped to the position, just keep search active
                    return
                }

                moveToOffset(editor, marks[0].offset)
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
    fun start(mode: Mode, anActionEvent: AnActionEvent) {
        if (isStart) return
        this.searchString = ""
        this.currentMode = mode
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

        setGrayColor(editor, true, mode);

        onJump = null
        finder = when (mode) {
            Mode.SEARCH -> Search()
            Mode.VIM_F -> VimF()
            Mode.VIM_F_BACKWARD -> VimF()
            Mode.VIM_T -> VimF()
            Mode.VIM_T_BACKWARD -> VimF()
            Mode.VIM_REPEAT -> VimF()
            Mode.VIM_REPEAT_BACKWARD -> VimF()
            else -> throw RuntimeException("Invalid start mode: $mode")
        }
        val marks = finder.start(editor, mode)
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
            // Clean up finder-specific resources like highlighters
            if (::finder.isInitialized) {
                finder.cleanup(editor)
            }
            // get editor and remove the gray highlighters
            setGrayColor(editor, false, Mode.SEARCH)
        }
        // 可选：清空 DataContext 引用
        lastDataContext = null
    }

    fun setGrayColor(editor: Editor, setGray: Boolean, mode: Mode = Mode.SEARCH) {
        if (setGray) {
            // Set gray color of text
            val startOffset: Int
            val endOffset: Int

            when (mode) {
                Mode.VIM_F, Mode.VIM_T -> {
                    // For vim f mode, gray out text after cursor
                    val cursorOffset = editor.caretModel.offset
                    startOffset = cursorOffset + 1
                    endOffset = editor.document.textLength
                }

                Mode.VIM_F_BACKWARD, Mode.VIM_T_BACKWARD -> {
                    // For vim F mode, gray out text before cursor
                    val cursorOffset = editor.caretModel.offset
                    startOffset = 0
                    endOffset = cursorOffset - 1
                }

                Mode.VIM_F_ALL, Mode.VIM_F_ALL_BACKWARD, Mode.VIM_T_ALL, Mode.VIM_T_ALL_BACKWARD -> {
                    // For vim f all mode, gray out all text
                    startOffset = 0
                    endOffset = editor.document.textLength
                }

                else -> {
                    // For search modes, gray out all visible text
                    val visibleArea = editor.scrollingModel.visibleArea
                    val startLogicalPosition = editor.xyToLogicalPosition(visibleArea.location)
                    val endLogicalPosition = editor.xyToLogicalPosition(
                        visibleArea.location.apply {
                            this.x += visibleArea.width
                            this.y += visibleArea.height
                        }
                    )
                    startOffset = editor.logicalPositionToOffset(startLogicalPosition)
                    endOffset = editor.logicalPositionToOffset(endLogicalPosition)
                }
            }

            if (startOffset < endOffset) {
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
            }

        } else {
            val markupModel = editor.markupModel
            // Remove each highlighter that was previously added
            highlighters.forEach { markupModel.removeHighlighter(it) }
            // Clear the list of highlighters after removal
            highlighters.clear()
        }
    }

    /**
     * 退出当前模式并把本次按键交回原始 typed handler 执行
     */
    fun stopAndDispatch(editor: Editor, c: Char) {
        val oldHandler = mOldTypedHandler
        val dc = lastDataContext
        // 正常停止（恢复所有 handler / 高亮 / 画布）
        stop(editor)
        // 透传按键
        if (oldHandler != null && dc != null) {
            oldHandler.execute(editor, c, dc)
        }
    }

    fun moveToOffset(editor: Editor, offset: Int) {
        val caret = editor.caretModel.currentCaret
        // If the caret has a selection, we need to adjust the selection to the new mark
        if (caret.hasSelection()) {
            val selectionStart =
                if ((caret.selectionStart == caret.offset) && (abs(caret.selectionStart - caret.selectionEnd) > 1))
                    // 选区开始位置从光标之后改为光标之前，保证选区开始位置的字符不变
                    if (caret.selectionEnd > caret.offset && caret.selectionEnd < offset)
                        caret.selectionEnd - 1
                    else
                        caret.selectionEnd
                else
                    // 选区开始位置从光标之前改为光标之后，保证选区开始位置的字符不变
                    if (caret.selectionStart < caret.offset && caret.selectionStart > offset)
                        caret.selectionStart + 1
                    else
                        caret.selectionStart

            val selectionEnd =
                if (((caret.selectionStart == caret.offset) && (abs(caret.selectionStart - caret.selectionEnd) > 1)) ||
                    (caret.selectionStart < caret.offset && caret.selectionStart > offset)
                )
                    offset
                else
                    offset + 1

            caret.setSelection(selectionStart, selectionEnd)
        }
        // Record the current command in the document history
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
        // Move the caret to the mark
        caret.moveToOffset(offset)
    }
}
