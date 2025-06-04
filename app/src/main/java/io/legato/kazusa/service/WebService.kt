package io.legato.kazusa.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseService
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.constant.IntentAction
import io.legato.kazusa.constant.NotificationId
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.receiver.NetworkChangedListener
import io.legato.kazusa.utils.NetworkUtils
import io.legato.kazusa.utils.getPrefBoolean
import io.legato.kazusa.utils.getPrefInt
import io.legato.kazusa.utils.postEvent
import io.legato.kazusa.utils.printOnDebug
import io.legato.kazusa.utils.sendToClip
import io.legato.kazusa.utils.servicePendingIntent
import io.legato.kazusa.utils.startForegroundServiceCompat
import io.legato.kazusa.utils.startService
import io.legato.kazusa.utils.stopService
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.web.HttpServer
import io.legato.kazusa.web.WebSocketServer
import splitties.init.appCtx
import splitties.systemservices.powerManager
import splitties.systemservices.wifiManager
import java.io.IOException

class WebService : BaseService() {

    companion object {
        var isRun = false
        var hostAddress = ""

        fun start(context: Context) {
            context.startService<WebService>()
        }

        fun startForeground(context: Context) {
            val intent = Intent(context, WebService::class.java)
            context.startForegroundServiceCompat(intent)
        }

        fun stop(context: Context) {
            context.stopService<WebService>()
        }

        fun serve() {
            appCtx.startService<WebService> {
                action = "serve"
            }
        }
    }

    private val useWakeLock = appCtx.getPrefBoolean(PreferKey.webServiceWakeLock, false)
    private val wakeLock: PowerManager.WakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "legado:webService")
            .apply {
                setReferenceCounted(false)
            }
    }
    private val wifiLock by lazy {
        @Suppress("DEPRECATION")
        wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "legado:AudioPlayService")
            ?.apply {
                setReferenceCounted(false)
            }
    }
    private var httpServer: HttpServer? = null
    private var webSocketServer: WebSocketServer? = null
    private var notificationList = mutableListOf(appCtx.getString(R.string.service_starting))
    private val networkChangedListener by lazy {
        NetworkChangedListener(this)
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        if (useWakeLock) {
            wakeLock.acquire()
            wifiLock?.acquire()
        }
        isRun = true
        upTile(true)
        networkChangedListener.register()
        networkChangedListener.onNetworkChanged = {
            val addressList = NetworkUtils.getLocalIPAddress()
            notificationList.clear()
            if (addressList.any()) {
                notificationList.addAll(addressList.map { address ->
                    getString(
                        R.string.http_ip,
                        address.hostAddress,
                        getPort()
                    )
                })
                hostAddress = notificationList.first()
            } else {
                hostAddress = getString(R.string.network_connection_unavailable)
                notificationList.add(hostAddress)
            }
            startForegroundNotification()
            postEvent(EventBus.WEB_SERVICE, hostAddress)
        }
    }

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.stop -> stopSelf()
            "copyHostAddress" -> sendToClip(hostAddress)
            "serve" -> if (useWakeLock) {
                wakeLock.acquire()
                wifiLock?.acquire()
            }

            else -> upWebServer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (useWakeLock) {
            wakeLock.release()
            wifiLock?.release()
        }
        networkChangedListener.unRegister()
        isRun = false
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        postEvent(EventBus.WEB_SERVICE, "")
        upTile(false)
    }

    private fun upWebServer() {
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        val addressList = NetworkUtils.getLocalIPAddress()
        if (addressList.any()) {
            val port = getPort()
            httpServer = HttpServer(port)
            webSocketServer = WebSocketServer(port + 1)
            try {
                httpServer?.start()
                webSocketServer?.start(1000 * 30) // 通信超时设置
                notificationList.clear()
                notificationList.addAll(addressList.map { address ->
                    getString(
                        R.string.http_ip,
                        address.hostAddress,
                        getPort()
                    )
                })
                hostAddress = notificationList.first()
                isRun = true
                postEvent(EventBus.WEB_SERVICE, hostAddress)
                startForegroundNotification()
            } catch (e: IOException) {
                toastOnUi(e.localizedMessage ?: "")
                e.printOnDebug()
                stopSelf()
            }
        } else {
            toastOnUi("web service cant start, no ip address")
            stopSelf()
        }
    }

    private fun getPort(): Int {
        var port = getPrefInt(PreferKey.webPort, 1122)
        if (port > 65530 || port < 1024) {
            port = 1122
        }
        return port
    }

    /**
     * 更新通知
     */
    override fun startForegroundNotification() {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdWeb)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_web_service_noti)
            .setOngoing(true)
            .setContentTitle(getString(R.string.web_service))
            .setContentText(notificationList.joinToString("\n"))
            .setContentIntent(
                servicePendingIntent<WebService>("copyHostAddress")
            )
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<WebService>(IntentAction.stop)
        )
        val notification = builder.build()
        startForeground(NotificationId.WebService, notification)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun upTile(active: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            kotlin.runCatching {
                startService<WebTileService> {
                    action = if (active) {
                        IntentAction.start
                    } else {
                        IntentAction.stop
                    }
                }
            }

        }
    }
}
