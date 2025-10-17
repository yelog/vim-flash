package org.yelog.ideavim.flash

import javax.swing.JPanel
import javax.swing.JTextField
import com.intellij.ui.ColorPanel
import java.awt.Color

class ConfigUI {
    lateinit var rootPanel: JPanel
    private lateinit var charactersTF: JTextField
    private lateinit var labelFgTF: ColorPanel
    private lateinit var labelBgTF: ColorPanel
    private lateinit var labelHitFgTF: ColorPanel
    private lateinit var labelHitBgTF: ColorPanel
    private lateinit var matchFgTF: ColorPanel
    private lateinit var matchBgTF: ColorPanel
    private lateinit var matchNearestFgTF: ColorPanel
    private lateinit var matchNearestBgTF: ColorPanel
    private lateinit var labelBeforeMatchCB: javax.swing.JCheckBox // Added for form binding
    private lateinit var autoJumpWhenSingleCB: javax.swing.JCheckBox // Added for form binding
    private lateinit var scrolloffTF:  JTextField // Added for form binding

    var characters: String?
        get() = charactersTF.text
        set(s) {
            charactersTF.text = s
        }

    var labelFg: Int
        get() = labelFgTF.selectedColor?.rgb ?: 0
        set(c) {
            labelFgTF.setSelectedColor(Color(c, true))
        }
    var labelBg: Int
        get() = labelBgTF.selectedColor?.rgb ?: 0
        set(c) {
            labelBgTF.setSelectedColor(Color(c, true))
        }
    var labelHitFg: Int
        get() = labelHitFgTF.selectedColor?.rgb ?: 0
        set(c) {
            labelHitFgTF.setSelectedColor(Color(c, true))
        }
    var labelHitBg: Int
        get() = labelHitBgTF.selectedColor?.rgb ?: 0
        set(c) {
            labelHitBgTF.setSelectedColor(Color(c, true))
        }
    var matchFg: Int
        get() = matchFgTF.selectedColor?.rgb ?: 0
        set(c) {
            matchFgTF.setSelectedColor(Color(c, true))
        }
    var matchBg: Int
        get() = matchBgTF.selectedColor?.rgb ?: 0
        set(c) {
            matchBgTF.setSelectedColor(Color(c, true))
        }
    var matchNearestFg: Int
        get() = matchNearestFgTF.selectedColor?.rgb ?: 0
        set(c) {
            matchNearestFgTF.setSelectedColor(Color(c, true))
        }
    var matchNearestBg: Int
        get() = matchNearestBgTF.selectedColor?.rgb ?: 0
        set(c) {
            matchNearestBgTF.setSelectedColor(Color(c, true))
        }
    var labelBeforeMatch: Boolean
        get() = try {
            labelBeforeMatchCB.isSelected
        } catch (e: Exception) {
            false
        }
        set(b) {
            labelBeforeMatchCB.isSelected = b
        }

    var autoJumpWhenSingle: Boolean
        get() = try {
            autoJumpWhenSingleCB.isSelected
        } catch (e: Exception) {
            false
        }
        set(b) {
            autoJumpWhenSingleCB.isSelected = b
        }
    var scrolloff: Int
        get() = try {
            Integer.parseInt(scrolloffTF.text)
        } catch (e: NumberFormatException) {
            0
        }
        set(i) {
            scrolloffTF.text = i.toString()
        }
}
