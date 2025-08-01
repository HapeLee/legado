package io.legato.kazusa.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import io.legato.kazusa.R
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.databinding.ViewReadMenuBinding
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.config.LocalConfig
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.help.source.getSourceType
import io.legato.kazusa.lib.dialogs.alert
import io.legato.kazusa.model.ReadBook
import io.legato.kazusa.ui.browser.WebViewActivity
import io.legato.kazusa.ui.widget.seekbar.SeekBarChangeListener
import io.legato.kazusa.utils.ConstraintModify
import io.legato.kazusa.utils.VibrationUtils
import io.legato.kazusa.utils.activity
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.dpToPx
import io.legato.kazusa.utils.getPrefBoolean
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.invisible
import io.legato.kazusa.utils.loadAnimation
import io.legato.kazusa.utils.modifyBegin
import io.legato.kazusa.utils.openUrl
import io.legato.kazusa.utils.putPrefBoolean
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.themeColor
import io.legato.kazusa.utils.visible
import splitties.views.onClick
import splitties.views.onLongClick

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var canShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private var confirmSkipToChapter: Boolean = false
    private var isMenuOutAnimating = false

    private val menuTopIn: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_top_in)
    }
    private val menuTopOut: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_top_out)
    }
    private val menuBottomIn: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_bottom_in)
    }
    private val menuBottomOut: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_bottom_out)
    }

    private val fadeIn = AlphaAnimation(0f, 1f).apply {
        duration = 280
        fillAfter = true
    }

    private val fadeOut = AlphaAnimation(1f, 0f).apply {
        duration = 280
        fillAfter = true
    }

//    private val immersiveMenu: Boolean
//        get() = AppConfig.readBarStyleFollowPage && ReadBookConfig.durConfig.curBgType() == 0
//    private var bgColor: Int = if (immersiveMenu) {
//        kotlin.runCatching {
//            ReadBookConfig.durConfig.curBgStr().toColorInt()
//        }.getOrDefault(context.bottomBackground)
//    } else {
//        context.bottomBackground
//    }
//    private var textColor: Int = if (immersiveMenu) {
//        ReadBookConfig.durConfig.curTextColor()
//    } else {
//        context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
//    }

