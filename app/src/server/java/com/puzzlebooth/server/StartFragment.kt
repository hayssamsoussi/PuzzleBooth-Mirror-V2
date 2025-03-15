package com.puzzlebooth.server

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.databinding.FragmentStartBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.utils.AnimationsManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class StartFragment : BaseFragment<FragmentStartBinding>(R.layout.fragment_start) {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun initViewBinding(view: View): FragmentStartBinding {
        return FragmentStartBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    fun setAnimation() {
        val layoutName = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sharedPreferences.getString("selectedAnimationLand", "")
        } else {
            sharedPreferences.getString("selectedAnimation", "")
        }

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

    private fun initViews() {

        setAnimation()

        binding.backButton?.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.clickable.setOnClickListener {
            sharedViewModel.capturedPhotos.clear()
            sharedViewModel.currentCaptureMode = PHOTO_MODE.SINGLE
            (requireActivity() as? MainActivity)?.hideQRCode()
            findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
//            val isVideoMessage = sharedPreferences.getBoolean("settings:isVideoMessage", false)
//            val isMultiplePhotos = CountdownMultiplePhotosFragment.multiplePhotos
//            when {
//                isVideoMessage -> findNavController().navigate(R.id.action_startFragment_to_countdownVideoFragment)
//                isMultiplePhotos -> findNavController().navigate(R.id.action_startFragment_to_countdownMultiplePhotosFragment)
//                else -> findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
//            }
        }
    }

    private fun startSession() {
        sharedViewModel.capturedPhotos.clear()
        sharedViewModel.currentCaptureMode = PHOTO_MODE.SINGLE
        binding.clickable.performClick()
        (requireActivity() as? MainActivity)?.hideQRCode()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        println("hhh event.text: ${event?.text}")
        when {
            event?.text == "start" -> startSession()
            event?.text == "start2" -> {
                // is multiphoto = true
                startSession()
            }
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