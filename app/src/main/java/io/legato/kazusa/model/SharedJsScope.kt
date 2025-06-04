package io.legato.kazusa.model

import androidx.collection.LruCache
import com.google.gson.reflect.TypeToken
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.http.newCallStrResponse
import io.legato.kazusa.help.http.okHttpClient
import io.legato.kazusa.utils.ACache
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.MD5Utils
import io.legato.kazusa.utils.isAbsUrl
import io.legato.kazusa.utils.isJsonObject
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import splitties.init.appCtx
import java.io.File
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

object SharedJsScope {

    private val cacheFolder = File(appCtx.cacheDir, "shareJs")
    private val aCache = ACache.get(cacheFolder)

    private val scopeMap = LruCache<String, WeakReference<Scriptable>>(16)

    fun getScope(jsLib: String?, coroutineContext: CoroutineContext?): Scriptable? {
        if (jsLib.isNullOrBlank()) {
            return null
        }
        val key = MD5Utils.md5Encode(jsLib)
        var scope = scopeMap[key]?.get()
        if (scope == null) {
            scope = RhinoScriptEngine.run {
                getRuntimeScope(ScriptBindings())
            }
            if (jsLib.isJsonObject()) {
                val jsMap: Map<String, String> = GSON.fromJson(
                    jsLib,
                    TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        String::class.java
                    ).type
                )
                jsMap.values.forEach { value ->
                    if (value.isAbsUrl()) {
                        val fileName = MD5Utils.md5Encode(value)
                        var js = aCache.getAsString(fileName)
                        if (js == null) {
                            js = runBlocking {
                                okHttpClient.newCallStrResponse {
                                    url(value)
                                }.body
                            }
                            if (js != null) {
                                aCache.put(fileName, js)
                            } else {
                                throw NoStackTraceException("下载jsLib-${value}失败")
                            }
                        }
                        RhinoScriptEngine.eval(js, scope, coroutineContext)
                    }
                }
            } else {
                RhinoScriptEngine.eval(jsLib, scope, coroutineContext)
            }
            if (scope is ScriptableObject) {
                scope.sealObject()
            }
            scopeMap.put(key, WeakReference(scope))
        }
        return scope
    }

    fun remove(jsLib: String?) {
        if (jsLib.isNullOrBlank()) {
            return
        }
        val key = MD5Utils.md5Encode(jsLib)
        scopeMap.remove(key)
    }

}