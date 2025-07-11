package io.legato.kazusa.ui.about

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.ReadRecordShow
import io.legato.kazusa.databinding.ActivityReadRecordBinding
import io.legato.kazusa.databinding.ItemReadRecordBinding
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.config.LocalConfig
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.primaryTextColor
import io.legato.kazusa.ui.book.search.SearchActivity
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.cnCompare
import io.legato.kazusa.utils.getInt
import io.legato.kazusa.utils.putInt
import io.legato.kazusa.utils.startActivityForBook
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ReadRecordActivity : BaseActivity<ActivityReadRecordBinding>() {

    private val adapter by lazy { RecordAdapter(this) }
    private var sortMode
        get() = LocalConfig.getInt("readRecordSort")
        set(value) {
            LocalConfig.putInt("readRecordSort", value)
        }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }

    override val binding by viewBinding(ActivityReadRecordBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initAllTime()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_read_record, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_enable_record)?.isChecked = AppConfig.enableReadRecord
        when (sortMode) {
            1 -> menu.findItem(R.id.menu_sort_read_long)?.isChecked = true
            2 -> menu.findItem(R.id.menu_sort_read_time)?.isChecked = true
            else -> menu.findItem(R.id.menu_sort_name)?.isChecked = true
        }
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                sortMode = 0
                item.isChecked = true
                initData()
            }

            R.id.menu_sort_read_long -> {
                sortMode = 1
                item.isChecked = true
                initData()
            }

            R.id.menu_sort_read_time -> {
                sortMode = 2
                item.isChecked = true
                initData()
            }

            R.id.menu_enable_record -> {
                AppConfig.enableReadRecord = !item.isChecked
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        initSearchView()
        binding.tvBookName.setText(R.string.all_read_time)
        binding.tvRemove.setOnClickListener {
            alert(R.string.delete, R.string.sure_del) {
                yesButton {
                    appDb.readRecordDao.clear()
                    initData()
                }
                noButton()
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
    }

    private fun initSearchView() {
        //searchView.applyTint(primaryTextColor)
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initData(newText)
                return false
            }
        })
    }

    private fun initAllTime() {
        lifecycleScope.launch {
            val allTime = withContext(IO) {
                appDb.readRecordDao.allTime
            }
            binding.tvReadingTime.text = formatDuring(allTime)
        }
    }

    private fun initData(searchKey: String? = null) {
        lifecycleScope.launch {
            val readRecords = withContext(IO) {
                appDb.readRecordDao.search(searchKey ?: "").let { records ->
                    when (sortMode) {
                        1 -> records.sortedByDescending { it.readTime }
                        2 -> records.sortedByDescending { it.lastRead }
                        else -> records.sortedWith { o1, o2 ->
                            o1.bookName.cnCompare(o2.bookName)
                        }
                    }
                }
            }
            adapter.setItems(readRecords)
        }
    }

    inner class RecordAdapter(context: Context) :
        RecyclerAdapter<ReadRecordShow, ItemReadRecordBinding>(context) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        override fun getViewBinding(parent: ViewGroup): ItemReadRecordBinding {
            return ItemReadRecordBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemReadRecordBinding,
            item: ReadRecordShow,
            payloads: MutableList<Any>,
        ) {
            binding.apply {
                tvBookName.text = item.bookName
                tvReadingTime.text = formatDuring(item.readTime)
                if (item.lastRead > 0) {
                    tvLastReadTime.text = dateFormat.format(item.lastRead)
                } else {
                    tvLastReadTime.text = ""
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemReadRecordBinding) {
            binding.apply {
                root.setOnClickListener {
                    val item = getItem(holder.layoutPosition) ?: return@setOnClickListener
                    lifecycleScope.launch {
                        val book = withContext(IO) {
                            appDb.bookDao.findByName(item.bookName).firstOrNull()
                        }
                        if (book == null) {
                            SearchActivity.start(this@ReadRecordActivity, item.bookName)
                        } else {
                            startActivityForBook(book)
                        }
                    }
                }
                tvRemove.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { item ->
                        sureDelAlert(item)
                    }
                }
            }
        }

        private fun sureDelAlert(item: ReadRecordShow) {
            alert(R.string.delete) {
                setMessage(getString(R.string.sure_del_any, item.bookName))
                yesButton {
                    appDb.readRecordDao.deleteByName(item.bookName)
                    initData()
                }
                noButton()
            }
        }

    }

    fun formatDuring(mss: Long): String {
        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val d = if (days > 0) "${days}天" else ""
        val h = if (hours > 0) "${hours}小时" else ""
        val m = if (minutes > 0) "${minutes}分钟" else ""
        val s = if (seconds > 0) "${seconds}秒" else ""
        var time = "$d$h$m$s"
        if (time.isBlank()) {
            time = "0秒"
        }
        return time
    }

}