package org.yelog.ideavim.flash

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "VIMFLASH", storages = [Storage("VIMFLASH.xml")])
class UserConfig : PersistentStateComponent<UserConfig.DataBean> {
    private val dataBean = DataBean()
    override fun getState(): DataBean {
        return dataBean
    }

    override fun loadState(dataBean1: DataBean) {
        XmlSerializerUtil.copyBean(dataBean1, dataBean)
    }

    class DataBean {
        var characters = DEFAULT_CHARACTERS
        var labelFg = DEFAULT_LABEL_FG_COLOR
        var labelBg = DEFAULT_LABEL_BG_COLOR
        var labelHitFg = DEFAULT_LABEL_HIT_FG_COLOR
        var labelHitBg = DEFAULT_LABEL_HIT_BG_COLOR
        var matchFg = DEFAULT_MATCH_FG_COLOR
        var matchBg = DEFAULT_MATCH_BG_COLOR
        var matchNearestFg = DEFAULT_MATCH_NEAREST_FG_COLOR
        var matchNearestBg = DEFAULT_MATCH_NEAREST_BG_COLOR
        var labelBeforeMatch = DEFAULT_LABEL_POSITION_BEFORE
        var autoJumpWhenSingle = DEFAULT_AUTO_JUMP_WHEN_SINGLE
        var scrolloff = DEFAULT_SCROLL_OFF
    }

    companion object {
        private const val DEFAULT_CHARACTERS = "hklyuiopnm,qwertzxcvbasdgjf;"
        const val DEFAULT_LABEL_FG_COLOR = 0xffc8d3f5.toInt()
        const val DEFAULT_LABEL_BG_COLOR = 0xffff007c.toInt()
        const val DEFAULT_LABEL_HIT_FG_COLOR = 0xff808080.toInt()
        const val DEFAULT_LABEL_HIT_BG_COLOR = 0xffff007c.toInt()
        const val DEFAULT_MATCH_FG_COLOR = 0xffc8d3f5.toInt()
        const val DEFAULT_MATCH_BG_COLOR = 0xff3e68d7.toInt()
        const val DEFAULT_MATCH_NEAREST_FG_COLOR = 0xff1b1d2b.toInt()
        const val DEFAULT_MATCH_NEAREST_BG_COLOR = 0xffff966c.toInt()
        const val DEFAULT_LABEL_POSITION_BEFORE = false
        const val DEFAULT_AUTO_JUMP_WHEN_SINGLE = true
        const val DEFAULT_SCROLL_OFF = 4

        // get instance
        private fun getInstance(): UserConfig {
            return ApplicationManager.getApplication().getService(UserConfig::class.java)
        }

        fun getDataBean(): DataBean {
            return getInstance().dataBean
        }
    }
}
