package com.puzzlebooth.server.album

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.album.listing.AlbumAdapter
import com.puzzlebooth.server.album.listing.LocalImage
import com.puzzlebooth.server.databinding.FragmentAlbumBinding

class AlbumFragment : BaseFragment<FragmentAlbumBinding>(R.layout.fragment_album) {

    override fun initViewBinding(view: View): FragmentAlbumBinding {
        return FragmentAlbumBinding.bind(view)
    }

    private var localFiles = mutableListOf<LocalImage>()

    private lateinit var albumAdapter: AlbumAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        initViews()
    }

    private fun initViews() {
        albumAdapter = AlbumAdapter(localFiles.toList()) {
            println("hhh action $it")
        }

        binding.rvAlbum.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.HORIZONTAL, false)
        }
    }

    private fun initData() {
        localFiles.clear()
        val file = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS)

        val twodaysmillis = 172800000
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
    }
}