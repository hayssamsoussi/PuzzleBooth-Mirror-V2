package com.puzzlebooth.remote

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.puzzlebooth.server.databinding.ActivityRemoteBinding


class RemoteActivity : AppCompatActivity() {


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
    }

    open fun sendThroughDelay(action: String) {
        val slow = true
        if(slow) {
            Handler().postDelayed(Runnable {
                send(action)
            }, 1500)
        } else {
            send(action)
        }
    }

    fun send(msgText: String){
//        val intent = Intent(this, SendService::class.java)
//        intent.putExtra(SendService.CONTENT, msgText)
//        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
//        intent.putExtra(SendService.PORT, UdpPort)
//        startService(intent)
    }

}