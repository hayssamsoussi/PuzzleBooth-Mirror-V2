package com.puzzlebooth.server.mosaic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentMosaicBinding
import com.puzzlebooth.server.getMainActivity
import com.puzzlebooth.server.mosaic.list.MosaicAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.Timer
import kotlin.concurrent.timerTask


class MosaicFragment : BaseFragment<FragmentMosaicBinding>(R.layout.fragment_mosaic) {

    val timer = Timer()
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "sendToPrint" -> binding.btnSendToPrint.performClick()
        }
    }

    lateinit var adapter: MosaicAdapter
    override fun initViewBinding(view: View): FragmentMosaicBinding {
        return FragmentMosaicBinding.bind(view)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //MosaicManager.startMosaic(requireContext())
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startTimer()
        initViews()
    }

    fun startTimer() {
        timer.schedule(timerTask {
            if(isVisible) {
                updateViews()
            }
        }, 0, 2000)
    }

    fun initViews() {
        val mosaicViews = MosaicManager.generateMosaicViews()

        adapter = MosaicAdapter(mosaicViews.toList()) {

        }

        binding.rvMosaic.layoutManager = GridLayoutManager(requireContext(), 8)
        binding.rvMosaic.adapter = adapter

        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Originals: ${MosaicManager.mosaic_originals.listFiles()?.size}")
        stringBuilder.appendLine("Drafts: ${MosaicManager.mosaic_draft.listFiles()?.size}")
        stringBuilder.appendLine("Images: ${MosaicManager.mosaic_images.listFiles()?.size}")
        stringBuilder.appendLine("To Print: ${MosaicManager.mosaic_toPrint.listFiles()?.size}")
        stringBuilder.appendLine("Done: ${MosaicManager.mosaic_done.listFiles()?.size}")
        stringBuilder.appendLine("Merge: ${MosaicManager.mosaic_merge.listFiles()?.size}")
        stringBuilder.appendLine("MosaicPrint: ${MosaicManager.mosaic_print.listFiles()?.size}")

        binding.mosaicSummary.text = stringBuilder.toString()

        binding.btnSendToPrint.lockButtonAfterClickFor(2000) {
            println("hhh clicked!!!")
            MosaicManager.moveToPrintsToMerge(requireContext())
            updateViews()
        }
    }

    fun updateViews() {
        binding.btnSendToPrint?.text = "${MosaicManager.mosaic_toPrint.list()?.size}/${MosaicManager.countMosaic}"
    }
}

fun Button.lockButtonAfterClickFor(delay: Long, block: () -> Unit) {
    var isClickable = true

    this.setOnClickListener {
        if (isClickable) {
            // Disable the button
            isClickable = false

            println("hhh clicked!")

            block.invoke()
        }
    }

    this.alpha = 0.1F
    this.isClickable = false
    this.invalidate()

    Handler(Looper.getMainLooper()).postDelayed(Runnable {
        isClickable = true
        this.isClickable = true
        this.alpha = 1F
    }, delay)

}