package io.legato.kazusa.lib.mobi.entities

import android.util.SparseArray

data class IndexData(
    val table: List<IndexEntry>,
    val cncx: SparseArray<String>
)
