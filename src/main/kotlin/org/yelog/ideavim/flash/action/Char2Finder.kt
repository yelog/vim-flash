package com.github.yelog.ideavimflash.finder

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.UserConfig.DataBean
import org.yelog.ideavim.flash.utils.findAll
import kotlin.math.abs

private const val STATE_WAIT_SEARCH_CHAR1 = 0
private const val STATE_WAIT_SEARCH_CHAR2 = 1
private const val STATE_WAIT_KEY = 2

class Char2Finder : Finder {
    private var state = STATE_WAIT_SEARCH_CHAR1
    private val config: DataBean = UserConfig.getDataBean()
    private lateinit var s: String
    private lateinit var visibleRange: TextRange
    private var firstChar = ' '

    override fun start(e: Editor, s: String, visibleRange: TextRange): List<MarksCanvas.Mark>? {
        this.s = s
        this.visibleRange = visibleRange
        state = STATE_WAIT_SEARCH_CHAR1
        return null
    }

    override fun input(e: Editor, c: Char, lastMarks: List<MarksCanvas.Mark>): List<MarksCanvas.Mark>? {
        return when (state) {
            STATE_WAIT_SEARCH_CHAR1 -> {
                firstChar = c
                state = STATE_WAIT_SEARCH_CHAR2
                null
            }
            STATE_WAIT_SEARCH_CHAR2 -> {
                val caretOffset = e.caretModel.offset
                val find = "" + firstChar + c
                val offsets = s.findAll(find, find.all { it.isLowerCase() })
                    .map { it + visibleRange.startOffset }
                    .sortedBy { abs(it - caretOffset) }
                    .toList()

                val tags = KeyTagsGenerator.createTagsTree(offsets.size, config.characters)
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
