package com.puzzlebooth.server

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.databinding.FragmentPrintBinding
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
        coroutineScope.launch {
            var countdownSeconds = 5
            while (countdownSeconds > 0) {
                binding.textDisplay.text = "Printing " + countdownSeconds.toString()
                delay(1000) // delay for 1 second
                countdownSeconds--
            }
            findNavController().navigate(R.id.action_printFragment_to_startFragment)
            binding.textDisplay.text = "Done!"
        }
    }
}