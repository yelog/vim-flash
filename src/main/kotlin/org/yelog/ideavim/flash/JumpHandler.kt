package org.yelog.ideavim.flash

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.util.Disposer
import org.yelog.ideavim.flash.action.Finder
import org.yelog.ideavim.flash.action.Search
import org.yelog.ideavim.flash.action.TreesitterFinder
import org.yelog.ideavim.flash.action.VimF
import org.yelog.ideavim.flash.utils.notify
import java.awt.Color
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import kotlin.math.abs


object JumpHandler : TypedActionHandler {
    private val config: UserConfig.DataBean by lazy { UserConfig.getDataBean() }
    private var mOldTypedHandler: TypedActionHandler? = null
    private var mOldEscActionHandler: EditorActionHandler? = null
    private var mOldBackSpaceActionHandler: EditorActionHandler? = null
    private var mOldEnterActionHandler: EditorActionHandler? = null
    private val canvasMap = mutableMapOf<Editor, MarksCanvas>()
    private var isStart = false
    private lateinit var finder: Finder
    private var currentMode = Mode.SEARCH
    private var onJump: (() -> Unit)? = null // Runnable that is called after jump
    private var lastMarks: List<MarksCanvas.Mark> = emptyList()
    private var remoteOriginOffset: Int = -1
    private var remoteOriginEditor: Editor? = null
    private var remoteAwaitingReturn: Boolean = false
    private var isCanvasAdded = false

    // 记录最近一次 DataContext，用于 stopAndDispatch 透传按键
    // --- remote offset 修正 ---
    private var remoteDocChangeShift: Int = 0
    private var remoteDocListenerAdded: Boolean = false
    private var remoteDocListenerDisposable: Disposable? = null
    private var lastDataContext: DataContext? = null
    private var remoteCommandListenerDisposable: Disposable? = null

    // Record the string being currently searched
    private var searchString = ""

    // 监听下一次命令结束后把光标恢复到原位置
    private val remoteCommandListener = object : CommandListener {
        override fun commandFinished(event: CommandEvent) {
            if (!remoteAwaitingReturn) return
            val editor = remoteOriginEditor
            if (editor != null && remoteOriginOffset >= 0) {
                val finalOffset = (remoteOriginOffset + remoteDocChangeShift).coerceAtLeast(0)
                val docLen = editor.document.textLength
                editor.caretModel.moveToOffset(finalOffset.coerceAtMost(docLen))
            }
            remoteOriginOffset = -1
            remoteOriginEditor = null
            remoteAwaitingReturn = false
            remoteDocChangeShift = 0
            if (remoteDocListenerAdded) {
                editor?.document?.let { detachRemoteDocumentListener(it) }
            }
            if (remoteCommandListenerDisposable != null) {
                try {
                    Disposer.dispose(remoteCommandListenerDisposable!!)
                } catch (_: Throwable) {
                } finally {
                    remoteCommandListenerDisposable = null
                }
            } else {
                unregisterCommandListener(this)
            }
        }
    }

