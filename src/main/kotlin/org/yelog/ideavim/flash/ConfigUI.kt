package org.yelog.ideavim.flash

import javax.swing.JPanel
import javax.swing.JTextField

class ConfigUI {
    lateinit var rootPanel: JPanel
    private lateinit var charactersTF: JTextField
    private lateinit var labelFgTF: JTextField
    private lateinit var labelBgTF: JTextField
    private lateinit var matchFgTF: JTextField
    private lateinit var matchBgTF: JTextField
    private lateinit var matchNearestFgTF: JTextField
    private lateinit var matchNearestBgTF: JTextField

    var characters: String?
        get() = charactersTF.text
        set(s) {
            charactersTF.text = s
        }

    var labelBg: Int
        get() = try {
            Integer.parseUnsignedInt(labelBgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            labelBgTF.text = Integer.toHexString(c)
        }
    var labelFg: Int
        get() = try {
            Integer.parseUnsignedInt(labelFgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            labelFgTF.text = Integer.toHexString(c)
        }
    var matchFg: Int
        get() = try {
            Integer.parseUnsignedInt(matchFgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            matchFgTF.text = Integer.toHexString(c)
        }
    var matchBg: Int
        get() = try {
            Integer.parseUnsignedInt(matchBgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            matchBgTF.text = Integer.toHexString(c)
        }
    var matchNearestFg: Int
        get() = try {
            Integer.parseUnsignedInt(matchNearestFgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            matchNearestFgTF.text = Integer.toHexString(c)
        }
    var matchNearestBg: Int
        get() = try {
            Integer.parseUnsignedInt(matchNearestBgTF.text, 16)
        } catch (e: NumberFormatException) {
            0
        }
        set(c) {
            matchNearestBgTF.text = Integer.toHexString(c)
        }
}
