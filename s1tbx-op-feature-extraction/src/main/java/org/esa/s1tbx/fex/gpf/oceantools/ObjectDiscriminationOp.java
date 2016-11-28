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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.engine_utilities.util.VectorUtils;
import org.geotools.feature.FeatureCollection;
import org.jdom2.Document;
import org.jdom2.Element;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ship detection discrimination operator. False ship detections are eliminated based on simple target
 * measurements. The operator first clusters contiguous detected pixels into a single cluster and then
 * extracts the width and length information from the target. Based on these measurements and user input
 * discrimination criteria, targets that are too big or too small are eliminated.
 * <p/>
 * [1] D. J. Crisp, "The State-of-the-Art in Ship Detection in Synthetic Aperture Radar Imagery." DSTO-RR-0272, 2004-05.
 */
@OperatorMetadata(alias = "Object-Discrimination",
        category = "Radar/SAR Applications/Ocean Applications/Object Detection",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Remove false alarms from the detected objects.")
public class ObjectDiscriminationOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "Minimum target size", defaultValue = "50.0", label = "Minimum Target Size (m)")
    private double minTargetSizeInMeter = 50.0;

    @Parameter(description = "Maximum target size", defaultValue = "600.0", label = "Maximum Target Size (m)")
    private double maxTargetSizeInMeter = 600.0;

    private boolean clusteringPerformed = false;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private double rangeSpacing = 0;
    private double azimuthSpacing = 0;

    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private MetadataElement absRoot = null;
    private final transient Map<Band, Band> bandMap = new HashMap<>(3);
    private final HashMap<String, List<ShipRecord>> bandClusterLists = new HashMap<>();
    private File targetReportFile = null;
    private SimpleFeatureType shipFeatureType;

    private static final String VECTOR_NODE_NAME = "ShipDetections";
    private static final String STYLE_FORMAT = "fill:#ff0000; fill-opacity:0.2; stroke:#ff0000; stroke-opacity:1.0; stroke-width:1.0; symbol:circle";

    private static final String ATTRIB_WIDTH = "width";
    private static final String ATTRIB_LENGTH= "length";
    private static final String ATTRIB_INTENSITY = "intensity";

    private static final String PRODUCT_SUFFIX = "_SHP";

    @Override
    public void initialize() throws OperatorException {
        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
            azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            latitude = OperatorUtils.getLatitude(sourceProduct);
            longitude = OperatorUtils.getLongitude(sourceProduct);

            setTargetReportFilePath();

            createTargetProduct();

            shipFeatureType = createFeatureType();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Set absolute path for outputing target report file.
     */
    private void setTargetReportFilePath() {
        final String fileName = sourceProduct.getName() + "_object_detection_report.xml";
        targetReportFile = new File(ResourceUtils.getReportFolder(), fileName);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        updateTargetProductMetadata();
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] bands = sourceProduct.getBands();
        final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
        for (Band band : bands) {
            bandNameList.add(band.getName());
        }
        final String[] sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        for (Band srcBand : sourceBands) {

            final String srcBandName = srcBand.getName();
            if (!srcBandName.contains(AdaptiveThresholdingOp.SHIPMASK_NAME)) {

                final Band targetBand = new Band(srcBandName,
                        srcBand.getDataType(),
                        sourceImageWidth,
                        sourceImageHeight);

                targetBand.setNoDataValueUsed(true);
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setUnit(srcBand.getUnit());
                targetProduct.addBand(targetBand);

                bandClusterLists.put(srcBandName, new ArrayList<>());

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

            final int x0 = Math.max(tx0 - 10, 0);
            final int y0 = Math.max(ty0 - 10, 0);
            final int w = Math.min(tw + 20, sourceImageWidth);
            final int h = Math.min(th + 20, sourceImageHeight);
            final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcData = sourceTile.getDataBuffer();
            final int[][] pixelsScanned = new int[h][w];
            final List<ShipRecord> clusterList = new ArrayList<>();

            targetTile.setRawSamples(getSourceTile(sourceBand, targetTileRectangle).getRawSamples());

            final Band bitMaskBand = bandMap.get(sourceBand);
            final Tile bitMaskTile = getSourceTile(bitMaskBand, sourceTileRectangle);
            final ProductData bitMaskData = bitMaskTile.getDataBuffer();

            final TileIndex srcIndex = new TileIndex(sourceTile);    // src and trg tile are different size

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            for (int ty = ty0; ty < maxy; ty++) {
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {

                    if (pixelsScanned[ty - y0][tx - x0] == 0) {

                        if (bitMaskData.getElemIntAt(srcIndex.getIndex(tx)) == 1) {

                            final List<PixelPos> clusterPixels = new ArrayList<>();
                            clustering(tx, ty, x0, y0, w, h, bitMaskData, bitMaskTile, pixelsScanned, clusterPixels);

                            final ShipRecord record = generateRecord(x0, y0, w, h, clusterPixels);

                            final double size = Math.sqrt(record.length * record.length + record.width * record.width);
                            if (size >= minTargetSizeInMeter && size <= maxTargetSizeInMeter) {
                                getClusterIntensity(clusterPixels, srcData, sourceTile, record);

                                clusterList.add(record);
                            }
                        }
                    }
                }
            }

            if(!clusterList.isEmpty()) {
                AddShipRecordsAsVectors(clusterList);
            }
            bandClusterLists.get(targetBand.getName()).addAll(clusterList);

            clusteringPerformed = true;
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Find pixels detected as target in a 3x3 window centered at a given point.
     *
     * @param xc            The x coordinate of the given point.
     * @param yc            The y coordinate of the given point.
     * @param x0            The x coordinate for the upper left corner point of the source rectangle.
     * @param y0            The y coordinate for the upper left corner point of the source rectangle.
     * @param w             The width of the source rectangle.
     * @param h             The height of the source rectangle.
     * @param bitMaskData   The bit maks band data.
     * @param bitMaskTile   The bit mask band tile.
     * @param pixelsScanned The binary array indicating which pixel in the tile has been scaned.
     * @param clusterPixels The list of pixels in the cluster.
     */
    private static void clustering(final int xc, final int yc, final int x0, final int y0, final int w, final int h,
                                   final ProductData bitMaskData, final Tile bitMaskTile,
                                   final int[][] pixelsScanned, List<PixelPos> clusterPixels) {

        pixelsScanned[yc - y0][xc - x0] = 1;
        clusterPixels.add(new PixelPos(xc, yc));

        final int[] x = {xc - 1, xc, xc + 1, xc - 1, xc + 1, xc - 1, xc, xc + 1};
        final int[] y = {yc - 1, yc - 1, yc - 1, yc, yc, yc + 1, yc + 1, yc + 1};

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
     *
     * @param x0            The x coordinate for the upper left corner point of the source rectangle.
     * @param y0            The y coordinate for the upper left corner point of the source rectangle.
     * @param w             The width of the source rectangle.
     * @param h             The height of the source rectangle.
     * @param clusterPixels The list of pixels in the cluster.
     * @return ShipRecord
     */
    private ShipRecord generateRecord(final int x0, final int y0, final int w, final int h,
                                      List<PixelPos> clusterPixels) {
        int xMin = x0 + w - 1;
        int xMax = x0;
        int yMin = y0 + h - 1;
        int yMax = y0;

        for (PixelPos pixel : clusterPixels) {
            if (pixel.x < xMin) {
                xMin = (int) pixel.x;
            }

            if (pixel.x > xMax) {
                xMax = (int) pixel.x;
            }

            if (pixel.y < yMin) {
                yMin = (int) pixel.y;
            }

            if (pixel.y > yMax) {
                yMax = (int) pixel.y;
            }
        }

        final double xMid = (xMin + xMax) / 2.0;
        final double yMid = (yMin + yMax) / 2.0;
        final double lat = latitude.getPixelDouble(xMid, yMid);
        final double lon = longitude.getPixelDouble(xMid, yMid);

        final double width = (xMax - xMin + 1) * rangeSpacing;
        final double length = (yMax - yMin + 1) * azimuthSpacing;

        return new ShipRecord((int)xMid, (int)yMid, lat, lon, width, length, 0.0);
    }

    /**
     * compute total cluster intensity.
     *
     * @param clusterPixels The list of pixels in the cluster.
     * @param srcData       The source band data.
     * @param sourceTile    The souce band tile.
     * @param record        The ship record.
     */
    private static void getClusterIntensity(final List<PixelPos> clusterPixels, final ProductData srcData,
                                            final Tile sourceTile, ShipRecord record) {

        double totalIntensity = 0.0;
        for (PixelPos pixel : clusterPixels) {
            totalIntensity += srcData.getElemDoubleAt(sourceTile.getDataBufferIndex((int) pixel.x, (int) pixel.y));
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
     *
     * @throws OperatorException when can't save metadata
     */
    private void writeBandClusterListsToFile() throws OperatorException {

        final Element root = new Element("Detection");
        final Document doc = new Document(root);

        for (String bandName : bandClusterLists.keySet()) {
            final Element elem = new Element("targetsDetected");
            elem.setAttribute("bandName", bandName);
            final List<ShipRecord> clusterList = bandClusterLists.get(bandName);
            for (ShipRecord rec : clusterList) {
                final Element subElem = new Element("target");
                subElem.setAttribute("x", String.valueOf(rec.x));
                subElem.setAttribute("y", String.valueOf(rec.y));
                subElem.setAttribute("lat", String.valueOf(rec.lat));
                subElem.setAttribute("lon", String.valueOf(rec.lon));
                subElem.setAttribute(ATTRIB_WIDTH, String.valueOf(rec.width));
                subElem.setAttribute(ATTRIB_LENGTH, String.valueOf(rec.length));
                subElem.setAttribute(ATTRIB_INTENSITY, String.valueOf(rec.intensity));

                writeExtraFeatureAttributes(subElem);

                elem.addContent(subElem);
            }
            root.addContent(elem);
        }

        try {
            XMLSupport.SaveXML(doc, targetReportFile.getAbsolutePath());
        } catch (IOException e) {
            SystemUtils.LOG.warning("Unable to save target report " + e.getMessage());
        }
    }

    private SimpleFeatureType createFeatureType() {

        final List<AttributeDescriptor> attributeDescriptors = new ArrayList<>();
        attributeDescriptors.add(VectorUtils.createAttribute(ATTRIB_WIDTH, Double.class));
        attributeDescriptors.add(VectorUtils.createAttribute(ATTRIB_LENGTH, Double.class));
        attributeDescriptors.add(VectorUtils.createAttribute(ATTRIB_INTENSITY, Double.class));

        List<AttributeInfo> attributeInfoList = getExtraFeatureAttributes();
        for (AttributeInfo attributeInfo : attributeInfoList) {
            attributeDescriptors.add(
                    VectorUtils.createAttribute(attributeInfo.attributeName, attributeInfo.attributeClass));
        }

        return VectorUtils.createFeatureType(targetProduct, VECTOR_NODE_NAME, attributeDescriptors);
    }

    private synchronized void AddShipRecordsAsVectors(final List<ShipRecord> clusterList) {

        VectorDataNode vectorDataNode = targetProduct.getVectorDataGroup().get(VECTOR_NODE_NAME);
        if(vectorDataNode == null) {
            vectorDataNode = new VectorDataNode(VECTOR_NODE_NAME, shipFeatureType);
            targetProduct.getVectorDataGroup().add(vectorDataNode);
        }
        final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = vectorDataNode.getFeatureCollection();
        final GeometryFactory geometryFactory = new GeometryFactory();

        int c = collection.size();
        for (ShipRecord rec : clusterList) {

            final String name = "target_" + StringUtils.padNum(c, 3, '0');

            Point p = geometryFactory.createPoint(new Coordinate(rec.x, rec.y));

            final SimpleFeature feature = PlainFeatureFactory.createPlainFeature(shipFeatureType, name, p, STYLE_FORMAT);
            feature.setAttribute(ATTRIB_WIDTH, rec.width);
            feature.setAttribute(ATTRIB_LENGTH, rec.length);
            feature.setAttribute(ATTRIB_INTENSITY, rec.intensity);
            setExtraFeatureAttributes(feature, rec);

            collection.add(feature);
            c++;
        }
    }

    protected List<AttributeInfo> getExtraFeatureAttributes() {
        return new ArrayList<>();
    }

    protected void writeExtraFeatureAttributes(final Element subElem) {

    }

    protected void setExtraFeatureAttributes(final SimpleFeature feature, final ShipRecord shipRecord) {

    }

    public static class ShipRecord {
        public final int x;
        public final int y;
        public final double lat;
        public final double lon;
        public final double width;
        public final double length;
        public double intensity;

        public ShipRecord(final int x, final int y,
                          final double lat, final double lon, final double width,
                          final double length, final double intensity) {
            this.x = x;
            this.y = y;
            this.lat = lat;
            this.lon = lon;
            this.width = width;
            this.length = length;
            this.intensity = intensity;
        }
    }

    public static class AttributeInfo {
        public final String attributeName;
        public final Class attributeClass;

        public AttributeInfo(final String attributeName, final Class attributeClass) {
            this.attributeName = attributeName;
            this.attributeClass = attributeClass;
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
