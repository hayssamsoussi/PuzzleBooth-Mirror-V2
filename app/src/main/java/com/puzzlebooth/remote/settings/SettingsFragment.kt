package com.puzzlebooth.remote.settings

import android.os.Bundle
import android.view.View
import com.puzzlebooth.remote.home.mainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.base.BaseFragment
import com.puzzlebooth.server.databinding.FragmentSettingsRemoteBinding

class SettingsFragment : BaseFragment<FragmentSettingsRemoteBinding>(R.layout.fragment_settings_remote) {

    override fun initViewBinding(view: View): FragmentSettingsRemoteBinding {
        return FragmentSettingsRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnShowLayout.setOnClickListener {
            mainActivity()?.send("showLayout")
        }

        binding.btnUpdateLayout.setOnClickListener {
            mainActivity()?.send("updateLayout")
        }

        binding.btnUpdateLayout.setOnClickListener {
            mainActivity()?.send("updateLayout")
        }

        binding.btnUpdateLayout.setOnClickListener {
            mainActivity()?.send("updateLayout")
        }

        binding.btnUpdateLayout.setOnClickListener {
            mainActivity()?.send("updateLayout")
        }
    }
}