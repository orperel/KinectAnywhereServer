package org.kinectanywhereandroid.framework;

import org.kinectanywhereandroid.model.Skeleton;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteKinect {

    public static final double INVALID_TIME = -1.0f;

    /** All data currently received for this kinect client */
    private Queue<List<Skeleton>> skeletonQueue = new ConcurrentLinkedQueue<>();
    public long lastBeacon = System.currentTimeMillis();
    public boolean isON = true;

    private int framesSinceLastPoll;
    private long lastPollTime;

    public void enqueue(List<Skeleton> skels) {

        skeletonQueue.add(skels);
        framesSinceLastPoll++;
    }

    public List<Skeleton> peek() {

        return skeletonQueue.peek();
    }

    public List<Skeleton> poll() {

        return skeletonQueue.poll();
    }

    /**
     * @return True if the next entry in the kinect queue contains any skeletons
     */
    public boolean isTrackingSkeletons() {

        return skeletonQueue != null &&
                !skeletonQueue.isEmpty() &&
                !skeletonQueue.peek().isEmpty();
    }

    /**
     * @return The timestamp of the next skeleton list in the head of the queue
     */
    public double nextTimeStamp() {

        if (!isTrackingSkeletons())
            return INVALID_TIME;

        return skeletonQueue.peek().get(0).timestamp;
    }

    public int fps() {

        long delta = System.currentTimeMillis() - lastPollTime;
        int fps = Math.round((float)framesSinceLastPoll / ((float)delta / 1000.0f));

        framesSinceLastPoll = 0;
        lastPollTime = System.currentTimeMillis();

        return fps;
    }
}
