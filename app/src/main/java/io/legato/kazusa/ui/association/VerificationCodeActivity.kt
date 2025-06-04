package io.legato.kazusa.ui.association

import android.os.Bundle
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.constant.SourceType
import io.legato.kazusa.databinding.ActivityTranslucenceBinding
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            val sourceType = intent.getIntExtra("sourceType", SourceType.book)
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName, sourceType)
            )
        } ?: finish()
    }

}