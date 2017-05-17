package org.kinectanywhereandroid;

import android.util.Log;

import java.net.DatagramPacket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import Jama.Matrix;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class KinectQueueWorkerThread extends Thread{
    private final static String TAG = "KINECT_QUEUE_WORKER_THREAD";

    MainActivity mActivity;
    CalibrationAlgo mCalibrator;
    boolean running;

    public KinectQueueWorkerThread(MainActivity mActivity) {
        super();
        this.mActivity = mActivity;
        this.mCalibrator = new CalibrationAlgo();
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private Map<String, Skeleton> preCalibrate(final Map<String, List<Skeleton>> skeletons) {

        Map<String, Skeleton> camCandidates = new HashMap<>();

        for (Map.Entry<String, List<Skeleton>> nextCam: skeletons.entrySet()) {

            if (nextCam == null || nextCam.getValue().size() != 1)
                return null;
            else
                camCandidates.put(nextCam.getKey(), nextCam.getValue().get(0));
        }

        return camCandidates;
    }

    private void drawSkeletons(final Map<String, List<Skeleton>> skeletons){

        Map<String, Skeleton> cameras = preCalibrate(skeletons);

        if (cameras == null || cameras.size() <= 0)
            return; // Avoid crashes

        Iterator<Map.Entry<String, Skeleton>> camIter = cameras.entrySet().iterator();

        Map.Entry<String, Skeleton> cam0 = camIter.next();

        final Map<String, List<Skeleton>> transformedSkels = new HashMap<>();
        transformedSkels.put(cam0.getKey(), Collections.singletonList(cam0.getValue()));

        if (!camIter.hasNext())
            return;

        Map.Entry<String, Skeleton> nextCam = null;

        do {
            nextCam = camIter.next();

            Matrix transformation = mCalibrator.calibrate(cam0.getValue(), nextCam.getValue());
            Skeleton transformedSkel = mCalibrator.transform(nextCam.getValue(), transformation);
            transformedSkels.put(nextCam.getKey(), Collections.singletonList(transformedSkel));

        }  while (camIter.hasNext());

        mActivity.runOnUiThread( new Runnable() {

            @Override
            public void run() {
                mActivity.drawSkeletons(transformedSkels);
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
                                // TODO: that's wrong... :(
                                coverAllHosts = false;
                            }
                        }

                        if (coverAllHosts || (abs(System.currentTimeMillis() - globalLastCurrentTimestamp) > 50)) {
                            try {
                                drawSkeletons(skeletons);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            skeletons = new HashMap<>();
                            coverAllHosts = true;
                            globalLastCurrentTimestamp = System.currentTimeMillis();
                        } else {
                            Log.d("bla", "bla");
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
            System.out.println(e);
        } finally {

        }
    }
}