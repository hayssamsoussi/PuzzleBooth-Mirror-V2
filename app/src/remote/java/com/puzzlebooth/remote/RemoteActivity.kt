package com.puzzlebooth.remote

import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.nearby.connection.Payload
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.puzzlebooth.main.BaseNearbyActivity
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.ActivityRemoteBinding

class RemoteActivity : BaseNearbyActivity() {


    override fun togglePrinterDot(isOnline: Boolean) {
        super.togglePrinterDot(isOnline)
        println("hhh togglePrinterDot $isOnline")
    }

    override fun toggleRemoteDot(isOnline: Boolean) {
        super.toggleRemoteDot(isOnline)
        println("hhh toggleRemoteDot $isOnline")
    }

    private lateinit var binding: ActivityRemoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteBinding.inflate(layoutInflater)

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

        mName = generateRandomName()

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

    fun send(msgText: String){

        send(Payload.fromBytes(msgText.toByteArray()))
//        val intent = Intent(this, SendService::class.java)
//        intent.putExtra(SendService.CONTENT, msgText)
//        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
//        intent.putExtra(SendService.PORT, UdpPort)
//        startService(intent)
    }

}