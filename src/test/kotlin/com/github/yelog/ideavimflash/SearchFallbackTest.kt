package com.github.yelog.vimflash

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerOrReplaceServiceInstance
import org.yelog.ideavim.flash.MarksCanvas
import org.yelog.ideavim.flash.Mode
import org.yelog.ideavim.flash.UserConfig
import org.yelog.ideavim.flash.action.Search
import java.awt.image.BufferedImage

class SearchFallbackTest : BasePlatformTestCase() {
    private lateinit var config: UserConfig.DataBean

    override fun setUp() {
        super.setUp()
        val service = UserConfig()
        ApplicationManager.getApplication().registerOrReplaceServiceInstance(UserConfig::class.java, service, testRootDisposable)
        config = service.state
        config.searchAcrossSplits = false
    }

    private fun startSearch(text: String): Search {
        myFixture.configureByText(PlainTextFileType.INSTANCE, text)
        myFixture.editor.caretModel.moveToOffset(0)
        return Search().also { it.start(myFixture.editor, Mode.SEARCH) }
    }

    private fun findUnderscores(search: Search): List<MarksCanvas.Mark> =
        search.input(myFixture.editor, '_', emptyList(), "_")

    fun testExhaustedDefaultAlphabetKeepsAllMatchesAndNearestTarget() {
        val text = config.characters.map { "_$it" }.joinToString(" ")
        val marks = findUnderscores(startSearch(text))

        assertEquals(config.characters.length, marks.size)
        assertTrue(marks.all { it.keyTag.isEmpty() })
        assertEquals(text.indices.filter { text[it] == '_' }.toSet(), marks.map { it.offset }.toSet())
        assertEquals(0, marks.single { it.isNearest }.offset)
        assertTrue(marks.all { it.sourceEditor === myFixture.editor })
    }

    fun testMoreInputRestoresLabelsAndAllowsSelection() {
        val firstCharacter = config.characters.first()
        val search = startSearch(config.characters.map { "_$it" }.joinToString(" "))
        val unlabeled = findUnderscores(search)
        val narrowed = search.input(myFixture.editor, firstCharacter, unlabeled, "_$firstCharacter")

        assertEquals(1, narrowed.size)
        assertEquals(0, narrowed.single().offset)
        assertTrue(narrowed.single().keyTag.isNotEmpty())
        val labelCharacter = narrowed.single().keyTag.single()
        val selected = search.input(myFixture.editor, labelCharacter, narrowed, "_$firstCharacter$labelCharacter")
        assertEquals(0, selected.single().offset)
        assertTrue(selected.single().hintMark)
    }

    fun testSingleRemainingCharacterKeepsMultipleMatches() {
        config.characters = "ab"
        val marks = findUnderscores(startSearch("_a _a"))
        assertEquals(2, marks.size)
        assertTrue(marks.all { it.keyTag.isEmpty() })
        assertEquals(1, marks.count { it.isNearest })
    }

    fun testSingleMatchCanUseOneRemainingLabelCharacter() {
        config.characters = "ab"
        val marks = findUnderscores(startSearch("_a"))
        assertEquals("b", marks.single().keyTag)
    }

    fun testLegacyEmptyConfigurationStillAllowsNarrowing() {
        config.characters = ""
        val search = startSearch("_x _y")
        val marks = findUnderscores(search)
        val narrowed = search.input(myFixture.editor, 'x', marks, "_x")
        assertEquals(2, marks.size)
        assertEquals(0, narrowed.single().offset)
        assertTrue(narrowed.single().isNearest)
    }

    fun testDuplicateConfiguredCharactersStillLabelEveryMatch() {
        config.characters = "aabb"
        val marks = findUnderscores(startSearch("_x _y _z"))
        assertEquals(3, marks.size)
        assertEquals(3, marks.map { it.keyTag }.toSet().size)
        assertTrue(marks.all { it.keyTag.isNotEmpty() })
    }

    fun testDefaultSplitSearchModeAlsoRetainsUnlabeledMatches() {
        config.searchAcrossSplits = true
        val marks = findUnderscores(startSearch(config.characters.map { "_$it" }.joinToString(" ")))
        assertEquals(config.characters.length, marks.size)
        assertTrue(marks.all { it.keyTag.isEmpty() })
    }

    fun testUnlabeledMatchesStillPaintNormalAndNearestHighlights() {
        config.characters = "ab"
        val marks = findUnderscores(startSearch("_a _a"))
        val canvas = MarksCanvas().apply {
            sync(myFixture.editor)
            setSize(400, 100)
            setData(marks, "_")
        }
        val image = BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            canvas.paint(graphics)
        } finally {
            graphics.dispose()
        }
        val colors = image.getRGB(0, 0, image.width, image.height, null, 0, image.width).toSet()
        assertTrue(colors.contains(config.matchNearestBg))
        assertTrue(colors.contains(config.matchBg))
    }
}
