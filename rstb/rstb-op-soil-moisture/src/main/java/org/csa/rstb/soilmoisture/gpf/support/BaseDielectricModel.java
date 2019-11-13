/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf.support;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;

/**
 * Dielectric model base class.
 */
public class BaseDielectricModel {

    // Quality Index

    // 0 to 9 are for successful retrieval
    protected static final long QUALITY_INDEX_BEST = 0;
    protected static final long QUALITY_INDEX_MIN_MAX_REACHED = 1;

    // 10 to 255 are for no retrieval
    protected static final long QUALITY_INDEX_NO_RDC = 10;
    protected static final long QUALITY_INDEX_NO_SAND_OR_CLAY = 11;
    protected static final long QUALITY_INDEX_MINIMIZER_FAILED = 12;
    protected static final long INVALID_QUALITY_INDEX_VALUE = QUALITY_INDEX_NO_RDC;
    private static final double EPSILON = 1.0e-5;
    protected Operator smDielectricModelInverOp;
    protected Product sourceProduct;
    protected Product targetProduct;
    // source is RDC
    protected Band rdcBand;
    protected double INVALID_RDC_VALUE;
    // target is SM
    protected Band smBand;
    protected double INVALID_SM_VALUE;
    // Quality index for target SM
    protected Band qualityIndexBand; // target
    // Soil moisture range (m^3/m^3). Results are restricted to this range.
    protected double minSM = 0.0d;
    protected double maxSM = 0.0d;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    BaseDielectricModel(final Operator op, final Product srcProduct, final Product tgtProduct,
                        final double invalidSMValue,
                        final double minSMVal, final double maxSMVal,
                        final Band smBand, final Band qualityIndexBand,
                        final String rdcBandName) {

        smDielectricModelInverOp = op;

        sourceProduct = srcProduct;
        targetProduct = tgtProduct;

        INVALID_SM_VALUE = invalidSMValue;
        minSM = minSMVal;
        maxSM = maxSMVal;

        this.smBand = smBand;
        this.qualityIndexBand = qualityIndexBand;

        getRDCBand(rdcBandName);

        qualityIndexBand.setNoDataValue(INVALID_QUALITY_INDEX_VALUE);
        qualityIndexBand.setNoDataValueUsed(true);
    }

    static protected boolean isValid(final double val) {

        return (!Double.isNaN(val) && !Double.isInfinite(val));
    }

    protected Band getSourceBand(String keyword) {

        final String[] sourceBandNames = sourceProduct.getBandNames();

        String bandName = "";
        keyword = keyword.toLowerCase();
        for (String s : sourceBandNames) {

            if (s.toLowerCase().contains(keyword)) {

                if (bandName.isEmpty()) {
                    bandName = s;

                } else {
                    throw new OperatorException("Too many " + keyword + " bands");
                }
            }
        }

        if (bandName.isEmpty()) {
            throw new OperatorException("No " + keyword + " band name in product");
        }

        final Band band = sourceProduct.getBand(bandName);

        if (band == null) {
            throw new OperatorException("Failed to get source " + keyword + " band ");
        }

        return band;
    }

    private void getRDCBand(final String rdcBandName) {

        rdcBand = getSourceBand(rdcBandName);

        INVALID_RDC_VALUE = rdcBand.getNoDataValue();

        /*
        final int datatype = rdcBand.getDataType();
        if (datatype != ProductData.TYPE_FLOAT64) {
            System.out.println("rdcBand data type = " + datatype);
        }  else {
            System.out.println("rdcBand is doubles");
        }
        */
    }

    protected long getQualityIndex(final double sm) {

        long qualityIndex = QUALITY_INDEX_BEST;

        if (sm == INVALID_SM_VALUE) {

            qualityIndex = QUALITY_INDEX_MINIMIZER_FAILED;

        } else if (Math.abs(sm - minSM) < EPSILON || Math.abs(sm - maxSM) < EPSILON) {

            qualityIndex = QUALITY_INDEX_MIN_MAX_REACHED;
        }

        return qualityIndex;
    }
}


