package org.kinectanywhereandroid.model;

import org.kinectanywhereandroid.model.Joint;

import java.io.Serializable;

public class Skeleton implements Serializable {

    private static final long serialVersionUID = 1L;

	public static final int JOINTS_COUNT = 20;
	
    public Joint[] joints ;
    public int trackingId;
    private double timestamp;

    public Skeleton() {

        joints = new Joint[JOINTS_COUNT];
    }

    public Skeleton(Skeleton srcSkel) {

        joints = new Joint[JOINTS_COUNT];

        for (int i = 0; i < JOINTS_COUNT; i++) {
            joints[i] = srcSkel.joints[i].clone();
        }

        trackingId = srcSkel.trackingId;
        timestamp = srcSkel.timestamp;
    }

    public void setTimestamp(long timestamp) {

        this.timestamp = timestamp;
    }

    public long getTimestamp() {

        return (long)timestamp;
    }
}
