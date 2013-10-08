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
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.dataio.kml.KmlWriter;
import org.esa.nest.datamodel.AbstractMetadata;

import javax.media.jai.operator.FileStoreDescriptor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

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

    @Parameter(description = "Minimum percentage of valid pixels", label="Minimum valid pixels (%)", defaultValue = "0.1")
    private float minValidPixels = 0.1f;

    private MetadataElement absRoot = null;
    private final HashMap<String, File> bandNameToFeatureDir = new HashMap<String, File>(5);
    private final List<TileInfo> tileInfoList = new ArrayList<TileInfo>(100);
    private String[] sourceBandNames;

    private KmlWriter kmlWriter = null;
    private File productOutputDir;
    private int patchWidth = 0;
    private int patchHeight = 0;
    private PrintWriter metadataWriter = null;
    private PrintWriter tileIndexWriter = null;
    private boolean folderStructureCreated = false;

    public static final String featureBandName = "_speckle_divergence";
    public static final String FEX_EXTENSION = ".fex";
    private static final String VERSION = "NEST 5.5 Urban Detection 0.1";

    private static final double Tdsl = 0.4; // threshold for detection adopted from Esch's paper.

    public FeatureWriterOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            if(outputFolder == null || !outputFolder.isAbsolute()) {
                throw new OperatorException("Please specify an output folder");
            }

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
    }

    @Override
    public synchronized void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            if(!folderStructureCreated) {
                createFeatureOutputDirectory();
            }
            String srcBandName = targetBand.getName();

            if(srcBandName.contains(featureBandName)) {
                final Rectangle targetTileRectangle = targetTile.getRectangle();
                final int tx0 = targetTileRectangle.x;
                final int ty0 = targetTileRectangle.y;
                final int tw  = targetTileRectangle.width;
                final int th  = targetTileRectangle.height;

                final int tileX = tx0/tw;
                final int tileY = ty0/th;

                //System.out.println(srcBandName+" tile "+ tileX +", "+tileY);

                srcBandName = srcBandName.substring(0, srcBandName.indexOf(featureBandName));
                final File tileDir = createTileFeatureDirectory(tileX, tileY, srcBandName);

                boolean valid = outputStatistics(tx0, ty0, tw, th, tileX, tileY, targetBand, targetTile, tileDir);
                if(!valid) {
                    FileUtils.deleteTree(tileDir);
                } else {
                    outputPatchImage(tx0, ty0, tw, th, srcBandName, targetBand, tileDir);
                }
            }

        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            if(!tileInfoList.isEmpty()) {
                finishMetadataFile();

                writeTileIndexFile();
            }
            if (kmlWriter != null) {
                kmlWriter.close();
            }
        } catch (Exception ignore) {
        }
        super.dispose();
    }

    private void writeMetadataFile(final File featureDir) throws Exception {
        final File metadataFile = new File(featureDir, "fex-metadata.txt");
        metadataWriter = new PrintWriter(metadataFile);
        metadataWriter.println("# Urban area SAR feature extraction");
        metadataWriter.println(String.format("version = %s", VERSION));
        metadataWriter.println(
                String.format("time = %s",
                        ProductData.UTC.create(new Date(System.currentTimeMillis()), 0).format().replace(" ", "T")));
        metadataWriter.println();
        metadataWriter.println("# Source product");
        metadataWriter.println("  source = "+sourceProduct.getName());
        metadataWriter.println();
        metadataWriter.println("# Extracted features:");
        metadataWriter.println("  featureCount = 8");
        metadataWriter.println("# Minimum speckle divergence");
        metadataWriter.println("  features.0 = speckle_divergence.minimum");
        metadataWriter.println("# Maximum speckle divergence");
        metadataWriter.println("  features.1 = speckle_divergence.maximum");
        metadataWriter.println("# Median divergence");
        metadataWriter.println("  features.2 = speckle_divergence.median");
        metadataWriter.println("# Mean speckle divergence");
        metadataWriter.println("  features.3 = speckle_divergence.mean");
        metadataWriter.println("# Speckle divergence standard diviation");
        metadataWriter.println("  features.4 = speckle_divergence.stdev");
        metadataWriter.println("# Speckle divergence coefficient of Variation");
        metadataWriter.println("  features.5 = speckle_divergence.coefVar");
        metadataWriter.println("# Count of valid pixels");
        metadataWriter.println("  features.6 = speckle_divergence.count");
        metadataWriter.println("# Sample percent over threshold of 0.4");
        metadataWriter.println("  features.7 = speckle_divergence.percentOverPnt4");
        metadataWriter.println();
    }

    private void finishMetadataFile() {
        metadataWriter.println("# Number of tiles generated");
        metadataWriter.println(String.format("  tileCount = %d", tileInfoList.size()));
        metadataWriter.println();
        metadataWriter.println("# List of tiles");
        for (int i = 0; i < tileInfoList.size(); i++) {
            final TileInfo tile = tileInfoList.get(i);
            metadataWriter.println(String.format("  tiles.%d.name = %s", i, tile.name));
            metadataWriter.println(String.format("  tiles.%d.tileX = %d", i, tile.tileX));
            metadataWriter.println(String.format("  tiles.%d.tileY = %d", i, tile.tileY));
            metadataWriter.println(String.format("  tiles.%d.x = %d", i, tile.x));
            metadataWriter.println(String.format("  tiles.%d.y = %d", i, tile.y));
            metadataWriter.println(String.format("  tiles.%d.width = %d", i, tile.width));
            metadataWriter.println(String.format("  tiles.%d.height = %d", i, tile.height));
            metadataWriter.println();
        }
        metadataWriter.close();
    }

    private void writeTileIndexFile() {

        for (int i = 0; i < tileInfoList.size(); i++) {
            final TileInfo tile = tileInfoList.get(i);

            final String tileInfo = String.format("%s%s%s %s %s %s %s %s %s",
                    productOutputDir.getName(), "/", tile.name,
                    tile.tileX,
                    tile.tileY,
                    tile.x,
                    tile.y,
                    tile.width,
                    tile.height);
            tileIndexWriter.println(tileInfo);
        }
        tileIndexWriter.close();
    }

    /**
     * Create a feature directory for feature output.
     * @throws Exception The exception.
     */
    private synchronized void createFeatureOutputDirectory() throws Exception {

        if(folderStructureCreated) return;

        if(!outputFolder.exists())
            outputFolder.mkdirs();
        productOutputDir = new File(outputFolder, sourceProduct.getName()+FEX_EXTENSION);
        makeDir(productOutputDir);

        writeMetadataFile(productOutputDir);

        // write tile index file
        final File tileIndexFile = new File(outputFolder, "fex-tiles.txt");
        tileIndexWriter = new PrintWriter(new BufferedWriter(new FileWriter(tileIndexFile, true)));

        if(sourceBandNames.length == 1) {
            bandNameToFeatureDir.put(sourceBandNames[0], productOutputDir);
        } else {
            for (String bandName:sourceBandNames) {
                final File featureDir = new File(productOutputDir, bandName);
                makeDir(featureDir);
                bandNameToFeatureDir.put(bandName, featureDir);
            }
        }

        final Writer writer = new FileWriter(new File(productOutputDir, "overview.kml"));
        kmlWriter = new KmlWriter(writer, sourceProduct.getName(),
                "Tiles of " + sourceProduct.getName());

        folderStructureCreated = true;
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

        if (!dir.mkdirs()) {
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
    private synchronized boolean outputStatistics(final int tx0, final int ty0, final int tw, final int th, final int tileX,
                                         final int tileY, final Band targetBand, final Tile targetTile,
                                         final File tileDir) throws IOException {

        final String tgtBandName = targetBand.getName();
        final Tile srcTile = getSourceTile(targetBand, new Rectangle(tx0, ty0, tw, th));

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
        final Band stxBand = new Band("Stx_"+tgtBandName, ProductData.TYPE_FLOAT64, tw, th);
        sourceProduct.addBand(stxBand);
        stxBand.setOwner(sourceProduct);
        stxBand.setData(ProductData.createInstance(dataArray));
        stxBand.setNoDataValue(0);
        stxBand.setNoDataValueUsed(true);

        final StxFactory stxFactory = new StxFactory();
        final Stx stx = stxFactory.create(stxBand, ProgressMonitor.NULL);

        final File featureFile = new File(tileDir, "features.txt");

        final Writer featureWriter = new BufferedWriter(new FileWriter(featureFile));

        try {
            featureWriter.write(String.format("%s.minimum = %s\n", tgtBandName, stx.getMinimum()));
            featureWriter.write(String.format("%s.maximum = %s\n", tgtBandName, stx.getMaximum()));
            featureWriter.write(String.format("%s.median  = %s\n", tgtBandName, stx.getMedian()));
            featureWriter.write(String.format("%s.mean    = %s\n", tgtBandName, stx.getMean()));
            featureWriter.write(String.format("%s.stdev   = %s\n", tgtBandName, stx.getStandardDeviation()));
            featureWriter.write(String.format("%s.coefVar = %s\n", tgtBandName, stx.getCoefficientOfVariation()));
            featureWriter.write(String.format("%s.count   = %s\n", tgtBandName, stx.getSampleCount()));
            featureWriter.write(String.format("%s.percentOverPnt4 = %s\n", tgtBandName, computeSamplePercentOverPnt4(dataArray)));

            sourceProduct.removeBand(stxBand);
        } finally {
            try {
                featureWriter.close();
            } catch (IOException ignored) {
            }
        }

        float pct = (stx.getSampleCount()/(float)(patchWidth*patchHeight));
        final boolean valid = pct > minValidPixels;
        if(valid) {
            tileInfoList.add(new TileInfo(tileDir.getName(), tileX, tileY, targetTile.getRectangle()));
        }
        return valid;
    }

    private static double computeSamplePercentOverPnt4(final double[] dataArray) {

        int numSamplesAboveThreshold = 0;
        for (double sd : dataArray) {
            if (sd > Tdsl) {
                numSamplesAboveThreshold++;
            }
        }

        return 100.0*numSamplesAboveThreshold/dataArray.length;
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

            final Band srcImage = subsetInfo.product.getBand(srcBandName);
            final Band spkDiv = subsetInfo.product.getBand(tgtBandName);

            if(true) {
                subsetInfo.productWriter = ProductIO.getProductWriter(formatName); // BEAM-DIMAP
                if (subsetInfo.productWriter == null) {
                    throw new OperatorException("No data product writer for the '" + formatName + "' format available");
                }
                subsetInfo.productWriter.setIncrementalMode(false);
                subsetInfo.productWriter.setFormatName(formatName);
                subsetInfo.product.setProductWriter(subsetInfo.productWriter);
                subsetInfo.file = new File(tileDir, "subset.dim");

                // output metadata
                subsetInfo.productWriter.writeProductNodes(subsetInfo.product, subsetInfo.file);

                // output original image
                final Rectangle trgRect = new Rectangle(tx0,ty0, tw, th);
                final Tile srcImageTile = getSourceTile(targetProduct.getBand(srcBandName), trgRect);
                final ProductData srcImageData = srcImageTile.getRawSamples();
                subsetInfo.productWriter.writeBandRasterData(srcImage, 0, 0,
                        srcImage.getSceneRasterWidth(), srcImage.getSceneRasterHeight(), srcImageData, ProgressMonitor.NULL);

                // output speckle divergence image
                final Tile spkDivTile = getSourceTile(targetProduct.getBand(tgtBandName), trgRect);
                final ProductData spkDivData = spkDivTile.getRawSamples();
                subsetInfo.productWriter.writeBandRasterData(spkDiv, 0, 0,
                        spkDiv.getSceneRasterWidth(), spkDiv.getSceneRasterHeight(), spkDivData, ProgressMonitor.NULL);
            }

            final BufferedImage image = ProductUtils.createColorIndexedImage(srcImage, ProgressMonitor.NULL);
            final File rgbFile = new File(tileDir.getParentFile(), tileDir.getName()+"_rgb.png");
            FileStoreDescriptor.create(image, rgbFile.getPath(), "PNG", null, null, null);

            // write tile to kml
            float w = subsetInfo.product.getSceneRasterWidth();
            float h = subsetInfo.product.getSceneRasterHeight();

            // quadPositions: counter clockwise lon,lat coordinates starting at lower-left
            GeoPos[] quadPositions = new GeoPos[]{
                    subsetInfo.product.getGeoCoding().getGeoPos(new PixelPos(0, h), null),
                    subsetInfo.product.getGeoCoding().getGeoPos(new PixelPos(w, h), null),
                    subsetInfo.product.getGeoCoding().getGeoPos(new PixelPos(w, 0), null),
                    subsetInfo.product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null),
            };
            kmlWriter.writeGroundOverlayEx(rgbFile.getName(), quadPositions, rgbFile.getName());

        } catch (Throwable t) {
            //throw new OperatorException(t);
            t.printStackTrace();
        }
    }

    private static class TileInfo {
        String name;
        int tileX;
        int tileY;
        int x;
        int y;
        int width;
        int height;

        TileInfo(String name, int tileX, int tileY, Rectangle rectangle) {
            this.name = name;
            this.tileX = tileX;
            this.tileY = tileY;

            x = rectangle.x;
            y = rectangle.y;
            width = rectangle.width;
            height = rectangle.height;
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