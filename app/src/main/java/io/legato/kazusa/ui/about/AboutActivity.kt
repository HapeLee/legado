package io.legato.kazusa.ui.about

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.databinding.ActivityAboutBinding
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.filletBackground
import io.legato.kazusa.utils.openUrl
import io.legato.kazusa.utils.share
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding


class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override val binding by viewBinding(ActivityAboutBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        //binding.llAbout.background = filletBackground
        val fTag = "aboutFragment"
        var aboutFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (aboutFragment == null) aboutFragment = AboutFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, aboutFragment, fTag)
            .commit()
        binding.tvAppSummary.post {
            kotlin.runCatching {
                val typedValue = TypedValue()
                val context = binding.root.context
                context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary,
                    typedValue,
                    true
                )
                val accentColor = typedValue.data
                val spannableString = SpannableString(binding.tvAppSummary.text)
                val gzh = getString(R.string.legado_gzh)
                val start = spannableString.indexOf(gzh)
                spannableString.setSpan(
                    accentColor, start, start + gzh.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.tvAppSummary.text = spannableString
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.about, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scoring -> openUrl("market://details?id=$packageName")
            R.id.menu_share_it -> share(
                getString(R.string.app_share_description),
                getString(R.string.app_name)
            )
        }
        return super.onCompatOptionsItemSelected(item)
    }

}
