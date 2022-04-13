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
package org.esa.s1tbx.fex.gpf.oceantools;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The oil spill detection operator.
 * <p/>
 * The algorithm for detecting dark spots is based on adaptive thresholding. The thresholding is based on
 * an estimate of the typical backscatter level in a large window, and the threshold is set to k decibel
 * below the estimated local mean backscatter level. Calibrated images are used, and simple speckle filtering
 * is applied prior to thresholding.
 * <p/>
 * [1] A. S. Solberg, C. Brekke and R. Solberg, "Algorithms for oil spill detection in Radarsat and ENVISAT
 * SAR images", Geoscience and Remote Sensing Symposium, 2004. IGARSS '04. Proceedings. 2004 IEEE International,
 * 20-24 Sept. 2004, page 4909-4912, vol.7.
 */

// Need to update the following graphs:
//    C:\ESA\snap-gpt-tests\gpt-tests-resources\graphs\s1tbx\FeatureExtraction
//    C:\ESA\s1tbx\s1tbx-op-feature-extraction-ui\src\main\resources\org\esa\s1tbx\fex\graphs\Radar\SAR Applications

@OperatorMetadata(alias = "Oil-Spill-Detection",
        category = "Radar/SAR Applications/Ocean Applications/Oil Spill Detection",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Detect oil spill.")
public class OilSpillDetectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Background window dimension (km)", defaultValue = "0.5", label = "Background Window Dimension (km)")
    private double backgroundWindowDim = 0.5;

    @Parameter(description = "Threshold shift from background mean", defaultValue = "2.0", label = "Threshold Shift (dB)")
    private double k = 2.0;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int backgroundWindowSize = 0;
    private int halfBackgroundWindowSize = 0;

    private double kInLinearScale = 0.0;

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<>();
    public final static String OILSPILLMASK_NAME = "_oil_spill_bit_msk";

    @Override
    public void initialize() throws OperatorException {
        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCalibrated(true);
            validator.checkIfTOPSARBurstProduct(false);

            getMission();

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            computeBackgroundWindowSize();

            if (k < 0) {
                throw new OperatorException("Threshold Shift cannot be negative");
            } else {
                kInLinearScale = FastMath.pow(10.0, k / 10.0);
            }

            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceImageWidth,
                    sourceImageHeight);

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            addBitmasks(targetProduct);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static void addBitmasks(final Product product) {
        for (Band band : product.getBands()) {
            if (band.getName().contains(OILSPILLMASK_NAME)) {
                final String expression = band.getName() + " > 0";
                //  final BitmaskDef mask = new BitmaskDef(band.getName()+"_detection",
                //      "Oil Spill Detection", expression, Color.RED, 0.5f);
                //  product.addBitmaskDef(mask);

                final Mask mask = new Mask(band.getName() + "_detection",
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

    private void computeBackgroundWindowSize() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if(absRoot == null) {
            throw new OperatorException("AbstractMetadata is null");
        }

        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 1);
        final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);
        final double minSpacing = Math.min(rangeSpacing, azimuthSpacing);
        backgroundWindowSize = (int)(backgroundWindowDim * 1000 / minSpacing);
        halfBackgroundWindowSize = backgroundWindowSize / 2;
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
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            final String srcBandNames = srcBand.getName();
            final String targetBandName = srcBandNames + OILSPILLMASK_NAME;
            targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

            final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);

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
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw = targetTileRectangle.width;
            final int th = targetTileRectangle.height;
            final ProductData trgData = targetTile.getDataBuffer();
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final int x0 = Math.max(tx0 - halfBackgroundWindowSize, 0);
            final int y0 = Math.max(ty0 - halfBackgroundWindowSize, 0);
            final int w = Math.min(tx0 + tw - 1 + halfBackgroundWindowSize, sourceImageWidth - 1) - x0 + 1;
            final int h = Math.min(ty0 + th - 1 + halfBackgroundWindowSize, sourceImageHeight - 1) - y0 + 1;
            final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcBuffer = sourceTile.getDataBuffer();
            final Double noDataValue = sourceBand.getNoDataValue();

            if(srcBuffer.getType() != ProductData.TYPE_FLOAT32) {
                throw new OperatorException("Oil spill inputs should be calibrated");
            }
            float[] data = (float[])srcBuffer.getElems();
            int dminX = tx0-x0, dminY = ty0-y0;
            int dmaxX = w, dmaxY = h;

            final TileIndex trgIndex = new TileIndex(targetTile);

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            double previousMean;
            List<Double> colSumList = new ArrayList<>(w);
            List<Integer> colNumPixelList = new ArrayList<>(w);

            for (int ty = ty0, y = dminY; ty < maxy && y < dmaxY; ty++, y++) {
                int ds = y * w;
                trgIndex.calculateStride(ty);
                previousMean = -1.0;

                for (int tx = tx0, x = dminX; tx < maxx && x < dmaxX; tx++, x++) {
                    double v = data[x + ds];
                    if (noDataValue.equals(v)) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                        continue;
                    }

                    final double backgroundMean = computeBackgroundMean(data, x, y, dmaxX, dmaxY, w, noDataValue,
                            previousMean, colSumList, colNumPixelList);

                    if (backgroundMean != noDataValue && v < backgroundMean / kInLinearScale) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 1);
                    } else {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                    }
                    previousMean = backgroundMean;
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute the mean value for pixels in a given sliding window.
     *
     * @param x          The x coordinate of the central point of the sliding window.
     * @param y          The y coordinate of the central point of the sliding window.
     * @param maxX       max x of data
     * @param maxY       max y of data
     * @param width      width of data rect
     * @param noDataValue the place holder for no data
     * @param previousMean Mean value computed for previous window
     * @param colSumList Column sum of pixels in the previous window
     * @param colNumPixelList The number of valid pixels for each column in the previous window
     * @return The mean value
     */
    private double computeBackgroundMean(float[] data, int x, int y, int maxX, int maxY, int width,
                                         final double noDataValue, final double previousMean,
                                         List<Double> colSumList, List<Integer> colNumPixelList) {

        final int x0 = Math.max(x - halfBackgroundWindowSize, 0);
        final int y0 = Math.max(y - halfBackgroundWindowSize, 0);
        final int xMax = Math.min(x + halfBackgroundWindowSize, maxX-1);
        final int yMax = Math.min(y + halfBackgroundWindowSize, maxY-1);

        if (previousMean != -1.0) {
            double colSum = 0.0;
            int colNumPixels = 0;
            for (int yy = y0; yy < yMax; yy++) {
                double v = data[yy * width + xMax - 1];
                if (v != noDataValue) {
                    colSum += v;
                    colNumPixels++;
                }
            }

            int previousValidPixels = colNumPixelList.stream().reduce(0, Integer::sum);
            final double previousSum = previousMean * previousValidPixels;
            final double mean = (previousSum - colSumList.get(0) + colSum) /
                    (previousValidPixels - colNumPixelList.get(0) + colNumPixels);
            colSumList.remove(0);
            colSumList.add(colSum);
            colNumPixelList.remove(0);
            colNumPixelList.add(colNumPixels);
            return mean;
        }

        colSumList.clear();
        colNumPixelList.clear();
        double totalSum = 0.0;
        int totalNumPixels = 0;
        for (int xx = x0; xx < xMax; xx++) {
            double colSum = 0.0;
            int colNumPixels = 0;
            for (int yy = y0; yy < yMax; yy++) {
                double v = data[yy * width + xx];
                if (v != noDataValue) {
                    colSum += v;
                    colNumPixels++;
                }
            }
            colSumList.add(colSum);
            colNumPixelList.add(colNumPixels);
            totalSum += colSum;
            totalNumPixels += colNumPixels;
        }

        if (totalNumPixels > 0) {
            return totalSum / totalNumPixels;
        } else {
            return noDataValue;
        }
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
