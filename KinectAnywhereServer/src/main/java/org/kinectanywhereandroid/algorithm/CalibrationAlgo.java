package org.kinectanywhereandroid.algorithm;

import java.util.LinkedList;
import java.util.List;

import org.kinectanywhereandroid.util.Pair;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Joint.JointTrackingState;
import org.kinectanywhereandroid.model.Skeleton;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class CalibrationAlgo {

    private static final double EPSILON = 0.000001;

   	private List<Pair<Joint, Joint>> selectPoints(Skeleton master, Skeleton matched) {
   		
        List<Pair<Joint, Joint>> selection = new LinkedList<Pair<Joint, Joint>>();

        for (int i = 0; i < Skeleton.JOINTS_COUNT; i++) {
            boolean isMasterTracked = (master.joints[i].trackingState == JointTrackingState.Tracked);
            boolean isOtherTracked =  (matched.joints[i].trackingState == JointTrackingState.Tracked);

            if ((isMasterTracked) && (isOtherTracked))
                selection.add(new Pair<Joint, Joint>(master.joints[i], matched.joints[i]));
        }

        return selection;
    }

    private Matrix computeCovariance(List<Pair<Joint, Joint>> matches, Pair<Joint, Joint> centroids) {
    	
        double[][] cov = new double[3][3];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                cov[i][j] = 0;

        double[] cent_a = {
            centroids.first.x,
            centroids.first.y,
            centroids.first.z
        };

        double[] cent_b = {
            centroids.second.x,
            centroids.second.y,
            centroids.second.z
        };

        for (Pair<Joint, Joint> pair: matches) {
            double[] pa = {
                pair.first.x,
                pair.first.y,
                pair.first.z,
            };

            double[] pb = {
                pair.second.x,
                pair.second.y,
                pair.second.z,
            };

            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    cov[i][j] += (pa[i] - cent_a[i]) * (pb[j] - cent_b[j]);

        }

        return new Matrix(cov);
    }

    private Pair<Joint, Joint> computeCentroids(List<Pair<Joint, Joint>> matches)
    {
        Joint pa = new Joint(0, 0, 0);
        Joint pb = new Joint(0, 0, 0);

        for (Pair<Joint, Joint> pair: matches)
        {
            Joint point1 = pair.first;
            pa.x += point1.x;
            pa.y += point1.y;
            pa.z += point1.z;

            Joint point2 = pair.second;
            pb.x += point2.x;
            pb.y += point2.y;
            pb.z += point2.z;
        }

        pa.x /= matches.size();
        pa.y /= matches.size();
        pa.z /= matches.size();
        pb.x /= matches.size();
        pb.y /= matches.size();
        pb.z /= matches.size();

        return new Pair<Joint, Joint>(pa, pb);
    }

    private Matrix absoluteOrientation(List<Pair<Joint, Joint>> matches)
    {
        Pair<Joint, Joint> centroids = computeCentroids(matches);
        Matrix cov = computeCovariance(matches, centroids);

        // Svd step
        SingularValueDecomposition decomp = cov.svd();
        double[][] u = decomp.getU().getArray();
        double[][] vt = decomp.getV().transpose().getArray();
        
        // Rotation
        double[][] rt = new double[3][3];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    rt[i][j] += u[i][k] * vt[k][j];

        Matrix r = new Matrix(rt);
        r = r.transpose();

        double[][] r_arr = r.getArray();
        
        if (r.det() < 0)
        {
        	r_arr[2][0] *= -1;
        	r_arr[2][1] *= -1;
        	r_arr[2][2] *= -1;
        }

        // Translation
        double[][] transform_arr = new double[4][4];
        transform_arr[0][0] = r_arr[0][0];
        transform_arr[1][0] = r_arr[1][0];
        transform_arr[2][0] = r_arr[2][0];
        transform_arr[0][1] = r_arr[0][1];
        transform_arr[1][1] = r_arr[1][1];
        transform_arr[2][1] = r_arr[2][1];
        transform_arr[0][2] = r_arr[0][2];
        transform_arr[1][2] = r_arr[1][2];
        transform_arr[2][2] = r_arr[2][2];
        
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

    public Matrix calibrate(Skeleton master, Skeleton matched)
    {
        // Step 1: Selection
        List<Pair<Joint, Joint>> selection = selectPoints(matched, master);

    	/*
        List<Pair<Joint, Joint>> test = new LinkedList<Pair<Joint, Joint>>();
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

        for (int i = 0; i < 10; i++)
        {
            Joint pa = new Joint((float)A[i][0], (float)A[i][1], (float)A[i][2]);
            Joint pb = new Joint((float)B[i][0], (float)B[i][1], (float)B[i][2]);
            test.add(new Pair<Joint, Joint>(pa, pb));
        }
        */
        
        // Step 2: Absolute Orientation
        Matrix transform = absoluteOrientation(selection);

        return transform;
    }
    
    /** 
     * Applies transformation to skeleton
     * @param skel
     * @param transformation
     */
    public Skeleton transform(Skeleton skel, Matrix transformation) {
    	
    	for (int i = 0; i < Skeleton.JOINTS_COUNT; i++) {
            
    		Joint joint = skel.joints[i];
    		Matrix jointPos = new Matrix(new double[][]{
    			{joint.x}, {joint.y}, {joint.z}, {1}
    		} );
    		
    		Matrix transformedJoint = transformation.times(jointPos);
    		joint.x = (float)transformedJoint.get(0, 0);
    		joint.y = (float)transformedJoint.get(1, 0);
    		joint.z = (float)transformedJoint.get(2, 0);
        }
        return skel;
    }
/*
    private static class Rotation {

        // Algorithms implemented according to:
        // https://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.pdf

        private float arctan2(float y, float x) {
            if ((Math.Abs(x) < EPSILON) && (y > 0))
                return (float)(Math.PI / 2);
            else if ((Math.Abs(x) < EPSILON) && (y < 0))
                return (float)(-Math.PI / 2);
            else if (x > 0)
                return (float)Math.Atan(y / x);
            else if (x < 0)
                return (float)(Math.Atan(y / x) + Math.PI);

            return 0; // Undefined for (0,0)
        }

        public Matrix extractRotation(Matrix homogeneousTransform) {

            // Homogeneous matrix is 4x4, rotation matrix is the top left 3x3 sub-matrix
            return homogeneousTransform.getMatrix(0, 0, 2, 2);
        }

        public Matrix rotationMatToAxisAngle(Matrix R)
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

                // TODO: Fix that
                if ((u.norm2() == Math.PI) && (((u[0, 0] == 0) && (u[1, 0] == 0) && (u[2, 0] < 0)) ||
                ((u[0, 0] == 0) && (u[1, 0] < 0)) ||
                (u[0, 0] < 0)))
                {
                    return u * (-1);
                }
                else
                {
                    return u;
                }
            }

            float theta = arctan2(s, c);

            // Sin theta != 0
            if (Math.Abs(Math.Sin(theta)) > EPSILON)
            {
                Matrix u = p * (1 / s);
                return u * theta;
            }

            return new Matrix(4, 1); // Undefined - shouldn't happen
        }

        static public Matrix4 axisAngletoRotationMat(Vector4 r)
        {
            // -- Calculate using the Rodrigues formula --
            // r is an axis angle vector in 3d space

            // The angle of rotation is encoded in the norm of axisAngle
            double theta = Math.Sqrt(r.X * r.X +
                    r.Y * r.Y +
                    r.Z * r.Z);

            Matrix4 rotationMat;

            // Make sure <= PI
            if (theta > Math.PI)
                theta -= 2 * Math.PI;

            // Rotation matrix is identity
            if (Math.Abs(theta) <= EPSILON)
            {
                rotationMat = Matrix4.Identity;
                rotationMat.M44 = 0;
                return rotationMat;
            }

            // Define: u = r / theta
            Vector4 u = new Vector4();
            u.X = (float)(r.X / theta);
            u.Y = (float)(r.Y / theta);
            u.Z = (float)(r.Z / theta);

            rotationMat = new Matrix4();

            float oneMinusCosTheta = (float)(1 - Math.Cos(theta));
            float halfSinTheta = (float)(Math.Sin(theta));
            rotationMat.M11 = (float)Math.Cos(theta) + oneMinusCosTheta * u.X * u.X;
            rotationMat.M12 = oneMinusCosTheta * u.X * u.Y + halfSinTheta * (-u.Z);
            rotationMat.M13 = oneMinusCosTheta * u.X * u.Z + halfSinTheta * (u.Y);
            rotationMat.M21 = oneMinusCosTheta * u.Y * u.X + halfSinTheta * (u.Z);
            rotationMat.M22 = (float)Math.Cos(theta) + oneMinusCosTheta * u.Y * u.Y;
            rotationMat.M23 = oneMinusCosTheta * u.Y * u.Z + halfSinTheta * (-u.X);
            rotationMat.M31 = oneMinusCosTheta * u.Z * u.X + halfSinTheta * (-u.Y);
            rotationMat.M32 = oneMinusCosTheta * u.Z * u.Y + halfSinTheta * (u.X);
            rotationMat.M33 = (float)Math.Cos(theta) + oneMinusCosTheta * u.Z * u.Z;

            rotationMat.M14 = 0;
            rotationMat.M24 = 0;
            rotationMat.M34 = 0;
            rotationMat.M41 = 0;
            rotationMat.M42 = 0;
            rotationMat.M43 = 0;
            rotationMat.M44 = 0;

            return rotationMat;
        }
    }
    */
}
