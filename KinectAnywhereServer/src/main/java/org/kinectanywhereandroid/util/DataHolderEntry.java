package org.kinectanywhereandroid.util;

import org.kinectanywhereandroid.framework.RemoteKinect;

import java.util.Map;

/**
 * Single entry in DataHolder container
 * @param <T> Data entry object type
 */
public class DataHolderEntry<T> {

    public final static DataHolderEntry<Map<String, RemoteKinect>> CONNECTED_HOSTS = new DataHolderEntry<>();
}
