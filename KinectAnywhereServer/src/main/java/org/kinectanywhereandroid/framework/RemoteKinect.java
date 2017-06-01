package org.kinectanywhereandroid.framework;

import org.kinectanywhereandroid.model.Skeleton;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteKinect {

    public static final double INVALID_TIME = -1.0f;

    /** All data currently received for this kinect client */
    public Queue<List<Skeleton>> skeletonQueue = new ConcurrentLinkedQueue<>();
    public double lastBeacon = System.currentTimeMillis();
    public boolean isON = true;

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
}
