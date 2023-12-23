package com.puzzlebooth.server

import android.content.SharedPreferences
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.connection.Payload
import com.puzzlebooth.main.BaseNearbyActivity
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.utils.UdpBroadcastListener
import java.io.File

class MainActivity : BaseNearbyActivity() {

    lateinit var udpBroadcastListener: UdpBroadcastListener
    lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        var lastTimePrinterConnectionReceived: Long = System.currentTimeMillis() - (600000)
        var mosaic = false
    }

    fun sendStatus() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel =  bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val printCount = File(getCurrentEventPhotosPath()).listFiles()?.size
        send(Payload.fromBytes("battery:$batteryLevel%;print_count:$printCount".toByteArray()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPreferences = getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        udpBroadcastListener = UdpBroadcastListener(this, sharedPreferences, 11791)

        // Start listening
        udpBroadcastListener.startListening()

        preriodicallyCheckPrinterStatus()

        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        udpBroadcastListener.stopListening()
    }
    private fun preriodicallyCheckPrinterStatus() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val lastReceivedMsAgo = now - lastTimePrinterConnectionReceived
                togglePrinterDot(lastReceivedMsAgo < 10000)
                mainHandler.postDelayed(this, 4000)
            }
        })
    }


    override fun toggleRemoteDot(state: State) {
        if(state == State.CONNECTED) {
            findViewById<ImageView>(R.id.dotStatusRemote).setColorFilter(Color.parseColor("#0da002"), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            findViewById<ImageView>(R.id.dotStatusRemote).setColorFilter(Color.parseColor("#d40000"), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    override fun togglePrinterDot(isOnline: Boolean) {
        if(isOnline) {
            findViewById<ImageView>(R.id.dotStatusPrinter).setColorFilter(Color.parseColor("#0da002"), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            findViewById<ImageView>(R.id.dotStatusPrinter).setColorFilter(Color.parseColor("#d40000"), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }
}