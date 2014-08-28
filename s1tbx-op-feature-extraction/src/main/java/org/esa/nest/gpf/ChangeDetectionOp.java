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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The change detection operator.
 * <p/>
 * The operator performs change detection by computing the ratio of log ratio of given image pair.
 * It is assumed that the input product is a stack of two co-registered images.
 * <p/>
 */

@OperatorMetadata(alias = "Change-Detection",
        category = "Classification/Primitive Features",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Change Detection.")
public class ChangeDetectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Include source bands", defaultValue = "false", label = "Include source bands")
    private boolean includeSourceBands = false;

    @Parameter(description = "Output Log Ratio", defaultValue = "false", label = "Output Log Ratio")
    private boolean outputLogRatio = false;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private String ratioBandName;

    private static String RATIO_BAND_NAME = "ratio";
    private static String LOG_RATIO_BAND_NAME = "log_ratio";

    @Override
    public void initialize() throws OperatorException {

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

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
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) { // if user did not select any band
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (band.getUnit() != null && band.getUnit().contains(Unit.INTENSITY)) {
                    bandNameList.add(band.getName());
                    if(bandNameList.size() == 2)
                        break;
                }
            }
            if(bandNameList.size() < 2) {
                bandNameList.clear();
                for (Band band : bands) {
                    if (band.getUnit() != null && band.getUnit().contains(Unit.AMPLITUDE)) {
                        bandNameList.add(band.getName());
                        if(bandNameList.size() == 2)
                            break;
                    }
                }
            }
            if(bandNameList.size() < 2) {
                bandNameList.clear();
                for (Band band : bands) {
                    bandNameList.add(band.getName());
                    if(bandNameList.size() == 2)
                        break;
                }
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        if (sourceBandNames.length != 2) {
            throw new OperatorException("Please select two source bands");
        }

        if(includeSourceBands) {
            for(String srcBandName : sourceBandNames) {
                ProductUtils.copyBand(srcBandName, sourceProduct, targetProduct, true);
            }
        }

        ratioBandName = RATIO_BAND_NAME;
        if (outputLogRatio) {
            ratioBandName = LOG_RATIO_BAND_NAME;
        }

        final Band targetRatioBand = new Band(ratioBandName,
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        targetRatioBand.setNoDataValue(0);
        targetRatioBand.setNoDataValueUsed(true);
        if (outputLogRatio) {
            targetRatioBand.setUnit("log_ratio");
        } else {
            targetRatioBand.setUnit("ratio");
        }
        targetProduct.addBand(targetRatioBand);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final Band nominatorBand = sourceProduct.getBand(sourceBandNames[0]);
            final Band denominatorBand = sourceProduct.getBand(sourceBandNames[1]);
            final Tile nominatorTile = getSourceTile(nominatorBand, targetRectangle);
            final Tile denominatorTile = getSourceTile(denominatorBand, targetRectangle);
            final ProductData nominatorData = nominatorTile.getDataBuffer();
            final ProductData denominatorData = denominatorTile.getDataBuffer();
            final double noDataValueN = nominatorBand.getNoDataValue();
            final double noDataValueD = denominatorBand.getNoDataValue();

            final Band targetRatioBand = targetProduct.getBand(ratioBandName);
            final Tile targetRatioTile = targetTiles.get(targetRatioBand);
            final ProductData ratioData = targetRatioTile.getDataBuffer();

            final TileIndex trgIndex = new TileIndex(targetTiles.get(targetTiles.keySet().iterator().next()));
            final TileIndex srcIndex = new TileIndex(nominatorTile);

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            double vRatio;
            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {
                    final int trgIdx = trgIndex.getIndex(tx);
                    final int srcIdx = srcIndex.getIndex(tx);

                    final double vN = nominatorData.getElemDoubleAt(srcIdx);
                    final double vD = denominatorData.getElemDoubleAt(srcIdx);
                    if (vN == noDataValueN || vD == noDataValueD || vN <= 0.0 || vD <= 0.0) {
                        ratioData.setElemFloatAt(trgIdx, 0.0f);
                        continue;
                    }

                    vRatio = vN / vD;
                    if (outputLogRatio) {
                        vRatio = Math.log(Math.max(vRatio, Constants.EPS));
                    }

                    ratioData.setElemFloatAt(trgIdx, (float) vRatio);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }


    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ChangeDetectionOp.class);
        }
    }
}