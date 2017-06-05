package org.kinectanywhereandroid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.kinectanywhereandroid.algorithm.CalibrationAlgo;
import org.kinectanywhereandroid.algorithm.SkelCalibrator;
import org.kinectanywhereandroid.framework.IKinectDataConsumer;
import org.kinectanywhereandroid.framework.KinectQueueWorkerThread;
import org.kinectanywhereandroid.framework.KinectSampleWorkerThread;
import org.kinectanywhereandroid.framework.QueuedSamplesKinect;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.framework.SingleSampleKinect;
import org.kinectanywhereandroid.network.UdpBroadcastingThread;
import org.kinectanywhereandroid.network.UdpServerThread;
import org.kinectanywhereandroid.recorder.UDPServerThreadMock;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.visual.SkelPainter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private enum AppMode {
        NORMAL,
        RECORD,
        REPLAY
    }

    private enum RemoteKinectMode {

        QUEUE,  // Process all samples from a queue
        SAMPLE  // Always keep the latest sample only
    }

    // -- App settings --
    private AppMode mode = AppMode.NORMAL;
    private RemoteKinectMode dataProcessingMode = RemoteKinectMode.SAMPLE;

    TextView infoIp;
    TextView textViewState, textViewPrompt;
    Menu _masterCameraMenu;
    Menu _calibrationModeMenu;
    Menu _activateClientMenu;
    Menu _shutdownMenu;
    MenuItem _showAvgSkelMenuItem;

    static final int UDP_SERVER_PORT = 11000;
    static final int UDP_BROADCATING_PORT = 5000;

    UdpServerThread udpServerThread;
    UdpBroadcastingThread udpBroadcastingThread;
    UDPServerThreadMock mockServer;
    IKinectDataConsumer kinectDataConsumer;
    SkelPainter painter;
    SkelCalibrator calibrator;
    UDPServerThreadMock recorder;

    ArrayList<String> _menuClients;
    Constructor<? extends RemoteKinect> _remoteKinectCtor;

    private enum MenuOptions {

        MASTER_CAMERA_GROUP(0),
        CALIBRATION_MODE_GROUP(1),
        SHOW_ESTIMATED_SKEL(2),
        ACTIVATE_CLIENT_GROUP(3),
        SHUTDOWN_MENU_GROUP(4),

        CALIBRATION_MODE_PER_FRAME(5),
        CALIBRATION_MODE_TEMPORAL_APPROX(6),
        CALIBRATION_MODE_BEST_IN_CLASS(7),
        CALIBRATION_MODE_KALMAN(8);

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
        // TODO: delete uncomment
//        infoIp = (TextView) findViewById(R.id.infoip);
//        textViewState = (TextView)findViewById(R.id.state);
//        textViewPrompt = (TextView)findViewById(R.id.prompt);
        _menuClients = new ArrayList<>();

//        try {
//            infoIp.setText(Utils.getIpAddress() + ":" + String.valueOf(UDP_SERVER_PORT));
//        } catch (SocketException e) {
//            Log.e(TAG, e.getLocalizedMessage());
//        }
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
        _showAvgSkelMenuItem = menu.add(MenuOptions.SHOW_ESTIMATED_SKEL.id, 0, Menu.NONE, "Show Estimated Skeleton").
                                        setCheckable(true).setChecked(false);
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_PER_FRAME.id,
                                 Menu.NONE, "Per Frame");
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_TEMPORAL_APPROX.id,
                                 Menu.NONE, "Temporal Approximation");
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_BEST_IN_CLASS.id,
                                 Menu.NONE, "Best In Class");
        _calibrationModeMenu.add(MenuOptions.CALIBRATION_MODE_GROUP.id, MenuOptions.CALIBRATION_MODE_KALMAN.id,
                                 Menu.NONE, "Kalman Approximation");
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
                        setCheckable(true).setChecked(connectedHosts.get(hostname).isON);
                _shutdownMenu.add(MenuOptions.SHUTDOWN_MENU_GROUP.id, optionId, Menu.NONE, hostname);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {

        updateMenusWithConnectedClients();

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Automatically choose the first available camera and set as master if no master is currently set.
     * Otherwise nothing happens
     */
    private void setMasterAutomatically() {

        if (_menuClients.size() > 0) {

            String master = DataHolder.INSTANCE.retrieve(DataHolderEntry.MASTER_CAMERA);

            if (master == null) {
                master = _menuClients.get(0);
                DataHolder.INSTANCE.save(DataHolderEntry.MASTER_CAMERA, master);

                Toast.makeText(this, master + " automatically set as master camera.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int groupId = item.getGroupId();
        int id = item.getItemId();
        MenuOptions group = MenuOptions.getOption(groupId);

        switch (group) {
            case MASTER_CAMERA_GROUP: {

                if (item.hasSubMenu())
                    return true; // Avoid sub menu setting values

                String master = null;
                if (id != 0)
                    master = _menuClients.get(id - 1);

                // If no master is picked reset show estimated skeleton
                if (master == null) {
                    DataHolder.INSTANCE.save(DataHolderEntry.SHOW_AVERAGE_SKELETONS, false);
                    _showAvgSkelMenuItem.setChecked(false);
                }

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
                else if (id == MenuOptions.CALIBRATION_MODE_BEST_IN_CLASS.id) {
                    DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE,
                                             CalibrationAlgo.CalibrationMode.BEST_IN_CLASS);
                }
                else if (id == MenuOptions.CALIBRATION_MODE_KALMAN.id) {
                    DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE,
                                             CalibrationAlgo.CalibrationMode.KALMAN);
                }

                // If there are connected clients and there is no master set, choose the first camera
                // so the calibration process shows some results on screen
                setMasterAutomatically();

                return true;
            }
            case SHOW_ESTIMATED_SKEL: {

                boolean isChecked = DataHolder.INSTANCE.retrieve(DataHolderEntry.SHOW_AVERAGE_SKELETONS);
                item.setChecked(!isChecked);
                DataHolder.INSTANCE.save(DataHolderEntry.SHOW_AVERAGE_SKELETONS, item.isChecked());

                if (item.isChecked()) {

                    // If there are connected clients and there is no master set, choose the first camera
                    // so the calibration process shows some results on screen
                    setMasterAutomatically();
                }

                return true;
            }
            case ACTIVATE_CLIENT_GROUP: {
                String client = _menuClients.get(id - 1);
                Map<String, RemoteKinect> connectedHosts = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);

                Queue<String> queue = DataHolder.INSTANCE.retrieve(DataHolderEntry.BROADCASTING_QUEUE);

                String msg = client;
                if (connectedHosts.get(client).isON) {
                    msg += "=OFF";
                } else {
                    msg += "=ON";
                }

                for (int i = 0; i < 3; i++) {
                    queue.add(msg);
                }

                return true;
            }
            case SHUTDOWN_MENU_GROUP: {
                String client = _menuClients.get(id - 1);
                Map<String, RemoteKinect> connectedHosts = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);

                Queue<String> queue = DataHolder.INSTANCE.retrieve(DataHolderEntry.BROADCASTING_QUEUE);

                String msg = client + "=SHUTDOWN";

                for (int i = 0; i < 3; i++) {
                    queue.add(msg);
                }

                return true;
            }
            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void createClientDataHandlerPipeline() {

        try {
            if (dataProcessingMode == RemoteKinectMode.QUEUE) {
                _remoteKinectCtor = QueuedSamplesKinect.class.getConstructor();
                kinectDataConsumer =  new KinectQueueWorkerThread();
            }
            else if (dataProcessingMode == RemoteKinectMode.SAMPLE) {
                _remoteKinectCtor = SingleSampleKinect.class.getConstructor();
                kinectDataConsumer =  new KinectSampleWorkerThread();
            }
            else {
                throw new IllegalStateException("Incorrect enum value for RemoteKinectMode");
            }
        }
        catch (NoSuchMethodException e) {
            Log.e(TAG, "Error fetching RemoteKinect sub class constructor");
        }
        DataHolder.INSTANCE.save(DataHolderEntry.REMOTE_KINECT_CTOR, _remoteKinectCtor);
    }

    @Override
    protected void onStart() {

        createClientDataHandlerPipeline();

        if (mode != AppMode.REPLAY) {

            boolean isRecord = (mode == AppMode.RECORD); // TODO: Delete this
            udpServerThread = new UdpServerThread(UDP_SERVER_PORT, this, isRecord);
            udpServerThread.start();
        }
        else {
            mockServer = new UDPServerThreadMock(this.getApplicationContext(), false);
        }

        udpBroadcastingThread = new UdpBroadcastingThread(UDP_BROADCATING_PORT);
        udpBroadcastingThread.start();

        DataHolder.INSTANCE.save(DataHolderEntry.CALIBRATION_MODE, CalibrationAlgo.CalibrationMode.BEST_IN_CLASS);
        DataHolder.INSTANCE.save(DataHolderEntry.SHOW_AVERAGE_SKELETONS, false);
        calibrator = new SkelCalibrator();
        kinectDataConsumer.register(calibrator);

        painter = new SkelPainter(this);
        kinectDataConsumer.register(painter);
        kinectDataConsumer.activate();

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

        if (kinectDataConsumer != null) {
            kinectDataConsumer.deactivate();
            kinectDataConsumer = null;
        }

        super.onStop();
    }

    public void updateState(final String state){
//        textViewState.setText(state);
    }

    public void updatePrompt(final String prompt){
        textViewPrompt.append(prompt);
    }
}