package com.github.yelog.vimflash

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.yelog.ideavim.flash.KeyTagsGenerator
import org.yelog.ideavim.flash.UserConfig

class KeyTagsGeneratorTest : BasePlatformTestCase() {
    fun testNonPositiveTargetCountsHaveNoLabels() {
        assertEmpty(KeyTagsGenerator.createTagsTree(0, "ab"))
        assertEmpty(KeyTagsGenerator.createTagsTree(-1, ""))
    }

    fun testEmptyAlphabetPreservesOneEmptyLabelPerTarget() {
        assertEquals(listOf(""), KeyTagsGenerator.createTagsTree(1, ""))
        assertEquals(List(28) { "" }, KeyTagsGenerator.createTagsTree(28, ""))
    }

    fun testSingleCharacterAlphabetOnlyLabelsOneTarget() {
        assertEquals(listOf("a"), KeyTagsGenerator.createTagsTree(1, "a"))
        assertEquals(listOf("", ""), KeyTagsGenerator.createTagsTree(2, "a"))
    }

    fun testDuplicateCharactersCannotCreateRecursiveBranches() {
        assertEquals(List(3) { "" }, KeyTagsGenerator.createTagsTree(3, "aa"))
        assertEquals(
            KeyTagsGenerator.createTagsTree(30, "ab"),
            KeyTagsGenerator.createTagsTree(30, "aabbaba"),
        )
    }

    fun testNormalizationRemovesWhitespaceAndKeepsCharacterOrder() {
        assertEquals("ba", KeyTagsGenerator.normalizeKeys(" b b\t a\nb "))
        assertEquals(listOf("", ""), KeyTagsGenerator.createTagsTree(2, " \t\n"))
    }

    fun testValidAlphabetKeepsExistingLabelOrder() {
        assertEquals(listOf("a", "b", "cb", "cc"), KeyTagsGenerator.createTagsTree(4, "abc"))
    }

    fun testLabelsAreCompleteUniqueAndPrefixFree() {
        for (alphabet in listOf("ab", "abc", UserConfig.DataBean().characters)) {
            for (targetCount in listOf(1, 2, 3, 4, 7, 28, 29, 100, 256)) {
                val labels = KeyTagsGenerator.createTagsTree(targetCount, alphabet)
                assertEquals(targetCount, labels.size)
                assertEquals(targetCount, labels.toSet().size)
                for (label in labels) {
                    assertTrue(label.isNotEmpty())
                    assertTrue(label.all { it in alphabet })
                    assertFalse(labels.any { it != label && it.startsWith(label) })
                }
            }
        }
    }
}
