package com.puzzlebooth.server

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.databinding.FragmentCountdownBinding
import com.puzzlebooth.server.utils.AnimationsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CountdownFragment : BaseFragment<FragmentCountdownBinding>(R.layout.fragment_countdown) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "start2" -> binding.camera.takePicture()
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

    companion object {
        var capturedPhoto: Bitmap? = null
        var measureTimeForPictureCapture: Long = 0
    }

    private inner class Listener : CameraListener() {
        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            measureTimeForPictureCapture = System.currentTimeMillis() - measureTimeForPictureCapture
            result.toBitmap() {
                if (it != null) {
                    println("hhh picture taken!")
                    binding.camera.close()
                    com.puzzlebooth.server.CountdownFragment.Companion.capturedPhoto = it
                    findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
                    println("hhh measureTime ${measureTimeForPictureCapture}")
                    //showPreview(it)
                }
            }
        }
    }

    override fun initViewBinding(view: View): FragmentCountdownBinding {
        return FragmentCountdownBinding.bind(view)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.camera.setLifecycleOwner(this)
        binding.camera.addCameraListener(Listener())

        initViews()
        startCountdown()
    }

    private fun initViews() {
        Glide.with(this)
            .load(AnimationsManager.countdown)
            .transform(RotateTransformation(requireContext(), 270f))
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource is GifDrawable) {
                        (resource).setLoopCount(1)
                    }
                    return false
                }
            })
            .into(binding.overlayAnimation)
    }

    private fun startCountdown() {
        lifecycleScope.launch {
            binding.camera.open()
            val exposure = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
            println("hhh settings the exposure as ${exposure}")
            binding.camera.exposureCorrection = 1.0F
            binding.camera.exposureCorrection = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
            val zoom = sharedPreferences.getFloat("camera:zoom", binding.camera.exposureCorrection)
            binding.camera.zoom = zoom
            //binding.btnPrint.visibility = View.GONE
            //binding.btnRetake.visibility = View.GONE

            var countdownSeconds = 3
            while (countdownSeconds > 0) {
                //binding.textDisplay.text = countdownSeconds.toString()
                delay(1000) // delay for 1 second
                countdownSeconds--
            }
            measureTimeForPictureCapture = System.currentTimeMillis()
            val currentAutoPhoto = sharedPreferences.getBoolean("settings:autoPhoto", false)

            if(currentAutoPhoto) {
                binding.camera.takePicture()
            }


            //binding.btnPrint.visibility = View.VISIBLE
            //binding.btnRetake.visibility = View.VISIBLE
            //binding.textDisplay.text = "Done!"
        }
    }
}