package com.puzzlebooth.remote

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.nearby.connection.Payload
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.puzzlebooth.main.BaseNearbyActivity
import com.puzzlebooth.main.models.MosaicInfo
import com.puzzlebooth.main.models.ServerStatus
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.ActivityRemoteBinding
import kotlinx.serialization.json.Json

class RemoteActivity : BaseNearbyActivity() {

    var mosaicOn = false
    var lastMosaicUpdates: MosaicInfo? = null

    override fun onReceive(endpoint: Endpoint?, payload: Payload?) {
        val event = payload?.asBytes()?.let { String(it) }
        if (event != null) {
            processEvent(event)
        }
        println("hhh received ${event}")
    }

    fun processEvent(event: String) {
        if(event.contains("battery")) {
            val serverStatus = Json.decodeFromString<ServerStatus>(event)
            binding.header1.text = "Battery: ${serverStatus.battery}"
            binding.header2.text = "Prints: ${serverStatus.printCount}"
        } else if(event.contains("originals")) {
            val mosaicInfo = Json.decodeFromString<MosaicInfo>(event)
            lastMosaicUpdates = mosaicInfo
            mosaicOn = true
        }
    }

    override fun toggleRemoteDot(state: State) {
        when(state) {
            State.SEARCHING -> {
                binding.connectionStatusContainer.setCardBackgroundColor(Color.parseColor("#1E1E1E"))
                binding.progressBar.visibility = View.VISIBLE
            }

            State.CONNECTED -> {
                binding.connectionStatusContainer.setCardBackgroundColor(Color.parseColor("#0da002"))
                binding.progressBar.visibility = View.INVISIBLE
            }

            State.UNKNOWN -> {
                binding.connectionStatusContainer.setCardBackgroundColor(Color.parseColor("#d40000"))
                binding.progressBar.visibility = View.INVISIBLE
            }
        }
    }

    override fun togglePrinterDot(isOnline: Boolean) {
//        if(isOnline) {
//            findViewById<ImageView>(R.id.dotStatusPrinter).setColorFilter(Color.parseColor("#0da002"), android.graphics.PorterDuff.Mode.SRC_IN);
//        } else {
//            findViewById<ImageView>(R.id.dotStatusPrinter).setColorFilter(Color.parseColor("#d40000"), android.graphics.PorterDuff.Mode.SRC_IN);
//        }
    }

    private lateinit var binding: ActivityRemoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteBinding.inflate(layoutInflater)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)


        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(binding.navHostFragmentActivityMasterBottomNavigation.id) as NavHostFragment?
        val navController = navHostFragment!!.navController
       // val navController = findNavController(binding.navHostFragmentActivityMasterBottomNavigation.id)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        //NavigationUI.setupWithNavController(navView, navController);

        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        mPreviousStateView = findViewById<TextView>(R.id.previous_state)
        mCurrentStateView = findViewById<TextView>(R.id.current_state)

        mDebugLogView = findViewById<TextView>(R.id.debug_log)
        mDebugLogView?.visibility = if (DEBUG) View.VISIBLE else View.GONE
        mDebugLogView?.movementMethod = ScrollingMovementMethod()

        mName = "PBR"
        binding.header3.text = "Name: ${mName}"
        (findViewById<CardView>(R.id.header_container)).setOnClickListener {
            send("request_print_count")
        }

        findViewById<CardView>(R.id.connection_status_container).setOnClickListener {
            setState(State.SEARCHING)
        }

        (findViewById<TextView>(R.id.name))?.text = mName
    }

    open fun sendThroughDelay(action: String) {
        val slow = false
        if(slow) {
            Handler().postDelayed(Runnable {
                send(action)
            }, 1500)
        } else {
            send(action)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_master_bottom_navigation) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

        if (currentFragment is VolumeKeyListener) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return currentFragment.onVolumeKeyPressed(keyCode)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_master_bottom_navigation) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()

        if (currentFragment is VolumeKeyListener) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return currentFragment.onVolumeKeyReleased(keyCode)
            }
        }
        return super.onKeyUp(keyCode, event)
    }


    fun send(msgText: String){

        send(Payload.fromBytes(msgText.toByteArray()))
//        val intent = Intent(this, SendService::class.java)
//        intent.putExtra(SendService.CONTENT, msgText)
//        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
//        intent.putExtra(SendService.PORT, UdpPort)
//        startService(intent)
    }

}

interface VolumeKeyListener {
    fun onVolumeKeyPressed(keyCode: Int): Boolean
    fun onVolumeKeyReleased(keyCode: Int): Boolean
}