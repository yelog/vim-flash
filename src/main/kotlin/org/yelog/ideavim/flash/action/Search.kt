package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.findAll
import org.yelog.ideavim.flash.utils.getVisibleRangeOffset
import kotlin.math.abs


class Search : Finder {
    // Record the string of the current visible area
    private lateinit var visibleString: String

    // Record the position of the visible area relative to the beginning of the document
    private lateinit var visibleRange: TextRange

    override fun start(e: Editor, mode: Int): List<MarksCanvas.Mark>? {
        this.visibleRange = e.getVisibleRangeOffset()
        this.visibleString = e.document.getText(this.visibleRange)
        return null
    }

    override fun input(
        e: Editor,
        c: Char,
        lastMarks: List<MarksCanvas.Mark>,
        searchString: String
    ): List<MarksCanvas.Mark> {
        if (lastMarks.any { it.keyTag.contains(c) }) {
            // hit a tag
            return advanceMarks(c, lastMarks);
        } else {
            // keep on searching
            val caretOffset = e.caretModel.offset
            val offsets = visibleString.findAll(searchString, !searchString.contains(Regex("[A-Z]")))
                .map { it + visibleRange.startOffset }
                .sortedBy { abs(it - caretOffset) }
                .toList()

            val nextCharList =
                offsets.map { this.visibleString[it - visibleRange.startOffset + searchString.length] }.distinct()
            var remainCharacter = UserConfig.getDataBean().characters
            for (s in nextCharList) {
                remainCharacter = remainCharacter.replace(s.toString(), "", true)
            }
            val tags = KeyTagsGenerator.createTagsTree(offsets.size, remainCharacter)
            return offsets.zip(tags)
                .map { MarksCanvas.Mark(it.second, it.first, searchString.length) }
                .toList()
        }
    }
}
