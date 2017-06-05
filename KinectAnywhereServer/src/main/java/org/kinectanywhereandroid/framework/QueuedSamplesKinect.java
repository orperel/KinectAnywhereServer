package org.kinectanywhereandroid.framework;


import net.jcip.annotations.ThreadSafe;

import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Kinect sensor representation that keeps a queue of all arriving data samples from the client
 */
@ThreadSafe
public class QueuedSamplesKinect extends RemoteKinect {

    /** All data currently received for this kinect client */
    private Queue<List<Skeleton>> skeletonQueue;

    public QueuedSamplesKinect() {

        super();
        skeletonQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
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
    @Override
    public boolean isTrackingSkeletons() {

        return skeletonQueue != null &&
                !skeletonQueue.isEmpty() &&
                !skeletonQueue.peek().isEmpty();
    }

    /**
     * @return The timestamp of the next skeleton list in the head of the queue
     */
    @Override
    public long nextTimeStamp() {

        if (!isTrackingSkeletons())
            return INVALID_TIME;

        return skeletonQueue.peek().get(0).getTimestamp();
    }
}
