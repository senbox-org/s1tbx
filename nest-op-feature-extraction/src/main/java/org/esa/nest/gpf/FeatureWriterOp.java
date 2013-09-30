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
import com.bc.ceres.glevel.MultiLevelImage;
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
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.datamodel.AbstractMetadata;

import java.awt.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Output features into patches
 */
@OperatorMetadata(alias = "FeatureWriter",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Writes a features into patches.",
        category = "Feature Extraction")
public class FeatureWriterOp extends Operator implements Output {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output folder to which the data is written.", label="Output Folder")
    private File outputFolder;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
               description = "The name of the output file format.")
    private String formatName;

    @Parameter(description = "Patch size in km", interval = "(0, *)", defaultValue = "12.0", label="Patch Size (km)")
    private double patchSizeKm = 12.0;

    private MetadataElement absRoot = null;
    private final Map<MultiLevelImage, List<Point>> todoLists = new HashMap<MultiLevelImage, List<Point>>();
    private final HashMap<String, File> bandNameToFeatureDir = new HashMap<String, File>();
    private String[] sourceBandNames;

    private int patchWidth = 0;
    private int patchHeight = 0;

    public static final String featureBandName = "_speckle_divergence";

    public FeatureWriterOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            if(outputFolder == null || !outputFolder.isAbsolute()) {
                throw new OperatorException("Please specify an output folder");
            }
            if(!outputFolder.exists())
                outputFolder.mkdirs();

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            targetProduct = sourceProduct;

            computePatchDimension();

            targetProduct.setPreferredTileSize(patchWidth, patchHeight);

            List<String> nameList = new ArrayList<String>();
            for(Band b : sourceProduct.getBands()) {
                if(!b.getName().contains(featureBandName)) {
                    nameList.add(b.getName());
                }
            }
            sourceBandNames = nameList.toArray(new String[nameList.size()]);

            createFeatureOutputDirectory();

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    /**
     * Compute patch dimension for given patch size in kilometer.
     */
    private void computePatchDimension() {

        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        patchWidth = (int)(patchSizeKm*1000.0/rangeSpacing);
        patchHeight = (int)(patchSizeKm*1000.0/azimuthSpacing);

        //if (patchWidth < windowSize && patchHeight < windowSize) {
       //    throw new OperatorException("The Patch Size (km) is too small: " + patchSizeKm);
        //}
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw  = targetTileRectangle.width;
            final int th  = targetTileRectangle.height;

            final int tileX = tx0/tw;
            final int tileY = ty0/th;
            String srcBandName = targetBand.getName();

            if(srcBandName.contains(featureBandName)) {
                srcBandName = srcBandName.substring(0, srcBandName.indexOf(featureBandName));
                final File tileDir = createTileFeatureDirectory(tileX, tileY, srcBandName);

                outputStatistics(tx0, ty0, tw, th, tileX, tileY, targetBand, targetTile, tileDir);
            } else {
                final File tileDir = createTileFeatureDirectory(tileX, tileY, srcBandName);

                outputPatchImage(tx0, ty0, tw, th, srcBandName, targetBand, tileDir);
            }

        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    /**
     * Create a feature directory for feature output.
     * @throws IOException The exception.
     */
    private void createFeatureOutputDirectory() throws IOException {

        final File outputDir = new File(outputFolder, sourceProduct.getName());
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
    private static void makeDir(final File dir) throws IOException {

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
     * Create directory for current tile feature output.
     * @param tileX Tile index in X direction.
     * @param tileY Tile index in Y direction.
     * @param srcBandName Source band name.
     * @return The directory.
     * @throws IOException The exceptions.
     */
    private synchronized File createTileFeatureDirectory(final int tileX, final int tileY, final String srcBandName)
            throws IOException {

        final File featureDir = bandNameToFeatureDir.get(srcBandName);
        final String tileDirName = String.format("x%02dy%02d", tileX, tileY);
        final File tileDir = new File(featureDir, tileDirName);
        if(!tileDir.exists()) {
            if (!tileDir.mkdir()) {
                throw new IOException(
                    MessageFormat.format("Tile directory ''{0}'' cannot be created.", tileDir));
            }
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
     * @param targetBand The target band name.
     * @param targetTile the tile
     * @param tileDir The tile directory for output.
     * @throws IOException The exceptions.
     */
    private void outputStatistics(final int tx0, final int ty0, final int tw, final int th, final int tileX,
                                         final int tileY, final Band targetBand, final Tile targetTile,
                                         final File tileDir) throws IOException {

        final String tgtBandName = targetBand.getName();
        final StxFactory stxFactory = new StxFactory();
        Band tmpBand = new Band(tgtBandName, ProductData.TYPE_FLOAT64, tw, th);
        Tile srcTile = getSourceTile(targetBand, new Rectangle(tx0, ty0, tw, th));

        try {
        final double[] dataArray = new double[tw*th];
        int cnt = 0;
        final TileIndex srcIndex = new TileIndex(targetTile);
        for (int ty = ty0; ty < ty0 + th; ty++) {
            srcIndex.calculateStride(ty);
            for (int tx = tx0; tx < tx0 + tw; tx++) {
                final double v = srcTile.getDataBuffer().getElemDoubleAt(srcIndex.getIndex(tx));
                dataArray[cnt++] = v;
            }
        }
        tmpBand.setData(ProductData.createInstance(dataArray));
        } catch(Exception e) {
            e.printStackTrace();
        }

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
                                  final Band targetBand, final File tileDir) {

        try {
            final String tgtBandName = targetBand.getName();
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
            subsetInfo.file = new File(tileDir, srcBandName+".dim");

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
            //throw new OperatorException(t);
            t.printStackTrace();
        }
    }

    private static class SubsetInfo {
        Product product;
        ProductSubsetBuilder subsetBuilder;
        File file;
        ProductWriter productWriter;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FeatureWriterOp.class);
        }
    }

}