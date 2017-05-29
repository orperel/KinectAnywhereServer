package org.kinectanywhereandroid.algorithm;

import org.junit.Test;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Skeleton;

import Jama.Matrix;

import static org.junit.Assert.assertArrayEquals;

public class CalibrationAlgoTest {

    // Baseline version
    BaselineCalibrationAlgo baseline = new BaselineCalibrationAlgo();

    // Updated version
    CalibrationAlgo algo = new CalibrationAlgo();

    Skeleton getSkelA() {

        org.kinectanywhereandroid.model.Skeleton ASkel = new Skeleton();

        double[][] A =
                { { 0.37091737, 0.38984888, 0.10545795 },
                        { 0.58442795, 0.71198069, 0.24099 },
                        { 0.67862418, 0.97619695, 0.02586278 },
                        { 0.06766309, 0.51348494, 0.98026193 },
                        { 0.53514552, 0.47004606, 0.52769418 },
                        { 0.91207793, 0.38898765, 0.89654981 },
                        { 0.4084758, 0.69658133, 0.6651389 },
                        { 0.94898908, 0.44990336, 0.94206914 },
                        { 0.65550344, 0.19385894, 0.04865845 },
                        { 0.8000711, 0.57958397, 0.82320822 } };

        for (int i = 0; i < org.kinectanywhereandroid.model.Skeleton.JOINTS_COUNT; i++) {

            if (i < A.length) {
                Joint p = new Joint((float) A[i][0], (float) A[i][1], (float) A[i][2]);
                p.trackingState = Joint.JointTrackingState.Tracked;
                ASkel.joints[i] = p;
            }
            else {
                ASkel.joints[i] = new Joint(); // Empty Joint
            }
        }

        return ASkel;
    }

    Skeleton getSkelB() {

        org.kinectanywhereandroid.model.Skeleton BSkel = new Skeleton();

        double[][] B =
                { { 1.00327991, 1.13433826, 0.98743358 },
                        { 1.25304985, 1.36061201, 1.22011313 },
                        { 1.51780598, 1.21547795, 1.40396954 },
                        { 1.07103277, 1.94517414, 0.52864631 },
                        { 0.99108274, 1.58719173, 1.06781318 },
                        { 0.78528514, 2.00041224, 1.33539333 },
                        { 1.2189174, 1.73540845, 0.95655822 },
                        { 0.83061778, 2.06179409, 1.37175794 },
                        { 0.76147611, 1.09884811, 1.23823001 },
                        { 1.00301917, 1.9412993, 1.27752252 } };

        for (int i = 0; i < org.kinectanywhereandroid.model.Skeleton.JOINTS_COUNT; i++) {

            if (i < B.length) {
                Joint p = new Joint((float) B[i][0], (float) B[i][1], (float) B[i][2]);
                p.trackingState = Joint.JointTrackingState.Tracked;
                BSkel.joints[i] = p;
            } else {
                BSkel.joints[i] = new Joint(); // Empty Joint
            }
        }

        return BSkel;
    }

    @Test
    public void calibrate() throws Exception {

        Skeleton skelA = getSkelA();
        Skeleton skelB = getSkelB();

        Matrix baselineTransform = baseline.calibrate(skelA, skelB);
        Matrix transform = algo.calibrate(skelA, skelB);

        double[][] baseArray = baselineTransform.getArray();
        double[][] transformArray = transform.getArray();

        assertArrayEquals(baseArray, transformArray);
    }

    @Test
    public void transform() throws Exception {

        Skeleton skelA = getSkelA();
        Skeleton skelB = getSkelB();

        Matrix transform = algo.calibrate(skelA, skelB);

        Matrix rotation = CalibrationAlgo.Rotation.extractRotation(transform);
        Matrix translation = CalibrationAlgo.Rotation.extractTranslation(transform);
        Matrix axisAngle = CalibrationAlgo.Rotation.rotationMatToAxisAngle(rotation);
        Matrix newRotation = CalibrationAlgo.Rotation.axisAngletoRotationMat(axisAngle);
        Matrix newHomogeneous = CalibrationAlgo.Rotation.composeHomogeneous(newRotation, translation);

        double[][] transformArray = transform.getArray();
        double[][] newArray = newHomogeneous.getArray();

        assertArrayEquals(transformArray, newArray);
    }
}