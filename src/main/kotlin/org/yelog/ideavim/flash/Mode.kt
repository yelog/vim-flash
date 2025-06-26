package org.yelog.ideavim.flash

enum class Mode(val char: Char) {
    SEARCH('s'), // Search mode
    GOTO_RECENT('g'), // Go to recent position
    VIM_F('f'), // Vim 'f' mode
    VIM_F_ALL('f'), // Vim 'f' mode
    VIM_F_BACKWARD('F'), // Vim 'F' mode (backward)
    VIM_F_ALL_BACKWARD('F'), // Vim 'F' mode (backward)
    VIM_T('t'), // Vim 't' mode
    VIM_T_ALL('t'), // Vim 't' mode
    VIM_T_BACKWARD('T'), // Vim 'T' mode (backward)
    VIM_T_ALL_BACKWARD('T'); // Vim 'T' mode (backward)

    companion object {
        fun fromChar(c: Char): Mode? = values().find { it.char == c }
        fun isVimMode(c: Char): Boolean {
            return c == VIM_F.char || c == VIM_F_BACKWARD.char ||
                    c == VIM_T.char || c == VIM_T_BACKWARD.char ||
                    c == VIM_F_ALL.char || c == VIM_F_ALL_BACKWARD.char ||
                    c == VIM_T_ALL.char || c == VIM_T_ALL_BACKWARD.char
        }
        fun isBackward(c: Char): Boolean {
            return c == VIM_F_BACKWARD.char || c == VIM_T_BACKWARD.char ||
                    c == VIM_F_ALL_BACKWARD.char || c == VIM_T_ALL_BACKWARD.char
        }
    }

    fun isVimNotAll(): Boolean = this == VIM_F || this == VIM_F_BACKWARD || this == VIM_T || this == VIM_T_BACKWARD

    fun isVimMode(): Boolean = isVimMode(this.char)

    fun isTill(): Boolean = this == VIM_T || this == VIM_T_BACKWARD ||
            this == VIM_T_ALL || this == VIM_T_ALL_BACKWARD

    fun getOffset(): Int {
        return when (this) {
            VIM_T, VIM_T_ALL -> -1
            VIM_T_BACKWARD, VIM_T_ALL_BACKWARD -> 1
            else -> 0
        }
    }

    fun isTillBefore(): Boolean = this == VIM_T || this == VIM_T_ALL
    fun isTillAfter(): Boolean = this == VIM_T_BACKWARD || this == VIM_T_ALL_BACKWARD

    fun toAll(): Mode {
        return when (this) {
            VIM_F -> VIM_F_ALL
            VIM_F_BACKWARD -> VIM_F_ALL_BACKWARD
            VIM_T -> VIM_T_ALL
            VIM_T_BACKWARD -> VIM_T_ALL_BACKWARD
            else -> this
        }
    }

    fun isBackward(): Boolean = isBackward(this.char)
}
