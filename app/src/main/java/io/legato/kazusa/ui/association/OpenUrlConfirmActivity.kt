package io.legato.kazusa.ui.association

import android.os.Bundle
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.constant.SourceType
import io.legato.kazusa.databinding.ActivityTranslucenceBinding
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

class OpenUrlConfirmActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("uri")?.let {
            val mimeType = intent.getStringExtra("mimeType")
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            val sourceType = intent.getIntExtra("sourceType", SourceType.book)
            showDialogFragment(OpenUrlConfirmDialog(it, mimeType, sourceOrigin, sourceName, sourceType))
        } ?: finish()
    }

}
