package com.puzzlebooth.server.mosaic

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.Fuel
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.animations.hide
import com.puzzlebooth.server.animations.show
import com.puzzlebooth.server.databinding.FragmentMosaicDownloadBinding
import com.puzzlebooth.server.network.Design
import com.puzzlebooth.server.network.Event
import com.puzzlebooth.server.theme.listing.DesignsAdapter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream


class MosaicDownloadFragment : BaseFragment<FragmentMosaicDownloadBinding>(R.layout.fragment_mosaic_download) {

    var currentDesign: Design? = null
    private var designs = mutableListOf<Design>()
    private lateinit var adapter: DesignsAdapter

    override fun initViewBinding(view: View): FragmentMosaicDownloadBinding {
        return FragmentMosaicDownloadBinding.bind(view)
    }

    private fun initData() {
        service
            .listMosaic()
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
                designs.addAll(it)
                adapter.notifyDataSetChanged()
            }.subscribe()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //MosaicManager.startMosaic(requireContext())
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        initViews()
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
                binding.downloadButton.text = "Download ${progress}%" }
            }
            .response { result ->
                result.fold(
                    success = {

                        requireActivity().runOnUiThread {
                            binding.downloadButton.text = "Download"
                            val pair = design.filename.substringAfter(":").substringBefore(".").split(":")
                            val columns = pair[0].toIntOrNull()
                            val rows = pair[1].toIntOrNull()

                            if(rows != null && columns != null) {
                                println("hhh fileName:${"${requireContext().cacheDir}/mosaics/${design.filename}"}")
                                MosaicManager.splitBitmap("${requireContext().cacheDir}/mosaics/${design.filename}", columns, rows)
                                MosaicManager.startMosaic(requireContext()) {}
                            }

                            findNavController().popBackStack()
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

    private fun updateEvent(event: Event) {
        if(!event.mosaic_url.isNullOrEmpty()) {
            currentDesign = Design("", event.mosaic_url!!.substringAfterLast("/").removeSuffix(".jpg"), event.mosaic_url!!)
            println("hhh currentDesign ${currentDesign.toString()}")
            currentDesign?.let {
                Glide.with(requireContext()).load(it.url).into(binding.ivLayout)
            }
            //downloadMosaic(Design("", event.mosaic_url.substringAfterLast("/").removeSuffix(".jpg"), event.mosaic_url))
        }
    }

    fun initViews() {
        adapter = DesignsAdapter(designs) {
            currentDesign = it
            Glide.with(requireContext()).load(it.url).into(binding.ivLayout)
        }

        binding.updateButton.setOnClickListener {
            val eventId = binding.editText.text.toString().toIntOrNull() ?: return@setOnClickListener
            fetchEventInfo(eventId).map {
                val event = it ?: return@map
                updateEvent(event)
            }.subscribe()
        }

        binding.exitButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.downloadButton.setOnClickListener {
            currentDesign?.let { it1 -> downloadMosaic(it1) }
        }

        binding.deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", object: OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        MosaicManager.deleteAll()
                    }
                })
                .setNegativeButton("No", object: OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        dialog?.dismiss()
                    }

                }).show()
        }

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvList.adapter = adapter

    }
}