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
package org.esa.s1tbx.commons.polsar;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for handling polarimetric bands
 */
public class PolBandUtils {

    public enum MATRIX {DUAL_HH_HV, DUAL_VH_VV, DUAL_HH_VV, C2, LCHCP, RCHCP, C3, T3, C4, T4, FULL, UNKNOWN}

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
            } else if (name.contains("LH") || name.contains("LCH") || name.contains("LCV")) {
                isLCHS2 = true;
            } else if (name.contains("RH") || name.contains("RCH") || name.contains("RCV")) {
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
     * @throws Exception if sourceProduct is not quad-pol
     */
    public static PolSourceBand[] getSourceBands(final Product srcProduct,
                                                  final MATRIX sourceProductType) throws Exception {

        final boolean isCoregistered = StackUtils.isCoregisteredStack(srcProduct);
        final List<PolSourceBand> quadSrcBandList = new ArrayList<>(10);

        if (isCoregistered) {
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
            return getDualPolSrcBands(srcProduct, getComplexBandNames(), sourceProductType);
        } else if (sourceProductType == MATRIX.DUAL_VH_VV) { // dual VH VV
            return getDualPolSrcBands(srcProduct, getComplexBandNames(), sourceProductType);
        } else if (sourceProductType == MATRIX.DUAL_HH_VV) { // dual HH VV
            return getDualPolSrcBands(srcProduct, getComplexBandNames(), sourceProductType);
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

    private static Band[] getDualPolSrcBands(final Product srcProduct, final String[] srcBandNames,
                                             final MATRIX sourceProductType) {

        final List<Band> hhBandList = new ArrayList<>();
        final List<Band> hvBandList = new ArrayList<>();
        final List<Band> vvBandList = new ArrayList<>();
        final List<Band> vhBandList = new ArrayList<>();
        for(Band srcBand : srcProduct.getBands()) {
            final String bandName = srcBand.getName();
            for (String s : srcBandNames) {
                if(bandName.startsWith(s)) {
                    if (bandName.toLowerCase().contains("hh")) {
                        hhBandList.add(srcBand);
                    } else if (bandName.toLowerCase().contains("hv")) {
                        hvBandList.add(srcBand);
                    } else if (bandName.toLowerCase().contains("vv")) {
                        vvBandList.add(srcBand);
                    } else if (bandName.toLowerCase().contains("vh")) {
                        vhBandList.add(srcBand);
                    }
                }
            }
        }

        if (sourceProductType == MATRIX.DUAL_HH_HV) {
            hhBandList.addAll(hvBandList);
            return hhBandList.toArray(new Band[hhBandList.size()]);
        } else if (sourceProductType == MATRIX.DUAL_VH_VV) {
            vvBandList.addAll(vhBandList);
            return vvBandList.toArray(new Band[vvBandList.size()]);
        } else if (sourceProductType == MATRIX.DUAL_HH_VV) {
            hhBandList.addAll(vvBandList);
            return hhBandList.toArray(new Band[hhBandList.size()]);
        }

        return null;
    }

    private static Band[] getQuadPolSrcBands(final Product srcProduct, final String[] srcBandNames) throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final boolean isComplex = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE).equals("COMPLEX");

        if(!isComplex) {
            final List<Band> bandList = new ArrayList<>();
            for (final String srcBandName : srcBandNames) {
                final Band band = srcProduct.getBand(srcBandName);
                final String bandUnit = band.getUnit();
                if (bandUnit == null || !bandUnit.contains(Unit.INTENSITY))
                    continue;
                final String pol = OperatorUtils.getBandPolarization(band.getName(), absRoot);

                if (pol.contains("hh") || pol.contains("hv") || pol.contains("vh") || pol.contains("vv")) {
                    bandList.add(band);
                }
            }

            if(bandList.size() < 4) {
                throw new Exception("A full polarization product is expected as input.");
            }
            return bandList.toArray(new Band[bandList.size()]);
        }

        int validBandCnt = 0;
        final Band[] sourceBands = new Band[8];
        for (final String srcBandName : srcBandNames) {

            final Band band = srcProduct.getBand(srcBandName);
            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            if (!(bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY))
                continue;
            final String pol = OperatorUtils.getBandPolarization(band.getName(), absRoot);

            if (pol.contains("hh")) {
                if (bandUnit.equals(Unit.UnitType.REAL)) {
                    sourceBands[0] = band;
                    ++validBandCnt;
                } else if (bandUnit.equals(Unit.UnitType.IMAGINARY)) {
                    sourceBands[1] = band;
                    ++validBandCnt;
                }
            } else if (pol.contains("hv")) {
                if (bandUnit.equals(Unit.UnitType.REAL)) {
                    sourceBands[2] = band;
                    ++validBandCnt;
                } else if (bandUnit.equals(Unit.UnitType.IMAGINARY)) {
                    sourceBands[3] = band;
                    ++validBandCnt;
                }
            } else if (pol.contains("vh")) {
                if (bandUnit.equals(Unit.UnitType.REAL)) {
                    sourceBands[4] = band;
                    ++validBandCnt;
                } else if (bandUnit.equals(Unit.UnitType.IMAGINARY)) {
                    sourceBands[5] = band;
                    ++validBandCnt;
                }
            } else if (pol.contains("vv")) {
                if (bandUnit.equals(Unit.UnitType.REAL)) {
                    sourceBands[6] = band;
                    ++validBandCnt;
                } else if (bandUnit.equals(Unit.UnitType.IMAGINARY)) {
                    sourceBands[7] = band;
                    ++validBandCnt;
                }
            }
        }

        if (validBandCnt != 8) {
            throw new Exception("A full polarization product is expected as input.");
        }
        return sourceBands;
    }

    private static Band[] getProductBands(final Product srcProduct, final String[] srcBandNames,
                                          final String[] validBandNames) throws Exception {

        final Band[] sourceBands = new Band[validBandNames.length];

        int validBandCnt = 0;
        for (final String bandName : srcBandNames) {
            final Band band = srcProduct.getBand(bandName);
            if (band == null) {
                throw new Exception("Band " + bandName + " not found");
            }

            for (final String validName : validBandNames) {
                if (bandName.contains(validName)) {
                    sourceBands[validBandCnt++] = band;
                    break;
                }
            }
        }

        if (validBandCnt != validBandNames.length) {
            throw new Exception("Input is not a valid polarimetric matrix");
        }
        return sourceBands;
    }

    public static void saveNewBandNames(final Product targetProduct, final PolSourceBand[] srcBandList) throws Exception {
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

    public static String[] getComplexBandNames() {
        return new String[]{
                "i_",
                "q_"
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
                "i_LCH",
                "q_LCH",
                "i_LCV",
                "q_LCV",
        };
    }

    /**
     * Get band names for Left Circular Hybrid mode compact pol scattering vector product.
     *
     * @return The source band names.
     */
    public static String[] getRCHModeS2BandNames() {
        return new String[]{
                "i_RCH",
                "q_RCH",
                "i_RCV",
                "q_RCV",
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

    public static String getPolarType(final Product product) {
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
