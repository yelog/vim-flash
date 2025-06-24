package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import java.awt.Color
import kotlin.math.abs

class VimF : Finder {
    // Record the string of the entire document
    private lateinit var documentText: String

    // Record the position of the visible area relative to the beginning of the document
    private lateinit var visibleRange: TextRange
    
    // Current cursor position
    private var cursorOffset: Int = 0
    
    // Target character to search for
    private var targetChar: Char? = null
    
    // All match positions for the target character (after cursor only)
    private var allMatches: List<Int> = emptyList()
    
    // Current match index
    private var currentMatchIndex: Int = -1
    
    // List to keep track of character highlighters
    private val charHighlighters = mutableListOf<RangeHighlighter>()
    
    // Config instance
    private val config: UserConfig.DataBean by lazy { UserConfig.getDataBean() }

    override fun start(e: Editor, visibleString: String, visibleRange: TextRange): List<MarksCanvas.Mark>? {
        this.documentText = e.document.text
        this.visibleRange = visibleRange
        this.cursorOffset = e.caretModel.offset
        this.targetChar = null
        this.allMatches = emptyList()
        this.currentMatchIndex = -1
        clearCharHighlighters(e)
        return null
    }

    override fun input(
        e: Editor,
        c: Char,
        lastMarks: List<MarksCanvas.Mark>,
        searchString: String
    ): List<MarksCanvas.Mark>? {
        
        // If it's the 'f' key for continuing to next match
        if (c == 'f' && targetChar != null && allMatches.isNotEmpty()) {
            // Move to next match
            currentMatchIndex = (currentMatchIndex + 1) % allMatches.size
            val nextOffset = allMatches[currentMatchIndex]
            e.caretModel.currentCaret.moveToOffset(nextOffset)
            
            // Update cursor position for future searches
            this.cursorOffset = nextOffset
            
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
                
                // Search in entire document after cursor position
                allMatches = findMatchesInDocument(c)
                
                if (allMatches.isEmpty()) {
                    // No matches found
                    return emptyList()
                }
                
                // Highlight all visible matches with configured colors
                highlightCharacters(e, allMatches)
                
                // Jump to the closest match immediately
                currentMatchIndex = 0
                val closestOffset = allMatches[0]
                e.caretModel.currentCaret.moveToOffset(closestOffset)
                
                // Update cursor position for future searches
                this.cursorOffset = closestOffset
                
                // Return single mark to indicate completion but keep finder active for 'f' repeats
                return listOf(MarksCanvas.Mark("", closestOffset, 1))
            }
            
            // If we get here, it means an unexpected character was typed
            // Clear highlights and end the search
            clearCharHighlighters(e)
            return emptyList()
        }
    }
    
    private fun findMatchesInDocument(c: Char): List<Int> {
        val matches = mutableListOf<Int>()
        
        // For lowercase letters, search case-insensitively
        val searchChars = if (c.isLowerCase()) {
            listOf(c, c.uppercaseChar())
        } else {
            listOf(c)
        }
        
        // Search only after cursor position (exclusive)
        for (i in (cursorOffset + 1) until documentText.length) {
            if (searchChars.contains(documentText[i])) {
                matches.add(i)
            }
        }
        
        return matches
    }
    
    private fun highlightCharacters(editor: Editor, offsets: List<Int>) {
        clearCharHighlighters(editor)
        
        // Use configured colors
        val highlightAttributes = TextAttributes().apply {
            backgroundColor = Color(config.labelBg, true)
            foregroundColor = Color(config.labelFg, true)
        }
        
        val markupModel = editor.markupModel
        
        // Only highlight matches that are visible in the current viewport
        val visibleStart = visibleRange.startOffset
        val visibleEnd = visibleRange.endOffset
        
        for (offset in offsets) {
            if (offset >= visibleStart && offset < visibleEnd) {
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
