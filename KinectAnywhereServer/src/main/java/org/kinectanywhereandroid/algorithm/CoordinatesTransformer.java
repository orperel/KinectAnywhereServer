package org.kinectanywhereandroid.algorithm;

import org.kinectanywhereandroid.model.Skeleton;

/**
 * Transforms skeletons between cameras coordiante systems
 */
public interface CoordinatesTransformer {

    /**
     * Transforms Skeleton joints from coordinates system of fromCamera to toCamera
     * @param fromCamera
     * @param toCamera
     * @param skeleton
     * @return Skeleton in coordinate system of toCamera
     */
    public Skeleton transform(String fromCamera, String toCamera, Skeleton skeleton);
}
