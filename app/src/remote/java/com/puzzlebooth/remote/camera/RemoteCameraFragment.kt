package com.puzzlebooth.remote.camera

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.remote.home.mainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentCameraRemoteBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RemoteCameraFragment : BaseFragment<FragmentCameraRemoteBinding>(R.layout.fragment_camera_remote) {

    override fun initViewBinding(view: View): FragmentCameraRemoteBinding {
        return FragmentCameraRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnShowSecretMenu.setOnClickListener { mainActivity()?.send("showsecretmenu") }
        binding.btnCloseSecretMenu.setOnClickListener { mainActivity()?.send("reset") }
        binding.btnZoomIn.setOnClickListener { mainActivity()?.send("zoomIn") }
        binding.btnZoomOut.setOnClickListener { mainActivity()?.send("zoomOut") }
        binding.btnBrightnessUp.setOnClickListener { mainActivity()?.send("brightnessUp") }
        binding.btnBrightnessDown.setOnClickListener { mainActivity()?.send("brightnessDown") }
        binding.btnTestPicture.setOnClickListener { mainActivity()?.send("testTakePicture") }
        binding.btnResetSecretMenu.setOnClickListener { mainActivity()?.send("resetTestMenu") }
    }

    override fun onPause() {
        super.onPause()
        mainActivity()?.send("reset")
        println("hhhtuning: onPause AlbumFragment")
    }
}