package io.legato.kazusa.ui.book.group

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseBottomSheetDialogFragment
import io.legato.kazusa.data.entities.BookGroup
import io.legato.kazusa.databinding.DialogBookGroupEditBinding
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.MD5Utils
import io.legato.kazusa.utils.SelectImageContract
import io.legato.kazusa.utils.externalFiles
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.inputStream
import io.legato.kazusa.utils.launch
import io.legato.kazusa.utils.readUri
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import io.legato.kazusa.utils.visible
import splitties.init.appCtx
import splitties.views.onClick
import java.io.FileOutputStream

class GroupEditDialog() : BaseBottomSheetDialogFragment(R.layout.dialog_book_group_edit) {

    constructor(bookGroup: BookGroup? = null) : this() {
        arguments = Bundle().apply {
            putParcelable("group", bookGroup?.copy())
        }
    }

    private val binding by viewBinding(DialogBookGroupEditBinding::bind)
    private val viewModel by viewModels<GroupViewModel>()
    private var bookGroup: BookGroup? = null
    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri ?: return@registerForActivityResult
        readUri(it.uri) { fileDoc, inputStream ->
            try {
                var file = requireContext().externalFiles
                val suffix = fileDoc.name.substringAfterLast(".")
                val fileName = it.uri.inputStream(requireContext()).getOrThrow().use { tmp ->
                    MD5Utils.md5Encode(tmp) + ".$suffix"
                }
                file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                binding.ivCover.load(file.absolutePath)
            } catch (e: Exception) {
                appCtx.toastOnUi(e.localizedMessage)
            }
        }
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        //binding.toolBar.setBackgroundColor(primaryColor)
        @Suppress("DEPRECATION")
        bookGroup = arguments?.getParcelable("group")
        bookGroup?.let {
            binding.btnDelete.visible(it.groupId > 0 || it.groupId == Long.MIN_VALUE)
            binding.tieGroupName.setText(it.groupName)
            binding.ivCover.load(it.cover)
            binding.spSort.setSelection(it.bookSort + 1)
            binding.cbEnableRefresh.isChecked = it.enableRefresh
        } ?: let {
            binding.toolBar.title = getString(R.string.add_group)
            binding.btnDelete.gone()
            binding.ivCover.load()
        }
        binding.run {
            ivCover.onClick {
                selectImage.launch()
            }
            btnCancel.onClick {
                dismiss()
            }
            btnOk.onClick {
                val groupName = tieGroupName.text?.toString()
                if (groupName.isNullOrEmpty()) {
                    toastOnUi("分组名称不能为空")
                } else {
                    val bookSort = binding.spSort.selectedItemPosition - 1
                    val coverPath = binding.ivCover.bitmapPath
                    val enableRefresh = binding.cbEnableRefresh.isChecked
                    bookGroup?.let {
                        it.groupName = groupName
                        it.cover = coverPath
                        it.bookSort = bookSort
                        it.enableRefresh = enableRefresh
                        viewModel.upGroup(it) {
                            dismiss()
                        }
                    } ?: let {
                        viewModel.addGroup(
                            groupName,
                            bookSort,
                            enableRefresh,
                            coverPath
                        ) {
                            dismiss()
                        }
                    }
                }

            }
            btnDelete.onClick {
                deleteGroup {
                    bookGroup?.let {
                        viewModel.delGroup(it) {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun deleteGroup(ok: () -> Unit) {
        alert(R.string.delete, R.string.sure_del) {
            yesButton {
                ok.invoke()
            }
            noButton()
        }
    }

}