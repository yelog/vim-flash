package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.JumpHandler
import org.yelog.ideavim.flash.JumpHandler.setGrayColor
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.Mode
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.getVisibleRangeOffset
import java.awt.Color

class VimF : Finder {
    // Record the string of the entire document
    private lateinit var documentText: String

    // Record the position of the visible area relative to the beginning of the document
    private lateinit var visibleRange: TextRange

    private var mode: Mode = Mode.VIM_F

    // Current cursor position
    private var cursorOffset: Int = 0

    // Target character to search for
    private var targetChar: Char? = null

    // All match positions for the target character (after cursor only)
    private var allMatches: List<Int> = emptyList()

    // Current match index
    private var currentMatchIndex: Int = -1

    // --- For repeat mode support ---
    companion object {
        private var lastTargetChar: Char? = null
        private var lastMode: Mode = Mode.VIM_F
    }

    private fun getMode(): Mode {
        return if (mode.isRepeat()) {
            lastMode
        } else {
            mode
        }
    }

    // List to keep track of character highlighters
    private val charHighlighters = mutableListOf<RangeHighlighter>()

    // Config instance
    private val config: UserConfig.DataBean by lazy { UserConfig.getDataBean() }


    override fun start(e: Editor, mode: Mode): List<MarksCanvas.Mark>? {
        this.documentText = e.document.text
        this.visibleRange = e.getVisibleRangeOffset()
        this.cursorOffset = e.caretModel.offset
        this.targetChar = null
        this.allMatches = emptyList()
        this.currentMatchIndex = -1
        this.mode = mode
        clearCharHighlighters(e)

        // If repeat mode, restore last mode and targetChar
        if (mode.isRepeat()) {
            lastMode = lastMode.toNotAll()
            setGrayColor(e, true, getMode())
            if (lastTargetChar != null) {
                this.targetChar = lastTargetChar
                paintMarksAndJumpNext(e)
            }
        }

        return null
    }

    override fun input(
        e: Editor,
        c: Char,
        lastMarks: List<MarksCanvas.Mark>,
        searchString: String
    ): List<MarksCanvas.Mark>? {
        visibleRange = e.getVisibleRangeOffset()

        // If it's the 'f' key for continuing to next match
        if (getMode().matchChar(c) && targetChar != null && allMatches.isNotEmpty()) {
            // 反向查找时
            if (Mode.isBackward(c)) {
                if (currentMatchIndex - 1 < 0) {
                    if (getMode().isVimNotAll()) {
                        if (mode.isRepeat()) {
                            lastMode = lastMode.toAll()
                        } else {
                            mode = mode.toAll()
                        }
                        val otherMatches = findMatchesInDocument(targetChar!!)
                        if (otherMatches.isNotEmpty()) {
                            setGrayColor(e, true, getMode())
                            allMatches = otherMatches + allMatches
                            currentMatchIndex = otherMatches.size - 1
                            highlightCharacters(e, allMatches)
                        }
                    }
                } else {
                    currentMatchIndex--
                }
            } else {
                currentMatchIndex = (currentMatchIndex + 1).coerceAtMost(allMatches.size - 1)
            }
            val nextOffset = allMatches[currentMatchIndex]
            // Calculate if the caret's line is outside the visible area (vertically)
            JumpHandler.moveToOffset(e, nextOffset + getMode().getOffset())
//            val caretOffset = e.caretModel.offset

            val scrolloff = config.scrolloff
            val caretLine = e.caretModel.logicalPosition.line
            val visibleStartLine = e.offsetToLogicalPosition(visibleRange.startOffset).line
            val visibleEndLine = e.offsetToLogicalPosition(visibleRange.endOffset).line

            val shouldScrollDown = caretLine >= visibleEndLine - scrolloff + 1
            val shouldScrollUp = caretLine <= visibleStartLine + scrolloff - 1
            val shouldScroll = shouldScrollDown || shouldScrollUp
//            val isCaretNotVisible = caretOffset < visibleRange.startOffset || caretOffset > visibleRange.endOffset

            if (shouldScroll) {
                // 将当前光标所在行滚动到可视区域的最后一行
                val caretLine = e.caretModel.logicalPosition.line
                val targetY = e.logicalPositionToXY(com.intellij.openapi.editor.LogicalPosition(caretLine, 0)).y
                val visibleHeight = e.scrollingModel.visibleArea.height
                // 计算出滚动条的绝对位置。-1 是为了防止可视区域包含看不见的下一行
                val scrollY = targetY - visibleHeight + e.lineHeight * (scrolloff + 1) - 1
                // 设置滚动条位置
                e.scrollingModel.scrollVertically(scrollY.coerceAtLeast(0))
            }
            // Return single mark to indicate completion but keep search active
            return listOf(MarksCanvas.Mark("", nextOffset, 1))
        }

        if (lastMarks.any { it.keyTag.contains(c) }) {
            // Hit a tag - advance marks
            return advanceMarks(c, lastMarks)
        } else {
            // First character input - this becomes our target character
            if (targetChar == null) {
                targetChar = c
                // Save for repeat
                lastTargetChar = c
                lastMode = getMode()
                return paintMarksAndJumpNext(e)
            }

            // If we get here, it means an unexpected character was typed
            // Clear highlights and end the search
            clearCharHighlighters(e)
            return emptyList()
        }
    }

