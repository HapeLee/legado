package io.legato.kazusa.lib.mobi.entities

data class TagxHeader(
    val magic: String,
    val length: Int,
    val numControlBytes: Int
)
