package io.legato.kazusa.lib.mobi.entities

data class Fragment(
    val insertOffset: Int,
    val selector: String,
    val index: Int,
    val offset: Int,
    val length: Int
)
