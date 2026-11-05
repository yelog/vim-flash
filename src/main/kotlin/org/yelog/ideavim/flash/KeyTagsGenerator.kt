package org.yelog.ideavim.flash

object KeyTagsGenerator {
    internal fun normalizeKeys(keys: String): String = keys.filterNot { it.isWhitespace() }.toSet().joinToString("")

    /** Empty labels preserve matches when the remaining alphabet cannot distinguish them. */
    fun createTagsTree(targetCount: Int, keys: String): List<String> {
        if (targetCount <= 0) return emptyList()
        val alphabet = normalizeKeys(keys)
        if (alphabet.isEmpty() || (alphabet.length == 1 && targetCount > 1)) {
            return List(targetCount) { "" }
        }

        val res: MutableList<String> = ArrayList()
        recur(targetCount, alphabet, "", res)
        return res
    }

    private fun recur(targetCount: Int, keys: String, prefix: String, res: MutableList<String>) {
        val keysLen = keys.length
        val keysCountMap = HashMap<Char, Int>()
        val chars = keys.toCharArray()
        for (c in chars) {
            keysCountMap[c] = 0
        }

        /* Calculate counts each branch stored. */
        chars.reverse()
        var targetsLeft = targetCount
        var level = 0
        while (targetsLeft > 0) {
            val childLen = if (level == 0) 1 else keysLen - 1
            for (key in chars) {
                keysCountMap[key] = keysCountMap[key]!! + childLen
                targetsLeft -= childLen
                if (targetsLeft <= 0) {
                    keysCountMap[key] = keysCountMap[key]!! + targetsLeft
                    break
                }
            }
            level++
        }

        /* Create tree ( represent by String array ). */
        chars.reverse()
        for (key in chars) {
            val count = keysCountMap[key]!!
            if (count > 1) {
                recur(count, keys, prefix + key, res)
            } else if (count == 1) {
                res.add(prefix + key)
            }
        }
    }
}
