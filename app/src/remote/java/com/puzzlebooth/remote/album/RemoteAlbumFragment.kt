package com.puzzlebooth.remote.album

import android.os.Bundle
import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.remote.home.mainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentAlbumRemoteBinding

class RemoteAlbumFragment : BaseFragment<FragmentAlbumRemoteBinding>(R.layout.fragment_album_remote) {

    override fun initViewBinding(view: View): FragmentAlbumRemoteBinding {
        return FragmentAlbumRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnShowAlbum.setOnClickListener { mainActivity()?.send("showAlbum") }
        binding.btnCloseAlbum.setOnClickListener { mainActivity()?.send("reset") }
        binding.btnAlbumNext.setOnClickListener { mainActivity()?.send("albumNext") }
        binding.btnAlbumPrevious.setOnClickListener { mainActivity()?.send("albumPrevious") }
        binding.btnALbumPrint.setOnClickListener { mainActivity()?.send("albumPrint") }
        binding.btnShowQR.setOnClickListener { mainActivity()?.send("albumQR") }
    }
}