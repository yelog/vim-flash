package com.github.yelog.ideavimflash.finder

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.findAll
import kotlin.math.abs

private const val STATE_WAIT_SEARCH_CHAR = 0
private const val STATE_WAIT_KEY = 1

class Search : Finder {
    private var state = STATE_WAIT_SEARCH_CHAR
    private lateinit var s: String
    private lateinit var visibleRange: TextRange

    override fun start(e: Editor, s: String, visibleRange: TextRange): List<MarksCanvas.Mark>? {
        this.s = s
        this.visibleRange = visibleRange
        state = STATE_WAIT_SEARCH_CHAR
        return null
    }

    override fun input(e: Editor, c: Char, lastMarks: List<MarksCanvas.Mark>): List<MarksCanvas.Mark> {
        return when (state) {
            STATE_WAIT_SEARCH_CHAR -> {
                val caretOffset = e.caretModel.offset
                val offsets = s.findAll(c, c.isLowerCase())
                    .map { it + visibleRange.startOffset }
                    .sortedBy { abs(it - caretOffset) }
                    .toList()

                val tags = KeyTagsGenerator.createTagsTree(offsets.size, UserConfig.getDataBean().characters)
                state = STATE_WAIT_KEY
                offsets.zip(tags)
                    .map { MarksCanvas.Mark(it.second, it.first) }
                    .toList()
            }
            STATE_WAIT_KEY -> advanceMarks(c, lastMarks)
            else -> throw RuntimeException("Impossible.")
        }
    }
}
