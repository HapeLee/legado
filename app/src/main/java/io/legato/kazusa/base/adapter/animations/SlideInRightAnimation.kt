package io.legato.kazusa.base.adapter.animations

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View


class SlideInRightAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> =
        arrayOf(ObjectAnimator.ofFloat(view, "translationX", view.rootView.width.toFloat(), 0f))
}