//    private var bottomBackgroundList: ColorStateList = Selector.colorBuild()
//        .setDefaultColor(bgColor)
//        .setPressedColor(ColorUtils.darkenColor(bgColor))
//        .create()
    private var onMenuOutEnd: (() -> Unit)? = null

    private val showBrightnessView
        get() = context.getPrefBoolean(
            PreferKey.showBrightnessView,
            true
        )

    private val sourceMenu by lazy {
        PopupMenu(context, binding.tvSourceAction).apply {
            inflate(R.menu.book_read_source)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_login -> callBack.showLogin()
                    R.id.menu_chapter_pay -> callBack.payAction()
                    R.id.menu_edit_source -> callBack.openSourceEditActivity()
                    R.id.menu_disable_source -> callBack.disableSource()
                }
                true
            }
        }
    }

    private val menuInListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            binding.tvSourceAction.text =
                ReadBook.bookSource?.bookSourceName ?: context.getString(R.string.book_source)
            binding.tvSourceAction.isGone = ReadBook.isLocalBook
            callBack.upSystemUiVisibility()
            binding.llBrightness.visible(showBrightnessView)
        }

        @SuppressLint("RtlHardcoded")
        override fun onAnimationEnd(animation: Animation) {
            binding.vwMenuBg.setOnClickListener { runMenuOut() }
            callBack.upSystemUiVisibility()
            if (!LocalConfig.readMenuHelpVersionIsLast) {
                callBack.showHelp()
            }
        }

        override fun onAnimationRepeat(animation: Animation) = Unit
    }
    private val menuOutListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            isMenuOutAnimating = true
            binding.vwMenuBg.setOnClickListener(null)
        }

        override fun onAnimationEnd(animation: Animation) {
            this@ReadMenu.invisible()
            binding.titleBar.invisible()
            binding.bottomMenu.invisible()
            canShowMenu = false
            isMenuOutAnimating = false
            onMenuOutEnd?.invoke()
            callBack.upSystemUiVisibility()
        }

        override fun onAnimationRepeat(animation: Animation) = Unit
    }

    init {
        initView()
        upBrightnessState()
        bindEvent()
    }

    private fun initView() = binding.run {
//        if (AppConfig.isNightTheme) {
//            fabNightTheme.setIconResource(R.drawable.ic_daytime)
//        } else {
//            fabNightTheme.setIconResource(R.drawable.ic_brightness)
//        }
        initAnimation()
//        if (immersiveMenu) {
////            val lightTextColor = ColorUtils.withAlpha(ColorUtils.lightenColor(textColor), 0.75f)
////            titleBar.setTextColor(textColor)
////            titleBar.setBackgroundColor(bgColor)
////            titleBar.setColorFilter(textColor)
////            tvChapterName.setTextColor(lightTextColor)
////            tvChapterUrl.setTextColor(lightTextColor)
//        } else if (reset) {
////            val bgColor = context.primaryColor
////            val textColor = context.primaryTextColor
////            titleBar.setTextColor(textColor)
////            titleBar.setBackgroundColor(bgColor)
////            titleBar.setColorFilter(textColor)
////            tvChapterName.setTextColor(textColor)
////            tvChapterUrl.setTextColor(textColor)
//        }
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dpToPx()
       //brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        llBrightness.background = brightnessBackground
        if (AppConfig.isEInkMode) {
            titleBar.setBackgroundResource(R.drawable.bg_eink_border_bottom)
            llBottomBg.setBackgroundResource(R.drawable.bg_eink_border_top)
        } else {
            //llBottomBg.setBackgroundColor(bgColor)
        }
//        fabSearch.backgroundTintList = bottomBackgroundList
//        fabSearch.setColorFilter(textColor)
//        fabAutoPage.backgroundTintList = bottomBackgroundList
//        fabAutoPage.setColorFilter(textColor)
//        fabReplaceRule.backgroundTintList = bottomBackgroundList
//        fabReplaceRule.setColorFilter(textColor)
//        fabNightTheme.backgroundTintList = bottomBackgroundList
//        fabNightTheme.setColorFilter(textColor)
        //tvPre.setTextColor(textColor)
        //tvNext.setTextColor(textColor)
//        ivCatalog.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvCatalog.setTextColor(textColor)
//        ivReadAloud.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvReadAloud.setTextColor(textColor)
//        ivFont.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvFont.setTextColor(textColor)
//        ivSetting.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvSetting.setTextColor(textColor)
//        vwBrightnessPosAdjust.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        llBrightness.setOnClickListener(null)
        seekBrightness.post {
            seekBrightness.progress = AppConfig.readBrightness
        }
        if (AppConfig.showReadTitleBarAddition) {
            titleBarAddition.visible()
        } else {
            titleBarAddition.gone()
        }
        upBrightnessVwPos()
        /**
         * 确保视图不被导航栏遮挡
         */
        binding.bottomView.applyNavigationBarPadding()
    }

    fun reset() {
        upColorConfig()
        initView()
    }

    fun refreshMenuColorFilter() {
//        if (immersiveMenu) {
//            //binding.titleBar.setColorFilter(textColor)
//        }
    }

    private fun upColorConfig() {
//        bgColor = if (immersiveMenu) {
//            kotlin.runCatching {
//                Color.parseColor(ReadBookConfig.durConfig.curBgStr())
//            }.getOrDefault(context.bottomBackground)
//        } else {
//            context.bottomBackground
//        }
//        textColor = if (immersiveMenu) {
//            ReadBookConfig.durConfig.curTextColor()
//        } else {
//            context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
//        }
//        bottomBackgroundList = Selector.colorBuild()
//            .setDefaultColor(bgColor)
//            .setPressedColor(ColorUtils.darkenColor(bgColor))
//            .create()
    }

    fun upBrightnessState() {
        if (brightnessAuto()) {
            binding.ivBrightnessAuto.setColorFilter(context.themeColor(androidx.appcompat.R.attr.colorPrimary))
            binding.seekBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.setColorFilter(context.themeColor(com.google.android.material.R.attr.colorOnSurface))
            binding.seekBrightness.isEnabled = true
        }
        setScreenBrightness(AppConfig.readBrightness.toFloat())
    }

    /**
     * 设置屏幕亮度
     */
    fun setScreenBrightness(value: Float) {
        activity?.run {
            var brightness = BRIGHTNESS_OVERRIDE_NONE
            if (!brightnessAuto() && value != BRIGHTNESS_OVERRIDE_NONE) {
                brightness = value
                if (brightness < 1f) brightness = 1f
                brightness /= 255f
            }
            val params = window.attributes
            params.screenBrightness = brightness
            window.attributes = params
        }
    }

    fun runMenuIn(anim: Boolean = !AppConfig.isEInkMode) {
        callBack.onMenuShow()
        this.visible()
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        if (anim) {
            binding.titleBar.startAnimation(menuTopIn)
            binding.bottomMenu.startAnimation(menuBottomIn)
            updateBrightnessVisibility(true)
        } else {
            menuInListener.onAnimationStart(menuBottomIn)
            menuInListener.onAnimationEnd(menuBottomIn)
        }
    }

    fun runMenuOut(anim: Boolean = !AppConfig.isEInkMode, onMenuOutEnd: (() -> Unit)? = null) {
        if (isMenuOutAnimating) {
            return
        }
        callBack.onMenuHide()
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            if (anim) {
                binding.titleBar.startAnimation(menuTopOut)
                binding.bottomMenu.startAnimation(menuBottomOut)
                updateBrightnessVisibility(false)

            } else {
                menuOutListener.onAnimationStart(menuBottomOut)
                menuOutListener.onAnimationEnd(menuBottomOut)
            }
        }
    }

    fun updateBrightnessVisibility(boolean: Boolean) {
        if (showBrightnessView) {
            if(boolean){
                binding.llBrightness.startAnimation(fadeIn)
            }else{
                binding.llBrightness.startAnimation(fadeOut)
            }
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun bindEvent() = binding.run {
        vwMenuBg.setOnClickListener { runMenuOut() }
        titleBar.toolbar.setOnClickListener {
            callBack.openBookInfoActivity()
        }
        val chapterViewClickListener = OnClickListener {
            if (ReadBook.isLocalBook) {
                return@OnClickListener
            }
            if (AppConfig.readUrlInBrowser) {
                context.openUrl(tvChapterUrl.text.toString().substringBefore(",{"))
            } else {
                Coroutine.async {
                    context.startActivity<WebViewActivity> {
                        val url = tvChapterUrl.text.toString()
                        val bookSource = ReadBook.bookSource
                        putExtra("title", tvChapterName.text)
                        putExtra("url", url)
                        putExtra("sourceOrigin", bookSource?.bookSourceUrl)
                        putExtra("sourceName", bookSource?.bookSourceName)
                        putExtra("sourceType", bookSource?.getSourceType())
                    }
                }
            }
        }
        val chapterViewLongClickListener = OnLongClickListener {
            if (ReadBook.isLocalBook) {
                return@OnLongClickListener true
            }
            context.alert(R.string.open_fun) {
                setMessage(R.string.use_browser_open)
                okButton {
                    AppConfig.readUrlInBrowser = true
                }
                noButton {
                    AppConfig.readUrlInBrowser = false
                }
            }
            true
        }
        tvChapterName.setOnClickListener(chapterViewClickListener)
        tvChapterName.setOnLongClickListener(chapterViewLongClickListener)
        //书源操作
        tvSourceAction.onClick {
            sourceMenu.menu.findItem(R.id.menu_login).isVisible =
                !ReadBook.bookSource?.loginUrl.isNullOrEmpty()
            sourceMenu.menu.findItem(R.id.menu_chapter_pay).isVisible =
                !ReadBook.bookSource?.loginUrl.isNullOrEmpty()
                        && ReadBook.curTextChapter?.isVip == true
                        && ReadBook.curTextChapter?.isPay != true
            sourceMenu.show()
        }
        //亮度跟随
        ivBrightnessAuto.setOnClickListener {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seekBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setScreenBrightness(progress.toFloat())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppConfig.readBrightness = seekBar.progress
            }

        })

        vwBrightnessPosAdjust.setOnClickListener {
            AppConfig.brightnessVwPos = !AppConfig.brightnessVwPos
            upBrightnessVwPos()
        }
        seekReadPage.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                VibrationUtils.vibrate(context, 12)
                if (AppConfig.progressBarBehavior == "page") ReadBook.skipToPage(slider.value.toInt() - 1)
                ReadBook.skipToPage(slider.value.toInt() - 1)
            }
        }

        //阅读进度
        seekReadPage.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                vwMenuBg.setOnClickListener(null)
                VibrationUtils.vibrate(context, 16)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                vwMenuBg.setOnClickListener { runMenuOut() }

                val progress = slider.value.toInt()
                when (AppConfig.progressBarBehavior) {
                    "page" -> ReadBook.skipToPage(progress - 1)
                    "chapter" -> {
                        if (confirmSkipToChapter) {
                            callBack.skipToChapter(progress - 1)
                        } else {
                            context.alert("章节跳转确认", "确定要跳转章节吗？") {
                                yesButton {
                                    confirmSkipToChapter = true
                                    callBack.skipToChapter(progress - 1)
                                }
                                noButton {
                                    upSeekBar()
                                }
                                onCancelled {
                                    upSeekBar()
                                }
                            }
                        }
                    }
                }
            }
        })

        //搜索
        fabSearch.setOnClickListener {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
        }

        //自动翻页
        fabAutoPage.setOnClickListener {
            runMenuOut {
                callBack.autoPage()
            }
        }

        //替换
        //fabReplaceRule.setOnClickListener { callBack.openReplaceRule() }

        //夜间模式
