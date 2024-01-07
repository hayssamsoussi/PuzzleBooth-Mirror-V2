package com.puzzlebooth.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.FileClientLegacy
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.main.utils.mosaicDraftPath
import com.puzzlebooth.server.databinding.FragmentPreviewBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import okio.Path.Companion.toPath
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviewFragment : BaseFragment<FragmentPreviewBinding>(R.layout.fragment_preview) {

    private var resultBitmap: Bitmap? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "cancel" -> binding.btnCancel.performClick()
            "printWithMosaic" -> binding.btnPrintWithMosaic.performClick()
            "print" -> binding.btnPrint.performClick()
            "save" -> binding.btnSave.performClick()
            "retake" -> binding.btnRetake.performClick()
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

    private fun initViews() {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)

        binding.btnPrintWithMosaic.visibility = if(MosaicManager.isRunning()) View.VISIBLE else View.GONE
        binding.buttonsContainer.visibility = if(sharedPreferences.getBoolean("settings:touchMode", false)) View.VISIBLE else View.GONE
        binding.btnPrint.setOnClickListener {

            val pair = saveFileToDrafts()
            val fileName = pair.first
            val normalPath = pair.second

            if(landscape) {
                val thread = Thread {
                    try {
                        val ip = sharedPreferences.getString("ip", "") ?: return@Thread
                        val port = sharedPreferences.getString("port", "") ?: return@Thread
                        port.toIntOrNull() ?: return@Thread

                        FileClientLegacy(
                            ip,
                            13456,
                            "$normalPath$fileName"
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                thread.start()
            } else {
                File("$normalPath$fileName").copyTo(File("${requireContext().draftPath()}$fileName"), true)
            }

            findNavController().navigate(R.id.action_previewFragment_to_printFragment)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_startFragment)
        }

        binding.btnRetake.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_countdownFragment)
        }

        binding.btnSave.setOnClickListener {
            saveFileToDrafts()
            findNavController().navigate(R.id.action_previewFragment_to_printFragment)
        }

        binding.btnPrintWithMosaic.setOnClickListener {
            val mosaicPath = requireContext().mosaicDraftPath()
            File(mosaicPath).mkdirs()

            val pair = saveFileToDrafts()
            val fileName = pair.first
            val normalPath = pair.second

            if(MosaicManager.original) {
                File("$normalPath$fileName").copyTo(File("${requireContext().draftPath()}/$fileName"), true)
            }

            File("$normalPath$fileName").copyTo(File("${requireContext().mosaicDraftPath()}/$fileName"), true)

            findNavController().navigate(R.id.action_previewFragment_to_printFragment)
        }
    }

    fun saveFileToDrafts(): Pair<String, String> {
        val landscape = sharedPreferences.getBoolean("settings:landscape", false)
        val selectedLayout = sharedPreferences.getString("selectedLayout", "")
        val quality = if(sharedPreferences.getBoolean("settings:printingQuality", false) ) 100 else 70

        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timeStamp: String = timeFormat.format(Date())
        val fileName = "${timeStamp}_$selectedLayout.jpeg"

        val normalPath = requireContext().getCurrentEventPhotosPath()
        val draftPath = requireContext().draftPath()


        File(normalPath).mkdirs()
        File(draftPath).mkdirs()

        val file = FileOutputStream("$normalPath$fileName")
        resultBitmap?.rotate(if(landscape) 90F else 0F)?.compress(Bitmap.CompressFormat.JPEG, quality, file)

        return Pair(fileName, normalPath)
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}