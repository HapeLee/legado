package io.legato.kazusa.lib.mobi.entities

data class ExthRecordType(
    val name: String,
    val type: String = "string",
    val many: Boolean = false
)
