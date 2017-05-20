package org.kinectanywhereandroid;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RemoteKinect {
    Queue<List<Skeleton>> skeletonQueue = new LinkedList<>();
    double lastBeacon = System.currentTimeMillis();
    boolean isON = true;
}
