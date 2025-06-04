package io.legato.kazusa.lib.mobi.decompress

interface Decompressor {

    fun decompress(data: ByteArray): ByteArray

}
