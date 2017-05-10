package org.kinectanywhereandroid;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    TextView infoIp, infoPort;
    TextView textViewState, textViewPrompt;

    static final int UdpServerPORT = 11000;
    UdpServerThread udpServerThread;

    Bitmap bg = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bg);
    AnalyticCoordinatesMapper cm = new AnalyticCoordinatesMapper(480, 800);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);
        textViewState = (TextView)findViewById(R.id.state);
        textViewPrompt = (TextView)findViewById(R.id.prompt);

        infoIp.setText(getIpAddress());
        infoPort.setText(String.valueOf(UdpServerPORT));

        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawRect(100, 100, 200, 200, paint);
        LinearLayout ll = (LinearLayout) findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(bg));
    }

    @Override
    protected void onStart() {
        udpServerThread = new UdpServerThread(UdpServerPORT, this);
        udpServerThread.start();
        super.onStart();
    }

    @Override
    protected void onStop() {
        if(udpServerThread != null){
            udpServerThread.setRunning(false);
            udpServerThread = null;
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
        paint.setColor(Color.parseColor("#CD5C5C"));

        PointF start = cm.MapSkeletonPointToDepthPoint(joint0);
        PointF end = cm.MapSkeletonPointToDepthPoint(joint1);

        canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        LinearLayout ll = (LinearLayout) findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(bg));
//        // We assume all drawn bones are inferred unless BOTH joints are tracked
//        Pen drawPen = this.inferredBonePen;
//        if (joint0.TrackingState == JointTrackingState.Tracked && joint1.TrackingState == JointTrackingState.Tracked)
//        {
//            drawPen = this.trackedBonePen;
//        }
//
//        drawingContext.DrawLine(drawPen, this.SkeletonPointToScreen(joint0.Position), this.SkeletonPointToScreen(joint1.Position));
    }

    public void drawSkeleton(final Skeleton skeleton){
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
//            Brush drawBrush = null;
//
//            if (joint.TrackingState == JointTrackingState.Tracked)
//            {
//                drawBrush = this.trackedJointBrush;
//            }
//            else if (joint.TrackingState == JointTrackingState.Inferred)
//            {
//                drawBrush = this.inferredJointBrush;
//            }
//
//            if (drawBrush != null)
//            {
//                drawingContext.DrawEllipse(drawBrush, null, this.SkeletonPointToScreen(joint.Position), JointThickness, JointThickness);
//            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
}