package com.puzzlebooth.server

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.CountdownMultiplePhotosFragment.Companion.capturedPhoto1
import com.puzzlebooth.server.CountdownMultiplePhotosFragment.Companion.capturedPhoto2
import com.puzzlebooth.server.CountdownMultiplePhotosFragment.Companion.capturedPhoto3
import com.puzzlebooth.server.databinding.FragmentStartBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.utils.AnimationsManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class StartFragment : BaseFragment<FragmentStartBinding>(R.layout.fragment_start) {

    override fun initViewBinding(view: View): FragmentStartBinding {
        return FragmentStartBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capturedPhoto1 = null
        capturedPhoto2 = null
        capturedPhoto3 = null

        initViews()
    }

    private fun initViews() {

        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutName = sharedPreferences.getString("selectedAnimationLand", "")
            if(layoutName.isNullOrEmpty()) {
                Glide.with(this)
                    .load(AnimationsManager.startLandscape)
                    .transform(RotateTransformation(requireContext(), 0f))
                    .into(binding.startAnimation)
            } else {
                val layoutPath = "${requireContext().cacheDir}/animations/${layoutName}"
                Glide.with(this)
                    .load(layoutPath)
                    .transform(RotateTransformation(requireContext(),
                        0f
                    ))
                    .into(binding.startAnimation)
            }
        } else {
            val layoutName = sharedPreferences.getString("selectedAnimation", "")
            println("hhh animation for portrait is ${layoutName}")
            if(layoutName.isNullOrEmpty()) {
                Glide.with(this)
                    .load(AnimationsManager.start)
                    .transform(RotateTransformation(requireContext(), 0f))
                    .into(binding.startAnimation)
            } else {
                val layoutPath = "${requireContext().cacheDir}/animations/${layoutName}"
                Glide.with(this)
                    .load(layoutPath)
                    .transform(RotateTransformation(requireContext(),
                        0f
                    ))
                    .into(binding.startAnimation)
            }
        }

        binding.backButton?.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.clickable.setOnClickListener {
            val isVideoMessage = sharedPreferences.getBoolean("settings:isVideoMessage", false)
            val isMultiplePhotos = CountdownMultiplePhotosFragment.multiplePhotos
            CountdownFragment.setCapturedPhoto(requireContext(), null)
            CountdownFragment.setCapturedPhoto2(requireContext(), null)
            CountdownFragment.setCapturedPhoto3(requireContext(), null)
            when {
                isVideoMessage -> findNavController().navigate(R.id.action_startFragment_to_countdownVideoFragment)
                isMultiplePhotos -> findNavController().navigate(R.id.action_startFragment_to_countdownMultiplePhotosFragment)
                else -> findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "start" -> binding.clickable.performClick()
            event?.text == "showAlbum" -> findNavController().navigate(R.id.action_startFragment_to_albumFragment)
            event?.text == "showsecretmenu" -> findNavController().navigate(R.id.action_startFragment_to_cameraFragment)
            event?.text == "request_print_count" -> requireActivity().getMainActivity()?.sendStatus()
            event?.text == "sendToPrint" -> MosaicManager.moveToPrintsToMerge(requireContext())
            event?.text?.contains("mosaic", true) == true -> {
                MosaicManager.processMosaicEvent(requireContext(), event.text)
            }
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            val a: Activity? = activity
            if (a != null) a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().getMainActivity()?.sendStatus()
    }
}

fun Activity.getMainActivity(): MainActivity? {
    return (this as? MainActivity)
}