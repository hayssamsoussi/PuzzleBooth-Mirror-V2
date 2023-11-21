package com.puzzlebooth.server

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewAnimationUtils
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.navigation.findNavController
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import com.puzzlebooth.server.base.MessageEvent
import com.puzzlebooth.server.utils.ConnectionsActivity
import com.puzzlebooth.server.utils.UdpBroadcastListener
import org.greenrobot.eventbus.EventBus
import java.util.Random

class MainActivity : BaseNearbyActivity() {


//    /** Listens to holding/releasing the volume rocker.  */
//    private val mGestureDetector: GestureDetector =
//        object : GestureDetector(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP) {
//            protected fun onHold() {
//                logV("onHold")
//                startRecording()
//            }
//
//            protected fun onRelease() {
//                logV("onRelease")
//                stopRecording()
//            }
//        }
//
//    /** For recording audio as the user speaks.  */
//    private val mRecorder: AudioRecorder? = null
//
//    /** For playing audio from other users nearby.  */
//    private val mAudioPlayer: AudioPlayer? = null

    //private val mOriginalVolume = 0
    
    lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        var lastTimePrinterConnectionReceived: Long = System.currentTimeMillis() - (600000)
        var mosaic = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        val udpListener = UdpBroadcastListener(this, sharedPreferences, 11791)

        // Start listening
        udpListener.startListening()

        preriodicallyCheckPrinterStatus()

        setContentView(R.layout.activity_main)
    }

    private fun preriodicallyCheckPrinterStatus() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                //sendUdpBroadcast("this is the app here", 11791)
                val now = System.currentTimeMillis()
                val lastReceivedMsAgo = now - MainActivity.lastTimePrinterConnectionReceived
                togglePrinterDot(lastReceivedMsAgo < 10000)
                mainHandler.postDelayed(this, 4000)
            }
        })
    }







    override fun toggleRemoteDot(isOnline: Boolean) {
        if(isOnline) {
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