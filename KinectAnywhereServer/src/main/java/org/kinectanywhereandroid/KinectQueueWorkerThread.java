package org.kinectanywhereandroid;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static java.lang.Math.abs;
import static java.lang.Math.min;

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

    private void drawSkeletons(final Map<String, List<Skeleton>> skeletons){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawSkeletons(skeletons);
            }
        });
    }

    @Override
    public void run() {
        running = true;

        Map<String, List<Skeleton>> skeletons = new HashMap<>();
        boolean coverAllHosts = true;
        double globalLastCurrentTimestamp = System.currentTimeMillis();

        try {
            while(running){
                double maxDiff = 0;
                String mostEarlyHost = "";
                double minTimestamp = Double.MAX_VALUE;

                Iterator<Map.Entry<String, Queue<List<Skeleton>>>> it = mActivity.kinectDict.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Queue<List<Skeleton>>> pair1 = it.next();
                    if (!mActivity.kinectDict.get(pair1.getKey()).isEmpty()) {
                        double kinect1Timestamp = mActivity.kinectDict.get(pair1.getKey()).peek().get(0).timestamp;

                        if (kinect1Timestamp < minTimestamp) {
                            minTimestamp = kinect1Timestamp;
                            mostEarlyHost = pair1.getKey();
                        }

                        Iterator<Map.Entry<String, Queue<List<Skeleton>>>> it2 = it;
                        while (it2.hasNext()) {
                            Map.Entry<String, Queue<List<Skeleton>>> pair2 = it2.next();

                            if (pair1.getKey() != pair2.getKey() && !mActivity.kinectDict.get(pair2.getKey()).isEmpty()) {
                                double kinect2Timestamp = mActivity.kinectDict.get(pair2.getKey()).peek().get(0).timestamp;
                                double diff = abs(kinect1Timestamp - kinect2Timestamp);
                                if (diff > maxDiff) {
                                    maxDiff = diff;
                                }
                            }
                        }
                    }
                }

                if (!mostEarlyHost.equals("")) {
                    if (maxDiff <= 40) {
                        Iterator<Map.Entry<String, Queue<List<Skeleton>>>> it3 = mActivity.kinectDict.entrySet().iterator();
                        while (it3.hasNext()) {
                            Map.Entry<String, Queue<List<Skeleton>>> pair = it3.next();
                            if (!mActivity.kinectDict.get(pair.getKey()).isEmpty()) {
                                skeletons.put(pair.getKey(), pair.getValue().poll());
                            } else {
                                coverAllHosts = false;
                            }
                        }

                        if (coverAllHosts || (abs(System.currentTimeMillis() - globalLastCurrentTimestamp) > 50)) {
                            drawSkeletons(skeletons);
                            skeletons = new HashMap<>();
                            globalLastCurrentTimestamp = Double.MAX_VALUE;
                            coverAllHosts = true;
                            globalLastCurrentTimestamp = System.currentTimeMillis();
                        }
                    } else {
                        mActivity.kinectDict.get(mostEarlyHost).poll();
                    }
                }


//                for (Map.Entry<String, Queue<List<Skeleton>>> entry : mActivity.kinectDict.entrySet())
//                {
//                    if (!mActivity.kinectDict.get(entry.getKey()).isEmpty()) {
//                        double kinect1Timestamp = mActivity.kinectDict.get(entry.getKey()).peek().get(0).timestamp;;
//                        if (maxDiff == 0) {
//                            maxDiff = kinect1Timestamp;
//                        } else {
//                            for (Map.Entry<String, Queue<List<Skeleton>>> entry2 : mActivity.kinectDict.entrySet())
//                            {
//                                if (entry.getKey() != entry.getKey() && !mActivity.kinectDict.get(entry2.getKey()).isEmpty()) {
//                                    double kinect2Timestamp = mActivity.kinectDict.get(entry2.getKey()).peek().get(0).timestamp;;
//                                    double diff = abs(kinect1Timestamp - kinect2Timestamp);
//                                }
//                            }
//                        }
//                    }
//                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}