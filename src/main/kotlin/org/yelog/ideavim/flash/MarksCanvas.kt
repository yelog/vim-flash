package org.yelog.ideavim.flash

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import org.yelog.ideavim.flash.utils.offsetToXYCompat
import java.awt.*
import javax.swing.JComponent

class MarksCanvas : JComponent() {
    private val config: UserConfig.DataBean = UserConfig.getDataBean()
    private var mMarks: List<Mark> = emptyList()
    private lateinit var mEditor: Editor
    private lateinit var mFont: Font
    private lateinit var mFontMetrics: FontMetrics

    fun sync(e: Editor) {
        val visibleArea = e.scrollingModel.visibleArea
        setBounds(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height)
        mEditor = e
        mFont = e.colorsScheme.getFont(EditorFontType.BOLD)
        mFontMetrics = e.contentComponent.getFontMetrics(mFont)
    }

    fun setData(marks: List<Mark>) {
        mMarks = marks
        repaint()
    }

    override fun paint(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        // 计算开始坐标的位置, 用于给所有坐标做排序
        val coordinates = mMarks
            .map { mEditor.offsetToXYCompat(it.offset) }
            .toList()

        mMarks.zip(coordinates)
            .sortedBy { it.second.x }
            .forEach {
                g2d.color = Color(config.matchBg, true)
                val keyTag = it.first.keyTag
                val charBounds = mFontMetrics.getStringBounds("x", g).bounds
                // draw match text background
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, config.matchBgOpacity)// 设置透明度
                // 给每个查询字符挨个渲染, 防止 soft-wrap 换行
                for (i in 0 until it.first.charLength) {
                    mEditor.offsetToXYCompat(it.first.offset + i).let { offset ->
                        g2d.fillRect(offset.x - x, offset.y - y, charBounds.width, charBounds.height)
                    }
                }

                // 计算标记的位置
                val markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength)

                g2d.color = Color(config.labelBg, true)
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
                val bounds = mFontMetrics.getStringBounds(keyTag.substring(it.first.advanceIndex), g).bounds
                // draw index background
                g2d.fillRect(
                    markOffset.x - x,
                    markOffset.y - y,
                    bounds.width,
                    bounds.height
                )
                g2d.font = mFont

                val xInCanvas = markOffset.x - x
                val yInCanvas = markOffset.y - y + bounds.height - mFontMetrics.descent + 2
                g2d.color = Color(config.labelFg, true)
                g2d.drawString(keyTag, xInCanvas, yInCanvas)
            }
        super.paint(g)
    }

    class Mark(
        val keyTag: String,
        val offset: Int,
        val charLength: Int = 0,
        val advanceIndex: Int = 0,
        val hintMark: Boolean = false
    )
}
