package org.kinectanywhereandroid;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RemoteKinect implements Serializable {
    Queue<List<Skeleton>> skeletonQueue = new LinkedList<>();
    double lastBeacon = System.currentTimeMillis();
    boolean isON = true;
}
