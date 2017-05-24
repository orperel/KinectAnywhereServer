package org.kinectanywhereandroid.visual;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;

import org.kinectanywhereandroid.MainActivity;
import org.kinectanywhereandroid.R;
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

    private final static int FPS_DELTA = 50; // Minimal amount of milliseconds between adjacent renderings
    private final static int CANVAS_WIDTH = 480;
    private final static int CANVAS_HEIGHT = 800;

    private MainActivity _activity;
    private Bitmap _bg;
    private Canvas _canvas;
    private AnalyticCoordinatesMapper _cm;

    /** Assigned colors to connected Kinect cameras */
    private Map<String, ColorsPalette> _camerasColorKit;

    public SkelPainter(MainActivity activity) {
        _activity = activity;
        _cm = new AnalyticCoordinatesMapper(CANVAS_WIDTH, CANVAS_HEIGHT);
        _bg = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
        _canvas = new Canvas(_bg);
        _camerasColorKit = new HashMap<>();
    }

    @Override
    public void handle(final SingleFrameData frame) {

        if (abs(frame.getTimestamp() - frame.getPrevTimestamp()) > FPS_DELTA)
        {
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
            _camerasColorKit.put(cameraName, ColorsPalette.nextColorKit());

        return _camerasColorKit.get(cameraName);
    }

    public void drawHosts(Canvas canvas) {
        Paint paint = new Paint();
        canvas.drawPaint(paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16);

        int i = 0;
        Map<String, RemoteKinect> kinectDict = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);

        for (Map.Entry<String, RemoteKinect> entry : kinectDict.entrySet()) {

            String cameraName = entry.getKey();
            ColorsPalette cameraColorKit = getCameraColorKit(cameraName);
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

        drawHosts(canvas);

        for (Pair<String, Skeleton> skeletonEntry : frame) {

            String cameraName = skeletonEntry.first;
            Skeleton skeleton = skeletonEntry.second;

            // Draws the skeleton
            drawBonesAndJoints(skeleton, canvas, getCameraColorKit(cameraName));
        }

        LinearLayout ll = (LinearLayout) _activity.findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(_bg));
    }

    /**
     * Definitions of look & feel for each camera contents
     */
    private static class ColorsPalette {

        public final static int CANVAS_BG_COLOR = Color.BLACK;

        // More color definitions - ARGB format
        private final static int DARKRED = Color.parseColor("FF7C1313");
        private final static int DARKBLUE = Color.parseColor("13137C");
        private final static int DARKGREEN = Color.parseColor("0B5813");
        private final static int DARKYELLOW = Color.parseColor("DC8B11");
        private final static int DARKCYAN = Color.parseColor("0C8F94");
        private final static int DARKMAGENTA = Color.parseColor("940C94");
        private final static int ORANGE = Color.parseColor("FF9100");
        private final static int DARKORANGE = Color.parseColor("AB6306");

        public Paint jointsPaint;
        public Paint untrackedJointsPaint;
        public Paint bonesPaint;
        public Paint untrackedBonesPaint;

        private static int lastKitIndex;

        public static ColorsPalette nextColorKit() {

            // Repeat used kits if we're out of available kits
            if (lastKitIndex >= 7)
                lastKitIndex = 0;

            return COLOR_KITS[lastKitIndex++];
        }

        private ColorsPalette() {

            jointsPaint = new Paint();
            untrackedJointsPaint = new Paint();
            bonesPaint = new Paint();
            untrackedBonesPaint = new Paint();

            setGeneralDefinitions(jointsPaint);
            setGeneralDefinitions(untrackedJointsPaint);
            setGeneralDefinitions(bonesPaint);
            setGeneralDefinitions(untrackedBonesPaint);
            setJointsSize(3.0f);
            setBonesWidth(2.5f);
            markDashed(untrackedJointsPaint);
            markDashed(untrackedBonesPaint);
        }

        private void setGeneralDefinitions(Paint painter) {

            painter.setStrokeCap(Paint.Cap.ROUND);
            painter.setStrokeJoin(Paint.Join.ROUND);
            painter.setAntiAlias(true);
            float shadowRadius = 0.7f;
            painter.setShadowLayer(shadowRadius, 0.5f, 0.5f, Color.DKGRAY);
            painter.setStyle(Paint.Style.FILL_AND_STROKE);
            painter.setTextSize(16.0f);
        }

        private void markDashed(Paint paint) {
            paint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
        }

        private ColorsPalette setJointsColor(int color) {
            jointsPaint.setColor(color);
            untrackedJointsPaint.setColor(color);
            return this;
        }

        private ColorsPalette setJointsSize(float size) {
            jointsPaint.setStrokeWidth(size);
            untrackedJointsPaint.setStrokeWidth(size);
            return this;
        }

        private ColorsPalette setBonesColor(int color) {
            bonesPaint.setColor(color);
            untrackedBonesPaint.setColor(color);
            return this;
        }

        private ColorsPalette setBonesWidth(float width) {
            bonesPaint.setStrokeWidth(width);
            untrackedBonesPaint.setStrokeWidth(width);
            return this;
        }

        private static final ColorsPalette CAM0;
        private static final ColorsPalette CAM1;
        private static final ColorsPalette CAM2;
        private static final ColorsPalette CAM3;
        private static final ColorsPalette CAM4;
        private static final ColorsPalette CAM5;
        private static final ColorsPalette CAM6;
        private static final ColorsPalette CAM7;

        private static final ColorsPalette[] COLOR_KITS;

        static {
            CAM0 = new ColorsPalette().setJointsColor(Color.RED).setBonesColor(DARKRED);
            CAM1 = new ColorsPalette().setJointsColor(Color.BLUE).setBonesColor(DARKBLUE);
            CAM2 = new ColorsPalette().setJointsColor(Color.GREEN).setBonesColor(DARKGREEN);
            CAM3 = new ColorsPalette().setJointsColor(Color.YELLOW).setBonesColor(DARKYELLOW);
            CAM4 = new ColorsPalette().setJointsColor(Color.CYAN).setBonesColor(DARKCYAN);
            CAM5 = new ColorsPalette().setJointsColor(Color.MAGENTA).setBonesColor(DARKMAGENTA);
            CAM6 = new ColorsPalette().setJointsColor(ORANGE).setBonesColor(DARKORANGE);
            CAM7 = new ColorsPalette().setJointsColor(Color.WHITE).setBonesColor(Color.GRAY);

            COLOR_KITS = new ColorsPalette[]{ CAM0, CAM1, CAM2, CAM3, CAM4, CAM5, CAM6, CAM7 };

            lastKitIndex = 0;
        }
    }
}
