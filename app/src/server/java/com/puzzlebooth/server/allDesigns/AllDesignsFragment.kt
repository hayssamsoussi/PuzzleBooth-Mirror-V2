package com.puzzlebooth.server.allDesigns

import android.app.AlertDialog
import android.graphics.Color
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
import com.puzzlebooth.server.databinding.FragmentAllDesignsBinding
import com.puzzlebooth.server.databinding.FragmentThemeBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.mosaic.fadeOutAndDisable
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

class AllDesignsFragment : BaseFragment<FragmentAllDesignsBinding>(R.layout.fragment_all_designs) {

    private var designs = mutableListOf<Design>()
    private lateinit var adapter: DesignsAdapter
    var mosaic: Design? = null
    var animation: Design? = null
    var layout: Design? = null
    var eventID: String? = null

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
                    updateEvent(it)
                }.doOnError {
                    it.printStackTrace()
                }.subscribe()
            }

            QRResult.QRUserCanceled -> "User canceled"
            QRResult.QRMissingPermission -> "Missing permission"
            is QRResult.QRError -> "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
        }
        result.let {  }
    }

    override fun initViewBinding(view: View): FragmentAllDesignsBinding {
        return FragmentAllDesignsBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initData() {
//        service
//            .listDesigns()
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .doOnError {
//                AlertDialog.Builder(requireContext())
//                    .setTitle("Error")
//                    .setMessage(it.message)
//                    .show()
//            }
//            .doOnNext {
//                designs.clear()
//                designs.addAll(getLocalLayouts())
//                designs.addAll(it)
//                adapter.notifyDataSetChanged()
//            }.subscribe()
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
        val animationName = sharedPreferences.getString("selectedAnimation", "")
        val mosaicName = sharedPreferences.getString("selectedMosaic", "")
        println("hhh showLayout animationName ${animationName}")
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

        if(animationName?.isNotEmpty() == true) {
            val animationFile = File("${requireContext().cacheDir}/animations/${animationName}")
            if (animationFile.exists()) {
                println("hhh animationFile exists")
                Glide.with(this)
                    .load(animationFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivAnimation)
            }
        }

        if(mosaicName?.isNotEmpty() == true) {
            val mosaicFile = File("${requireContext().cacheDir}/mosaics/${mosaicName}")
            if (mosaicFile.exists()) {
                Glide.with(this)
                    .load(mosaicFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.transform(RotateTransformation(requireContext(), 270f))
                    .into(binding.ivMosaic)
            }
        }


    }

    private  fun storeSelectedLayout(fileName: String) {
        val edit = sharedPreferences.edit()
        edit.putString("selectedLayout", fileName)
        edit.apply()
    }

    private  fun storeSelectedAnimation(fileName: String) {
        println("hhh storing selected animation ${fileName}")
        val edit = sharedPreferences.edit()
        edit.putString("selectedAnimation", fileName)
        edit.apply()
    }

    private fun storeSelectedMosaic(fileName: String) {
        println("hhh storingMosaic: ${fileName}")
        val edit = sharedPreferences.edit()
        edit.putString("selectedMosaic", fileName)
        edit.apply()
    }

    fun showRemoteLayout() {
        if(layout != null) {
            Glide.with(requireContext())
                .load(layout?.url)
                .into(binding.ivDesign)
        } else {
            Glide.with(requireContext()).clear(binding.ivDesign)
        }

        if(mosaic != null) {
            Glide.with(requireContext())
            .load(mosaic?.url)
            .into(binding.ivMosaic)
        } else {
            Glide.with(requireContext()).clear(binding.ivMosaic)
        }

        if(animation != null) {
            Glide.with(requireContext())
                .load(animation?.url)
                .into(binding.ivAnimation)
        } else {
            Glide.with(requireContext()).clear(binding.ivAnimation)
        }
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

    private fun downloadAnimation(design: Design) {
        println("hhh animation url ${design.toString()}")
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
                        binding.animationLabel.text = "Animation {${progress.toInt()}%}"
                    }
                }
                .response { result ->
                    result.fold(
                        success = {

                            requireActivity().runOnUiThread {
                                showRemoteLayout()
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

    fun downloadMosaic(design: Design) {
        println("hhh downloading ${design.toString()}")
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
                requireActivity().runOnUiThread {
                    binding.mosaicLabel.text = "Mosaic {${progress}%}" }
            }
            .response { result ->
                result.fold(
                    success = {

                        requireActivity().runOnUiThread {
                            //binding.mosaicLabel.text = "Mosaic"


                            requireActivity().runOnUiThread {
                                showRemoteLayout()
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
                        binding.designLabel.text = "Design {${progress.toInt()}%}"
                    }
                }
                .response { result ->
                    result.fold(
                        success = {
                            requireActivity().runOnUiThread {
                                showRemoteLayout()
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

    fun storeEventID(id: String) {
        val edit = sharedPreferences.edit()
        edit.putString("eventID", id)
        edit.apply()
    }

    private fun initViews() {
        adapter = DesignsAdapter(designs) {
            downloadLayout(it)
        }

        binding.clearButton.setOnClickListener {
            layout = null
            mosaic = null
            animation = null

            storeEventID("-1")
            showRemoteLayout()
        }

        binding.saveButton.setOnClickListener {
            binding.progressBarContainer.show()
            val eventQuery = binding.editText.text.toString()
            storeEventID(eventQuery)
            storeSelectedLayout(layout?.filename ?: "")
            storeSelectedAnimation(animation?.filename ?: "")
            storeSelectedMosaic(mosaic?.filename ?: "")

            if(mosaic != null) {
                val mosaicDesign = sharedPreferences.getString("selectedMosaic", "")
                val pair = mosaicDesign?.substringAfter(":")?.substringBefore(".")?.split(":")
                val columns = pair?.get(0)?.toIntOrNull()
                val rows = pair?.get(1)?.toIntOrNull()

                if(rows != null && columns != null) {
                    println("hhh fileName:${"${requireContext().cacheDir}/mosaics/${mosaic!!.filename}"}")
                    MosaicManager.splitBitmap("${requireContext().cacheDir}/mosaics/${sharedPreferences.getString("selectedMosaic", "")}", columns, rows)
                    MosaicManager.startMosaic(requireContext()) {}

                }
            }

            binding.progressBarContainer.hide()
            findNavController().popBackStack()
        }

        binding.refreshButton.setOnClickListener {
            val eventQuery = binding.editText.text.toString()

            eventQuery.toIntOrNull()?.let {
                fetchEventInfo(it).map {
                    updateEvent(it)
                    binding.saveButton.isEnabled = true
                    binding.saveButton.alpha = 1F
                }.doOnError {
                    it.printStackTrace()
                }.subscribe()
            }
        }

        binding.textInputLayout.setEndIconOnClickListener(View.OnClickListener {
            scanQrCodeLauncher.launch(null)
        });
    }

    private fun updateEvent(event: Event) {
        var mosaic = event.mosaic_url
        val design = event.design_url
        val animation = event.animation_url

        binding.designLabel.setTextColor(if(design.isNullOrEmpty()) Color.RED else Color.WHITE)
        binding.mosaicLabel.setTextColor(if(mosaic.isNullOrEmpty()) Color.RED else Color.WHITE)
        binding.animationLabel.setTextColor(if(animation.isNullOrEmpty()) Color.RED else Color.WHITE)

        event.design_url?.let { design ->
            if(design.isNotEmpty()) {
                this.layout = Design("", design.substringAfterLast("/"), design)
                downloadLayout(this.layout!!)
            }
        }

        event.animation_url?.let { animation ->
            if(animation.isNotEmpty()) {
                this.animation = Design("", animation.substringAfterLast("/"), animation)
                downloadAnimation(this.animation!!)
                //downloadAnimation(Design("", animation.substringAfterLast("/"), animation))
            }
        }

        event.mosaic_url?.let { mosaic ->
            if(mosaic.isNotEmpty()) {
                this.mosaic = Design("", mosaic.substringAfterLast("/"), mosaic)
                downloadMosaic(this.mosaic!!)
            }
        }
    }

    private fun fetchEventInfo(eventId: Int): Observable<Event> {
        return service
            .getEvent(eventId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                storeSelectedAnimation("")
                storeSelectedMosaic("")
                storeSelectedLayout("")

                Glide.with(requireContext()).clear(binding.ivDesign)
                Glide.with(requireContext()).clear(binding.ivMosaic)
                Glide.with(requireContext()).clear(binding.ivAnimation)

                binding.designLabel.setTextColor(Color.WHITE)
                binding.mosaicLabel.setTextColor(Color.WHITE)
                binding.animationLabel.setTextColor(Color.WHITE)

                //binding.tvEventDescription.text = ""
                binding.progressBarContainer.show()
            }
            .doOnError {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error!")
                    .setMessage(it.message)
                    .show()
                binding.progressBarContainer.hide()
            }
            .doOnComplete {
                binding.progressBarContainer.hide()
            }
    }

}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}