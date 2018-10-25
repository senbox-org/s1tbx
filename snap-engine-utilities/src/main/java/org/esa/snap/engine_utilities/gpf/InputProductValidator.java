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
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;

/**
 * Validates input products using commonly used verifications
 */
public class InputProductValidator {

    private final Product product;
    private final MetadataElement absRoot;

    private final static String SHOULD_BE_SAR_PRODUCT = "Input should be a SAR product";
    private final static String SHOULD_NOT_BE_LEVEL0 = "Level-0 RAW products are not supported";
    private final static String SHOULD_BE_COREGISTERED = "Input should be a coregistered stack.";
    private final static String SHOULD_BE_SLC = "Input should be a single look complex SLC product.";
    private final static String SHOULD_BE_GRD = "Input should be a detected product.";
    private final static String SHOULD_BE_S1 = "Input should be a Sentinel-1 product.";
    private final static String SHOULD_BE_DEBURST = "Source product should first be deburst.";
    private final static String SHOULD_BE_MULTISWATH_SLC = "Source product should be multi sub-swath SLC burst product.";
    private final static String SHOULD_BE_QUAD_POL = "Input should be a full pol SLC product.";
    private final static String SHOULD_BE_CALIBRATED = "Source product should be calibrated.";
    private final static String SHOULD_NOT_BE_CALIBRATED = "Source product has already been calibrated.";
    private final static String SHOULD_BE_MAP_PROJECTED = "Source product should be map projected.";
    private final static String SHOULD_NOT_BE_MAP_PROJECTED = "Source product should not be map projected.";
    private final static String SHOULD_BE_COMPATIBLE = "Source products do not have compatible dimensions and geocoding.";

    private final static float geographicError = 1.0e-3f;

    public InputProductValidator(final Product product) throws OperatorException {
        this.product = product;
        absRoot = AbstractMetadata.getAbstractedMetadata(product);
    }

