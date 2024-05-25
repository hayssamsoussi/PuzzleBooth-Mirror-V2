package com.puzzlebooth.server.mosaic

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.R
import com.puzzlebooth.server.album.listing.AlbumAdapter
import com.puzzlebooth.server.album.listing.LocalImage
import java.io.File

interface ChoosePictureDialogFragmentListener {
    fun fillThisPic(file: File)
}

class ChoosePictureDialogFragment: DialogFragment() {

    var listener: ChoosePictureDialogFragmentListener? = null

    private lateinit var albumAdapter: AlbumAdapter
    private var localFiles = mutableListOf<LocalImage>()

    companion object {
        fun newInstance(text: String): ChoosePictureDialogFragment {
            val args = Bundle()
            args.putString("text", text)
            val fragment = ChoosePictureDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = getDialog()!!.window
            val size = Point()

            val display = window!!.windowManager.defaultDisplay
            display.getSize(size)

            val width: Int = size.x

            window.setLayout((width * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setCanceledOnTouchOutside(false);
        return inflater.inflate(R.layout.fragment_choose_picture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getExtras()
        //setUpViews()
        refreshData()
    }

    private fun getExtras() {
        val args = arguments
        val text = args?.getString("text")


    }

    private fun refreshData() {
        val rvAlbum = requireView().findViewById<RecyclerView>(R.id.rvAlbum)
        requireView().findViewById<Button>(R.id.btn_skip).setOnClickListener {
            dismiss()
        }
        localFiles.clear()
        val file = File(requireContext().getCurrentEventPhotosPath())

        val monthmillis = 172800000
        val twodaysago = if(false) (System.currentTimeMillis() - monthmillis) else 0
        val files = file.listFiles()?.filter {
            it.isFile && it.extension.equals("jpeg", true) &&
                    it.lastModified() > twodaysago
        }

        if(!files.isNullOrEmpty()) {
            files.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                localFiles.add(LocalImage(file, index))
            }
        }

        albumAdapter = AlbumAdapter(localFiles.toList()) {
            listener?.fillThisPic(it.file)
            dismiss()
            //currentPositionSelected = it.toIntOrNull() ?: -1
            //albumAdapter.setSelectedPosition(it.toIntOrNull() ?: -1)
        }
//
        rvAlbum.apply {
            this.adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 5, RecyclerView.VERTICAL, false)
        }
    }
}