package com.puzzlebooth.server

import android.R.attr
import android.R.attr.duration
import android.R.attr.resource
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Mode
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.databinding.FragmentCountdownBinding
import com.puzzlebooth.server.databinding.FragmentMultiplePhotosCountdownBinding
import com.puzzlebooth.server.utils.AnimationsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File


class CountdownMultiplePhotosFragment : BaseFragment<FragmentMultiplePhotosCountdownBinding>(R.layout.fragment_multiple_photos_countdown) {

    var countdownRunning = true

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "start2" -> binding.btnStart2.performClick()
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
        var multiplePhotos = false
        var capturedPhoto1: Bitmap? = null
        var capturedPhoto2: Bitmap? = null
        var capturedPhoto3: Bitmap? = null
    }

    fun refreshPhotoThumbnails() {
        Glide.with(requireContext())
            .load(capturedPhoto1)
            .into(binding.image1)
        Glide.with(requireContext())
            .load(capturedPhoto2)
            .into(binding.image2)
        Glide.with(requireContext())
            .load(capturedPhoto3)
            .into(binding.image3)
    }

    private inner class Listener : CameraListener() {
        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            if(multiplePhotos == true) {
                println("hhh multiplePhotos")
                result.toBitmap { bitmap ->
                    if(capturedPhoto1 == null) {
                        println("hhh capturedPhoto1 != null")
                        capturedPhoto1 = bitmap
                    } else if(capturedPhoto2 == null) {
                        println("hhh capturedPhoto2 != null")
                        capturedPhoto2 = bitmap
                    } else {
                        capturedPhoto3 = bitmap
                        findNavController().navigate(R.id.action_countdownMultiplePhotosFragment_to_previewFragment)
                    }

                    refreshPhotoThumbnails()
                }
            } else {
                result.toBitmap() {
                    if (it != null) {
                        binding.camera.close()
                        capturedPhoto1 = it
                        findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
                    }
                }
            }

        }
    }

    override fun initViewBinding(view: View): FragmentMultiplePhotosCountdownBinding {
        return FragmentMultiplePhotosCountdownBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capturedPhoto1 = null
        capturedPhoto2 = null
        capturedPhoto3 = null

        binding.camera.setLifecycleOwner(this)
        binding.camera.addCameraListener(Listener())

        initViews()
        //startCountdown()
    }

    fun Context.playAudioFromRaw(resId: Int): MediaPlayer? {
        val mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.start()
        return mediaPlayer
    }


    private fun initViews() {

        binding.btnStart2.setOnClickListener {
            if(!countdownRunning) {
                //requireContext().playAudioFromRaw(R.raw.camera_sound)
                //val exposure = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
                //binding.camera.exposureCorrection = exposure
                //binding.camera.takePicture()
                startCountdown()
            }
        }

        binding.camera.open()

        countdownRunning = true

        startCountdown()
    }

    fun startCountdown() {
        //val zoom = sharedPreferences.getFloat("camera:zoom", binding.camera.exposureCorrection)
        //binding.camera.zoom = zoom
        val currentAutoPhoto = sharedPreferences.getBoolean("settings:autoPhoto", true)

        val animation = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            AnimationsManager.countdownLandscape
        } else {
            AnimationsManager.countdown
        }

        Glide
            .with(this)
            .asGif()
            .load(animation)
            .transform(RotateTransformation(requireContext(), 0f))
            .listener(object: RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    resource.registerAnimationCallback(object :
                        Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            // This code will be executed when the GIF ends
                            // Do your desired actions here
                            countdownRunning = false
                            if(currentAutoPhoto || isLandscape()) {
                                println("hhh capture photo")
                                binding.camera.takePicture()
                                //binding.btnStart2.performClick()
                            }
                            println("hhh anbimation end")
                        }
                    })

                    return false
                }

            })
            .into(binding.overlayAnimation)
    }
}