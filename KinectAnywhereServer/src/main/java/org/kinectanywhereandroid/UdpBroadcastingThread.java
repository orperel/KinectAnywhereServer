package org.kinectanywhereandroid;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class UdpBroadcastingThread extends Thread{
    private final static String TAG = "UDP_BROADCASTING_THREAD";
    public static final int TIME_TO_WAIT_BETWEEN_BROADCASTING_MILLIS = 2000;

    int broadcastingPort;
    DatagramSocket socket;
    boolean running;

    public UdpBroadcastingThread(int broadcastingPort) {
        super();
        this.broadcastingPort = broadcastingPort;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    @Override
    public void run() {

        running = true;

        try {
            InetAddress address = InetAddress.getByName(Utils.getBroadcastingAddress());
            byte[] sendData = "SERVER".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, broadcastingPort);

            while(running){
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.send(sendPacket);
                socket.close();

                Thread.sleep(TIME_TO_WAIT_BETWEEN_BROADCASTING_MILLIS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                Log.e(TAG, "socket.close()");
            }
        }
    }
}