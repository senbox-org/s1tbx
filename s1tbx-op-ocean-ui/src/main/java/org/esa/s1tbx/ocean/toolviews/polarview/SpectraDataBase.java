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

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

/**
 * Base class for SpectraData
 */
public class SpectraDataBase {

    protected final Product product;
    protected final SpectraData.WaveProductType waveProductType;
    protected final int recordLength;
    protected final int numRecords;

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

    public SpectraDataBase(final Product product, final SpectraData.WaveProductType waveProductType) {
        this.product = product;
        this.waveProductType = waveProductType;

        final RasterDataNode rasterNode = product.getBandAt(0);
        numRecords = rasterNode.getRasterHeight() - 1;
        recordLength = rasterNode.getRasterWidth();
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
}
