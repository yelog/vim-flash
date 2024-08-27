package org.yelog.ideavim.flash

import com.intellij.openapi.options.Configurable
import org.yelog.ideavim.flash.UserConfig.DataBean
import javax.swing.JComponent

class Configurable : Configurable {
    private lateinit var config: DataBean
    private lateinit var ui: ConfigUI

    override fun isModified(): Boolean {
        return ui.characters != config.characters || ui.labelBg != config.labelBg || ui.labelFg != config.labelFg || ui.matchBg != config.matchBg
    }

    override fun getDisplayName(): String {
        return "vim-flash"
    }

    override fun apply() {
        config.characters = ui.characters.orEmpty()
        config.labelFg = ui.labelFg
        config.labelBg = ui.labelBg
        if (ui.labelBg == null) {
            config.labelBg = UserConfig.DEFAULT_LABEL_BG_COLOR
        }
        if (ui.labelFg == null) {
            config.labelFg = UserConfig.DEFAULT_LABEL_FONT_COLOR
        }
        if (ui.matchBgOpacity == null) {
            config.matchBgOpacity = UserConfig.DEFAULT_MATCH_BG_OPACITY
        }
        if (ui.matchBg == null) {
            config.matchBg = UserConfig.DEFAULT_MATCH_BG_COLOR
        }
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
        ui.labelBg = config.labelBg
        ui.labelFg = config.labelFg
        ui.matchBg = config.matchBg
        ui.matchBgOpacity = config.matchBgOpacity
    }
}
