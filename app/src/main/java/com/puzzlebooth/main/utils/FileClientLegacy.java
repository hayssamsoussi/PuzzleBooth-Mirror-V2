package com.puzzlebooth.main.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileClientLegacy {

    private Socket s;

    public FileClientLegacy(String host, int port, String file) {
        try {
            s = new Socket(host, port);
            sendFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileClientLegacy(String host, int port, String file, Boolean isQrCode) {
        try {
            s = new Socket(host, port);
            sendQRorder(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendQRorder(String file) throws IOException {
        //Send file
        File myFile = new File(file);

        OutputStream os = s.getOutputStream();

        DataOutputStream dos = new DataOutputStream(os);
        dos.writeUTF("QR_"+myFile.getName());
        dos.flush();

        s.close();
    }

    public void sendFile(String file) throws IOException {
        //Send file
        File myFile = new File(file);
        byte[] mybytearray = new byte[(int) myFile.length()];

        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        //bis.read(mybytearray, 0, mybytearray.length);

        DataInputStream dis = new DataInputStream(bis);
        dis.readFully(mybytearray, 0, mybytearray.length);

        OutputStream os = s.getOutputStream();

        //Sending file name and file size to the server
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeUTF(myFile.getName());
        dos.writeLong(mybytearray.length);
        dos.write(mybytearray, 0, mybytearray.length);
        dos.flush();

        //Sending file data to the server
        os.write(mybytearray, 0, mybytearray.length);
        os.flush();

        //Closing socket
        s.close();

//        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
//        FileInputStream fis = new FileInputStream(file);
//        byte[] buffer = new byte[4096];
//
//        while (fis.read(buffer) > 0) {
//            dos.write(buffer);
//        }
//
//        fis.close();
//        dos.close();
    }

//    public static void main(String[] args) {
//        FileClient fc = new FileClient("localhost", 1988, "artbetbanner.png");
//    }
}
