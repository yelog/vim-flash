package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.yelog.ideavim.flash.KeyTagsGenerator
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.Mode
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.utils.findAll
import org.yelog.ideavim.flash.utils.getVisibleRangeOffset
import kotlin.math.abs


class Search : Finder {
    // 单编辑器模式下
    private lateinit var visibleString: String
    private lateinit var visibleRange: TextRange

    // 记录发起搜索的编辑器与光标，用于全局最近匹配判定
    private var originEditor: Editor? = null
    private var originCaretOffset: Int = 0

    // 跨分屏支持
    private data class SplitInfo(
        val editor: Editor,
        val visibleRange: TextRange,
        val visibleString: String
    )
    private var splits: List<SplitInfo> = emptyList()

    override fun start(e: Editor, mode: Mode): List<MarksCanvas.Mark>? {
        val config = UserConfig.getDataBean()
        originEditor = e
        originCaretOffset = e.caretModel.offset
        if (config.searchAcrossSplits) {
            val project = e.project
            if (project != null) {
                val selectedEditors = FileEditorManager.getInstance(project).selectedEditors
                splits = selectedEditors
                    .filterIsInstance<TextEditor>()
                    .map { textEd ->
                        val ed = textEd.editor
                        val vr = ed.getVisibleRangeOffset()
                        SplitInfo(ed, vr, ed.document.getText(vr))
                    }
                if (splits.none { it.editor == e }) {
                    val vr = e.getVisibleRangeOffset()
                    splits = splits + SplitInfo(e, vr, e.document.getText(vr))
                }
            } else {
                splits = listOf(SplitInfo(e, e.getVisibleRangeOffset(), e.document.getText(e.getVisibleRangeOffset())))
            }
        } else {
            this.visibleRange = e.getVisibleRangeOffset()
            this.visibleString = e.document.getText(this.visibleRange)
            splits = emptyList()
        }
        return null
    }

    override fun input(
        e: Editor,
        c: Char,
        lastMarks: List<MarksCanvas.Mark>,
        searchString: String
    ): List<MarksCanvas.Mark> {
        if (lastMarks.any { it.keyTag.contains(c) }) {
            // 命中标签
            return advanceMarks(c, lastMarks);
        } else {
            val ignoreCase = !searchString.contains(Regex("[A-Z]"))
            val dataBean = UserConfig.getDataBean()
            val allMatches = mutableListOf<Triple<Editor, Int, String>>() // editor, offset, visibleString

            if (splits.isNotEmpty()) {
                // 跨分屏模式
                for (info in splits) {
                    val caretOffset = if (info.editor == e) e.caretModel.offset else info.editor.caretModel.offset
                    val offsets = info.visibleString.findAll(searchString, ignoreCase)
                        .map { it + info.visibleRange.startOffset }
                        .sortedBy { abs(it - caretOffset) }
                    offsets.forEach { off ->
                        allMatches += Triple(info.editor, off, info.visibleString)
                    }
                }
            } else {
                // 单编辑器
                val caretOffset = e.caretModel.offset
                val offsets = visibleString.findAll(searchString, ignoreCase)
                    .map { it + visibleRange.startOffset }
                    .sortedBy { abs(it - caretOffset) }
                offsets.forEach { off ->
                    allMatches += Triple(e, off, visibleString)
                }
            }

            if (allMatches.isEmpty()) return emptyList()

            // 计算下一字符集合（从对应编辑器的可视字符串中取得）
            var remainCharacter = dataBean.characters
            val nextCharSet = HashSet<Char>()
            for ((editor2, offset2, visStr) in allMatches) {
                val localIndex = offset2 - (if (splits.isNotEmpty()) {
                    splits.first { it.editor == editor2 }.visibleRange.startOffset
                } else visibleRange.startOffset) + searchString.length
                if (localIndex in visStr.indices) {
                    val ch = visStr[localIndex]
                    nextCharSet.add(ch)
                }
            }
            for (ch in nextCharSet) {
                remainCharacter = remainCharacter.replace(ch.toString(), "", true)
            }

            val tags = KeyTagsGenerator.createTagsTree(allMatches.size, remainCharacter)

            // 计算最近匹配：始终优先当前分屏（发起搜索的编辑器）
            val originMatches = allMatches.filter { it.first == originEditor }
            val nearestTriple = if (originMatches.isNotEmpty()) {
                originMatches.minByOrNull { (_, off, _) ->
                    kotlin.math.abs(off - originCaretOffset)
                }
            } else {
                allMatches.minByOrNull { (ed, off, _) ->
                    kotlin.math.abs(off - ed.caretModel.offset)
                }
            }

            return allMatches.zip(tags).map { (triple, tag) ->
                MarksCanvas.Mark(
                    keyTag = tag,
                    offset = triple.second,
                    charLength = searchString.length,
                    sourceEditor = triple.first,
                    isNearest = (triple == nearestTriple)
                )
            }
        }
    }
}