//        fabNightTheme.setOnClickListener {
//            AppConfig.isNightTheme = !AppConfig.isNightTheme
//            ThemeConfig.applyDayNight(context)
//        }

        //上一章
        tvPre.setOnClickListener { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.setOnClickListener { ReadBook.moveToNextChapter(true) }

        //目录
        ivCatalog.setOnClickListener {
            runMenuOut {
                callBack.openChapterList()
            }
        }

        //朗读
        ivReadAloud.setOnClickListener {
            runMenuOut {
                callBack.onClickReadAloud()
            }
        }

        ivReadAloud.onLongClick {
            runMenuOut {
                callBack.showReadAloudDialog()
            }
        }

        //设置
        ivSetting.setOnClickListener {
            runMenuOut {
                callBack.showReadStyle()
            }
        }
    }

    private fun initAnimation() {
        menuTopIn.setAnimationListener(menuInListener)
        menuTopOut.setAnimationListener(menuOutListener)
    }

    fun upBookView() {
        binding.titleBar.title = " "
        binding.tvBookName.text = ReadBook.book?.name
        ReadBook.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            if (!ReadBook.isLocalBook) {
                binding.tvChapterUrl.text = it.chapter.getAbsoluteURL()
                //binding.tvChapterUrl.visible()
            } else {
                binding.tvChapterUrl.gone()
            }
            upSeekBar()
            binding.tvPre.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvNext.isEnabled = ReadBook.durChapterIndex != ReadBook.simulatedChapterSize - 1
        } ?: let {
            binding.tvChapterUrl.gone()
        }
    }

    fun upSeekBar() {
        binding.seekReadPage.apply {
            when (AppConfig.progressBarBehavior) {
                "page" -> {
                    ReadBook.curTextChapter?.let { chapter ->
                        if (chapter.pageSize >= 0 && ReadBook.durPageIndex >= 0) {
                            valueFrom = 1f
                            valueTo = chapter.pageSize.toFloat().coerceAtLeast(2f)
                            stepSize = 1f
                            value = (ReadBook.durPageIndex).coerceIn(1, chapter.pageSize).toFloat()
                        } else {
                            value = 0f
                            valueFrom = 0f
                            valueTo = 1000f
                        }
                    }
                }

                "chapter" -> {
                    if (ReadBook.simulatedChapterSize > 0)
                    {
                        valueFrom = 1f
                        valueTo = ReadBook.simulatedChapterSize.toFloat().coerceAtLeast(2f)
                        stepSize = 1f
                        value = (ReadBook.durChapterIndex).coerceIn(1, ReadBook.simulatedChapterSize).toFloat()
                    } else {
                        valueFrom = 0f
                        value = 0f
                        valueTo = 1000f
                    }
                }
            }
        }
    }

