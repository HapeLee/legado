package io.legato.kazusa.utils

import android.text.TextUtils
import io.legato.kazusa.lib.icu4j.CharsetDetector
import org.jsoup.Jsoup
import java.io.File

/**
 * 自动获取文件的编码
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object EncodingDetect {

    private val headTagRegex = "(?i)<head>[\\s\\S]*?</head>".toRegex()
    private val headOpenBytes = "<head>".toByteArray()
    private val headCloseBytes = "</head>".toByteArray()

    fun getHtmlEncode(bytes: ByteArray): String {
        try {
            var head: String? = null
            val startIndex = bytes.indexOf(headOpenBytes)
            if (startIndex > -1) {
                val endIndex = bytes.indexOf(headCloseBytes, startIndex)
                if (endIndex > -1) {
                    head = String(bytes.copyOfRange(startIndex, endIndex + headCloseBytes.size))
                }
            }
            val doc = Jsoup.parseBodyFragment(head ?: headTagRegex.find(String(bytes))!!.value)
            val metaTags = doc.getElementsByTag("meta")
            var charsetStr: String
            for (metaTag in metaTags) {
                charsetStr = metaTag.attr("charset")
                if (!TextUtils.isEmpty(charsetStr)) {
                    return charsetStr
                }
                val httpEquiv = metaTag.attr("http-equiv")
                if (httpEquiv.equals("content-type", true)) {
                    val content = metaTag.attr("content")
                    val idx = content.indexOf("charset=", ignoreCase = true)
                    charsetStr = if (idx > -1) {
                        content.substring(idx + "charset=".length)
                    } else {
                        content.substringAfter(";")
                    }
                    if (!TextUtils.isEmpty(charsetStr)) {
                        return charsetStr
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return getEncode(bytes)
    }

    fun getEncode(bytes: ByteArray): String {
        val match = CharsetDetector().setText(bytes).detect()
        return match?.name ?: "UTF-8"
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(filePath: String): String {
        return getEncode(File(filePath))
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(file: File): String {
        val tempByte = getFileBytes(file)
        if (tempByte.isEmpty()) {
            return "UTF-8"
        }
        return getEncode(tempByte)
    }

    private fun getFileBytes(file: File): ByteArray {
        val byteArray = ByteArray(8000)
        var pos = 0
        try {
            file.inputStream().buffered().use {
                while (pos < byteArray.size) {
                    val n = it.read(byteArray, pos, 1)
                    if (n == -1) {
                        break
                    }
                    if (byteArray[pos] < 0) {
                        pos++
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error: $e")
        }
        return byteArray.copyOf(pos)
    }
}