package com.puzzlebooth.server.album

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.models.RemotePhoto
import com.puzzlebooth.main.models.RemotePhotoRequest
import com.puzzlebooth.main.utils.FileClientLegacy
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.MainActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.album.listing.AlbumAdapter
import com.puzzlebooth.server.album.listing.LocalImage
import com.puzzlebooth.server.album.listing.PhotosAdapter
import com.puzzlebooth.server.databinding.FragmentAlbumBinding
import io.paperdb.Paper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class AlbumFragment : BaseFragment<FragmentAlbumBinding>(R.layout.fragment_album) {

    var currentPositionSelected = -1

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "albumNext" -> binding.btnNext.performClick()
            event?.text == "albumPrevious" -> binding.btnPrevious.performClick()
            event?.text?.startsWith("albumPrint:") == true -> printPhoto(event.text.removePrefix("albumPrint:").toIntOrNull() ?: -1)
            event?.text?.startsWith("albumQR:") == true -> getQRPhoto(event.text.removePrefix("albumQR:").toIntOrNull() ?: -1)
            event?.text == "reset" -> findNavController().popBackStack()
        }
    }

    private fun getQRPhoto(index: Int) {
        if(index == -1) return

        val fileToQR = localFiles.firstOrNull { it.position == index }

        val fileName = fileToQR?.file?.name
        val filePath = fileToQR?.file?.path

        if(fileName != null && filePath != null) {
            if(sharedPreferences.getBoolean("settings:showQR", false)) {
                processPrintingAction(fileName, filePath, "print:;;;")
            } else {
                processPrintingAction(fileName, filePath, "print")
            }
        }
    }

    private fun printPhoto(index: Int) {
        if(index == -1) return

        val fileToPrint = localFiles.firstOrNull { it.position == index }
        fileToPrint?.file?.let { file ->
            File(file.path).copyTo(File("${requireContext().draftPath()}${file.name}"), true)
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

        refreshData()
        initViews()
    }


    private fun initViews() {
        val adapter = PhotosAdapter(requireContext(), localFiles.toList())
//        binding.photoGrid.adapter = adapter
//
//        binding.photoGrid.setOnItemClickListener { _, view, position, _ ->
//            // Handle item click
//            currentPosition = position
//            adapter.setSelectedPosition(position)
//        }
//
        binding.btnPrevious.setOnClickListener {
            binding.rvAlbum.scrollBy(0, -200)
//            currentPage -= 1
//            refreshData()
//            if (currentPosition > 0) {
//                currentPosition--
//                if(currentPosition%2==0)
//                    binding.photoGrid.scrollListBy((requireContext().getScreenHeight() - 100f.dpToPx(requireContext())).toInt()/3)
//                binding.photoGrid.setSelection(currentPosition)
//                adapter.setSelectedPosition(currentPosition)
//            }
        }

        binding.btnNext.setOnClickListener {
            binding.rvAlbum.scrollBy(0, 200)
//            if (currentPosition < localFiles.size - 1) {
//                currentPosition++
//                if(currentPosition%2==0)
//                    binding.photoGrid.scrollListBy((requireContext().getScreenHeight() - 100f.dpToPx(requireContext())).toInt()/3)
//                binding.photoGrid.setSelection(currentPosition)
//                adapter.setSelectedPosition(currentPosition)
//            }
        }

        binding.btnQR.setOnClickListener {
            getQRPhoto(currentPositionSelected)
        }

        binding.btnPrint.setOnClickListener {
            printPhoto(currentPositionSelected)

//            val landscape = sharedPreferences.getBoolean("settings:landscape", false)
//            if(landscape) {
//                val thread = Thread {
//                    try {
//                        val ip = sharedPreferences.getString("ip", "") ?: return@Thread
//                        val port = sharedPreferences.getString("port", "") ?: return@Thread
//                        port.toIntOrNull() ?: return@Thread
//
//                        FileClientLegacy(
//                            ip,
//                            13456,
//                            localFiles[currentPosition].file.path
//                        )
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//
//                thread.start()
//            } else {
//                println("hhh printing ${localFiles[currentPosition].file.path}")
//                File(localFiles[currentPosition].file.path).copyTo(File("${requireContext().draftPath()}${localFiles[currentPosition].file.name}"), true)
//            }
        }
    }

    fun processPrintingAction(fileName: String, normalPath: String, event: String) {
        if(event.contains(":")) {
            val substring = event.substringAfter(":") ?: ""
            val array = substring.split(";")
            val email = array[1]
            val personName = array[0]
            val phone = array[2]

            if(email.isEmpty() && personName.isEmpty() && phone.isEmpty()) {
                val requestBody: RequestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "fileToUpload",
                        fileName,
                        RequestBody.create("image/jpeg".toMediaTypeOrNull(), File(normalPath))
                    )
                    .build()

                // upload without db entry
                service
                    .uploadPhotoFile(requestBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError {
                        it.printStackTrace()
                    }
                    .doOnNext {
                        (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image.php?link=${fileName}")
                    }
                    .subscribe()
            } else {
                val remotePhoto = RemotePhoto(
                    name = fileName,
                    phone = phone,
                    personName = personName,
                    email = email,
                    sender = 0)

                val request = RemotePhotoRequest(
                    listOf(
                        remotePhoto
                    )
                )

                Paper.book().write(fileName, remotePhoto)

                val requestBody: RequestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "fileToUpload",
                        fileName,
                        RequestBody.create("image/jpeg".toMediaTypeOrNull(), File(normalPath))
                    )
                    .build()


                val requests = listOf(service.uploadPhotoNumber(request), service.uploadPhotoFile(requestBody))

                service
                    .uploadPhotoNumber(request)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError {
                        it.printStackTrace()
                    }
                    .doOnNext {
                        service
                            .uploadPhotoFile(requestBody)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnError {
                                it.printStackTrace()
                            }
                            .doOnNext {
                                (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image.php?link=${fileName}")
                            }
                            .subscribe()
                    }
                    .subscribe()
            }
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

    private fun refreshData() {
        localFiles.clear()
        val file = File(requireContext().getCurrentEventPhotosPath())

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

        albumAdapter = AlbumAdapter(localFiles.toList()) {
            currentPositionSelected = it.position.toString().toIntOrNull() ?: -1
            //albumAdapter.setSelectedPosition(it.toIntOrNull() ?: -1)
        }
//
        binding.rvAlbum.apply {
            this.adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 5, RecyclerView.VERTICAL, false)
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