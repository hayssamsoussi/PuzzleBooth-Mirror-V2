package com.puzzlebooth.server

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.PreviewFragment.Companion.isMultiPhoto
import com.puzzlebooth.server.settings.PhotoQuality
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PHOTO_MODE(val photos: Int) {
    SINGLE(1), MULTIPLE(3)
}

class SharedViewModel() : ViewModel() {
    val capturedPhotos = mutableListOf<File>()
    var currentCaptureMode: PHOTO_MODE = PHOTO_MODE.SINGLE

    fun saveBitmapToOriginals(context: Context, sharedPreferences: SharedPreferences, bitmap: Bitmap): Single<File> {
        return Single.fromCallable {
            val keyName = if(isMultiPhoto(sharedPreferences)) "selectMultiLayout" else "selectedLayout"
            val selectedLayout = sharedPreferences.getString(keyName, "")
            val quality = PhotoQuality.getCurrentQualityInt(context)

            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timeStamp: String = timeFormat.format(Date())
            val fileName = "${timeStamp}_$selectedLayout.jpeg"

            val originalsPath = context.getCurrentEventPhotosPath() + "originals/"
            val file = FileOutputStream("$originalsPath$fileName")
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, file)

            File("$originalsPath$fileName")
        }
    }
    fun createOriginalFile(context: Context, sharedPreferences: SharedPreferences): File {
        val keyName = if(isMultiPhoto(sharedPreferences)) "selectMultiLayout" else "selectedLayout"
        val selectedLayout = sharedPreferences.getString(keyName, "")
        val quality = PhotoQuality.getCurrentQualityInt(context)

        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timeStamp: String = timeFormat.format(Date())
        val fileName = "${timeStamp}_$selectedLayout.jpeg"


        val originalsPath = context.getCurrentEventPhotosPath() + "originals/"
        File(originalsPath).mkdirs()

        val file = File("$originalsPath$fileName")
        return file
    }

    // LiveData to hold some shared data
    private val _sharedData = MutableLiveData<String>()
    val sharedData: LiveData<String> get() = _sharedData

    // Function to update the data
    fun updateData(newData: String) {
        _sharedData.value = newData
    }
}