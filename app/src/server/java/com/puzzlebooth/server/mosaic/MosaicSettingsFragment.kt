package com.puzzlebooth.server.mosaic

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.MosaicAdapter
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentMosaicDetailBinding
import com.puzzlebooth.server.databinding.FragmentMosaicSettingsBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class  MosaicSettingsFragment : BaseFragment<FragmentMosaicSettingsBinding>(R.layout.fragment_mosaic_settings) {

    lateinit var adapter: MosaicAdapter
    override fun initViewBinding(view: View): FragmentMosaicSettingsBinding {
        return FragmentMosaicSettingsBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    fun initViews() {
        binding.tvDownload.setOnClickListener {
            findNavController().navigate(R.id.action_mosaicSettingsFragment_to_mosaicDownloadFragment)
        }
    }


}