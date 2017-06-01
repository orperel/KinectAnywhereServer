package org.kinectanywhereandroid.visual;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.LinearLayout;

import org.kinectanywhereandroid.MainActivity;
import org.kinectanywhereandroid.R;
import org.kinectanywhereandroid.algorithm.CoordinatesTransformer;
import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;
import org.kinectanywhereandroid.util.Pair;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;

/**
 * Manages visualization of calibration process & current skeleton transformation on canvas.
 */
public class SkelPainter implements IKinectFrameEventListener {

    private final static int FPS_DELTA = 60; // Minimal amount of milliseconds between adjacent renderings
    private final static int CANVAS_WIDTH = 480;
    private final static int CANVAS_HEIGHT = 800;

    private MainActivity _activity;
    private Bitmap _bg;
    private Canvas _canvas;
    private AnalyticCoordinatesMapper _cm;
    private  ColorsPalette[] COLOR_KITS;
    private int lastKitIndex;
    private long prevTimestamp;

    public ColorsPalette nextColorKit() {

        // Repeat used kits if we're out of available kits
        if (lastKitIndex >= 7)
            lastKitIndex = 0;

        return COLOR_KITS[lastKitIndex++];
    }

    /** Assigned colors to connected Kinect cameras */
    private Map<String, ColorsPalette> _camerasColorKit;

    public SkelPainter(MainActivity activity) {
        _activity = activity;
        _cm = new AnalyticCoordinatesMapper(CANVAS_WIDTH, CANVAS_HEIGHT);
        _bg = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
        _canvas = new Canvas(_bg);
        _camerasColorKit = new HashMap<>();

        ColorsPalette CAM0 = new ColorsPalette().setJointsColor(Color.RED).setBonesColor(ColorsPalette.DARKRED);
        ColorsPalette CAM1 = new ColorsPalette().setJointsColor(Color.BLUE).setBonesColor(ColorsPalette.DARKBLUE);
        ColorsPalette CAM2 = new ColorsPalette().setJointsColor(Color.GREEN).setBonesColor(ColorsPalette.DARKGREEN);
        ColorsPalette CAM3 = new ColorsPalette().setJointsColor(Color.YELLOW).setBonesColor(ColorsPalette.DARKYELLOW);
        ColorsPalette CAM4 = new ColorsPalette().setJointsColor(Color.CYAN).setBonesColor(ColorsPalette.DARKCYAN);
        ColorsPalette CAM5 = new ColorsPalette().setJointsColor(Color.MAGENTA).setBonesColor(ColorsPalette.DARKMAGENTA);
        ColorsPalette CAM6 = new ColorsPalette().setJointsColor(ColorsPalette.ORANGE).setBonesColor(ColorsPalette.DARKORANGE);
        ColorsPalette CAM7 = new ColorsPalette().setJointsColor(Color.WHITE).setBonesColor(Color.GRAY);

        COLOR_KITS = new ColorsPalette[]{ CAM0, CAM1, CAM2, CAM3, CAM4, CAM5, CAM6, CAM7 };

        lastKitIndex = 0;
        prevTimestamp = 0;
    }

