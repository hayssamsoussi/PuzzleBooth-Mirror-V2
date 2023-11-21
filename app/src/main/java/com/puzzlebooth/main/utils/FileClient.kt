package com.puzzlebooth.main.utils

import android.os.AsyncTask
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.Socket

class FileClient(val ip: String, val port: Int) : AsyncTask<File, Int, Void>() {

    override fun doInBackground(vararg params: File): Void? {
        try {
            val file = params[0]
            val socket = Socket(ip, port) // Replace "your_server_ip" with the actual IP address of your server

            val outputStream = DataOutputStream(socket.getOutputStream())
            val fileInputStream = FileInputStream(file)
            val bufferedInputStream = BufferedInputStream(fileInputStream)

            // Send file name and size to the server
            outputStream.writeUTF(file.name)
            outputStream.writeLong(file.length())

            // Send file content
            val buffer = ByteArray(8192) // Same buffer size as the server
            var bytesRead: Int

            while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            bufferedInputStream.close()
            fileInputStream.close()
            outputStream.close()
            socket.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}