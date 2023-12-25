package com.puzzlebooth.server

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startCountdown()
    }

    private fun startCountdown() {
        Glide.with(this)
            .load(AnimationsManager.printing)
            //.transform(RotateTransformation(requireContext(), 270f))
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
                        (resource).setLoopCount(2)
                    }
                    return false
                }
            })
            .into(binding.overlayAnimation)

        coroutineScope.launch {
            var countdownSeconds = 5
            delay(9000)
            findNavController().navigate(R.id.action_printFragment_to_startFragment)
            //binding.textDisplay.text = "Done!"
        }
    }
}