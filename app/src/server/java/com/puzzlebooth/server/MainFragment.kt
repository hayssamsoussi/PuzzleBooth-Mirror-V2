package com.puzzlebooth.server

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.server.databinding.FragmentMainBinding
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.network.SyncRequest
import com.puzzlebooth.server.network.SyncResponse
import com.puzzlebooth.server.settings.PhotoQuality.Companion.getAppVersionName
import com.puzzlebooth.server.utils.Status
import com.puzzlebooth.server.utils.SyncManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.JsonElement
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.BufferedReader
import java.io.InputStreamReader


class MainFragment: BaseFragment<FragmentMainBinding>(R.layout.fragment_main) {

    private val manageStorageResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                askForPermissions()
                Toast.makeText(requireContext(), "Permission needed!", Toast.LENGTH_SHORT).show()
            }
        }

    private val externalStorageResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission needed!", Toast.LENGTH_SHORT).show()
            }
        }

    fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                return
            }
            //createDir()
        }
    }

    private val readExternalStorageResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
                //Handler().postDelayed(Runnable { takePhoto() }, 2000)
            } else {
                Toast.makeText(requireContext(), "Permission needed!", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                //startCamera()
            } else {
                Toast.makeText(requireContext(), "Permission needed!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun initViewBinding(view: View): FragmentMainBinding {
        return FragmentMainBinding.bind(view)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        checkPermissions()
    }

    private fun initViews() {

        val edit = sharedPreferences.edit()
        edit.putFloat("camera:exposure", 0F)
        edit.apply()

        binding.textDisplay.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_startFragment)
        }

        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        binding.album.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_albumFragment)
        }

        binding.designs.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_designsFragment)
        }

        binding.theme.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_themeFragment)
        }

        binding.multiTheme.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_themeFragment, Bundle().apply {
                this.putBoolean("isFromMulti", true)
            })
        }

        binding.hayssam.setOnClickListener {
            //syncDeviceStatus("hayssam", "default", "online")
            fetchConfiguration("A")
        }

        binding.animations.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Portrait or Landscape?")
                .setPositiveButton("Portrait", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        findNavController().navigate(R.id.action_mainFragment_to_animationsFragment)
                        dialog?.dismiss()
                    }
                })
                .setNegativeButton("Landscape", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        findNavController().navigate(R.id.action_mainFragment_to_animationsLandFragment)
                        dialog?.dismiss()
                    }

                }).show()

        }

        binding.camera.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_cameraFragment)
        }
//        binding.layouts.setOnClickListener {
//            val activity = requireActivity()
//            if(activity is MainActivity) {
//                activity.send(Payload.fromBytes("test".toByteArray()))
//            }
//            //findNavController().navigate(R.id.action_startFragment_to_layoutFragment)
//        }

        binding.mosaic.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_mosaicFragment)
        }

        binding.allDesigns.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_allDesignsFragment)
        }

        binding.sync.setOnClickListener {
            sync()
        }


        val version = "Version: " + getAppVersionName(requireContext())
        binding.tvVersion.text = version

//        binding.audio.setOnClickListener {
//            startActivity(Intent(requireContext(), AudioActivity::class.java))
//        }
    }

    fun syncDeviceStatus(deviceId: String, eventId: String, status: String) {
        val request = SyncRequest(
            device_id = deviceId,
            event_id = eventId,
            status = status
        )

        service.syncStatus(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ syncResponse: SyncResponse ->
                // Handle successful sync response
                println("hhh Sync successful: ${syncResponse.message}")
            }, { error ->
                // Handle errors here
                println("hhh Error syncing status: ${error.localizedMessage}")
            })
    }


    fun fetchConfiguration(deviceType: String, eventId: String = "default") {
        service.fetchConfig(deviceType, eventId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ configJson: JsonElement ->
                // Handle the JSON configuration response
                println("hhh  Fetched configuration: $configJson")
                // Process the JSON as needed
            }, { error ->
                // Handle errors here
                println("hhh Error fetching config: ${error.localizedMessage}")
            })
    }


    fun sync() {
        val progressDialog: ProgressDialog = ProgressDialog(requireContext())

        SyncManager(requireContext()) {
            requireActivity().runOnUiThread {
                println("*** syncmanager: status: ${it}")
                when(it.first) {
                    Status.DONE -> {
                        progressDialog.dismiss()
                    }
                    Status.SYNCING -> {
                        progressDialog
                            .setTitle("Syncing")
                        progressDialog.show()
                    }
                    Status.ERROR -> {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermissions() {
        binding.cameraPermission.apply {
            val isAllowed = (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            if(isAllowed) {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                this.visibility = View.GONE
            } else {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }

            if(!isAllowed)
                setOnClickListener {
                    cameraPermissionResult.launch(android.Manifest.permission.CAMERA)
                }
            else
                setOnClickListener(null)
        }

        binding.storagePermission.apply {
            val isAllowed =
                (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)

            if(isAllowed) {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                this.visibility = View.GONE
            } else {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }

            if(!isAllowed)
                setOnClickListener {
                    readExternalStorageResult.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            else
                setOnClickListener(null)
        }

        binding.manageStoragePermission.apply {
            val isAllowed = Environment.isExternalStorageManager() == true

            if(isAllowed) {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                this.visibility = View.GONE
            } else {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }

            if(!isAllowed)
                setOnClickListener {
                    manageStorageResult.launch(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                }
            else
                setOnClickListener(null)

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        when(event?.text) {
            "start" -> binding.textDisplay.performClick()
            "showAlbum" -> binding.album.performClick()
            "showsecretmenu" -> binding.camera.performClick()
            "theme" -> binding.theme.performClick()
            "animations" -> binding.animations.performClick()
            "mosaic" -> binding.mosaic.performClick()
            "sync" -> binding.sync.performClick()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}