package io.legato.kazusa.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.data.appDb
import io.legato.kazusa.help.LifecycleHelp
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.model.AudioPlay
import io.legato.kazusa.model.ReadAloud
import io.legato.kazusa.model.ReadBook
import io.legato.kazusa.service.AudioPlayService
import io.legato.kazusa.service.BaseReadAloudService
import io.legato.kazusa.ui.book.audio.AudioPlayActivity
import io.legato.kazusa.ui.book.read.ReadBookActivity
import io.legato.kazusa.utils.LogUtils
import io.legato.kazusa.utils.getPrefBoolean
import io.legato.kazusa.utils.postEvent


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {

        private const val TAG = "MediaButtonReceiver"

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false
                val keycode: Int = keyEvent.keyCode
                val action: Int = keyEvent.action
                if (action == KeyEvent.ACTION_DOWN) {
                    LogUtils.d(TAG, "Receive mediaButton event, keycode:$keycode")
                    when (keycode) {
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                ReadBook.moveToPrevChapter(true)
                            } else {
                                ReadAloud.prevParagraph(context)
                            }
                        }

                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                ReadBook.moveToNextChapter(true)
                            } else {
                                ReadAloud.nextParagraph(context)
                            }
                        }

                        else -> readAloud(context)
                    }
                }
            }
            return true
        }

        fun readAloud(context: Context, isMediaKey: Boolean = true) {
            when {
                BaseReadAloudService.isRun -> {
                    if (BaseReadAloudService.isPlay()) {
                        ReadAloud.pause(context)
                        AudioPlay.pause(context)
                    } else {
                        ReadAloud.resume(context)
                        AudioPlay.resume(context)
                    }
                }

                AudioPlayService.isRun -> {
                    if (AudioPlayService.pause) {
                        AudioPlay.resume(context)
                    } else {
                        AudioPlay.pause(context)
                    }
                }

                isMediaKey && !AppConfig.readAloudByMediaButton -> {
                    // break
                }

                LifecycleHelp.isExistActivity(ReadBookActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)

                LifecycleHelp.isExistActivity(AudioPlayActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)

                else -> if (AppConfig.mediaButtonOnExit || LifecycleHelp.activitySize() > 0 || !isMediaKey) {
                    ReadAloud.upReadAloudClass()
                    if (ReadBook.book != null) {
                        ReadBook.readAloud()
                    } else {
                        appDb.bookDao.lastReadBook?.let {
                            ReadBook.resetData(it)
                            ReadBook.clearTextChapter()
                            ReadBook.loadContent(false) {
                                ReadBook.readAloud()
                            }
                        }
                    }
                }
            }
        }
    }

}
