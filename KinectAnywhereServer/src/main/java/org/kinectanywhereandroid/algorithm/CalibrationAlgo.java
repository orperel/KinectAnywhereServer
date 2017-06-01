package org.kinectanywhereandroid.algorithm;

import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

import org.kinectanywhereandroid.util.Pair;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Joint.JointTrackingState;
import org.kinectanywhereandroid.model.Skeleton;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Contains algorithms for performing calibration between multiple IR Kinect Depth cameras.
 * This module assumes processed Kinect data in the form of Skeleton joints converted by the
 * Microsoft Kinect driver from depth maps.
 *
 * Features:
 * - Absolute-Orientation algorithm - for finding the best Rotation & Translation between a pair
 *   of Skeletons (tracked joints only), to position the joints as closely as possible to each other.
 * - Rodrigues algorithm - for conversion from rotation matrix to axis-angle format, which is used
 *   for processing rotations with less dimensions (for transformation estimation, filtering, etc).
 * - Transformation of Skeletons between coordinate systems.
 *
 * @see <a href="http://nghiaho.com/?page_id=671">Absolute Orientation</a>
 */
public class CalibrationAlgo {

    /** For floating points rounding errors */
    private static final double EPSILON = 0.000001;

    public enum CalibrationMode {

        PER_FRAME,
        FIRST_ORDER_TEMPORAL_APPROX,
        BEST_IN_CLASS,
        KALMAN
    }

    /**
     * Calibrate the matched skeleton to the coordinates system of the master skeleton.
     * Both skeleton are expected to be approximately aligned after this method returns.
     * @param master The skeleton in master coordinates system.
     * @param matched The skeleton in a second coordinates system, which will be converted to the
     *                master coordinates system.
     * @return 4x4 Homogeneous transformation matrix from matched skeleton coordinates system
     *         to master coordinates system.
     */
    @NonNull
    public Matrix calibrate(Skeleton matched, Skeleton master)
    {
        // Step 1: Selection (filter only joints that are tracked in both skeletons)
        List<Pair<Joint, Joint>> selection = AbsoluteOrientation.selectPoints(matched, master);

        // Step 2: Absolute Orientation (align skeletons to best rotation and translation)
        Matrix transform = AbsoluteOrientation.estimateTransformation(selection);

        return transform;
    }

    /**
     * Applies transformation to skeleton's joints (rotate and translate).
     * @param skel Skeleton in first coordinates system.
     * @param transformation 4x4 Homogeneous transformation matrix to transform the skeleton
     *                       to a second coordinates system.
     */
    @NonNull
    public static Skeleton transform(Skeleton skel, Matrix transformation) {

        Skeleton transformedSkel = new Skeleton(skel);

        for (int i = 0; i < Skeleton.JOINTS_COUNT; i++) {

            Joint joint = transformedSkel.joints[i];
            Matrix jointPos = new Matrix(new double[][]{
                    {joint.x}, {joint.y}, {joint.z}, {1}
            } );

            Matrix transformedJoint = transformation.times(jointPos);
            transformedSkel.joints[i].x = (float)transformedJoint.get(0, 0);
            transformedSkel.joints[i].y = (float)transformedJoint.get(1, 0);
            transformedSkel.joints[i].z = (float)transformedJoint.get(2, 0);
        }
        return transformedSkel;
    }

    private static class AbsoluteOrientation {

        /**
         * A pre-processing phase which parses both Skeletons and chooses only
         * joint types which are tracked in both Skeletons.
         * @param master First Skeleton
         * @param matched Second Skeleton
         * @return List of pair of joints which are tracked in both skeletons (e.g: head, left hand, etc).
         */
        @NonNull
        private static List<Pair<Joint, Joint>> selectPoints(Skeleton master, Skeleton matched) {

            List<Pair<Joint, Joint>> selection = new LinkedList<>();

            for (int i = 0; i < Skeleton.JOINTS_COUNT; i++) {
                boolean isMasterTracked = (master.joints[i].trackingState == JointTrackingState.Tracked);
                boolean isOtherTracked =  (matched.joints[i].trackingState == JointTrackingState.Tracked);

                if ((isMasterTracked) && (isOtherTracked))
                    selection.add(new Pair<>(master.joints[i], matched.joints[i]));
            }

            return selection;
        }

        /**
         * Computes the center average points of 2 clouds of points (joints).
         * @param matches 2 cloud of joints.
         * @return The centroid position of each of both clouds.
         */
        @NonNull
        private static Pair<Joint, Joint> computeCentroids(List<Pair<Joint, Joint>> matches)
        {
            Joint pa = new Joint(0, 0, 0);
            Joint pb = new Joint(0, 0, 0);

            for (Pair<Joint, Joint> pair: matches)
            {
                pa.add(pair.first);
                pb.add(pair.second);
            }

            pa.normalize(matches.size());
            pb.normalize(matches.size());

            return new Pair<>(pa, pb);
        }

