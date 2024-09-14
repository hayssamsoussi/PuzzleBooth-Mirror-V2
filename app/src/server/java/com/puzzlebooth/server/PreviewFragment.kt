package com.puzzlebooth.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
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
            if(sharedPreferences.getBoolean("settings:multiPhoto", false)) {
                File("$normalPath$fileName").copyTo(
                    File("${requireContext().draftPathCutIn2()}$fileName"),
                    true
                )
            } else {
                File("$normalPath$fileName").copyTo(
                    File("${requireContext().draftPath()}$fileName"),
                    true
                )
            }


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
        binding.buttonsContainer?.visibility = if(sharedPreferences.getBoolean("settings:touchMode", true)) View.VISIBLE else View.GONE
        binding.btnPrint.setOnClickListener {
            printAction("print:;;;")
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_startFragment)
        }

        binding.btnRetake.setOnClickListener {
            if(isMultiPhoto()) {
                if(CountdownFragment.getCapturedPhoto3(requireContext()) != null) {
                    CountdownFragment.setCapturedPhoto3(requireContext(), null)
                } else if(CountdownFragment.getCapturedPhoto2(requireContext()) != null) {
                    CountdownFragment.setCapturedPhoto2(requireContext(), null)
                } else if(CountdownFragment.getCapturedPhoto(requireContext()) != null) {
                    CountdownFragment.setCapturedPhoto(requireContext(), null)
                }
            }

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
        val keyName = if(sharedPreferences.getBoolean("settings:multiPhoto", false)) "selectMultiLayout" else "selectedLayout"
        val selectedLayout = sharedPreferences.getString(keyName, "")
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

        resultBitmap?.compress(Bitmap.CompressFormat.JPEG, quality, file)

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

    fun isMultiPhoto(): Boolean {
        return sharedPreferences.getBoolean("settings:multiPhoto", false)
    }

    fun isMultiLayoutDone(): Boolean {
        if(CountdownFragment.getCapturedPhoto(requireContext()) == null) {
            println("hhh capturedPhoto is null")
            return false
        }
        if(CountdownFragment.getCapturedPhoto2(requireContext()) == null) {
            println("hhh capturedPhoto2 is null")
            return false
        }
        if(CountdownFragment.getCapturedPhoto3(requireContext()) == null) {
            println("hhh capturedPhoto3 is null")
            return false
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()

        if(isMultiPhoto()) {
            // we show image and then we go back to coutndown unless 3 photos taken
            if(isMultiLayoutDone()) {
                resultBitmap = processPhoto(createBitmapBWithAlignedCopiesOfA(
                    createBitmapPageWithImages(
                        CountdownFragment.getCapturedPhoto(requireContext())!!,
                        CountdownFragment.getCapturedPhoto2(requireContext())!!,
                        CountdownFragment.getCapturedPhoto3(requireContext())!!
                    )
                ))
            } else {
                listOf(CountdownFragment.getCapturedPhoto(requireContext()), CountdownFragment.getCapturedPhoto2(requireContext()), CountdownFragment.getCapturedPhoto3(requireContext())).findLast { it != null }.let {
                    resultBitmap = it
                }
            }
        } else {
            resultBitmap = CountdownFragment.getCapturedPhoto(requireContext())?.let { processPhoto(it) }
        }

        displayBitmap()
    }

    private fun displayBitmap() {
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

    // Function to convert cm to pixels
    fun cmToPixels(cm: Float, dpi: Int): Int {
//
//        val inches = cm / 2.54f // 1 inch = 2.54 cm
//        return (inches * dpi).toInt()
        println("hhh ${cm} to ${cm*100}")
        return (cm*1000).toInt()
    }

    // Function to crop the bitmap to the desired size, maintaining the aspect ratio
    fun cropBitmapToSize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calculate aspect ratios
        val aspectRatioBitmap = originalWidth.toFloat() / originalHeight
        val aspectRatioTarget = targetWidth.toFloat() / targetHeight

        // Calculate the dimensions to crop based on the aspect ratio
        var cropWidth = originalWidth
        var cropHeight = originalHeight

        if (aspectRatioBitmap > aspectRatioTarget) {
            // Crop width to match target aspect ratio
            cropWidth = (originalHeight * aspectRatioTarget).toInt()
        } else {
            // Crop height to match target aspect ratio
            cropHeight = (originalWidth / aspectRatioTarget).toInt()
        }

        // Calculate the crop starting points
        val xOffset = (originalWidth - cropWidth) / 2
        val yOffset = (originalHeight - cropHeight) / 2

        // Create the cropped bitmap
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, cropWidth, cropHeight)
    }

    fun createBitmapPageWithImages(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        bitmap3: Bitmap,
        dpi: Int = 300 // Default DPI for print resolution
    ): Bitmap {
        // Convert page dimensions from cm to pixels
        val pageWidthPx = cmToPixels(5f, dpi)
        val pageHeightPx = cmToPixels(15f, dpi)

        // Create a blank page bitmap
        val resultBitmap = Bitmap.createBitmap(pageWidthPx, pageHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()

        // Fill the canvas with a white background
        canvas.drawColor(Color.WHITE)

        // Dimensions for the bitmaps after cropping
        val targetBitmapWidthPx = cmToPixels(4f, dpi) // 6 cm in width
        val targetBitmapHeightPx = cmToPixels(4f, dpi) // 4 cm in height

        // Calculate margins and spacing
        val topMarginPx = cmToPixels(.5f, dpi) // 1 cm top margin
        val verticalSpacingPx = cmToPixels(.2f, dpi) // 1 cm spacing between bitmaps

        // Calculate the horizontal offset to center the bitmaps
        val horizontalOffsetPx = (pageWidthPx - targetBitmapWidthPx) / 2

        // Crop and place the first bitmap (centered horizontally)
        val croppedBitmap1 = cropBitmapToSize(bitmap1, targetBitmapWidthPx, targetBitmapHeightPx)
        canvas.drawBitmap(
            croppedBitmap1,
            null,
            Rect(
                horizontalOffsetPx,
                topMarginPx,
                horizontalOffsetPx + targetBitmapWidthPx,
                topMarginPx + targetBitmapHeightPx
            ),
            paint
        )

        // Calculate the Y position for the second bitmap
        val secondBitmapTopPx = topMarginPx + targetBitmapHeightPx + verticalSpacingPx
        val croppedBitmap2 = cropBitmapToSize(bitmap2, targetBitmapWidthPx, targetBitmapHeightPx)
        canvas.drawBitmap(
            croppedBitmap2,
            null,
            Rect(
                horizontalOffsetPx,
                secondBitmapTopPx,
                horizontalOffsetPx + targetBitmapWidthPx,
                secondBitmapTopPx + targetBitmapHeightPx
            ),
            paint
        )

        // Calculate the Y position for the third bitmap
        val thirdBitmapTopPx = secondBitmapTopPx + targetBitmapHeightPx + verticalSpacingPx
        val croppedBitmap3 = cropBitmapToSize(bitmap3, targetBitmapWidthPx, targetBitmapHeightPx)
        canvas.drawBitmap(
            croppedBitmap3,
            null,
            Rect(
                horizontalOffsetPx,
                thirdBitmapTopPx,
                horizontalOffsetPx + targetBitmapWidthPx,
                thirdBitmapTopPx + targetBitmapHeightPx
            ),
            paint
        )

        return resultBitmap
    }

    fun createBitmapBWithAlignedCopiesOfA(
        bitmapA: Bitmap, // Parent A (7.5 cm x 15 cm)
        dpi: Int = 300 // Default DPI for print resolution
    ): Bitmap {
        // Define dimensions of A and B in cm
        val widthACm = 5f
        val heightACm = 15f
        val widthBCm = 10f
        val heightBCm = 15f

        // Convert dimensions from cm to pixels
        val widthAPx = cmToPixels(widthACm, dpi)
        val heightAPx = cmToPixels(heightACm, dpi)
        val widthBPx = cmToPixels(widthBCm, dpi)
        val heightBPx = cmToPixels(heightBCm, dpi)

        // Create a blank white bitmap for parent B (10 cm x 15 cm)
        val bitmapB = Bitmap.createBitmap(widthBPx, heightBPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapB)
        val paint = Paint()

        // Fill the canvas with a white background
        canvas.drawColor(Color.WHITE)

        // Crop or resize A if needed to match the 7.5 cm x 15 cm dimensions
        val resizedA1 = Bitmap.createScaledBitmap(bitmapA, widthAPx, heightAPx, true)
        val resizedA2 = Bitmap.createScaledBitmap(bitmapA, widthAPx, heightAPx, true)

        // Calculate the center of B
        val centerOfBPx = widthBPx / 2

        // Draw Copy 1 of A with its end aligned with the center of B
        // Start X position for Copy 1 of A: Center of B minus width of A
        val startX1 = centerOfBPx - widthAPx
        canvas.drawBitmap(
            resizedA1,
            null,
            Rect(
                startX1, 0, // Top-left corner of A1
                centerOfBPx, heightAPx // Bottom-right corner of A1 aligned with center of B
            ),
            paint
        )

        // Draw Copy 2 of A with its start aligned with the center of B
        // Start X position for Copy 2 of A: Center of B
        val startX2 = centerOfBPx
        canvas.drawBitmap(
            resizedA2,
            null,
            Rect(
                startX2, 0, // Top-left corner of A2 aligned with center of B
                startX2 + widthAPx, heightAPx // Bottom-right corner of A2
            ),
            paint
        )

        return bitmapB
    }

    private fun processPhoto(bitmap: Bitmap): Bitmap? {
        val baseBitmap = bitmap
        val keyName = if(sharedPreferences.getBoolean("settings:multiPhoto", false)) "selectMultiLayout" else "selectedLayout"
        val layoutName = sharedPreferences.getString(keyName, "")
        val layoutPath = "${requireContext().cacheDir}/layouts/${layoutName}"

        val overlayBitmap = when {
            layoutName.isNullOrEmpty() -> {
                if(isMultiPhoto()) {
                    drawableToBitmap(ContextCompat.getDrawable(requireContext(), R.drawable.blankmultiple))
                } else {
                    drawableToBitmap(ContextCompat.getDrawable(requireContext(), R.drawable.blank))
                }
            }
            else -> {
                BitmapFactory.decodeFile(layoutPath)
            }
        }

        println("hhh overlayBitmap = ${overlayBitmap?.height}:${overlayBitmap?.width}")
        if(overlayBitmap != null) {
            // Scale the overlay bitmap to fit the height of the base bitmap
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

    fun printAction(printEvent: String) {
        println("hhh ${isMultiPhoto()} && ${isMultiLayoutDone()}")
        if(isMultiPhoto() && !isMultiLayoutDone()) {
            findNavController().navigate(R.id.action_previewFragment_to_countdownFragment)
        } else {
            CountdownFragment.setCapturedPhoto(requireContext(), null)
            CountdownFragment.setCapturedPhoto2(requireContext(), null)
            CountdownFragment.setCapturedPhoto3(requireContext(), null)

            if(sharedPreferences.getBoolean("settings:showQR", false)) {
                processPrintingAction("${printEvent}")
            } else {
                processPrintingAction("print")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when {
            event?.text == "cancel" -> binding.btnCancel.performClick()
            event?.text?.startsWith("print") == true -> {
                printAction(event.text)
            }
            event?.text == "save" -> binding.btnSave?.performClick()
            event?.text == "retake" -> binding.btnRetake.performClick()
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}