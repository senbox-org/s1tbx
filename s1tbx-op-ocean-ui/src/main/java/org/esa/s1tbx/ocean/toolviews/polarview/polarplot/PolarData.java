/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.ocean.toolviews.polarview.polarplot;

import java.awt.*;

public class PolarData {

    private final float firstDir;
    private final float dirStep;
    private final float radii[];
    private final float minValue;
    private final float maxValue;

    private final int Nr;
    private final int Nth;
    private final AxisInfo rData = new AxisInfo();
    private final AxisInfo cData = new AxisInfo();

    private int rScreenPoints[] = null;
    private Object[] cValues = null;
    private float dirOffset = 0;

    private final Color backgroundColor = Color.white;
    private Color[][] colors;
    private ColourScale cScale = null;
    private int plotCount = 0;

    public PolarData(float cValues[][], float firstDir, float dirStep, float radii[], final float minValue, final float maxValue) {
        this.cValues = cValues;
        colors = null;

        this.firstDir = firstDir;
        this.dirStep = dirStep;
        this.radii = radii;
        this.minValue = minValue;
        this.maxValue = maxValue;
        Nth = cValues.length;
        Nr = cValues[0].length;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setRAxis(final Axis rAxis) {
        rData.setAxis(rAxis);
    }

    public void setDirOffset(final float dirOffset) {
        this.dirOffset = dirOffset;
    }

    void preparePlot() {
        plotCount = cValues.length;
        prepareColors(cValues);
        prepareRTPoints();
    }

    public void draw(final Graphics g) {
        if (rData.axis == null)
            return;
        rData.axis.getAxisGraphics(g);
        preparePlot();
        int lastRad = 0x7fffffff;
        final int arcAngle = (int) dirStep;
        if (rScreenPoints[0] < rScreenPoints[Nr]) {
            int rad, rad2;
            for (int ri = Nr - 1; ri >= 0; ri--) {
                float th = firstDir + dirOffset;
                final float r = rScreenPoints[ri + 1];
                rad = (int) r;
                if (rad >= lastRad) {
                    rad = lastRad - 1;
                    rScreenPoints[ri + 1] = rad;
                }
                lastRad = rad;
                rad2 = rad + rad;
                for (int thi = 0; thi < Nth; ) {
                    int angle = (int) th;
                    g.setColor(colors[thi][ri]);
                    g.fillArc(-rad, -rad, rad2, rad2, angle, arcAngle);
                    th += dirStep;
                    thi++;
                }
            }

            rad = rScreenPoints[0];
            rad2 = rad + rad;
            g.setColor(backgroundColor);
            g.fillOval(-rad, -rad, rad2, rad2);
        } else {
            int rad, rad2;
            for (int ri = 0; ri < Nr; ri++) {
                float th = firstDir + dirOffset;
                final float r = rScreenPoints[ri + 1];
                rad = (int) r;
                if (rad >= lastRad) {
                    rad = lastRad - 1;
                    rScreenPoints[ri + 1] = rad;
                }
                lastRad = rad;
                rad2 = rad + rad;
                for (int thi = 0; thi < Nth; ) {
                    int angle = (int) th;
                    g.setColor(colors[thi][ri]);
                    g.fillArc(-rad, -rad, rad2, rad2, angle, arcAngle);
                    th += dirStep;
                    thi++;
                }
            }

            rad = rScreenPoints[Nr];
            rad2 = rad + rad;
            g.setColor(backgroundColor);
            g.fillOval(-rad, -rad, rad2, rad2);
        }
    }

    private void prepareRTPoints() {
        if (rData.axis == null)
            return;
        if (rScreenPoints == null || rScreenPoints.length != radii.length) {
            rScreenPoints = new int[radii.length];
            rData.touch();
        }
        rData.checkTouchedAxis();
        if (rData.touched) {
            rData.untouch();
            final int rP[][] = {rScreenPoints};
            final float r[][] = {radii};
            computeScreenPoints(r, rP, rData.axis);
        }
    }

    public double valueFromScreenPoint(final int r) {
        if (rScreenPoints[0] < rScreenPoints[Nr]) {
            if (r < rScreenPoints[0])
                return (double) radii[0];
            for (int ri = 1; ri <= Nr; ri++) {
                if (rScreenPoints[ri - 1] <= r && rScreenPoints[ri] >= r) {
                    if (rScreenPoints[ri - 1] == r)
                        return (double) radii[ri - 1];
                    if (rScreenPoints[ri] == r)
                        return (double) radii[ri];
                    else {
                        return (double) (((radii[ri] - radii[ri - 1]) * ((float) r - (float) rScreenPoints[ri - 1])) /
                                (float) (rScreenPoints[ri] - rScreenPoints[ri - 1]) + radii[ri - 1]);
                    }
                }
            }
            return (double) radii[Nr];
        }
        if (r > rScreenPoints[0])
            return (double) radii[0];
        for (int ri = 1; ri <= Nr; ri++) {
            if (rScreenPoints[ri - 1] >= r && rScreenPoints[ri] <= r) {
                if (rScreenPoints[ri - 1] == r)
                    return (double) radii[ri - 1];
                if (rScreenPoints[ri] == r)
                    return (double) radii[ri];
                else {
                    return (double) (((radii[ri] - radii[ri - 1]) * ((float) r - (float) rScreenPoints[ri])) /
                            (float) (rScreenPoints[ri - 1] - rScreenPoints[ri]) + radii[ri - 1]);
                }
            }
        }
        return (double) radii[Nr];
    }

    private static void computeScreenPoints(final float values[][], final int points[][], final Axis axis) {
        if (axis == null)
            return;
        final int np = values.length;
        for (int i = 0; i < np; i++) {
            if (values[i] == null) {
                points[i] = null;
            } else {
                final int n = values[i].length;
                if (points[i] == null || points[i].length != n)
                    points[i] = new int[n];
                for (int j = 0; j < n; j++) {
                    points[i][j] = axis.getScreenPoint(values[i][j]);
                }
            }
        }
    }

    public void setCAxis(final Axis cAxis) {
        cData.setAxis(cAxis);
    }

    public ColourScale getColorScale() {
        return cScale;
    }

    public void setColorScale(final ColourScale cScale) {
        this.cScale = cScale;
        cData.touch();
    }

    private void prepareColors(final Object cValues[]) {
        if (cScale == null)
            return;
        if (colors == null || colors.length != plotCount) {
            colors = new Color[plotCount][];
            cData.touched = true;
        }
        if (cData.axis != null) {
            final int TouchId = cData.axis.getTouchId();
            if (TouchId != cData.touchId) {
                cData.touched = true;
                cData.touchId = TouchId;
            }
        }
        if (cData.touched) {
            cData.touched = false;
            cScale.setRange(cData.axis.getRange());
            computeColors((float[][]) cValues, colors, cScale);
        }
    }

    private static void computeColors(final float values[][], final Color colors[][], final ColourScale cScale) {
        if (cScale == null)
            return;
        final int np = values.length;
        for (int i = 0; i < np; i++) {
            if (values[i] == null) {
                colors[i] = null;
            } else {
                final int n = values[i].length;
                if (colors[i] == null || colors[i].length != n)
                    colors[i] = new Color[n];
                for (int j = 0; j < n; j++) {
                    colors[i][j] = cScale.getColor(values[i][j]);
                }
            }
        }
    }

    private static final class AxisInfo {

        private Axis axis = null;
        private boolean touched;
        private int touchId;

        AxisInfo() {
            touched = true;
            touchId = -1;
        }

        void setAxis(Axis axis) {
            this.axis = axis;
            touched = true;
        }

        void touch() {
            touched = true;
        }

        void untouch() {
            touched = false;
        }

        void checkTouchedAxis() {
            final int id = axis.getTouchId();
            if (touchId != id) {
                touched = true;
                touchId = id;
            }
        }
    }
}
