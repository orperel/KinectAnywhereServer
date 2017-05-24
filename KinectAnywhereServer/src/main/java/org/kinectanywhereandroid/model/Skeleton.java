package org.kinectanywhereandroid.model;

import org.kinectanywhereandroid.model.Joint;

import java.io.Serializable;

public class Skeleton implements Serializable {

    private static final long serialVersionUID = 1L;

	public static final int JOINTS_COUNT = 20;
	
    public Joint[] joints = new Joint[JOINTS_COUNT];
    public int trackingId;
    public double timestamp;
}
