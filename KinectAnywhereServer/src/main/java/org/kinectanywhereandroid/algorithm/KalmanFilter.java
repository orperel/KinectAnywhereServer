package org.kinectanywhereandroid.algorithm;

import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.Pair;

import java.util.Random;

import Jama.Matrix;


/** Incomplete implementation - not in use
 *  This is a possible future implementation for a calibration filter
 *  to be applied on top of the other calibration algorithms.
 */
public class KalmanFilter {

    /** Avoid prediction when delta-time is too big, this helps avoid noisy predictions
     *  (it happens if the last sample is too far apart from the current one,
     *  increasing the risk of predicting something wrong)
     */
    private final static int DELTA_TIME_THRESHOLD = 150;

    /**
     * Sigma for measurement noise when sampling elements for Vk
     */
    private final static float MEASUREMENT_NOISE_SIGMA = 1.0f;

    /**
     * Sigma for prediction noise when sampling elements for Wk
     */
    private final static float PREDICTION_NOISE_SIGMA = 1.0f;

    private final static int M = Skeleton.JOINTS_COUNT;
    private final static int D = 3 * M;

    private final static Random rand = new Random();

    private Matrix F;                           // Prediction model matrix
    private Matrix P;                           // DxD error covariance matrix
    private SingleFrameData prevMeasurement;    // y(k-1)
    private Matrix prevEstimatedState;          // x(k-1)

    public KalmanFilter() {

        F = Matrix.identity(D, D);
        P = new Matrix(D, D);
        prevMeasurement = null;
        prevEstimatedState = null;
    }

    /**
     * @param frame Single fram data
     * @return 3 * number of joints * number of sensors vector of samples of all joints from all sensors
     */
    private Matrix skelToStateVector(SingleFrameData frame) {

        int N = frame.numOfCameras();
        Matrix stateVec = new Matrix(D * N, 1);

        int index = 0;

        for (Pair<String, Skeleton> skelEntry: frame) {

            // There should always be M joints, even if they're untracked (contain dummy values)
            for (Joint joint: skelEntry.second.joints) {

                if (joint.trackingState == Joint.JointTrackingState.Tracked) {

                    stateVec.set(index, 0, joint.x);
                    stateVec.set(index + 1, 0, joint.y);
                    stateVec.set(index + 2, 0, joint.z);
                }
                else {
                    stateVec.set(index, 0, 0);
                    stateVec.set(index + 1, 0, 0);
                    stateVec.set(index + 2, 0, 0);
                }

                index += 3;
            }
        }

        return stateVec;
    }

    /**
     * @param stateVec Single fram data
     * @return 3 * number of joints * number of sensors vector of samples of all joints from all sensors
     */
    private Skeleton stateVectorToSkeleton(Matrix stateVec) {

        // TODO: Complete this
        return null;
    }

    private Matrix calculateAverageVelocity(SingleFrameData prevMeasurement, SingleFrameData currMeasurement) {

        int N = currMeasurement.numOfCameras();
        Matrix velocity = new Matrix(D, 1);
        double[][] velocityArr = velocity.getArray();

        // Assumption: 1 Skeleton per camera
        for (Pair<String, Skeleton> skelEntry: currMeasurement) {

            String camera = skelEntry.first;
            Skeleton currSkel = currMeasurement.getSingletonSkeleton(camera);
            Skeleton prevSkel = prevMeasurement.getSingletonSkeleton(camera);
            long deltaTime = currSkel.getTimestamp() - prevSkel.getTimestamp();

            // There should always be M joints, even if they're untracked (contain dummy values)
            for (int index = 0; index < Skeleton.JOINTS_COUNT; index++) {

                Joint currJoint = currSkel.joints[index];
                Joint prevJoint = prevSkel.joints[index];

                int velocityIndex = index * 3;

                // Predict only tracked joints
                if ((currJoint.trackingState == Joint.JointTrackingState.Tracked) &&
                    (prevJoint.trackingState == Joint.JointTrackingState.Tracked)) {

                    velocityArr[velocityIndex][0]     += (currJoint.x - prevJoint.x) / deltaTime;
                    velocityArr[velocityIndex + 1][0] += (currJoint.y - prevJoint.y) / deltaTime;
                    velocityArr[velocityIndex + 2][0] += (currJoint.z - prevJoint.z) / deltaTime;
                }
            }
        }

        return velocity.times(1 / currMeasurement.numOfCameras());
    }

    /**
     * @param currMeasurement
     * @return State vector for time k
     */
    private Matrix predict(SingleFrameData currMeasurement) {

        Matrix avgVelocity = calculateAverageVelocity(prevMeasurement, currMeasurement);
        long avgDeltaTime = currMeasurement.getAverageSensorsFrameTime() - prevMeasurement.getAverageSensorsFrameTime();
        Matrix Gu = avgVelocity.times(avgDeltaTime);

        return F.times(prevEstimatedState).plus(Gu);
    }

    /**
     * @param mean
     * @param sigma
     * @return A sample from a normal distribution with mean and sigma given
     */
    private double gaussian(double mean, double sigma) {

        return rand.nextGaussian() * sigma + mean;
    }

    /**
     * @param mean Vector of means for each sample
     * @param sigma Unified sigma for all samples
     * @return Creates a vector of Nx1 samples from a normal gaussian distribution.
     *         Each gaussian sample uses (mean_i, sigma)
     */
    private Matrix gaussianVector(Matrix mean, double sigma) {

        int size = mean.getRowDimension();
        Matrix gaussianVec = new Matrix(size, 1);

        for (int i = 0; i < size; i++) {

            double sampleMean = mean.get(i, 0);
            double nextVal = gaussian(sampleMean, sigma);
            gaussianVec.set(i, 0, nextVal);
        }

        return gaussianVec;
    }

    /**
     * Computes covariance matrix for given uncorrelated vector.
     * @param vec Vec is n dimensional column vector of Nx1
     * @param expectation Expectation vector (column vector, Nx1)
     * @return NxN covariance matrix
     */
    private Matrix covariance(Matrix vec, Matrix expectation) {

        int N = vec.getRowDimension();
        Matrix normalizedVec = vec.minus(expectation);
        Matrix cov = new Matrix(N, N);
        double[][] covArray = cov.getArray();

        // Assume vec's dimensions are uncorrelated
        for (int i = 0; i < N; i++) {
            covArray[i][i] = normalizedVec.get(i, 0) * normalizedVec.get(i, 0);
        }

        return cov;
    }

    /**
     * @param vec Column vector to invert element-wise
     * @return Column vector element-wise inverted
     */
    private Matrix inverseVector(Matrix vec) {

        int N = vec.getRowDimension();
        Matrix inverse = new Matrix(N, 1);

        for (int i = 0; i < N; i++) {

            double nextVal = 1 / vec.get(i, 1);
            inverse.set(i, i, nextVal);
        }

        return inverse;
    }

    public void addSample(SingleFrameData frameK) {

        // This is a prediction of Xk given Xk-1,
        // frameK is used to calculate the velocity
        Matrix xkPredicted = predict(frameK);

        Matrix vkInverse = gaussianVector(xkPredicted, MEASUREMENT_NOISE_SIGMA);
        Matrix vK = inverseVector(vkInverse);

        // TODO: Maybe zeros should be replaced with 1/ XkPredicted vector?
        Matrix zeroMean = new Matrix(D, 1);
        Matrix Rk = covariance(vK, zeroMean);

        prevMeasurement = frameK;
    }
}

// Future tasks:
// TODO: Verify timeK is bigger than time k+1
// TODO: Handle first frame with null checks
// TODO: If joint is untracked - keep predicting for 3-5 frames