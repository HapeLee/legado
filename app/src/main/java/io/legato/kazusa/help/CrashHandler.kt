package io.legato.kazusa.help

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Looper
import android.webkit.WebSettings
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.config.LocalConfig
import io.legato.kazusa.model.ReadAloud
import io.legato.kazusa.utils.FileDoc
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.createFileIfNotExist
import io.legato.kazusa.utils.createFolderReplace
import io.legato.kazusa.utils.externalCache
import io.legato.kazusa.utils.getFile
import io.legato.kazusa.utils.longToastOnUiLegacy
import io.legato.kazusa.utils.stackTraceStr
import io.legato.kazusa.utils.writeText
import splitties.init.appCtx
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 异常管理类
 */
class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {

    /**
     * 系统默认UncaughtExceptionHandler
     */
    private var mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        //设置该CrashHandler为系统默认的
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * uncaughtException 回调函数
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (shouldAbsorb(ex)) {
            AppLog.put("发生未捕获的异常\n${ex.localizedMessage}", ex)
            Looper.loop()
        } else {
            ReadAloud.stop(context)
            handleException(ex)
            mDefaultHandler?.uncaughtException(thread, ex)
        }
    }

    private fun shouldAbsorb(e: Throwable): Boolean {
        return when {
            e::class.simpleName == "CannotDeliverBroadcastException" -> true
            e is SecurityException && e.message?.contains(
                "nor current process has android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS",
                true
            ) == true -> true

            else -> false
        }
    }

    /**
     * 处理该异常
     */
    private fun handleException(ex: Throwable?) {
        if (ex == null) return
        LocalConfig.appCrash = true
        //保存日志文件
        saveCrashInfo2File(ex)
        if ((ex is OutOfMemoryError || ex.cause is OutOfMemoryError) && AppConfig.recordHeapDump) {
            doHeapDump()
        }
        context.longToastOnUiLegacy(ex.stackTraceStr)
        Thread.sleep(3000)
    }

    companion object {
        /**
         * 存储异常和参数信息
         */
        private val paramsMap by lazy {
            val map = LinkedHashMap<String, String>()
            kotlin.runCatching {
                //获取系统信息
                map["MANUFACTURER"] = Build.MANUFACTURER
                map["BRAND"] = Build.BRAND
                map["MODEL"] = Build.MODEL
                map["SDK_INT"] = Build.VERSION.SDK_INT.toString()
                map["RELEASE"] = Build.VERSION.RELEASE
                map["WebViewUserAgent"] = try {
                    WebSettings.getDefaultUserAgent(appCtx)
                } catch (e: Throwable) {
                    e.toString()
                }
                map["packageName"] = appCtx.packageName
                map["heapSize"] = Runtime.getRuntime().maxMemory().toString()
                //获取app版本信息
                AppConst.appInfo.let {
                    map["versionName"] = it.versionName
                    map["versionCode"] = it.versionCode.toString()
                }
            }
            map
        }

        /**
         * 格式化时间
         */
        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

        /**
         * 保存错误信息到文件中
         */
        fun saveCrashInfo2File(ex: Throwable) {
            val sb = StringBuilder()
            for ((key, value) in paramsMap) {
                sb.append(key).append("=").append(value).append("\n")
            }

            val writer = StringWriter()
            val printWriter = PrintWriter(writer)
            ex.printStackTrace(printWriter)
            var cause: Throwable? = ex.cause
            while (cause != null) {
                cause.printStackTrace(printWriter)
                cause = cause.cause
            }
            printWriter.close()
            val result = writer.toString()
            sb.append(result)
            val crashLog = sb.toString()
            val timestamp = System.currentTimeMillis()
            val time = format.format(Date())
            val fileName = "crash-$time-$timestamp.log"
            try {
                val backupPath = AppConfig.backupPath
                    ?: throw NoStackTraceException("备份路径未配置")
                val uri = Uri.parse(backupPath)
                val fileDoc = FileDoc.fromUri(uri, true)
                fileDoc.createFileIfNotExist(fileName, "crash")
                    .writeText(crashLog)
            } catch (_: Exception) {
            }
            kotlin.runCatching {
                appCtx.externalCacheDir?.let { rootFile ->
                    val exceedTimeMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    rootFile.getFile("crash").listFiles()?.forEach {
                        if (it.lastModified() < exceedTimeMillis) {
                            it.delete()
                        }
                    }
                    FileUtils.createFileIfNotExist(rootFile, "crash", fileName)
                        .writeText(crashLog)
                }
            }
        }

        /**
         * 进行堆转储
         */
        fun doHeapDump(manually: Boolean = false) {
            val heapDir = appCtx
                .externalCache
                .getFile("heapDump")
            heapDir.createFolderReplace()
            val fileName = if (manually) {
                "heap-dump-manually-${System.currentTimeMillis()}.hprof"
            } else {
                "heap-dump-${System.currentTimeMillis()}.hprof"
            }
            val heapFile = heapDir.getFile(fileName)
            val heapDumpName = heapFile.absolutePath
            Debug.dumpHprofData(heapDumpName)
        }

    }

}
