package io.legato.kazusa.help.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import io.legato.kazusa.utils.isAbsUrl
import io.legato.kazusa.utils.isContentScheme
import io.legato.kazusa.utils.isDataUrl
import io.legato.kazusa.utils.lifecycle
import java.io.File
import androidx.core.net.toUri

//https://bumptech.github.io/glide/doc/generatedapi.html
//Instead of GlideApp, use com.bumptech.Glide
@Suppress("unused")
object ImageLoader {

    /**
     * 自动判断path类型
     */
    fun load(context: Context, path: String?): RequestBuilder<Drawable> {
        return when {
            path.isNullOrEmpty() -> Glide.with(context).load(path)
            path.isDataUrl() -> Glide.with(context).load(path)
            path.isAbsUrl() -> Glide.with(context).load(path)
            path.isContentScheme() -> Glide.with(context).load(path.toUri())
            else -> kotlin.runCatching {
                Glide.with(context).load(File(path))
            }.getOrElse {
                Glide.with(context).load(path)
            }
        }
    }

    fun load(fragment: Fragment, lifecycle: Lifecycle, path: String?): RequestBuilder<Drawable> {
        val requestManager = Glide.with(fragment).lifecycle(lifecycle)
        return when {
            path.isNullOrEmpty() -> requestManager.load(path)
            path.isDataUrl() -> requestManager.load(path)
            path.isAbsUrl() -> requestManager.load(path)
            path.isContentScheme() -> requestManager.load(Uri.parse(path))

            else -> kotlin.runCatching {
                requestManager.load(File(path))
            }.getOrElse {
                requestManager.load(path)
            }
        }
    }

    fun loadBitmap(context: Context, path: String?): RequestBuilder<Bitmap> {
        val requestManager = Glide.with(context).`as`(Bitmap::class.java)
        return when {
            path.isNullOrEmpty() -> requestManager.load(path)
            path.isDataUrl() -> requestManager.load(path)
            path.isAbsUrl() -> requestManager.load(path)
            path.isContentScheme() -> requestManager.load(Uri.parse(path))
            else -> kotlin.runCatching {
                requestManager.load(File(path))
            }.getOrElse {
                requestManager.load(path)
            }
        }
    }

    fun loadFile(context: Context, path: String?): RequestBuilder<File> {
        return when {
            path.isNullOrEmpty() -> Glide.with(context).asFile().load(path)
            path.isAbsUrl() -> Glide.with(context).asFile().load(path)
            path.isContentScheme() -> Glide.with(context).asFile().load(Uri.parse(path))
            else -> kotlin.runCatching {
                Glide.with(context).asFile().load(File(path))
            }.getOrElse {
                Glide.with(context).asFile().load(path)
            }
        }
    }

    fun load(context: Context, @DrawableRes resId: Int?): RequestBuilder<Drawable> {
        return Glide.with(context).load(resId)
    }

    fun load(context: Context, file: File?): RequestBuilder<Drawable> {
        return Glide.with(context).load(file)
    }

    fun load(context: Context, uri: Uri?): RequestBuilder<Drawable> {
        return Glide.with(context).load(uri)
    }

    fun load(context: Context, drawable: Drawable?): RequestBuilder<Drawable> {
        return Glide.with(context).load(drawable)
    }

    fun load(context: Context, bitmap: Bitmap?): RequestBuilder<Drawable> {
        return Glide.with(context).load(bitmap)
    }

    fun load(context: Context, bytes: ByteArray?): RequestBuilder<Drawable> {
        return Glide.with(context).load(bytes)
    }

}
