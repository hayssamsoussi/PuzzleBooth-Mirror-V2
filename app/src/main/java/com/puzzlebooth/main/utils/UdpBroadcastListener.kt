package com.puzzlebooth.main.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

class UdpBroadcastListener (val context: Context, private val sharedPreferences: SharedPreferences, private val port: Int) {

    private var isListening = false
    private val socket: DatagramSocket = DatagramSocket(port)

    fun startListening() {
        if (!isListening) {
            isListening = true
            GlobalScope.launch(Dispatchers.IO) {
                println("hhh UDP Broadcast Listener started")
                listenForMessages()
            }
        }
    }

    fun stopListening() {
        isListening = false
        socket.close()
        println("hhh UDP Broadcast Listener closed")
    }

    private suspend fun listenForMessages() {
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)

        while (isListening) {
            socket.receive(packet)

            val message = String(buffer, 0, packet.length)
            val senderAddress = packet.address
            val senderPort = packet.port

            // Handle the received message as needed
            // Sender
            //val intent = Intent("custom-event")
            //intent.putExtra("message", "testb")
            //LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            if(message.startsWith("i_am_the_printer_on_", true)) {
                try {
                    val jsonIpPort = message.lowercase().removePrefix("i_am_the_printer_on_")
                    val jsonObject = JSONObject(jsonIpPort)
                    val receivedIP = jsonObject.optString("ip")
                    val receivedPort = jsonObject.optString("port")
                    val edit = sharedPreferences.edit()
                    edit.putString("ip", receivedIP)
                    edit.putString("port", receivedPort)
                    edit.apply()
                    //sendToTarget("ip:${sharedPreferences.getString("ip", "")},port:${sharedPreferences.getString("port", "")}", ServerService.lastReceiverAddress)
                    //send("ip:${receivedIP.toString()},port:${receivedPort.toString()}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    //ip = ""
                    //port = ""
                }
            }
            //MainActivity.lastTimePrinterConnectionReceived = System.currentTimeMillis()
            println("Received message from $senderAddress:$senderPort: $message")
        }
    }
}