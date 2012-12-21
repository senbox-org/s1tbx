/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.views.polarview;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Vector;

public class ColourScale {

    private ColorModel cm = null;
    private final Color[] colors;
    private final int thresholdCount;
    private final int[] colorIndexThresholds;
    private final Vector<ColorBar> coloredClients = new Vector<ColorBar>();

    private double colorIndexValues[] = null;
    private double darkestValue = 0;
    private double darkestIndex = 0;
    private double lightestValue = 0;
    private double lightestIndex = 0;
    private double scale = 0;

    private ColourScale(Color colorTable[]) {
        thresholdCount = colorTable.length;
        colorIndexThresholds = new int[thresholdCount];
        colorIndexThresholds[0] = 0;
        colorIndexThresholds[thresholdCount - 1] = 255;
        colors = colorTable;
    }

    public static ColourScale newMonochromeScale(double range[], Color chromum) {
        final Color monochromeColorTable[] = {
                Color.black, Color.black, chromum, chromum
        };
        return new ColourScale(monochromeColorTable, range);
    }

    public static ColourScale newCustomScale(double range[]) {
        return new ColourScale(PolarView.colourTable, range);
    }

    private ColourScale(Color colorTable[], double range[]) {
        this(colorTable);
        colorIndexValues = new double[thresholdCount];
        setRange(range[0], range[1]);
        createColorMap();
    }

    public void setRange(double range[]) {
        setRange(range[0], range[1]);
    }

    public void setRange(int range[]) {
        setRange(range[0], range[1]);
    }

    private void setRange(int minValue, int maxValue) {
        setRange((double) minValue, (double) maxValue);
    }

    private void setRange(double minValue, double maxValue) {
        darkestValue = minValue;
        lightestValue = maxValue;
        validateRange();
        setEvenThresholds();
        darkestIndex = colorIndexThresholds[0];
        lightestIndex = colorIndexThresholds[thresholdCount - 1];
        updateRange();
    }

    byte getColorIndex(int value) {
        return getColorIndex((double) value);
    }

    byte getColorIndex(float value) {
        return getColorIndex((double) value);
    }

    byte getColorIndex(double value) {
        double val = value - darkestValue;
        if (val < 0.0D)
            return (byte) (int) darkestIndex;
        if (scale != 0.0D)
            val *= scale;
        val += darkestIndex;
        if (val > lightestIndex)
            return (byte) (int) lightestIndex;
        else
            return (byte) ((int) Math.round(val) & 0xff);
    }

    private int getIntegerColorValue(int index) {
        return (int) Math.round(getDoubleColorValue(index));
    }

    private double getDoubleColorValue(int index) {
        double value = (double) index - darkestIndex;
        if (scale != 0.0D)
            value /= scale;
        return value + darkestValue;
    }

    private void updateColorValues() {
        for (int i = 0; i < thresholdCount; i++) {
            colorIndexValues[i] = getIntegerColorValue(colorIndexThresholds[i]);
        }
    }

    private void validateRange() {
        darkestValue = Math.min(darkestValue, lightestValue);
        final double range = lightestValue - darkestValue;
        scale = 255D / range;
    }

    public final Color getColor(int value) {
        return new Color(getRGB(value));
    }

    public final Color getColor(float value) {
        return new Color(getRGB(value));
    }

    public final Color getColor(double value) {
        return new Color(getRGB(value));
    }

    private int getRGB(int value) {
        return cm.getRGB(getColorIndex(value) & 0xff);
    }

    private int getRGB(float value) {
        return cm.getRGB(getColorIndex(value) & 0xff);
    }

    private int getRGB(double value) {
        return cm.getRGB(getColorIndex(value) & 0xff);
    }

    public ColorModel getColorModel() {
        return cm;
    }

    public synchronized void addColoredObject(ColorBar ip) {
        if (!coloredClients.contains(ip)) {
            coloredClients.addElement(ip);
        }
    }

    private void setEvenThresholds() {
        int N = thresholdCount - 1;
        int first = 0;
        int last = N;
        if (colors[last].equals(colors[last - 1])) {
            colorIndexThresholds[last] = 255;
            last--;
            N--;
        }
        if (colors[first].equals(colors[first + 1])) {
            colorIndexThresholds[first] = 0;
            first++;
            N--;
        }
        final double colorStep = 255D / N;
        int offset = 0;
        int i = 0;
        for (int t = first; t <= last; t++) {
            colorIndexThresholds[t] = offset + (int) Math.round((double) i * colorStep);
            i++;
        }
    }

    private void createColorMap() {
        final int lastThreshold = thresholdCount - 1;
        final byte cmap[] = new byte[768];
        Color lastColor = colors[0];
        int lastIndex = colorIndexThresholds[0];
        int c = 0;
        for (int i = 1; i < thresholdCount; i++) {
            final int cRange = colorIndexThresholds[i] - lastIndex;
            final int lastRGB[] = {lastColor.getRed(), lastColor.getGreen(), lastColor.getBlue()};
            final int nextRGB[] = {colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue()};

            for (int j = 0; j < cRange; j++) {
                final float nextScale = (float) j / (float) cRange;
                final float lastScale = 1.0F - nextScale;
                cmap[c++] = (byte) (int) ((float) lastRGB[0] * lastScale + (float) nextRGB[0] * nextScale);
                cmap[c++] = (byte) (int) ((float) lastRGB[1] * lastScale + (float) nextRGB[1] * nextScale);
                cmap[c++] = (byte) (int) ((float) lastRGB[2] * lastScale + (float) nextRGB[2] * nextScale);
            }

            lastColor = colors[i];
            lastIndex = colorIndexThresholds[i];
        }

        final Color finalColor = colors[lastThreshold];
        cmap[c++] = (byte) finalColor.getRed();
        cmap[c++] = (byte) finalColor.getGreen();
        cmap[c] = (byte) finalColor.getBlue();
        cm = new IndexColorModel(8, 256, cmap, 0, false);
    }

    private synchronized void notifyRangeChange() {
        ColorBar ip;
        for (Enumeration elem = coloredClients.elements(); elem.hasMoreElements(); ip.updatedColorScale()) {
            ip = (ColorBar) elem.nextElement();
        }
    }

    private void updateRange() {
        updateColorValues();
        createColorMap();
        notifyRangeChange();
    }
}