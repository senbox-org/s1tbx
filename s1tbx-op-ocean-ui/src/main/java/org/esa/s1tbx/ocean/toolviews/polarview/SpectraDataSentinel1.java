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

import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.PolarData;
import org.esa.snap.framework.datamodel.Product;

/**
 * Spectra Data for Sentinel-1 L2 OCN
 */
public class SpectraDataSentinel1 extends SpectraDataBase implements SpectraData {

    public SpectraDataSentinel1(final Product product) {
        super(product);
    }

    public String[] getSpectraMetadata(int rec) {
        return null;
    }

    public float[][] getSpectrum(int imageNum, int rec, boolean getReal) {
        return null;
    }

    public PolarData getPolarData(final int currentRec, final SpectraUnit spectraUnit) {
        return null;
    }

    public String[] updateReadouts(final double rTh[], final int currentRecord) {
        return null;
    }

    public float getMinValue(final boolean real) {
        return 0;
    }

    public float getMaxValue(final boolean real) {
        return 0;
    }
}
