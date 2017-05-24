package org.kinectanywhereandroid.framework;

import android.support.annotation.Nullable;

import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.Pair;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Single frame data of skeletons from all cameras
 */
public class SingleFrameData implements Iterable<Pair<String, Skeleton>>, Serializable {

    private static final long serialVersionUID = 1L;

    private final static double UNINITIALIZED = Double.MIN_VALUE;

    private Map<String, List<Skeleton>> _skeletons;
    private double _timestamp;
    private double _prevFrameTimestamp;

    private SingleFrameData() {
        // Allow creation only via SingleFrameDataBuilder
        _skeletons = new HashMap<>();
        _timestamp = UNINITIALIZED;
        _prevFrameTimestamp = UNINITIALIZED;
    }

    /**
     * Get all skeletons the camera detected
     * @param cameraName Unique camera name
     * @return List of skels
     */
    public List<Skeleton> getSkeletons(String cameraName) {

        return _skeletons.get(cameraName);
    }

    /**
     * Returns the single skeleton the camera has
     * @param cameraName Camera host name
     * @return The single skeleton of the camera or NULL if there is no skeleton or more than 1
     */
    public Skeleton getSingletonSkeleton(String cameraName) {
        List<Skeleton> detectedSkels = _skeletons.get(cameraName);

        if ((detectedSkels == null) || (detectedSkels.size() != 1))
            return null;

        return detectedSkels.get(0);
    }

    /**
     * Returns if the camera is tracking a single skeleton
     * @param cameraName Camera host name
     * @return True if the camera is tracking exactly 1 skeleton, false otherwise
     */
    public boolean isTrackingSingleSkeleton(String cameraName) {

        return (getSingletonSkeleton(cameraName) != null);
    }

    /**
     * @return True if all kinect cameras have tracked at least 1 skeleton in this frame
     */
    public boolean isAllKinectsTracking() {

        for (List<Skeleton> camData: _skeletons.values()) {
            if (camData == null || camData.isEmpty())
                return false;
        }

        return true;
    }

    /**
     * @return True if all kinect cameras have tracked at exactly 1 skeleton in this frame
     */
    public boolean isAllKinectsTrackingSingle() {

        for (List<Skeleton> camData: _skeletons.values()) {
            if ((camData == null) || (camData.size() != 1))
                return false;
        }

        return true;
    }

    /**
     * @return The exact time of when this frame was assembled in milliseconds
     */
    public double getTimestamp() {
        return _timestamp;
    }

    /**
     * @return The exact time of when the globally previous frame was assembled in milliseconds
     */
    public double getPrevTimestamp() {
        return _prevFrameTimestamp;
    }

    /**
     *
     * @return Iterator for Skeletons of all cameras
     */
    @Override
    public Iterator<Pair<String, Skeleton>> iterator() {
        return new FrameDataIterator();
    }

    private class FrameDataIterator implements Iterator<Pair<String, Skeleton>> {

        Iterator<Map.Entry<String, List<Skeleton>>> _camIter;
        Iterator<Skeleton> _skelIter;
        String _currCam;

        FrameDataIterator() {

            _camIter = _skeletons.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return (_skelIter != null) && (_skelIter.hasNext());
        }

        @Override
        public Pair<String, Skeleton> next() {

            // Fetch next camera info
            while ((_skelIter == null) || (!_skelIter.hasNext())) {

                if (!_camIter.hasNext())
                    throw new NoSuchElementException("End of frame data encountered (cameras, skeletons)");

                Map.Entry<String, List<Skeleton>> nextEntry = _camIter.next();

                if (nextEntry == null)
                    continue;

                _currCam = nextEntry.getKey();
                List<Skeleton> nextSkelList = nextEntry.getValue();

                if (nextSkelList == null)
                    continue;

                _skelIter = nextSkelList.iterator();
            }

            // Fetch next skeleton of camera
            return new Pair(_currCam, _skelIter.next());
        }
    }

    /**
     * Builder object for SingleFrameData.
     * Meant to be used by framework package members only.
     */
    static class SingleFrameDataBuilder {

        SingleFrameData frame = new SingleFrameData();

        /** Keeps the timestamp of the last frame data object constructed */
        static double prevTimestamp = 0;

        /**
         * @return Finalize creation, returns a complete SingleFrameData object
         */
        SingleFrameData build() {

            if ((frame._timestamp == UNINITIALIZED) || (frame._skeletons.isEmpty()))
                throw new IllegalStateException("SingleFrameData built with missing mandatory data");

            frame._prevFrameTimestamp = prevTimestamp;
            prevTimestamp = frame._timestamp; // List latest frame as prev from now on
            return frame;
        }

        /** List a host without skeletons */
        void addQuietHost(String host) {

            frame._skeletons.put(host, Collections.EMPTY_LIST);
        }

        /** Add an identified skeleton under a given host */
        void addSkeleton(String host, Skeleton skel) {

            List<Skeleton> camSkels = frame._skeletons.get(host);

            // First skeleton for this host
            if (camSkels == null) {
                camSkels = new LinkedList<>();
                frame._skeletons.put(host, camSkels);
            }

            camSkels.add(skel);
        }

        /** Set a group of identified skeleton under a given host */
        void addSkeletons(String host, List<Skeleton> skels) {

            frame._skeletons.put(host, skels);
        }

        /** Set a timestamp for when this frame was assembled */
        void addTimestamp(double timestamp) {
            frame._timestamp = timestamp;
        }
    }
}
