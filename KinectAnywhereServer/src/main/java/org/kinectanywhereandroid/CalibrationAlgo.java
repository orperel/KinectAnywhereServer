package org.kinectanywhereandroid;

import java.util.LinkedList;
import java.util.List;

import org.kinectanywhereandroid.Joint.JointTrackingState;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class CalibrationAlgo {

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
    			{joint.x}, {joint.y}, {joint.z}, {0} 
    		} );
    		
    		Matrix transformedJoint = transformation.times(jointPos);
    		joint.x = (float)transformedJoint.get(0, 0);
    		joint.y = (float)transformedJoint.get(1, 0);
    		joint.z = (float)transformedJoint.get(2, 0);
        }
        return skel;
    }
}
