package com.puzzlebooth.server.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.Contacts.Photo
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentSettingsBinding
import com.puzzlebooth.server.settings.listing.SettingsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.math.min

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

        binding.btnVideoMessage.setOnClickListener {
            toggleVideoMessage()
            updateViews()
        }

        binding.btnPrintSlow.setOnClickListener {
            togglePrintingSlow()
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

        val version = "Version: " + getAppVersionName()
        binding.tvVersion.text = version

    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Version not found"
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

    fun toggleVideoMessage() {
        val current = sharedPreferences.getBoolean("settings:isVideoMessage", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:isVideoMessage", !current)
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
        //val current = PhotoQuality.getCurrentQuality(requireContext())
        PhotoQuality.toggleQuality(requireContext())
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:printingQuality", !current)
//        edit.apply()
    }

    fun updateViews() {
        val currentFlash = if(sharedPreferences.getBoolean("settings:flash", false)) "ON" else "OFF"
        val currentAutoPhoto = if(sharedPreferences.getBoolean("settings:autoPhoto", false)) "ON" else "OFF"
        val currentPrintSlow = if(sharedPreferences.getBoolean("settings:printingSlow", false)) "ON" else "OFF"
        val currentPrintQuality = PhotoQuality.getCurrentQuality(requireContext()).getRepresentation()
        val currentTouchMode = if(sharedPreferences.getBoolean("settings:touchMode", false)) "ON" else "OFF"
        val currentLandscape = if(sharedPreferences.getBoolean("settings:landscape", false)) "ON" else "OFF"
        val currentShowQR = if(sharedPreferences.getBoolean("settings:showQR", false)) "ON" else "OFF"
        val currentVideoMessage = if(sharedPreferences.getBoolean("settings:isVideoMessage", false)) "ON" else "OFF"

        binding.btnShowQR.text = "Show QR: ${currentShowQR}"
        binding.btnFlash.text = "Flash: ${currentFlash}"
        binding.btnAutoPhoto.text = "Auto Photo: ${currentAutoPhoto}"
        binding.btnPrintSlow.text = "Printing Slow: ${currentPrintSlow}"
        binding.btnQualtiy.text = "Printing Quality: ${currentPrintQuality}"
        binding.btnTouchMode.text = "Touch Mode: ${currentTouchMode}"
        binding.btnLandscape.text = "Landscape: ${currentLandscape}"
        binding.btnVideoMessage.text = "isVideoMessage: ${currentVideoMessage}"

    }
}

enum class PhotoQuality(val quality: Int) {
    QUALITY_20(20), QUALITY_40(40), QUALITY_60(60), QUALITY_80(80), QUALITY_100(100);

    fun getRepresentation(): String {
        return when(this) {
            QUALITY_20 -> "20%"
            QUALITY_40 -> "40%"
            QUALITY_60 -> "60%"
            QUALITY_80 -> "80%"
            QUALITY_100 -> "100%"
        }
    }

    companion object {
        fun getCurrentQualityInt(context: Context): Int {
            return getCurrentQuality(context).quality
        }

        fun getCurrentQuality(context: Context): PhotoQuality {
            val sharedPreferences = context.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
            val quality = sharedPreferences.getInt("settings:printingQuality", PhotoQuality.entries.size - 1)
            return PhotoQuality.entries[quality]
        }

        fun toggleQuality(context: Context) {
            val sharedPreferences = context.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
            val current = sharedPreferences.getInt("settings:printingQuality", PhotoQuality.entries.size - 1)
            val edit = sharedPreferences.edit()
            if(current == (PhotoQuality.entries.size -1)) {
                edit.putInt("settings:printingQuality", 0)
            } else {
                edit.putInt("settings:printingQuality", current + 1)
            }

            edit.apply()
        }


    }
}