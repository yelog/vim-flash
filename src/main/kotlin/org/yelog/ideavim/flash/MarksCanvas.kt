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
        mFont = e.colorsScheme.getFont(EditorFontType.BOLD)
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
                val keyTag = it.first.keyTag
                val charBounds = mFontMetrics.getStringBounds("x", g).bounds
                val bounds = mFontMetrics.getStringBounds(keyTag, g).bounds
                // draw match text background
                // Render each query character individually to prevent soft-wrap line breaks
                for (i in 0 until it.first.charLength) {
                    mEditor.offsetToXYCompat(it.first.offset + i).let { offset ->
                        // Get the character at the specified offset
                        val document = mEditor.document
                        val offsetTotal = it.first.offset + i
                        val charAtOffset = if (offsetTotal in 0 until document.textLength) {
                            document.charsSequence[offsetTotal]
                        } else {
                            ' '
                        }
                        val fg: Int
                        val bg: Int
                        // If it is the first iteration of the loop
                        if (mMarks[0].offset == it.first.offset) {
                            g2d.color = Color(config.matchNearestBg, true)
                            fg = config.matchNearestFg
                            bg = config.matchNearestBg
                        } else {
                            fg = config.matchFg
                            bg = config.matchBg
                        }
                        g2d.color = Color(bg, true)
                        g2d.fillRect(offset.x - x, offset.y - y, charBounds.width, charBounds.height)
                        g2d.color = Color(fg, true)
                        g2d.drawString(
                            charAtOffset.toString(),
                            offset.x - x,
                            offset.y - y + charBounds.height - mFontMetrics.descent + 2
                        )
                    }
                }

                // already hit
                if (it.first.advanceIndex > 0) {
                    // Calculate the position of the marker
                    val markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength)
                    val xInCanvas = markOffset.x - x
                    val yInCanvas = markOffset.y - y + charBounds.height - mFontMetrics.descent + 2
                    val chosenTags = keyTag.substring(0, it.first.advanceIndex)
                    g2d.color = Color(config.labelHitBg, true)
                    // draw index background
                    g2d.fillRect(
                        markOffset.x - x,
                        markOffset.y - y,
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
                val markOffset = mEditor.offsetToXYCompat(it.first.offset + it.first.charLength + it.first.advanceIndex)
                val xInCanvas = markOffset.x - x
                val yInCanvas = markOffset.y - y + charBounds.height - mFontMetrics.descent + 2
                // remain tags
                val remainTags = keyTag.substring(it.first.advanceIndex)

                g2d.color = Color(config.labelBg, true)
                // draw index background
                g2d.fillRect(
                    xInCanvas,
                    markOffset.y - y,
                    mFontMetrics.getStringBounds(remainTags, g).bounds.width,
                    bounds.height
                )
                g2d.color = Color(config.labelFg, true)
                g2d.drawString(
                    remainTags,
                    xInCanvas,
                    yInCanvas
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
    )
}
