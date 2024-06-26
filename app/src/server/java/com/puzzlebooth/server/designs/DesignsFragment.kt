package com.puzzlebooth.server.designs

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.kittinunf.fuel.Fuel
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentDesignsBinding
import com.puzzlebooth.server.databinding.FragmentThemeBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.network.Design
import com.puzzlebooth.server.network.Event
import com.puzzlebooth.server.theme.listing.DesignsAdapter
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class DesignsFragment : BaseFragment<FragmentDesignsBinding>(R.layout.fragment_designs) {

    private var designs = mutableListOf<Design>()
    private lateinit var adapter: DesignsAdapter

    override fun onResume() {
        super.onResume()
        showLayout()
    }

    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        when(result) {
            is QRResult.QRSuccess -> {
                val rawValue = result.content.rawValue
                val jsonObject = JSONObject(rawValue)
                val id = jsonObject.optInt("id")

                fetchEventInfo(id).map {
                    val event = it ?: return@map
                    updateEvent(event)
                }.subscribe()
            }

            QRResult.QRUserCanceled -> "User canceled"
            QRResult.QRMissingPermission -> "Missing permission"
            is QRResult.QRError -> "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
        }
        result.let {  }
    }

    override fun initViewBinding(view: View): FragmentDesignsBinding {
        return FragmentDesignsBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initData()
    }

    private fun initData() {
        service
            .listDesigns()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(it.message)
                    .show()
            }
            .doOnNext {
                designs.clear()
                designs.addAll(getLocalLayouts())
                designs.addAll(it)
                adapter.notifyDataSetChanged()
            }.subscribe()
    }

    private fun getLocalLayouts(): List<Design> {
        val list = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/").listFiles()?.filter { it.name.endsWith(".png") }
            ?: return listOf()

        return list.map { file ->
            Design(
                creation_date = "",
                filename = file.name,
                url = file.path.toString(),
                isLocal = true
            )
        }
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
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivDesign)
            }
        }

        val animationName = sharedPreferences.getString("selectedAnimation", "")
        if (animationName?.isNotEmpty() == true) {
            val layoutFile = File("${requireContext().cacheDir}/animations/${animationName}")
            if (layoutFile.exists()) {
                Glide.with(this)
                    .load(layoutFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivAnimation)
            }
        }

        val mosaicName = sharedPreferences.getString("selectedMosaic", "")
        if (mosaicName?.isNotEmpty() == true) {
            val layoutFile = File("${requireContext().cacheDir}/mosaics/${mosaicName}")
            if (layoutFile.exists()) {
                Glide.with(this)
                    .load(layoutFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivAnimation)
            }
        }
    }

    private  fun storeSelectedLayout(fileName: String) {
        val edit = sharedPreferences.edit()
        edit.putString("selectedLayout", fileName)
        edit.apply()
    }

    private fun downloadLayout(design: Design) {
        val locallayouts = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/"

        if (!File("${requireContext().cacheDir}/layouts/").exists()) {
            File("${requireContext().cacheDir}/layouts/").mkdirs()
        }

        val file = File("${requireContext().cacheDir}/layouts/${design.filename}")
        if (file.exists()) {
            file.delete()
        }

        val outputStream = FileOutputStream(file)

        if (design.isLocal) {
            val fileLocal = File(locallayouts + design.filename)
            Files.copy(fileLocal.toPath(), outputStream)

            storeSelectedLayout(design.filename)

            requireActivity().runOnUiThread {
                showLayout()
            }
        } else {
            Fuel.download(design.url)
                .streamDestination { response, _ ->
                    Pair(
                        outputStream
                    ) { response.body().toStream() }
                }
                .fileDestination { _, _ ->
                    file
                }
                .progress { readBytes, totalBytes ->
                    val progress = readBytes.toFloat() / totalBytes.toFloat() * 100
                }
                .response { result ->
                    result.fold(
                        success = {
                            storeSelectedLayout(design.filename)

                            requireActivity().runOnUiThread {
                                showLayout()
                            }
                        },
                        failure = {
                            it.printStackTrace()
                            Toast.makeText(requireContext(), "Error downloading file!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    )
                }
        }
    }

    private fun initViews() {
        adapter = DesignsAdapter(designs) {
            downloadLayout(it)
        }

        binding.loadButton.setOnClickListener {
            val eventId = binding.editText.text.toString().toIntOrNull() ?: return@setOnClickListener
            fetchEventInfo(eventId).map {
                val event = it ?: return@map
                updateEvent(event)
            }.subscribe()
        }

        binding.submitButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.textInputLayout.setEndIconOnClickListener(View.OnClickListener {
            scanQrCodeLauncher.launch(null)
        });
    }

    private fun updateEvent(event: Event) {
        if(!event.design_url.isNullOrEmpty())
            downloadLayout(Design("", event.design_url!!.substringAfterLast("/").removeSuffix(".png"), event.design_url!!))

        if(!event.animation_url.isNullOrEmpty())
            downloadLayout(Design("", event.animation_url!!.substringAfterLast("/").removeSuffix(".gif"), event.animation_url!!))

        if(!event.mosaic_url.isNullOrEmpty())
            downloadMosaic(Design("", event.mosaic_url!!.substringAfterLast("/").removeSuffix(".jpg"), event.mosaic_url!!))
    }

    fun downloadMosaic(design: Design) {
        val url = design.url
        if (!File("${requireContext().cacheDir}/mosaics/").exists()) {
            File("${requireContext().cacheDir}/mosaics/").mkdirs()
        }

        val file = File("${requireContext().cacheDir}/mosaics/${design.filename}")
        if (file.exists()) {
            file.delete()
        }

        val outputStream = FileOutputStream(file)

        Fuel.download(url)
            .streamDestination { response, _ ->
                Pair(
                    outputStream
                ) { response.body().toStream() }
            }
            .fileDestination { _, _ ->
                file
            }
            .progress { readBytes, totalBytes ->
                val progress = readBytes.toFloat() / totalBytes.toFloat() * 100
                requireActivity().runOnUiThread { binding.submitButton.text = "Download ${progress}%" }
            }
            .response { result ->
                result.fold(
                    success = {

                        requireActivity().runOnUiThread {
                            binding.submitButton.text = "Submit"
                            val pair = design.filename.substringAfter(":").substringBefore(".").split(":")
                            val columns = pair[0].toIntOrNull()
                            val rows = pair[1].toIntOrNull()

                            if(rows != null && columns != null) {
                                MosaicManager.splitBitmap("${requireContext().cacheDir}/mosaics/${design.filename}", columns, rows)
                                MosaicManager.startMosaic(requireContext()) {}
                            }
                        }
                    },
                    failure = {
                        it.printStackTrace()
                        Toast.makeText(requireContext(), "Error downloading file!", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            }

    }

    private fun fetchEventInfo(eventId: Int): Observable<Event> {
        return service
            .getEvent(eventId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                //Glide.with(requireContext()).clear(binding.ivLayout)
                //binding.tvEventDescription.text = ""
                //binding.progressBar.show()
            }
            .doOnError {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error!")
                    .setMessage(it.message)
                    .show()
                //binding.progressBar.hide()
            }
            .doOnComplete {
                //binding.progressBar.hide()
            }
    }

}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}