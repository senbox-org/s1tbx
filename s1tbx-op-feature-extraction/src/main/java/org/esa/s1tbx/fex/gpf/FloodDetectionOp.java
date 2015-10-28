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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 simple detection of water in slave where there wasn't any water in the master
 */

@OperatorMetadata(alias = "Flood-Detection",
        category = "Radar/Feature Extraction",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Detect flooded area.", internal = false)
public class FloodDetectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

   // @Parameter(description = "The list of source bands.", alias = "sourceBands",
    //        rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<>();

    public static final String MASK_NAME = "_Flood";

    @Override
    public void initialize() throws OperatorException {

        try {

            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            addSelectedBands();

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

       // if (sourceBandNames == null || sourceBandNames.length == 0) { // if user did not select any band
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (band.getUnit() != null && (band.getUnit().startsWith(Unit.INTENSITY) || band.getUnit().startsWith(Unit.CLASS)))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        //}

        final Band[] sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final String sourceBandName = sourceBandNames[i];
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }

        if(sourceBandNames.length == 0) {
            throw new OperatorException("No calibrated bands founds");
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

            ProductUtils.copyBand(srcBandNames, sourceProduct, targetProduct, true);
        }

        final Band mstBand = targetProduct.getBandAt(0);
        final Band slvBand = targetProduct.getNumBands() > 1 ? targetProduct.getBandAt(1) : null;
        final Band terrainMask = targetProduct.getBand("Terrain_Mask");
        final Band globCover = targetProduct.getBand("GlobCover");
        final Band homogeneity = targetProduct.getBand("Homogeneity");
        final Band energy = targetProduct.getBand("Energy");

        final boolean isdB = mstBand.getUnit().contains(Unit.DB);

        //create Mask
        String expression;
        if(isdB) {
            expression = "(" + mstBand.getName() + " < -13 )";
            if (slvBand != null) {
                expression = "!(" + mstBand.getName() + " < -13 )" +
                        " && (" + slvBand.getName() + " < -13 )";
            }
        } else {
            expression = "(" + mstBand.getName() + " < 0.05 && " + mstBand.getName() + " > 0)";
            if (slvBand != null) {
                expression = "!(" + mstBand.getName() + " < 0.05 && " + mstBand.getName() + " > 0)" +
                        " && (" + slvBand.getName() + " < 0.05 && " + slvBand.getName() + " > 0)";
            }
        }

        if (terrainMask != null) {
            expression += " && " + terrainMask.getName() + " == 0";
        }
        if (globCover != null) {
            expression += " && " + globCover.getName() + " != 210"; // existing water
        }

        if (energy != null) {
          //  expression += " && " + energy.getName() + " > 0.8";
        } else if (homogeneity != null) {
         //   expression += " && " + homogeneity.getName() + " > 0.6";
        }

        final Mask mask = new Mask(mstBand.getName() + "_flood",
                mstBand.getRasterWidth(),
                mstBand.getRasterHeight(),
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
