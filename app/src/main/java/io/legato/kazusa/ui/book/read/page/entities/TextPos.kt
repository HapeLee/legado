package io.legato.kazusa.ui.book.read.page.entities

import androidx.annotation.Keep

/**
 * 位置信息
 */
@Keep
@Suppress("unused")
data class TextPos(
    var relativePagePos: Int,
    var lineIndex: Int,
    var columnIndex: Int,
) {

    fun upData(
        relativePos: Int,
        lineIndex: Int,
        charIndex: Int,
    ) {
        this.relativePagePos = relativePos
        this.lineIndex = lineIndex
        this.columnIndex = charIndex
    }

    fun upData(pos: TextPos) {
        relativePagePos = pos.relativePagePos
        lineIndex = pos.lineIndex
        columnIndex = pos.columnIndex
    }

    fun compare(pos: TextPos): Int {
        return when {
            relativePagePos < pos.relativePagePos -> -3
            relativePagePos > pos.relativePagePos -> 3
            lineIndex < pos.lineIndex -> -2
            lineIndex > pos.lineIndex -> 2
            columnIndex < pos.columnIndex -> -1
            columnIndex > pos.columnIndex -> 1
            else -> 0
        }
    }

    fun compare(relativePos: Int, lineIndex: Int, charIndex: Int): Int {
        return when {
            this.relativePagePos < relativePos -> -3
            this.relativePagePos > relativePos -> 3
            this.lineIndex < lineIndex -> -2
            this.lineIndex > lineIndex -> 2
            this.columnIndex < charIndex -> -1
            this.columnIndex > charIndex -> 1
            else -> 0
        }
    }

    fun reset() {
        relativePagePos = 0
        lineIndex = -1
        columnIndex = -1
    }

    fun isSelected(): Boolean {
        return lineIndex >= 0 && columnIndex >= 0
    }

}