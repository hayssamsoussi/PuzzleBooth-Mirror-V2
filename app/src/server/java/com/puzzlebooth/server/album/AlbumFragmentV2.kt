package com.puzzlebooth.server.album

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.MaterialShapeDrawable
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

class AlbumFragmentV2 : BaseFragment<FragmentAlbumBinding>(R.layout.fragment_album) {

    private var selectedPhoto: LocalImage? = null
    private var localFiles = mutableListOf<LocalImage>()
    private lateinit var albumAdapter: AlbumAdapter

    // Progress indicator related variables
    private var progressVisible = false
    private var isProcessing = false

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "albumNext" -> moveSelection(1)
            event?.text == "albumPrevious" -> moveSelection(-1)
            event?.text?.startsWith("albumPrint:") == true -> {
                val index = event.text.removePrefix("albumPrint:").toIntOrNull() ?: return
                selectPhotoByIndex(index)
                printSelectedPhoto()
            }
            event?.text?.startsWith("albumQR:") == true -> {
                val index = event.text.removePrefix("albumQR:").toIntOrNull() ?: return
                selectPhotoByIndex(index)
                displayQRForSelectedPhoto()
            }
            event?.text == "reset" -> findNavController().popBackStack()
        }
    }

    override fun initViewBinding(view: View): FragmentAlbumBinding {
        return FragmentAlbumBinding.bind(view)
    }

    /**
     * Shows or hides the progress indicator
     * @param show Whether to show or hide the progress
     * @param message Optional message to display with the progress
     */
    private fun showProgress(show: Boolean, message: String = "") {
        progressVisible = show

        if (show) {
            binding.progressOverlay.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE

            if (message.isNotEmpty()) {
                binding.progressMessage.text = message
                binding.progressMessage.visibility = View.VISIBLE
            } else {
                binding.progressMessage.visibility = View.GONE
            }

            // Disable buttons while progress is showing
            setButtonsEnabled(false)
        } else {
            binding.progressOverlay.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.progressMessage.visibility = View.GONE

            // Re-enable buttons
            setButtonsEnabled(true)
        }
    }

    /**
     * Enable or disable all buttons while progress is showing
     */
    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnPrint.isEnabled = enabled
        binding.btnQR.isEnabled = enabled
        binding.btnNext.isEnabled = enabled
        binding.btnPrevious.isEnabled = enabled
        binding.btnExit.isEnabled = enabled
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPhotos()
        initViews()
    }

    private fun initViews() {
        albumAdapter = AlbumAdapter(localFiles) { photo ->
            selectedPhoto = photo
            albumAdapter.setSelectedPhoto(photo)
        }

        binding.rvAlbum.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
        }

        binding.btnExit.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.btnPrevious.setOnClickListener {
            moveSelection(-1)
        }

        binding.btnNext.setOnClickListener {
            moveSelection(1)
        }

        binding.btnQR.setOnClickListener {
            displayQRForSelectedPhoto()
        }

        binding.btnPrint.setOnClickListener {
            printSelectedPhoto()
        }
    }

    private fun loadPhotos() {
        localFiles.clear()
        val directory = File(requireContext().getCurrentEventPhotosPath())

        val files = directory.listFiles()?.filter {
            it.isFile && it.extension.equals("jpeg", true)
        }

        if (!files.isNullOrEmpty()) {
            files.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                localFiles.add(LocalImage(file, index))
            }
        }
    }

    private fun moveSelection(direction: Int) {
        if (localFiles.isEmpty()) return

        // Find current selection index
        val currentIndex = selectedPhoto?.let { localFiles.indexOf(it) } ?: -1

        // Calculate new index
        val newIndex = when {
            currentIndex == -1 -> if (direction > 0) 0 else localFiles.size - 1
            else -> {
                val proposed = currentIndex + direction
                when {
                    proposed < 0 -> localFiles.size - 1
                    proposed >= localFiles.size -> 0
                    else -> proposed
                }
            }
        }

        // Update selection
        selectedPhoto = localFiles[newIndex]
        albumAdapter.setSelectedPhoto(selectedPhoto)

        // Scroll to the selected photo
        binding.rvAlbum.smoothScrollToPosition(newIndex)
    }

    private fun selectPhotoByIndex(index: Int) {
        val photo = localFiles.getOrNull(index) ?: return
        selectedPhoto = photo
        albumAdapter.setSelectedPhoto(photo)

        // Scroll to the selected photo
        binding.rvAlbum.smoothScrollToPosition(index)
    }

    private fun printSelectedPhoto() {
        val photo = selectedPhoto ?: return

        // Prevent multiple prints while already processing
        if (isProcessing) return
        isProcessing = true

        val file = photo.file

        // Show printing progress
        showProgress(true, "Printing photo...")

        try {
            File(file.path).copyTo(File("${requireContext().draftPath()}${file.name}"), true)
            // Hide progress after a short delay to make it visible to the user
            binding.root.postDelayed({
                showProgress(false)
                isProcessing = false
            }, 1500)
        } catch (e: Exception) {
            showProgress(false)
            isProcessing = false
            // Optional: show error toast
        }
    }

    private fun displayQRForSelectedPhoto() {
        val photo = selectedPhoto ?: return

        // Prevent multiple QR code generations while already processing
        if (isProcessing) return
        isProcessing = true

        val fileName = photo.file.name
        val filePath = photo.file.path

        // Show QR code generation progress
        showProgress(true, "Generating QR code...")

        try {
            if (sharedPreferences.getBoolean("settings:showQR", false)) {
                processPrintingAction(fileName, filePath, "print:;;;")
            } else {
                processPrintingAction(fileName, filePath, "print")
            }
            // Progress will be hidden in showQRCode() method
        } catch (e: Exception) {
            showProgress(false)
            isProcessing = false
            // Optional: show error toast
        }
    }

    fun processPrintingAction(fileName: String, normalPath: String, event: String) {
        if(event.contains(":")) {
            val substring = event.substringAfter(":") ?: ""
            val array = substring.split(";")
            val email = array.getOrNull(1) ?: ""
            val personName = array.getOrNull(0) ?: ""
            val phone = array.getOrNull(2) ?: ""

            if(email.isEmpty() && personName.isEmpty() && phone.isEmpty()) {
                uploadPhotoWithoutDatabaseEntry(fileName, normalPath)
            } else {
                uploadPhotoWithUserInfo(fileName, normalPath, personName, email, phone)
            }
        } else {
            uploadPhotoWithoutDatabaseEntry(fileName, normalPath)
        }
    }

    private fun uploadPhotoWithoutDatabaseEntry(fileName: String, filePath: String) {
        val requestBody: RequestBody = createMultipartRequest(fileName, filePath)

        service
            .uploadPhotoFile(requestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { it.printStackTrace() }
            .doOnNext { showQRCode(fileName) }
            .subscribe()
    }

    private fun uploadPhotoWithUserInfo(fileName: String, filePath: String, personName: String, email: String, phone: String) {
        val remotePhoto = RemotePhoto(
            name = fileName,
            phone = phone,
            personName = personName,
            email = email,
            sender = 0
        )

        val request = RemotePhotoRequest(listOf(remotePhoto))
        val requestBody: RequestBody = createMultipartRequest(fileName, filePath)

        Paper.book().write(fileName, remotePhoto)

        service
            .uploadPhotoNumber(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { it.printStackTrace() }
            .doOnNext {
                service
                    .uploadPhotoFile(requestBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { it.printStackTrace() }
                    .doOnNext { showQRCode(fileName) }
                    .subscribe()
            }
            .subscribe()
    }

    private fun createMultipartRequest(fileName: String, filePath: String): RequestBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "fileToUpload",
                fileName,
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), File(filePath))
            )
            .build()
    }

    private fun showQRCode(fileName: String) {
        val followQR = sharedPreferences.getBoolean("settings:followQR", false)
        val url = if(followQR) {
            "https://puzzleslb.com/api/mirror/show_image.php?link=$fileName"
        } else {
            "https://puzzleslb.com/api/mirror/show_image_unlocked.php?link=$fileName"
        }

        // Hide progress before showing QR code
        showProgress(false)
        isProcessing = false

        // Show QR code in MainActivity
        (requireActivity() as? MainActivity)?.showQRCode(url)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}

