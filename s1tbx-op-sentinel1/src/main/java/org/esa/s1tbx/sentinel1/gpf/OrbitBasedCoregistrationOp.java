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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.s1tbx.insar.gpf.geometric.SARGeocoding;
import org.esa.s1tbx.insar.gpf.geometric.SARUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.ProductInformation;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Co-registering non-burst products based on orbits and DEM.
 */
@OperatorMetadata(alias = "Orbit-Based-Coregistration",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Orbit based co-registration")
public final class OrbitBasedCoregistrationOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME;

    @Parameter(defaultValue = "true", label = "Mask out areas with no elevation")
    private boolean maskOutAreaWithoutElevation = true;

    @Parameter(defaultValue = "false", label = "Output Range and Azimuth Offset")
    private boolean outputRangeAzimuthOffset = false;

    private Resampling selectedResampling = null;
    private Product masterProduct = null;
    private Product slaveProduct = null;
    private Metadata mstMetadata = new Metadata();
    private Metadata slvMetadata = new Metadata();
    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0; // no data value for DEM
    private double noDataValue = 0.0;
    private GeoCoding targetGeoCoding = null;
    private final HashMap<Band, Band> targetBandToSlaveBandMap = new HashMap<>(2);
    private final double invalidIndex = -9999.0;
    private final double extendedPercentage = 0.1; // extend DEM tile by 10%

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public OrbitBasedCoregistrationOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            if (sourceProduct.length != 2) {
                throw new OperatorException("Please select two source products");
            }

            masterProduct = sourceProduct[0];
            slaveProduct = sourceProduct[1];

            getProductMetadata(masterProduct, mstMetadata);
            getProductMetadata(slaveProduct, slvMetadata);

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);

            createTargetProduct();

            updateTargetProductMetadata();

            noDataValue = masterProduct.getBandAt(0).getNoDataValue();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static void getProductMetadata(final Product sourceProduct, final Metadata metadata) throws Exception {

        metadata.sourceImageWidth = sourceProduct.getSceneRasterWidth();
        metadata.sourceImageHeight = sourceProduct.getSceneRasterHeight();

        metadata.absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        metadata.srgrFlag = AbstractMetadata.getAttributeBoolean(metadata.absRoot, AbstractMetadata.srgr_flag);

        metadata.wavelength = SARUtils.getRadarFrequency(metadata.absRoot);

        metadata.rangeSpacing = AbstractMetadata.getAttributeDouble(
                metadata.absRoot, AbstractMetadata.range_spacing);

        metadata.azimuthSpacing = AbstractMetadata.getAttributeDouble(
                metadata.absRoot, AbstractMetadata.azimuth_spacing);

        metadata.firstLineTime = AbstractMetadata.parseUTC(
                metadata.absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        metadata.lastLineTime = AbstractMetadata.parseUTC(
                metadata.absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days

        metadata.lineTimeInterval = metadata.absRoot.getAttributeDouble(
                AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day

        OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(
                metadata.absRoot);

        metadata.orbit = new SARGeocoding.Orbit(
                orbitStateVectors, metadata.firstLineTime, metadata.lineTimeInterval, metadata.sourceImageHeight);


        if (metadata.srgrFlag) {
            metadata.srgrConvParams = AbstractMetadata.getSRGRCoefficients(metadata.absRoot);
        } else {
            metadata.nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(
                    metadata.absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        metadata.nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, metadata.sourceImageWidth);

    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(
                masterProduct.getName(),
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(masterProduct, targetProduct);
        
        final String[] masterBandNames = masterProduct.getBandNames();
        final String mstSuffix = "_mst" + StackUtils.createBandTimeStamp(masterProduct);
        for (String bandName : masterBandNames) {
            if (masterProduct.getBand(bandName) instanceof VirtualBand) {
                continue;
            }
            final Band targetBand = ProductUtils.copyBand(
                    bandName, masterProduct, bandName + mstSuffix, targetProduct, true);

            if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(
                        targetProduct, targetProduct.getBandAt(idx-1), targetBand, mstSuffix);
            }
        }

        final Band masterBand = masterProduct.getBand(masterBandNames[0]);
        final int masterBandWidth = masterBand.getSceneRasterWidth();
        final int masterBandHeight = masterBand.getSceneRasterHeight();

        final String[] slaveBandNames = slaveProduct.getBandNames();
        final String slvSuffix = "_slv1" + StackUtils.createBandTimeStamp(slaveProduct);
        for (String bandName:slaveBandNames) {
            final Band srcBand = slaveProduct.getBand(bandName);
            if (srcBand instanceof VirtualBand) {
                continue;
            }
            final Band targetBand = new Band(
                    bandName + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            targetBand.setUnit(srcBand.getUnit());
            targetBand.setDescription(srcBand.getDescription());
            targetProduct.addBand(targetBand);
            targetBandToSlaveBandMap.put(targetBand, srcBand);

            if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(
                        targetProduct, targetProduct.getBandAt(idx-1), targetBand, slvSuffix);
            }
        }

        copySlaveMetadata();

        if (outputRangeAzimuthOffset) {
            final Band azOffsetBand = new Band(
                    "azOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            azOffsetBand.setUnit("Index");
            targetProduct.addBand(azOffsetBand);

            final Band rgOffsetBand = new Band(
                    "rgOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            rgOffsetBand.setUnit("Index");
            targetProduct.addBand(rgOffsetBand);
        }

        targetGeoCoding = targetProduct.getSceneGeoCoding();
    }

    private void copySlaveMetadata() {

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(
                targetProduct.getMetadataRoot());
        final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(slaveProduct);
        if (slvAbsMetadata != null) {
            final String timeStamp = StackUtils.createBandTimeStamp(slaveProduct);
            final MetadataElement targetSlaveMetadata = new MetadataElement(slaveProduct.getName() + timeStamp);
            targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
            ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
        }
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);
        final MetadataElement slvInputElem = ProductInformation.getInputProducts(slaveProduct);
        final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
        for (MetadataAttribute attrib : slvInputProductAttrbList) {
            final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(
                    inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
            inputAttrb.getData().setElems(attrib.getData().getElemString());
        }
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
     public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
             throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            final PixelPos[][] slavePixPos = computeSlavePixPos(tx0, ty0, tw, th, pm);

            if (slavePixPos == null) {
                return;
            }

            if (outputRangeAzimuthOffset) {
                outputRangeAzimuthOffsets(tx0, ty0, tw, th, targetTileMap, slavePixPos);
            }

            final int margin = selectedResampling.getKernelSize();
            final Rectangle sourceRectangle = getBoundingBox(slavePixPos, margin);

            if (sourceRectangle == null) {
                return;
            }

            performInterpolation(tx0, ty0, tw, th, sourceRectangle, targetTileMap, slavePixPos);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get elevation model.
     *
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) return;
        try {
            if (externalDEMFile != null) { // if external DEM file is specified by user
                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                demNoDataValue = externalDEMNoDataValue;
                demName = externalDEMFile.getPath();
            } else {
                dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
                demNoDataValue = dem.getDescriptor().getNoDataValue();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        isElevationModelAvailable = true;
    }

    private PixelPos[][] computeSlavePixPos(final int x0, final int y0, final int w, final int h, ProgressMonitor pm)
            throws Exception {

        try {
            // Compute lat/lon boundaries for target tile
            final double[] latLonMinMax = new double[4];
            computeImageGeoBoundary(x0, x0 + w, y0, y0 + h, latLonMinMax);

            final double extralat = extendedPercentage*(latLonMinMax[1] - latLonMinMax[0]);
            final double extralon = extendedPercentage*(latLonMinMax[3] - latLonMinMax[2]);

            final double latMin = latLonMinMax[0] - extralat;
            final double latMax = latLonMinMax[1] + extralat;
            final double lonMin = latLonMinMax[2] - extralon;
            final double lonMax = latLonMinMax[3] + extralon;

            // Compute lat/lon indices in DEM for the boundaries;
            final PixelPos upperLeft = dem.getIndex(new GeoPos(latMax, lonMin));
            final PixelPos lowerRight = dem.getIndex(new GeoPos(latMin, lonMax));
            final int latMaxIdx = (int)Math.floor(upperLeft.getY());
            final int latMinIdx = (int)Math.ceil(lowerRight.getY());
            final int lonMinIdx = (int)Math.floor(upperLeft.getX());
            final int lonMaxIdx = (int)Math.ceil(lowerRight.getX());

            // Loop through all DEM points bounded by the indices computed above. For each point,
            // get its lat/lon and its azimuth/range indices in target image;
            final int numLines = latMinIdx - latMaxIdx;
            final int numPixels = lonMaxIdx - lonMinIdx;
            double[][] masterAz = new double[numLines][numPixels];
            double[][] masterRg = new double[numLines][numPixels];
            double[][] slaveAz = new double[numLines][numPixels];
            double[][] slaveRg = new double[numLines][numPixels];
            double[][] lat = new double[numLines][numPixels];
            double[][] lon = new double[numLines][numPixels];
            final PositionData posData = new PositionData();
            final PixelPos pix = new PixelPos();

            boolean noValidSlavePixPos = true;
            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    pix.setLocation(lonMinIdx + p, latMaxIdx + l);
                    GeoPos gp = dem.getGeoPos(pix);
                    lat[l][p] = gp.lat;
                    lon[l][p] = gp.lon;
                    final double alt = dem.getElevation(gp);

                    if (alt != demNoDataValue) {
                        GeoUtils.geo2xyzWGS84(gp.lat, gp.lon, alt, posData.earthPoint);
                        if(getPosition(gp.lat, gp.lon, alt, mstMetadata, posData)) {

                            masterAz[l][p] = posData.azimuthIndex;
                            masterRg[l][p] = posData.rangeIndex;
                            if (getPosition(gp.lat, gp.lon, alt, slvMetadata, posData)) {

                                slaveAz[l][p] = posData.azimuthIndex;
                                slaveRg[l][p] = posData.rangeIndex;
                                noValidSlavePixPos = false;
                                continue;
                            }
                        }
                    }

                    masterAz[l][p] = invalidIndex;
                    masterRg[l][p] = invalidIndex;
                }
            }

            if (noValidSlavePixPos) {
                return null;
            }

            // Compute azimuth/range offsets for pixels in target tile using Delaunay interpolation
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, y0 + h - 1, x0, x0 + w - 1);

            final double rgAzRatio = mstMetadata.rangeSpacing / mstMetadata.azimuthSpacing;

            final double[][] latArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] lonArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] azArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] rgArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            for (double[] data : azArray) {
                Arrays.fill(data, invalidIndex);
            }
            for (double[] data : rgArray) {
                Arrays.fill(data, invalidIndex);
            }

            TriangleUtils.gridDataLinear(
                    masterAz, masterRg, slaveAz, slaveRg, lat, lon, azArray, rgArray, latArray, lonArray,
                    tileWindow, rgAzRatio, 1, 1, invalidIndex, 0);

            boolean allElementsAreNull = true;
            final PixelPos[][] slavePixelPos = new PixelPos[h][w];

            double alt = 0;
            for(int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if (rgArray[yy][xx] == invalidIndex || azArray[yy][xx] == invalidIndex) {
                        slavePixelPos[yy][xx] = null;
                    } else {
                        if (maskOutAreaWithoutElevation) {
                            alt = dem.getElevation(new GeoPos(latArray[yy][xx], lonArray[yy][xx]));
                            if (alt != demNoDataValue) {
                                slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                                allElementsAreNull = false;
                            } else {
                                slavePixelPos[yy][xx] = null;
                            }
                        } else {
                            slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                            allElementsAreNull = false;
                        }
                    }
                }
            }

            if (allElementsAreNull) {
                return null;
            }

            return slavePixelPos;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlavePixPos", e);
        }

        return null;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xMin, final int xMax, final int yMin, final int yMax,
                                         double[] latLonMinMax) throws Exception {

        final GeoPos geoPosFirstNear = targetGeoCoding.getGeoPos(new PixelPos(xMin, yMin), null);
        final GeoPos geoPosFirstFar = targetGeoCoding.getGeoPos(new PixelPos(xMax, yMin), null);
        final GeoPos geoPosLastNear = targetGeoCoding.getGeoPos(new PixelPos(xMin, yMax), null);
        final GeoPos geoPosLastFar = targetGeoCoding.getGeoPos(new PixelPos(xMax, yMax), null);

        final double[] lats = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
        final double[] lons = {geoPosFirstNear.getLon(), geoPosFirstFar.getLon(), geoPosLastNear.getLon(), geoPosLastFar.getLon()};
        double latMin = 90.0;
        double latMax = -90.0;
        for (double lat : lats) {
            if (lat < latMin) {
                latMin = lat;
            }
            if (lat > latMax) {
                latMax = lat;
            }
        }

        double lonMin = 180.0;
        double lonMax = -180.0;
        for (double lon : lons) {
            if (lon < lonMin) {
                lonMin = lon;
            }
            if (lon > lonMax) {
                lonMax = lon;
            }
        }

        latLonMinMax[0] = latMin;
        latLonMinMax[1] = latMax;
        latLonMinMax[2] = lonMin;
        latLonMinMax[3] = lonMax;
    }

    /**
     * Compute azimuth and range indices in SAR image for a given target point on the Earth's surface.
     */
    private static boolean getPosition(final double lat, final double lon, final double alt,
                                       final Metadata metadata, final PositionData data) {

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                metadata.firstLineTime, metadata.lineTimeInterval, metadata.wavelength, data.earthPoint,
                metadata.orbit.sensorPosition, metadata.orbit.sensorVelocity);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime, metadata.orbit, data.earthPoint, data.sensorPos);

        data.azimuthIndex = (zeroDopplerTime - metadata.firstLineTime) / metadata.lineTimeInterval;

        if (!metadata.srgrFlag) {
            data.rangeIndex = (data.slantRange - metadata.nearEdgeSlantRange) / metadata.rangeSpacing;
        } else {
            data.rangeIndex = SARGeocoding.computeRangeIndex(
                    metadata.srgrFlag, metadata.sourceImageWidth, metadata.firstLineTime, metadata.lastLineTime,
                    metadata.rangeSpacing, zeroDopplerTime, data.slantRange, metadata.nearEdgeSlantRange,
                    metadata.srgrConvParams);
        }

        if (!metadata.nearRangeOnLeft) {
            data.rangeIndex = metadata.sourceImageWidth - 1 - data.rangeIndex;
        }
        return true;
    }

    /**
     * Get the source rectangle in slave image that contains all the given pixels.
     */
    private Rectangle getBoundingBox(
            final PixelPos[][] slavePixPos, final int margin) {

        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (PixelPos[] slavePixPo : slavePixPos) {
            for (int j = 0; j < slavePixPos[0].length; j++) {
                if (slavePixPo[j] != null) {
                    final int x = (int) Math.floor(slavePixPo[j].getX());
                    final int y = (int) Math.floor(slavePixPo[j].getY());

                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        minX = Math.max(minX - margin, 0);
        maxX = Math.min(maxX + margin, slvMetadata.sourceImageWidth - 1);
        minY = Math.max(minY - margin, 0);
        maxY = Math.min(maxY + margin, slvMetadata.sourceImageHeight - 1);

        if (minX > maxX || minY > maxY) {
            return null;
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void performInterpolation(final int x0, final int y0, final int w, final int h,
                                      final Rectangle sourceRectangle, final Map<Band, Tile> targetTileMap,
                                      final PixelPos[][] slavePixPos) {

        try {
            final Set<Band> keySet = targetBandToSlaveBandMap.keySet();
            final int numSlvBands = targetBandToSlaveBandMap.size();
            Tile targetTile = null;
            final ProductData[] targetBuffers = new ProductData[numSlvBands];
            final ResamplingRaster[] slvResamplingRasters = new ResamplingRaster[numSlvBands];

            int k = 0;
            for (Band band : keySet) {
                targetTile = targetTileMap.get(band);
                targetBuffers[k] = targetTile.getDataBuffer();

                final Band slvBand = targetBandToSlaveBandMap.get(band);
                final Tile slvTile = getSourceTile(slvBand, sourceRectangle);
                final ProductData slvBuffer = slvTile.getDataBuffer();
                slvResamplingRasters[k] = new ResamplingRaster(slvTile, slvBuffer);
                k++;
            }

            final TileIndex tgtIndex = new TileIndex(targetTile);

            final Resampling.Index resamplingIndex = selectedResampling.createIndex();

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final PixelPos slavePixelPos = slavePixPos[yy][x - x0];

                    if (slavePixelPos == null) {
                        continue;
                    }

                    selectedResampling.computeCornerBasedIndex(slavePixelPos.x, slavePixelPos.y,
                            slvMetadata.sourceImageWidth, slvMetadata.sourceImageHeight, resamplingIndex);

                    saveToTargetBand(resamplingIndex, slvResamplingRasters, targetBuffers, tgtIdx);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("performInterpolation", e);
        }
    }

    private void saveToTargetBand(final Resampling.Index resamplingIndex, final ResamplingRaster[] slvResamplingRasters,
                                  final ProductData[] targetBuffers, final int tgtIdx) throws Exception {

        for (int i = 0; i < targetBuffers.length; ++i) {
            final double value = selectedResampling.resample(slvResamplingRasters[i], resamplingIndex);
            targetBuffers[i].setElemDoubleAt(tgtIdx, value);
        }
    }

    private void outputRangeAzimuthOffsets(final int x0, final int y0, final int w, final int h,
                                           final Map<Band, Tile> targetTileMap, final PixelPos[][] slavePixPos) {

        try {
            final Band azOffsetBand = targetProduct.getBand("azOffset");
            final Band rgOffsetBand = targetProduct.getBand("rgOffset");

            if (azOffsetBand == null || rgOffsetBand == null) {
                return;
            }

            final Tile tgtTileAzOffset = targetTileMap.get(azOffsetBand);
            final Tile tgtTileRgOffset = targetTileMap.get(rgOffsetBand);
            final ProductData tgtBufferAzOffset = tgtTileAzOffset.getDataBuffer();
            final ProductData tgtBufferRgOffset = tgtTileRgOffset.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileAzOffset);

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;

                    if (slavePixPos[yy][xx] == null) {
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                    } else {
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].y));
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].x));
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("outputRangeAzimuthOffsets", e);
        }
    }

    private static class PositionData {
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
        double slantRange;
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final ProductData dataBuffer;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;

        public ResamplingRaster(final Tile tile, final ProductData dataBuffer) {
            this.tile = tile;
            this.dataBuffer = dataBuffer;
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;

            try {
                for (int i = 0; i < y.length; i++) {
                    for (int j = 0; j < x.length; j++) {
                        final int index = tile.getDataBufferIndex(x[j], y[i]);
                        double v = dataBuffer.getElemDoubleAt(index);
                        if (usesNoData && (scalingApplied && geophysicalNoDataValue == v || noDataValue == v)) {
                            samples[i][j] = Double.NaN;
                            allValid = false;
                            continue;
                        }
                        samples[i][j] = v;
                    }
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
                allValid = false;
            }

            return allValid;
        }
    }

    private static class TriangleUtils {

        public static void gridDataLinear(final double[][] x_in, final double[][] y_in,
                                          final double[][] z1_in, final double[][] z2_in,
                                          final double[][] z3_in, final double[][] z4_in,
                                          final double[][] z1_out, final double[][] z2_out,
                                          final double[][] z3_out, final double[][] z4_out,
                                          final org.jlinda.core.Window window, final double xyRatio,
                                          final int xScale, final int yScale, final double invalidIndex,
                                          final int offset)
                throws Exception {

            final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, xyRatio, invalidIndex);

            if (FDT == null) {
                return;
            }

            interpolate(xyRatio, window, xScale, yScale, offset, invalidIndex, FDT,
                    z1_in, z2_in, z3_in, z4_in, z1_out, z2_out, z3_out, z4_out);
        }

        private static FastDelaunayTriangulator triangulate(final double[][] x_in, final double[][] y_in,
                                                            final double xyRatio, final double invalidIndex)
                throws Exception {

            java.util.List<Geometry> list = new ArrayList<>();
            GeometryFactory gf = new GeometryFactory();
            for (int i = 0; i < x_in.length; i++) {
                for (int j = 0; j < x_in[0].length; j++) {
                    if (x_in[i][j] == invalidIndex || y_in[i][j] == invalidIndex) {
                        continue;
                    }
                    list.add(gf.createPoint(new Coordinate(x_in[i][j], y_in[i][j] * xyRatio, i*x_in[0].length + j)));
                }
            }

            if (list.size() < 3) {
                return null;
            }

            FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
            try {
                FDT.triangulate(list.iterator());
            } catch (TriangulationException te) {
                te.printStackTrace();
            }

            return FDT;
        }

        private static void interpolate(final double xyRatio, final org.jlinda.core.Window tileWindow,
                                        final double xScale, final double yScale, final double offset,
                                        final double invalidIndex, FastDelaunayTriangulator FDT,
                                        final double[][] z1_in, final double[][] z2_in,
                                        final double[][] z3_in, final double[][] z4_in,
                                        final double[][] z1_out, final double[][] z2_out,
                                        final double[][] z3_out, final double[][] z4_out) {

            final double x_min = tileWindow.linelo;
            final double y_min = tileWindow.pixlo;

            int i, j; // counters
            long i_min, i_max, j_min, j_max; // minimas/maximas
            double xp, yp;
            double xkj, ykj, xlj, ylj;
            double f; // function
            double a, b, c;
            double zj, zk, zl, zkj, zlj;

            // containers for xy coordinates of Triangles: p1-p2-p3-p1
            double[] vx = new double[4];
            double[] vy = new double[4];
            double[] vz = new double[3];
            double[] abc1 = new double[3];
            double[] abc2 = new double[3];
            double[] abc3 = new double[3];
            double[] abc4 = new double[3];

            // declare demRadarCode_phase
            final int nx = (int) tileWindow.lines();
            final int ny = (int) tileWindow.pixels();

            // interpolate: loop over triangles
            for (Triangle triangle : FDT.triangles) {

                // store triangle coordinates in local variables
                vx[0] = vx[3] = triangle.getA().x;
                vy[0] = vy[3] = triangle.getA().y / xyRatio;

                vx[1] = triangle.getB().x;
                vy[1] = triangle.getB().y / xyRatio;

                vx[2] = triangle.getC().x;
                vy[2] = triangle.getC().y / xyRatio;

                // skip invalid indices
                if (vx[0] == invalidIndex || vx[1] == invalidIndex || vx[2] == invalidIndex ||
                    vy[0] == invalidIndex || vy[1] == invalidIndex || vy[2] == invalidIndex) {
                    continue;
                }

                // Compute grid indices the current triangle may cover
                xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
                i_min = coordToIndex(xp, x_min, xScale, offset);

                xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
                i_max = coordToIndex(xp, x_min, xScale, offset);

                yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
                j_min = coordToIndex(yp, y_min, yScale, offset);

                yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
                j_max = coordToIndex(yp, y_min, yScale, offset);

                // skip triangle that is above or below the region
                if ((i_max < 0) || (i_min >= nx)) {
                    continue;
                }

                // skip triangle that is on the left or right of the region
                if ((j_max < 0) || (j_min >= ny)) {
                    continue;
                }

                // triangle covers the upper or lower boundary
                if (i_min < 0) {
                    i_min = 0;
                }

                if (i_max >= nx) {
                    i_max = nx - 1;
                }

                // triangle covers left or right boundary
                if (j_min < 0) {
                    j_min = 0;
                }

                if (j_max >= ny) {
                    j_max = ny - 1;
                }

                // compute plane defined by the three vertices of the triangle: z = ax + by + c
                xkj = vx[1] - vx[0];
                ykj = vy[1] - vy[0];
                xlj = vx[2] - vx[0];
                ylj = vy[2] - vy[0];

                f = 1.0 / (xkj * ylj - ykj * xlj);

                vz[0] = triangle.getA().z;
                vz[1] = triangle.getB().z;
                vz[2] = triangle.getC().z;

                abc1 = getABC(vx, vy, vz, z1_in, f, xkj, ykj, xlj, ylj);
                abc2 = getABC(vx, vy, vz, z2_in, f, xkj, ykj, xlj, ylj);
                abc3 = getABC(vx, vy, vz, z3_in, f, xkj, ykj, xlj, ylj);
                abc4 = getABC(vx, vy, vz, z4_in, f, xkj, ykj, xlj, ylj);

                for (i = (int) i_min; i <= i_max; i++) {
                    xp = indexToCoord(i, x_min, xScale, offset);
                    for (j = (int) j_min; j <= j_max; j++) {
                        yp = indexToCoord(j, y_min, yScale, offset);

                        if (!pointInTriangle(vx, vy, xp, yp)) {
                            continue;
                        }

                        z1_out[i][j] = abc1[0] * xp + abc1[1] * yp + abc1[2];
                        z2_out[i][j] = abc2[0] * xp + abc2[1] * yp + abc2[2];
                        z3_out[i][j] = abc3[0] * xp + abc3[1] * yp + abc3[2];
                        z4_out[i][j] = abc4[0] * xp + abc4[1] * yp + abc4[2];
                    }
                }
            }
        }

        private static double[] getABC(
                final double[] vx, final double[] vy, final double[] vz, final double[][] z_in,
                final double f, final double  xkj, final double ykj, final double xlj, final double ylj) {

            final int i0 = (int)(vz[0]/z_in[0].length);
            final int j0 = (int)(vz[0] - i0*z_in[0].length);
            final double zj = z_in[i0][j0];

            final int i1 = (int)(vz[1]/z_in[1].length);
            final int j1 = (int)(vz[1] - i1*z_in[1].length);
            final double zk = z_in[i1][j1];

            final int i2 = (int)(vz[2]/z_in[2].length);
            final int j2 = (int)(vz[2] - i2*z_in[2].length);
            final double zl = z_in[i2][j2];

            final double zkj = zk - zj;
            final double zlj = zl - zj;

            final double[] abc = new double[3];
            abc[0] = -f * (ykj * zlj - zkj * ylj);
            abc[1] = -f * (zkj * xlj - xkj * zlj);
            abc[2] = -abc[0] * vx[1] - abc[1] * vy[1] + zk;

            return abc;
        }

        private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
            int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
            int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
            int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }

        private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
            return irint((((coord - coord0) / (deltaCoord)) - offset));
        }

        private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
            return (coord0 + idx * deltaCoord + offset);
        }

        private static long irint(final double coord) {
            return ((long) rint(coord));
        }

        private static double rint(final double coord) {
            return Math.floor(coord + 0.5);
        }
    }

    private static class Metadata {
        public MetadataElement absRoot = null;
        public SARGeocoding.Orbit orbit = null;
        public double firstLineTime = 0.0;
        public double lastLineTime = 0.0; // in days
        public double lineTimeInterval = 0.0;
        public double wavelength = 0.0;
        public double rangeSpacing = 0.0;
        public double azimuthSpacing = 0.0;
        public double nearEdgeSlantRange = 0.0; // in m
        public boolean srgrFlag = false;
        public boolean nearRangeOnLeft = false;
        public int sourceImageWidth = 0;
        public int sourceImageHeight = 0;
        public AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    }

	    public static void performDerampDemod(final Tile slaveTileI, final Tile slaveTileQ,
                                          final Rectangle slaveRectangle, final double[][] derampDemodPhase,
                                          final double[][] derampDemodI, final double[][] derampDemodQ) {
		}

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(OrbitBasedCoregistrationOp.class);
        }
    }
}
