package org.kinectanywhereandroid;

public class Skeleton {
	
	public static final int JOINTS_COUNT = 20;
	
    Joint[] joints = new Joint[JOINTS_COUNT];
    int trackingId;
    double timestamp;
}
