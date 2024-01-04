package com.puzzlebooth.server

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun initViewBinding(view: View): FragmentStartBinding {
        return FragmentStartBinding.bind(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)
        //binding.buttonsContainer.visibility = if(sharedPreferences.getBoolean("settings:touchMode", false)) View.VISIBLE else View.GONE

        Glide.with(this)
            .load(AnimationsManager.start)
            .transform(RotateTransformation(requireContext(),
                if(landscape)
                    270f
                else
                    0f
            ))
            .into(binding.startAnimation)

        binding.clickable.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "start" -> binding.clickable.performClick()
            event?.text == "showAlbum" -> findNavController().navigate(R.id.action_startFragment_to_albumFragment)
            event?.text == "showsecretmenu" -> findNavController().navigate(R.id.action_startFragment_to_cameraFragment)
            event?.text == "request_print_count" -> requireActivity().getMainActivity()?.sendStatus()
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