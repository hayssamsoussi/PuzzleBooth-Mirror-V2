package com.puzzlebooth.server

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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


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

        binding.textDisplay.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_startFragment)
        }

        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        binding.album.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_albumFragment)
        }

        binding.theme.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_themeFragment)
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
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermissions() {
        binding.cameraPermission.apply {
            val isAllowed = (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            setTextColor(
                if(isAllowed) {
                    ContextCompat.getColor(requireContext(), R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.red)
                })

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

            setTextColor(
                if(isAllowed) {
                    ContextCompat.getColor(requireContext(), R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.red)
                })

            if(!isAllowed)
                setOnClickListener {
                    readExternalStorageResult.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            else
                setOnClickListener(null)
        }

        binding.manageStoragePermission.apply {
            val isAllowed = Environment.isExternalStorageManager() == true

            setTextColor(
                if(isAllowed) {
                    ContextCompat.getColor(requireContext(), R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.red)
                })

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
            "mosaic" -> binding.mosaic.performClick()
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

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}