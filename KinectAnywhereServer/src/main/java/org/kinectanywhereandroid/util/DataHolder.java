package org.kinectanywhereandroid.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton for sharing data between Android activities
 */
public enum DataHolder {

    /** Single instance of DataHolder*/
    INSTANCE;

    Map<DataHolderEntry, WeakReference<Object>> data = new HashMap<>();

    DataHolder() {
        // Singleton ctor
    }

    public void save(DataHolderEntry id, Object object) {
        data.put(id, new WeakReference<>(object));
    }

    public <T> T retrieve(DataHolderEntry<T> id) {
        WeakReference<Object> objectWeakReference = data.get(id);
        if (objectWeakReference == null) {
            return null;
        }
        return (T)objectWeakReference.get();
    }
}
