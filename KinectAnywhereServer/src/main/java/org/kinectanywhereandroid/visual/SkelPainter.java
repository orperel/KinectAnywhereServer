package org.kinectanywhereandroid.visual;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.widget.LinearLayout;

import org.kinectanywhereandroid.MainActivity;
import org.kinectanywhereandroid.R;
import org.kinectanywhereandroid.framework.IKinectFrameEventListener;
import org.kinectanywhereandroid.framework.RemoteKinect;
import org.kinectanywhereandroid.model.Joint;
import org.kinectanywhereandroid.framework.SingleFrameData;
import org.kinectanywhereandroid.model.Skeleton;
import org.kinectanywhereandroid.util.DataHolder;
import org.kinectanywhereandroid.util.DataHolderEntry;

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

    public SkelPainter(MainActivity activity) {
        _activity = activity;
        _cm = new AnalyticCoordinatesMapper(CANVAS_WIDTH, CANVAS_HEIGHT);
        _bg = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
        _canvas = new Canvas(_bg);
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
                          Joint.JointType jointType0, Joint.JointType jointType1)
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


        PointF start = _cm.MapSkeletonPointToDepthPoint(joint0);
        PointF end = _cm.MapSkeletonPointToDepthPoint(joint1);

        // We assume all drawn bones are inferred unless BOTH joints are tracked
        if (joint0.trackingState == Joint.JointTrackingState.Tracked && joint1.trackingState == Joint.JointTrackingState.Tracked)
        {
            paint.setColor(Color.parseColor("#ffffff"));
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);

        } else {
            paint.setColor(Color.parseColor("#506bd8"));
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }
    }


    public void drawBonesAndJoints(Skeleton skeleton, Canvas canvas) {

        // Render Torso
        this.DrawBone(canvas, skeleton, Joint.JointType.Head, Joint.JointType.ShoulderCenter);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.ShoulderRight);
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderCenter, Joint.JointType.Spine);
        this.DrawBone(canvas, skeleton, Joint.JointType.Spine, Joint.JointType.HipCenter);
        this.DrawBone(canvas, skeleton, Joint.JointType.HipCenter, Joint.JointType.HipLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.HipCenter, Joint.JointType.HipRight);

        // Left Arm
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderLeft, Joint.JointType.ElbowLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.ElbowLeft, Joint.JointType.WristLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.WristLeft, Joint.JointType.HandLeft);

        // Right Arm
        this.DrawBone(canvas, skeleton, Joint.JointType.ShoulderRight, Joint.JointType.ElbowRight);
        this.DrawBone(canvas, skeleton, Joint.JointType.ElbowRight, Joint.JointType.WristRight);
        this.DrawBone(canvas, skeleton, Joint.JointType.WristRight, Joint.JointType.HandRight);

        // Left Leg
        this.DrawBone(canvas, skeleton, Joint.JointType.HipLeft, Joint.JointType.KneeLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.KneeLeft, Joint.JointType.AnkleLeft);
        this.DrawBone(canvas, skeleton, Joint.JointType.AnkleLeft, Joint.JointType.FootLeft);

        // Right Leg
        this.DrawBone(canvas, skeleton, Joint.JointType.HipRight, Joint.JointType.KneeRight);
        this.DrawBone(canvas, skeleton, Joint.JointType.KneeRight, Joint.JointType.AnkleRight);
        this.DrawBone(canvas, skeleton, Joint.JointType.AnkleRight, Joint.JointType.FootRight);

        // Render Joints
        for (Joint joint : skeleton.joints)
        {
            // Project 3d point to 2d screen coordinates
            PointF center = _cm.MapSkeletonPointToDepthPoint(joint);

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

    public void drawHosts(Canvas canvas) {
        Paint paint = new Paint();
        canvas.drawPaint(paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16);

        int i = 0;
        Map<String, RemoteKinect> kinectDict = DataHolder.INSTANCE.retrieve(DataHolderEntry.CONNECTED_HOSTS);

        for (Map.Entry<String, RemoteKinect> entry : kinectDict.entrySet()) {
            canvas.drawText(entry.getKey(), 30, 30 + i * 10, paint);

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

        canvas.drawColor(Color.BLACK);

        drawHosts(canvas);

        for (Skeleton skeleton : frame) {

            // Draws the skeleton
            drawBonesAndJoints(skeleton, canvas);
        }

        LinearLayout ll = (LinearLayout) _activity.findViewById(R.id.rect);
        ll.setBackground(new BitmapDrawable(_bg));
    }

//    private void drawSkeletons(final Map<String, List<Skeleton>> skeletons){

//        Map<String, Skeleton> cameras = preCalibrate(skeletons);
//
//        if (cameras == null || cameras.size() <= 0)
//            return; // Avoid crashes
//
//        Iterator<Map.Entry<String, Skeleton>> camIter = cameras.entrySet().iterator();
//
//        Map.Entry<String, Skeleton> cam0 = camIter.next();
//
//        final Map<String, List<Skeleton>> transformedSkels = new HashMap<>();
//        transformedSkels.put(cam0.getKey(), Collections.singletonList(cam0.getValue()));
//
//        if (!camIter.hasNext())
//            return;
//
//        Map.Entry<String, Skeleton> nextCam = null;
//
//        do {
//            nextCam = camIter.next();
//
//            Matrix transformation = mCalibrator.calibrate(cam0.getValue(), nextCam.getValue());
//            Skeleton transformedSkel = mCalibrator.transform(nextCam.getValue(), transformation);
//            transformedSkels.put(nextCam.getKey(), Collections.singletonList(transformedSkel));
//
//        }  while (camIter.hasNext());
//
//        mActivity.runOnUiThread( new Runnable() {
//
//            @Override
//            public void run() {
//                mActivity.drawSkeletons(transformedSkels);
//            }
//        });
//    }

}
