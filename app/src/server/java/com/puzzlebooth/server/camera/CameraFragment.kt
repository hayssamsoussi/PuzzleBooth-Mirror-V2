package com.puzzlebooth.server.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.FileClientLegacy
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentCameraBinding
import com.puzzlebooth.server.databinding.FragmentPreviewBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "brightnessUp" -> binding.upButton.performClick()
            "brightnessDown" -> binding.downButton.performClick()
            "reset" -> findNavController().popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        binding.camera.close()
        EventBus.getDefault().unregister(this)
    }

    override fun initViewBinding(view: View): FragmentCameraBinding {
        return FragmentCameraBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        binding.camera.open()

        binding.upButton.setOnClickListener {
            binding.camera.exposureCorrection += .5F
            println("hhh expsosure now is " + binding.camera.exposureCorrection)
        }

        binding.downButton.setOnClickListener {
            binding.camera.exposureCorrection -= .5F
        }

        binding.btnSubmit.setOnClickListener {
            val edit = sharedPreferences.edit()
            println("hhh saving exposure as ${binding.camera.exposureCorrection}")
            edit.putFloat("camera:exposure", binding.camera.exposureCorrection)
            edit.putFloat("camera:zoom", binding.camera.zoom)
            edit.apply()
            findNavController().popBackStack()
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}