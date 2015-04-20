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
package org.esa.s1tbx.datamodel;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.ProductUtils;

import java.util.HashMap;

/**
 * Calibration base class.
 */

public class BaseCalibrator {

    protected Operator calibrationOp;
    protected Product sourceProduct;
    protected Product targetProduct;

    protected boolean outputImageInComplex = false;
    protected boolean outputImageScaleInDb = false;
    protected boolean isComplex = false;
    protected String incidenceAngleSelection = null;

    protected MetadataElement absRoot = null;
    protected MetadataElement origMetadataRoot = null;

    protected static final double underFlowFloat = 1.0e-30;

    protected final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<>(2);

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BaseCalibrator() {
    }

    /**
     * Set flag indicating if target image is output in complex.
     */
    public void setOutputImageInComplex(boolean flag) {
        outputImageInComplex = flag;
    }

    /**
     * Set flag indicating if target image is output in dB scale.
     */
    public void setOutputImageIndB(boolean flag) {
        outputImageScaleInDb = flag;
    }

    public void setIncidenceAngleForSigma0(String incidenceAngleForSigma0) {
        incidenceAngleSelection = incidenceAngleForSigma0;
    }

    /**
     * Get calibration flag from abstract metadata.
     */
    public void getCalibrationFlag() {
        if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
            throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
        }
    }

    /**
     * Get sample type from abstract metadata.
     */
    public void getSampleType() {
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if (sampleType.equals("COMPLEX")) {
            isComplex = true;
        }
    }


    /**
     * Create target product.
     */
    public Product createTargetProduct(final Product sourceProduct, final String[] sourceBandNames) {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands(sourceProduct, sourceBandNames);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        return targetProduct;
    }

    /**
     * Add the user selected bands to the target product.
     */
    private void addSelectedBands(final Product sourceProduct, final String[] sourceBandNames) {

        if (outputImageInComplex) {
            outputInComplex(sourceProduct, sourceBandNames);
        } else {
            outputInIntensity(sourceProduct, sourceBandNames);
        }
    }

    private void outputInComplex(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        for (int i = 0; i < sourceBands.length; i += 2) {

            final Band srcBandI = sourceBands[i];
            final String unit = srcBandI.getUnit();
            String nextUnit = null;
            if (unit == null) {
                throw new OperatorException("band " + srcBandI.getName() + " requires a unit");
            } else if (unit.contains(Unit.DB)) {
                throw new OperatorException("Calibration of bands in dB is not supported");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("I and Q bands should be selected in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
                nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
            } else {
                throw new OperatorException("Please select I and Q bands in pairs only");
            }

            final String[] srcBandINames = {srcBandI.getName()};
            targetBandNameToSourceBandName.put(srcBandINames[0], srcBandINames);
            final Band targetBandI = targetProduct.addBand(srcBandINames[0], ProductData.TYPE_FLOAT32);
            targetBandI.setUnit(unit);

            final Band srcBandQ = sourceBands[i + 1];
            final String[] srcBandQNames = {srcBandQ.getName()};
            targetBandNameToSourceBandName.put(srcBandQNames[0], srcBandQNames);
            final Band targetBandQ = targetProduct.addBand(srcBandQNames[0], ProductData.TYPE_FLOAT32);
            targetBandQ.setUnit(nextUnit);

            final String suffix = "_" + OperatorUtils.getSuffixFromBandName(srcBandI.getName());
            ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
        }
    }

    private void outputInIntensity(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        String targetBandName;
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            String targetUnit = Unit.INTENSITY;
            int targetType = ProductData.TYPE_FLOAT32;

            if (unit.contains(Unit.DB)) {

                throw new OperatorException("Calibration of bands in dB is not supported");
            } else if (unit.contains(Unit.PHASE)) {

                final String[] srcBandNames = {srcBand.getName()};
                targetBandName = srcBand.getName();
                targetType = srcBand.getDataType();
                targetUnit = Unit.PHASE;
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }

            } else if (unit.contains(Unit.IMAGINARY)) {

                throw new OperatorException("Real and imaginary bands should be selected in pairs");

            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length)
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");

                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String[] srcBandNames = new String[2];
                srcBandNames[0] = srcBand.getName();
                srcBandNames[1] = sourceBands[i + 1].getName();
                targetBandName = createTargetBandName(srcBandNames[0], absRoot);
                ++i;
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }

            } else {

                final String[] srcBandNames = {srcBand.getName()};
                targetBandName = createTargetBandName(srcBandNames[0], absRoot);
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }
            }

            // add band only if it doesn't already exist
            if (targetProduct.getBand(targetBandName) == null) {
                final Band targetBand = new Band(targetBandName,
                        targetType,
                        srcBand.getRasterWidth(),
                        srcBand.getRasterHeight());

                if (outputImageScaleInDb && !targetUnit.equals(Unit.PHASE)) {
                    targetUnit = Unit.INTENSITY_DB;
                }
                targetBand.setUnit(targetUnit);
                targetProduct.addBand(targetBand);
            }
        }
    }

    private String createTargetBandName(final String srcBandName, final MetadataElement absRoot) {
        final String pol = OperatorUtils.getBandPolarization(srcBandName, absRoot);
        String targetBandName = "Sigma0";
        if (pol != null && !pol.isEmpty()) {
            targetBandName = "Sigma0_" + pol.toUpperCase();
        }
        if (outputImageScaleInDb) {
            targetBandName += "_dB";
        }
        return targetBandName;
    }

}
