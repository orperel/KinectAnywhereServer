package org.kinectanywhereandroid.recorder;


import android.content.Context;

import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.IKinectQueueConsumer;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Replays recorded sessions as if the KinectQueueWorkerThread have handled them.
 */
public class KinectQueueReplayMock extends Thread implements IKinectQueueConsumer {

    private Queue<SingleFrameData> _replayedFrames;
    List<WeakReference<IKinectFrameEventListener>> _listeners;

    public KinectQueueReplayMock(Context appContext) {

        _listeners = new LinkedList<>();

        String fileName = SkelRecorder.RECORD_FILENAME + SkelRecorder.RECORD_FILENAME_EXT;
        FileInputStream fis = null;
        ObjectInputStream is = null;

        try {
            fis = appContext.openFileInput(fileName);
            is = new ObjectInputStream(fis);
            _replayedFrames = (Queue<SingleFrameData>) is.readObject();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (is != null)
                    is.close();
                if (fis != null)
                    fis.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void replay() {

        while (!_replayedFrames.isEmpty()) {

            SingleFrameData frame = _replayedFrames.poll();

            if (frame == null)
                continue; // Invalid frame was discarded

            // Notify listeners (painter, calibration, etc)
            for (WeakReference<IKinectFrameEventListener> weakListener: _listeners) {
                IKinectFrameEventListener listener = weakListener.get();
                if (listener != null)
                    listener.handle(frame);
            }

            try {
                Thread.currentThread().sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void run() {

        try {
            replay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(IKinectFrameEventListener listener) {
        _listeners.add(new WeakReference<>(listener));
    }

    @Override
    public void activate() {

        start();
    }

    @Override
    public void deactivate() {
    }
}
