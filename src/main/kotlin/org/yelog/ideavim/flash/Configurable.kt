package org.yelog.ideavim.flash

import com.intellij.openapi.options.Configurable
import org.yelog.ideavim.flash.UserConfig.DataBean
import javax.swing.JComponent

class Configurable : Configurable {
    private lateinit var config: DataBean
    private lateinit var ui: ConfigUI

    override fun isModified(): Boolean {
        return ui.characters != config.characters || ui.labelFg != config.labelFg || ui.labelBg != config.labelBg || ui.labelHitFg != config.labelHitFg || ui.labelHitBg != config.labelHitBg || ui.matchFg != config.matchFg || ui.matchBg != config.matchBg || ui.matchNearestFg != config.matchNearestFg || ui.matchNearestBg != config.matchNearestBg
    }

    override fun getDisplayName(): String {
        return "vim-flash"
    }

    override fun apply() {
        config.characters = ui.characters.orEmpty()
        config.labelFg = ui.labelFg
        config.labelBg = ui.labelBg
        config.labelHitFg = ui.labelHitFg
        config.labelHitBg = ui.labelHitBg
        config.matchFg = ui.matchFg
        config.matchBg = ui.matchBg
        config.matchNearestFg = ui.matchNearestFg
        config.matchNearestBg = ui.matchNearestBg
    }

    override fun reset() {
        fillUI()
    }

    override fun createComponent(): JComponent {
        config = UserConfig.getDataBean()
        ui = ConfigUI()
        fillUI()
        return ui.rootPanel
    }

    private fun fillUI() {
        ui.characters = config.characters
        ui.labelFg = config.labelFg
        ui.labelBg = config.labelBg
        ui.labelHitFg = config.labelHitFg
        ui.labelHitBg = config.labelHitBg
        ui.matchFg = config.matchFg
        ui.matchBg = config.matchBg
        ui.matchNearestFg = config.matchNearestFg
        ui.matchNearestBg = config.matchNearestBg
    }
}
