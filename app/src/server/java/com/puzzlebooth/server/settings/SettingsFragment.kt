package com.puzzlebooth.server.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.Contacts.Photo
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.nearby.connection.Strategy
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentSettingsBinding
import com.puzzlebooth.server.settings.PhotoQuality.Companion.getAppVersionName
import com.puzzlebooth.server.settings.listing.SettingsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

        binding.btnCanonTwoPrinters.setOnClickListener {
            toggleCanonPrintingTwoPrinters()
            updateViews()
        }

        binding.btnCanon.setOnClickListener {
            toggleCanonPrinting()
            updateViews()
        }

        binding.btnTwoCopies?.setOnClickListener {
            toggleTwoCopies()
            updateViews()
        }

        binding.btnShowQR.setOnClickListener {
            toggleShowQR()
            updateViews()
        }

        binding.btnMultiPhoto?.setOnClickListener {
            toggleMultiPhoto()
            updateViews()
        }

        binding.btnFollowQR.setOnClickListener {
            toggleFollowQR()
            updateViews()
        }

        binding.btnClearGlideCache?.setOnClickListener {
            clearCacheGlide()
        }

        binding.backButton?.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnSaveOriginal?.setOnClickListener {
            toggleSaveOriginal()
            updateViews()
        }

        val version = "Version: " + getAppVersionName(requireContext())
        binding.tvVersion.text = version

    }

    fun clearCacheGlide() {
        GlobalScope.launch(Dispatchers.IO) {
            // Call your function here
            Glide.get(requireContext()).clearDiskCache()
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

    fun toggleMultiPhoto() {
        val current = sharedPreferences.getBoolean("settings:multiPhoto", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:multiPhoto", !current)
        edit.apply()
    }

    fun toggleFollowQR() {
        val current = sharedPreferences.getBoolean("settings:followQR", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:followQR", !current)
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

    fun toggleCanonPrintingTwoPrinters() {
        val current = sharedPreferences.getBoolean("settings:canonPrintingTwoPrinters", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:canonPrintingTwoPrinters", !current)
        edit.apply()
    }

    fun toggleCanonPrinting() {
        val current = sharedPreferences.getBoolean("settings:canonPrinting", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:canonPrinting", !current)
        edit.apply()
    }

    fun toggleSaveOriginal() {
        val current = sharedPreferences.getBoolean("settings:saveOriginal", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:saveOriginal", !current)
        edit.apply()
    }

    fun toggleTwoCopies() {
        val current = sharedPreferences.getBoolean("settings:twoCopies", false)

        val edit = sharedPreferences.edit()
        edit.putBoolean("settings:twoCopies", !current)
        edit.apply()
    }

    fun toggleConnection() {
        val sharedPreferences = context?.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
        val current = sharedPreferences?.getInt("settings:connectionRemote", ConnectionRemote.entries[0].index)
        val edit = sharedPreferences?.edit()
        if(current == (ConnectionRemote.entries.size -1)) {
            edit?.putInt("settings:connectionRemote", 0)
        } else {
            if (current != null) {
                edit?.putInt("settings:connectionRemote", current + 1)
            }
        }

        edit?.apply()
    }

    fun toggleQuality() {
        //val current = PhotoQuality.getCurrentQuality(requireContext())
        PhotoQuality.toggleQuality(requireContext())
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:printingQuality", !current)
//        edit.apply()
    }

    fun getCurrentConnection(context: Context): ConnectionRemote {
        val sharedPreferences = context.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
        val quality = sharedPreferences.getInt("settings:connectionRemote", ConnectionRemote.entries[0].index)
        return ConnectionRemote.entries[quality]
    }

    fun Button.setStatus(isEnabled: Boolean, prefix: String) {
        if(isEnabled) {
            this.setBackgroundColor(Color.GREEN)
        } else {
            this.setBackgroundColor(Color.RED)
        }

        this.text = "${prefix}: ${if(isEnabled) "true" else "false"}"
    }

    fun updateViews() {
        val currentFlash = sharedPreferences.getBoolean("settings:flash", false)
        val currentAutoPhoto = if(sharedPreferences.getBoolean("settings:autoPhoto", false)) "ON" else "OFF"
        val currentPrintSlow = if(sharedPreferences.getBoolean("settings:printingSlow", false)) "ON" else "OFF"
        val currentPrintQuality = PhotoQuality.getCurrentQuality(requireContext()).getRepresentation()
        val currentButtonPrinting = sharedPreferences.getBoolean("settings:canonPrinting", false)
        val currentTouchMode = if(sharedPreferences.getBoolean("settings:touchMode", false)) "ON" else "OFF"
        val currentLandscape = if(sharedPreferences.getBoolean("settings:landscape", false)) "ON" else "OFF"
        val currentShowQR = sharedPreferences.getBoolean("settings:showQR", false)
        val currentVideoMessage = if(sharedPreferences.getBoolean("settings:isVideoMessage", false)) "ON" else "OFF"
        val currentButtonPrintingTwoPrinters = sharedPreferences.getBoolean("settings:canonPrintingTwoPrinters", false)
        val currentTwoCopies = sharedPreferences.getBoolean("settings:twoCopies", false)
        val currentFollowQR = sharedPreferences.getBoolean("settings:followQR", false)


        binding.btnShowQR.setStatus(sharedPreferences.getBoolean("settings:showQR", false), "Show QR")
        binding.btnFlash.setStatus(sharedPreferences.getBoolean("settings:flash", false), "Flash")
        binding.btnAutoPhoto.setStatus(sharedPreferences.getBoolean("settings:autoPhoto", true), "Auto Photo")
        binding.btnPrintSlow.setStatus(sharedPreferences.getBoolean("settings:printingSlow", true), "Print slow")
        binding.btnQualtiy.text = "Printing Quality: ${currentPrintQuality}"
        binding.btnTouchMode.setStatus(sharedPreferences.getBoolean("settings:touchMode", true), "Touch mode")
        binding.btnLandscape.text = "Landscape: ${currentLandscape}"
        binding.btnCanon.setStatus(sharedPreferences.getBoolean("settings:canonPrinting", false), "Canon printing")
        binding.btnCanonTwoPrinters.setStatus(sharedPreferences.getBoolean("settings:canonPrintingTwoPrinters", false), "Canon two printers")
        binding.btnTwoCopies?.setStatus(sharedPreferences.getBoolean("settings:twoCopies", false), "2 copies")
        binding.btnSaveOriginal?.setStatus(sharedPreferences.getBoolean("settings:saveOriginal", false), "Save original")
        binding.btnVideoMessage.setStatus(sharedPreferences.getBoolean("settings:isVideoMessage", false), "Video message")
        binding.btnFollowQR.setStatus(sharedPreferences.getBoolean("settings:followQR", false), "Follow QR")
        binding.btnMultiPhoto?.setStatus(sharedPreferences.getBoolean("settings:multiPhoto", false), "Multi Photo")


    }
}

enum class ConnectionRemote(val index: Int) {
    P2P_STAR(0), P2P_CLUSTER(1), P2P_POINT_TO_POINT(2)
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
        fun getAppVersionName(context: Context): String {
            return try {
                val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                "Version not found"
            }
        }

        fun getCurrentQualityInt(context: Context): Int {
            return getCurrentQuality(context).quality
        }

        fun getCurrentQuality(context: Context): PhotoQuality {
            val sharedPreferences = context.getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
            val quality = sharedPreferences.getInt("settings:printingQuality", PhotoQuality.entries.size - 3)
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