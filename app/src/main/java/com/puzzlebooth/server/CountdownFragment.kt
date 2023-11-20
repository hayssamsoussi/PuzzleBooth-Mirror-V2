package com.puzzlebooth.server

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.puzzlebooth.server.base.BaseFragment
import com.puzzlebooth.server.databinding.FragmentCountdownBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountdownFragment : BaseFragment<FragmentCountdownBinding>(R.layout.fragment_countdown) {

    companion object {
        var capturedPhoto: Bitmap? = null
    }

    private inner class Listener : CameraListener() {
        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            result.toBitmap() {
                if (it != null) {
                    println("hhh picture taken!")
                    binding.camera.close()
                    com.puzzlebooth.server.CountdownFragment.Companion.capturedPhoto = it
                    findNavController().navigate(R.id.action_countdownFragment_to_previewFragment)
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

    }

    private fun startCountdown() {
        lifecycleScope.launch {
            binding.camera.open()
            //binding.btnPrint.visibility = View.GONE
            //binding.btnRetake.visibility = View.GONE

            var countdownSeconds = 5
            while (countdownSeconds > 0) {
                binding.textDisplay.text = countdownSeconds.toString()
                delay(1000) // delay for 1 second
                countdownSeconds--
            }

            binding.camera.takePicture()

            //binding.btnPrint.visibility = View.VISIBLE
            //binding.btnRetake.visibility = View.VISIBLE
            binding.textDisplay.text = "Done!"
        }
    }
}