package io.legato.kazusa.ui.font

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseBottomSheetDialogFragment
import io.legato.kazusa.base.BaseDialogFragment
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.databinding.DialogFontSelectBinding
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.lib.dialogs.SelectItem
import io.legato.kazusa.lib.dialogs.alert
import io.legato.kazusa.lib.permission.Permissions
import io.legato.kazusa.lib.permission.PermissionsCompat
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.ui.file.HandleFileContract
import io.legato.kazusa.utils.FileDoc
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.RealPathUtil
import io.legato.kazusa.utils.applyTint
import io.legato.kazusa.utils.cnCompare
import io.legato.kazusa.utils.externalFiles
import io.legato.kazusa.utils.getPrefString
import io.legato.kazusa.utils.isContentScheme
import io.legato.kazusa.utils.list
import io.legato.kazusa.utils.listFileDocs
import io.legato.kazusa.utils.putPrefString
import io.legato.kazusa.utils.setLayout
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri

/**
 * 字体选择对话框
 */
class FontSelectDialog : BaseBottomSheetDialogFragment(R.layout.dialog_font_select),
    Toolbar.OnMenuItemClickListener,
    FontAdapter.CallBack {
    private val fontRegex = Regex("(?i).*\\.[ot]tf")
    private val binding by viewBinding(DialogFontSelectBinding::bind)
    private val adapter by lazy {
        val curFontPath = callBack?.curFontPath ?: ""
        FontAdapter(requireContext(), curFontPath, this)
    }
    private val selectFontDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                putPrefString(PreferKey.fontFolder, uri.toString())
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc != null) {
                    loadFontFiles(FileDoc.fromDocumentFile(doc))
                } else {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        loadFontFilesByPermission(path)
                    }
                }
            } else {
                uri.path?.let { path ->
                    putPrefString(PreferKey.fontFolder, path)
                    loadFontFilesByPermission(path)
                }
            }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        //binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.select_font)
        binding.toolBar.inflateMenu(R.menu.font_select)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        val fontPath = getPrefString(PreferKey.fontFolder)
        if (fontPath.isNullOrEmpty()) {
            openFolder()
        } else {
            if (fontPath.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(requireContext(), fontPath.toUri())
                if (doc?.canRead() == true) {
                    loadFontFiles(FileDoc.fromDocumentFile(doc))
                } else {
                    openFolder()
                }
            } else {
                loadFontFilesByPermission(fontPath)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_default -> {
                val requireContext = requireContext()
                alert(titleResource = R.string.system_typeface) {
                    items(
                        requireContext.resources.getStringArray(R.array.system_typefaces).toList()
                    ) { _, i ->
                        AppConfig.systemTypefaces = i
                        onDefaultFontChange()
                        dismissAllowingStateLoss()
                    }
                }
            }
            R.id.menu_other -> {
                openFolder()
            }
        }
        return true
    }

    private fun openFolder() {
        lifecycleScope.launch {
            val defaultPath = "SD${File.separator}Fonts"
            selectFontDir.launch {
                otherActions = arrayListOf(SelectItem(defaultPath, -1))
            }
        }
    }

    private fun getLocalFonts(): ArrayList<FileDoc> {
        val path = FileUtils.getPath(requireContext().externalFiles, "font")
        return File(path).listFileDocs {
            it.name.matches(fontRegex)
        }
    }

    private fun loadFontFilesByPermission(path: String) {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                loadFontFiles(
                    FileDoc.fromFile(File(path))
                )
            }
            .request()
    }

    private fun loadFontFiles(fileDoc: FileDoc) {
        execute {
            val fontItems = fileDoc.list {
                it.name.matches(fontRegex)
            } ?: ArrayList()
            mergeFontItems(fontItems, getLocalFonts())
        }.onSuccess {
            adapter.setItems(it)
        }.onError {
            AppLog.put("加载字体文件失败\n${it.localizedMessage}", it)
            toastOnUi("getFontFiles:${it.localizedMessage}")
        }
    }

    private fun mergeFontItems(
        items1: ArrayList<FileDoc>,
        items2: ArrayList<FileDoc>
    ): List<FileDoc> {
        val items = ArrayList(items1)
        items2.forEach { item2 ->
            var isInFirst = false
            items1.forEach for1@{ item1 ->
                if (item2.name == item1.name) {
                    isInFirst = true
                    return@for1
                }
            }
            if (!isInFirst) {
                items.add(item2)
            }
        }
        return items.sortedWith { o1, o2 ->
            o1.name.cnCompare(o2.name)
        }
    }

    override fun onFontSelect(docItem: FileDoc) {
        execute {
            callBack?.selectFont(docItem.toString())
        }.onSuccess {
            dismissAllowingStateLoss()
        }
    }

    private fun onDefaultFontChange() {
        callBack?.selectFont("")
    }

    private val callBack: CallBack?
        get() = (parentFragment as? CallBack) ?: (activity as? CallBack)

    interface CallBack {
        fun selectFont(path: String)
        val curFontPath: String
    }
}