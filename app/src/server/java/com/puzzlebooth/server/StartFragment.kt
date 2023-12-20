package com.puzzlebooth.server

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.nearby.connection.Payload
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.server.databinding.FragmentStartBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class StartFragment : BaseFragment<FragmentStartBinding>(R.layout.fragment_start) {

    private val externalStorageResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission needed!", Toast.LENGTH_SHORT).show()
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

    override fun initViewBinding(view: View): FragmentStartBinding {
        return FragmentStartBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        binding.textDisplay.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_countdownFragment)
        }

        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_settingsFragment)
        }

        binding.album.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_albumFragment)
        }

        binding.theme.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_themeFragment)
        }

        binding.camera.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_cameraFragment)
        }
//        binding.layouts.setOnClickListener {
//            val activity = requireActivity()
//            if(activity is MainActivity) {
//                activity.send(Payload.fromBytes("test".toByteArray()))
//            }
//            //findNavController().navigate(R.id.action_startFragment_to_layoutFragment)
//        }

        binding.bluetooth.setOnClickListener {
            ///findNavController().navigate(R.id.action_startFragment_to_bluetoothFragmentt)
        }
    }

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
                    //externalStorageResult.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
            "bluetooth" -> binding.bluetooth.performClick()
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