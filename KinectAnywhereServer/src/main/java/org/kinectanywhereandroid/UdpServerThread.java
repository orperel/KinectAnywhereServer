package org.kinectanywhereandroid;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

// Good example: http://android-er.blogspot.co.il/2016/06/android-datagramudp-server-example.html

public class UdpServerThread extends Thread{
    private final static String TAG = "UDP_SERVER_THREAD";
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

    private void drawSkeletons(final List<Skeleton> skeletonList){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawSkeletons(skeletonList);
            }
        });
    }

    public List<Skeleton> parseSkeleton(final DatagramPacket packet) {
        float[] points = new float[3];
        byte[] point = new byte[4];

        int i = 0;

        List<Skeleton> skeletonList = new LinkedList<>();

        while (i < packet.getLength()) {
            Skeleton skeleton = new Skeleton();

            // Parse skeletons tracker id
            for (int k = 0; k < 4; k++) {   // Get current point from packet
                point[k] = packet.getData()[i + k];
            }
            skeleton.trackingId = ByteBuffer.wrap(point).order(ByteOrder.LITTLE_ENDIAN).getInt();

            i += 4;

            // Parse joints
            while (i < packet.getLength()) {
                Joint joint = new Joint();

                joint.type = Joint.JointType.values()[packet.getData()[i++]]; // Get current type from packet
                joint.trackingState = Joint.JointTrackingState.values()[packet.getData()[i++]]; // Get current type from packet

                for (int j = 0; j < 3; j++) { // For every point in joint (x,y,z):
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

                skeleton.joints[joint.type.getValue()] = joint;

                // Check for end of skeleton
                if (packet.getData()[i] == -1 && packet.getData()[i+1] == -1) {
                    i += 2;
                }
            }

            skeletonList.add(skeleton);
        }

        return skeletonList;
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
                socket.receive(packet);   //this code block the program flow

                List<Skeleton> skeletonList = parseSkeleton(packet);

                drawSkeletons(skeletonList);
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