package org.kinectanywhereandroid.algorithm;

import org.kinectanywhereandroid.model.Skeleton;

/**
 * Transforms skeletons between cameras coordiante systems
 */
public interface CoordinatesTransformer {

    public Skeleton transform(String fromCamera, String toCamera, Skeleton skeleton);
}