    private val remoteDocListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            // 只在修改位置在原始光标之前时调整
            if (remoteAwaitingReturn && remoteOriginOffset >= 0) {
                if (event.offset < remoteOriginOffset) {
                    val diff = event.newLength - event.oldLength
                    remoteDocChangeShift += diff
                }
            }
        }
    }

    private fun attachRemoteDocumentListener(document: Document): Boolean {
        return try {
            remoteDocListenerDisposable = registerRemoteDocumentListener(document)
            true
        } catch (_: Throwable) {
            remoteDocListenerDisposable = null
            false
        }
    }

    private fun registerRemoteDocumentListener(document: Document): Disposable? {
        return try {
            val methodNew = document.javaClass.getMethod(
                "addDocumentListener",
                DocumentListener::class.java,
                Disposable::class.java
            )
            val disposable = Disposer.newDisposable("vim-flash-remote-doc")
            try {
                methodNew.invoke(document, remoteDocListener, disposable)
                disposable
            } catch (t: Throwable) {
                try {
                    Disposer.dispose(disposable)
                } catch (_: Throwable) {
                }
                throw t
            }
        } catch (_: NoSuchMethodException) {
            try {
                val methodOld = document.javaClass.getMethod("addDocumentListener", DocumentListener::class.java)
                methodOld.invoke(document, remoteDocListener)
            } catch (t: Throwable) {
                notify("addDocumentListener(old) error: ${t.message}")
                throw t
            }
            null
        } catch (t: Throwable) {
            notify("addDocumentListener(new) error: ${t.message}")
            throw t
        }
    }

    private fun detachRemoteDocumentListener(document: Document) {
        try {
            val disposable = remoteDocListenerDisposable
            if (disposable != null) {
                remoteDocListenerDisposable = null
                try {
                    Disposer.dispose(disposable)
                } catch (t: Throwable) {
                    notify("dispose document listener error: ${t.message}")
                }
                return
            }
            val method = document.javaClass.getMethod("removeDocumentListener", DocumentListener::class.java)
            method.invoke(document, remoteDocListener)
        } catch (_: NoSuchMethodException) {
            // Platforms that rely solely on parent disposables
        } catch (t: Throwable) {
            notify("removeDocumentListener error: ${t.message}")
        } finally {
            remoteDocListenerAdded = false
        }
    }

    /**
     * 通过反射安全调用 removeCommandListener，避免在旧平台 (无该方法) 上抛出 NoSuchMethodError
     */
    private fun unregisterCommandListener(listener: CommandListener) {
        val cp = CommandProcessor.getInstance()
        try {
            val method = cp.javaClass.getMethod("removeCommandListener", CommandListener::class.java)
            method.invoke(cp, listener)
        } catch (_: NoSuchMethodException) {
            // 旧版本平台无此方法，忽略（可能会有微小泄漏，但频率极低）
        } catch (t: Throwable) {
            notify("unregisterCommandListener error: ${t.message}")
        }
    }

    /**
     * 通过反射优先调用 addCommandListener(listener, disposable) 新签名；若不存在则回退旧签名。
     */
    private fun registerCommandListener(listener: CommandListener) {
        val cp = CommandProcessor.getInstance()
        try {
            val methodNew = cp.javaClass.getMethod(
                "addCommandListener",
                CommandListener::class.java,
                Disposable::class.java
            )
            val disposable = Disposer.newDisposable("vim-flash-remote-command")
            methodNew.invoke(cp, listener, disposable)
            remoteCommandListenerDisposable = disposable
            return
        } catch (_: NoSuchMethodException) {
            // ignore, fallback
        } catch (t: Throwable) {
            notify("registerCommandListener(new) error: ${t.message}")
        }

        // fallback old
        try {
            val oldMethod = cp.javaClass.getMethod("addCommandListener", CommandListener::class.java)
            oldMethod.invoke(cp, listener)
        } catch (t: Throwable) {
            notify("registerCommandListener(old) error: ${t.message}")
        }
    }

    // Map: 每个编辑器对应其灰色高亮列表
    private val highlightersMap = mutableMapOf<Editor, MutableList<RangeHighlighter>>()

    override fun execute(e: Editor, c: Char, dc: DataContext) {
        // 保存 DataContext，便于后续透传
        lastDataContext = dc

        // Treesitter 模式不累加 searchString，以便非 label 按键直接透传
        if (currentMode != Mode.TREESITTER) {
            this.searchString += c
        }

        val previousMarks = lastMarks
        val marks = finder.input(e, c, lastMarks, searchString)

        // Treesitter 模式：输入的按键不是任何 label，则直接退出并透传该按键
        if (currentMode == Mode.TREESITTER && marks != null && marks === previousMarks) {
            stopAndDispatch(e, c)
            return
        }

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
                val targetEditor = marks[0].sourceEditor ?: editor

                // Remote 模式
                if (currentMode == Mode.REMOTE) {
                    val origin = remoteOriginOffset
                    moveToOffset(targetEditor, marks[0].offset)
                    stop(targetEditor)
                    if (origin >= 0) {
                        remoteAwaitingReturn = true
                        remoteOriginOffset = origin
                        remoteOriginEditor = targetEditor
                        remoteDocChangeShift = 0
                        if (!remoteDocListenerAdded) {
                            remoteDocListenerAdded = attachRemoteDocumentListener(targetEditor.document)
                        }
                        registerCommandListener(remoteCommandListener)
                    }
                    onJump?.invoke()
                    return
                }

                // Treesitter 范围
                if (currentMode == Mode.TREESITTER && marks[0].rangeEnd > marks[0].offset) {
                    val caret = targetEditor.caretModel.currentCaret
                    caret.removeSelection()
                    caret.setSelection(marks[0].offset, marks[0].rangeEnd)
                    caret.moveToOffset(marks[0].offset)
                    stop(targetEditor)
                    onJump?.invoke()
                    return
                }

                // VimF 保持
                if (currentMode.isVimMode()) {
                    return
                }

                moveToOffset(targetEditor, marks[0].offset)
                targetEditor.contentComponent.requestFocus()
                stop(targetEditor)
                onJump?.invoke()
            }

            else -> {
                // 跨分屏显示
                if (currentMode == Mode.SEARCH && config.searchAcrossSplits && marks.any { it.sourceEditor != null }) {
                    val grouped = marks.groupBy { it.sourceEditor ?: editor }

                    // 需要显示的编辑器集合
                    val editorsWithMarks = grouped.keys

                    // 移除已无匹配的分屏画布（否则残留上一次的标签）
                    val toRemove = canvasMap.filterKeys { it !in editorsWithMarks }.toList()
                    toRemove.forEach { (ed, canvas) ->
                        val parent = canvas.parent
                        if (parent != null) {
                            parent.remove(canvas)
                            parent.repaint()
                        }
                        canvasMap.remove(ed)
                    }

                    // 为每个有匹配的编辑器创建或更新 canvas
                    grouped.forEach { (ed, ms) ->
                        val canvas = canvasMap.getOrPut(ed) {
                            MarksCanvas().apply {
                                sync(ed)
                                ed.contentComponent.add(this)
                            }
                        }
                        canvas.sync(ed)
                        canvas.setData(ms, this.searchString)
                        ed.contentComponent.repaint()
                    }
                } else {
                    // 旧逻辑：只在当前编辑器添加单画布
                    val canvas = canvasMap.getOrPut(editor) { MarksCanvas().apply { sync(editor); editor.contentComponent.add(this) } }
                    canvas.sync(editor)
                    canvas.setData(marks, this.searchString)
                    editor.contentComponent.repaint()
                }
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
        remoteOriginOffset = -1
        val editor = anActionEvent.getData(CommonDataKeys.EDITOR) ?: return
        if (mode == Mode.REMOTE) {
            remoteOriginOffset = editor.caretModel.offset
            remoteOriginEditor = editor
            remoteAwaitingReturn = false
        }
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
            Mode.TREESITTER -> TreesitterFinder()
            Mode.REMOTE -> Search()
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
            // 移除所有画布
            canvasMap.forEach { (ed, canvas) ->
                val parent = canvas.parent
                if (parent != null) {
                    parent.remove(canvas)
                    parent.repaint()
                }
            }
            canvasMap.clear()
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
        val cfg = config
        if (setGray) {
            // 需要处理的编辑器集合：搜索模式并且开启跨分屏时，灰显所有 TextEditor；否则仅当前 editor
            val editors: List<Editor> =
                if (mode == Mode.SEARCH && cfg.searchAcrossSplits) {
                    val project = editor.project
                    if (project != null) {
                        FileEditorManager.getInstance(project).allEditors
                            .filterIsInstance<TextEditor>()
                            .map { it.editor }
                            .ifEmpty { listOf(editor) }
                    } else {
                        listOf(editor)
                    }
                } else {
                    listOf(editor)
                }

            editors.forEach { ed ->
                val (startOffset, endOffset) = when (mode) {
                    Mode.VIM_F, Mode.VIM_T -> {
                        val cursorOffset = ed.caretModel.offset
                        cursorOffset + 1 to ed.document.textLength
                    }
                    Mode.VIM_F_BACKWARD, Mode.VIM_T_BACKWARD -> {
                        val cursorOffset = ed.caretModel.offset
                        0 to (cursorOffset - 1).coerceAtLeast(0)
                    }
                    Mode.VIM_F_ALL, Mode.VIM_F_ALL_BACKWARD, Mode.VIM_T_ALL, Mode.VIM_T_ALL_BACKWARD -> {
                        0 to ed.document.textLength
                    }
                    else -> {
                        val visibleArea = ed.scrollingModel.visibleArea
                        val startLogicalPosition = ed.xyToLogicalPosition(visibleArea.location)
                        val endLogicalPosition = ed.xyToLogicalPosition(
                            visibleArea.location.apply {
                                this.x += visibleArea.width
                                this.y += visibleArea.height
                            }
                        )
                        ed.logicalPositionToOffset(startLogicalPosition) to ed.logicalPositionToOffset(endLogicalPosition)
                    }
                }

                if (startOffset < endOffset) {
                    val grayAttributes = TextAttributes().apply {
                        foregroundColor = Color.GRAY
                    }
                    val markupModel = ed.markupModel
                    val highlighter = markupModel.addRangeHighlighter(
                        startOffset,
                        endOffset,
                        HighlighterLayer.SELECTION - 1,
                        grayAttributes,
                        HighlighterTargetArea.EXACT_RANGE
                    )
                    val list = highlightersMap.getOrPut(ed) { mutableListOf() }
                    list.add(highlighter)
                }
            }
        } else {
            // 移除所有相关编辑器的灰显（跨分屏时全部移除）
            val editors = if (mode == Mode.SEARCH && cfg.searchAcrossSplits) {
                highlightersMap.keys.toList()
            } else {
                listOf(editor)
            }
            editors.forEach { ed ->
                highlightersMap.remove(ed)?.let { list ->
                    val markupModel = ed.markupModel
                    list.forEach { markupModel.removeHighlighter(it) }
                }
            }
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
