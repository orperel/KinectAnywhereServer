package org.kinectanywhereandroid;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;


public class UdpServerThread extends Thread{
    private final static String TAG = "bla";
    int serverPort;
    MainActivity mActivity;
    DatagramSocket socket;

    boolean running;

    public UdpServerThread(int serverPort, MainActivity mActivity) {
        super();
        this.serverPort = serverPort;
        this.mActivity = mActivity;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void updateState(final String state){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.updateState(state);
            }
        });
    }


    private void updatePrompt(final String prompt){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.updatePrompt(prompt);
            }
        });
    }

    private void drawSkeleton(final Skeleton skel){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawSkeleton(skel);
            }
        });
    }

    @Override
    public void run() {

        running = true;

        try {
            updateState("Starting UDP Server");
            socket = new DatagramSocket(serverPort);

            updateState("UDP Server is running");
            Log.e(TAG, "UDP Server is running");

            while(running){
                byte[] buf = new byte[500];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);     //this code block the program flow

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                updatePrompt("Request from: " + address + ":" + port + "\n");

                Skeleton skel = new Skeleton();

                int i = 4;


                float[] points = new float[3];
                byte[] point = new byte[4];

                while (i < packet.getLength()) {
                    Joint joint = new Joint();

                    joint.type = Joint.JointType.values()[packet.getData()[i++]]; // Get current type from packet
                    joint.trackingState = Joint.JointTrackingState.values()[packet.getData()[i++]]; // Get current type from packet

                    for (int j = 0 ; j < 3; j++) { // For every point in joint (x,y,z):
                        for (int k = 0; k < 4; k++) {   // Get current point from packet
                            point[k] = packet.getData()[i + k];
                        }

                        points[j] = ByteBuffer.wrap(point).order(ByteOrder.LITTLE_ENDIAN).getFloat(); // Convert current point to float
                        i += 4;
                    }

                    // Create joint from points
                    joint.x = points[0];
                    joint.y = points[1];
                    joint.z = points[2];

                    skel.joints[joint.type.getValue()] = joint;
                }

                drawSkeleton(skel);


                Log.e(TAG, Float.toString(skel.joints[0].x));

//                    String dString = new Date().toString() + "\n"
//                            + "Your address " + address.toString() + ":" + String.valueOf(port);
//                    buf = dString.getBytes();
//                    packet = new DatagramPacket(buf, buf.length, address, port);
//                    socket.send(packet);

            }

            Log.e(TAG, "UDP Server ended");

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                Log.e(TAG, "socket.close()");
            }
        }
    }
}