package com.puzzlebooth.remote.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import com.puzzlebooth.remote.RemoteActivity
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.qr_code.QRCodeFragment
import com.puzzlebooth.remote.VolumeKeyListener
import com.puzzlebooth.remote.add_number.RemoteAddNumberFragment
import com.puzzlebooth.remote.add_number.RemoteAddNumberListener
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding

class HomeFragment : BaseFragment<FragmentHomeRemoteBinding>(R.layout.fragment_home_remote), VolumeKeyListener {

    override fun initViewBinding(view: View): FragmentHomeRemoteBinding {
        return FragmentHomeRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        binding.btnStart.setOnClickListener { mainActivity()?.sendThroughDelay("start") }
        binding.btnStart2.setOnClickListener { mainActivity()?.sendThroughDelay("start2") }
        binding.btnRefresh.setOnClickListener { mainActivity()?.sendThroughDelay("cancel") }
        binding.btnSave.setOnClickListener { mainActivity()?.sendThroughDelay("save") }
        binding.btnContinueWithMosaic.setOnClickListener { mainActivity()?.sendThroughDelay("printWithMosaic") }
        binding.btnContinue.setOnClickListener { printAction() }
        binding.btnQRCode.setOnClickListener { showQRCodeRadisson() }
        binding.btnRetry.setOnClickListener { mainActivity()?.sendThroughDelay("retake") }
    }

    override fun onVolumeKeyPressed(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                binding.btnStart.performClick()
                binding.btnContinue.performClick()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                binding.btnRetry.performClick()
                true
            }
            else -> false
        }
    }

    override fun onVolumeKeyReleased(keyCode: Int): Boolean {
        // Optional: Handle key release, if necessary
        return true
    }

    fun showQRCodeRadisson() {
        val fragment = QRCodeFragment.newInstance("")
        fragment.show(parentFragmentManager, "")
    }

    private fun printAction() {
        mainActivity()?.sendThroughDelay("print:;;")
//        val fragment = RemoteAddNumberFragment.newInstance()
//        fragment.listener = object: RemoteAddNumberListener {
//            override fun onSubmit(phone: String) {
//                if(phone.isNotEmpty())
//                    mainActivity()?.sendThroughDelay("print:${phone}")
//                else
//                    mainActivity()?.sendThroughDelay("print")
//            }
//
//            override fun onSkip() {
//                mainActivity()?.sendThroughDelay("print")
//            }
//
//        }
//
//        fragment.show(parentFragmentManager, "")
    }

    override fun onResume() {
        super.onResume()
        //binding.btnContinueWithMosaic.visibility = if(mainActivity()?.mosaicOn == true) View.VISIBLE else View.GONE
    }
}

fun Fragment.mainActivity(): RemoteActivity? {
    return (this.activity as? RemoteActivity)
}