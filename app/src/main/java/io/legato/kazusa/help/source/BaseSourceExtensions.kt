package io.legato.kazusa.help.source

import io.legato.kazusa.constant.SourceType
import io.legato.kazusa.data.entities.BaseSource
import io.legato.kazusa.data.entities.BookSource
import io.legato.kazusa.data.entities.RssSource
import io.legato.kazusa.model.SharedJsScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

fun BaseSource.getShareScope(coroutineContext: CoroutineContext? = null): Scriptable? {
    return SharedJsScope.getScope(jsLib, coroutineContext)
}

fun BaseSource.getSourceType(): Int {
    return when (this) {
        is BookSource -> SourceType.book
        is RssSource -> SourceType.rss
        else -> error("unknown source type: ${this::class.simpleName}.")
    }
}
