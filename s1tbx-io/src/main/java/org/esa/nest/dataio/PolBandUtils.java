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
package org.esa.nest.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.StackUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for handling polarimetric bands
 */
public class PolBandUtils {

    public static enum MATRIX {DUAL_HH_HV, DUAL_VH_VV, DUAL_HH_VV, C2, LCHCP, RCHCP, C3, T3, C4, T4, FULL, UNKNOWN}

    public static class PolSourceBand {
        public final String productName;
        public final Band[] srcBands;
        public final String suffix;
        public Band[] targetBands;

        public double spanMin = 1e+30;
        public double spanMax = -1e+30;
        public boolean spanMinMaxSet = false;

        public PolSourceBand(final String productName, final Band[] bands, final String suffix) {
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
     *
     * @param sourceProduct The source product
     * @return The product type
     */
    public static MATRIX getSourceProductType(final Product sourceProduct) {

        final String[] bandNames = sourceProduct.getBandNames();
        boolean isC3 = false, isT3 = false, isC2 = false, isLCHS2 = false, isRCHS2 = false;
        boolean isHH = false, isHV = false, isVV = false, isVH = false;
        for (String name : bandNames) {
            if (name.contains("C44")) {
                return MATRIX.C4;
            } else if (name.contains("T44")) {
                return MATRIX.T4;
            } else if (name.contains("C33")) {
                isC3 = true;
            } else if (name.contains("T33")) {
                isT3 = true;
            } else if (name.contains("C22")) {
                isC2 = true;
            } else if (name.contains("LH")) {
                isLCHS2 = true;
            } else if (name.contains("RH")) {
                isRCHS2 = true;
            } else if (name.contains("_HH")) {
                isHH = true;
            } else if (name.contains("_HV")) {
                isHV = true;
            } else if (name.contains("_VV")) {
                isVV = true;
            } else if (name.contains("_VH")) {
                isVH = true;
            }
        }

        if (isC3)
            return MATRIX.C3;
        else if (isT3)
            return MATRIX.T3;
        else if (isC2)
            return MATRIX.C2;
        else if (isLCHS2)
            return MATRIX.LCHCP;
        else if (isRCHS2)
            return MATRIX.RCHCP;
        else if (isHH && isHV && !isVH && !isVV)
            return MATRIX.DUAL_HH_HV;
        else if (!isHH && !isHV && isVH && isVV)
            return MATRIX.DUAL_VH_VV;
        else if (isHH && !isHV && !isVH && isVV)
            return MATRIX.DUAL_HH_VV;
        else if (isHH && isHV && isVH && isVV)
            return MATRIX.FULL;

        return MATRIX.UNKNOWN;
    }

    /**
     * Check input product format, get source bands and set corresponding flag.
     *
     * @param srcProduct        the input product
     * @param sourceProductType The source product type
     * @return QuadSourceBand[]
     * @throws org.esa.beam.framework.gpf.OperatorException if sourceProduct is not quad-pol
     */
    public static PolSourceBand[] getSourceBands(final Product srcProduct,
                                                  final MATRIX sourceProductType) throws Exception {

        final boolean isCoregistered = StackUtils.isCoregisteredStack(srcProduct);
        final List<PolSourceBand> quadSrcBandList = new ArrayList<PolSourceBand>(10);

        if (isCoregistered && !sourceProductType.equals(PolBandUtils.MATRIX.C2)) {
            final String[] mstBandNames = StackUtils.getMasterBandNames(srcProduct);
            final Band[] mstBands = getBands(srcProduct, sourceProductType, mstBandNames);
            final String suffix = mstBandNames[0].substring(mstBandNames[0].lastIndexOf('_'), mstBandNames[0].length());
            quadSrcBandList.add(new PolSourceBand(srcProduct.getName(), mstBands, suffix));

            final String[] slvProductNames = StackUtils.getSlaveProductNames(srcProduct);
            for (String slvProd : slvProductNames) {
                final String[] slvBandNames = StackUtils.getSlaveBandNames(srcProduct, slvProd);
                final Band[] slvBands = getBands(srcProduct, sourceProductType, slvBandNames);
                final String suf = slvBandNames[0].substring(slvBandNames[0].lastIndexOf('_'), slvBandNames[0].length());
                quadSrcBandList.add(new PolSourceBand(slvProd, slvBands, suf));
            }
        } else {
            final String[] bandNames = srcProduct.getBandNames();
            final Band[] mstBands = getBands(srcProduct, sourceProductType, bandNames);
            quadSrcBandList.add(new PolSourceBand(srcProduct.getName(), mstBands, ""));
        }
        return quadSrcBandList.toArray(new PolSourceBand[quadSrcBandList.size()]);
    }

    /**
     * Check input product format, get source bands and set corresponding flag.
     *
     * @param srcProduct        The source product
     * @param sourceProductType The source product type
     * @param bandNames         the src band names
     * @return QuadSourceBand[]
     */
    private static Band[] getBands(final Product srcProduct, final MATRIX sourceProductType, final String[] bandNames) throws Exception {

        if (sourceProductType == MATRIX.DUAL_HH_HV) { // dual pol HH HV
            return getDualPolSrcBands(srcProduct, getDualPolHHHVBandNames());
        } else if (sourceProductType == MATRIX.DUAL_VH_VV) { // dual VH VV
            return getDualPolSrcBands(srcProduct, getDualPolVHVVBandNames());
        } else if (sourceProductType == MATRIX.DUAL_HH_VV) { // dual HH VV
            return getDualPolSrcBands(srcProduct, getDualPolHHVVBandNames());
        }else if (sourceProductType == MATRIX.FULL) { // full pol
            return getQuadPolSrcBands(srcProduct, bandNames);
        } else if (sourceProductType == MATRIX.C3) { // C3
            return getProductBands(srcProduct, bandNames, getC3BandNames());
        } else if (sourceProductType == MATRIX.T3) { // T3
            return getProductBands(srcProduct, bandNames, getT3BandNames());
        } else if (sourceProductType == MATRIX.C4) {
            return getProductBands(srcProduct, bandNames, getC4BandNames());
        } else if (sourceProductType == MATRIX.T4) {
            return getProductBands(srcProduct, bandNames, getT4BandNames());
        } else if (sourceProductType == MATRIX.C2) { // compact pol C2
            return getProductBands(srcProduct, bandNames, getC2BandNames());
        } else if (sourceProductType == MATRIX.LCHCP) { // LCH compact pol S2
            return getProductBands(srcProduct, bandNames, getLCHModeS2BandNames());
        } else if (sourceProductType == MATRIX.RCHCP) { // RCH compact pol S2
            return getProductBands(srcProduct, bandNames, getRCHModeS2BandNames());
        }
        return null;
    }

    private static Band[] getDualPolSrcBands(final Product srcProduct, final String[] srcBandNames) {

        Band[] bands = new Band[srcBandNames.length];
        int idx = 0;
        for (String s : srcBandNames) {
            bands[idx++] = srcProduct.getBand(s);
        }
        return bands;
    }

    private static Band[] getQuadPolSrcBands(final Product srcProduct, final String[] srcBandNames)
            throws Exception {

        final Band[] sourceBands = new Band[8];
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);

        int validBandCnt = 0;
        for (final String srcBandName : srcBandNames) {

            final Band band = srcProduct.getBand(srcBandName);
            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            if (!(bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY))
                continue;
            final String pol = OperatorUtils.getBandPolarization(band.getName(), absRoot);

            if (pol.contains("hh") && bandUnit == Unit.UnitType.REAL) {
                sourceBands[0] = band;
                ++validBandCnt;
            } else if (pol.contains("hh") && bandUnit == Unit.UnitType.IMAGINARY) {
                sourceBands[1] = band;
                ++validBandCnt;
            } else if (pol.contains("hv") && bandUnit == Unit.UnitType.REAL) {
                sourceBands[2] = band;
                ++validBandCnt;
            } else if (pol.contains("hv") && bandUnit == Unit.UnitType.IMAGINARY) {
                sourceBands[3] = band;
                ++validBandCnt;
            } else if (pol.contains("vh") && bandUnit == Unit.UnitType.REAL) {
                sourceBands[4] = band;
                ++validBandCnt;
            } else if (pol.contains("vh") && bandUnit == Unit.UnitType.IMAGINARY) {
                sourceBands[5] = band;
                ++validBandCnt;
            } else if (pol.contains("vv") && bandUnit == Unit.UnitType.REAL) {
                sourceBands[6] = band;
                ++validBandCnt;
            } else if (pol.contains("vv") && bandUnit == Unit.UnitType.IMAGINARY) {
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
            if (band == null) {
                throw new OperatorException("Band " + bandName + " not found");
            }

            for (final String validName : validBandNames) {
                if (bandName.contains(validName)) {
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

    public static void saveNewBandNames(final Product targetProduct, final PolSourceBand[] srcBandList) {
        if (StackUtils.isCoregisteredStack(targetProduct)) {
            boolean masterProduct = true;
            for (final PolSourceBand bandList : srcBandList) {
                if (masterProduct) {
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

    public static boolean isDualPol(final MATRIX m) {
        return m == MATRIX.DUAL_HH_HV || m == MATRIX.DUAL_VH_VV || m == MATRIX.DUAL_HH_VV ||
                m == MATRIX.C2 || m == MATRIX.LCHCP || m == MATRIX.RCHCP;
    }

    public static boolean isQuadPol(final MATRIX m) {
        return m == MATRIX.C3 || m == MATRIX.T3 || m == MATRIX.C4 || m == MATRIX.T4;
    }

    public static boolean isFullPol(final MATRIX m) {
        return m == MATRIX.FULL;
    }

    public static String[] getDualPolHHHVBandNames() {
        return new String[]{
                "i_HH",
                "q_HH",
                "i_HV",
                "q_HV"
        };
    }

    public static String[] getDualPolVHVVBandNames() {
        return new String[]{
                "i_VH",
                "q_VH",
                "i_VV",
                "q_VV"
        };
    }

    public static String[] getDualPolHHVVBandNames() {
        return new String[]{
                "i_HH",
                "q_HH",
                "i_VV",
                "q_VV"
        };
    }

    /**
     * Get band names for compact pol Stokes vector product.
     *
     * @return The source band names.
     */
    public static String[] getG4BandNames() {
        return new String[]{
                "g0",
                "g1",
                "g2",
                "g3",
        };
    }

    /**
     * Get band names for Right Circular Hybrid mode compact pol scattering vector product.
     *
     * @return The source band names.
     */
    public static String[] getLCHModeS2BandNames() {
        return new String[]{
                "i_LH",
                "q_LH",
                "i_LV",
                "q_LV",
        };
    }

    /**
     * Get band names for Left Circular Hybrid mode compact pol scattering vector product.
     *
     * @return The source band names.
     */
    public static String[] getRCHModeS2BandNames() {
        return new String[]{
                "i_RH",
                "q_RH",
                "i_RV",
                "q_RV",
        };
    }

    /**
     * Get compact pol covariance matrix product source band names.
     *
     * @return The source band names.
     */
    public static String[] getC2BandNames() {
        return new String[]{
                "C11",
                "C12_real",
                "C12_imag",
                "C22",
        };
    }

    public static String[] getC3BandNames() {
        return new String[]{
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
        return new String[]{
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
        return new String[]{
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
        return new String[]{
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

    public static String getPolarType(final Product product) throws Exception {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            if (!AbstractMetadata.isNoData(absRoot, AbstractMetadata.mds1_tx_rx_polar) &&
                    !AbstractMetadata.isNoData(absRoot, AbstractMetadata.mds2_tx_rx_polar)) {
                if (!AbstractMetadata.isNoData(absRoot, AbstractMetadata.mds3_tx_rx_polar) &&
                        !AbstractMetadata.isNoData(absRoot, AbstractMetadata.mds4_tx_rx_polar)) {
                    return "full";
                }
                return "dual";
            }
        }
        return "single";
    }

    public static boolean isBandForMatrixElement(final String bandName, final String elemPrefix) {

        return bandName.length() > elemPrefix.length() &&
                bandName.substring(1, elemPrefix.length()+1).equals(elemPrefix);
    }
}
