package org.kinectanywhereandroid.util;

import org.kinectanywhereandroid.algorithm.CalibrationAlgo;
import org.kinectanywhereandroid.algorithm.CoordinatesTransformer;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.model.Skeleton;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import Jama.Matrix;

/**
 * Single entry in DataHolder container
 * @param <T> Data entry object type
 */
public class DataHolderEntry<T> {

    /**
     * List of connected Kinect hosts to the calibration server
     */
    public final static DataHolderEntry<Map<String, RemoteKinect>> CONNECTED_HOSTS = new DataHolderEntry<>();

    /**
     * This helper object transforms a skeleton between the coordinates systems of a pair of Kinect cameras
     */
    public final static DataHolderEntry<CoordinatesTransformer> CAMERA_TRANSFORMER = new DataHolderEntry<>();

    /** If null - all skeletons are managed in their respective coordinate system.
     *  If not null - skeletons are converted to the coordinates system of this master camera */
    public final static DataHolderEntry<String> MASTER_CAMERA = new DataHolderEntry<>();

    /**
     * Calibration algorithm used by the skeleton calibrator
     */
    public final static DataHolderEntry<CalibrationAlgo.CalibrationMode> CALIBRATION_MODE = new DataHolderEntry<>();

    /**
     * List of skeletons viewed by master camera, including predictions from other cameras for missing
     * joints
     */
    public final static DataHolderEntry<List<Skeleton>> AVERAGE_SKELETONS = new DataHolderEntry<>();

    /**
     * When true - the master view with predicted joints is displayed.
     * Otherwise - the view of all cameras is displayed.
     */
    public final static DataHolderEntry<Boolean> SHOW_AVERAGE_SKELETONS = new DataHolderEntry<>();
}
