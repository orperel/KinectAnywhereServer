package org.kinectanywhereandroid.framework;

import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class RemoteKinect {

    public static final long INVALID_TIME = -1;

    public long lastBeacon = System.currentTimeMillis();
    public boolean isON = true;

    protected int framesSinceLastPoll;
    protected long lastPollTime;

    /**
     * Cache another set of samples from the sensor
     * @param skels
     */
    public abstract void enqueue(List<Skeleton> skels);

    /**
     * @return Next entry from the sensor is valid and has tracked any skeletons
     */
    public abstract boolean isTrackingSkeletons();

    /**
     * @return The timestamp of the next skeleton list sample
     */
    public abstract long nextTimeStamp();

    public int fps() {

        long delta = System.currentTimeMillis() - lastPollTime;
        int fps = Math.round((float)framesSinceLastPoll / ((float)delta / 1000.0f));

        framesSinceLastPoll = 0;
        lastPollTime = System.currentTimeMillis();

        return fps;
    }
}
