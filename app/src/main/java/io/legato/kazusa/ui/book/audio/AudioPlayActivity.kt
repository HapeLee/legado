package io.legato.kazusa.ui.book.audio

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseActivity
import io.legato.kazusa.constant.BookType
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.constant.Status
import io.legato.kazusa.constant.Theme
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookChapter
import io.legato.kazusa.data.entities.BookSource
import io.legato.kazusa.databinding.ActivityAudioPlayBinding
import io.legato.kazusa.help.book.isAudio
import io.legato.kazusa.help.book.removeType
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.lib.dialogs.alert
import io.legato.kazusa.model.AudioPlay
import io.legato.kazusa.model.BookCover
import io.legato.kazusa.service.AudioPlayService
import io.legato.kazusa.ui.about.AppLogDialog
import io.legato.kazusa.ui.book.changesource.ChangeBookSourceDialog
import io.legato.kazusa.ui.book.source.edit.BookSourceEditActivity
import io.legato.kazusa.ui.book.toc.TocActivityResult
import io.legato.kazusa.ui.login.SourceLoginActivity
import io.legato.kazusa.utils.StartActivityContract
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.dpToPx
import io.legato.kazusa.utils.invisible
import io.legato.kazusa.utils.observeEvent
import io.legato.kazusa.utils.observeEventSticky
import io.legato.kazusa.utils.sendToClip
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.startActivityForBook
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import io.legato.kazusa.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onLongClick
import java.util.Locale

/**
 * 音频播放
 */
