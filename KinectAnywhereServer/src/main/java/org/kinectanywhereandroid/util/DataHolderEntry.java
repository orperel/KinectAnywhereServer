package org.kinectanywhereandroid.util;

import org.kinectanywhereandroid.algorithm.CoordinatesTransformer;
import org.kinectanywhereandroid.framework.RemoteKinect;

import java.lang.reflect.Method;
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
}
