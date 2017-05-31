package org.kinectanywhereandroid.recorder;

import android.content.Context;
import android.util.Log;

import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.util.Pair;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class UDPServerThreadMock extends Thread {

    public final static String RECORD_FILENAME = "calibration_sess";
    public final static String RECORD_FILENAME_EXT = "rc2";
    private final static String TAG = "UDPServerThreadMock";

    private long _startTime;
    private Queue<RecordedPacket> _recordings;
    private Context _appContext;
    private Map<String, RemoteKinect> _kinectDict; // Mapping of mocked clients

    private boolean _isRecord;

    public UDPServerThreadMock(Context appContext, boolean isRecord) {

        _startTime = System.currentTimeMillis();
        _appContext = appContext;
        _recordings = new LinkedList<>();

        _isRecord = isRecord;

        if (!_isRecord) {
            loadReplay();
            _kinectDict = new HashMap<>();
            DataHolder.INSTANCE.save(DataHolderEntry.CONNECTED_HOSTS, _kinectDict); // Share hosts list with rest of app modules
        }
    }

    public void recordSkels(String hostname, List<Skeleton> skelList) {

        if (!_isRecord)
            return;

        long now = System.currentTimeMillis();
        long delta = now - _startTime;

        _recordings.offer(new RecordedPacket(delta, hostname, skelList));
    }

    /**
     * End of recording - dump all accumulated frames to an external file
     */
    public void finishRecording() {

        if (!_isRecord)
            return;

        String fileName = RECORD_FILENAME + RECORD_FILENAME_EXT;
        FileOutputStream fos = null;
        ObjectOutputStream os = null;

        try {
            fos = _appContext.openFileOutput(fileName, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(_recordings);

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        finally {
            try {
                if (os != null)
                    os.close();
                if (fos != null)
                    fos.close();
            }
            catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
    }

    private void loadReplay() {

        String fileName = RECORD_FILENAME + RECORD_FILENAME_EXT;
        FileInputStream fis = null;
        ObjectInputStream is = null;

        try {
            fis = _appContext.openFileInput(fileName);
            is = new ObjectInputStream(fis);
            _recordings = (Queue<RecordedPacket>) is.readObject();

        }
        catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        finally {
            try {
                if (is != null)
                    is.close();
                if (fis != null)
                    fis.close();
            }
            catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
    }

    public void startReplay() {

        _startTime = System.currentTimeMillis(); // Update start time
        start();
    }

    @Override
    public void run() {

        try {

            while (true) {

                // Add to queue only when enough time passed (delta)
                long currDelta = System.currentTimeMillis() - _startTime;
                if (currDelta >= _recordings.peek().delta) {

                    RecordedPacket nextPacket = _recordings.poll();

                    String hostname = nextPacket.hostname;
                    if (_kinectDict.get(hostname) == null) {
                        _kinectDict.put(hostname, new RemoteKinect());
                    }

                    RemoteKinect remoteKinect = _kinectDict.get(hostname);
                    remoteKinect.lastBeacon = System.currentTimeMillis();
                    remoteKinect.skeletonQueue.add(nextPacket.skels);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    // Single packet
    private class RecordedPacket implements Serializable {

        private static final long serialVersionUID = 1L;

        long delta;
        String hostname;
        List<Skeleton> skels;

        public RecordedPacket(long delta, String hostname, List<Skeleton> skels) {

            this.delta = delta;
            this.hostname = hostname;
            this.skels = skels;
        }
    }
}
