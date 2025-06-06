package io.legato.kazusa.ui.book.import.remote

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.constant.BookType
import io.legato.kazusa.data.appDb
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.AppWebDav
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.lib.webdav.Authorization
import io.legato.kazusa.model.analyzeRule.CustomUrl
import io.legato.kazusa.model.localBook.LocalBook
import io.legato.kazusa.model.remote.RemoteBook
import io.legato.kazusa.model.remote.RemoteBookWebDav
import io.legato.kazusa.utils.AlphanumComparator
import io.legato.kazusa.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Collections

class RemoteBookViewModel(application: Application) : BaseViewModel(application) {
    var sortKey = RemoteBookSort.Default
    var sortAscending = false
    val dirList = arrayListOf<RemoteBook>()
    val permissionDenialLiveData = MutableLiveData<Int>()

    var dataCallback: DataCallback? = null

    val dataFlow = callbackFlow<List<RemoteBook>> {

        val list = Collections.synchronizedList(ArrayList<RemoteBook>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<RemoteBook>) {
                list.clear()
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun addItems(remoteFiles: List<RemoteBook>) {
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }

            override fun screen(key: String?) {
                if (key.isNullOrBlank()) {
                    trySend(list)
                } else {
                    trySend(
                        list.filter { it.filename.contains(key) }
                    )
                }
            }
        }

        awaitClose {
            dataCallback = null
        }
    }.map { list ->
        if (sortAscending) when (sortKey) {
            RemoteBookSort.Name -> list.sortedWith(compareBy<RemoteBook> { !it.isDir }
                    then compareBy(AlphanumComparator) { it.filename })

            else -> list.sortedWith(compareBy({ !it.isDir }, { it.lastModify }))
        } else when (sortKey) {
            RemoteBookSort.Name -> list.sortedWith { o1, o2 ->
                val compare = -compareValues(o1.isDir, o2.isDir)
                if (compare == 0) {
                    return@sortedWith -AlphanumComparator.compare(o1.filename, o2.filename)
                }
                return@sortedWith compare
            }

            else -> list.sortedWith { o1, o2 ->
                val compare = -compareValues(o1.isDir, o2.isDir)
                if (compare == 0) {
                    return@sortedWith -compareValues(o1.lastModify, o2.lastModify)
                }
                return@sortedWith compare
            }
        }
    }.flowOn(Dispatchers.IO)

    private var remoteBookWebDav: RemoteBookWebDav? = null
    var isDefaultWebdav = false

    fun initData(onSuccess: () -> Unit) {
        execute {
            isDefaultWebdav = false
            appDb.serverDao.get(AppConfig.remoteServerId)?.getWebDavConfig()?.let {
                val authorization = Authorization(it)
                remoteBookWebDav = RemoteBookWebDav(it.url, authorization, AppConfig.remoteServerId)
                return@execute
            }
            isDefaultWebdav = true
            remoteBookWebDav = AppWebDav.defaultBookWebDav
                ?: throw NoStackTraceException("webDav没有配置")
        }.onError {
            context.toastOnUi("初始化webDav出错:${it.localizedMessage}")
        }.onSuccess {
            onSuccess.invoke()
        }
    }

    fun loadRemoteBookList(path: String?, loadCallback: (loading: Boolean) -> Unit) {
        executeLazy {
            val bookWebDav = remoteBookWebDav
                ?: throw NoStackTraceException("没有配置webDav")
            dataCallback?.clear()
            val url = path ?: bookWebDav.rootBookUrl
            val bookList = bookWebDav.getRemoteBookList(url)
            dataCallback?.setItems(bookList)
        }.onError {
            AppLog.put("获取webDav书籍出错\n${it.localizedMessage}", it)
            context.toastOnUi("获取webDav书籍出错\n${it.localizedMessage}")
        }.onStart {
            loadCallback.invoke(true)
        }.onFinally {
            loadCallback.invoke(false)
        }.start()
    }

    fun addToBookshelf(remoteBooks: HashSet<RemoteBook>, finally: () -> Unit) {
        execute {
            val bookWebDav = remoteBookWebDav
                ?: throw NoStackTraceException("没有配置webDav")
            remoteBooks.forEach { remoteBook ->
                val downloadBookUri = bookWebDav.downloadRemoteBook(remoteBook)
                LocalBook.importFiles(downloadBookUri).forEach { book ->
                    book.origin = BookType.webDavTag + CustomUrl(remoteBook.path)
                        .putAttribute("serverID", bookWebDav.serverID)
                        .toString()
                    book.save()
                }
                remoteBook.isOnBookShelf = true
            }
        }.onError {
            AppLog.put("导入出错\n${it.localizedMessage}", it)
            context.toastOnUi("导入出错\n${it.localizedMessage}")
            if (it is SecurityException) {
                permissionDenialLiveData.postValue(1)
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun updateCallBackFlow(filterKey: String?) {
        dataCallback?.screen(filterKey)
    }

    interface DataCallback {

        fun setItems(remoteFiles: List<RemoteBook>)

        fun addItems(remoteFiles: List<RemoteBook>)

        fun clear()

        fun screen(key: String?)

    }
}