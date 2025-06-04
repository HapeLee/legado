package io.legato.kazusa.ui.qrcode

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.Result
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.databinding.ActivityQrcodeCaptureBinding
import io.legato.kazusa.utils.QRCodeUtils
import io.legato.kazusa.utils.SelectImageContract
import io.legato.kazusa.utils.launch
import io.legato.kazusa.utils.readBytes
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

class QrCodeActivity : BaseActivity<ActivityQrcodeCaptureBinding>(), ScanResultCallback {

    override val binding by viewBinding(ActivityQrcodeCaptureBinding::inflate)

    private val selectQrImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.readBytes(this)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            onScanResultCallback(QRCodeUtils.parseCodeResult(bitmap))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "qrCodeFragment"
        val qrCodeFragment = QrCodeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_content, qrCodeFragment, fTag)
            .commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.qr_code_scan, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_choose_from_gallery -> selectQrImage.launch()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onScanResultCallback(result: Result?) {
        val intent = Intent()
        intent.putExtra("result", result?.text)
        setResult(RESULT_OK, intent)
        finish()
    }

}