package org.yelog.ideavim.flash

import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JLabel
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Cursor
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

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
    private lateinit var searchAcrossSplitsCB: javax.swing.JCheckBox

    // Reset labels
    private lateinit var charactersResetLB: JLabel
    private lateinit var labelColorResetLB: JLabel
    private lateinit var labelHitColorResetLB: JLabel
    private lateinit var matchColorResetLB: JLabel
    private lateinit var matchNearestColorResetLB: JLabel
    private lateinit var labelPositionResetLB: JLabel
    private lateinit var autoJumpResetLB: JLabel
    private lateinit var searchAcrossSplitsResetLB: JLabel
    private lateinit var scrolloffResetLB: JLabel

    private var defaultBean: UserConfig.DataBean? = null

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

    var searchAcrossSplits: Boolean
        get() = try {
            searchAcrossSplitsCB.isSelected
        } catch (e: Exception) {
            true
        }
        set(b) {
            searchAcrossSplitsCB.isSelected = b
        }

    fun initReset(defaultBean: UserConfig.DataBean) {
        this.defaultBean = defaultBean
        installListeners()
        updateResetStates()
    }

    private fun installListeners() {
        charactersTF.document.addDocumentListener(simpleDocListener { updateResetStates() })
        scrolloffTF.document.addDocumentListener(simpleDocListener { updateResetStates() })
        labelBeforeMatchCB.addChangeListener { updateResetStates() }
        autoJumpWhenSingleCB.addChangeListener { updateResetStates() }
        searchAcrossSplitsCB.addChangeListener { updateResetStates() }

        val colorPanels = listOf(
            labelFgTF, labelBgTF, labelHitFgTF, labelHitBgTF,
            matchFgTF, matchBgTF, matchNearestFgTF, matchNearestBgTF
        )
        colorPanels.forEach { panel ->
            panel.addActionListener { updateResetStates() }
        }

        addResetAction(charactersResetLB) {
            defaultBean?.let { characters = it.characters }
        }
        addResetAction(labelColorResetLB) {
            defaultBean?.let {
                labelFg = it.labelFg
                labelBg = it.labelBg
            }
        }
        addResetAction(labelHitColorResetLB) {
            defaultBean?.let {
                labelHitFg = it.labelHitFg
                labelHitBg = it.labelHitBg
            }
        }
        addResetAction(matchColorResetLB) {
            defaultBean?.let {
                matchFg = it.matchFg
                matchBg = it.matchBg
            }
        }
        addResetAction(matchNearestColorResetLB) {
            defaultBean?.let {
                matchNearestFg = it.matchNearestFg
                matchNearestBg = it.matchNearestBg
            }
        }
        addResetAction(labelPositionResetLB) {
            defaultBean?.let {
                labelBeforeMatch = it.labelBeforeMatch
            }
        }
        addResetAction(autoJumpResetLB) {
            defaultBean?.let {
                autoJumpWhenSingle = it.autoJumpWhenSingle
            }
        }
        addResetAction(scrolloffResetLB) {
            defaultBean?.let {
                scrolloff = it.scrolloff
            }
        }
        addResetAction(searchAcrossSplitsResetLB) {
            defaultBean?.let {
                searchAcrossSplits = it.searchAcrossSplits
            }
        }
    }

    private fun addResetAction(label: JLabel, action: () -> Unit) {
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (label.isEnabled) {
                    action()
                    updateResetStates()
                }
            }
        })
    }

    private fun updateResetStates() {
        val d = defaultBean ?: return
        updateResetLabel(
            charactersResetLB,
            characters != d.characters
        )
        updateResetLabel(
            labelColorResetLB,
            labelFg != d.labelFg || labelBg != d.labelBg
        )
        updateResetLabel(
            labelHitColorResetLB,
            labelHitFg != d.labelHitFg || labelHitBg != d.labelHitBg
        )
        updateResetLabel(
            matchColorResetLB,
            matchFg != d.matchFg || matchBg != d.matchBg
        )
        updateResetLabel(
            matchNearestColorResetLB,
            matchNearestFg != d.matchNearestFg || matchNearestBg != d.matchNearestBg
        )
        updateResetLabel(
            labelPositionResetLB,
            labelBeforeMatch != d.labelBeforeMatch
        )
        updateResetLabel(
            autoJumpResetLB,
            autoJumpWhenSingle != d.autoJumpWhenSingle
        )
        updateResetLabel(
            scrolloffResetLB,
            scrolloff != d.scrolloff
        )
        updateResetLabel(
            searchAcrossSplitsResetLB,
            searchAcrossSplits != d.searchAcrossSplits
        )
    }

    private fun updateResetLabel(label: JLabel, modified: Boolean) {
        label.isEnabled = modified
        label.foreground = if (modified) JBColor.BLUE else JBColor.GRAY
        label.cursor = if (modified) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        else Cursor.getDefaultCursor()
    }

    private fun simpleDocListener(onChange: () -> Unit) = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = onChange()
        override fun removeUpdate(e: DocumentEvent?) = onChange()
        override fun changedUpdate(e: DocumentEvent?) = onChange()
    }
}