    @Override
    public void handle(final SingleFrameData frame) {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - prevTimestamp > FPS_DELTA) {
            prevTimestamp = currentTimestamp;

            _activity.runOnUiThread( new Runnable() {

                @Override
                public void run() {
                    drawSkeletons(frame, _canvas);
                }
            });
        }
    }

    private void DrawBone(Canvas canvas, Skeleton skeleton,
                          Joint.JointType jointType0, Joint.JointType jointType1, ColorsPalette colorKit)
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


        PointF start = _cm.MapSkeletonPointToDepthPoint(joint0);
        PointF end = _cm.MapSkeletonPointToDepthPoint(joint1);

        // We assume all drawn bones are inferred unless BOTH joints are tracked
        if (joint0.trackingState == Joint.JointTrackingState.Tracked && joint1.trackingState == Joint.JointTrackingState.Tracked)
        {
            canvas.drawLine(start.x, start.y, end.x, end.y, colorKit.bonesPaint);

        } else {
            canvas.drawLine(start.x, start.y, end.x, end.y, colorKit.untrackedBonesPaint);
        }
    }


    public void drawBonesAndJoints(Skeleton skeleton, Canvas canvas, ColorsPalette colorsKit) {

        // Render Torso
        this.DrawBone(canvas, skeleton, Joint.JointType.Head, Joint.JointType.ShoulderCenter, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderRight, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.Spine, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.Spine, Joint.JointType.HipCenter, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.HipCenter, Joint.JointType.HipLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.HipCenter, Joint.JointType.HipRight, colorsKit);

        // Left Arm
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderLeft, Joint.JointType.ElbowLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.ElbowLeft, Joint.JointType.WristLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.WristLeft, Joint.JointType.HandLeft, colorsKit);

        // Right Arm
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderRight, Joint.JointType.ElbowRight, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.ElbowRight, Joint.JointType.WristRight, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.WristRight, Joint.JointType.HandRight, colorsKit);

        // Left Leg
        this.DrawBone(canvas, skeleton, Joint.JointType.HipLeft, Joint.JointType.KneeLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.KneeLeft, Joint.JointType.AnkleLeft, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.AnkleLeft, Joint.JointType.FootLeft, colorsKit);

        // Right Leg
        this.DrawBone(canvas, skeleton, Joint.JointType.HipRight, Joint.JointType.KneeRight, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.KneeRight, Joint.JointType.AnkleRight, colorsKit);
        this.DrawBone(canvas, skeleton, Joint.JointType.AnkleRight, Joint.JointType.FootRight, colorsKit);

        // Render Joints
        for (Joint joint : skeleton.joints)
        {
            // Project 3d point to 2d screen coordinates
            PointF center = _cm.MapSkeletonPointToDepthPoint(joint);

            if (joint.trackingState == Joint.JointTrackingState.Tracked)
            {
                canvas.drawCircle(center.x, center.y, 4f, colorsKit.jointsPaint);
            }
            else if (joint.trackingState == Joint.JointTrackingState.Inferred)
            {
                canvas.drawCircle(center.x, center.y, 2f, colorsKit.untrackedJointsPaint);
            }
        }
    }

    private ColorsPalette getCameraColorKit(String cameraName) {

        if (!_camerasColorKit.containsKey(cameraName))
            _camerasColorKit.put(cameraName, nextColorKit());

        return _camerasColorKit.get(cameraName);
    }

    public void drawHosts(Canvas canvas, String masterCamera) {
        Paint paint = new Paint();
        canvas.drawPaint(paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16);

        int i = 0;
        Map<String, RemoteKinect> kinectDict = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);

        for (Map.Entry<String, RemoteKinect> entry : kinectDict.entrySet()) {

            String cameraName = entry.getKey();
            ColorsPalette cameraColorKit = getCameraColorKit(cameraName);

            if (cameraName.equals(masterCamera)) { // Master camera name in bold
                paint.setTypeface(Typeface.DEFAULT_BOLD);
            }
            else { // Non-master camera
                paint.setTypeface(Typeface.DEFAULT);
            }

            canvas.drawText(cameraName, 30, 30 + i * 10, cameraColorKit.jointsPaint);

            if (entry.getValue().isON) {
                paint.setColor(Color.GREEN);
            } else {
                paint.setColor(Color.RED);
            }

            canvas.drawCircle(15, 23 + i * 10, 6, paint);
            i++;
        }
    }

    public void drawSkeletons(SingleFrameData frame, Canvas canvas){

        canvas.drawColor(ColorsPalette.CANVAS_BG_COLOR);

        String masterCamera = DataHolder.INSTANCE.retrieve(DataHolderEntry.MASTER_CAMERA);
        drawHosts(canvas, masterCamera);

        for (Pair<String, Skeleton> skeletonEntry : frame) {

            String cameraName = skeletonEntry.first;
            Skeleton skeleton = skeletonEntry.second;

            // If a master camera is defined, transform to master camera coordinates and then draw
            if (masterCamera != null) {

                CoordinatesTransformer ct = DataHolder.INSTANCE.retrieve(DataHolderEntry.CAMERA_TRANSFORMER);
                skeleton = ct.transform(cameraName, masterCamera, skeleton);
            }

            // Draws the skeleton
            drawBonesAndJoints(skeleton, canvas, getCameraColorKit(cameraName));
        }

        LinearLayout ll = (LinearLayout) _activity.findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(_bg));
    }
}