    private fun paintMarksAndJumpNext(e: Editor): List<MarksCanvas.Mark>? {
        // Search in entire document after cursor position
        allMatches = findMatchesInDocument(targetChar!!)

        if (allMatches.isEmpty()) {
            // No matches found
            return emptyList()
        }

        // Highlight all visible matches with configured colors
        highlightCharacters(e, allMatches)

        // Jump to the closest match immediately
        currentMatchIndex = 0
        val closestOffset = allMatches[0]
        val isNotInVisible = closestOffset < visibleRange.startOffset || closestOffset > visibleRange.endOffset
        JumpHandler.moveToOffset(e, closestOffset + getMode().getOffset())
        if (isNotInVisible) {
            // Scroll the file so that the line with the caret is center in the visible area
            e.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
        }

        // Return single mark to indicate completion but keep finder active for 'f' repeats
        return listOf(MarksCanvas.Mark("", closestOffset, 1))
    }

    private fun findMatchesInDocument(c: Char): List<Int> {
        val matches = mutableListOf<Int>()

        // For lowercase letters, search case-insensitively
        val searchChars = if (c.isLowerCase()) {
            listOf(c, c.uppercaseChar())
        } else {
            listOf(c)
        }
        val indexRange = when (getMode()) {
            Mode.VIM_F, Mode.VIM_T -> (cursorOffset + 1) until documentText.length
            Mode.VIM_F_ALL, Mode.VIM_T_ALL -> 0 until cursorOffset
            Mode.VIM_F_BACKWARD, Mode.VIM_T_BACKWARD -> (cursorOffset - 1) downTo 0
            Mode.VIM_F_ALL_BACKWARD, Mode.VIM_T_ALL_BACKWARD -> documentText.length - 1 downTo cursorOffset
            else -> return emptyList() // Should not happen
        }

        indexRange
            .filter { i -> searchChars.contains(documentText[i]) }
            .filterNot { i ->
                (getMode().isTillBefore() && (i == 0 || documentText[i - 1] == '\n')) ||
                        (getMode().isTillAfter() && (i == documentText.length - 1 || documentText[i + 1] == '\n'))
            }
            .forEach { matches.add(it) }
        return matches
    }

    private fun highlightCharacters(editor: Editor, offsets: List<Int>) {
        clearCharHighlighters(editor)

        // Use configured colors
        val highlightAttributes = TextAttributes().apply {
            backgroundColor = Color(config.labelBg, true)
            foregroundColor = Color(config.labelFg, true)
        }

        val placeholderAttributes = TextAttributes().apply {
            backgroundColor = Color(config.matchBg, true)
            foregroundColor = Color(config.matchFg, true)
        }

        val markupModel = editor.markupModel

        for (offset in offsets) {
            if (getMode().isTill()) {
                val tillOffset = getMode().isTillBefore().let {
                    if (it) -1 else 1
                }
                val highlighter = markupModel.addRangeHighlighter(
                    offset + tillOffset,
                    offset + 1 + tillOffset,
                    HighlighterLayer.SELECTION + 1,
                    placeholderAttributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
                charHighlighters.add(highlighter)
            }
            val highlighter = markupModel.addRangeHighlighter(
                offset,
                offset + 1,
                HighlighterLayer.SELECTION + 1,
                highlightAttributes,
                HighlighterTargetArea.EXACT_RANGE
            )
            charHighlighters.add(highlighter)
        }
    }

    override fun cleanup(e: Editor) {
        clearCharHighlighters(e)
    }

    private fun clearCharHighlighters(editor: Editor) {
        val markupModel = editor.markupModel
        charHighlighters.forEach { markupModel.removeHighlighter(it) }
        charHighlighters.clear()
    }
}
