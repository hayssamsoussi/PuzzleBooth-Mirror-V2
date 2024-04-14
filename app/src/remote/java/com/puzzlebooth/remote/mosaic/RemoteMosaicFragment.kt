package com.puzzlebooth.remote.mosaic

import android.R.attr.button
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.puzzlebooth.main.RemoteMosaicAdapter
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.models.MosaicBox
import com.puzzlebooth.main.models.MosaicInfo
import com.puzzlebooth.remote.RemoteActivity
import com.puzzlebooth.remote.home.mainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentMosaicRemoteBinding
import java.util.Timer
import kotlin.concurrent.timerTask


class RemoteMosaicFragment : BaseFragment<FragmentMosaicRemoteBinding>(R.layout.fragment_mosaic_remote) {

    private val timer = Timer()

    private var mosaicViews = mutableListOf<MosaicBox>()

    private var adapter: RemoteMosaicAdapter? = null

    override fun initViewBinding(view: View): FragmentMosaicRemoteBinding {
        return FragmentMosaicRemoteBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //startTimer()
        initViews()
    }

    fun startTimer() {
        timer.schedule(timerTask {
            if(isVisible) {
                activity?.runOnUiThread {
                    updateViews()
                }
            }
        }, 0, 2000)
    }

    fun updateViews() {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Originals: ${getMosaicInfo()?.originals}")
        stringBuilder.appendLine("Drafts: ${getMosaicInfo()?.drafts}")
        stringBuilder.appendLine("Images: ${getMosaicInfo()?.images}")
        stringBuilder.appendLine("To Print: ${getMosaicInfo()?.toPrint}")
        stringBuilder.appendLine("Done: ${getMosaicInfo()?.done}")
        stringBuilder.appendLine("Merge: ${getMosaicInfo()?.merge}")
        stringBuilder.appendLine("MosaicPrint: ${getMosaicInfo()?.mosaicPrint}")

        binding?.mosaicSummary?.text = stringBuilder.toString()

        binding?.btnSendToPrint?.text = "${getMosaicInfo()?.toPrint}/6"

        fetchMosaicViews()
        activity?.runOnUiThread {
            adapter?.notifyDataSetChanged()
        }
    }

    fun fetchMosaicViews() {
        mosaicViews.clear()
        mosaicViews.addAll((activity as? RemoteActivity)?.lastMosaicUpdates?.boxes ?: listOf())
        //mosaicViews.addAll(MosaicManager.generateMosaicViews())
    }

    fun getMosaicInfo(): MosaicInfo? {
        return (activity as? RemoteActivity)?.lastMosaicUpdates
    }


    fun initViews() {
        binding.btnRefresh.setOnClickListener {
            updateViews()
        }

        binding.btnSendToPrint.setOnClickListener {
            mainActivity()?.sendThroughDelay("sendToPrint")
        }

        if(mainActivity()?.mosaicOn == true) {
            binding.rvMosaic.visibility = View.VISIBLE
            binding.summariesContainer.visibility = View.VISIBLE
            binding.mosaicOff.visibility = View.GONE

            fetchMosaicViews()

            adapter = RemoteMosaicAdapter(mosaicViews) {
                // Initializing the popup menu and giving the reference as current context
                val popupMenu = PopupMenu(requireContext(), it.second)

                // Inflating popup menu from popup_menu.xml file
                popupMenu.menuInflater.inflate(
                    com.puzzlebooth.server.R.menu.mosaic_popup_menu,
                    popupMenu.getMenu()
                )
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item?.itemId) {
                        com.puzzlebooth.server.R.id.delete -> {
                            mainActivity()?.send("deleteMosaic:${it.first.boxNumber}")
                        }

                        com.puzzlebooth.server.R.id.print -> {
                            mainActivity()?.send("printMosaic:${it.first.boxNumber}")
                        }
                    }
                    true
                }
                // Showing the popup menu
                popupMenu.show()
            }
        } else {
            binding.rvMosaic.visibility = View.GONE
            binding.summariesContainer.visibility = View.GONE
            binding.mosaicOff.visibility = View.VISIBLE
        }

        binding.rvMosaic.layoutManager = GridLayoutManager(requireContext(), 8)
        binding.rvMosaic.adapter = adapter
    }

//    override fun onPause() {
//        super.onPause()
//        mainActivity()?.send("reset")
//        println("hhhtuning: onPause AlbumFragment")
//    }
}