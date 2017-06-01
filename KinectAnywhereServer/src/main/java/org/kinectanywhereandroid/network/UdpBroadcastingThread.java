package org.kinectanywhereandroid.network;

import android.util.Log;

import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class UdpBroadcastingThread extends Thread{
    private final static String TAG = "UDP_BROADCASTING_THREAD";
    public static final int TIME_TO_WAIT_BETWEEN_BROADCASTING_MILLIS = 2000;

    Queue<String> _messageQueue;
    int broadcastingPort;
    DatagramSocket socket;
    boolean running;

    public UdpBroadcastingThread(int broadcastingPort) {
        super();
        this.broadcastingPort = broadcastingPort;

        _messageQueue = new ConcurrentLinkedQueue<>();
        DataHolder.INSTANCE.save(DataHolderEntry.BROADCASTING_QUEUE, _messageQueue);
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    @Override
    public void run() {

        running = true;

        try {
            InetAddress address = InetAddress.getByName(Utils.getBroadcastingAddress());

            while(running){
                String msg;
                if (_messageQueue.isEmpty()) {
                    msg = "SERVER";
                } else {
                    msg = _messageQueue.poll();
                }

                byte[] sendData = msg.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, broadcastingPort);

                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.send(sendPacket);
                socket.close();

                Thread.sleep(TIME_TO_WAIT_BETWEEN_BROADCASTING_MILLIS);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            if(socket != null){
                socket.close();
                Log.e(TAG, "socket.close()");
            }
        }
    }
}