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
        //initClicks()
    }

    private fun initViews() {
        // RV

        binding.btnStart.setOnClickListener { mainActivity()?.sendThroughDelay("start") }
        binding.btnStart2.setOnClickListener { mainActivity()?.sendThroughDelay("start2") }
        binding.btnReset.setOnClickListener { mainActivity()?.sendThroughDelay("cancel") }
        binding.btnContinue.setOnClickListener { mainActivity()?.sendThroughDelay("print") }

        // haflet nour and mahdi in la salle date n night trend
        if(System.currentTimeMillis() < 1694003066919) {
            binding.btnContinueWithoutPrint.setOnClickListener { mainActivity()?.sendThroughDelay("continuewithoutprint") }
        } else {
            binding.btnContinueWithoutPrint.visibility = View.GONE
        }

        binding.btnPrintLastCopy.setOnClickListener { mainActivity()?.sendThroughDelay("printlastcopy") }
        binding.btnPrintLastCopy.visibility = View.GONE
        binding.btnRetry.setOnClickListener { mainActivity()?.sendThroughDelay("retake") }

        binding.printerControllerButton.setOnClickListener {
            //findNavController().navigate(R.id.action_)
        }
        binding.cameraControllerButton.setOnClickListener {
            //findNavController().navigate(R.id.action_firstFragment_to_cameraFragment)
        }
        binding.albumButton.setOnClickListener {
            //indNavController().navigate(R.id.action_firstFragment_to_albumFragment)
        }
    }
}

fun Fragment.mainActivity(): RemoteActivity? {
    return (this.activity as? RemoteActivity)
}