package org.kinectanywhereandroid.algorithm;

import android.support.annotation.NonNull;

import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.util.Pair;

import java.util.HashMap;
import java.util.Map;

import Jama.Matrix;

/**
 * Responds to new frames by feeding into calibration algorithm
 */
public class SkelCalibrator implements IKinectFrameEventListener, CoordinatesTransformer {

    private CalibrationAlgo _algo;

    /**
     * Transformations between camera 1 coordinates to camera 2 coordinates.
     * Key: <From, To>
     * Value: 4x4 homogeneous transformation matrix
     */
    private Map<Pair<String, String>, Matrix> _transformations;

    public SkelCalibrator() {

        _algo = new CalibrationAlgo();
        _transformations = new HashMap<>();
        DataHolder.INSTANCE.save(DataHolderEntry.CAMERA_TRANSFORMER, this);
    }

    /**
     * Get transformation from coordinates of camera 1 to camera 2
     * @param fromCamera
     * @param toCamera
     * @return The 4x4 homogeneous transformation matrix
     */
    @NonNull
    public Matrix getTransformation(String fromCamera, String toCamera) {

        Pair<String, String> key = new Pair<>(fromCamera, toCamera);

        if (!_transformations.containsKey(key))
            _transformations.put(key, Matrix.identity(4, 4));

        return _transformations.get(key);
    }

    /**
     * Set transformation from coordinates of camera 1 to camera 2
     * @param fromCamera
     * @param toCamera
     * @param transformation
     */
    public void setTransformation(String fromCamera, String toCamera, Matrix transformation) {

        Pair<String, String> key = new Pair<>(fromCamera, toCamera);
        _transformations.put(key, transformation);
    }

    /**
     * Transforms the skeleton from coordinates system of camera 1 to coordinates system of camera 2
     * @param fromCamera Name of camera 1
     * @param toCamera Name of camera 2
     * @param skeleton Skeleton in coordinates of camera 1 (fromCamera)
     * @return The skeleton in coordinates system of camera 2 (toCamera)
     */
    @Override
    @NonNull
    public Skeleton transform(String fromCamera, String toCamera, Skeleton skeleton) {

        Matrix transformation = getTransformation(fromCamera, toCamera);
        return CalibrationAlgo.transform(skeleton, transformation);
    }

    /**
     * Execute calibrate cameras step using the skeletons data that arrived from each Kinect camera
     * @param frame The frame data that arrived from each Kinect camera
     */
    @Override
    public void handle(SingleFrameData frame) {

        // Calibrate each pair of cameras only when each camera tracks exactly a single skeleton
        for (Pair<String, Skeleton> fromEntries: frame) {
            for (Pair<String, Skeleton> toEntries: frame) {

                String fromCamera = fromEntries.first;
                String toCamera = toEntries.first;

                // Different cameras each tracking a single skeleton
                if ((!fromCamera.equals(toCamera)) &&
                        (frame.isTrackingSingleSkeleton(fromCamera)) &&
                        (frame.isTrackingSingleSkeleton(toCamera))) {

                    // TODO: Support other calibration modes
                    Matrix transformation = calibrateSingleFrame(fromEntries, toEntries);
                    setTransformation(fromCamera, toCamera, transformation);
                }
            }
        }
    }

    /**
     * Calibrate current frame without considering the previous calibration attempts
     * @param fromEntries
     * @param toEntries
     * @return The transformation matrix between fromCamera to toMatrix considering the current frame
     *         data only.
     */
    @NonNull
    private Matrix calibrateSingleFrame(Pair<String, Skeleton> fromEntries,
                                        Pair<String, Skeleton> toEntries) {

        Skeleton fromSkel = fromEntries.second;
        Skeleton toSkel = toEntries.second;

        return _algo.calibrate(fromSkel, toSkel);
    }
}