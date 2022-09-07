package io.legado.app.ui.book.read.page.entities

/**
 * 字符信息
 */
data class TextChar(
    val charData: String,
    var start: Float,
    var end: Float,
    var selected: Boolean = false,
    var isImage: Boolean = false,
    var isSearchResult: Boolean = false
) {

    fun isTouch(x: Float): Boolean {
        return x > start && x < end
    }

}