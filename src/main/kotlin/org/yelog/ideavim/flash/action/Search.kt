package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.findAll
import kotlin.math.abs


class Search : Finder {
    // 记录当前可视区域的字符串
    private lateinit var visibleString: String

    // 记录可视区域的相对文档开头的位置
    private lateinit var visibleRange: TextRange

    override fun start(e: Editor, visibleString: String, visibleRange: TextRange): List<MarksCanvas.Mark>? {
        this.visibleString = visibleString
        this.visibleRange = visibleRange
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
