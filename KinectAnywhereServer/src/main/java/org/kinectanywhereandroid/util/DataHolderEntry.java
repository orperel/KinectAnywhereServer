package org.kinectanywhereandroid.util;

import org.kinectanywhereandroid.algorithm.CalibrationAlgo;
import org.kinectanywhereandroid.algorithm.CoordinatesTransformer;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.model.Skeleton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
     * Constructor for RemoteKinect concrete impls.
     * Determines how RemoteKinect incoming should be handled
     * (queued to handle all incoming samples or always use the last one to arrive).
     */
    public final static DataHolderEntry<Constructor<? extends RemoteKinect>> REMOTE_KINECT_CTOR = new DataHolderEntry<>();

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
	
	/**
	 * Queue for sending broadcast messages
     */
    public final static DataHolderEntry<Queue<String>> BROADCASTING_QUEUE = new DataHolderEntry<>();
}
