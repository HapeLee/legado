package io.legato.kazusa.base

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class BaseBottomSheetDialogFragment(
    @LayoutRes layoutID: Int
) : BottomSheetDialogFragment(layoutID) {

    private var onDismissListener: OnDismissListener? = null

    fun setOnDismissListener(onDismissListener: OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val screenHeight = resources.displayMetrics.heightPixels
            (dialog as? BottomSheetDialog)?.behavior?.apply {
                peekHeight = (screenHeight * 0.7f).toInt()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onFragmentCreated(view, savedInstanceState)
        observeLiveBus()
    }

    abstract fun onFragmentCreated(view: View, savedInstanceState: Bundle?)

    override fun show(manager: FragmentManager, tag: String?) {
        kotlin.runCatching {
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        }.onFailure {
            AppLog.put("显示BottomSheetDialog失败 tag:$tag\n$it")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
    }

    fun <T> execute(
        scope: CoroutineScope = lifecycleScope,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> T
    ) = Coroutine.async(scope, context) { block() }

    open fun observeLiveBus() {
    }

}