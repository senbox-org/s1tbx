/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.ocean.toolviews.polarview;

import org.apache.commons.math3.util.FastMath;
import org.esa.snap.framework.datamodel.Product;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for SpectraData
 */
public class SpectraDataBase {

    protected final Product product;
    protected final SpectraData.WaveProductType waveProductType;

    protected int numRecords;
    protected int recordLength;

    protected int numDirBins;
    protected int numWLBins;

    protected float firstDirBins = 0;
    protected float dirBinStep = 0;
    protected float firstWLBin = 0;
    protected float lastWLBin = 0;
    protected final double minRadius = -10;
    protected final double maxRadius = 333.33333333333;

    protected double windSpeed = 0;
    protected double windDirection = 0;

    protected float spectrum[][];

    protected final DecimalFormat frmt = new DecimalFormat("0.0000");

    public SpectraDataBase(final Product product, final SpectraData.WaveProductType waveProductType) {
        this.product = product;
        this.waveProductType = waveProductType;
    }

    public double getMinRadius() {
        return minRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getWindDirection() {
        return windDirection;
    }

    public int getNumRecords() {
        return numRecords;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public String[] updateReadouts(final double rTh[], final int currentRecord) {
        if (spectrum == null)
            return null;

        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        int wvBin = (int) (((rStep / 2.0 + Math.log(10000.0 / rTh[0])) - Math.log(firstWLBin)) / rStep);
        wvBin = Math.min(wvBin, spectrum[0].length - 1);
        final int wl = (int) Math.round(FastMath.exp((double) wvBin * rStep + Math.log(firstWLBin)));

        final float thFirst, thStep;
        final int thBin, element, direction;
        if (waveProductType == SpectraData.WaveProductType.CROSS_SPECTRA) {
            thFirst = firstDirBins - 5f;
            thStep = dirBinStep;
            thBin = (int) (((rTh[1] - (double) thFirst) % 360.0) / (double) thStep);
            element = (thBin % (spectrum.length / 2)) * spectrum[0].length + wvBin;
            direction = (int) ((float) thBin * thStep + thStep / 2.0f + thFirst);
        } else {
            thFirst = firstDirBins + 5f;
            thStep = -dirBinStep;
            thBin = (int) ((((360.0 - rTh[1]) + (double) thFirst) % 360.0) / (double) (-thStep));
            element = thBin * spectrum[0].length + wvBin;
            direction = (int) (-((float) thBin * thStep + thStep / 2.0f + thFirst));
        }

        final List<String> readoutList = new ArrayList<>(5);
        readoutList.add("Record: " + (currentRecord + 1) + " of " + (numRecords + 1));
        readoutList.add("Wavelength: " + wl + " m");
        readoutList.add("Direction: " + direction + " °");
        readoutList.add("Bin: " + (thBin + 1) + "," + (wvBin + 1) + " Element: " + element);
        readoutList.add("Value: " + spectrum[thBin][wvBin]);

        return readoutList.toArray(new String[readoutList.size()]);
    }
}
