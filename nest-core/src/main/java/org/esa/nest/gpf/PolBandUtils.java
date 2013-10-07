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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for handling polarimetric bands
 */
public class PolBandUtils {

    public static enum MATRIX { FULL, C3, T3, C4, T4, C2, COMPACT }

    public static class QuadSourceBand {
        public final String productName;
        public final Band[] srcBands;
        public final String suffix;
        public Band[] targetBands;

        public double spanMin = 1e+30;
        public double spanMax = -1e+30;
        public boolean spanMinMaxSet = false;

        public QuadSourceBand(final String productName, final Band[] bands, final String suffix) {
            this.productName = productName;
            this.srcBands = bands;
            this.suffix = suffix;
        }
        public void addTargetBands(final Band[] targetBands) {
            this.targetBands = targetBands;
        }
    }

    /**
     * Check input product format, get source product type.
     * @param sourceProduct The source product
     * @return The product type
     */
    public static MATRIX getSourceProductType(final Product sourceProduct) {

        final String[] bandNames = sourceProduct.getBandNames();
        boolean isC3 = false, isT3 = false, isC2 = false, isS2 = false;
        for(String name : bandNames) {
            if(name.contains("C44")) {
                return MATRIX.C4;
            } else if(name.contains("T44")) {
                return MATRIX.T4;
            } else if(name.contains("C33")) {
                isC3 = true;
            } else if(name.contains("T33")) {
                isT3 = true;
            } else if(name.contains("C22")) {
                isC2 = true;
            } else if (name.contains("RH")) {
                isS2 = true;
            }
        }

        if(isC3)
            return MATRIX.C3;
        else if(isT3)
            return MATRIX.T3;
        else if (isC2)
            return MATRIX.C2;
        else if (isS2)
            return MATRIX.COMPACT;

        return MATRIX.FULL;
    }

    /**
     * Check input product format, get source bands and set corresponding flag.
     * @param srcProduct the input product
     * @param sourceProductType The source product type
     * @return QuadSourceBand[]
     * @throws org.esa.beam.framework.gpf.OperatorException if sourceProduct is not quad-pol
     */
    public static QuadSourceBand[] getSourceBands(final Product srcProduct,
                                                  final MATRIX sourceProductType) throws OperatorException {

        final boolean isCoregistered = StackUtils.isCoregisteredStack(srcProduct);
        final List<QuadSourceBand> quadSrcBandList = new ArrayList<QuadSourceBand>(10);

        if(isCoregistered) {
            final String[] mstBandNames = StackUtils.getMasterBandNames(srcProduct);
            final Band[] mstBands = getBands(srcProduct, sourceProductType, mstBandNames);
            final String suffix = mstBandNames[0].substring(mstBandNames[0].lastIndexOf('_'), mstBandNames[0].length());
            quadSrcBandList.add(new QuadSourceBand(srcProduct.getName(), mstBands, suffix));

            final String[] slvProductNames = StackUtils.getSlaveProductNames(srcProduct);
            for(String slvProd : slvProductNames) {
                final String[] slvBandNames = StackUtils.getSlaveBandNames(srcProduct, slvProd);
                final Band[] slvBands = getBands(srcProduct, sourceProductType, slvBandNames);
                final String suf = slvBandNames[0].substring(slvBandNames[0].lastIndexOf('_'), slvBandNames[0].length());
                quadSrcBandList.add(new QuadSourceBand(slvProd, slvBands, suf));
            }
        } else {
            final String[] bandNames = srcProduct.getBandNames();
            final Band[] mstBands = getBands(srcProduct, sourceProductType, bandNames);
            quadSrcBandList.add(new QuadSourceBand(srcProduct.getName(), mstBands, ""));
        }
        return quadSrcBandList.toArray(new QuadSourceBand[quadSrcBandList.size()]);
    }

    /**
     * Check input product format, get source bands and set corresponding flag.
     * @param srcProduct The source product
     * @param sourceProductType The source product type
     * @param bandNames the src band names
     * @return QuadSourceBand[]
     */
    private static Band[] getBands(final Product srcProduct, final MATRIX sourceProductType, final String[] bandNames) {

        if (sourceProductType == MATRIX.FULL) { // full pol
            return getQuadPolSrcBands(srcProduct, bandNames);
        } else if(sourceProductType == MATRIX.C3) { // C3
            return getProductBands(srcProduct, bandNames, getC3BandNames());
        } else if(sourceProductType == MATRIX.T3) { // T3
            return getProductBands(srcProduct, bandNames, getT3BandNames());
        } else if(sourceProductType == MATRIX.C4) {
            return getProductBands(srcProduct, bandNames, getC4BandNames());
        } else if(sourceProductType == MATRIX.T4) {
            return getProductBands(srcProduct, bandNames, getT4BandNames());
        } else if(sourceProductType == MATRIX.C2) { // compact pol C2
            return getProductBands(srcProduct, bandNames, getC2BandNames());
        } else if(sourceProductType == MATRIX.COMPACT) { // compact pol S2
            return getProductBands(srcProduct, bandNames, getS2BandNames());
        }
        return null;
    }

    private static Band[] getQuadPolSrcBands(final Product srcProduct, final String[] srcBandNames)
            throws OperatorException {

        final Band[] sourceBands = new Band[8];
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);

