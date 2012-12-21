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
package org.esa.nest.gpf.oceantools;

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
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The oil spill detection operator.
 *
 * The algorithm for detecting dark spots is based on adaptive thresholding. The thresholding is based on
 * an estimate of the typical backscatter level in a large window, and the threshold is set to k decibel
 * below the estimated local mean backscatter level. Calibrated images are used, and simple speckle filtering
 * is applied prior to thresholding.
 *
 * [1] A. S. Solberg, C. Brekke and R. Solberg, "Algorithms for oil spill detection in Radarsat and ENVISAT
 * SAR images", Geoscience and Remote Sensing Symposium, 2004. IGARSS '04. Proceedings. 2004 IEEE International,
 * 20-24 Sept. 2004, page 4909-4912, vol.7.
 */

@OperatorMetadata(alias = "Oil-Spill-Detection",
        category = "Ocean-Tools",
        description = "Detect oil spill.")
public class OilSpillDetectionOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Background window size", defaultValue = "13", label="Background Window Size")
    private int backgroundWindowSize = 61;

    @Parameter(description = "Threshold shift from background mean", defaultValue = "2.0", label="Threshold Shift (dB)")
    private double k = 2.0;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int halfBackgroundWindowSize = 0;

    private double kInLinearScale = 0.0;

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<String, String>();
    public final static String OILSPILLMASK_NAME = "_oil_spill_bit_msk";

    @Override
    public void initialize() throws OperatorException {
        try {

            getMission();

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();
            halfBackgroundWindowSize = (backgroundWindowSize - 1) / 2;

            if (k < 0) {
                throw new OperatorException("Threshold Shift cannot be negative");
            } else {
                kInLinearScale = Math.pow(10.0, k/10.0);
            }

            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceImageWidth,
                                        sourceImageHeight);

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            addBitmasks(targetProduct);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static void addBitmasks(final Product product) {
        for(Band band : product.getBands()) {
            if(band.getName().contains(OILSPILLMASK_NAME)) {
                final String expression = band.getName() + " > 0";
              //  final BitmaskDef mask = new BitmaskDef(band.getName()+"_detection",
              //      "Oil Spill Detection", expression, Color.RED, 0.5f);
              //  product.addBitmaskDef(mask);

                final Mask mask = new Mask(band.getName()+"_detection",
                             product.getSceneRasterWidth(),
                             product.getSceneRasterHeight(),
                             Mask.BandMathsType.INSTANCE);
                mask.setDescription("Oil Spill Detection");
                mask.getImageConfig().setValue("color", Color.RED);
                mask.getImageConfig().setValue("transparency", 0.5);
                mask.getImageConfig().setValue("expression", expression);
                mask.setNoDataValue(0);
                mask.setNoDataValueUsed(true);
                product.getMaskGroup().add(mask);
            }
        }
    }

    /**
     * Get mission from the metadata of the product.
     */
    private void getMission() {

        /*final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if(absRoot == null) {
            throw new OperatorException("AbstractMetadata is null");
        }
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        if(mission.equals("ERS1") || mission.equals("ERS2")) {
            pyramidLevel = 1;
        } else if(mission.equals("ENVISAT")) {
            pyramidLevel = 2;
        } else if(mission.equals("RS1") || mission.equals("RS2")) {
            pyramidLevel = 3;
        }  */
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) { // if user did not select any band
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if(band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY))
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
            final String unit = srcBand.getUnit();
            if(unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            final String srcBandNames = srcBand.getName();
            final String targetBandName = srcBandNames + OILSPILLMASK_NAME;
            targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

            final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct);
            targetBand.setSourceImage(srcBand.getSourceImage());

            final Band targetBandMask = new Band(targetBandName,
                    ProductData.TYPE_INT8,
                    sourceImageWidth,
                    sourceImageHeight);
            targetBandMask.setNoDataValue(0);
            targetBandMask.setNoDataValueUsed(true);
            targetBandMask.setUnit(Unit.AMPLITUDE);
            targetProduct.addBand(targetBandMask);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw  = targetTileRectangle.width;
            final int th  = targetTileRectangle.height;
            final ProductData trgData = targetTile.getDataBuffer();
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final int x0 = Math.max(tx0 - halfBackgroundWindowSize, 0);
            final int y0 = Math.max(ty0 - halfBackgroundWindowSize, 0);
            final int w  = Math.min(tx0 + tw - 1 + halfBackgroundWindowSize, sourceImageWidth - 1) - x0 + 1;
            final int h  = Math.min(ty0 + th - 1 + halfBackgroundWindowSize, sourceImageHeight - 1) - y0 + 1;
            final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcBuffer = sourceTile.getDataBuffer();
            final double noDataValue = sourceBand.getNoDataValue();

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceTile);    // src and trg tile are different size

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {
                    final double v = srcBuffer.getElemDoubleAt(srcIndex.getIndex(tx));
                    if (v == noDataValue) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                        continue;
                    }

                    final double backgroundMean = computeBackgroundMean(tx, ty, sourceTile, noDataValue);
                    final double threshold = backgroundMean / kInLinearScale;
                    if (v < threshold) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 1);
                    } else {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute the mean value for pixels in a given sliding window.
     * @param tx The x coordinate of the central point of the sliding window.
     * @param ty The y coordinate of the central point of the sliding window.
     * @param sourceTile The source image tile.
     * @param noDataValue the place holder for no data
     * @return The mena value.
     */
    private double computeBackgroundMean(final int tx, final int ty, final Tile sourceTile, final double noDataValue) {

        final int x0 = Math.max(tx - halfBackgroundWindowSize, 0);
        final int y0 = Math.max(ty - halfBackgroundWindowSize, 0);
        final int w  = Math.min(tx + halfBackgroundWindowSize, sourceImageWidth - 1) - x0 + 1;
        final int h  = Math.min(ty + halfBackgroundWindowSize, sourceImageHeight - 1) - y0 + 1;
        final ProductData srcData = sourceTile.getDataBuffer();
        final TileIndex tileIndex = new TileIndex(sourceTile);

        double mean = 0.0;
        int numPixels = 0;
        final int maxy = y0 + h;
        final int maxx = x0 + w;
        for (int y = y0; y < maxy; y++) {
            tileIndex.calculateStride(y);
            for (int x = x0; x < maxx; x++) {
                final double v = srcData.getElemDoubleAt(tileIndex.getIndex(x));
                if (v != noDataValue) {
                    mean += v;
                    numPixels++;
                }
            }
        }
        return mean/numPixels;
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OilSpillDetectionOp.class);
        }
    }
}