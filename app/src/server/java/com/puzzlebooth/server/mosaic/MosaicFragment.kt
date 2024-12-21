package com.puzzlebooth.server.mosaic

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.github.kittinunf.fuel.Fuel
import com.kongzue.dialogx.dialogs.MessageDialog
import com.puzzlebooth.main.MosaicAdapter
import com.puzzlebooth.main.MosaicItem
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.getCurrentEventName
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.main.utils.mosaicDraftPath
import com.puzzlebooth.main.utils.showInputDialog
import com.puzzlebooth.main.utils.showMenuDialog
import com.puzzlebooth.server.R
import com.puzzlebooth.server.animations.hide
import com.puzzlebooth.server.animations.show
import com.puzzlebooth.server.databinding.FragmentMosaicBinding
import com.puzzlebooth.server.network.Design
import com.puzzlebooth.server.network.Event
import com.puzzlebooth.server.theme.listing.DesignsAdapter
import io.paperdb.Paper
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.util.Timer
import javax.sql.DataSource
import kotlin.concurrent.timerTask


class MosaicFragment : BaseFragment<FragmentMosaicBinding>(R.layout.fragment_mosaic) {

    var mosaicViews = mutableListOf<MosaicItem>()

    var NUM_COLUMNS = 8

    var currentDesign: Design? = null
    private var designs = mutableListOf<Design>()

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
        when {
            event?.text == "sendToPrint" -> binding.btnSendToPrint.performClick()
            event?.text == "mosaicDownload" -> binding.btnSendToPrint.performClick()
            event?.text?.startsWith("openMosaic") == true -> {
                val mosaicPosition = event.text.substringAfter(":")
                if(mosaicPosition.isNotEmpty()) {
                    MosaicManager
                        .getMosaicFileAt(mosaicPosition.toInt())
                        .doOnSuccess {
                            openMosaicDetails(it)
                        }
                        .subscribe()

                    //openMosaicDetails()
                }
            }
        }
    }

    var adapter: MosaicAdapter? = null

    override fun initViewBinding(view: View): FragmentMosaicBinding {
        return FragmentMosaicBinding.bind(view)
    }

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

        initViews()
        updateMosaicInfo(true)

    }

    fun fetchMosaicViews(): Completable {
        println("hhh fetchMosaicViews")
        return Completable.create { emitter ->
            val startTime = System.currentTimeMillis() // Record the start time
            MosaicManager
                .generateMosaicViews()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    activity?.runOnUiThread {
                        showProgress()
                    }
                }
                .doOnSuccess {
                    val endTime = System.currentTimeMillis() // Record the end time
                    val duration = endTime - startTime // Calculate the duration
                    println("hhh:MosaicFragment:fetchMosaicViews:Time taken: $duration ms")

                    activity?.runOnUiThread {
                        hideProgress()
                    }

                    mosaicViews.clear()
                    mosaicViews.addAll(it)
                    emitter.onComplete()
                }
                .subscribe()
        }
    }

    fun openMosaicDetails(mosaicItem: MosaicItem) {
        val bundle = Bundle()
        bundle.putString("filePath", mosaicItem.file.path)
        bundle.putInt("position", mosaicItem.position)
        findNavController().navigate(R.id.action_mosaicFragment_to_mosaicDetailFragment, bundle)
    }

    fun initViews() {
        fetchMosaicViews()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                val showMosaicImages = sharedPreferences.getBoolean("settings:showMosaicImages", false)

                adapter = MosaicAdapter(showMosaicImages, mosaicViews.toList()) {
                    if(!it.original) {
                        openMosaicDetails(it)
                    } else {
                        //openPictureChooser()
                    }
                }
                val colRows = Paper.book().read<String>("${requireContext().getCurrentEventName()}:columns:rows", "")
                if(!colRows.isNullOrEmpty()) {
                    NUM_COLUMNS = colRows.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8
                }
                binding.rvMosaic.layoutManager = GridLayoutManager(requireContext(), NUM_COLUMNS)
                binding.rvMosaic.adapter = adapter

            }
            .subscribe()

        binding.bannerMosaic.setOnClickListener {
            val showMosaicImages = sharedPreferences.getBoolean("settings:showMosaicImages", false)
            val showOrHideImagesBtn = if(showMosaicImages) "Hide Images" else "Show Images"
            val menuItems = arrayOf("Auto Fill", showOrHideImagesBtn)

            requireContext().showMenuDialog("Choose an option", menuItems) { index ->
                when (index) {
                    0 -> {
                        MosaicManager.autoFill(requireContext())
                        updateMosaicInfo()
                    }
                    1 -> {
                        val edit = sharedPreferences.edit()
                        edit.putBoolean("settings:showMosaicImages", !showMosaicImages)
                        edit.apply()
                        updateMosaicInfo()
                    }
                    2 -> { /* Action for Item 3 */ }
                }
            }
        }

        binding.btnSendToPrint.setOnClickListener {
            println("hhh clicked!!!")
            binding.btnSendToPrint.fadeOutAndDisable(2000)
            MosaicManager.moveToPrintsToMerge(requireContext())
            updateMosaicInfo()
        }

        binding.refreshButton.setOnClickListener {
            initViews()
        }

        binding.downloadContainer.setOnClickListener {
            findNavController().navigate(R.id.action_mosaicFragment_to_mosaicDownloadFragment)
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.tvAutoPrint.setOnClickListener {
            requireContext().showInputDialog("Ade l 3ared", "10") {

            }
        }

        binding.settingsButton.setOnClickListener {
            openPictureChooser()
            //findNavController().navigate(R.id.action_mosaicFragment_to_mosaicSettingsFragment)
        }
//        binding.btn.setOnClickListener {
//            findNavController().navigate(R.id.action_mosaicFragment_to_mosaicDownloadFragment)
//        }
    }

    fun openPictureChooser() {
        val fragment = ChoosePictureDialogFragment.newInstance("")
        fragment.listener = object: ChoosePictureDialogFragmentListener {
            override fun fillThisPic(file: File) {
                MosaicManager.fillRandomPic(requireContext(), file)
            }

        }
        fragment.show(parentFragmentManager, "")
    }

    fun updateMosaicInfo(isFirstTime: Boolean? = false) {
        //initViews()
        if(isFirstTime == false) fetchMosaicViews()

        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Originals: ${MosaicManager.mosaic_originals.listFiles()?.size}")
        stringBuilder.appendLine("Drafts: ${MosaicManager.mosaic_draft.listFiles()?.size}")
        stringBuilder.appendLine("Images: ${MosaicManager.mosaic_images.listFiles()?.size}")
        stringBuilder.appendLine("To Print: ${MosaicManager.mosaic_toPrint.listFiles()?.size}")
        stringBuilder.appendLine("Done: ${MosaicManager.mosaic_done.listFiles()?.size}")
        stringBuilder.appendLine("Merge: ${MosaicManager.mosaic_merge.listFiles()?.size}")
        stringBuilder.appendLine("MosaicPrint: ${MosaicManager.mosaic_print.listFiles()?.size}")

        binding.mosaicSummary.text = stringBuilder.toString()

        binding.btnSendToPrint.text = "${MosaicManager.mosaic_toPrint.list()?.size}/${MosaicManager.countMosaic}"



        //val showMosaicImages = sharedPreferences.getBoolean("settings:showMosaicImages", false)
        //adapter?.notify(showMosaicImages)
    }
}

fun Button.fadeOutAndDisable(duration: Long = 2000L) {
    // Disable the button
    this.isEnabled = false

    // Fade out the button
    this.animate()
        .alpha(.5f)
        .setDuration(duration)
        .withEndAction {
            // After the fade out animation, wait for 2 seconds and then restore the button
            this.postDelayed({
                // Enable the button
                this.isEnabled = true

                // Restore the button
                this.alpha = 1f
            }, 2000)
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
        println("hhh clicked delayed done!")
        isClickable = true
        this.isClickable = true
        this.alpha = 1F
    }, delay)
}