        int validBandCnt = 0;
        for (final String srcBandName : srcBandNames) {

            final Band band = srcProduct.getBand(srcBandName);
            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            if(!(bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY))
                continue;
            final String pol = OperatorUtils.getBandPolarization(band.getName(), absRoot);

            if (pol.contains("hh") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[0] = band;
                ++validBandCnt;
            } else if (pol.contains("hh") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[1] = band;
                ++validBandCnt;
            } else if (pol.contains("hv") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[2] = band;
                ++validBandCnt;
            } else if (pol.contains("hv") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[3] = band;
                ++validBandCnt;
            } else if (pol.contains("vh") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[4] = band;
                ++validBandCnt;
            } else if (pol.contains("vh") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[5] = band;
                ++validBandCnt;
            } else if (pol.contains("vv") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[6] = band;
                ++validBandCnt;
            } else if (pol.contains("vv") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[7] = band;
                ++validBandCnt;
            }
        }

        if (validBandCnt != 8) {
            throw new OperatorException("A full polarization product is expected as input.");
        }
        return sourceBands;
    }

    private static Band[] getProductBands(final Product srcProduct, final String[] srcBandNames,
                                            final String[] validBandNames) throws OperatorException {

        final Band[] sourceBands = new Band[validBandNames.length];

        int validBandCnt = 0;
        for (final String bandName : srcBandNames) {
            final Band band = srcProduct.getBand(bandName);
            if(band == null) {
                throw new OperatorException("Band "+bandName+" not found");
            }

            for(final String validName: validBandNames) {
                if(bandName.contains(validName)) {
                    sourceBands[validBandCnt++] = band;
                    break;
                }
            }
        }

        if (validBandCnt != validBandNames.length) {
            throw new OperatorException("Input is not a valid polarimetric matrix");
        }
        return sourceBands;
    }

    public static Band[] addBands(final Product targetProduct, final String[] targetBandNameList, final String suffix) {
        final List<Band> bandList = new ArrayList<Band>(targetBandNameList.length);
        for (String targetBandName : targetBandNameList) {

            final Band targetBand = new Band(targetBandName + suffix,
                                             ProductData.TYPE_FLOAT32,
                                             targetProduct.getSceneRasterWidth(),
                                             targetProduct.getSceneRasterHeight());

            if (targetBandName.contains("_real")) {
                targetBand.setUnit(Unit.REAL);
            } else if (targetBandName.contains("_imag")) {
                targetBand.setUnit(Unit.IMAGINARY);
            } else {
                targetBand.setUnit(Unit.INTENSITY);
            }

            bandList.add(targetBand);
            targetProduct.addBand(targetBand);
        }
        return bandList.toArray(new Band[bandList.size()]);
    }

    public static void saveNewBandNames(final Product targetProduct, final QuadSourceBand[] srcBandList) {
        if(StackUtils.isCoregisteredStack(targetProduct)) {
            boolean masterProduct = true;
            for(final PolBandUtils.QuadSourceBand bandList :srcBandList) {
                if(masterProduct) {
                    final String[] bandNames = StackUtils.bandsToStringArray(bandList.targetBands);
                    StackUtils.saveMasterProductBandNames(targetProduct, bandNames);
                    masterProduct = false;
                } else {
                    final String[] bandNames = StackUtils.bandsToStringArray(bandList.targetBands);
                    StackUtils.saveSlaveProductBandNames(targetProduct, bandList.productName, bandNames);
                }
            }
        }
    }

    /**
     * Get compact pol product source band names.
     * @return The source band names.
     */
    public static String[] getS2BandNames() {
        return new String[] {
            "i_RH",
            "q_RH",
            "i_RV",
            "q_RV",
        };
    }

    /**
     * Get compact pol covariance matrix product source band names.
     * @return The source band names.
     */
    public static String[] getC2BandNames() {
        return new String[] {
            "C11",
            "C12_real",
            "C12_imag",
            "C22",
        };
    }

    public static String[] getC3BandNames() {
        return new String[] {
            "C11",
            "C12_real",
            "C12_imag",
            "C13_real",
            "C13_imag",
            "C22",
            "C23_real",
            "C23_imag",
            "C33"
        };
    }

    public static String[] getC4BandNames() {
        return new String[] {
            "C11",
            "C12_real",
            "C12_imag",
            "C13_real",
            "C13_imag",
            "C14_real",
            "C14_imag",
            "C22",
            "C23_real",
            "C23_imag",
            "C24_real",
            "C24_imag",
            "C33",
            "C34_real",
            "C34_imag",
            "C44"
        };
    }

    public static String[] getT3BandNames() {
        return new String[] {
            "T11",
            "T12_real",
            "T12_imag",
            "T13_real",
            "T13_imag",
            "T22",
            "T23_real",
            "T23_imag",
            "T33"
        };
    }

    public static String[] getT4BandNames() {
        return new String[] {
            "T11",
            "T12_real",
            "T12_imag",
            "T13_real",
            "T13_imag",
            "T14_real",
            "T14_imag",
            "T22",
            "T23_real",
            "T23_imag",
            "T24_real",
            "T24_imag",
            "T33",
            "T34_real",
            "T34_imag",
            "T44"
        };
    }

    public static String getPolarType(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if(absRoot != null) {
            final String pol1 = absRoot.getAttributeString(AbstractMetadata.mds1_tx_rx_polar, "").trim();
            final String pol2 = absRoot.getAttributeString(AbstractMetadata.mds2_tx_rx_polar, "").trim();
            final String pol3 = absRoot.getAttributeString(AbstractMetadata.mds3_tx_rx_polar, "").trim();
            final String pol4 = absRoot.getAttributeString(AbstractMetadata.mds4_tx_rx_polar, "").trim();
            if(!pol1.isEmpty() && !pol2.isEmpty()) {
                if(!pol3.isEmpty() && !pol4.isEmpty()) {
                    return "full";
                }
                return "dual";
            }
        }
        return "single";
    }
}
