package io.legato.kazusa.ui.book.manga.entities

import io.legato.kazusa.data.entities.BookChapter

data class MangaChapter(
    val chapter: BookChapter,
    val pages: List<BaseMangaPage>,
    val imageCount: Int
)