// Border color property
var ImageView.borderColor: Int
    get() {
        return when (this) {
            is ShapeableImageView -> {
                (this.background as? MaterialShapeDrawable)?.strokeColor?.defaultColor ?: 0
            }
            else -> {
                (this.background as? GradientDrawable)?.getStroke()?.first ?: 0
            }
        }
    }
    set(value) {
        when (this) {
            is ShapeableImageView -> {
                val materialShapeDrawable = MaterialShapeDrawable(this.shapeAppearanceModel)
                materialShapeDrawable.strokeColor = ColorStateList.valueOf(value)
                materialShapeDrawable.strokeWidth = this.borderWidth
                this.background = materialShapeDrawable
            }
            else -> {
                val drawable = this.background as? GradientDrawable ?: GradientDrawable()
                drawable.setStroke(this.borderWidth.toInt(), value)
                this.background = drawable
            }
        }
    }

// Border width property
var ImageView.borderWidth: Float
    get() {
        return when (this) {
            is ShapeableImageView -> {
                (this.background as? MaterialShapeDrawable)?.strokeWidth ?: 0f
            }
            else -> {
                (this.background as? GradientDrawable)?.getStroke()?.second?.toFloat() ?: 0f
            }
        }
    }
    set(value) {
        when (this) {
            is ShapeableImageView -> {
                val materialShapeDrawable = (this.background as? MaterialShapeDrawable)
                    ?: MaterialShapeDrawable(this.shapeAppearanceModel)
                materialShapeDrawable.strokeWidth = value
                materialShapeDrawable.strokeColor = ColorStateList.valueOf(this.borderColor)
                this.background = materialShapeDrawable
            }
            else -> {
                val drawable = this.background as? GradientDrawable ?: GradientDrawable()
                drawable.setStroke(value.toInt(), this.borderColor)
                this.background = drawable
            }
        }
    }

// Helper extension to get stroke details from GradientDrawable
private fun GradientDrawable.getStroke(): Pair<Int, Int>? {
    try {
        val strokeStateClass = Class.forName("android.graphics.drawable.GradientDrawable\$GradientState")
        val strokeState = this.javaClass.getDeclaredMethod("getGradientState").invoke(this)
        val strokeColorField = strokeStateClass.getDeclaredField("mStrokeColor")
        val strokeWidthField = strokeStateClass.getDeclaredField("mStrokeWidth")

        strokeColorField.isAccessible = true
        strokeWidthField.isAccessible = true

        val strokeColor = strokeColorField.get(strokeState) as Int
        val strokeWidth = strokeWidthField.get(strokeState) as Int

        return Pair(strokeColor, strokeWidth)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}