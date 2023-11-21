package com.puzzlebooth.server.layout

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.utils.RotateTransformation
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentLayoutBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class LayoutFragment : BaseFragment<FragmentLayoutBinding>(R.layout.fragment_layout) {

    private val remoteFiles = mutableListOf<FileServer>()
    private val REMOTE_LAYOUTS = "https://puzzleslb.com/puzzlebooth/uploads/mirror_booth_uploads/layouts1/"

    private fun getRemoteLayouts() {
        "${REMOTE_LAYOUTS}/list.php".httpGet()
            .responseObject(FilesServer.Deserializer()) { request, response, result ->
                val (people, err) = result
                println("hhh result ${result.component1()?.files}")
                remoteFiles.clear()
                people?.files?.filter { it.file.endsWith(".png") }
                    ?.let { remoteFiles.addAll(it) }
                remoteFiles.addAll(getLocalLayouts())

                println("hhh remotefiles size" + remoteFiles.size)
                requireActivity().runOnUiThread {
                    binding.listView.apply {
                        this.adapter = ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            remoteFiles.map { it.file })

                        //this.setSelection(4)
                        setOnItemClickListener { adapterView, view, i, l ->
                            val fileName = adapterView.getItemAtPosition(i).toString()
                            downloadLayout(fileName)
                            println("hhh clicked on ${adapterView.getItemAtPosition(i)}")
                        }
                    }
                }
            }
    }

    fun getLocalLayouts(): List<FileServer> {
        val locallayouts =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/"
        return File(locallayouts).listFiles()?.filter { it.name.endsWith(".png") }?.map {
            println("hhh localfile ${it.name}")
            FileServer("[LOCAL]" + it.name, it.length())
        } ?: listOf()
    }

    private fun showLayout() {
        val layoutName = sharedPreferences.getString("selectedLayout", "")
        if (layoutName?.isNotEmpty() == true) {
            val layoutFile = File("${requireContext().cacheDir}/layouts/${layoutName}")
            if (layoutFile.exists()) {
                Glide.with(this)
                    .load(layoutFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(
                        RotateTransformation(
                            requireContext(),
                            270f
                        )
                    )
                    .into(binding.layoutIv)
            }
        }
    }

    override fun initViewBinding(view: View): FragmentLayoutBinding {
        return FragmentLayoutBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLayout()
        binding.btnUpdateLayout.setOnClickListener {
            findNavController().popBackStack()
        }

        getRemoteLayouts()
    }

    private fun downloadLayout(fileName: String) {
        val locallayouts =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/"

        if (!File("${requireContext().cacheDir}/layouts/").exists()) {
            File("${requireContext().cacheDir}/layouts/").mkdirs()
        }

        val file = File("${requireContext().cacheDir}/layouts/${fileName}")
        if (file.exists()) {
            println("hhh ${fileName} exists")
            file.delete()
        }


        val outputStream = FileOutputStream(file)

        if (fileName.startsWith("[LOCAL]")) {
            val fileWithoutPrefix = fileName.removePrefix("[LOCAL]")
            val fileLocal = File(locallayouts + fileWithoutPrefix)
            Files.copy(fileLocal.toPath(), outputStream)

            val edit = sharedPreferences.edit()
            edit.putString("selectedLayout", fileName)
            edit.apply()

            requireActivity().runOnUiThread {
                showLayout()
            }
        } else {

            Fuel.download("${REMOTE_LAYOUTS}${fileName}")
                .streamDestination { response, _ ->
                    Pair(
                        outputStream,
                        { response.body().toStream() })
                }
                .fileDestination { response, request ->
                    file
                }
                .progress { readBytes, totalBytes ->
                    val progress = readBytes.toFloat() / totalBytes.toFloat() * 100
                    println("hhh progress ${progress}")
                }
                .response { result ->
                    result.fold(
                        success = {

                            val edit = sharedPreferences.edit()
                            edit.putString("selectedLayout", fileName)
                            edit.apply()

                            requireActivity().runOnUiThread {
                                showLayout()
                            }
                        },
                        failure = {
                            it.printStackTrace()
                            Toast.makeText(requireContext(), "Error downloading file!", Toast.LENGTH_SHORT)
                                .show()
                            //_downloadStatus.postValue(it.message?.let { it1 -> FileDownloadStatus.Failed(it1) })
                        }
                    )
                }
        }
    }
}

data class FilesServer(
    val files: List<FileServer>
){
    class Deserializer: ResponseDeserializable<FilesServer> {
        override fun deserialize(content: String): FilesServer = Gson().fromJson(content, FilesServer::class.java)
    }
}

data class FileServer(
    val file: String,
    val size: Long
){
    class Deserializer: ResponseDeserializable<FileServer> {
        override fun deserialize(content: String): FileServer = Gson().fromJson(content, FileServer::class.java)
    }
}