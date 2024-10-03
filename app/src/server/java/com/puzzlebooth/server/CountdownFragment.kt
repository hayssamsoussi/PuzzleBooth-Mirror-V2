package com.puzzlebooth.server

import android.R.attr
import android.R.attr.duration
import android.R.attr.resource
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.activityViewModels
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
import com.puzzlebooth.main.SharedViewModel
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.PreviewFragment.Companion.isMultiPhoto
import com.puzzlebooth.server.databinding.FragmentCountdownBinding
import com.puzzlebooth.server.utils.AnimationsManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream


class CountdownFragment : BaseFragment<FragmentCountdownBinding>(R.layout.fragment_countdown) {

    var countdownRunning = true
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val countdownValues = arrayOf("3", "2", "1", "Preparing Photo...")
    private var countdownIndex = 0
    private var countdownValues2 = listOf(3, 2, 1)
    private var currentIndex = 0

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        println("hhh received ${event?.text}")
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



        // Function to save Bitmap to cache directory
        fun saveBitmapToCache(context: Context, bitmap: Bitmap?, fileName: String): File? {
            try {
                // Create a file in the cache directory
                val cacheFile = File(context.cacheDir, fileName)

                if(bitmap != null) {
                    // Open FileOutputStream to write the Bitmap
                    val fileOutputStream = FileOutputStream(cacheFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()

                    return cacheFile
                } else {
                    cacheFile.delete()
                    return null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        // Function to retrieve a Bitmap from cache directory
        fun getBitmapFromCache(context: Context, fileName: String): Bitmap? {
            return try {
                // Create a file reference to the file in the cache directory
                val cacheFile = File(context.cacheDir, fileName)

                // Decode the file into a Bitmap
                if (cacheFile.exists()) {
                    BitmapFactory.decodeFile(cacheFile.absolutePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private inner class Listener : CameraListener() {
        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            sharedViewModel.createOriginalFile(requireContext(), sharedPreferences).let {
                result.toBitmap { fileCallback ->
                    if (fileCallback != null) {
                        sharedViewModel
                            .saveBitmapToOriginals(requireContext(), sharedPreferences, fileCallback)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { fileSuccess ->
                                sharedViewModel.capturedPhotos.add(fileSuccess)
                                requireActivity().runOnUiThread {
                                    findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
                                }
                            }.subscribe()
                    }
                }
            }



//            if(isMultiPhoto(sharedPreferences)) {
//                println("hhh multiplePhotos")
//                result.toBitmap { bitmap ->
//                    if(getCapturedPhoto(requireContext()) == null) {
//                        setCapturedPhoto(requireContext(), bitmap)
//                    } else if(getCapturedPhoto2(requireContext()) == null) {
//                        setCapturedPhoto2(requireContext(), bitmap)
//                    } else {
//                        setCapturedPhoto3(requireContext(), bitmap)
//                    }
//
//                    findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
//
//                    //refreshPhotoThumbnails()
//                }
//            } else {
//                result.toBitmap() {
//                    if (it != null) {
//                        binding.camera.close()
//                        capturedPhoto = it
//                        findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
//                    }
//                }
//            }
//            result.toBitmap() {
//                if (it != null) {
//                    println("hhh picture taken!")
//                    binding.camera.close()
//                    capturedPhoto = it
//                    findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
//                }
//            }
        }
    }

    override fun initViewBinding(view: View): FragmentCountdownBinding {
        return FragmentCountdownBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                val exposure = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
                binding.camera.exposureCorrection = exposure
                binding.camera.takePicture()
            }
        }

        val landscape = sharedPreferences.getBoolean("settings:landscape", false)

        binding.camera.open()

        countdownRunning = true


        //val zoom = sharedPreferences.getFloat("camera:zoom", binding.camera.exposureCorrection)
        //binding.camera.zoom = zoom
        val currentAutoPhoto = sharedPreferences.getBoolean("settings:autoPhoto", true)

        val animation = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            AnimationsManager.countdownLandscape
        } else {
            AnimationsManager.countdownLandscape
        }

        //startCountdown4()
        Glide
            .with(this)
            .asGif()
            .load(animation)
            .transform(RotateTransformation(requireContext(), if(landscape)
                270f
            else
                0f))
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
                                binding.btnStart2.performClick()
                            }
                            println("hhh anbimation end")
                        }
                    })

                    return false
                }

            })
            .into(binding.overlayAnimation)
    }
//    private fun startCountdown4() {
//        if (currentIndex < countdownValues.size) {
//            val currentValue = countdownValues[currentIndex]
//            binding.countdownTextView?.text = currentValue.toString()
//
//            // Create an ObjectAnimator to animate scaleX and scaleY
//            val animator = ObjectAnimator.ofFloat(binding.countdownTextView, "scaleX", 5f, 1f)
//            animator.duration = 1000 // 1 second
//            animator.repeatCount = 0
//
//            val animatorY = ObjectAnimator.ofFloat(binding.countdownTextView, "scaleY", 5f, 1f)
//            animatorY.duration = 1000
//
//            // Start both animations together
//            animator.start()
//            animatorY.start()
//
//            animator.addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    currentIndex++
//                    startCountdown4() // Start the next countdown number after animation ends
//                }
//            })
//        }
//    }
//
//
//    private fun startCountdown() {
//        countdownRunning = true
//
//        lifecycleScope.launch {
//            //binding.camera.mode = Mode.VIDEO
//            binding.camera.open()
//            val exposure = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
//            binding.camera.exposureCorrection = exposure
//            //binding.camera.exposureCorrection = sharedPreferences.getFloat("camera:exposure", binding.camera.exposureCorrection)
//
//            val zoom = sharedPreferences.getFloat("camera:zoom", binding.camera.exposureCorrection)
//            binding.camera.zoom = zoom
//
//
//            val currentAutoPhoto = sharedPreferences.getBoolean("settings:autoPhoto", true)
//
//
//
//            if(currentAutoPhoto) {
//                println("hhh capture photo")
//                //binding.btnStart2.performClick()
//                //binding.camera.takePicture()
//            }
//        }
//    }
}