package org.kinectanywhereandroid.framework;

/**
 * Interface for Kinect raw data queues consumers
 */
public interface IKinectQueueConsumer {

    /** Register modules that may want to respond to arriving kinect frames (e.g: paint, calibrate..) */
    public void register(IKinectFrameEventListener listener);

    /** Start consuming from Kinect queues */
    public void activate();

    /** Stop consuming from Kinect queues */
    public void deactivate();
}
