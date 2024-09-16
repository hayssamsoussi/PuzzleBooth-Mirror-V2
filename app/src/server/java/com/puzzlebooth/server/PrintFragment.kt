package com.puzzlebooth.server

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.CountdownFragment.Companion.capturedPhoto
import com.puzzlebooth.server.CountdownFragment.Companion.capturedPhoto2
import com.puzzlebooth.server.CountdownFragment.Companion.capturedPhoto3
import com.puzzlebooth.server.StartFragment.Companion.isMultiPhoto
import com.puzzlebooth.server.databinding.FragmentPrintBinding
import com.puzzlebooth.server.utils.AnimationsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PrintFragment : BaseFragment<FragmentPrintBinding>(R.layout.fragment_print) {

    override fun initViewBinding(view: View): FragmentPrintBinding {
        return FragmentPrintBinding.bind(view)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun clearMultiPhoto() {
        capturedPhoto = null
        capturedPhoto2 = null
        capturedPhoto3 = null
        isMultiPhoto = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearMultiPhoto()

        startCountdown()
    }

    private fun startCountdown() {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)

        val printingAnimation = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            AnimationsManager.printingLand
        } else {
            AnimationsManager.printing
        }

        Glide.with(this)
            .load(printingAnimation)
            .transform(RotateTransformation(requireContext(), if(landscape) 270f else 0F))
            //.transform(RotateTransformation(requireContext(), 270f))
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
                        (resource).setLoopCount(3)
                    }
                    return false
                }
            })
            .into(binding.overlayAnimation)

        coroutineScope.launch {
            var countdownSeconds = 5
            val isSlow = sharedPreferences.getBoolean("settings:printingSlow", true)

            delay(if(isSlow) 15000 else 9000)

            if(isVisible) {
                findNavController().navigate(R.id.action_printFragment_to_startFragment)
            }

            //binding.textDisplay.text = "Done!"
        }
    }
}