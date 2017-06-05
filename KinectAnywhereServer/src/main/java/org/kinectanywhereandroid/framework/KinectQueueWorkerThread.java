package org.kinectanywhereandroid.framework;

import android.support.annotation.Nullable;
import android.util.Log;

import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;
import static org.kinectanywhereandroid.framework.SingleFrameData.SingleFrameDataBuilder;

/**
 * Processes kinect queues of frames and notifies listeners about new incoming data arriving
 * (e.g: calibrate, paint and so on)
 */
public class KinectQueueWorkerThread extends TimerTask implements IKinectDataConsumer {

    private final static String TAG = "QUEUE_WORKER_THREAD";

    /** Threshold of gap in milliseconds allowed between kinect camera snapshots to be considered the same frame.
     *  Assumption: The kinect cameras are synchronized in time as closely as possible
     */
    private final static int FRAME_THRESHOLD = 45;

    private Timer _timer;
    private List<WeakReference<IKinectFrameEventListener>> _listeners;
    private boolean _running;
    private long _lastTimerTick;

    public KinectQueueWorkerThread() {
        super();
        _listeners = new LinkedList<>();
        _running = false;
        _lastTimerTick = System.currentTimeMillis();
    }

    @Override
    public void register(IKinectFrameEventListener listener) {
        _listeners.add(new WeakReference<>(listener));
    }

    /**
     * @param kinectDict Data of remotely connected kinect clients
     * @return Next assembled kinect frame information from queried connected kinects,
     *         may return null if the next frame is not ready yet (or queue is filled with excessive frames
     *         which have to get removed)
     */
    @Nullable
    public SingleFrameData sampleKinectQueues(Map<String, RemoteKinect> kinectDict) {


        long maxDiff = 0;
        String mostEarlyHost = null;
        long minTimestamp = Long.MAX_VALUE;
        boolean allKinectsReady = true;
        SingleFrameDataBuilder frameBuilder = new SingleFrameDataBuilder();

        Log.i(TAG, "Begin frame");

        // Iterate all skeletons for all connected kinect cameras and compare maximum time differences
        // for latest recorded data
        for(Map.Entry<String, RemoteKinect> remoteKinectEntry: kinectDict.entrySet()) {

            String kinectHostname = remoteKinectEntry.getKey();
            QueuedSamplesKinect kinect = (QueuedSamplesKinect)remoteKinectEntry.getValue();

            if (kinect.isTrackingSkeletons()) {

                // All skeletons have the same timestamp for the same camera
                long kinect1Timestamp = kinect.nextTimeStamp();

                if (kinect1Timestamp < minTimestamp) {
                    minTimestamp = kinect1Timestamp;
                    mostEarlyHost = kinectHostname;
                }

                // Compare against all other cameras to check timestamp distance
                for(Map.Entry<String, RemoteKinect> comparedKinectEntry: kinectDict.entrySet()) {

                    String comparedHostname = comparedKinectEntry.getKey();
                    QueuedSamplesKinect comparedKinect = (QueuedSamplesKinect)comparedKinectEntry.getValue();

                    if (!kinectHostname.equals(comparedHostname) && comparedKinect.isTrackingSkeletons()) {
                        long kinect2Timestamp = comparedKinect.nextTimeStamp();
                        long diff = abs(kinect1Timestamp - kinect2Timestamp);
                        if (diff > maxDiff) {
                            maxDiff = diff; // Store maximum difference of time between 2 kinects
                        }
                    }
                }

                Log.i(TAG, kinectHostname + " tracked " + kinect.peek().size() + " skels");
                frameBuilder.addSkeletons(kinectHostname, kinect.poll()); // List a camera with skeletons
            }
            else {
                Log.i(TAG, kinectHostname + " tracked no skels");
                frameBuilder.addQuietHost(kinectHostname); // List a camera without skeletons

                allKinectsReady = false;
            }
        }

        if (mostEarlyHost != null) {
            // Second pass - build the actual frame
            // The frame is valid only if the kinect time signatures of latest data are not too far apart,
            // otherwise we discard this frame and throw away the oldest piece of info for one of the kinects
            if (maxDiff > FRAME_THRESHOLD) {
                //kinectDict.get(mostEarlyHost).poll(); // Discard

                Log.i(TAG, "WorkerThread bailed out due to " + mostEarlyHost + " being late at " + maxDiff);
                return null;
            }
        }
        else {
            Log.i(TAG, "No skeletons in any queue for this frame");
            return null;
        }

        long timeSinceLastUpdate = System.currentTimeMillis() - _lastTimerTick;
        if (!allKinectsReady && timeSinceLastUpdate < 0) {
            return null;
        }

        Log.i(TAG, "Prompting frame");

        frameBuilder.addTimestamp(System.currentTimeMillis());
        _lastTimerTick = System.currentTimeMillis();
        return frameBuilder.build();
    }

    @Override
    public void run() {

        try {
            if (_running){

                Map<String, RemoteKinect> kinectDict = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);
                SingleFrameData frame = sampleKinectQueues(kinectDict);

                if (frame == null)
                    return; // Invalid frame was discarded

                // Notify listeners (painter, calibration, etc)
                for (WeakReference<IKinectFrameEventListener> weakListener: _listeners) {
                    IKinectFrameEventListener listener = weakListener.get();
                    if (listener != null)
                        listener.handle(frame);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception have occurred in KinecQueueWorkerThread", e);
        }
    }

    @Override
    public void activate() {

        _running = true;

        // 30 fps
        long delay = 0;
        long period = 32;
        _timer = new Timer();
        _timer.scheduleAtFixedRate(this, delay, period);
    }

    @Override
    public void deactivate() {

        _running = false;
        _timer.cancel();
    }
}