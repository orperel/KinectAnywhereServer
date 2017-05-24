package org.kinectanywhereandroid.framework;

import org.kinectanywhereandroid.model.Skeleton;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RemoteKinect implements Serializable {

    /** All data currently received for this kinect client */
    public Queue<List<Skeleton>> skeletonQueue = new LinkedList<>();
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
            return -1.0;

        return skeletonQueue.peek().get(0).timestamp;
    }
}
