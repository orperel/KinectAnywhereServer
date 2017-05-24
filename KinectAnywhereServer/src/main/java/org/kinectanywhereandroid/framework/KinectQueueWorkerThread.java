package org.kinectanywhereandroid.framework;

import org.kinectanywhereandroid.MainActivity;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;
import static org.kinectanywhereandroid.framework.SingleFrameData.SingleFrameDataBuilder;

/**
 * Samples kinect queues of frames and notifies listeners about new incoming data arriving
 * (e.g: calibrate, paint and so on)
 */
public class KinectQueueWorkerThread extends Thread implements IKinectQueueConsumer {

    private final static String TAG = "KINECT_QUEUE_WORKER_THREAD";

    /** Threshold of gap in milliseconds allowed between kinect camera snapshots to be considered the same frame.
     *  Assumption: The kinect cameras are synchronized in time as closely as possible
     */
    private final static int FRAME_THRESHOLD = 40;

    MainActivity mActivity;
    List<WeakReference<IKinectFrameEventListener>> _listeners;
    boolean running;

    public KinectQueueWorkerThread(MainActivity mActivity) {
        super();
        this.mActivity = mActivity;
        _listeners = new LinkedList<>();
    }

    public void setRunning(boolean running){

        this.running = running;
    }

    @Override
    public void register(IKinectFrameEventListener listener) {
        _listeners.add(new WeakReference<>(listener));
    }

    /**
     * @param kinectDict Data of remotely connected kinect clients
     * @return Next assembled kinect frame information from queried connected kinects
     */
    public SingleFrameData sampleKinectQueues(Map<String, RemoteKinect> kinectDict) {

        double maxDiff = 0;
        String mostEarlyHost = "";
        double minTimestamp = Double.MAX_VALUE;

        // Iterate all skeletons for all connected kinect cameras and compare maximum time differences
        // for latest recorded data
        for(Map.Entry<String, RemoteKinect> remoteKinectEntry: kinectDict.entrySet()) {

            String kinectHostname = remoteKinectEntry.getKey();
            RemoteKinect kinect = remoteKinectEntry.getValue();

            if (kinect.isTrackingSkeletons()) {

                // All skeletons have the same timestamp for the same camera
                double kinect1Timestamp = kinect.nextTimeStamp();

                if (kinect1Timestamp < minTimestamp) {
                    minTimestamp = kinect1Timestamp;
                    mostEarlyHost = kinectHostname;
                }

                // Compare against all other cameras to check timestamp distance
                for(Map.Entry<String, RemoteKinect> comparedKinectEntry: kinectDict.entrySet()) {

                    String comparedHostname = comparedKinectEntry.getKey();
                    RemoteKinect comparedKinect = comparedKinectEntry.getValue();

                    if (!kinectHostname.equals(comparedHostname) && comparedKinect.isTrackingSkeletons()) {
                        double kinect2Timestamp = comparedKinect.nextTimeStamp();
                        double diff = abs(kinect1Timestamp - kinect2Timestamp);
                        if (diff > maxDiff) {
                            maxDiff = diff; // Store maximum difference of time between 2 kinects
                        }
                    }
                }
            }
        }

        SingleFrameData frame = null;

        // Second pass - build the actual frame
        // The frame is valid only if the kinect time signatures of latest data are not too far apart,
        // otherwise we discard this frame and throw away the oldest piece of info for one of the kinects
        if (maxDiff > FRAME_THRESHOLD) {
            kinectDict.get(mostEarlyHost).skeletonQueue.poll(); // Discard
        }
        else {
            SingleFrameDataBuilder frameBuilder = new SingleFrameDataBuilder();

            for(Map.Entry<String, RemoteKinect> remotekinectEntry: kinectDict.entrySet()) {

                String kinectHostname = remotekinectEntry.getKey();
                RemoteKinect kinect = remotekinectEntry.getValue();

                if (kinect.isTrackingSkeletons())
                    frameBuilder.addQuietHost(kinectHostname); // List a camera without skeletons
                else
                    frameBuilder.addSkeletons(kinectHostname, kinect.skeletonQueue.poll()); // List a camera without skeletons
            }

            frameBuilder.addTimestamp(System.currentTimeMillis());
            frame = frameBuilder.build();
        }

        return frame;
    }

    @Override
    public void run() {
        running = true;

        try {
            while(running){

                Map<String, RemoteKinect> kinectDict = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);
                SingleFrameData frame = sampleKinectQueues(kinectDict);

                if (frame == null)
                    continue; // Invalid frame was discarded

                // Notify listeners (painter, calibration, etc)
                for (WeakReference<IKinectFrameEventListener> weakListener: _listeners) {
                    IKinectFrameEventListener listener = weakListener.get();
                    if (listener != null)
                        listener.handle(frame);
                }

                sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void activate() {

        start();
    }
}