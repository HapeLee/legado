package io.legato.kazusa.ui.file

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseActivity
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.databinding.ActivityFileManageBinding
import io.legato.kazusa.databinding.ItemFileBinding
import io.legato.kazusa.databinding.ItemPathPickerBinding
//import io.legado.app.lib.theme.primaryTextColor
import io.legato.kazusa.ui.file.utils.FilePickerIcon
import io.legato.kazusa.ui.widget.recycler.VerticalDivider
import io.legato.kazusa.utils.ConvertUtils
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.openFileUri
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import java.io.File

class FileManageActivity : VMBaseActivity<ActivityFileManageBinding, FileManageViewModel>() {

    override val binding by viewBinding(ActivityFileManageBinding::inflate)
    override val viewModel by viewModels<FileManageViewModel>()
    private val dirParent = ".."
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val pathAdapter by lazy {
        PathAdapter()
    }
    private val fileAdapter by lazy {
        FileAdapter()
    }
    private val currentFiles = arrayListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initSearchView()
        viewModel.upFiles(viewModel.rootDoc)
    }

    private fun initView() {
        binding.rvPath.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvPath.adapter = pathAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = fileAdapter
        binding.recyclerView.applyNavigationBarPadding()
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.lastDir != viewModel.rootDoc) {
                gotoLastDir()
                return@addCallback
            }
            finish()
        }
    }

    private fun initSearchView() {
        //searchView.applyTint(primaryTextColor)
        searchView.queryHint = getString(R.string.screen) + " • " + getString(R.string.file_manage)
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                updateFiles()
                return false
            }
        })
    }

    private fun updateFiles() {
        if (searchView.query.isNotEmpty()) {
            currentFiles.filter {
                it.name == dirParent || it.name.contains(searchView.query)
            }.let {
                fileAdapter.setItems(it)
            }
        } else {
            fileAdapter.setItems(currentFiles)
        }
    }

    private fun gotoLastDir() {
        viewModel.subDocs.removeLastOrNull()
        pathAdapter.setItems(viewModel.subDocs)
        viewModel.upFiles(viewModel.lastDir)
    }

    override fun observeLiveBus() {
        viewModel.filesLiveData.observe(this) {
            searchView.setQuery("", false)
            currentFiles.clear()
            currentFiles.addAll(it)
            updateFiles()
        }
    }

    @SuppressLint("SetTextI18n")
    inner class PathAdapter :
        RecyclerAdapter<File, ItemPathPickerBinding>(this@FileManageActivity) {

        private val arrowIcon = ConvertUtils.toDrawable(FilePickerIcon.getArrow())

        init {
            addHeaderView {
                ItemPathPickerBinding.inflate(inflater, it, false).apply {
                    textView.text = "root"
                    imageView.setImageDrawable(arrowIcon)
                    root.setOnClickListener {
                        viewModel.subDocs.clear()
                        setItems(viewModel.subDocs)
                        viewModel.upFiles(viewModel.rootDoc)
                    }
                }
            }
        }

        override fun getViewBinding(parent: ViewGroup): ItemPathPickerBinding {
            return ItemPathPickerBinding.inflate(inflater, parent, false).apply {
                imageView.setImageDrawable(arrowIcon)
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemPathPickerBinding) {
            binding.root.setOnClickListener {
                viewModel.subDocs = viewModel.subDocs.subList(0, holder.layoutPosition)
                setItems(viewModel.subDocs)
                viewModel.upFiles(viewModel.subDocs.lastOrNull())
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemPathPickerBinding,
            item: File,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

    inner class FileAdapter : RecyclerAdapter<File, ItemFileBinding>(this@FileManageActivity) {
        private val upIcon = ConvertUtils.toDrawable(FilePickerIcon.getUpDir())!!
        private val folderIcon = ConvertUtils.toDrawable(FilePickerIcon.getFolder())!!
        private val fileIcon = ConvertUtils.toDrawable(FilePickerIcon.getFile())!!

        override fun getViewBinding(parent: ViewGroup): ItemFileBinding {
            return ItemFileBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemFileBinding) {
            binding.root.setOnClickListener {
                val item = getItemByLayoutPosition(holder.layoutPosition)
                item?.let {
                    if (item == viewModel.lastDir) {
                        gotoLastDir()
                    } else if (item.isDirectory) {
                        viewModel.subDocs.add(item)
                        pathAdapter.setItems(viewModel.subDocs)
                        viewModel.upFiles(item)
                    } else {
                        openFileUri(
                            FileProvider.getUriForFile(
                                this@FileManageActivity,
                                AppConst.authority,
                                item
                            )
                        )
                    }
                }
            }
            binding.root.setOnLongClickListener { view ->
                val item = getItemByLayoutPosition(holder.layoutPosition)
                if (item == viewModel.lastDir) {
                    return@setOnLongClickListener true
                }
                item?.let {
                    showFileMenu(view, item)
                }
                return@setOnLongClickListener true
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemFileBinding,
            item: File,
            payloads: MutableList<Any>
        ) {
            if (item == viewModel.lastDir) {
                binding.imageView.setImageDrawable(upIcon)
                binding.textView.text = dirParent
            } else if (item.isDirectory) {
                binding.imageView.setImageDrawable(folderIcon)
                binding.textView.text = item.name
            } else {
                binding.imageView.setImageDrawable(fileIcon)
                binding.textView.text = item.name
            }
        }

        private fun showFileMenu(view: View, file: File) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.file_long_click)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_del -> viewModel.delFile(file)
                }
                true
            }
            popupMenu.show()
        }

    }

}