        /**
         * Computes the covariance matrix between the accumulated pairs of joints.
         * Each variance between pair of joints is added to to the accumulated covariance matrix.
         * All joints of each original skeleton are normalized by placing their centroid on (0, 0).
         * @param matches Pair of matched joint points in 2 Skeletons.
         * @param centroids Center position of each cloud of joint points.
         * @return 3x3 Covariance matrix between 2 sets of joints (both sides of "matches" pair, accumulated).
         */
        @NonNull
        private static Matrix computeCovariance(List<Pair<Joint, Joint>> matches, Pair<Joint, Joint> centroids) {

            // Java arrays default to 0
            double[][] cov = new double[3][3];

            double[] cent_a = centroids.first.toArray();
            double[] cent_b = centroids.second.toArray();

            for (Pair<Joint, Joint> pair: matches) {
                double[] pa = pair.first.toArray();
                double[] pb = pair.second.toArray();

                // Outer product of each pair of Joints
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        cov[i][j] += (pa[i] - cent_a[i]) * (pb[j] - cent_b[j]);

            }

            return new Matrix(cov);
        }

        /**
         * Finds optimal Rotation and Translation between pairs of joints.
         * This implementation uses SVD (Kabsch's algorithm) to find the
         * homogeneous transformation matrix estimation between the pairs of points.
         * Result aims to minimize MSE as much as possible.
         * @param matches Pairs of points to match with a transformation matrix
         * @return 4x4 homogeneous transformation matrix that minimizes MSE between pairs of joints
         *         in the same coordinates system (1 set of joints is converted to the coordinates
         *         system of the other).
         *
         * @see <a href="https://en.wikipedia.org/wiki/Kabsch_algorithm">Kabsch Algorithm</a>
         */
        @NonNull
        private static Matrix estimateTransformation(List<Pair<Joint, Joint>> matches)
        {
            Pair<Joint, Joint> centroids = computeCentroids(matches);
            Matrix cov = computeCovariance(matches, centroids);

            // Svd step
            SingularValueDecomposition decomp = cov.svd();
            Matrix u = decomp.getU();
            Matrix v = decomp.getV();

            // Rotation calculation
            Matrix r = v.times(u.transpose());

            double[][] r_arr = r.getArray();

            if (r.det() < 0)
            {
                r_arr[2][0] *= -1;
                r_arr[2][1] *= -1;
                r_arr[2][2] *= -1;
            }

            // --Construct 4x4 Homogeneous transformation matrix--
            // [ R R R Tx
            //   R R R Ty
            //   R R R Tz
            //   0 0 0 1 ]
            double[][] transform_arr = new double[4][4];

            // Rotation
            transform_arr[0][0] = r_arr[0][0];
            transform_arr[1][0] = r_arr[1][0];
            transform_arr[2][0] = r_arr[2][0];
            transform_arr[0][1] = r_arr[0][1];
            transform_arr[1][1] = r_arr[1][1];
            transform_arr[2][1] = r_arr[2][1];
            transform_arr[0][2] = r_arr[0][2];
            transform_arr[1][2] = r_arr[1][2];
            transform_arr[2][2] = r_arr[2][2];

            // Translation
            transform_arr[0][3] = (-1) * (transform_arr[0][0] * centroids.first.x +
                    transform_arr[0][1] * centroids.first.y +
                    transform_arr[0][2] * centroids.first.z) +
                    centroids.second.x;
            transform_arr[1][3] = (-1) * (transform_arr[1][0] * centroids.first.x +
                    transform_arr[1][1] * centroids.first.y +
                    transform_arr[1][2] * centroids.first.z) +
                    centroids.second.y;
            transform_arr[2][3] = (-1) * (transform_arr[2][0] * centroids.first.x +
                    transform_arr[2][1] * centroids.first.y +
                    transform_arr[2][2] * centroids.first.z) +
                    centroids.second.z;

            // Normalize last row
            transform_arr[3][0] = 0;
            transform_arr[3][1] = 0;
            transform_arr[3][2] = 0;
            transform_arr[3][3] = 1;

            return new Matrix(transform_arr);
        }
    }

    public static class Rotation {

        // Algorithms implemented according to:
        // https://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.pdf

        private static double arctan2(double y, double x) {
            if ((Math.abs(x) < EPSILON) && (y > 0))
                return (Math.PI / 2);
            else if ((Math.abs(x) < EPSILON) && (y < 0))
                return (-Math.PI / 2);
            else if (x > 0)
                return Math.atan(y / x);
            else if (x < 0)
                return (Math.atan(y / x) + Math.PI);

            return 0; // Undefined for (0,0)
        }

        public static Matrix extractRotation(Matrix homogeneousTransform) {

            // Homogeneous matrix is 4x4, rotation matrix is the top left 3x3 sub-matrix
            return homogeneousTransform.getMatrix(0, 2, 0, 2);
        }

        public static Matrix extractTranslation(Matrix homogeneousTransform) {

            // Homogeneous matrix is 4x4, translation vector is the top right 3x1 sub-matrix
            return homogeneousTransform.getMatrix(0, 2, 3, 3);
        }

