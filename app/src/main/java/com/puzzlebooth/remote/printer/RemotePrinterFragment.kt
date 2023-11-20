package com.puzzlebooth.remote.printer

import android.os.Bundle
import android.view.View
import com.puzzlebooth.remote.home.mainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.base.BaseFragment
import com.puzzlebooth.server.databinding.FragmentPrinterRemoteBinding

class RemotePrinterFragment : BaseFragment<FragmentPrinterRemoteBinding>(R.layout.fragment_printer_remote) {

    override fun initViewBinding(view: View): FragmentPrinterRemoteBinding {
        return FragmentPrinterRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClicks()
    }

    override fun onResume() {
        super.onResume()
//        if(MasterBottomNavigation.ip.isNotEmpty()) {
//            binding.etIP.setText(MasterBottomNavigation.ip)
//            binding.etPort.setText(MasterBottomNavigation.port)
//        }
    }

    private fun initClicks() {
        binding.btnAutoConnectPrinter.setOnClickListener {
            binding.connectingProgress.visibility = View.VISIBLE
            mainActivity()?.sendThroughDelay("autoConnectPrinter")
        }

        binding.btnTestPrint.setOnClickListener {
            mainActivity()?.sendThroughDelay("testPrint")
        }

        binding.btnSendIP.setOnClickListener {
            binding.connectingProgress.visibility = View.VISIBLE
            mainActivity()?.send("ip:${binding.etIP.text.toString()},port:${binding.etPort.text.toString()}")
        }
    }
}