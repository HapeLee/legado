package io.legato.kazusa.help.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import io.legato.kazusa.help.globalExecutor

class AsyncRecycleBitmapPool(private val delegate: BitmapPool) : BitmapPool by delegate {

    constructor(maxSize: Int) : this(
        if (maxSize > 0) {
            LruBitmapPool(maxSize.toLong())
        } else {
            BitmapPoolAdapter()
        }
    )

    override fun put(bitmap: Bitmap) {
        globalExecutor.execute {
            delegate.put(bitmap)
        }
    }

}
