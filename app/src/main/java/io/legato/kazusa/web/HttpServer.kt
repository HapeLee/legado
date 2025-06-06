package io.legato.kazusa.web

import android.graphics.Bitmap
import fi.iki.elonen.NanoHTTPD
import io.legato.kazusa.api.ReturnData
import io.legato.kazusa.api.controller.BookController
import io.legato.kazusa.api.controller.BookSourceController
import io.legato.kazusa.api.controller.ReplaceRuleController
import io.legato.kazusa.api.controller.RssSourceController
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.service.WebService
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.LogUtils
import io.legato.kazusa.utils.stackTraceStr
import io.legato.kazusa.web.utils.AssetsWeb
import kotlinx.coroutines.runBlocking
import okio.Pipe
import okio.buffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class HttpServer(port: Int) : NanoHTTPD(port) {
    private val assetsWeb = AssetsWeb("web")

    override fun serve(session: IHTTPSession): Response {
        WebService.serve()
        var returnData: ReturnData? = null
        val ct = ContentType(session.headers["content-type"]).tryUTF8()
        session.headers["content-type"] = ct.contentTypeHeader
        var uri = session.uri

        val startAt = System.currentTimeMillis()
        LogUtils.d(TAG) {
            "${session.method.name} - $uri - ${session.queryParameterString} - Start($startAt)"
        }

        try {
            when (session.method) {
                Method.OPTIONS -> {
                    val response = newFixedLengthResponse("")
                    response.addHeader("Access-Control-Allow-Methods", "POST")
                    response.addHeader("Access-Control-Allow-Headers", "content-type")
                    response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
                    //response.addHeader("Access-Control-Max-Age", "3600");
                    return response
                }

                Method.POST -> {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
                    val postData = files["postData"]

                    returnData = runBlocking {
                        when (uri) {
                            "/saveBookSource" -> BookSourceController.saveSource(postData)
                            "/saveBookSources" -> BookSourceController.saveSources(postData)
                            "/deleteBookSources" -> BookSourceController.deleteSources(postData)
                            "/saveBook" -> BookController.saveBook(postData)
                            "/deleteBook" -> BookController.deleteBook(postData)
                            "/saveBookProgress" -> BookController.saveBookProgress(postData)
                            "/addLocalBook" -> BookController.addLocalBook(session.parameters, files)
                            "/saveReadConfig" -> BookController.saveWebReadConfig(postData)
                            "/saveRssSource" -> RssSourceController.saveSource(postData)
                            "/saveRssSources" -> RssSourceController.saveSources(postData)
                            "/deleteRssSources" -> RssSourceController.deleteSources(postData)
                            "/saveReplaceRule" -> ReplaceRuleController.saveRule(postData)
                            "/deleteReplaceRule" -> ReplaceRuleController.delete(postData)
                            "/testReplaceRule" -> ReplaceRuleController.testRule(postData)
                            else -> null
                        }
                    }
                }

                Method.GET -> {
                    val parameters = session.parameters

                    returnData = when (uri) {
                        "/getBookSource" -> BookSourceController.getSource(parameters)
                        "/getBookSources" -> BookSourceController.sources
                        "/getBookshelf" -> BookController.bookshelf
                        "/getChapterList" -> BookController.getChapterList(parameters)
                        "/refreshToc" -> BookController.refreshToc(parameters)
                        "/getBookContent" -> BookController.getBookContent(parameters)
                        "/cover" -> BookController.getCover(parameters)
                        "/image" -> BookController.getImg(parameters)
                        "/getReadConfig" -> BookController.getWebReadConfig()
                        "/getRssSource" -> RssSourceController.getSource(parameters)
                        "/getRssSources" -> RssSourceController.sources
                        "/getReplaceRules" -> ReplaceRuleController.allRules
                        else -> null
                    }
                }

                else -> Unit
            }

            if (returnData == null) {
                if (uri.endsWith("/"))
                    uri += "index.html"
                return assetsWeb.getResponse(uri)
            }

            val response = if (returnData.data is Bitmap) {
                val outputStream = ByteArrayOutputStream()
                (returnData.data as Bitmap).compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                outputStream.close()
                val inputStream = ByteArrayInputStream(byteArray)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/png",
                    inputStream,
                    byteArray.size.toLong()
                )
            } else {
                val data = returnData.data
                if (data is List<*> && data.size > 3000) {
                    val pipe = Pipe(16 * 1024)
                    Coroutine.async {
                        pipe.sink.buffer().outputStream().bufferedWriter(Charsets.UTF_8).use {
                            GSON.toJson(returnData, it)
                        }
                    }
                    newChunkedResponse(
                        Response.Status.OK,
                        "application/json",
                        pipe.source.buffer().inputStream()
                    )
                } else {
                    newFixedLengthResponse(GSON.toJson(returnData))
                }
            }
            response.addHeader("Access-Control-Allow-Methods", "GET, POST")
            response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
            LogUtils.d(TAG) {
                "${session.method.name} - $uri - ${session.queryParameterString} - End($startAt)"
            }
            return response
        } catch (e: Exception) {
            LogUtils.d(TAG) {
                "${session.method.name} - $uri - ${session.queryParameterString} - Error End($startAt)\n$e\n${e.stackTraceStr}"
            }
            return newFixedLengthResponse(e.message)
        }

    }

    companion object {
        private const val TAG = "HttpServer"
    }

}
