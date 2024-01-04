package com.puzzlebooth.server.album

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.FileClientLegacy
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.R
import com.puzzlebooth.server.album.listing.AlbumAdapter
import com.puzzlebooth.server.album.listing.LocalImage
import com.puzzlebooth.server.album.listing.PhotosAdapter
import com.puzzlebooth.server.databinding.FragmentAlbumBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class AlbumFragment : BaseFragment<FragmentAlbumBinding>(R.layout.fragment_album) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "albumNext" -> binding.btnNext.performClick()
            "albumPrevious" -> binding.btnPrevious.performClick()
            "albumPrint" -> binding.btnPrint.performClick()
            "reset" -> findNavController().popBackStack()
        }
    }

    private var currentPosition = 0

    
    override fun initViewBinding(view: View): FragmentAlbumBinding {
        return FragmentAlbumBinding.bind(view)
    }

    private var localFiles = mutableListOf<LocalImage>()

    private lateinit var albumAdapter: AlbumAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        initViews()
    }

    private fun initViews() {
        val adapter = PhotosAdapter(requireContext(), localFiles.toList())
        binding.photoGrid.adapter = adapter

        binding.photoGrid.setOnItemClickListener { _, view, position, _ ->
            // Handle item click
            currentPosition = position
            adapter.setSelectedPosition(position)
        }

        binding.btnPrevious.setOnClickListener {
            if (currentPosition > 0) {
                currentPosition--
                if(currentPosition%2==0)
                    binding.photoGrid.scrollListBy((requireContext().getScreenHeight() - 100f.dpToPx(requireContext())).toInt()/3)
                binding.photoGrid.setSelection(currentPosition)
                adapter.setSelectedPosition(currentPosition)
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentPosition < localFiles.size - 1) {
                currentPosition++
                if(currentPosition%2==0)
                    binding.photoGrid.scrollListBy((requireContext().getScreenHeight() - 100f.dpToPx(requireContext())).toInt()/3)
                binding.photoGrid.setSelection(currentPosition)
                adapter.setSelectedPosition(currentPosition)
            }
        }

        binding.btnPrint.setOnClickListener {
            val landscape = sharedPreferences.getBoolean("settings:landscape", false)
            if(landscape) {
                val thread = Thread {
                    try {
                        val ip = sharedPreferences.getString("ip", "") ?: return@Thread
                        val port = sharedPreferences.getString("port", "") ?: return@Thread
                        port.toIntOrNull() ?: return@Thread

                        FileClientLegacy(
                            ip,
                            13456,
                            localFiles[currentPosition].file.path
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                thread.start()
            } else {
                println("hhh printing ${localFiles[currentPosition].file.path}")
                File(localFiles[currentPosition].file.path).copyTo(File("${requireContext().draftPath()}${localFiles[currentPosition].file.name}"), true)
            }
        }



//        albumAdapter = AlbumAdapter(localFiles.toList()) {
//            println("hhh action $it")
//        }
//
//        binding.rvAlbum.apply {
//            adapter = albumAdapter
//            layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
//        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun initData() {
        localFiles.clear()
        val file = File(requireContext().getCurrentEventPhotosPath())

        val twodaysmillis = 172800000
        val monthmillis = 172800000
        val twodaysago = if(false) (System.currentTimeMillis() - monthmillis) else 0
        val files = file.listFiles()?.filter {
            it.isFile && it.extension.equals("jpeg", true) &&
                    it.lastModified() > twodaysago
        }

        if(!files.isNullOrEmpty()) {
            files.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                localFiles.add(LocalImage(file, index))
            }
        }
    }
}


fun Context.getScreenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}

fun Context.getScreenWidth(): Int {
    val displayMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

fun Int.pxToDp(context: Context): Float {
    val displayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, this.toFloat(), displayMetrics)
}

fun Float.dpToPx(context: Context): Float {
    val displayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, displayMetrics)
}