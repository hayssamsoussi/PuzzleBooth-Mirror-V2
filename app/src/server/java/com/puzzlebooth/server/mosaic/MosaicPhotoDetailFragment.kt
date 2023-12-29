package com.puzzlebooth.server.mosaic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentMosaicBinding
import com.puzzlebooth.server.databinding.FragmentMosaicDetailBinding
import com.puzzlebooth.server.mosaic.list.MosaicAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.Timer
import kotlin.concurrent.timerTask


class MosaicPhotoDetailFragment : BaseFragment<FragmentMosaicDetailBinding>(R.layout.fragment_mosaic_detail) {
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "print" -> binding.btnPrint.performClick()
            "delete" -> binding.btnDeletee.performClick()
        }
    }

    lateinit var adapter: MosaicAdapter
    override fun initViewBinding(view: View): FragmentMosaicDetailBinding {
        return FragmentMosaicDetailBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    fun initViews() {
        val mosaicViews = MosaicManager.generateMosaicViews()

        adapter = MosaicAdapter(mosaicViews.toList()) { }

        val filePath = arguments?.getString("filePath")
        val position = arguments?.getInt("position")

        if(filePath != null)
            Glide.with(requireContext()).load(File(filePath)).into(binding.mosaicIv)

        binding.btnDeletee.setOnClickListener {
            if(position != null) {
                MosaicManager.deleteImageAtIndex(position)
            }
        }
    }
}