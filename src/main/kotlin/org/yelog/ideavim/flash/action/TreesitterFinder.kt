package org.yelog.ideavim.flash.action

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.Mode
import kotlin.math.min

/**
 * 模拟 flash.nvim treesitter() 功能：
 *  - 初次进入直接展示从光标所在最内层 PSI 元素向外扩展的语法节点范围
 *  - 每个范围的首尾显示同一标签（a-z）
 *  - 按下对应标签后选中该范围并退出
 */
class TreesitterFinder : Finder {

    private var marks: List<MarksCanvas.Mark> = emptyList()

    override fun start(e: Editor, mode: Mode): List<MarksCanvas.Mark>? {
        val project = e.project ?: return emptyList()
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(e.document) ?: return emptyList()

        var offset = e.caretModel.offset
        if (offset >= e.document.textLength && e.document.textLength > 0) {
            offset = e.document.textLength - 1
        }
        if (offset < 0) return emptyList()

        var element = psiFile.findElementAt(offset)
        if (element == null && offset > 0) {
            element = psiFile.findElementAt(offset - 1)
        }
        if (element == null) return emptyList()

        val ranges = mutableListOf<Pair<Int, Int>>() // (start, endExclusive)
        val seen = HashSet<String>()
        while (element != null) {
            val tr = element.textRange
            if (tr != null && tr.length > 0) {
                val key = "${tr.startOffset}:${tr.endOffset}"
                if (!seen.contains(key)) {
                    // 过滤掉纯空白
                    val text = element.text
                    if (text != null && text.any { !it.isWhitespace() }) {
                        ranges.add(tr.startOffset to tr.endOffset)
                        seen.add(key)
                    }
                }
            }
            element = element.parent
        }
        if (ranges.isEmpty()) return emptyList()

        // 从内到外，限制层数（去掉 y/c/d/x/s 以避免与常用操作命令冲突）
        val letters = ('a'..'z').filter { it !in setOf('y', 'c', 'd', 'x', 's') }
        val limited = ranges.take(min(letters.size, ranges.size))

        marks = limited.mapIndexed { index, (start, end) ->
            MarksCanvas.Mark(
                keyTag = letters[index].toString(),
                offset = start,
                charLength = 0,
                advanceIndex = 0,
                hintMark = false,
                rangeEnd = end
            )
        }

        // 触发后立即选中最小范围（第一个/最内层）
        if (marks.isNotEmpty()) {
            val first = marks[0]
            if (first.rangeEnd > first.offset) {
                val caret = e.caretModel.currentCaret
                caret.removeSelection()
                caret.setSelection(first.offset, first.rangeEnd)
                caret.moveToOffset(first.offset)
            }
        }

        return marks
    }

    override fun input(
        e: Editor,
        c: Char,
        lastMarks: List<MarksCanvas.Mark>,
        searchString: String
    ): List<MarksCanvas.Mark>? {
        // 查找匹配标签
        val found = lastMarks.firstOrNull { it.keyTag.isNotEmpty() && it.keyTag[0].equals(c, ignoreCase = true) }
        return if (found != null) {
            listOf(
                MarksCanvas.Mark(
                    keyTag = found.keyTag,
                    offset = found.offset,
                    charLength = 0,
                    advanceIndex = 0,
                    hintMark = true,
                    rangeEnd = found.rangeEnd
                )
            )
        } else {
            // 不匹配就继续等待
            lastMarks
        }
    }
}
