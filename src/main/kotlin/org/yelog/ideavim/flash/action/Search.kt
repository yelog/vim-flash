package org.yelog.ideavim.flash.action

import ai.grazie.utils.findAllMatches
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.findAll
import java.util.ArrayList
import kotlin.math.abs


class Search : Finder {
    private lateinit var visibleString: String
    private lateinit var visibleRange: TextRange
    private var searchString = ""

    override fun start(e: Editor, visibleString: String, visibleRange: TextRange): List<MarksCanvas.Mark>? {
        this.visibleString = visibleString
        this.visibleRange = visibleRange
        this.searchString = ""
        return null
    }

    override fun input(e: Editor, c: Char, lastMarks: List<MarksCanvas.Mark>): List<MarksCanvas.Mark> {
        if (lastMarks.any { it.keyTag.contains(c) }) {
            // hit a tag
            return advanceMarks(c, lastMarks);
        } else {
            // keep on searching
            this.searchString += c
            val caretOffset = e.caretModel.offset
            val offsets = visibleString.findAll(this.searchString, this.searchString.all { it.isLowerCase() })
                .map { it + visibleRange.startOffset }
                .sortedBy { abs(it - caretOffset) }
                .toList()

            val nextCharList = Regex("(?<="+this.searchString+")(.)", RegexOption.IGNORE_CASE).findAllMatches(this.visibleString).map { it.value }.distinct()
            var remainCharacter = UserConfig.getDataBean().characters
            for (s in nextCharList) {
                remainCharacter = remainCharacter.replace(s, "")
            }
            val tags = KeyTagsGenerator.createTagsTree(offsets.size, remainCharacter)
            return offsets.zip(tags)
                .map { MarksCanvas.Mark(it.second, it.first, this.searchString.length) }
                .toList()
        }
    }
}
