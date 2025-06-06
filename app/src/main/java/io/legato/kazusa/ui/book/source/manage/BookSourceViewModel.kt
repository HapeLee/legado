package io.legato.kazusa.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.BookSource
import io.legato.kazusa.data.entities.BookSourcePart
import io.legato.kazusa.data.entities.toBookSource
import io.legato.kazusa.help.source.SourceHelp
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.cnCompare
import io.legato.kazusa.utils.outputStream
import io.legato.kazusa.utils.splitNotBlank
import io.legato.kazusa.utils.stackTraceStr
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.writeToOutputStream
import splitties.init.appCtx
import java.io.File

/**
 * 书源管理数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val minOrder = appDb.bookSourceDao.minOrder - 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = minOrder - index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun bottomSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val maxOrder = appDb.bookSourceDao.maxOrder + 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = maxOrder + index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun del(sources: List<BookSourcePart>) {
        execute {
            SourceHelp.deleteBookSourceParts(sources)
        }
    }

    fun update(vararg bookSource: BookSource) {
        execute { appDb.bookSourceDao.update(*bookSource) }
    }

    fun upOrder(items: List<BookSourcePart>) {
        if (items.isEmpty()) return
        execute {
            appDb.bookSourceDao.upOrder(items)
        }
    }

    fun enable(enable: Boolean, items: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(enable, items)
        }
    }

    fun enableSelection(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(true, sources)
        }
    }

    fun disableSelection(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(false, sources)
        }
    }

    fun enableExplore(enable: Boolean, items: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(enable, items)
        }
    }

    fun enableSelectExplore(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(true, sources)
        }
    }

    fun disableSelectExplore(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(false, sources)
        }
    }

    fun selectionAddToGroups(sources: List<BookSourcePart>, groups: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    addGroup(groups)
                }
            }
            appDb.bookSourceDao.upGroup(array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<BookSourcePart>, groups: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    removeGroup(groups)
                }
            }
            appDb.bookSourceDao.upGroup(array)
        }
    }

    private fun saveToFile(sources: List<BookSource>, success: (file: File) -> Unit) {
        execute {
            val path = "${context.filesDir}/shareBookSource.json"
            FileUtils.delete(path)
            val file = FileUtils.createFileWithReplace(path)
            file.outputStream().buffered().use {
                GSON.writeToOutputStream(it, sources)
            }
            file
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun saveToFile(
        adapter: BookSourceAdapter,
        searchKey: String?,
        sortAscending: Boolean,
        sort: BookSourceSort,
        success: (file: File) -> Unit
    ) {
        execute {
            val selection = adapter.selection
            val selectedRate = selection.size.toFloat() / adapter.itemCount.toFloat()
            val sources = if (selectedRate == 1f) {
                getBookSources(searchKey, sortAscending, sort)
            } else if (selectedRate < 0.3) {
                selection.toBookSource()
            } else {
                val keys = selection.map { it.bookSourceUrl }.toHashSet()
                val bookSources = getBookSources(searchKey, sortAscending, sort)
                bookSources.filter {
                    keys.contains(it.bookSourceUrl)
                }
            }
            saveToFile(sources, success)
        }
    }

    private fun getBookSources(
        searchKey: String?,
        sortAscending: Boolean,
        sort: BookSourceSort
    ): List<BookSource> {
        return when {
            searchKey.isNullOrEmpty() -> {
                appDb.bookSourceDao.all
            }

            searchKey == appCtx.getString(R.string.enabled) -> {
                appDb.bookSourceDao.allEnabled
            }

            searchKey == appCtx.getString(R.string.disabled) -> {
                appDb.bookSourceDao.allDisabled
            }

            searchKey == appCtx.getString(R.string.need_login) -> {
                appDb.bookSourceDao.allLogin
            }

            searchKey == appCtx.getString(R.string.no_group) -> {
                appDb.bookSourceDao.allNoGroup
            }

            searchKey == appCtx.getString(R.string.enabled_explore) -> {
                appDb.bookSourceDao.allEnabledExplore
            }

            searchKey == appCtx.getString(R.string.disabled_explore) -> {
                appDb.bookSourceDao.allDisabledExplore
            }

            searchKey.startsWith("group:") -> {
                val key = searchKey.substringAfter("group:")
                appDb.bookSourceDao.groupSearch(key)
            }

            else -> {
                appDb.bookSourceDao.search(searchKey)
            }
        }.let { data ->
            if (sortAscending) when (sort) {
                BookSourceSort.Weight -> data.sortedBy { it.weight }
                BookSourceSort.Name -> data.sortedWith { o1, o2 ->
                    o1.bookSourceName.cnCompare(o2.bookSourceName)
                }

                BookSourceSort.Url -> data.sortedBy { it.bookSourceUrl }
                BookSourceSort.Update -> data.sortedByDescending { it.lastUpdateTime }
                BookSourceSort.Respond -> data.sortedBy { it.respondTime }
                BookSourceSort.Enable -> data.sortedWith { o1, o2 ->
                    var sortNum = -o1.enabled.compareTo(o2.enabled)
                    if (sortNum == 0) {
                        sortNum = o1.bookSourceName.cnCompare(o2.bookSourceName)
                    }
                    sortNum
                }

                else -> data
            }
            else when (sort) {
                BookSourceSort.Weight -> data.sortedByDescending { it.weight }
                BookSourceSort.Name -> data.sortedWith { o1, o2 ->
                    o2.bookSourceName.cnCompare(o1.bookSourceName)
                }

                BookSourceSort.Url -> data.sortedByDescending { it.bookSourceUrl }
                BookSourceSort.Update -> data.sortedBy { it.lastUpdateTime }
                BookSourceSort.Respond -> data.sortedByDescending { it.respondTime }
                BookSourceSort.Enable -> data.sortedWith { o1, o2 ->
                    var sortNum = o1.enabled.compareTo(o2.enabled)
                    if (sortNum == 0) {
                        sortNum = o1.bookSourceName.cnCompare(o2.bookSourceName)
                    }
                    sortNum
                }

                else -> data.reversed()
            }
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.bookSourceDao.noGroup
            sources.forEach { source ->
                source.bookSourceGroup = group
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.bookSourceDao.getByGroup(oldGroup)
            sources.forEach { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = appDb.bookSourceDao.getByGroup(group)
                sources.forEach { source ->
                    source.removeGroup(group)
                }
                appDb.bookSourceDao.update(*sources.toTypedArray())
            }
        }
    }

}