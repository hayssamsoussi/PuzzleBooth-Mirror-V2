package com.puzzlebooth.main.utils

import android.content.Context
import android.os.Environment
import android.os.FileUtils
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Constants {
    var printerOne = false
}

fun Context.getCurrentEventPhotosPath(): String {
    val timeFormatDate = SimpleDateFormat("yyyyMMdd", Locale.US)
    val timeStampDate: String = timeFormatDate.format(Date())
    val sharedPreferences = this.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
    val selectedLayout = sharedPreferences.getString("selectedLayout", "")
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${timeStampDate}-${selectedLayout}/"
}

fun Context.getCurrentEventName(): String {
    val timeFormatDate = SimpleDateFormat("yyyyMMdd", Locale.US)
    val timeStampDate: String = timeFormatDate.format(Date())
    val sharedPreferences = this.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
    val selectedLayout = sharedPreferences.getString("selectedLayout", "")
    return "${timeStampDate}-${selectedLayout}"
}

fun Context.draftPath(): String {
    val sharedPreferences = this.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
    val currentCanonPrinting = sharedPreferences.getBoolean("settings:canonPrinting", false)
    return if(currentCanonPrinting) {
        println("hhh currentCanonPrinting: ${currentCanonPrinting}")
        val currentCanonPrintingTwoPrinters = sharedPreferences.getBoolean("settings:canonPrintingTwoPrinters", false)
        println("hhh currentCanonPrintingTwoPrinters: ${currentCanonPrintingTwoPrinters}")
        if(currentCanonPrintingTwoPrinters) {
            if(Constants.printerOne) {
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts_canon_1/"
            } else {
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts_canon_2/"
            }
        } else {
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts_canon_1/"
        }
    } else {
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts/"
    }
}

fun Context.draftPathCutIn2(): String {
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts_cut_in_2/"
}

fun Context.mosaicDraftPath(): String {
    val current = this.getCurrentEventPhotosPath()
    return "${current}mosaic/draft"
}

fun Context.showInputDialog(
    title: String,
    hint: String = "",
    callback: (String) -> Unit
) {
    val input = EditText(this).apply {
        this.hint = hint
    }

    AlertDialog.Builder(this).apply {
        setTitle(title)
        setView(input)
        setPositiveButton("OK") { dialog, _ ->
            val inputText = input.text.toString()
            callback(inputText)
            dialog.dismiss()
        }
        setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        create()
        show()
    }
}