    public boolean isSARProduct() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        return absRoot != null && absRoot.getAttributeDouble("radar_frequency", 99999) != 99999;
    }

    public void checkIfSARProduct() {
        if("RAW".equals(product.getProductType())) {
            throw new OperatorException(SHOULD_NOT_BE_LEVEL0);
        }
        if(!isSARProduct()) {
            throw new OperatorException(SHOULD_BE_SAR_PRODUCT);
        }
    }

    public void checkIfCoregisteredStack() throws OperatorException {
        if (!StackUtils.isCoregisteredStack(product)) {
            throw new OperatorException(SHOULD_BE_COREGISTERED);
        }
    }

    public boolean isComplex() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, AbstractMetadata.NO_METADATA_STRING).trim();
            if (sampleType.equalsIgnoreCase("complex"))
                return true;
        }
        return false;
    }

    public void checkIfSLC() throws OperatorException {
        if (!isComplex()) {
            throw new OperatorException(SHOULD_BE_SLC);
        }
    }

    public void checkIfGRD() throws OperatorException {
        if (isComplex()) {
            throw new OperatorException(SHOULD_BE_GRD);
        }
    }

    public boolean isMultiSwath() {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") && contains(bandNames, "IW2")) ||
                (contains(bandNames, "EW1") && contains(bandNames, "EW2"));
    }

    public boolean isSentinel1Product() throws OperatorException {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        return mission.startsWith("SENTINEL-1");
    }

    public void checkIfSentinel1Product() throws OperatorException {
        if (!isSentinel1Product()) {
            throw new OperatorException(SHOULD_BE_S1);
        }
    }

    public void checkMission(final String[] validMissions) throws OperatorException {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION, "").toUpperCase();
        for (String validMission : validMissions) {
            if (mission.startsWith(validMission.toUpperCase()))
                return;
        }
        throw new OperatorException(mission + " is not a valid mission from: " + StringUtils.arrayToString(validMissions, ","));
    }

    public void checkProductType(final String[] validProductTypes) throws OperatorException {
        final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE, "");
        for (String validProductType : validProductTypes) {
            if (productType.equals(validProductType))
                return;
        }
        throw new OperatorException(productType + " is not a valid product type from: " + StringUtils.arrayToString(validProductTypes, ","));
    }

    public void checkAcquisitionMode(final String[] validModes) throws OperatorException {
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        for (String validMode : validModes) {
            if (acquisitionMode.equals(validMode))
                return;
        }
        throw new OperatorException(acquisitionMode + " is not a valid acquisition mode from: " + StringUtils.arrayToString(validModes, ","));
    }

    public boolean isTOPSARProduct() {
        boolean isS1 = false;
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION, "");
        if (mission.startsWith("SENTINEL-1") || mission.startsWith("RS2")) {  // also include RS2 in TOPS mode
            isS1 = true;
        }
        final String[] bandNames = product.getBandNames();
        return isS1 && (contains(bandNames, "IW1") || contains(bandNames, "IW2") || contains(bandNames, "IW3") ||
                contains(bandNames, "EW1") || contains(bandNames, "EW2") || contains(bandNames, "EW3") ||
                contains(bandNames, "EW4") || contains(bandNames, "EW5"));
    }

    public void checkIfTOPSARBurstProduct(final boolean shouldbe) throws OperatorException {

        final boolean isTOPSARProduct = isTOPSARProduct();
        if (shouldbe && !isTOPSARProduct) {
            // It should be a TOP SAR Burst product but it is not even a TOP SAR Product
            throw new OperatorException("Source product should be an SLC burst product");
        } else if (shouldbe && isTOPSARProduct && isDebursted()) {
            // It should be a TOP SAR Burst product and it is a TOP SAR product but it has been deburst
            throw new OperatorException("Source product should NOT be a deburst product");
        } else if (!shouldbe && isTOPSARProduct && !isDebursted()) {
            // It should not be a TOP SAR burst product but it is.
            throw new OperatorException(SHOULD_BE_DEBURST);
        }
    }

    public void checkIfMultiSwathTOPSARProduct() throws OperatorException {
        if (!isMultiSwath()) {
            throw new OperatorException(SHOULD_BE_MULTISWATH_SLC);
        }
    }

    public boolean isDebursted() {
        if(!isSentinel1Product())
            return true;

        boolean isDebursted = true;
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        MetadataElement annotation = origProdRoot.getElement("annotation");
        if (annotation == null) {
            return true;
        }

        final MetadataElement[] elems = annotation.getElements();
        for (MetadataElement elem : elems) {
            final MetadataElement product = elem.getElement("product");
            final MetadataElement swathTiming = product.getElement("swathTiming");
            final MetadataElement burstList = swathTiming.getElement("burstList");
            final int count = Integer.parseInt(burstList.getAttributeString("count"));
            if (count != 0) {
                isDebursted = false;
                break;
            }
        }
        return isDebursted;
    }

    private static boolean contains(final String[] list, final String tag) {
        for (String s : list) {
            if (s.contains(tag))
                return true;
        }
        return false;
    }

    public boolean isFullPolSLC() {

        int validBandCnt = 0;
        for (final Band band : product.getBands()) {

            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            if (!(bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY))
                continue;
            final String pol = OperatorUtils.getPolarizationFromBandName(band.getName());
            if(pol == null)
                continue;

            if (pol.contains("hh") || pol.contains("hv") || pol.contains("vh") || pol.contains("vv")) {
                ++validBandCnt;
            }
        }

        return validBandCnt == 8;
    }

    public void checkIfQuadPolSLC() throws OperatorException {
        if (!isFullPolSLC()) {
            throw new OperatorException(SHOULD_BE_QUAD_POL);
        }
    }

    public boolean isMapProjected() {
        if (product.getSceneGeoCoding() instanceof MapGeoCoding || product.getSceneGeoCoding() instanceof CrsGeoCoding)
            return true;
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        return absRoot != null && !AbstractMetadata.isNoData(absRoot, AbstractMetadata.map_projection);
    }

    public void checkIfMapProjected(final boolean shouldBe) throws OperatorException {
        final boolean isMapProjected = isMapProjected();
        if (!shouldBe && isMapProjected) {
            throw new OperatorException(SHOULD_NOT_BE_MAP_PROJECTED);
        } else if (shouldBe && !isMapProjected) {
            throw new OperatorException(SHOULD_BE_MAP_PROJECTED);
        }
    }

    public boolean isCalibrated() {
        return (absRoot != null &&
                absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean());
    }

    public void checkIfCalibrated(final boolean shouldBe) throws OperatorException {
        final boolean isCalibrated = isCalibrated();
        if (!shouldBe && isCalibrated) {
            throw new OperatorException(SHOULD_NOT_BE_CALIBRATED);
        } else if (shouldBe && !isCalibrated) {
            throw new OperatorException(SHOULD_BE_CALIBRATED);
        }
    }

    public void checkIfTanDEMXProduct() throws OperatorException {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("TDM")) {
            throw new OperatorException("Input should be a TanDEM-X product.");
        }
    }

    public void checkIfCompatibleProducts(final Product[] sourceProducts) {
        for(Product srcProduct : sourceProducts) {
            if(!product.isCompatibleProduct(srcProduct, geographicError)) {
                throw new OperatorException(SHOULD_BE_COMPATIBLE);
            }
        }
    }
}
