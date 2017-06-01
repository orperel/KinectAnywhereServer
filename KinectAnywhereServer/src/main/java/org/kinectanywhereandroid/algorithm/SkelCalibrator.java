package org.kinectanywhereandroid.algorithm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import Jama.Matrix;

/**
 * Responds to new frames by feeding into calibration algorithm
 */
public class SkelCalibrator implements IKinectFrameEventListener, CoordinatesTransformer {

    private CalibrationAlgo _algo;
    private Map<Pair<String, String>, TemporalApproximation> _temporalApproximations;
    private Map<Pair<String, String>, BestInClass> _bestInClassApproximations;

    /**
     * Transformations between camera 1 coordinates to camera 2 coordinates.
     * Key: <From, To>
     * Value: 4x4 homogeneous transformation matrix
     */
    private Map<Pair<String, String>, Matrix> _transformations;

    public SkelCalibrator() {

        _algo = new CalibrationAlgo();
        _transformations = new HashMap<>();
        _temporalApproximations = new HashMap<>();
        _bestInClassApproximations = new HashMap<>();
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

        if (fromCamera.equals(toCamera))
            return skeleton;

        Matrix transformation = getTransformation(fromCamera, toCamera);
        return CalibrationAlgo.transform(skeleton, transformation);
    }

    /**
     * Predicts skeleton with hidden joints according to data from all other calibrated cameras
     * @param frame Current frame
     * @return List of skeletons viewed by the master camera, with hidden joints predicted
     */
    @NonNull
    public List<Skeleton> predictAverageSkeletons(SingleFrameData frame) {

        String masterCamera = DataHolder.INSTANCE.retrieve(DataHolderEntry.MASTER_CAMERA);

        if (masterCamera == null)
            return Collections.emptyList();

        List<Skeleton> trackedSkeletons = frame.getSkeletons(masterCamera);
        if (trackedSkeletons.isEmpty())
            return Collections.emptyList();

        // Master tracked skeleton & a joint type to a list that contains:
        // for each joint - a summation of all other camera views, and the number of cameras
        // participating. To get the average joint - divide Join coordinates by the number of
        // cameras
        Map<Pair<Skeleton, Joint.JointType>, Pair<Joint, Integer>> trackedSkelToAvgViewsCount = new HashMap<>();

        for (Pair<String, Skeleton> camTracking: frame) {

            String cameraName = camTracking.first;
            Skeleton currentSkel = camTracking.second;
            if (cameraName.equals(masterCamera))
                continue;

            Skeleton transfomedSkel = transform(cameraName, masterCamera, currentSkel);
            double minMSE = Double.MAX_VALUE;
            Skeleton matchedMasterSkeleton = null;

            // Search for which master tracked skeleton the current skeleton matches
            for (Skeleton masterTrackedSkel: trackedSkeletons) {

                double mse = calculateMSE(transfomedSkel, masterTrackedSkel);
                if (mse < minMSE) {
                    minMSE = mse;
                    matchedMasterSkeleton = masterTrackedSkel;
                }
            }

            // For each tracked joint - add to average skeleton
            for (Joint transformedJoint: transfomedSkel.joints) {

                if (transformedJoint.trackingState != Joint.JointTrackingState.Tracked)
                    continue;

                Pair<Skeleton, Joint.JointType> key = new Pair<>(matchedMasterSkeleton, transformedJoint.type);

                if (!trackedSkelToAvgViewsCount.containsKey(key)) {
                    Pair<Joint, Integer> initialValue = new Pair<>(new Joint(), 0);
                    trackedSkelToAvgViewsCount.put(key, initialValue);
                }

                Pair<Joint, Integer> averageJoint = trackedSkelToAvgViewsCount.get(key);
                averageJoint.first.add(transformedJoint);
                int newCount = averageJoint.second + 1;
                trackedSkelToAvgViewsCount.put(key, new Pair<>(averageJoint.first, newCount));
            }
        }

        // Now fill all missing joints with predictions from other cameras
        List<Skeleton> prediction = new LinkedList<>();

        for (Skeleton masterSkel: trackedSkeletons) {

            Skeleton predictedSkel = new Skeleton(masterSkel);
            prediction.add(predictedSkel);

            for (int jointId = 0; jointId < Skeleton.JOINTS_COUNT; jointId++) {

                Joint masterJoint = predictedSkel.joints[jointId];

                // Joint is missing, try to make up with data from other cameras
                if (masterJoint.trackingState != Joint.JointTrackingState.Tracked) {

                    Pair<Skeleton, Joint.JointType> key = new Pair<>(masterSkel, masterJoint.type);

                    if (!trackedSkelToAvgViewsCount.containsKey(key))
                        continue; // No data to predict with

                    Pair<Joint, Integer> avgJointData = trackedSkelToAvgViewsCount.get(key);
                    Joint avgJoint = avgJointData.first;
                    int numOfViews = avgJointData.second;
                    avgJoint.normalize(numOfViews); // Get the real average joint from average of all cameras

                    masterJoint.trackingState = Joint.JointTrackingState.Predicted;
                    masterJoint.x = avgJoint.x;
                    masterJoint.y = avgJoint.y;
                    masterJoint.z = avgJoint.z;
                }
            }
        }

        return prediction;
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

                    CalibrationAlgo.CalibrationMode mode = DataHolder.INSTANCE.retrieve(DataHolderEntry.CALIBRATION_MODE);
                    Matrix transformation;

                    // Choose algorithm by mode
                    switch (mode) {

                        case PER_FRAME: {
                            transformation = calibrateSingleFrame(fromEntries, toEntries);
                            break;
                        }

                        case FIRST_ORDER_TEMPORAL_APPROX: {
                            transformation = calibrateFirstOrderApproximation(fromEntries, toEntries);
                            break;
                        }

                        case BEST_IN_CLASS: {
                            transformation = calibrateBestInClassApproximation(fromEntries, toEntries);
                            break;
                        }

                        default: { // Shouldn't happen - this is a fallback
                            transformation = calibrateSingleFrame(fromEntries, toEntries);
                            break;
                        }
                    }

                    setTransformation(fromCamera, toCamera, transformation);
                }
            }
        }

        // Predict hidden joints for master skeletons view and save to DataHolder singleton
        // Do so only if this view is turned on (otherwise don't waste CPU time on that)
        boolean isShowAverageSkels = DataHolder.INSTANCE.retrieve(DataHolderEntry.SHOW_AVERAGE_SKELETONS);
        if (isShowAverageSkels) {

            List<Skeleton> predictedSkels = predictAverageSkeletons(frame);
            DataHolder.INSTANCE.save(DataHolderEntry.AVERAGE_SKELETONS, predictedSkels);
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

    /**
     * Calibrate current frame using a rotation axis temporal aproximation
     * @param fromEntries
     * @param toEntries
     * @return The transformation matrix between fromCamera to toMatrix considering temporal average
     *         approximation.
     */
    @NonNull
    private Matrix calibrateFirstOrderApproximation(Pair<String, Skeleton> fromEntries,
                                                    Pair<String, Skeleton> toEntries) {

        String fromCam = fromEntries.first;
        String toCam = toEntries.first;
        Skeleton fromSkel = fromEntries.second;
        Skeleton toSkel = toEntries.second;

        Matrix currFrameTransform = _algo.calibrate(fromSkel, toSkel);
        Pair<String, String> pairKey = new Pair<>(fromCam, toCam);

        Skeleton transformedSkel = _algo.transform(fromSkel, currFrameTransform);
        double mse = calculateMSE(toSkel, transformedSkel);
        Log.i("Calibrator", "TemporalFirstOrder MSE: " + mse);
        if (mse > 0.2) { // Avoid noisy samples that may ruin the averaging process

            if (!_temporalApproximations.containsKey(pairKey)) {
                return Matrix.identity(4, 4);
            }
            else {
                return _temporalApproximations.get(pairKey).getTransform();
            }
        }

        Matrix rotationSample = CalibrationAlgo.Rotation.extractRotation(currFrameTransform);
        Matrix translationSample = CalibrationAlgo.Rotation.extractTranslation(currFrameTransform);

        TemporalApproximation approximator = null;

        if (!_temporalApproximations.containsKey(pairKey)) {
            approximator = new TemporalApproximation(rotationSample, translationSample);
            _temporalApproximations.put(pairKey, approximator);
        }
        else {
            approximator = _temporalApproximations.get(pairKey);
            approximator.add(rotationSample, translationSample);
        }

        return approximator.getTransform();
    }

    /**
     * Calibrate current frame using the best transformation in terms of mean square error
     * found so far.
     * @param fromEntries
     * @param toEntries
     * @return The transformation matrix between fromCamera to toMatrix considering best transformation
     * found so far (for minimal mse for some frame)
     */
    @NonNull
    private Matrix calibrateBestInClassApproximation(Pair<String, Skeleton> fromEntries,
                                                     Pair<String, Skeleton> toEntries) {

        String fromCam = fromEntries.first;
        String toCam = toEntries.first;
        Skeleton fromSkel = fromEntries.second;
        Skeleton toSkel = toEntries.second;

        Matrix currFrameTransform = _algo.calibrate(fromSkel, toSkel);
        Pair<String, String> pairKey = new Pair<>(fromCam, toCam);


        if (!_bestInClassApproximations.containsKey(pairKey)) {
            _bestInClassApproximations.put(pairKey, new BestInClass());
        }

        BestInClass approximator = _bestInClassApproximations.get(pairKey);
        approximator.applyCandidate(fromSkel, toSkel, currFrameTransform);

        Matrix transform = approximator.getTransform();
        double currentMSE = calculateMSE(toSkel, CalibrationAlgo.transform(fromSkel, transform));
        Log.i("Calibrator", "BestInClass MSE: " + currentMSE);

        return transform;
    }

    private double calculateMSE(Skeleton skel1, Skeleton skel2) {

        double squaredSum = 0;

        for (int i = 0; i < Skeleton.JOINTS_COUNT; i++) {

            Joint j1 = skel1.joints[i];
            Joint j2 = skel2.joints[i];

            if ((j1.trackingState == Joint.JointTrackingState.Tracked) &&
                (j2.trackingState == Joint.JointTrackingState.Tracked)) {

                double distance = j1.distance(skel2.joints[i]);
                squaredSum += Math.pow(distance, 2);
            }
        }

        return Math.sqrt(squaredSum);
    }

    private class TemporalApproximation {

        private Matrix axisAngle;
        private Matrix translation;

        private int framesAveraged;

        public TemporalApproximation(Matrix rotationMatSample, Matrix translationVecSample) {

            framesAveraged = 1;
            axisAngle = CalibrationAlgo.Rotation.rotationMatToAxisAngle(rotationMatSample);;
            translation = new Matrix(translationVecSample.getArray());
        }

        public void add(Matrix rotationMatSample, Matrix translationVecSample) {

            translation = translation.times(framesAveraged).plus(translationVecSample).times(1.0 / (framesAveraged + 1.0));

            Matrix axisAngleSample = CalibrationAlgo.Rotation.rotationMatToAxisAngle(rotationMatSample);
            axisAngle = axisAngle.times(framesAveraged).plus(axisAngleSample).times(1.0 / (framesAveraged + 1.0));

            framesAveraged++;
        }

        public Matrix getTransform() {

            Matrix rotationMat = CalibrationAlgo.Rotation.axisAngletoRotationMat(axisAngle);
            Matrix homogeneousMat = CalibrationAlgo.Rotation.composeHomogeneous(rotationMat, translation);

            return homogeneousMat;
        }
    }

    private class BestInClass {

        private double minMSE;
        private Matrix bestTransform;

        public BestInClass() {
            minMSE = Double.MAX_VALUE;
            bestTransform = Matrix.identity(4, 4);
        }

        public void applyCandidate(Skeleton from, Skeleton to, Matrix transform) {

            Skeleton transformedSkel = CalibrationAlgo.transform(from, transform);
            double mse = calculateMSE(transformedSkel, to);

            if (mse < minMSE) {
                bestTransform = transform;
            }
        }

        public Matrix getTransform() {

            return bestTransform;
        }
    }
}