//    fun upSeekBar() {
//        binding.seekReadPage.apply {
//            when (AppConfig.progressBarBehavior) {
//                "page" -> {
//                    ReadBook.curTextChapter?.let {
//                        max = it.pageSize.minus(1)
//                        progress = ReadBook.durPageIndex
//                    }
//                }
//
//                "chapter" -> {
//                    max = ReadBook.simulatedChapterSize - 1
//                    progress = ReadBook.durChapterIndex
//                }
//            }
//        }
//    }

    fun setSeekPage(seek: Int) {
        binding.seekReadPage.value = seek.toFloat() + 1
    }

    fun setAutoPage(autoPage: Boolean) = binding.run {
        if (autoPage) {
            fabAutoPage.setIconResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setIconResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        //fabAutoPage.setColorFilter(textColor)
    }

    private fun upBrightnessVwPos() {
        if (AppConfig.brightnessVwPos) {
            binding.root.modifyBegin()
                .clear(R.id.ll_brightness, ConstraintModify.Anchor.LEFT)
                .rightToRightOf(R.id.ll_brightness, R.id.vw_menu_root)
                .commit()
        } else {
            binding.root.modifyBegin()
                .clear(R.id.ll_brightness, ConstraintModify.Anchor.RIGHT)
                .leftToLeftOf(R.id.ll_brightness, R.id.vw_menu_root)
                .commit()
        }
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun openBookInfoActivity()
        fun showReadStyle()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
        fun skipToChapter(index: Int)
        fun onMenuShow()
        fun onMenuHide()
    }

}
