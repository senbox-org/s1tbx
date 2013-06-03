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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Document;
import org.jdom.Element;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ship detection discrimination operator. False ship detections are eliminated based on simple target
 * measurements. The operator first clusters contiguous detected pixels into a single cluster and then
 * extracts the width and length information from the target. Based on these measurements and user input
 * discrimination criteria, targets that are too big or too small are eliminated.
 *
 * [1] D. J. Crisp, "The State-of-the-Art in Ship Detection in Synthetic Aperture Radar Imagery." DSTO–RR–0272,
 * 2004-05.
 */
@OperatorMetadata(alias = "Object-Discrimination",
        category = "Ocean-Tools",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Remove false alarms from the detected objects.")
public class ObjectDiscriminationOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "Minimum target size", defaultValue = "80.0", label="Minimum Target Size (m)")
    private double minTargetSizeInMeter = 80.0;

    @Parameter(description = "Maximum target size", defaultValue = "400.0", label="Maximum Target Size (m)")
    private double maxTargetSizeInMeter = 400.0;

    private boolean clusteringPerformed = false;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private double rangeSpacing = 0;
    private double azimuthSpacing = 0;

    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private MetadataElement absRoot = null;
    private final transient Map<Band, Band> bandMap = new HashMap<Band, Band>(3);
    private final HashMap<String, List<ShipRecord>> bandClusterLists = new HashMap<String, List<ShipRecord>>();
    private File targetReportFile = null;


    @Override
    public void initialize() throws OperatorException {
        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getPixelSpacings();

            getSourceImageDimension();

            getTiePointGrid();

            setTargetReportFilePath();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get the range and azimuth spacings (in meter).
     * @throws Exception when metadata is missing or equal to default no data value
     */
    private void getPixelSpacings() throws Exception {

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        //System.out.println("Range spacing is " + rangeSpacing);
        //System.out.println("Azimuth spacing is " + azimuthSpacing);
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
        //System.out.println("Source image width = " + sourceImageWidth);
        //System.out.println("Source image height = " + sourceImageHeight);
    }

    /**
     * Get latitude anf longitude tie point grid.
     */
    private void getTiePointGrid() {
        latitude = OperatorUtils.getLatitude(sourceProduct);
        longitude = OperatorUtils.getLongitude(sourceProduct);
    }

    /**
     * Set absolute path for outputing target report file.
     */
    private void setTargetReportFilePath() {
        final String fileName = sourceProduct.getName() + "_object_detection_report.xml";
        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if(!appUserDir.exists()) {
            appUserDir.mkdirs();
        }
        targetReportFile = new File(appUserDir.toString(), fileName);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        updateTargetProductMetadata();
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] bands = sourceProduct.getBands();
        final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
        for (Band band : bands) {
            bandNameList.add(band.getName());
        }
        final String[] sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for (Band srcBand : sourceBands) {

            final String srcBandName = srcBand.getName();
            if (!srcBandName.contains(AdaptiveThresholdingOp.SHIPMASK_NAME)) {

                final Band targetBand = new Band(srcBandName,
                                                 srcBand.getDataType(),
                                                 sourceImageWidth,
                                                 sourceImageHeight);

                targetBand.setUnit(srcBand.getUnit());
                targetProduct.addBand(targetBand);

                bandClusterLists.put(srcBandName, new ArrayList<ShipRecord>());

                final String bitMaskBandName = srcBandName + AdaptiveThresholdingOp.SHIPMASK_NAME;
                final Band bitMaskBand = sourceProduct.getBand(bitMaskBandName);
                if (bitMaskBand != null) {
                    bandMap.put(srcBand, bitMaskBand);
                } else {
                    throw new OperatorException("No bit mask band found for band: " + srcBandName);
                }

            }
        }
    }

    /**
     * Save target report file path in the metadata.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absTgt.setAttributeString(AbstractMetadata.target_report_file, targetReportFile.getAbsolutePath());
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

            final int x0 = Math.max(tx0 - 10, 0);
            final int y0 = Math.max(ty0 - 10, 0);
            final int w  = Math.min(tw + 20, sourceImageWidth);
            final int h  = Math.min(th + 20, sourceImageHeight);
            final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcData = sourceTile.getDataBuffer();
            final int[][] pixelsScanned = new int[h][w];
            final List<ShipRecord> clusterList = bandClusterLists.get(targetBand.getName());

            final Band bitMaskBand = bandMap.get(sourceBand);
            final Tile bitMaskTile = getSourceTile(bitMaskBand, sourceTileRectangle);
            final ProductData bitMaskData = bitMaskTile.getDataBuffer();

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceTile);    // src and trg tile are different size

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {

                    final int srcIdx = srcIndex.getIndex(tx);

                    if (pixelsScanned[ty - y0][tx - x0] == 0 &&
                        bitMaskData.getElemIntAt(srcIdx) == 1) {

                        final List<PixelPos> clusterPixels = new ArrayList<PixelPos>();
                        clustering(tx, ty, x0, y0, w, h, bitMaskData, bitMaskTile, pixelsScanned, clusterPixels);

                        final ShipRecord record = generateRecord(x0, y0, w, h, clusterPixels);

                        final double size = Math.sqrt(record.length*record.length + record.width*record.width);
                        if (size >= minTargetSizeInMeter && size <= maxTargetSizeInMeter) {
                            getClusterIntensity(clusterPixels, srcData, sourceTile, record);
                            clusterList.add(record);
                        }
                    }
                    trgData.setElemDoubleAt(trgIndex.getIndex(tx), srcData.getElemDoubleAt(srcIdx));
                }
            }

            clusteringPerformed = true;
       } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
       }
    }

    /**
     * Find pixels detected as target in a 3x3 window centered at a given point.
     * @param xc The x coordinate of the given point.
     * @param yc The y coordinate of the given point.
     * @param x0 The x coordinate for the upper left corner point of the source rectangle.
     * @param y0 The y coordinate for the upper left corner point of the source rectangle.
     * @param w The width of the source rectangle.
     * @param h The height of the source rectangle.
     * @param bitMaskData The bit maks band data.
     * @param bitMaskTile The bit mask band tile.
     * @param pixelsScanned The binary array indicating which pixel in the tile has been scaned.
     * @param clusterPixels The list of pixels in the cluster.
     */
    private static void clustering(final int xc, final int yc, final int x0, final int y0, final int w, final int h,
                            final ProductData bitMaskData, final Tile bitMaskTile,
                            final int[][] pixelsScanned, List<PixelPos> clusterPixels) {

        pixelsScanned[yc - y0][xc - x0] = 1;
        clusterPixels.add(new PixelPos(xc, yc));

        final int[] x = {xc-1,   xc, xc+1, xc-1, xc+1, xc-1,   xc, xc+1};
        final int[] y = {yc-1, yc-1, yc-1,   yc,   yc, yc+1, yc+1, yc+1};

        for (int i = 0; i < 8; i++) {

            if (x[i] >= x0 && x[i] < x0 + w && y[i] >= y0 && y[i] < y0 + h &&
                pixelsScanned[y[i] - y0][x[i] - x0] == 0 &&
                bitMaskData.getElemIntAt(bitMaskTile.getDataBufferIndex(x[i], y[i])) == 1) {

                clustering(x[i], y[i], x0, y0, w, h, bitMaskData, bitMaskTile, pixelsScanned, clusterPixels);
            }
        }
    }

    /**
     * Generate a ship record for the detected cluster.
     * @param x0 The x coordinate for the upper left corner point of the source rectangle.
     * @param y0 The y coordinate for the upper left corner point of the source rectangle.
     * @param w The width of the source rectangle.
     * @param h The height of the source rectangle.
     * @param clusterPixels The list of pixels in the cluster.
     * @return ShipRecord
     */
    private ShipRecord generateRecord(final int x0, final int y0, final int w, final int h,
                                List<PixelPos> clusterPixels) {
        int xMin = x0 + w -1;
        int xMax = x0;
        int yMin = y0 + h -1;
        int yMax = y0;

        for (PixelPos pixel : clusterPixels) {
            if (pixel.x < xMin) {
                xMin = (int)pixel.x;
            }

            if (pixel.x > xMax) {
                xMax = (int)pixel.x;
            }

            if (pixel.y < yMin) {
                yMin = (int)pixel.y;
            }

            if (pixel.y > yMax) {
                yMax = (int)pixel.y;
            }
        }

        final double xMid = (xMin + xMax)/2.0;
        final double yMid = (yMin + yMax)/2.0;
        final double lat = latitude.getPixelDouble(xMid, yMid);
        final double lon = longitude.getPixelDouble(xMid, yMid);

        final double width = (xMax - xMin + 1)*rangeSpacing;
        final double length = (yMax - yMin + 1)*azimuthSpacing;

        return new ShipRecord(lat, lon, width, length, 0.0);
    }

    /**
     * compute total cluster intensity.
     * @param clusterPixels The list of pixels in the cluster.
     * @param srcData The source band data.
     * @param sourceTile The souce band tile.
     * @param record The ship record.
     */
    private static void getClusterIntensity(final List<PixelPos> clusterPixels, final ProductData srcData,
                                     final Tile sourceTile, ShipRecord record) {

        double totalIntensity = 0.0;
        for (PixelPos pixel : clusterPixels) {
            totalIntensity += srcData.getElemDoubleAt(sourceTile.getDataBufferIndex((int)pixel.x, (int)pixel.y));
        }

        record.intensity = totalIntensity;
    }

    /**
     * Output cluster information to file.
     */
    @Override
    public void dispose() {

        if (!clusteringPerformed) {
            return;
        }

        writeBandClusterListsToFile();
    }

    /**
     * Output cluster information to file.
     * @throws OperatorException when can't save metadata
     */
    private void writeBandClusterListsToFile() throws OperatorException {

        final Element root = new Element("Detection");
        final Document doc = new Document(root);

        for (String bandName : bandClusterLists.keySet())  {
            final Element elem = new Element("targetsDetected");
            elem.setAttribute("bandName", bandName);
            final List<ShipRecord> clusterList = bandClusterLists.get(bandName);
            for (ShipRecord rec : clusterList) {
                final Element subElem = new Element("target");
                subElem.setAttribute("lat", String.valueOf(rec.lat));
                subElem.setAttribute("lon", String.valueOf(rec.lon));
                subElem.setAttribute("width", String.valueOf(rec.width));
                subElem.setAttribute("length", String.valueOf(rec.length));
                subElem.setAttribute("intensity", String.valueOf(rec.intensity));
                elem.addContent(subElem);
            }
            root.addContent(elem);
        }
        XMLSupport.SaveXML(doc, targetReportFile.getAbsolutePath());
    }


    public static class ShipRecord {
        public double lat;
        public double lon;
        public double width;
        public double length;
        public double intensity;

        public ShipRecord(final double lat, final double lon, final double width,
                          final double length, final double intensity) {
            this.lat = lat;
            this.lon = lon;
            this.width = width;
            this.length = length;
            this.intensity = intensity;
        }
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ObjectDiscriminationOp.class);
        }
    }
}
