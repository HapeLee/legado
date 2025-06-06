@file:Suppress("unused")

package io.legato.kazusa.utils

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
object DrawableUtils {

    fun createTransitionDrawable(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ): TransitionDrawable {
        return createTransitionDrawable(ColorDrawable(startColor), ColorDrawable(endColor))
    }

    fun createTransitionDrawable(start: Drawable, end: Drawable): TransitionDrawable {
        val drawables = arrayOfNulls<Drawable>(2)

        drawables[0] = start
        drawables[1] = end

        return TransitionDrawable(drawables)
    }

}

fun Drawable.setTintListMutate(
    tint: ColorStateList,
    tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP
) {
    val wrappedDrawable = DrawableCompat.wrap(this)
    wrappedDrawable.mutate()
    DrawableCompat.setTintMode(wrappedDrawable, tintMode)
    DrawableCompat.setTintList(wrappedDrawable, tint)
}

fun Drawable.setTintMutate(
    @ColorInt tint: Int,
    tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP
) {
    val wrappedDrawable = DrawableCompat.wrap(this)
    wrappedDrawable.mutate()
    DrawableCompat.setTintMode(wrappedDrawable, tintMode)
    DrawableCompat.setTint(wrappedDrawable, tint)
}