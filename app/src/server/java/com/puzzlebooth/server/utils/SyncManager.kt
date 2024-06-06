package com.puzzlebooth.server.utils

import android.content.Context
import com.puzzlebooth.main.models.RemotePhoto
import com.puzzlebooth.main.models.RemotePhotoRequest
import com.puzzlebooth.main.utils.RemoteFilesResponse
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.network.APIService
import com.puzzlebooth.server.network.RetrofitInstance
import io.paperdb.Paper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.io.File

enum class Status {
    SYNCING, DONE, ERROR;
}

class SyncManager(val context: Context, callback: (Pair<Status, String>) -> Unit) {

    private val uploadedFilesMap = mutableMapOf<String, Boolean>()
    private val service = RetrofitInstance.getRetrofitInstance().create(APIService::class.java)

    init {
        println("*** syncmanager: sync manager initted")
        val requestsToUpload = mutableListOf<Observable<ResponseBody>>()
        fetchRemoteFiles()
            .doOnSubscribe {
                callback.invoke(Pair(Status.SYNCING, ""))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                val localFiles = fetchLocalFiles()
                println("*** syncmanager: local files ${localFiles.size}")
                localFiles.forEach {
                    uploadedFilesMap[it.name] = false
                }

                it.files.forEach {
                    if(uploadedFilesMap.contains(it.file)) {
                        uploadedFilesMap[it.file] = true
                        println("*** syncmanager: setting ${it.file} to true")
                    }
                }

                val total = uploadedFilesMap.size
                val uploading = uploadedFilesMap.filter { !it.value }.size
                val uploaded = uploadedFilesMap.filter { it.value }.size

                callback.invoke(Pair(Status.SYNCING, "Uploading: ${uploading} - Uploaded: ${uploaded} - Total: ${total}"))

                println("*** syncmanager: map: ${uploadedFilesMap.toString()}")
                uploadedFilesMap.forEach {
                    if(it.value == false) {
                        val requestBody: RequestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "fileToUpload",
                                it.key,
                                RequestBody.create("image/jpeg".toMediaTypeOrNull(), File("${context.getCurrentEventPhotosPath()}${it.key}"))
                            )
                            .build()

                        requestsToUpload.add(
                            service.uploadPhotoFile(requestBody)
                        )

//                        val remotePhotoRequestCached: RemotePhoto? = Paper.book().read<RemotePhoto>(it.key)
//                        requestsToUpload.add(
//                            service.uploadPhotoNumber(RemotePhotoRequest(listOf(RemotePhoto(name = it.key, sender = 0, personName = remotePhotoRequestCached?.personName ?: "", email = remotePhotoRequestCached?.email ?: "", phone = remotePhotoRequestCached?.phone ?: ""))))
//                        )

                        Observable.merge(requestsToUpload)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete {
                                callback.invoke(Pair(Status.DONE, ""))
                            }
                            .subscribe()
                    }
                }
            }
            .doOnComplete {
                callback.invoke(Pair(Status.DONE, ""))
            }
            .doOnError {
                it.printStackTrace()
                callback.invoke(Pair(Status.ERROR, ""))
            }.subscribe()
    }

    private fun fetchRemoteFiles(): Observable<RemoteFilesResponse> {
        return service.listPhotos()
    }

    private fun fetchLocalFiles(): List<File> {
        val file = File(context.getCurrentEventPhotosPath())
        println("*** syncmanager: getCurrentEventPhotosPath: ${context.getCurrentEventPhotosPath()} ")
        val files = file.listFiles()?.filter { it.isFile && it.extension.equals("jpeg", true) }
        println("*** syncmanager: getCurrentEventPhotosPath: files: ${files?.size} ")
        return files ?: listOf()
    }
}