package com.puzzlebooth.server.animations

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
import com.puzzlebooth.server.databinding.FragmentThemeBinding
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

class AnimationsFragment : BaseFragment<FragmentThemeBinding>(R.layout.fragment_theme) {

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

        initViews()
        initData()
    }

    private fun initData() {
        service
            .listAnimations()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                it.printStackTrace()
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
        val list = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/").listFiles()?.filter { it.name.endsWith(".gif") }
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
        val layoutName = sharedPreferences.getString("selectedAnimation", "")
        if (layoutName?.isNotEmpty() == true) {
            val layoutFile = File("${requireContext().cacheDir}/animations/${layoutName}")
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
        val edit = sharedPreferences.edit()
        edit.putString("selectedAnimation", fileName)
        edit.apply()
    }
    private fun downloadLayout(design: Design) {
        val locallayouts = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/"

        if (!File("${requireContext().cacheDir}/animations/").exists()) {
            File("${requireContext().cacheDir}/animations/").mkdirs()
        }

        val file = File("${requireContext().cacheDir}/animations/${design.filename}")
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

                    requireActivity().runOnUiThread {
                        binding.submitButton.text = progress.toString()
                    }
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


        binding.scanButton.visibility = View.GONE
        binding.mosaicButton?.visibility = View.GONE
    }

    private fun updateEvent(event: Event) {
        event.animation_url?.let {
            downloadLayout(Design("", it.substringAfterLast("/").removeSuffix(".gif"), it))
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