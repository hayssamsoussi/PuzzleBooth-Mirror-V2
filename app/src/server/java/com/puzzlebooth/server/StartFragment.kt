package com.puzzlebooth.server

import android.app.Activity
import android.content.Context.BATTERY_SERVICE
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.nearby.connection.Payload
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.databinding.FragmentStartBinding
import com.puzzlebooth.server.utils.AnimationsManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class StartFragment : BaseFragment<FragmentStartBinding>(R.layout.fragment_start) {

    override fun initViewBinding(view: View): FragmentStartBinding {
        return FragmentStartBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        binding.buttonsContainer.visibility = if(sharedPreferences.getBoolean("settings:touchMode", false)) View.VISIBLE else View.GONE

        Glide.with(this)
            .load(AnimationsManager.start)
            .transform(RotateTransformation(requireContext(), 270f))
            .into(binding.startAnimation)


        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "start" -> binding.btnStart.performClick()
            "showAlbum" -> binding.album.performClick()
            "showsecretmenu" -> binding.camera.performClick()
            "theme" -> binding.theme.performClick()
            "bluetooth" -> binding.bluetooth.performClick()
            "reset" -> requireActivity().getMainActivity()?.sendStatus()
            "request_print_count" -> requireActivity().getMainActivity()?.sendStatus()
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