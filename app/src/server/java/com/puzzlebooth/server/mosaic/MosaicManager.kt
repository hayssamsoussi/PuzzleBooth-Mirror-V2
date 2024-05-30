package com.puzzlebooth.server.mosaic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import androidx.core.graphics.scale
import androidx.navigation.fragment.findNavController
import com.puzzlebooth.main.MosaicItem
import com.puzzlebooth.main.models.MosaicBox
import com.puzzlebooth.main.models.MosaicInfo
import com.puzzlebooth.main.utils.draftPath
import com.puzzlebooth.main.utils.draftPathCutIn2
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask

//https://www.imgonline.com.ua/eng/cut-photo-into-pieces.php
// TODO album scroll when navigating
// TODO Fix status from phone
// TODO start mosaic on download
object MosaicManager {
    private var mosaic = true
    var original = true
    private var mosaicFirst = true
    var countMosaic = 6
    var timer1: Timer? = null
    var timer2: Timer? = null

    lateinit var mosaic_originals: File
    lateinit var mosaic_images: File
    lateinit var mosaic_working: File
    lateinit var mosaic_draft: File
    lateinit var mosaic_toPrint: File
    lateinit var mosaic_merge: File
    lateinit var mosaic_print: File
    lateinit var mosaic_done: File

    fun isRunning(): Boolean {
        val originalsExist = mosaic_originals.list()?.isNotEmpty() == true
        return (originalsExist)
    }

    fun getMosaicFileAt(position: Int): MosaicItem {
        val mosaicViews = generateMosaicViews()
        return mosaicViews[position - 1]
    }

    fun processMosaicEvent(context: Context, event: String?) {
        if(event == null) return

        val mosaicPosition = event.substringAfter(":")
        val position = mosaicPosition.toIntOrNull() ?: return

        when {
            event.startsWith("deleteMosaic") ->  deleteImageAtIndex(position)
            event.startsWith("printMosaic") ->  MosaicManager.copyImagesToPrintableMosaic(context, position)
        }
    }

    fun getMosaicInfo(): MosaicInfo? {
        if(!isRunning()) {
            return null
        }

        val map = mutableListOf<MosaicBox>()
        if(mosaic_originals.exists()) {
            val size = mosaic_originals.listFiles().size
            if(size > 0) {
                (1..size).forEach {
                    val image = "$it".padStart(3, '0') + ".jpg"
                    val doesImageExist = mosaic_images.list { dir, name -> name == image }?.isNotEmpty() == true
                    map.add(MosaicBox(it, doesImageExist))
                }
            }
        }

        return MosaicInfo(
            originals = mosaic_originals.listFiles()?.size ?: 0,
            images = mosaic_images.listFiles()?.size ?: 0,
            drafts = mosaic_draft.listFiles()?.size ?: 0,
            toPrint = mosaic_toPrint.listFiles()?.size ?: 0,
            merge = mosaic_merge.listFiles()?.size ?: 0,
            mosaicPrint = mosaic_print.listFiles()?.size ?: 0,
            done = mosaic_done.listFiles()?.size ?: 0,
            boxes = map
        )
    }

