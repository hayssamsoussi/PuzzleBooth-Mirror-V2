package com.puzzlebooth.server.theme

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.kittinunf.fuel.Fuel
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.utils.showMenuDialog
import com.puzzlebooth.server.R
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

class ThemeFragment : BaseFragment<FragmentThemeBinding>(R.layout.fragment_theme) {

    companion object {
        var showLocal = false
    }

    var isFromMulti = false
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

    override fun initViewBinding(view: View): FragmentThemeBinding {
        return FragmentThemeBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFromMulti = arguments?.getBoolean("isFromMulti", false) == true

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
                designs.add(Design("", "blank.png", "https://www.puzzleslb.com/puzzlebooth/uploads/mirror_booth_uploads/layouts_backup/blank.png"))
                if(showLocal) designs.addAll(getLocalLayouts())
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
        val keyName = if(isFromMulti) "selectMultiLayout" else "selectedLayout"
        val layoutName = sharedPreferences.getString(keyName, "")
        if (layoutName?.isNotEmpty() == true) {
            val layoutFile = File("${requireContext().cacheDir}/layouts/${layoutName}")
            if (layoutFile.exists()) {
                Glide.with(this)
                    .load(layoutFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivLayout)
            }
        }
    }

    private  fun storeSelectedLayout(fileName: String) {
        val keyName = if(isFromMulti) "selectMultiLayout" else "selectedLayout"
        val edit = sharedPreferences.edit()
        edit.putString(keyName, fileName)
        edit.apply()
    }

    private fun downloadMosaic(url: String) {
        if (!File("${requireContext().cacheDir}/layouts/").exists()) {
            File("${requireContext().cacheDir}/layouts/").mkdirs()
        }

        val file = File("${requireContext().cacheDir}/layouts/mosaic.jpg")
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
            }
            .response { result ->
                result.fold(
                    success = {
                        //storeSelectedLayout()
//                        requireActivity().runOnUiThread {
//                            MosaicManager.splitBitmap("${requireContext().cacheDir}/layouts/mosaic.jpg", 8, 11)
//                        }
                    },
                    failure = {
                        it.printStackTrace()
                        Toast.makeText(requireContext(), "Error downloading file!", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            }
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

        binding.submitButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvList.adapter = adapter


        binding.textInputLayout.setEndIconOnClickListener(View.OnClickListener {
            scanQrCodeLauncher.launch(null)
        });

        binding.numericKeyboard?.keySpecialListener = View.OnClickListener {
            val eventId = binding.editText.text.toString().toIntOrNull() ?: return@OnClickListener
            fetchEventInfo(eventId).map {
                val event = it ?: return@map
                updateEvent(event)
            }.subscribe()
        }

        binding.header?.setOnClickListener {
            val showOrHideLocals = if(showLocal) "Hide LOCAL" else "Show LOCAL"
            val menuItems = arrayOf(showOrHideLocals)
            requireContext().showMenuDialog("Choose an option", menuItems) { index ->
                when (index) {
                    0 -> {
                        showLocal = !showLocal
                        initData()
                    }
                }
            }
        }

        binding.backButton?.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.scanButton.visibility = View.GONE
        binding.mosaicButton?.visibility = View.GONE
    }

    private fun updateEvent(event: Event) {
//        val stringBuilder = StringBuilder()
//        stringBuilder.appendLine("ID: ${event.id}")
//        stringBuilder.appendLine("Names: ${event.names}")
//        stringBuilder.appendLine("Location: ${event.location}")
//        //binding.tvEventDescription.text = stringBuilder.toString()

        if(!event.design_url.isNullOrEmpty()) {
            downloadLayout(Design("", event.design_url!!.substringAfterLast("/"), event.design_url!!))
        } else {
            AlertDialog.Builder(requireContext())
                .setMessage("No design assigned to this event!")
                .show()
        }
    }

    private fun fetchEventInfo(eventId: Int): Observable<Event> {
        return service
            .getEvent(eventId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                Glide.with(requireContext()).clear(binding.ivLayout)
                //binding.tvEventDescription.text = ""
                binding.progressBar.show()
            }
            .doOnError {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error!")
                    .setMessage(it.message)
                    .show()
                binding.progressBar.hide()
            }
            .doOnComplete {
                binding.progressBar.hide()
            }
    }

}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}