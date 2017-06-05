package org.kinectanywhereandroid.framework;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kinect sensor representation that keeps only the last sample to arrive at the server
 */
@ThreadSafe
public class SingleSampleKinect extends RemoteKinect {

    private AtomicReference<List<Skeleton>> latestSample;

    public SingleSampleKinect() {

        super();
        latestSample = new AtomicReference<>();
    }

    /**
     * @return Sample the last sample that arrived and the time it arrived in
     */
    public Pair<Long, List<Skeleton>> sample() {

        synchronized (this) {
            return new Pair<>(nextTimeStamp(), latestSample.get());
        }
    }

    /**
     * Keep latest sample given.
     * This method locks using the current object as a lock.
     * @param skels
     */
    @Override
    public void enqueue(List<Skeleton> skels) {

        synchronized (this) {
            latestSample.set(skels);
            framesSinceLastPoll++;
        }
    }

    /**
     * @return True if the latest sensor sample contains any skeletons
     */
    @Override
    public boolean isTrackingSkeletons() {

        return latestSample != null && !latestSample.get().isEmpty();
    }

    /**
     * @return The timestamp of the last sample if it tracked any skeletons
     */
    @Override
    public long nextTimeStamp() {

        if (!isTrackingSkeletons())
            return INVALID_TIME;

        return latestSample.get().get(0).getTimestamp();
    }
}
