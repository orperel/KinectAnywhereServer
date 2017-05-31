package org.kinectanywhereandroid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.kinectanywhereandroid.algorithm.CalibrationAlgo;
import org.kinectanywhereandroid.algorithm.SkelCalibrator;
import org.kinectanywhereandroid.framework.IKinectQueueConsumer;
import org.kinectanywhereandroid.framework.KinectQueueWorkerThread;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.network.UdpBroadcastingThread;
import org.kinectanywhereandroid.network.UdpServerThread;
import org.kinectanywhereandroid.network.Utils;
import org.kinectanywhereandroid.recorder.UDPServerThreadMock;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.visual.SkelPainter;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private enum AppMode {
        NORMAL,
        RECORD,
        REPLAY
    }

    private AppMode mode = AppMode.NORMAL;

    TextView infoIp;
    TextView textViewState, textViewPrompt;
    Menu _masterCameraMenu;
    Menu _calibrationModeMenu;
    Menu _activateClientMenu;
    Menu _shutdownMenu;

    static final int UDP_SERVER_PORT = 11000;
    static final int UDP_BROADCATING_PORT = 5000;

    UdpServerThread udpServerThread;
    UdpBroadcastingThread udpBroadcastingThread;
    UDPServerThreadMock mockServer;
    IKinectQueueConsumer kinectQueueConsumer;
    SkelPainter painter;
    SkelCalibrator calibrator;
    UDPServerThreadMock recorder;

    ArrayList<String> _menuClients;

    private enum MenuOptions {

        MASTER_CAMERA_GROUP(0),
        CALIBRATION_MODE_GROUP(1),
        SHOW_ESTIMATED_SKEL(2),
        ACTIVATE_CLIENT_GROUP(3),
        SHUTDOWN_MENU_GROUP(4),

        CALIBRATION_MODE_PER_FRAME(5),
        CALIBRATION_MODE_TEMPORAL_APPROX(6);

        public final int id;

        MenuOptions(int id) {
            this.id = id;
        }

        @Nullable
        public static MenuOptions getOption(int id) {

            for (MenuOptions option: MenuOptions.values()) {
                if (option.id == id)
                    return option;
            }

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        textViewState = (TextView)findViewById(R.id.state);
        textViewPrompt = (TextView)findViewById(R.id.prompt);
        _menuClients = new ArrayList<>();

        try {
            infoIp.setText(Utils.getIpAddress() + ":" + String.valueOf(UDP_SERVER_PORT));
        } catch (SocketException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu);

        _masterCameraMenu = menu.addSubMenu("Master Camera");
        _calibrationModeMenu = menu.addSubMenu("Calibration Mode");
        _activateClientMenu = menu.addSubMenu("Switch Client On/Off");
        _shutdownMenu = menu.addSubMenu("Shutdown Client");

        _masterCameraMenu.add(MenuOptions.MASTER_CAMERA_GROUP.id, 0, Menu.FIRST, "No master");
        menu.add(MenuOptions.SHOW_ESTIMATED_SKEL.id, 0, Menu.NONE, "Show Estimated Skeleton").
                setCheckable(true).setChecked(false);
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_PER_FRAME.id,
                                 Menu.NONE, "Per Frame");
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_TEMPORAL_APPROX.id,
                                 Menu.NONE, "Temporal Approximation");
        _activateClientMenu.add(MenuOptions.ACTIVATE_CLIENT_GROUP.id, 0, Menu.FIRST, "All cameras");
        _shutdownMenu.add(MenuOptions.SHUTDOWN_MENU_GROUP.id, 0, Menu.FIRST, "All cameras");

        return true;
    }

    private void updateMenusWithConnectedClients() {

        Map<String, RemoteKinect> connectedHosts = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);
        if (connectedHosts == null)
            return;

        Set<String> connectedHostnames = connectedHosts.keySet();
        for (String hostname: connectedHostnames) {

            if (!_menuClients.contains(hostname)) {

                _menuClients.add(hostname);
                int optionId = _menuClients.indexOf(hostname) + 1;

                _masterCameraMenu.add(MenuOptions.MASTER_CAMERA_GROUP.id, optionId, Menu.NONE, hostname);
                _activateClientMenu.add(MenuOptions.ACTIVATE_CLIENT_GROUP.id, optionId, Menu.NONE, hostname).
                        setCheckable(true).setChecked(true);
                _shutdownMenu.add(MenuOptions.SHUTDOWN_MENU_GROUP.id, optionId, Menu.NONE, hostname);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {

        updateMenusWithConnectedClients();

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int groupId = item.getGroupId();
        int id = item.getItemId();
        MenuOptions group = MenuOptions.getOption(groupId);

        switch (group) {
            case MASTER_CAMERA_GROUP: {

                String master = null;
                if (id != 0)
                    master = _menuClients.get(id - 1);

                DataHolder.INSTANCE.save(DataHolderEntry.MASTER_CAMERA, master);
                return true;
            }
            case CALIBRATION_MODE_GROUP: {

                if (id == MenuOptions.CALIBRATION_MODE_PER_FRAME.id) {
                    DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE,
                                             CalibrationAlgo.CalibrationMode.PER_FRAME);
                }
                else if (id == MenuOptions.CALIBRATION_MODE_TEMPORAL_APPROX.id) {
                    DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE,
                                             CalibrationAlgo.CalibrationMode.FIRST_ORDER_TEMPORAL_APPROX);
                }

                return true;
            }
            case SHOW_ESTIMATED_SKEL: {

                return true;
            }
            case ACTIVATE_CLIENT_GROUP: {

                return true;
            }
            case SHUTDOWN_MENU_GROUP: {

                return true;
            }
            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onStart() {

        if (mode != AppMode.REPLAY) {

            boolean isRecord = (mode == AppMode.RECORD); // TODO: Delete this
            udpServerThread = new UdpServerThread(UDP_SERVER_PORT, this, isRecord);
            udpServerThread.start();

            udpBroadcastingThread = new UdpBroadcastingThread(UDP_BROADCATING_PORT);
            udpBroadcastingThread.start();
        }
        else {
            mockServer = new UDPServerThreadMock(this.getApplicationContext(), false);
        }

        kinectQueueConsumer =  new KinectQueueWorkerThread();

        DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE, CalibrationAlgo.CalibrationMode.PER_FRAME);
        calibrator = new SkelCalibrator();
        kinectQueueConsumer.register(calibrator);

        painter = new SkelPainter(this);
        kinectQueueConsumer.register(painter);
        kinectQueueConsumer.activate();

        if (mockServer != null)
            mockServer.startReplay();

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

        super.onStop();
    }

    public void updateState(final String state){
        textViewState.setText(state);
    }

    public void updatePrompt(final String prompt){
        textViewPrompt.append(prompt);
    }
}