        public static Matrix composeHomogeneous(Matrix rotationMat, Matrix translationVec) {

            // Rotation matrix is a 3x3 matrix
            // Translation vec is a 3x1 vector
            // Homogeneous matrix is 4x4, rotation matrix is the top left 3x3 sub-matrix
            double[][] homogeneousTransform = new double[][] {
                    { rotationMat.get(0, 0), rotationMat.get(0, 1), rotationMat.get(0, 2), translationVec.get(0, 0) },
                    { rotationMat.get(1, 0), rotationMat.get(1, 1), rotationMat.get(1, 2), translationVec.get(1, 0) },
                    { rotationMat.get(2, 0), rotationMat.get(2, 1), rotationMat.get(2, 2), translationVec.get(2, 0) },
                    { 0, 0, 0, 1}
            };

            return new Matrix(homogeneousTransform);
        }

        public static Matrix rotationMatToAxisAngle(Matrix R)
        {
            // -- Calculate using the Inverse Rodrigues formula --
            // rotationMat is a rotation matrix in SO3 where det(rotationMat)=1 and (rotationMat')*(rotationMat) = I

            // Calculation factors

            // A = (R - R') / 2
            final Matrix A = (R.minus(R.transpose())).times(0.5f);

            // p = [ a32 a13 a21 ]'
            Matrix p = new Matrix(new double[][]{ { A.get(2, 1) }, { A.get(0, 2) }, { A.get(1, 0) } });

            // s = ||p||
            double s = p.norm2();

            // c = (r00 + r11 + r22 - 1) / 2
            double c = (R.trace() - 1) * 0.5f;

            // If s = 0, c = 1 (avoid floating points rounding errors)
            if ((s <= EPSILON) && (c <= 1 + EPSILON) && ( 1 - EPSILON <= c))
            {
                return new Matrix(3, 1); // Axis angle vector r = 0
            }
            else if ((s <= EPSILON) && (c <= -1 + EPSILON) && (-1 - EPSILON <= c)) // s = 0, c = -1
            {
                // TODO: v must be a non zero column of R + I
                Matrix v = new Matrix(new double[][] { { R.get(0, 0) + 1}, { R.get(1, 0) }, { R.get(2, 0) } });

                // u = PI * (v / ||v||)
                Matrix u = v.times(1 / v.norm2()).times(Math.PI);

                if ((u.norm2() == Math.PI) &&
                        (((u.get(0, 0) == 0) && (u.get(1, 0) == 0) && (u.get(2, 0) < 0)) ||
                        ((u.get(0, 0) == 0) && (u.get(1, 0) < 0)) ||
                        (u.get(0, 0) < 0)))
                {
                    return u.times(-1);
                }
                else
                {
                    return u;
                }
            }

            double theta = arctan2(s, c);

            // Sin theta != 0
            if (Math.abs(Math.sin(theta)) > EPSILON)
            {
                Matrix u = p.times(1 / s);
                return u.times(theta);
            }

            return new Matrix(4, 1); // Undefined - shouldn't happen
        }

        public static Matrix axisAngletoRotationMat(Matrix r)
        {
            // -- Calculate using the Rodrigues formula --
            // r is an axis angle vector in 3d space

            // The angle of rotation is encoded in the norm of axisAngle
            double theta = r.norm2();

            // Make sure <= PI
            if (theta > Math.PI)
                theta -= 2 * Math.PI;

            // Rotation matrix is identity
            if (Math.abs(theta) <= EPSILON)
            {
                Matrix rotationMat = Matrix.identity(3, 3);
                return rotationMat;
            }

            // Define: u = r / theta
            Matrix u = r.times(1 / theta);
            double ux = u.get(0, 0);
            double uy = u.get(1, 0);
            double uz = u.get(2, 0);

            double[][] rotationMatArr = new double[3][3];

            float oneMinusCosTheta = (float)(1 - Math.cos(theta));
            float halfSinTheta = (float)(Math.sin(theta));
            rotationMatArr[0][0] = (float)Math.cos(theta) + oneMinusCosTheta * ux * ux;
            rotationMatArr[0][1] = oneMinusCosTheta * ux * uy + halfSinTheta * (-uz);
            rotationMatArr[0][2] = oneMinusCosTheta * ux * uz + halfSinTheta * (uy);
            rotationMatArr[1][0] = oneMinusCosTheta * uy * ux + halfSinTheta * (uz);
            rotationMatArr[1][1] = (float)Math.cos(theta) + oneMinusCosTheta * uy * uy;
            rotationMatArr[1][2] = oneMinusCosTheta * uy * uz + halfSinTheta * (-ux);
            rotationMatArr[2][0] = oneMinusCosTheta * uz * ux + halfSinTheta * (-uy);
            rotationMatArr[2][1] = oneMinusCosTheta * uz * uy + halfSinTheta * (ux);
            rotationMatArr[2][2] = (float)Math.cos(theta) + oneMinusCosTheta * uz * uz;

            return new Matrix(rotationMatArr);
        }
    }
}
