package org.kinectanywhereandroid.visual;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

/**
 * Definitions of look & feel for each camera contents
 */
public class ColorsPalette {

    public final static int CANVAS_BG_COLOR = Color.BLACK;

    // More color definitions - ARGB format
    public final static int DARKRED = Color.parseColor("#FF7C1313");
    public final static int DARKBLUE = Color.parseColor("#FF13137C");
    public final static int DARKGREEN = Color.parseColor("#FF0B5813");
    public final static int DARKYELLOW = Color.parseColor("#FFDC8B11");
    public final static int DARKCYAN = Color.parseColor("#FF0C8F94");
    public final static int DARKMAGENTA = Color.parseColor("#FF940C94");
    public final static int ORANGE = Color.parseColor("#FFFF9100");
    public final static int DARKORANGE = Color.parseColor("#FFAB6306");

    public Paint jointsPaint;
    public Paint untrackedJointsPaint;
    public Paint bonesPaint;
    public Paint untrackedBonesPaint;

    public ColorsPalette() {

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

    public void setGeneralDefinitions(Paint painter) {

        painter.setStrokeCap(Paint.Cap.ROUND);
        painter.setStrokeJoin(Paint.Join.ROUND);
        painter.setAntiAlias(true);
        painter.setStyle(Paint.Style.FILL_AND_STROKE);
        activateShadow(painter);
        painter.setTextSize(16.0f);
    }

    public void markDashed(Paint paint) {
        paint.setPathEffect(new DashPathEffect(new float[] {3,10}, 0));
    }

    public ColorsPalette setJointsColor(int color) {
        jointsPaint.setColor(color);
        untrackedJointsPaint.setColor(color);
        return this;
    }

    public ColorsPalette setJointsSize(float size) {
        jointsPaint.setStrokeWidth(size);
        untrackedJointsPaint.setStrokeWidth(size - 0.5f);
        return this;
    }

    public ColorsPalette setBonesColor(int color) {
        bonesPaint.setColor(color);
        untrackedBonesPaint.setColor(color);
        return this;
    }

    public ColorsPalette setBonesWidth(float width) {
        bonesPaint.setStrokeWidth(width);
        untrackedBonesPaint.setStrokeWidth(width - 0.5f);
        return this;
    }

    private void activateShadow(Paint painter) {

        float shadowRadius = 0.4f;
        painter.setShadowLayer(shadowRadius, 0.5f, 0.5f, Color.DKGRAY);
        painter.setAntiAlias(true);
    }

    private void deactivateShadow(Paint painter) {

        painter.setShadowLayer(0, 0.0f, 0.0f, Color.DKGRAY);
        painter.setAntiAlias(false);
    }

    public void activateShadow() {

        activateShadow(jointsPaint);
        activateShadow(untrackedJointsPaint);
        activateShadow(bonesPaint);
        activateShadow(untrackedBonesPaint);
    }

    public void deactivateShadow() {

        deactivateShadow(jointsPaint);
        deactivateShadow(untrackedJointsPaint);
        deactivateShadow(bonesPaint);
        deactivateShadow(untrackedBonesPaint);
    }
}