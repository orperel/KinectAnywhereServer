package org.kinectanywhereandroid;

import java.net.DatagramPacket;
import java.util.List;

public class KinectQueueWorkerThread extends Thread{
    private final static String TAG = "KINECT_QUEUE_WORKER_THREAD";

    MainActivity mActivity;
    boolean running;

    public KinectQueueWorkerThread(MainActivity mActivity) {
        super();
        this.mActivity = mActivity;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void drawSkeletons(final List<Skeleton> skeletonList){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawSkeletons(skeletonList);
            }
        });
    }

    @Override
    public void run() {
        running = true;

        try {
            while(running){
                if (!mActivity.kinectDict.get("bla").isEmpty()) {
                    drawSkeletons(mActivity.kinectDict.get("bla").poll());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}