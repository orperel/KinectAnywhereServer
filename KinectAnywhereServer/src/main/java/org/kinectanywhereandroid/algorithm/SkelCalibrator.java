package org.kinectanywhereandroid.algorithm;

import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.SingleFrameData;

/**
 * Responds to new frames by feeding into calibration algorithm
 */
public class SkelCalibrator implements IKinectFrameEventListener {

    CalibrationAlgo _algo;

    public SkelCalibrator() {
        _algo = new CalibrationAlgo();
    }

    @Override
    public void handle(SingleFrameData frame) {

        // TODO: implement
    }
}
