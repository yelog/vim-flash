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
        var labelBg = DEFAULT_LABEL_BG_COLOR
        var labelFg = DEFAULT_LABEL_FONT_COLOR
        var matchBgOpacity = DEFAULT_MATCH_BG_OPACITY
        var matchBg = DEFAULT_MATCH_BG_COLOR
    }

    companion object {
        private const val DEFAULT_CHARACTERS = "hklyuiopnm,qwertzxcvbasdgjf;"
        const val DEFAULT_LABEL_FONT_COLOR = 0xffc8d3f5.toInt()
        const val DEFAULT_LABEL_BG_COLOR = 0xffff007c.toInt()
        const val DEFAULT_MATCH_BG_OPACITY = 0.3f
        const val DEFAULT_MATCH_BG_COLOR = 0xff3e68d7.toInt()

        private val instance: UserConfig
            get() = ApplicationManager.getApplication().getService(UserConfig::class.java)

        fun getDataBean(): DataBean {
            return instance.dataBean
        }
    }
}
