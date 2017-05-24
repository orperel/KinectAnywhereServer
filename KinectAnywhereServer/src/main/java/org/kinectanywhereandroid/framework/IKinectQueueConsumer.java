package org.kinectanywhereandroid.framework;

/**
 * Interface for Kinect raw data queues consumers
 */
public interface IKinectQueueConsumer {

    public void register(IKinectFrameEventListener listener);
    public void activate();
}
