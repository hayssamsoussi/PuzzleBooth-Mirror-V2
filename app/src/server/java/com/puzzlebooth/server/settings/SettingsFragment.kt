package com.puzzlebooth.server.settings

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentSettingsBinding
import com.puzzlebooth.server.settings.listing.SettingsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

enum class SettingType {
    TOGGLE, MULTI_TOGGLE, INPUT, DETAILS;
}

data class Setting(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultValue: String,
    val type: SettingType
)

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    lateinit var adapter: SettingsAdapter
    private var settings = mutableListOf<Setting>()

    override fun initViewBinding(view: View): FragmentSettingsBinding {
        return FragmentSettingsBinding.bind(view)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initData()
        initViews()
    }

    private fun initData() {
        settings.addAll(
            listOf(
                Setting("towerbooth","Tower", "Tower booth animation", sharedPreferences.getString("settings:towerbooth", "") ?: "", SettingType.MULTI_TOGGLE),
                Setting("flash","Flash", "Flash", sharedPreferences.getBoolean("settings:flash", false).toString(), SettingType.MULTI_TOGGLE),
            )
        )
    }

    private fun initViews() {
//        adapter = SettingsAdapter(settings) {
//            PopupMenu(requireContext(), it.first).apply {
//                this.menu.add("test")
//                this.menu.add("test2")
//            }.show()
//        }

        //binding.settingsList.adapter = adapter
        //binding.settingsList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.btnFlash.setOnClickListener {
            toggleFlash()
            updateViews()
        }

        binding.btnAutoPhoto.setOnClickListener {
            toggleAutoPhoto()
            updateViews()
        }

        binding.btnQualtiy.setOnClickListener {
            toggleQuality()
            updateViews()
        }

        binding.btnTouchMode.setOnClickListener {
            toggleTouchMode()
            updateViews()
        }

        binding.btnLandscape.setOnClickListener {
            toggleLandscape()
            updateViews()

            Handler().postDelayed(Runnable {
                activity?.recreate()
            }, 2000)
        }

        binding.btnShowQR.setOnClickListener {
            toggleShowQR()
            updateViews()
        }
    }

    override fun onResume() {
        super.onResume()
        updateViews()
    }

    fun toggleShowQR() {
        val current = sharedPreferences.getBoolean("settings:showQR", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:showQR", !current)
        edit.apply()
    }

    fun toggleFlash() {
        val current = sharedPreferences.getBoolean("settings:flash", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:flash", !current)
        edit.apply()
    }

    fun toggleAutoPhoto() {
        val current = sharedPreferences.getBoolean("settings:autoPhoto", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:autoPhoto", !current)
        edit.apply()
    }

    fun toggleTouchMode() {
        val current = sharedPreferences.getBoolean("settings:touchMode", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:touchMode", !current)
        edit.apply()
    }

    fun toggleLandscape() {
        val current = sharedPreferences.getBoolean("settings:landscape", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:landscape", !current)
        edit.apply()
    }

    fun togglePrintingSlow() {
        val current = sharedPreferences.getBoolean("settings:printingSlow", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:printingSlow", !current)
        edit.apply()
    }

    fun toggleQuality() {
        val current = sharedPreferences.getBoolean("settings:printingQuality", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:printingQuality", !current)
        edit.apply()
    }

    fun updateViews() {
        val currentFlash = if(sharedPreferences.getBoolean("settings:flash", false)) "ON" else "OFF"
        val currentAutoPhoto = if(sharedPreferences.getBoolean("settings:autoPhoto", false)) "ON" else "OFF"
        val currentPrintSlow = if(sharedPreferences.getBoolean("settings:printingSlow", false)) "ON" else "OFF"
        val currentPrintQuality = if(sharedPreferences.getBoolean("settings:printingQuality", false)) "100%" else "40%"
        val currentTouchMode = if(sharedPreferences.getBoolean("settings:touchMode", false)) "ON" else "OFF"
        val currentLandscape = if(sharedPreferences.getBoolean("settings:landscape", false)) "ON" else "OFF"
        val currentShowQR = if(sharedPreferences.getBoolean("settings:showQR", false)) "ON" else "OFF"

        binding.btnShowQR.text = "Show QR: ${currentShowQR}"
        binding.btnFlash.text = "Flash: ${currentFlash}"
        binding.btnAutoPhoto.text = "Auto Photo: ${currentAutoPhoto}"
        binding.btnPrintSlow.text = "Printing Slow: ${currentPrintSlow}"
        binding.btnQualtiy.text = "Printing Quality: ${currentPrintQuality}"
        binding.btnTouchMode.text = "Touch Mode: ${currentTouchMode}"
        binding.btnLandscape.text = "Landscape: ${currentLandscape}"

    }
}