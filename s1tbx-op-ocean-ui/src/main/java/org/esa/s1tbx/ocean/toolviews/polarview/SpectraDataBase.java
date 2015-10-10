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

import org.esa.snap.core.datamodel.Product;

import java.text.DecimalFormat;

/**
 * Base class for SpectraData
 */
public class SpectraDataBase {

    protected final Product product;
    protected SpectraData.WaveProductType waveProductType;

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

    public SpectraDataBase(final Product product) {
        this.product = product;
    }

    public void setWaveProductType(final SpectraData.WaveProductType waveProductType) {
        this.waveProductType = waveProductType;
    }

    public SpectraData.WaveProductType getWaveProductType() {
        return waveProductType;
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
}
