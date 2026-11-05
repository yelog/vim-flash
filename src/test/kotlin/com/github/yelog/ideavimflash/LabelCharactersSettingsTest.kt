package com.github.yelog.vimflash

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerOrReplaceServiceInstance
import com.intellij.util.ui.UIUtil
import org.yelog.ideavim.flash.Configurable
import org.yelog.ideavim.flash.UserConfig
import javax.swing.JTextField

class LabelCharactersSettingsTest : BasePlatformTestCase() {
    private lateinit var config: UserConfig.DataBean
    private lateinit var configurable: Configurable
    private lateinit var characters: JTextField

    override fun setUp() {
        super.setUp()
        val service = UserConfig()
        ApplicationManager.getApplication().registerOrReplaceServiceInstance(UserConfig::class.java, service, testRootDisposable)
        config = service.state
        configurable = Configurable()
        characters = UIUtil.findComponentOfType(configurable.createComponent(), JTextField::class.java)!!
        assertEquals(config.characters, characters.text)
    }

    fun testRejectsEmptySingleAndDuplicateOnlyCharactersWithoutSaving() {
        val originalCharacters = config.characters
        for (invalid in listOf("", " ", "a", "aaa", "a a")) {
            characters.text = invalid
            assertThrows(ConfigurationException::class.java) { configurable.apply() }
            assertEquals(originalCharacters, config.characters)
        }
    }

    fun testSavingNormalizesDuplicatesAndWhitespace() {
        characters.text = "b b a a b"
        configurable.apply()
        assertEquals("ba", config.characters)
        assertEquals("ba", characters.text)
        assertFalse(configurable.isModified)
    }
}
