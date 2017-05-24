package org.kinectanywhereandroid.framework;


/**
 * Interface for responding to a new frame arriving from multiple Kinect cameras
 */
public interface IKinectFrameEventListener {

    void handle(SingleFrameData frame);
}
