package org.kinectanywhereandroid.recorder;

import android.content.Context;

import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.SingleFrameData;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;


/**
 * Records the current session data to a file for later replay analysis
 */
public class SkelRecorder implements IKinectFrameEventListener {

    public final static String RECORD_FILENAME = "calibration_sess";
    public final static String RECORD_FILENAME_EXT = "rc2";

    private Queue<SingleFrameData> _recordedFrames;
    private Context _appContext;

    public SkelRecorder(Context appContext) {
        _appContext = appContext;
    }

    @Override
    public void handle(SingleFrameData frame) {

        _recordedFrames.offer(frame);
    }

    public void finalizeRecording() {

        String fileName = RECORD_FILENAME + RECORD_FILENAME_EXT;
        FileOutputStream fos = null;
        ObjectOutputStream os = null;

        try {
            fos = _appContext.openFileOutput(fileName, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(_recordedFrames);

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (os != null)
                    os.close();
                if (fos != null)
                    fos.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
