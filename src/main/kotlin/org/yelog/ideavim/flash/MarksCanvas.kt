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

        val coordinates = mMarks
            .map { mEditor.offsetToXYCompat(it.offset) }
            .toList()

        mMarks.zip(coordinates)
            .sortedBy { it.second.x }
            .forEach {
                g2d.color = Color(config.backgroundColor, true)
                val keyTag = it.first.keyTag
                val charBounds = mFontMetrics.getStringBounds("x", g).bounds
                // draw match text background
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)// 设置透明度
                g2d.fillRect(it.second.x - x, it.second.y - y, charBounds.width * it.first.charLength, charBounds.height)

                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
                val bounds = mFontMetrics.getStringBounds(keyTag.substring(it.first.advanceIndex), g).bounds
                // draw index background
                g2d.fillRect(
                    it.second.x - x + it.first.charLength * charBounds.width,
                    it.second.y - y,
                    bounds.width,
                    bounds.height
                )
                g2d.font = mFont

                val xInCanvas = it.second.x - x + it.first.charLength * bounds.width
                val yInCanvas = it.second.y - y + bounds.height - mFontMetrics.descent + 2
                if (keyTag.length == 2) {
                    if (it.first.advanceIndex == 0) {
                        val midX = xInCanvas + bounds.width / 2

                        // first char
                        g2d.color = Color(config.hit2Color0, true)
                        g2d.drawString(keyTag[0].toString(), xInCanvas, yInCanvas)

                        // second char
                        g2d.color = Color(config.hit2Color1, true)
                        g2d.drawString(keyTag[1].toString(), midX, yInCanvas)
                    } else {
                        g2d.color = Color(config.hit2Color1, true)
                        g2d.drawString(keyTag[1].toString(), xInCanvas, yInCanvas)
                    }
                } else {
                    g2d.color = Color(config.hit1Color, true)
                    g2d.drawString(keyTag[0].toString(), xInCanvas, yInCanvas)
                }
            }
        super.paint(g)
    }

    class Mark(val keyTag: String, val offset: Int, val charLength: Int = 0, val advanceIndex: Int = 0)
}
