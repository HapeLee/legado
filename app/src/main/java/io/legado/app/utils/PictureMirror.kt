package io.legado.app.utils

import android.graphics.Canvas
import android.graphics.Picture
import android.os.Build
import android.view.View
import androidx.core.graphics.record
import java.util.concurrent.locks.ReentrantLock

class PictureMirror {

    var picture: Picture? = null
    @Volatile
    var isDirty = true
    val lock = ReentrantLock()
    @Volatile
    var scheduleInvalidateView: View? = null

    inline fun drawLocked(
        canvas: Canvas?,
        width: Int,
        height: Int,
        view: View? = null,
        block: Canvas.() -> Unit
    ) {
        if (atLeastApi23) {
            if (picture == null) picture = Picture()
            val picture = picture!!
            if (isDirty) {
                if (!lock.tryLock()) {
                    if (canvas != null && view != null) {
                        scheduleInvalidateView = view
                    }
                    return
                }
                try {
                    picture.record(width, height, block)
                    isDirty = false
                    scheduleInvalidateView?.postInvalidate()
                    scheduleInvalidateView = null
                } finally {
                    lock.unlock()
                }
            }
            canvas?.drawPicture(picture)
        } else {
            canvas?.block()
        }
    }

    /**
     * 非线程安全，多线程调用可能会崩溃
     */
    inline fun draw(
        canvas: Canvas?,
        width: Int,
        height: Int,
        block: Canvas.() -> Unit
    ) {
        if (atLeastApi23) {
            if (picture == null) picture = Picture()
            val picture = picture!!
            if (isDirty) {
                picture.record(width, height, block)
                isDirty = false
            }
            canvas?.drawPicture(picture)
        } else {
            canvas?.block()
        }
    }

    fun invalidate() {
        isDirty = true
    }

    fun recycle() {
        picture = null
        isDirty = true
    }

    companion object {
        val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

}
