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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The urban area detection operator.
 *
 * The operator implements the algorithm given in [1].
 *
 * [1] T. Esch, M. Thiel, A. Schenk, A. Roth, A. Müller, and S. Dech,
 *     "Delineation of Urban Footprints From TerraSAR-X Data by Analyzing
 *     Speckle Characteristics and Intensity Information," IEEE Transactions
 *     on Geoscience and Remote Sensing, vol. 48, no. 2, pp. 905-916, 2010.
 */

@OperatorMetadata(alias = "Urban-Area-Detection",
        category = "Feature-Extraction-Tools",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Detect urban area.")
public class UrbanAreaDetectionOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11, WINDOW_SIZE_13x13,
            WINDOW_SIZE_15x15, WINDOW_SIZE_17x17}, defaultValue = WINDOW_SIZE_15x15, label="Window Size")
    private String windowSizeStr = WINDOW_SIZE_15x15;

    @Parameter(description = "Patch size in km", interval = "(0, *)", defaultValue = "12.0", label="Patch Size (km)")
    private double patchSizeKm = 12.0;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
               description = "The name of the output patch format.")
    private String formatName;

    @Parameter(description = "Flag for output features", defaultValue = "true", label="Output Features")
    private boolean outputFeatures = true;

    private MetadataElement absRoot = null;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int windowSize = 0;
    private int halfWindowSize = 0;
    private int patchWidth = 0;
    private int patchHeight = 0;

    private double c = 0.0; // theoretical coefficient of variance
    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<String, String>();
    private final HashMap<String, File> bandNameToFeatureDir = new HashMap<String, File>();

    public static final String SPECKLE_DIVERGENCE_MASK_NAME = "_speckle_divergence";

    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    private static final String WINDOW_SIZE_11x11 = "11x11";
    private static final String WINDOW_SIZE_13x13 = "13x13";
    private static final String WINDOW_SIZE_15x15 = "15x15";
    private static final String WINDOW_SIZE_17x17 = "17x17";

    @Override
    public void initialize() throws OperatorException {

        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            setWindowSize();

            computePatchDimension();

            computeTheoreticalCoefficientOfVariance();

            createTargetProduct();

            if (outputFeatures) {
                createFeatureOutputDirectory();
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Set Window size.
     */
    private void setWindowSize() {

        if (windowSizeStr.equals(WINDOW_SIZE_5x5)) {
            windowSize = 5;
        } else if (windowSizeStr.equals(WINDOW_SIZE_7x7)) {
            windowSize = 7;
        } else if (windowSizeStr.equals(WINDOW_SIZE_9x9)) {
            windowSize = 9;
        } else if (windowSizeStr.equals(WINDOW_SIZE_11x11)) {
            windowSize = 11;
        } else if (windowSizeStr.equals(WINDOW_SIZE_13x13)) {
            windowSize = 13;
        } else if (windowSizeStr.equals(WINDOW_SIZE_15x15)) {
            windowSize = 15;
        } else if (windowSizeStr.equals(WINDOW_SIZE_17x17)) {
            windowSize = 17;
        } else {
            throw new OperatorException("Unknown window size: " + windowSize);
        }

        halfWindowSize = windowSize/2;
    }

    /**
     * Compute patch dimension for given patch size in kilometer.
     */
    private void computePatchDimension() {

        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        patchWidth = (int)(patchSizeKm*1000.0/rangeSpacing);
        patchHeight = (int)(patchSizeKm*1000.0/azimuthSpacing);

        if (patchWidth < windowSize && patchHeight < windowSize) {
            throw new OperatorException("The Patch Size (km) is too small: " + patchSizeKm);
        }
    }

    /**
     * Compute theoretical coefficient of variance.
     */
    private void computeTheoreticalCoefficientOfVariance() {

        final int azimuthLooks = absRoot.getAttributeInt(AbstractMetadata.azimuth_looks);
        final int rangeLooks = absRoot.getAttributeInt(AbstractMetadata.range_looks);
        c = 1.0 / (azimuthLooks + rangeLooks);
    }


    /**
     * Create target product.
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        targetProduct.setPreferredTileSize(patchWidth, patchHeight);
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
            final String srcBandNames = srcBand.getName();
            final String unit = srcBand.getUnit();
            if(unit == null) {
                throw new OperatorException("band " + srcBandNames + " requires a unit");
            }

            if (unit.contains(Unit.IMAGINARY) || unit.contains(Unit.REAL) || unit.contains(Unit.PHASE)) {
                throw new OperatorException("Please select amplitude or intensity band");
            }

            final String targetBandName = srcBandNames + SPECKLE_DIVERGENCE_MASK_NAME;
            targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

            final Band targetBand = ProductUtils.copyBand(srcBandNames, sourceProduct, targetProduct, false);
            targetBand.setSourceImage(srcBand.getSourceImage());

            final Band targetBandMask = new Band(targetBandName,
                                                 ProductData.TYPE_FLOAT32,
                                                 sourceImageWidth,
                                                 sourceImageHeight);

            targetBandMask.setNoDataValue(srcBand.getNoDataValue());
            targetBandMask.setNoDataValueUsed(true);
            targetBandMask.setUnit("speckle_divergence");
            targetProduct.addBand(targetBandMask);
        }
    }

    /**
     * Create a feature directory for feature output.
     * @throws IOException The exception.
     */
    private void createFeatureOutputDirectory() throws IOException {

        final File parentDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        final File outputDir = new File(parentDir, sourceProduct.getName());
        makeDir(outputDir);

        for (String bandName:sourceBandNames) {
            final File featureDir = new File(outputDir, bandName);
            makeDir(featureDir);
            bandNameToFeatureDir.put(bandName, featureDir);
        }
    }

    /**
     * Create a directory.
     * @param dir The directory.
     * @throws IOException The exceptions.
     */
    private void makeDir(final File dir) throws IOException {

        if (dir == null) {
            throw new IOException(
                    MessageFormat.format("Invalid directory ''{0}''.", dir));
        }

        if (dir.exists()) {
            if (!FileUtils.deleteTree(dir)) {
                throw new IOException(
                        MessageFormat.format("Existing directory ''{0}'' cannot be deleted.", dir));
            }
        }

        if (!dir.mkdir()) {
            throw new IOException(MessageFormat.format("Directory ''{0}'' cannot be created.", dir));
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

            final Rectangle sourceTileRectangle = getSourceTileRectangle(tx0, ty0, tw, th);
            final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcData = sourceTile.getDataBuffer();
            final double noDataValue = sourceBand.getNoDataValue();
            final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand);

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceTile);    // src and trg tile are different size

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            int sampleCount = 0;
            final double[] speckleDivergenceArray = new double[tw*th];

            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {
                    final double v = srcData.getElemDoubleAt(srcIndex.getIndex(tx));
                    if (v == noDataValue) {
                        trgData.setElemFloatAt(trgIndex.getIndex(tx), (float)noDataValue);
                        continue;
                    }

                    final double cv = computeCoefficientOfVariance(tx, ty, sourceTile, srcData, bandUnit, noDataValue);
                    final double speckleDivergence = cv - c;
                    trgData.setElemFloatAt(trgIndex.getIndex(tx), (float)speckleDivergence);
                    speckleDivergenceArray[sampleCount++] = speckleDivergence;
                }
            }

            if (outputFeatures) {
                outputFeatures(tx0, ty0, tw, th, srcBandName, targetBand.getName(), speckleDivergenceArray);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source tile rectangle.
     * @param x0 X coordinate of pixel at the upper left corner of the target tile.
     * @param y0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param w The width of the target tile.
     * @param h The height of the target tile.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfWindowSize) {
            sx0 -= halfWindowSize;
            sw += halfWindowSize;
        }

        if (y0 >= halfWindowSize) {
            sy0 -= halfWindowSize;
            sh += halfWindowSize;
        }

        if (x0 + w + halfWindowSize <= sourceImageWidth) {
            sw += halfWindowSize;
        }

        if (y0 + h + halfWindowSize <= sourceImageHeight) {
            sh += halfWindowSize;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Compute local coefficient of variance.
     * @param tx The x coordinate of the central pixel of the sliding window.
     * @param ty The y coordinate of the central pixel of the sliding window.
     * @param sourceTile The source image tile.
     * @param srcData The source image data.
     * @param bandUnit The source band unit.
     * @param noDataValue the place holder for no data
     * @return The local coefficient of variance.
     */
    private double computeCoefficientOfVariance(final int tx, final int ty,
                                                final Tile sourceTile, final ProductData srcData,
                                                final Unit.UnitType bandUnit, final double noDataValue) {

        final double[] samples = new double[windowSize*windowSize];

        final int numSamples = getSamples(tx, ty, bandUnit, noDataValue, sourceTile, srcData, samples);

        if (numSamples == 0) {
            return noDataValue;
        }

        final double mean = getMeanValue(samples, numSamples);

        final double variance = getVarianceValue(samples, numSamples, mean);

        return Math.sqrt(variance)/mean;
    }

    /**
     * Get source samples in the sliding window.
     * @param tx The x coordinate of the central pixel of the sliding window.
     * @param ty The y coordinate of the central pixel of the sliding window.
     * @param bandUnit The source band unit.
     * @param noDataValue the place holder for no data
     * @param sourceTile The source image tile.
     * @param srcData The source image data.
     * @param samples The sample array.
     * @return The number of samples.
     */
    private int getSamples(final int tx, final int ty, final Unit.UnitType bandUnit, final double noDataValue,
                           final Tile sourceTile, final ProductData srcData, final double[] samples) {


        final int x0 = Math.max(tx - halfWindowSize, 0);
        final int y0 = Math.max(ty - halfWindowSize, 0);
        final int w  = Math.min(tx + halfWindowSize, sourceImageWidth - 1) - x0 + 1;
        final int h  = Math.min(ty + halfWindowSize, sourceImageHeight - 1) - y0 + 1;

        final TileIndex tileIndex = new TileIndex(sourceTile);

        int numSamples = 0;
        final int maxy = y0 + h;
        final int maxx = x0 + w;

        if (bandUnit == Unit.UnitType.INTENSITY) {

            for (int y = y0; y < maxy; y++) {
                tileIndex.calculateStride(y);
                for (int x = x0; x < maxx; x++) {
                    final double v = srcData.getElemDoubleAt(tileIndex.getIndex(x));
                    if (v != noDataValue) {
                        samples[numSamples++] = v;
                    }
                }
            }

        } else {

            for (int y = y0; y < maxy; y++) {
                tileIndex.calculateStride(y);
                for (int x = x0; x < maxx; x++) {
                    final double v = srcData.getElemDoubleAt(tileIndex.getIndex(x));
                    if (v != noDataValue) {
                        samples[numSamples++] = v*v;
                    }
                }
            }
        }

        return numSamples;
    }

    /**
     * Get the mean value of the samples.
     * @param samples The sample array.
     * @param numSamples The number of samples.
     * @return mean The mean value.
     */
    private static double getMeanValue(final double[] samples, final int numSamples) {

        double mean = 0.0;
        for (int i = 0; i < numSamples; i++) {
            mean += samples[i];
        }
        mean /= numSamples;

        return mean;
    }

    /**
     * Get the variance of samples.
     * @param samples The sample array.
     * @param numSamples The number of samples.
     * @param mean the mean of samples.
     * @return var The variance.
     */
    private static double getVarianceValue(final double[] samples, final int numSamples, final double mean) {

        double var = 0.0;
        if (numSamples > 1) {
            for (int i = 0; i < numSamples; i++) {
                final double diff = samples[i] - mean;
                var += diff * diff;
            }
            var /= (numSamples - 1);
        }

        return var;
    }

    /**
     * Output features to file.
     * @param tx0 X coordinate of pixel at the upper left corner of the target tile.
     * @param ty0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param tw The width of the target tile.
     * @param th The height of the target tile.
     * @param srcBandName The source band name.
     * @param tgtBandName The target band name.
     * @param speckleDivergenceArray The data array.
     * @throws IOException The exceptions.
     */
    private synchronized void outputFeatures(final int tx0, final int ty0, final int tw, final int th,
                                               final String srcBandName, final String tgtBandName,
                                               final double[] speckleDivergenceArray) throws IOException {

        final int tileX = tx0/patchWidth;
        final int tileY = ty0/patchHeight;

        final File tileDir = createTileFeatureDirectory(tileX, tileY, srcBandName);

        outputStatistics(tx0, ty0, tw, th, tileX, tileY, tgtBandName, tileDir, speckleDivergenceArray);

        //outputPatchImage(tx0, ty0, tw, th, srcBandName, tgtBandName, tileDir);
    }

    /**
     * Create directory for current tile feature output.
     * @param tileX Tile index in X direction.
     * @param tileY Tile index in Y direction.
     * @param srcBandName Source band name.
     * @return The directory.
     * @throws IOException The exceptions.
     */
    private File createTileFeatureDirectory(final int tileX, final int tileY, final String srcBandName)
            throws IOException {

        final File featureDir = bandNameToFeatureDir.get(srcBandName);
        final String tileDirName = String.format("x%02dy%02d", tileX, tileY);
        final File tileDir = new File(featureDir, tileDirName);
        if (!tileDir.mkdir()) {
            throw new IOException(
                    MessageFormat.format("Tile directory ''{0}'' cannot be created.", tileDir));
        }

        return tileDir;
    }

    /**
     * Output statistics to file.
     * @param tx0 X coordinate of pixel at the upper left corner of the target tile.
     * @param ty0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param tw The width of the target tile.
     * @param th The height of the target tile.
     * @param tileX Tile index in X direction.
     * @param tileY Tile index in Y direction.
     * @param tgtBandName The target band name.
     * @param tileDir The tile directory for output.
     * @param speckleDivergenceArray The data array.
     * @throws IOException The exceptions.
     */
    private void outputStatistics(final int tx0, final int ty0, final int tw, final int th, final int tileX,
                                  final int tileY, final String tgtBandName, final File tileDir,
                                  final double[] speckleDivergenceArray) throws IOException {

        final StxFactory stxFactory = new StxFactory();
        Band tmpBand = new Band(tgtBandName, ProductData.TYPE_FLOAT64, tw, th);
        tmpBand.setData(ProductData.createInstance(speckleDivergenceArray));
        final Stx stx = stxFactory.create(tmpBand, ProgressMonitor.NULL);

        final File featureFile = new File(tileDir, "features.txt");

        final Writer featureWriter = new BufferedWriter(new FileWriter(featureFile));

        try {
            featureWriter.write(String.format("tileX = %s, tileY = %s, x0 = %s, y0 = %s, width = %s, height = %s\n\n",
                    tileX, tileY, tx0, ty0, tw, th));
            featureWriter.write(String.format("%s.minimum = %s\n", tgtBandName, stx.getMinimum()));
            featureWriter.write(String.format("%s.maximum = %s\n", tgtBandName, stx.getMaximum()));
            featureWriter.write(String.format("%s.median  = %s\n", tgtBandName, stx.getMedian()));
            featureWriter.write(String.format("%s.mean    = %s\n", tgtBandName, stx.getMean()));
            featureWriter.write(String.format("%s.stdev   = %s\n", tgtBandName, stx.getStandardDeviation()));
            featureWriter.write(String.format("%s.coefVar = %s\n", tgtBandName, stx.getCoefficientOfVariation()));
            featureWriter.write(String.format("%s.count   = %s\n", tgtBandName, stx.getSampleCount()));

        } finally {
            try {
                featureWriter.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void outputPatchImage(final int tx0, final int ty0, final int tw, final int th, final String srcBandName,
                                  final String tgtBandName, final File tileDir) {

        try {
            // create subset
            final ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.addNodeNames(targetProduct.getTiePointGridNames());
            subsetDef.addNodeNames(targetProduct.getBandNames());
            subsetDef.setRegion(tx0, ty0, tw, th);
            subsetDef.setSubSampling(1, 1);
            subsetDef.setIgnoreMetadata(false);

            // create subsetInfo
            SubsetInfo subsetInfo = new SubsetInfo();
            subsetInfo.subsetBuilder = new ProductSubsetBuilder();
            subsetInfo.product = subsetInfo.subsetBuilder.readProductNodes(targetProduct, subsetDef);
            subsetInfo.file = new File(tileDir, "patch.dim");

            subsetInfo.productWriter = ProductIO.getProductWriter(formatName); // BEAM-DIMAP
            if (subsetInfo.productWriter == null) {
                throw new OperatorException("No data product writer for the '" + formatName + "' format available");
            }
            subsetInfo.productWriter.setIncrementalMode(false);
            subsetInfo.productWriter.setFormatName(formatName);
            subsetInfo.product.setProductWriter(subsetInfo.productWriter);

            // output metadata
            subsetInfo.productWriter.writeProductNodes(subsetInfo.product, subsetInfo.file);

            // output original image
            final Rectangle trgRect = new Rectangle(tx0,ty0, tw, th);
            final Tile srcImageTile = getSourceTile(targetProduct.getBand(srcBandName), trgRect);
            final ProductData srcImageData = srcImageTile.getRawSamples();
            final Band srcImage = subsetInfo.product.getBand(srcBandName);
            subsetInfo.productWriter.writeBandRasterData(srcImage, 0, 0,
                    srcImage.getSceneRasterWidth(), srcImage.getSceneRasterHeight(), srcImageData, ProgressMonitor.NULL);

            // output speckle divergence image
            final Tile spkDivTile = getSourceTile(targetProduct.getBand(tgtBandName), trgRect);
            final ProductData spkDivData = spkDivTile.getRawSamples();
            final Band spkDiv = subsetInfo.product.getBand(tgtBandName);
            subsetInfo.productWriter.writeBandRasterData(spkDiv, 0, 0,
                    spkDiv.getSceneRasterWidth(), spkDiv.getSceneRasterHeight(), spkDivData, ProgressMonitor.NULL);

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private static class SubsetInfo {
        Product product;
        ProductSubsetBuilder subsetBuilder;
        File file;
        ProductWriter productWriter;
    }


    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(UrbanAreaDetectionOp.class);
        }
    }
}