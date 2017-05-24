package org.kinectanywhereandroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.kinectanywhereandroid.algorithm.SkelCalibrator;
import org.kinectanywhereandroid.framework.IKinectQueueConsumer;
import org.kinectanywhereandroid.framework.KinectQueueWorkerThread;
import org.kinectanywhereandroid.network.UdpBroadcastingThread;
import org.kinectanywhereandroid.network.UdpServerThread;
import org.kinectanywhereandroid.network.Utils;
import org.kinectanywhereandroid.recorder.KinectQueueReplayMock;
import org.kinectanywhereandroid.recorder.SkelRecorder;
import org.kinectanywhereandroid.visual.SkelPainter;

import java.net.SocketException;

public class MainActivity extends AppCompatActivity {

    private enum AppMode {
        NORMAL,
        RECORD,
        REPLAY
    }

    private AppMode mode = AppMode.RECORD;

    TextView infoIp;
    TextView textViewState, textViewPrompt;

    static final int UDP_SERVER_PORT = 11000;
    static final int UDP_BROADCATING_PORT = 5000;

    UdpServerThread udpServerThread;
    UdpBroadcastingThread udpBroadcastingThread;
    IKinectQueueConsumer kinectQueueConsumer;
    SkelPainter painter;
    SkelCalibrator calibrator;
    SkelRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        textViewState = (TextView)findViewById(R.id.state);
        textViewPrompt = (TextView)findViewById(R.id.prompt);

        try {
            infoIp.setText(Utils.getIpAddress() + ":" + String.valueOf(UDP_SERVER_PORT));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.hosts:
                Intent myIntent = new Intent(getApplicationContext(), ListKinectsActivity.class);
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        udpServerThread = new UdpServerThread(UDP_SERVER_PORT, this);
        udpServerThread.start();

        udpBroadcastingThread = new UdpBroadcastingThread(UDP_BROADCATING_PORT);
        udpBroadcastingThread.start();

        kinectQueueConsumer = (mode != AppMode.REPLAY) ?
                new KinectQueueWorkerThread(this) :
                new KinectQueueReplayMock(getApplicationContext());

        calibrator = new SkelCalibrator();
        kinectQueueConsumer.register(calibrator);

        painter = new SkelPainter(this);
        kinectQueueConsumer.register(painter);

        if (mode == AppMode.RECORD) {
            recorder = new SkelRecorder(getApplicationContext());
            kinectQueueConsumer.register(recorder);
        }

        kinectQueueConsumer.activate();

        super.onStart();
    }

    @Override
    protected void onStop() {
        if(udpServerThread != null){
            udpServerThread.setRunning(false);
            udpServerThread = null;
        }

        if(udpBroadcastingThread != null){
            udpBroadcastingThread.setRunning(false);
            udpBroadcastingThread = null;
        }

        if (kinectQueueConsumer != null) {
            kinectQueueConsumer.deactivate();
            kinectQueueConsumer = null;
        }

        if (mode == AppMode.RECORD) {
            recorder.finalizeRecording();
        }

        super.onStop();
    }

    public void updateState(final String state){
        textViewState.setText(state);
    }

    public void updatePrompt(final String prompt){
        textViewPrompt.append(prompt);
    }
}