    fun startMosaic(context: Context, timerTik: () -> Unit) {
        println("hhh context.getCurrentEventPhotosPath(): ${context.getCurrentEventPhotosPath()}")
        mosaic_originals = File(context.getCurrentEventPhotosPath() + "mosaic/originals")
        mosaic_images = File(context.getCurrentEventPhotosPath() + "mosaic/images")
        mosaic_working = File(context.getCurrentEventPhotosPath() + "mosaic/working")
        mosaic_draft = File(context.getCurrentEventPhotosPath() + "mosaic/draft")
        mosaic_toPrint = File(context.getCurrentEventPhotosPath() + "mosaic/toPrint")
        mosaic_merge = File(context.getCurrentEventPhotosPath() + "mosaic/merge")
        mosaic_print = File(context.getCurrentEventPhotosPath() + "mosaicPrint")
        mosaic_done = File(context.getCurrentEventPhotosPath() + "mosaic/done")

        if(!mosaic_originals.exists()) { mosaic_originals.mkdirs() }
        if(!mosaic_images.exists()) { mosaic_images.mkdirs() }
        if(!mosaic_working.exists()) { mosaic_working.mkdirs() }
        if(!mosaic_draft.exists()) { mosaic_draft.mkdirs() }
        if(!mosaic_done.exists()) { mosaic_done.mkdirs() }
        if(!mosaic_toPrint.exists()) { mosaic_toPrint.mkdirs() }
        if(!mosaic_merge.exists()) { mosaic_merge.mkdirs() }
        if(!mosaic_print.exists()) { mosaic_print.mkdirs() }

        if(mosaic_originals.listFiles()?.isEmpty() == true) { println("error the originals are wrong!")
            return
        }

        if(timer1 != null && timer2 != null) {
            return
        }

        if(!File("${context.getCurrentEventPhotosPath()}mosaic/settings").exists()) {
            File("${context.getCurrentEventPhotosPath()}mosaic/settings").writeText("mosaic=off\noriginal=off\nisMosaicFirst=off\ncountMosaic=6")
        }

        timer1 = Timer()
        timer1?.schedule(timerTask {
            val mergeDir = File("${context.getCurrentEventPhotosPath()}mosaic/merge")
            println("hhh timer1 mergeDir:${mergeDir.listFiles()?.size}")
            mergeDir.listFiles()?.takeLast(countMosaic).let {
                println("hhh it?.size: ${it?.size}")
                it?.size?.let { size ->
                    if(size != 0) {
                        println("hhh timer1 *** got 6 and now generating printable mosaic of these 6")
                        generatePrintableMosaic(context, it)
                    }
                }
            }

            val toPrintDir = File("${context.getCurrentEventPhotosPath()}mosaic/toPrint")
            if((toPrintDir.listFiles()?.size ?: 0 ) >= countMosaic) {
                moveToPrintsToMerge(context)
            }

        }, 0, 4000)

        timer2 = Timer()
        timer2?.schedule(timerTask {
            timerTik.invoke()
            if(mosaic_draft.listFiles()?.isNotEmpty() == true) {
                println("hhh timer2 mosaic_draft is not empty")

                val workingSize = (mosaic_working.listFiles()?.filter { it.extension == "jpeg" }?.size)
                println("hhh timer2 *** mosaic_draft: workingSize: $workingSize")
                if((workingSize ?: 0) > 0) {
                    println("hhh timer2 *** there's $workingSize files in the working file so waiting..")
                } else {
                    val firstDraft = mosaic_draft.listFiles()?.firstOrNull { it.extension == "jpeg" }

                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            if(firstDraft != null) {
                                println("hhh timer2 ** will work on ${firstDraft.name}")
                                val random = getRandomImageIndex()
                                if (random != null) {
                                    transformDraftToMosaic(context, firstDraft, random)
                                }
                            }
                        }
                    }, 2000)
                }
            }
        }, 0, 3000)
    }

    fun forcePrintImage() {

    }

    fun deleteAll() {
        mosaic_originals.deleteRecursively()
        mosaic_images.deleteRecursively()
        mosaic_draft.deleteRecursively()
        mosaic_done.deleteRecursively()
        mosaic_toPrint.deleteRecursively()
        mosaic_merge.deleteRecursively()
        mosaic_print.deleteRecursively()
    }

    fun deleteImageAtIndex(index: Int) {
        val imageName = "${index}".padStart(3, '0') + ".jpg"
        val mosaicImages = mosaic_images
        val mosaicToPrint = mosaic_toPrint
        val file = File("${mosaicImages.path}/$imageName")
        val fileToPrint = File("${mosaicToPrint.path}/$imageName")
        file.delete()
        fileToPrint.delete()
    }

    fun getRandomImageIndex(): Int? {
        val originals = mosaic_originals.list()?.mapNotNull { it.toString().removeSuffix(".jpg").toIntOrNull() }
        val images = mosaic_images.list()?.mapNotNull { it.toString().removeSuffix(".jpg").toIntOrNull() }
        println("hhh originals: ${originals?.size}")
        println("hhh images: ${images?.size}")
        val filtered = if(images != null) originals?.subtract(images) else listOf<Int>()
        println("hhh filtered: ${filtered?.size}")

        return if(filtered.isNullOrEmpty()) null else filtered.random()
    }

    fun resizeAndZoomFirstBitmapToFitSecondWidth(bitmap1: Bitmap, bitmap2: Bitmap, zoomFactor: Float): Bitmap {
        val originalWidth1 = bitmap1.width
        val originalWidth2 = bitmap2.width

        val aspectRatio1 = bitmap1.height.toFloat() / originalWidth1.toFloat()

        val newWidth1 = originalWidth2
        val newHeight1 = (newWidth1 * aspectRatio1).toInt()

        // Resize the first bitmap to fit the width of the second bitmap
        val resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, newWidth1, newHeight1, true)

        // Apply zoom to the resized bitmap
        val matrix = Matrix()
        matrix.postScale(zoomFactor, zoomFactor)

        return Bitmap.createBitmap(resizedBitmap1, 0, 0, resizedBitmap1.width, resizedBitmap1.height, matrix, true)
    }

    fun resizeFirstBitmap(bitmap1: Bitmap, newWidth: Int): Bitmap {
        val originalWidth1 = bitmap1.width

        val aspectRatio1 = bitmap1.height.toFloat() / originalWidth1.toFloat()

        val newWidth1 = newWidth
        val newHeight1 = (newWidth1 * aspectRatio1).toInt()

        // Resize the first bitmap to fit the width of the second bitmap
        val resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, newWidth1, newHeight1, true)

        return Bitmap.createScaledBitmap(resizedBitmap1, resizedBitmap1.width, resizedBitmap1.height, true)
    }

    fun mergeBitmapsWithTextV2(
        bottomImagePath: String,
        topImagePath: String,
        text: String,
        outputImagePath: String
    ): File? {
        try {
            // Load the bottom and top images
            val bottomImage = BitmapFactory.decodeFile(bottomImagePath) //asliye
            val topImageBefore = BitmapFactory.decodeFile(topImagePath) //tsawaret
            val topImage = resizeAndZoomFirstBitmapToFitSecondWidth(topImageBefore, bottomImage, 1.3f)

            println("hhhh bottom image width:${bottomImage.width}/height:${bottomImage.height}")
            println("hhhh top image width:${topImage.width}/height:${topImage.height}")

            // Calculate the scaled dimensions of the top image
            val scaledWidth = (topImage.width * 1).toInt()
            val scaledHeight = (topImage.height * 1).toInt()

            println("hhhh scaledWidth:${scaledWidth}")
            println("hhhh scaledHeight:${scaledHeight}")

            // Create a new mutable bitmap with the same dimensions as the bottom image
            val mergedBitmap = Bitmap.createBitmap(bottomImage.width, bottomImage.height, Bitmap.Config.ARGB_8888)

            // Create a canvas for drawing on the merged bitmap
            val canvas = Canvas(mergedBitmap)

            // Draw the bottom image onto the merged bitmap
            canvas.drawBitmap(bottomImage, 0f, 0f, null)

            // Set the transparency of the top image
            //val alpha = 0.25f
            val alpha = 0.25f
            val paint = Paint()
            paint.alpha = (alpha * 255).toInt()

            // Calculate the position to center the top image vertically and horizontally
            val topImageX = (bottomImage.width - scaledWidth) / 2f
            val topImageY = (bottomImage.height - scaledHeight) / 7f

            println("hhhh topImageX:(${topImage.width}-${scaledWidth}) / 2f = ${topImageX}")
            println("hhhh topImageX:(${topImage.height}-${scaledHeight}) / 7f = ${topImageY}")

            // Draw the scaled top image onto the merged bitmap
            canvas.drawBitmap(topImage, topImageX, topImageY, paint)

            // Set the transparency of the text
            val textAlpha = 0.3f

            // Set the paint for the text
            paint.alpha = (textAlpha * 255).toInt()
            paint.color = Color.WHITE
            paint.textSize = 28f

            // Calculate the position for the text in the bottom right corner
            val textX = bottomImage.width - paint.measureText(text) - 20
            val textY = (bottomImage.height - 20).toFloat()

            // Draw the text onto the merged bitmap
            canvas.drawText(text, textX, textY, paint)

            // Save the merged bitmap to the output file
            val outputFile = File(outputImagePath)
            mergedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputFile.outputStream())

            println("Merged image saved to $outputImagePath")
            return outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun moveDraftToWorking(context: Context, file: File): File? {
        val draft = File("${context.getCurrentEventPhotosPath()}mosaic/draft/${file.name}")
        val destFile = File("${context.getCurrentEventPhotosPath()}mosaic/working/${file.name}")

        try {
            Files.move(draft.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if(destFile.isFile) return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            println("** cannot move now used by another process")
        }

        return null
    }

    fun transformDraftToMosaic(context: Context, draft: File, randomNumber: Int) {
        println(" *** creating a mosaic pic on number $randomNumber")
        val topImageFile = moveDraftToWorking(context, draft)

        if(topImageFile != null) {
            // generate a random mosaic number
            val bottomImagePath = File("${context.getCurrentEventPhotosPath()}mosaic/originals/${randomNumber.toString().padStart(3, '0')}.jpg")

            val outputImageFile = mergeBitmapsWithTextV2(
                bottomImagePath.path,
                topImageFile.path ?: "",
                randomNumber.toString(),
                "${context.getCurrentEventPhotosPath()}mosaic/images/${randomNumber.toString().padStart(3, '0')}.jpg"
            )

            if(outputImageFile != null) {
                moveWorkingToDoneV2(context, draft)

                if(mosaicFirst) {
                    if(mosaic) copyImagesToPrintableMosaic(context, outputImageFile)

//                    Timer().schedule(object : TimerTask() {
//                        override fun run() {
//                            if(original) copyDoneToRoot(context, draft)
//                        }
//                    }, 1000)
                } else {
                    //if(original) copyDoneToRoot(context, draft)

                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            if(mosaic) copyImagesToPrintableMosaic(context, outputImageFile)
                        }
                    }, 2000)
                }
            }
        }
    }

    fun moveToPrintsToMerge(context: Context) {
        val toPrintMosaicsDir = File("${context.getCurrentEventPhotosPath()}mosaic/toPrint/")

        toPrintMosaicsDir.listFiles().take(6).forEach {
            Files.move(it.toPath(), File("${context.getCurrentEventPhotosPath()}mosaic/merge/${it.name}").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun generatePrintableMosaic(context: Context, list: List<File>?) {
        try {
            val randName = list?.firstOrNull()?.name
            val outputImagePath = File("${context.draftPathCutIn2()}$randName.jpg").path
            val backgroundWidth = 1000  // 10 cm = 1000 px (assuming 1 cm = 100 px)
            val backgroundHeight = 1500 // 15 cm = 1500 px (assuming 1 cm = 100 px)
            val squareSize = 490        // 5 cm = 500 px (assuming 1 cm = 100 px)

            // Create a blank white background bitmap
            val background = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(background)
            canvas.drawColor(Color.DKGRAY)

            val randomText = StringBuilder()

            list?.forEachIndexed { index, file ->
                val squareImagePath = file.path

                // Load the square image
                val square = BitmapFactory.decodeFile(squareImagePath)
                println("hhhhhh square width:${square.width}height:${square.height}")
                println("hhhhhh background width:${background.width}height:${background.height}")

                val xandy = when (index) {
                    0 -> Pair((backgroundWidth - squareSize) - 17, (backgroundHeight - squareSize) / 2f)
                    1 -> Pair((backgroundWidth - squareSize) - 17, ((backgroundHeight - squareSize) / 2f) - squareSize)
                    2 -> Pair((backgroundWidth - squareSize) - 17, ((backgroundHeight - squareSize) / 2f) + squareSize)
                    3 -> Pair(8f, ((backgroundHeight - squareSize) / 2f))
                    4 -> Pair(8f, ((backgroundHeight - squareSize) / 2f) - squareSize)
                    5 -> Pair(8f, ((backgroundHeight - squareSize) / 2f) + squareSize)
                    else -> Pair(squareSize.toFloat(), squareSize.toFloat())
                }

                val squareX = xandy.first.toFloat()
                val squareY = xandy.second.toFloat()

                // Draw the square image onto the background bitmap
                val newSquare = square.scale(squareSize, squareSize)
                canvas.drawBitmap(newSquare, squareX, squareY, null)

                file.delete()

                randomText.append("/${file.name}")
            }

            val paint = Paint()
            paint.textSize = 120f
            paint.typeface = Typeface.create("Arial", Typeface.BOLD)
            paint.color = Color.WHITE

            // Enable anti-aliasing for smoother text rendering
            paint.isAntiAlias = true

            // Calculate the position for the text in the bottom right corner
            val textX = (background.width * 0.9 + paint.measureText(randomText.toString())).toInt()
            val textY = (background.height * 0.9 - 20).toInt()

            // Draw the text onto the background bitmap
            canvas.drawText(randomText.toString(), textX.toFloat(), textY.toFloat(), paint)

            // Save the resulting image to the output file as JPEG
            val outputImage = File(outputImagePath)
            background.compress(Bitmap.CompressFormat.JPEG, 100, outputImage.outputStream())

            println("Image processing complete.")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copyImagesToPrintableMosaic(context: Context, position: Int): File? {
        val mosaicItem = MosaicManager.getMosaicFileAt(position)
        val file = mosaicItem.file
        return copyImagesToPrintableMosaic(context, file)
    }

    fun copyImagesToPrintableMosaic(context: Context, file: File): File? {
        val draft = File("${context.getCurrentEventPhotosPath()}mosaic/images/${file.name}")
        val destFile = File("${context.getCurrentEventPhotosPath()}mosaic/toPrint/${file.name}")

        Files.copy(draft.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        if(destFile.isFile) return destFile
        return null
    }

    fun copyDoneToRoot(context: Context, file: File): File? {
        val draft = File("${context.getCurrentEventPhotosPath()}mosaic/done/${file.name}")
        val destFile = File("./${file.name}")

        Files.copy(draft.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        if(destFile.isFile) return destFile
        return null
    }

    fun moveWorkingToDoneV2(context: Context, file: File): File? {
        val draft = File("${context.getCurrentEventPhotosPath()}mosaic/working/${file.name}")
        val destFile = File("${context.getCurrentEventPhotosPath()}mosaic/done/${file.name}")

        Files.move(draft.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        if(destFile.isFile) return destFile
        return null
    }

    fun generateMosaicViews(): List<MosaicItem> {
        val map = mutableListOf<MosaicItem>()
        if(mosaic_originals.exists()) {
            val size = mosaic_originals.listFiles().size
            if(size > 0) {
                (1..size).forEach {
                    val image = "$it".padStart(3, '0') + ".jpg"
                    val doesImageExist = mosaic_images.list { dir, name -> name == image }?.isNotEmpty() == true

                    if(doesImageExist) {
                        map.add(MosaicItem(it, File(mosaic_images.path + "/" + image), false))
                    } else {
                        map.add(MosaicItem(it, File(mosaic_originals.path + "/" + image), true))
                    }
                }
            }
        }

        return map
    }

    fun splitBitmap(inputImagePath: String, columns: Int, rows: Int): List<Bitmap> {
        println("hhh downloaded the image and path is ${inputImagePath}")
        val bitmap = BitmapFactory.decodeFile(inputImagePath)

        println("hhh we need to generate 8 x 11 so each box will be of width ${bitmap.width/8} and height ${bitmap.height/11}")

        val outputFolder = MosaicManager.mosaic_originals
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        val boxWidth = bitmap.width / columns
        val boxHeight = bitmap.height / rows
        val bitmaps = mutableListOf<Bitmap>()

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val x = col * boxWidth
                val y = row * boxHeight

                val boxBitmap = Bitmap.createBitmap(bitmap, x, y, boxWidth, boxHeight)
                bitmaps.add(boxBitmap)
            }
        }

        bitmaps.forEachIndexed { index, item ->
            val outputFilePath = File(outputFolder, "${index+1}".padStart(3, '0') + ".jpg")
            try {
                val outputStream = FileOutputStream(outputFilePath)
                item.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return bitmaps
    }
}