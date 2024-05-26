package com.puzzlebooth.server

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.gms.nearby.connection.Payload
import com.puzzlebooth.main.BaseNearbyActivity
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.models.ServerStatus
import com.puzzlebooth.main.qr_code.QRCodeFragment
import com.puzzlebooth.main.utils.getCurrentEventPhotosPath
import com.puzzlebooth.server.mosaic.MosaicManager
import com.puzzlebooth.server.utils.UdpBroadcastListener
import io.paperdb.Paper
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus
import java.io.File

class MainActivity : BaseNearbyActivity() {

    //var udpBroadcastListener: UdpBroadcastListener? = null
    var sharedPreferences: SharedPreferences? = null
    var showQRFragment: QRCodeFragment? = null

    companion object {
        var lastTimePrinterConnectionReceived: Long = System.currentTimeMillis() - (600000)
        var mosaic = false
    }

    fun sendStatus() {
        Handler().postDelayed(Runnable {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batteryLevel =  bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val printCount = File(getCurrentEventPhotosPath()).listFiles()?.size
            val serverStatus = ServerStatus(
                batteryLevel.toString(), printCount.toString(), MosaicManager.isRunning()
            )

            send(Payload.fromBytes(Json.encodeToString(serverStatus).toByteArray()))
        }, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Paper.init(this);

        mName = "PBS"

        sharedPreferences = getSharedPreferences("MySharedPref", AppCompatActivity.MODE_PRIVATE)
        val isLandscape = false

        if((isLandscape == true) && requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            MosaicManager.startMosaic(this) {
                //sendMosaicStatus()
            }

            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

            println("hhh udpBroadcastListener init")
            //com.puzzlebooth.main.UdpBroadcastListener.start(this, sharedPreferences!!)
            //udpBroadcastListener = UdpBroadcastListener(this, sharedPreferences!!, 11791)

            // Start listening
            //udpBroadcastListener?.startListening()

            preriodicallyCheckPrinterStatus()

            setContentView(R.layout.activity_main)

            val landscape = sharedPreferences?.getBoolean("settings:landscape", false)
            findViewById<ImageView>(R.id.dotStatusPrinter).visibility = if(landscape == true) View.VISIBLE else View.GONE
        }
    }

    fun showQRCode(text: String) {
        runOnUiThread {
            showQRFragment = QRCodeFragment.newInstance(text)
            showQRFragment?.show(supportFragmentManager, "")
//            val bitmap = net.glxn.qrgen.android.QRCode.from(text).withSize(200,200).bitmap()
//            alertDialogBuilder = AlertDialog.Builder(this)
//            val factory: LayoutInflater = LayoutInflater.from(this)
//            val view: View = factory.inflate(R.layout.alert_dialog_image, null)
//            view.findViewById<ImageView>(R.id.dialog_imageview).setImageBitmap(bitmap)
//            alertDialogBuilder?.setView(view)
//            alertDialogBuilder?.show()
        }
    }

    fun sendMosaicStatus() {
        val mosaicInfo = MosaicManager.getMosaicInfo()
        val decoded = Json.encodeToString(mosaicInfo)
        println("hhh send mosaic status")
        send(Payload.fromBytes(decoded.toByteArray()))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check for volume button press
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            println("hhh volumne iup pressed")
            // Notify the fragment about the volume button press
            EventBus.getDefault().post(MessageEvent("start2"))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        //println("hhh udpBroadcastListener.stopListening()")
        //com.puzzlebooth.main.UdpBroadcastListener.stop()
    }

    override fun onReceive(endpoint: Endpoint?, payload: Payload?) {
        super.onReceive(endpoint, payload)
        val event = payload?.asBytes()?.let { String(it) }
        when {
            event.equals("cancel") -> {
                showQRFragment?.dismissAllowingStateLoss()
            }
            event.equals("sendToPrint") -> {
                MosaicManager.moveToPrintsToMerge(this)
            }
        }
        if(event.equals("cancel")) {
            showQRFragment?.dismissAllowingStateLoss()
        }
    }

    override fun onEndpointConnected(endpoint: Endpoint?) {
        super.onEndpointConnected(endpoint)
        findViewById<TextView>(R.id.nameTv).text = endpoint?.name ?: "N/A"
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
        val landscape = sharedPreferences?.getBoolean("settings:landscape", false)

        if(state == State.CONNECTED) {
            findViewById<ImageView>(R.id.dotStatusRemote)?.setColorFilter(Color.parseColor("#0da002"), android.graphics.PorterDuff.Mode.SRC_IN)
            findViewById<LinearLayout>(R.id.dotStatusRemoteContainer)?.alpha = 0.3F
        } else {
            findViewById<ImageView>(R.id.dotStatusRemote)?.setColorFilter(Color.parseColor("#d40000"), android.graphics.PorterDuff.Mode.SRC_IN)
            findViewById<LinearLayout>(R.id.dotStatusRemoteContainer)?.alpha = 1F
        }
    }

    override fun togglePrinterDot(isOnline: Boolean) {

        if(isOnline) {
            findViewById<ImageView>(R.id.dotStatusPrinter)?.setColorFilter(Color.parseColor("#0da002"), android.graphics.PorterDuff.Mode.SRC_IN)
            findViewById<ImageView>(R.id.dotStatusPrinter)?.alpha = 0.5F
        } else {
            findViewById<ImageView>(R.id.dotStatusPrinter)?.setColorFilter(Color.parseColor("#d40000"), android.graphics.PorterDuff.Mode.SRC_IN)
            findViewById<ImageView>(R.id.dotStatusPrinter)?.alpha = 1F
        }
    }
}