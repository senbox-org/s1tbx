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
package org.esa.s1tbx.fex.gpf;

import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Mask;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.util.ProductUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The urban area detection operator.
 * <p/>
 * The operator implements the algorithm given in [1].
 * <p/>
 * [1] T. Esch, M. Thiel, A. Schenk, A. Roth, A. Müller, and S. Dech,
 * "Delineation of Urban Footprints From TerraSAR-X Data by Analyzing
 * Speckle Characteristics and Intensity Information," IEEE Transactions
 * on Geoscience and Remote Sensing, vol. 48, no. 2, pp. 905-916, 2010.
 */

@OperatorMetadata(alias = "Flood-Detection",
        category = "Image Analysis/Feature Extraction",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Detect flooded area.", internal = true)
public class FloodDetectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<>();

    public static final String MASK_NAME = "_Flood";

    @Override
    public void initialize() throws OperatorException {

        try {

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws org.esa.snap.framework.gpf.OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) { // if user did not select any band
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final Band[] sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final String sourceBandName = sourceBandNames[i];
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }

        for (Band srcBand : sourceBands) {
            final String srcBandNames = srcBand.getName();
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBandNames + " requires a unit");
            }

            if (unit.contains(Unit.IMAGINARY) || unit.contains(Unit.REAL) || unit.contains(Unit.PHASE)) {
                throw new OperatorException("Please select amplitude or intensity band");
            }

            final String targetBandName = srcBandNames + MASK_NAME;
            targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

            final Band targetBand = ProductUtils.copyBand(srcBandNames, sourceProduct, targetProduct, false);
            targetBand.setSourceImage(srcBand.getSourceImage());
        }

        final Band mstBand = targetProduct.getBandAt(0);
        final Band slvBand = targetProduct.getBandAt(1);
        final Band terrainMask = targetProduct.getBand("Terrain_Mask");

        //create Mask
        String expression = "(" + mstBand.getName() + " < 0.05 && " + mstBand.getName() + " > 0)";
        if (slvBand != null) {
            expression += " && !(" + slvBand.getName() + " < 0.05 && " + slvBand.getName() + " > 0)";
        }
        if (terrainMask != null) {
            expression += " && " + terrainMask.getName() + " == 0";
        }

        final Mask mask = new Mask(mstBand.getName() + "_flood",
                mstBand.getSceneRasterWidth(),
                mstBand.getSceneRasterHeight(),
                Mask.BandMathsType.INSTANCE);

        mask.setDescription("Flood");
        mask.getImageConfig().setValue("color", Color.BLUE);
        mask.getImageConfig().setValue("transparency", 0.7);
        mask.getImageConfig().setValue("expression", expression);
        mask.setNoDataValue(0);
        mask.setNoDataValueUsed(true);
        targetProduct.getMaskGroup().add(mask);
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FloodDetectionOp.class);
        }
    }
}
