package com.puzzlebooth.main.utils

import android.content.Context
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.getCurrentEventPhotosPath(): String {
    val timeFormatDate = SimpleDateFormat("yyyyMMdd", Locale.US)
    val timeStampDate: String = timeFormatDate.format(Date())
    val sharedPreferences = this.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
    val selectedLayout = sharedPreferences.getString("selectedLayout", "")
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${timeStampDate}-${selectedLayout}/"
}

fun Context.draftPath(): String {
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/mirror_drafts/"
}

class FileUtils {
}