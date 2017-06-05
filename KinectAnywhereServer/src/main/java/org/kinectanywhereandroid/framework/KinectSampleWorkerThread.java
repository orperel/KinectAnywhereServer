package org.kinectanywhereandroid.framework;

import android.support.annotation.Nullable;
import android.util.Log;

import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.util.Pair;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;
import static org.kinectanywhereandroid.framework.SingleFrameData.SingleFrameDataBuilder;

/**
 * Samples kinect queues of frames and notifies listeners about latest incoming data arriving
 * (e.g: calibrate, paint and so on)
 */
public class KinectSampleWorkerThread extends TimerTask implements IKinectDataConsumer {

    private final static String TAG = "SAMPLE_WORKER_THREAD";

    /** Threshold of gap in milliseconds allowed between kinect camera snapshots to be considered the same frame.
     *  If a camera falls more than FRAME_THRESHOLD milliseconds behind, it's sample is dropped for the current frame.
     *  Assumption: The kinect cameras are synchronized in time as closely as possible
     */
    private final static int FRAME_THRESHOLD = 300;

    private Timer _timer;
    private List<WeakReference<IKinectFrameEventListener>> _listeners;
    private boolean _running;

    public KinectSampleWorkerThread() {
        super();
        _listeners = new LinkedList<>();
        _running = false;
    }

    @Override
    public void register(IKinectFrameEventListener listener) {
        _listeners.add(new WeakReference<>(listener));
    }

    /**
     * Collect samples from all active kinects since the last sample might change during the processing
     * of this frame. We don't lock the RemoteKinect here with a synchronization block, so one kinect
     * may be updated while the previous kinect is still being sampled.
     * This is ok since we assume these samples should be close enough to each other and the collection
     * time is fast.
     * @param kinectDict
     * @return "Frozen" state of samples from currently active kinect.
     */
    private Map<String, Pair<Long, List<Skeleton>>> collectSamples(Map<String, RemoteKinect> kinectDict) {

        Map<String, Pair<Long, List<Skeleton>>> samples = new HashMap<>();

        for(Map.Entry<String, RemoteKinect> remoteKinectEntry: kinectDict.entrySet()) {

            SingleSampleKinect kinect = (SingleSampleKinect)remoteKinectEntry.getValue();
            samples.put(remoteKinectEntry.getKey(), kinect.sample());
        }

        return samples;
    }

    private long getMostUpdatedTime(Map<String, Pair<Long, List<Skeleton>>> samples) {

        long mostUpdatedTime = 0;

        for(Map.Entry<String, Pair<Long, List<Skeleton>>> sampleEntry: samples.entrySet()) {

            long sampleTime = sampleEntry.getValue().first;

            // Ignore untracked skeletons without sampling time
            if (sampleTime == RemoteKinect.INVALID_TIME)
                continue;

            if (sampleTime > mostUpdatedTime)
                mostUpdatedTime = sampleTime;
        }

        return mostUpdatedTime;
    }

    private boolean isTrackingSkeletons(List<Skeleton> skels) {

        return skels != null && !skels.isEmpty();
    }

    /**
     * @param kinectDict Data of remotely connected kinect clients
     * @return Next assembled kinect frame information from queried connected kinects,
     *         may return null if the next frame is not ready yet.
     */
    @Nullable
    public SingleFrameData sampleKinectQueues(Map<String, RemoteKinect> kinectDict) {

        Map<String, Pair<Long, List<Skeleton>>> samples = collectSamples(kinectDict);
        long mostUpdatedTime = getMostUpdatedTime(samples);
        SingleFrameDataBuilder frameBuilder = new SingleFrameDataBuilder();

        // Iterate all skeletons for all connected kinect cameras and drop sensor data that is too old
        for(Map.Entry<String, Pair<Long, List<Skeleton>>> sampleEntry: samples.entrySet()) {

            String kinectHostname = sampleEntry.getKey();
            long sampleTime = sampleEntry.getValue().first;
            List<Skeleton> sampleData = sampleEntry.getValue().second;

            if ((isTrackingSkeletons(sampleData) && (mostUpdatedTime - sampleTime < FRAME_THRESHOLD))) {

                frameBuilder.addSkeletons(kinectHostname, sampleData); // List a camera with skeletons
            }
            else {
                Log.i(TAG, kinectHostname + " tracked no skels");
                frameBuilder.addQuietHost(kinectHostname); // List a camera without skeletons
            }
        }

        frameBuilder.addTimestamp(System.currentTimeMillis());
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