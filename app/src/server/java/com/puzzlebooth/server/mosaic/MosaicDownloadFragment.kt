package com.puzzlebooth.server.mosaic

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.Fuel
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentMosaicDownloadBinding
import com.puzzlebooth.server.network.Design
import com.puzzlebooth.server.theme.listing.DesignsAdapter
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

    fun initViews() {
        adapter = DesignsAdapter(designs) {
            currentDesign = it
            Glide.with(requireContext()).load(it.url).into(binding.ivLayout)
        }


        binding.downloadButton.setOnClickListener {
            currentDesign?.let { it1 -> downloadMosaic(it1) }
        }

        binding.deleteButton.setOnClickListener {
            MosaicManager.deleteAll()
        }

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvList.adapter = adapter

    }
}