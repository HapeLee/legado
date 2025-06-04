package io.legato.kazusa.utils

import io.legato.kazusa.data.entities.BookChapter

fun BookChapter.internString() {
    title = title.intern()
    bookUrl = bookUrl.intern()
}