@SuppressLint("ObsoleteSdkInt")
class AudioPlayActivity :
    VMBaseActivity<ActivityAudioPlayBinding, AudioPlayViewModel>(toolBarTheme = Theme.Dark),
    ChangeBookSourceDialog.CallBack,
    AudioPlay.CallBack {

    override val binding by viewBinding(ActivityAudioPlayBinding::inflate)
    override val viewModel by viewModels<AudioPlayViewModel>()
    private val timerSliderPopup by lazy { TimerSliderPopup(this) }
    private var adjustProgress = false

    private val progressTimeFormat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("mm:ss", Locale.getDefault())
        } else {
            java.text.SimpleDateFormat("mm:ss", Locale.getDefault())
        }
    }
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            if (it.first != AudioPlay.book?.durChapterIndex
                || it.second == 0
            ) {
                AudioPlay.skipTo(it.first)
            }
        }
    }
    private val sourceEditResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource()
            }
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        AudioPlay.register(this)
        viewModel.titleData.observe(this) {
            binding.titleBar.title = it
        }
        viewModel.coverData.observe(this) {
            upCover(it)
        }
        viewModel.initData(intent)
        initView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.audio_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !AudioPlay.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_wake_lock)?.isChecked = AppConfig.audioPlayUseWakeLock
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> AudioPlay.book?.let {
                showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
            }

            R.id.menu_login -> AudioPlay.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                }
            }

            R.id.menu_wake_lock -> AppConfig.audioPlayUseWakeLock = !AppConfig.audioPlayUseWakeLock
            R.id.menu_copy_audio_url -> sendToClip(AudioPlayService.url)
            R.id.menu_edit_source -> AudioPlay.bookSource?.let {
                sourceEditResult.launch {
                    putExtra("sourceUrl", it.bookSourceUrl)
                }
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.fabPlayStop.setOnClickListener {
            playButton()
        }
        binding.fabPlayStop.onLongClick {
            AudioPlay.stop()
        }
        binding.ivSkipNext.setOnClickListener {
            AudioPlay.next()
        }
        binding.ivSkipPrevious.setOnClickListener {
            AudioPlay.prev()
        }

//        binding.playerProgress.setOnSeekBarChangeListener(object : SeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                binding.tvDurTime.text = progressTimeFormat.format(progress.toLong())
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar) {
//                adjustProgress = true
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                adjustProgress = false
//                AudioPlay.adjustProgress(seekBar.progress)
//            }
//        })

        // 替换原来的 SeekBar 监听器
        binding.playerProgress.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvDurTime.text = progressTimeFormat.format(value.toLong())
            }
        }

        binding.playerProgress.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                adjustProgress = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                adjustProgress = false
                AudioPlay.adjustProgress(slider.value.toInt())
            }
        })

        binding.ivChapter.setOnClickListener {
            AudioPlay.book?.let {
                tocActivityResult.launch(it.bookUrl)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            binding.ivFastRewind.invisible()
            binding.ivFastForward.invisible()
        }
        binding.ivFastForward.setOnClickListener {
            AudioPlay.adjustSpeed(0.1f)
        }
        binding.ivFastRewind.setOnClickListener {
            AudioPlay.adjustSpeed(-0.1f)
        }
        binding.ivTimer.setOnClickListener {
            timerSliderPopup.showAsDropDown(it, 0, (-100).dpToPx(), Gravity.TOP)
        }
        binding.llPlayMenu.applyNavigationBarPadding()
    }

    private fun upCover(path: String?) {
        BookCover.load(this, path, sourceOrigin = AudioPlay.bookSource?.bookSourceUrl) {
            BookCover.loadBlur(this, path, sourceOrigin = AudioPlay.bookSource?.bookSourceUrl)
                .into(binding.ivBg)
        }.into(binding.ivCover)
    }

    private fun playButton() {
        when (AudioPlay.status) {
            Status.PLAY -> AudioPlay.pause(this)
            Status.PAUSE -> AudioPlay.resume(this)
            else -> AudioPlay.loadOrUpPlayUrl()
        }
    }

    override val oldBook: Book?
        get() = AudioPlay.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isAudio) {
            viewModel.changeTo(source, book, toc)
        } else {
            AudioPlay.stop()
            lifecycleScope.launch {
                withContext(IO) {
                    AudioPlay.book?.migrateTo(book, toc)
                    book.removeType(BookType.updateError)
                    AudioPlay.book?.delete()
                    appDb.bookDao.insert(book)
                }
                startActivityForBook(book)
                finish()
            }
        }
    }

    override fun finish() {
        val book = AudioPlay.book ?: return super.finish()

        if (AudioPlay.inBookshelf) {
            return super.finish()
        }

        if (!AppConfig.showAddToShelfAlert) {
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    AudioPlay.book?.removeType(BookType.notShelf)
                    AudioPlay.book?.save()
                    AudioPlay.inBookshelf = true
                    setResult(RESULT_OK)
                }
                noButton { viewModel.removeFromBookshelf { super.finish() } }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AudioPlay.status != Status.PLAY) {
            AudioPlay.stop()
        }
        AudioPlay.unregister(this)
    }

    @SuppressLint("SetTextI18n")
    override fun observeLiveBus() {
        observeEvent<Boolean>(EventBus.MEDIA_BUTTON) {
            if (it) {
                playButton()
            }
        }
        observeEventSticky<Int>(EventBus.AUDIO_STATE) {
            AudioPlay.status = it
            if (it == Status.PLAY) {
                binding.fabPlayStop.setImageResource(R.drawable.ic_pause_24dp)
            } else {
                binding.fabPlayStop.setImageResource(R.drawable.ic_play_24dp)
            }
        }
        observeEventSticky<String>(EventBus.AUDIO_SUB_TITLE) {
            binding.tvSubTitle.text = it
            binding.ivSkipPrevious.isEnabled = AudioPlay.durChapterIndex > 0
            binding.ivSkipNext.isEnabled =
                AudioPlay.durChapterIndex < AudioPlay.simulatedChapterSize - 1
        }
//        observeEventSticky<Int>(EventBus.AUDIO_SIZE) {
//            binding.playerProgress.max = it
//            binding.tvAllTime.text = progressTimeFormat.format(it.toLong())
//        }
//        observeEventSticky<Int>(EventBus.AUDIO_PROGRESS) {
//            if (!adjustProgress) binding.playerProgress.progress = it
//            binding.tvDurTime.text = progressTimeFormat.format(it.toLong())
//        }
//        observeEventSticky<Int>(EventBus.AUDIO_BUFFER_PROGRESS) {
//            binding.playerProgress.secondaryProgress = it
//
//        }
        observeEventSticky<Int>(EventBus.AUDIO_SIZE) {
            binding.playerProgress.valueTo = it.toFloat()
            binding.tvAllTime.text = progressTimeFormat.format(it.toLong())
        }

        observeEventSticky<Int>(EventBus.AUDIO_PROGRESS) {
            if (!adjustProgress) binding.playerProgress.value = it.toFloat()
            binding.tvDurTime.text = progressTimeFormat.format(it.toLong())
        }

        observeEventSticky<Int>(EventBus.AUDIO_BUFFER_PROGRESS) {
            //updateBufferProgress(it)
        }

        observeEventSticky<Float>(EventBus.AUDIO_SPEED) {
            binding.tvSpeed.text = String.format(Locale.ROOT, "%.1fX", it)
            binding.tvSpeed.visible()
        }
        observeEventSticky<Int>(EventBus.AUDIO_DS) {
            binding.tvTimer.text = "${it}m"
            binding.tvTimer.visible(it > 0)
        }
    }

    override fun upLoading(loading: Boolean) {
        runOnUiThread {
            binding.progressLoading.visible(loading)
        }
    }

}