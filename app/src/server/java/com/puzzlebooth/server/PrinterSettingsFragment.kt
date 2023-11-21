package com.puzzlebooth.server

//enum class SettingType {
//    TOGGLE, MULTI_TOGGLE, INPUT, DETAILS;
//}
//
//data class Setting(
//    val id: String,
//    val title: String,
//    val subtitle: String,
//    val type: SettingType
//)
//
//class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {
//
//    companion object {
//        val list = listOf<Setting>(
//            Setting("countdown","Countdown", "Countdown seconds", SettingType.INPUT),
//            Setting("compress","Compress", "Countdown seconds", SettingType.MULTI_TOGGLE),
//            Setting("flash","Use Flash", "Turn on flash", SettingType.TOGGLE),
//            Setting("animation","Animation", "What animation to use", SettingType.DETAILS),
//        )
//    }
//    override fun initViewBinding(view: View): FragmentSettingsBinding {
//        return FragmentSettingsBinding.bind(view)
//    }
//
//    private val coroutineScope = CoroutineScope(Dispatchers.Main)
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        initViews()
//    }
//
//    fun initViews() {
//        binding.btnFlash.setOnClickListener {
//            toggleFlash()
//            updateViews()
//        }
//
//        binding.btnTower.setOnClickListener {
//            toggleTower()
//            updateViews()
//        }
//
//        binding.btnAutoPhoto.setOnClickListener {
//            toggleAutoPhoto()
//            updateViews()
//        }
//
//        binding.btnPrintSlow.setOnClickListener {
//            togglePrintingSlow()
//            updateViews()
//        }
//    }
//
//    fun toggleFlash() {
//        val current = sharedPreferences.getBoolean("settings:flash", false)
//
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:flash", !current)
//        edit.apply()
//    }
//
//    fun toggleAutoPhoto() {
//        val current = sharedPreferences.getBoolean("settings:autoPhoto", false)
//
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:autoPhoto", !current)
//        edit.apply()
//    }
//
//    fun toggleTower() {
//        val current = sharedPreferences.getBoolean("settings:tower", false)
//
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:tower", !current)
//        edit.apply()
//    }
//
//    fun togglePrintingSlow() {
//        val current = sharedPreferences.getBoolean("settings:printingSlow", false)
//
//        val edit = sharedPreferences.edit()
//        edit.putBoolean("settings:printingSlow", !current)
//        edit.apply()
//    }
//
//    fun updateViews() {
//        val currentTower = if(sharedPreferences.getBoolean("settings:tower", false)) "ON" else "OFF"
//        val currentFlash = if(sharedPreferences.getBoolean("settings:flash", false)) "ON" else "OFF"
//        val currentAutoPhoto = if(sharedPreferences.getBoolean("settings:autoPhoto", false)) "ON" else "OFF"
//        val currentPrintSlow = if(sharedPreferences.getBoolean("settings:printingSlow", false)) "ON" else "OFF"
//
//
//        binding.btnFlash.text = "Flash: ${currentFlash}"
//        binding.btnTower.text = "Tower: ${currentTower}"
//        binding.btnAutoPhoto.text = "Auto Photo: ${currentAutoPhoto}"
//        binding.btnPrintSlow.text = "Printing Slow: ${currentPrintSlow}"
//    }
//}