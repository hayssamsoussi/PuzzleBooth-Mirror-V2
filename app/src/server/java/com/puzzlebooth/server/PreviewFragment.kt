package com.puzzlebooth.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.FileUtils
import android.view.View
import androidx.compose.ui.unit.Constraints
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.models.RemotePhoto
import com.puzzlebooth.main.models.RemotePhotoRequest
import com.puzzlebooth.main.utils.Constants
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.draftPathCutIn2
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.main.utils.mosaicDraftPath
import com.puzzlebooth.server.databinding.FragmentPreviewBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.settings.PhotoQuality
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviewFragment : BaseFragment<FragmentPreviewBinding>(R.layout.fragment_preview) {

    private var resultBitmap: Bitmap? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "cancel" -> binding.btnCancel.performClick()
            event?.text?.startsWith("print") == true -> {
                if (sharedPreferences.getBoolean("settings:showQR", false)) {
                    processPrintingAction(event.text)
                } else {
                    processPrintingAction("print")
                }
            }
            event?.text == "save" -> binding.btnSave?.performClick()
            event?.text == "retake" -> binding.btnRetake.performClick()
        }
    }


    private fun processPrintingAction(event: String) {
        Constants.printerOne = !Constants.printerOne

        val isUpload = event.contains(":")
        val pair = saveFileToDrafts()
        val fileName = pair.first
        val normalPath = pair.second

        if (isUpload) {
            val substring = event.substringAfter(":") ?: ""
            val array = substring.split(";")
            val email = array[1]
            val personName = array[0]
            val phone = array[2]

            if (email.isEmpty() && personName.isEmpty() && phone.isEmpty()) {
                uploadFile(fileName, normalPath)
            } else {
                uploadFileWithDBEntry(fileName, normalPath, phone, personName, email)
            }
        }

        if (MosaicManager.isRunning()) {
            val mosaicPath = requireContext().mosaicDraftPath()
            File(mosaicPath).mkdirs()

            if (MosaicManager.original) {
                File("$normalPath$fileName").copyTo(
                    File("${requireContext().draftPath()}/$fileName"),
                    true
                )
            }

            File("$normalPath$fileName").copyTo(
                File("${requireContext().mosaicDraftPath()}/$fileName"),
                true
            )
        } else {
            File("$normalPath$fileName").copyTo(
                File("${requireContext().draftPath()}$fileName"),
                true
            )

            val secondCopy = sharedPreferences.getBoolean("settings:twoCopies", false)
            if(secondCopy) {
                File("$normalPath$fileName").copyTo(
                    File("${requireContext().draftPath()}COPY_$fileName"),
                    true
                )
            }
        }

        findNavController().navigate(R.id.action_previewFragment_to_printFragment)
    }



    private fun initViews() {
        binding.buttonsContainer?.visibility = if(sharedPreferences.getBoolean("settings:touchMode", false)) View.VISIBLE else View.GONE
        binding.btnPrint.setOnClickListener {
            if(sharedPreferences.getBoolean("settings:showQR", false)) {
                processPrintingAction("print:;;;")
            } else {
                processPrintingAction("print")
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_startFragment)
        }

        binding.btnRetake.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_countdownFragment)
        }

        binding.btnSave?.setOnClickListener {
            saveFileToDrafts()
            findNavController().navigate(R.id.action_previewFragment_to_printFragment)
        }
    }

    fun uploadFile(fileName: String, normalPath: String) {
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "fileToUpload",
                fileName,
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), File("$normalPath$fileName"))
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
                val followQR = sharedPreferences.getBoolean("settings:followQR", false)
                if(followQR) {
                    (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image.php?link=${fileName}")
                } else {
                    (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image_unlocked.php?link=${fileName}")
                }
            }
            .subscribe()
    }

    private fun saveFileToDrafts(): Pair<String, String> {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)
        val selectedLayout = sharedPreferences.getString("selectedLayout", "")
        val quality = PhotoQuality.getCurrentQualityInt(requireContext())

        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timeStamp: String = timeFormat.format(Date())
        val fileName = "${timeStamp}_$selectedLayout.jpeg"

        val normalPath = requireContext().getCurrentEventPhotosPath()
        val draftPath = requireContext().draftPath()
        val draftPathCutIn2 = requireContext().draftPathCutIn2()

        File(normalPath).mkdirs()
        File(draftPath).mkdirs()
        File(draftPathCutIn2).mkdirs()

        val file = FileOutputStream("$normalPath$fileName")

        resultBitmap?.rotate(if(landscape) 90F else 0F)?.compress(Bitmap.CompressFormat.JPEG, quality, file)

        return Pair(fileName, normalPath)
    }

    fun uploadFileWithDBEntry(fileName: String, normalPath: String, phone: String, personName: String, email: String) {
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
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), File("$normalPath$fileName"))
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
                        val followQR = sharedPreferences.getBoolean("settings:followQR", false)
                        if(followQR) {
                            (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image.php?link=${fileName}")
                        } else {
                            (requireActivity() as? MainActivity)?.showQRCode("https://puzzleslb.com/puzzlebooth/show_image_unlocked.php?link=${fileName}")
                        }
                    }
                    .subscribe()
            }
            .subscribe()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun initViewBinding(view: View): FragmentPreviewBinding {
        return FragmentPreviewBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        resultBitmap = processPhoto()
        displayBitmap()
    }

    private fun displayBitmap() {
        // Display result
        Glide.with(requireContext())
            .load(resultBitmap)
            .into(binding.imageView)
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if(drawable == null) return null

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        // Create a Bitmap with the specified width and height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create a Canvas and associate it with the Bitmap
        val canvas = Canvas(bitmap)

        // Set the bounds of the Drawable
        drawable.setBounds(0, 0, canvas.width, canvas.height)

        // Draw the Drawable onto the Canvas
        drawable.draw(canvas)

        return bitmap
    }

    private fun processPhoto(): Bitmap? {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)
        val baseBitmap = com.puzzlebooth.server.CountdownFragment.capturedPhoto ?: return null
        val layoutName = sharedPreferences.getString("selectedLayout", "")
        val layoutPath = "${requireContext().cacheDir}/layouts/${layoutName}"
        var overlayBitmap = if(layoutName.isNullOrEmpty()) drawableToBitmap(ContextCompat.getDrawable(requireContext(), R.drawable.blank)) else BitmapFactory.decodeFile(layoutPath)

        if(overlayBitmap != null) {
            // Scale the overlay bitmap to fit the height of the base bitmap
            if(landscape)
                overlayBitmap = overlayBitmap.rotate(270F)

            val scaledOverlayWidth = baseBitmap.height * overlayBitmap.width / overlayBitmap.height
            val scaledOverlayBitmap = Bitmap.createScaledBitmap(overlayBitmap, scaledOverlayWidth, baseBitmap.height, true)

            // Crop the base bitmap if it's wider than the scaled overlay
            val croppedBaseBitmap = if (baseBitmap.width > scaledOverlayWidth) {
                val cropStartX = (baseBitmap.width - scaledOverlayWidth) / 2
                Bitmap.createBitmap(baseBitmap, cropStartX, 0, scaledOverlayWidth, baseBitmap.height)
            } else {
                baseBitmap
            }

            // Create a bitmap to hold the final combined image
            val resultBitmap = Bitmap.createBitmap(scaledOverlayWidth, baseBitmap.height, baseBitmap.config)

            // Draw the cropped base bitmap and then the scaled overlay bitmap onto the canvas
            Canvas(resultBitmap).apply {
                drawBitmap(croppedBaseBitmap, 0f, 0f, null)
                drawBitmap(scaledOverlayBitmap, 0f, 0f, null)
            }

            return resultBitmap
        }

        return null
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}