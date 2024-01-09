package com.puzzlebooth.remote.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.puzzlebooth.remote.RemoteActivity
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentHomeRemoteBinding

class HomeFragment : BaseFragment<FragmentHomeRemoteBinding>(R.layout.fragment_home_remote) {

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
        binding.btnContinue.setOnClickListener { mainActivity()?.sendThroughDelay("print") }
        binding.btnRetry.setOnClickListener { mainActivity()?.sendThroughDelay("retake") }
    }

    override fun onResume() {
        super.onResume()
        binding.btnContinueWithMosaic.visibility = if(mainActivity()?.mosaicOn == true) View.VISIBLE else View.GONE
    }
}

fun Fragment.mainActivity(): RemoteActivity? {
    return (this.activity as? RemoteActivity)
}