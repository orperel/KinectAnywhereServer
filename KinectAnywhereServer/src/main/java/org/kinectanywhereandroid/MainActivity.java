package org.kinectanywhereandroid;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView infoIp;
    TextView textViewState, textViewPrompt;

    static final int UDP_SERVER_PORT = 11000;
    static final int UDP_BROADCATING_PORT = 5000;

    UdpServerThread udpServerThread;
    UdpBroadcastingThread udpBroadcastingThread;

    Bitmap bg = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bg);
    AnalyticCoordinatesMapper cm = new AnalyticCoordinatesMapper(480, 800);

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
    protected void onStart() {
        udpServerThread = new UdpServerThread(UDP_SERVER_PORT, this);
        udpServerThread.start();

        udpBroadcastingThread = new UdpBroadcastingThread(UDP_BROADCATING_PORT);
        udpBroadcastingThread.start();

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

        super.onStop();
    }

    public void updateState(final String state){
        textViewState.setText(state);
    }

    public void updatePrompt(final String prompt){
        textViewPrompt.append(prompt);
    }

    private void DrawBone(Skeleton skeleton, Joint.JointType jointType0, Joint.JointType jointType1)
    {
        Joint joint0 = skeleton.joints[jointType0.getValue()];
        Joint joint1 = skeleton.joints[jointType1.getValue()];

        // If we can't find either of these joints, exit
        if (joint0.trackingState == Joint.JointTrackingState.NotTracked ||
                joint1.trackingState == Joint.JointTrackingState.NotTracked)
        {
            return;
        }

        // Don't draw if both points are inferred
        if (joint0.trackingState == Joint.JointTrackingState.Inferred &&
                joint1.trackingState == Joint.JointTrackingState.Inferred)
        {
            return;
        }

        Paint paint = new Paint();


        PointF start = cm.MapSkeletonPointToDepthPoint(joint0);
        PointF end = cm.MapSkeletonPointToDepthPoint(joint1);

        // We assume all drawn bones are inferred unless BOTH joints are tracked
        if (joint0.trackingState == Joint.JointTrackingState.Tracked && joint1.trackingState == Joint.JointTrackingState.Tracked)
        {
            paint.setColor(Color.parseColor("#000000"));
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);

        } else {
            paint.setColor(Color.parseColor("#506bd8"));
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }


        LinearLayout ll = (LinearLayout) findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(bg));
    }


    public void drawBonesAndJoints(Skeleton skeleton) {
        // Render Torso
        this.DrawBone(skeleton, Joint.JointType.Head, Joint.JointType.ShoulderCenter);
        this.DrawBone(skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderLeft);
        this.DrawBone(skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderRight);
        this.DrawBone(skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.Spine);
        this.DrawBone(skeleton, Joint.JointType.Spine, Joint.JointType.HipCenter);
        this.DrawBone(skeleton, Joint.JointType.HipCenter, Joint.JointType.HipLeft);
        this.DrawBone(skeleton, Joint.JointType.HipCenter, Joint.JointType.HipRight);

        // Left Arm
        this.DrawBone(skeleton, Joint.JointType.ShoulderLeft, Joint.JointType.ElbowLeft);
        this.DrawBone(skeleton, Joint.JointType.ElbowLeft, Joint.JointType.WristLeft);
        this.DrawBone(skeleton, Joint.JointType.WristLeft, Joint.JointType.HandLeft);

        // Right Arm
        this.DrawBone(skeleton, Joint.JointType.ShoulderRight, Joint.JointType.ElbowRight);
        this.DrawBone(skeleton, Joint.JointType.ElbowRight, Joint.JointType.WristRight);
        this.DrawBone(skeleton, Joint.JointType.WristRight, Joint.JointType.HandRight);

        // Left Leg
        this.DrawBone(skeleton, Joint.JointType.HipLeft, Joint.JointType.KneeLeft);
        this.DrawBone(skeleton, Joint.JointType.KneeLeft, Joint.JointType.AnkleLeft);
        this.DrawBone(skeleton, Joint.JointType.AnkleLeft, Joint.JointType.FootLeft);

        // Right Leg
        this.DrawBone(skeleton, Joint.JointType.HipRight, Joint.JointType.KneeRight);
        this.DrawBone(skeleton, Joint.JointType.KneeRight, Joint.JointType.AnkleRight);
        this.DrawBone(skeleton, Joint.JointType.AnkleRight, Joint.JointType.FootRight);

        // Render Joints
        for (Joint joint : skeleton.joints)
        {
            PointF center = cm.MapSkeletonPointToDepthPoint(joint);

            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#CD5C5C"));

            if (joint.trackingState == Joint.JointTrackingState.Tracked)
            {
                canvas.drawCircle(center.x, center.y, 4, paint);
            }
            else if (joint.trackingState == Joint.JointTrackingState.Inferred)
            {
                canvas.drawCircle(center.x, center.y, 1, paint);
            }
        }
    }

    public void drawSkeletons(final List<Skeleton> skeletonList){
        canvas.drawColor(Color.WHITE);

        for (Skeleton skeleton : skeletonList)
        {
            // Draws the skeleton
            drawBonesAndJoints(skeleton);
        }
    }
}