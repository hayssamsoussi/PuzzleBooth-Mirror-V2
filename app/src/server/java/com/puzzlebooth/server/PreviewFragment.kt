package com.puzzlebooth.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.FileClientLegacy
import com.puzzlebooth.server.databinding.FragmentPreviewBinding
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
        when(event?.text) {
            "cancel" -> binding.btnCancel.performClick()
            "print" -> binding.btnPrint.performClick()
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
        val baseBitmap = com.puzzlebooth.server.CountdownFragment.capturedPhoto ?: return null
        val layoutName = sharedPreferences.getString("selectedLayout", "")
        val layoutPath = "${requireContext().cacheDir}/layouts/${layoutName}"
        val overlayBitmap = if(layoutName.isNullOrEmpty()) drawableToBitmap(ContextCompat.getDrawable(requireContext(), R.drawable.blank)) else BitmapFactory.decodeFile(layoutPath)

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

    private fun initViews() {
        binding.buttonsContainer.visibility = if(sharedPreferences.getBoolean("settings:touchMode", false)) View.VISIBLE else View.GONE
        binding.btnPrint.setOnClickListener {
            val selectedLayout = sharedPreferences.getString("selectedLayout", "")

            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timeFormatDate = SimpleDateFormat("yyyyMMdd", Locale.US)
            val timeStampDate: String = timeFormatDate.format(Date())
            val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${timeStampDate}-${selectedLayout}/"
            File(path).mkdirs()
            val timeStamp: String = timeFormat.format(Date())
            val fileName = "${timeStamp}_$selectedLayout.jpeg"
            var file = FileOutputStream("$path$fileName")

            var quality = if(sharedPreferences.getBoolean("settings:printingQuality", false) ) 100 else 70
            resultBitmap?.rotate(90F)?.compress(Bitmap.CompressFormat.JPEG, quality, file)

            val pathFile = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)
            val filesList = pathFile.list()
            val todaysFiles = filesList.filter { it.startsWith(timeStampDate) }

            val thread = Thread {
                try {
                    val ip = sharedPreferences.getString("ip", "") ?: return@Thread
                    val port = sharedPreferences.getString("port", "") ?: return@Thread
                    val portNumber = port?.toIntOrNull() ?: return@Thread
                    FileClientLegacy(
                        ip,
                        13456,
                        "$path$fileName"
                    )
                //FileClient(ip, 13456).execute(pathFile)
                    //FileClient(sharedPreferences.getString("ip", "192.168.43.1"), sharedPreferences.getString("port", "13456")?.toIntOrNull() ?: 0, pathFile.path + "/" + todaysFiles.last())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            thread.start()

            //sendToTarget("printCount:${todaysFiles.size}", ServerService.lastReceiverAddress)

            findNavController().navigate(R.id.action_previewFragment_to_printFragment)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().setGraph(R.navigation.nav_graph)
        }

        binding.btnRetake.setOnClickListener {
            findNavController().navigate(R.id.action_previewFragment_to_countdownFragment)
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}