package org.yelog.ideavim.flash

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import org.yelog.ideavim.flash.utils.offsetToXYCompat
import java.awt.*
import javax.swing.JComponent

class MarksCanvas : JComponent() {
    private val config: UserConfig.DataBean by lazy { UserConfig.getDataBean() }
    private var mMarks: List<Mark> = emptyList()
    private lateinit var mEditor: Editor
    private lateinit var mFont: Font
    private lateinit var mFontMetrics: FontMetrics
    private var searchString = ""

    fun sync(e: Editor) {
        val visibleArea = e.scrollingModel.visibleArea
        setBounds(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height)
        mEditor = e
        // 获取编辑器原始文本字体
        mFont = e.colorsScheme.getFont(EditorFontType.PLAIN)
        mFontMetrics = e.contentComponent.getFontMetrics(mFont)
    }

    fun setData(marks: List<Mark>, searchString: String) {
        this.mMarks = marks
        this.searchString = searchString
        repaint()
    }

    override fun paint(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.font = mFont
        g2d.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        // Calculate the starting coordinate position, used for sorting all coordinates
        val coordinates = mMarks
            .map { mEditor.offsetToXYCompat(it.offset) }
            .toList()

        mMarks.zip(coordinates)
            .sortedBy { it.second.x }
            .forEach {
                val mark = it.first
                // 语法范围标签（无字符高亮，只在首尾画标签）
                if (mark.charLength == 0 && mark.rangeEnd > mark.offset) {
                    val keyTag = mark.keyTag
                    val bounds = mFontMetrics.getStringBounds(keyTag, g).bounds
                    val globalLineHeight = mEditor.lineHeight

                    // 起点标签
                    val startXY = mEditor.offsetToXYCompat(mark.offset)
                    val startYBase = startXY.y - y + (globalLineHeight - bounds.height) / 2
                    g2d.color = Color(config.labelBg, true)
                    g2d.fillRect(
                        startXY.x - x,
                        startYBase,
                        bounds.width,
                        bounds.height
                    )
                    g2d.color = Color(config.labelFg, true)
                    g2d.drawString(
                        keyTag,
                        startXY.x - x,
                        startYBase + mFontMetrics.ascent
                    )

                    // 终点标签（放在 rangeEnd - 1 字符处）
                    val endPos = (mark.rangeEnd - 1).coerceAtLeast(mark.offset)
                    val endXY = mEditor.offsetToXYCompat(endPos)
                    val endYBase = endXY.y - y + (globalLineHeight - bounds.height) / 2
                    g2d.color = Color(config.labelBg, true)
                    g2d.fillRect(
                        endXY.x - x,
                        endYBase,
                        bounds.width,
                        bounds.height
                    )
                    g2d.color = Color(config.labelFg, true)
                    g2d.drawString(
                        keyTag,
                        endXY.x - x,
                        endYBase + mFontMetrics.ascent
                    )
                    return@forEach
                }

                val keyTag = it.first.keyTag
                val charBounds = mFontMetrics.getStringBounds("x", g).bounds
                val bounds = mFontMetrics.getStringBounds(keyTag, g).bounds
                // draw match text background
                // Render each query character individually to prevent soft-wrap line breaks
                val globalLineHeight = mEditor.lineHeight
                for (i in 0 until it.first.charLength) {
                    mEditor.offsetToXYCompat(it.first.offset + i).let { offset ->
                        // 获取字符实际位置
                        val document = mEditor.document
                        val offsetTotal = it.first.offset + i
                        val charAtOffset = if (offsetTotal in 0 until document.textLength) {
                            document.charsSequence[offsetTotal]
                        } else {
                            ' '
                        }
                        val fg: Int
                        val bg: Int
                        if (mMarks[0].offset == it.first.offset) {
                            g2d.color = Color(config.matchNearestBg, true)
                            fg = config.matchNearestFg
                            bg = config.matchNearestBg
                        } else {
                            fg = config.matchFg
                            bg = config.matchBg
                        }
                        g2d.color = Color(bg, true)
                        // 让背景和字符都垂直居中于该行
                        val yBase = offset.y - y + (globalLineHeight - charBounds.height) / 2
                        g2d.fillRect(offset.x - x, yBase, charBounds.width, charBounds.height)
                        g2d.color = Color(fg, true)
                        g2d.drawString(
                            charAtOffset.toString(),
                            offset.x - x,
                            yBase + mFontMetrics.ascent
                        )
                    }
                }

                // already hit
                var markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength)
                var xInCanvas = 0;
                var yInCanvas = 0;
                if (config.labelBeforeMatch) {
                    if (it.first.advanceIndex > 0) {
                        // Calculate the position of the marker - in front of search text
                        val markOffset = mEditor.offsetToXYCompat(it.first.offset)
                        // 计算已选择的标签在搜索词前的位置
                        val chosenTags = keyTag.substring(0, it.first.advanceIndex)
                        val tagWidth = mFontMetrics.getStringBounds(chosenTags, g).bounds.width
                        val remainTagWidth = mFontMetrics.getStringBounds(keyTag.substring(it.first.advanceIndex), g).bounds.width
                        val xInCanvas = markOffset.x - x - tagWidth - remainTagWidth
                        val yBase = markOffset.y - y + (globalLineHeight - bounds.height) / 2
                        val yInCanvas = yBase + bounds.height - mFontMetrics.descent + 2

                        g2d.color = Color(config.labelHitBg, true)
                        // draw index background
                        g2d.fillRect(
                            xInCanvas,
                            yBase,
                            tagWidth,
                            bounds.height
                        )
                        g2d.color = Color(config.labelHitFg, true)
                        g2d.drawString(
                            chosenTags,
                            xInCanvas,
                            yBase + mFontMetrics.ascent
                        )
                    }
                    // wait to hit
                    // 改为在搜索字符前显示标签
                    markOffset = mEditor.offsetToXYCompat(it.first.offset)
                    xInCanvas = markOffset.x - x - mFontMetrics.getStringBounds(keyTag.substring(it.first.advanceIndex), g).bounds.width
                    val yBase = markOffset.y - y + (globalLineHeight - bounds.height) / 2
                    yInCanvas = yBase + bounds.height - mFontMetrics.descent + 2
                } else {
                    if (it.first.advanceIndex > 0) {
                        // Calculate the position of the marker
                        val markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength)
                        val xInCanvas = markOffset.x - x
                        val yBase = markOffset.y - y + (globalLineHeight - bounds.height) / 2
                        val yInCanvas = yBase + bounds.height - mFontMetrics.descent + 2
                        val chosenTags = keyTag.substring(0, it.first.advanceIndex)
                        g2d.color = Color(config.labelHitBg, true)
                        // draw index background
                        g2d.fillRect(
                            markOffset.x - x,
                            yBase,
                            mFontMetrics.getStringBounds(chosenTags, g).bounds.width,
                            bounds.height
                        )
                        g2d.color = Color(config.labelHitFg, true)
                        g2d.drawString(
                            chosenTags,
                            xInCanvas,
                            yInCanvas
                        )
                    }
                    // wait to hit
                    markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength + it.first.advanceIndex)
                    xInCanvas = markOffset.x - x
                    yInCanvas = markOffset.y - y + charBounds.height - mFontMetrics.descent + 2
                }
                // remain tags
                val remainTags = keyTag.substring(it.first.advanceIndex)

                val yBase = markOffset.y - y + (globalLineHeight - bounds.height) / 2
                g2d.color = Color(config.labelBg, true)
                // draw index background
                g2d.fillRect(
                    xInCanvas,
                    yBase,
                    mFontMetrics.getStringBounds(remainTags, g).bounds.width,
                    bounds.height
                )
                g2d.color = Color(config.labelFg, true)
                g2d.drawString(
                    remainTags,
                    xInCanvas,
                    yBase + mFontMetrics.ascent
                )
            }
        super.paint(g)
    }

    class Mark(
        val keyTag: String,
        val offset: Int,
        val charLength: Int = 0,
        val advanceIndex: Int = 0,
        val hintMark: Boolean = false,
        val rangeEnd: Int = -1, // 语法范围结束（不包含），-1 表示无
    )
}
