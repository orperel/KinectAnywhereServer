package org.kinectanywhereandroid.framework;

/**
 * Interface for Kinect raw data samples consumers
 */
public interface IKinectDataConsumer {

    /** Register modules that may want to respond to arriving kinect frames (e.g: paint, calibrate..) */
    public void register(IKinectFrameEventListener listener);

    /** Start consuming from Kinect queues */
    public void activate();

    /** Stop consuming from Kinect queues */
    public void deactivate();
}
