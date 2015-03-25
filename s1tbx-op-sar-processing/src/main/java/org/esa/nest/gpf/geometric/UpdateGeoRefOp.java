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
package org.esa.nest.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.PosVector;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileGeoreferencing;
import org.esa.snap.gpf.TileIndex;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This operator updates the Geo reference of the source product. Basically for each given point with
 * latitude and longitude coordinate, it computes its image position in SAR geometry using DEM, orbit
 * state vectors of the source image, and mathematical modeling of SAR imaging geometry. The latitude
 * longitude coordinate is finally assigned to the image point.
 */

@OperatorMetadata(alias = "Update-Geo-Reference",
        category = "SAR Processing/Geometric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Update Geo Reference")
public final class UpdateGeoRefOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME},
            defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue = "false", label = "Re-grid method (slower)")
    boolean reGridMethod = false;

    boolean orbitMethod = false;

    private MetadataElement absRoot = null;
    private ElevationModel dem = null;
    private GeoCoding sourceGeoCoding = null;
    private SLCImage meta = null;
    private Orbit jOrbit = null;

    private int tileSize = 400;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean srgrFlag = false;
    private boolean isElevationModelAvailable = false;
    private boolean nearRangeOnLeft = true;

    private double demNoDataValue = 0; // no data value for DEM
    private double rangeSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lastLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private double nearEdgeSlantRange = 0.0; // in m
    private double wavelength = 0.0; // in m
    private double delLat = 0.0;
    private double delLon = 0.0;

    private SARGeocoding.Orbit orbit = null;

    private OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

    private static double noDataValue = -999.0;
    public static String LATITUDE_BAND_NAME = "lat_band";
    public static String LONGITUDE_BAND_NAME = "lon_band";


    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product}
     * annotated with the {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getSourceImageDimension();

            getMetadata();

            computeSensorPositionsAndVelocities();

            createTargetProduct();

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);

            if (reGridMethod) {
                computeDEMTraversalSampleInterval();
            } else if (orbitMethod) {
                meta = new SLCImage(absRoot);
                jOrbit = new Orbit(absRoot, 3);
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        if (dem != null) {
            dem.dispose();
            dem = null;
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     *
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        wavelength = SARUtils.getRadarFrequency(absRoot);
        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day
        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        } else {
            nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);
    }

    /**
     * Compute sensor position and velocity for each range line.
     */
    private void computeSensorPositionsAndVelocities() {

        orbit = new SARGeocoding.Orbit(orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
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

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if (externalDEMFile != null) { // if external DEM file is specified by user
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
        } else {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        absTgt.setAttributeString("DEM resampling method", demResamplingMethod);

        if (externalDEMFile != null) {
            absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
        }

        sourceGeoCoding = sourceProduct.getGeoCoding();

        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), tileSize);
    }

    private void addSelectedBands() {

        // add selected source bands
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                String unit = band.getUnit();
                if (unit == null || unit.contains(Unit.INTENSITY)) {
                    bandNameList.add(band.getName());
                }
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
            final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
            targetBand.setSourceImage(srcBand.getSourceImage());
        }

        // add latitude and longitude bands
        Band latBand = new Band(LATITUDE_BAND_NAME, ProductData.TYPE_FLOAT64, sourceImageWidth, sourceImageHeight);
        Band lonBand = new Band(LONGITUDE_BAND_NAME, ProductData.TYPE_FLOAT64, sourceImageWidth, sourceImageHeight);
        targetProduct.addBand(latBand);
        targetProduct.addBand(lonBand);

        targetProduct.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 6));
    }

    private void computeTileOverlapPercentage(final int x0, final int y0, final int w, final int h,
                                              double[] overlapPercentages) throws Exception {

        final PixelPos pixPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double tileOverlapPercentageMax = -Double.MAX_VALUE;
        double tileOverlapPercentageMin = Double.MAX_VALUE;
        for (int y = y0; y < y0 + h; y += 20) {
            for (int x = x0; x < x0 + w; x += 20) {
                pixPos.setLocation(x, y);
                sourceGeoCoding.getGeoPos(pixPos, geoPos);
                final double alt = dem.getElevation(geoPos);
                GeoUtils.geo2xyzWGS84(geoPos.getLat(), geoPos.getLon(), alt, earthPoint);

                final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                        firstLineUTC, lineTimeInterval, wavelength, earthPoint, orbit.sensorPosition, orbit.sensorVelocity);

                if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
                    continue;
                }

                final double slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, earthPoint, sensorPos);

                final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

                final int azimuthIndex = (int) ((zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval + 0.5);

                double tileOverlapPercentage = (double) (azimuthIndex - y) / (double) tileSize;

                if (tileOverlapPercentage > tileOverlapPercentageMax) {
                    tileOverlapPercentageMax = tileOverlapPercentage;
                }
                if (tileOverlapPercentage < tileOverlapPercentageMin) {
                    tileOverlapPercentageMin = tileOverlapPercentage;
                }
            }
        }

        if (tileOverlapPercentageMin != Double.MAX_VALUE && tileOverlapPercentageMin < 0.0) {
            overlapPercentages[0] = tileOverlapPercentageMin - 0.5;
        } else {
            overlapPercentages[0] = 0.0;
        }

        if (tileOverlapPercentageMax != -Double.MAX_VALUE && tileOverlapPercentageMax > 0.0) {
            overlapPercentages[1] = tileOverlapPercentageMax + 0.5;
        } else {
            overlapPercentages[1] = 0.0;
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        double[] tileOverlapPercentage = {0.0, 0.0};
        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }
            computeTileOverlapPercentage(x0, y0, w, h, tileOverlapPercentage);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h +
            //                   ", tileOverlapPercentageMin = " + tileOverlapPercentage[0] +
            //                   ", tileOverlapPercentageMax = " + tileOverlapPercentage[1]);
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        final Tile latTile = targetTiles.get(targetProduct.getBand(LATITUDE_BAND_NAME));
        final Tile lonTile = targetTiles.get(targetProduct.getBand(LONGITUDE_BAND_NAME));
        final ProductData latData = latTile.getDataBuffer();
        final ProductData lonData = lonTile.getDataBuffer();
        final double[][] latArray = new double[h][w];
        final double[][] lonArray = new double[h][w];
        for (int r = 0; r < h; r++) {
            Arrays.fill(latArray[r], noDataValue);
            Arrays.fill(lonArray[r], noDataValue);
        }

        final int ymin = Math.max(y0 - (int) (tileSize * tileOverlapPercentage[1]), 0);
        final int ymax = y0 + h + (int) (tileSize * Math.abs(tileOverlapPercentage[0]));
        final int xmax = x0 + w;

        final PositionData posData = new PositionData();
        final GeoPos geoPos = new GeoPos();

        try {
            if (reGridMethod) {
                final double[] latLonMinMax = new double[4];
                computeImageGeoBoundary(x0, xmax, ymin, ymax, latLonMinMax);

                final double latMin = latLonMinMax[0];
                final double latMax = latLonMinMax[1];
                final double lonMin = latLonMinMax[2];
                final double lonMax = latLonMinMax[3];
                final int nLat = (int) ((latMax - latMin) / delLat) + 1;
                final int nLon = (int) ((lonMax - lonMin) / delLon) + 1;

                final double[][] tileDEM = new double[nLat + 1][nLon + 1];
                double alt;

                for (int i = 0; i < nLat; i++) {
                    final double lat = latMin + i * delLat;
                    for (int j = 0; j < nLon; j++) {
                        double lon = lonMin + j * delLon;
                        if (lon >= 180.0) {
                            lon -= 360.0;
                        }
                        geoPos.setLocation(lat, lon);
                        alt = dem.getElevation(geoPos);
                        if (alt == demNoDataValue) {
                            continue;
                        }

                        tileDEM[i][j] = alt;

                        if (!getPosition(lat, lon, alt, x0, y0, w, h, posData)) {
                            continue;
                        }

                        final int ri = (int) Math.round(posData.rangeIndex);
                        final int ai = (int) Math.round(posData.azimuthIndex);
                        if (ri < x0 || ri >= x0 + w || ai < y0 || ai >= y0 + h) {
                            continue;
                        }

                        latArray[ai - y0][ri - x0] = lat;
                        lonArray[ai - y0][ri - x0] = lon;
                    }
                }

            } else {

                final double[][] localDEM = new double[ymax - ymin + 2][w + 2];
                final TileGeoreferencing tileGeoRef = new TileGeoreferencing(sourceProduct, x0, ymin, w, ymax - ymin);

                final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, demResamplingMethod, tileGeoRef,
                        x0, ymin, w, ymax - ymin, sourceProduct, true, localDEM);

                if (!valid) {
                    return;
                }

                for (int y = ymin; y < ymax; y++) {
                    final int yy = y - ymin;

                    for (int x = x0; x < xmax; x++) {
                        final int xx = x - x0;
                        double alt = localDEM[yy + 1][xx + 1];

                        if (alt == demNoDataValue) {
                            continue;
                        }

                        tileGeoRef.getGeoPos(x, y, geoPos);
                        if (!geoPos.isValid()) {
                            continue;
                        }

                        double lat = geoPos.lat;
                        double lon = geoPos.lon;
                        if (lon >= 180.0) {
                            lon -= 360.0;
                        }

                        if (orbitMethod) {
                            double[] latlon = jOrbit.lp2ell(new Point(x + 0.5, y + 0.5), meta);
                            lat = latlon[0] * Constants.RTOD;
                            lon = latlon[1] * Constants.RTOD;
                            alt = dem.getElevation(new GeoPos(lat, lon));
                        }

                        if (!getPosition(lat, lon, alt, x0, y0, w, h, posData)) {
                            continue;
                        }

                        final int ri = (int) Math.round(posData.rangeIndex);
                        final int ai = (int) Math.round(posData.azimuthIndex);
                        if (ri < x0 || ri >= x0 + w || ai < y0 || ai >= y0 + h) {
                            continue;
                        }

                        latArray[ai - y0][ri - x0] = lat;
                        lonArray[ai - y0][ri - x0] = lon;
                    }
                }
            }

            // todo should replace the following code with Delaunay interpolation
            final TileIndex trgIndex = new TileIndex(latTile);
            for (int y = y0; y < y0+h; y++) {
                final int yy = y - y0;
                trgIndex.calculateStride(y);
                for (int x = x0; x < x0+w; x++) {
                    final int xx = x - x0;
                    final int index = trgIndex.getIndex(x);

                    if (latArray[yy][xx] == noDataValue) {
                        latData.setElemDoubleAt(index, fillHole(xx, yy, latArray));
                    } else {
                        latData.setElemDoubleAt(index, latArray[yy][xx]);
                    }

                    if (lonArray[yy][xx] == noDataValue) {
                        lonData.setElemDoubleAt(index, fillHole(xx, yy, lonArray));
                    } else {
                        lonData.setElemDoubleAt(index, lonArray[yy][xx]);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private double fillHole(final int xx, final int yy, final double[][] srcArray) throws Exception {

        try {
            final int h = srcArray.length;
            final int w = srcArray[0].length;

            double vU = noDataValue, vD = noDataValue, vL = noDataValue, vR = noDataValue;
            int yU = -1, yD = -1, xL = -1, xR = -1;
            for (int y = yy; y >= 0; y--) {
                if (srcArray[y][xx] != noDataValue) {
                    vU = srcArray[y][xx];
                    yU = y;
                    break;
                }
            }

            for (int y = yy; y < h; y++) {
                if (srcArray[y][xx] != noDataValue) {
                    if (vU != noDataValue) {
                        vD = srcArray[y][xx];
                        yD = y;
                        break;
                    } else {
                        vU = srcArray[y][xx];
                        yU = y;
                    }
                }
            }

            for (int x = xx; x >= 0; x--) {
                if (srcArray[yy][x] != noDataValue) {
                    vL = srcArray[yy][x];
                    xL = x;
                    break;
                }
            }

            for (int x = xx; x < w; x++) {
                if (srcArray[yy][x] != noDataValue) {
                    if (vL != noDataValue) {
                        vR = srcArray[yy][x];
                        xR = x;
                        break;
                    } else {
                        vL = srcArray[yy][x];
                        xL = x;
                    }
                }
            }

            if (vU != noDataValue && vD != noDataValue && vL != noDataValue && vR != noDataValue) {
                final double vY = vU + (vD - vU) * (yy - yU) / (yD - yU);
                final double vX = vL + (vR - vL) * (xx - xL) / (xR - xL);
                return 0.5*(vY + vX);
            } else if (vU != noDataValue && vD != noDataValue) {
                return vU + (vD - vU) * (yy - yU) / (yD - yU);
            } else if (vL != noDataValue && vR != noDataValue) {
                return vL + (vR - vL) * (xx - xL) / (xR - xL);
            } else if (vL != noDataValue) {
                return vL;
            } else if (vR != noDataValue) {
                return vR;
            } else if (vU != noDataValue) {
                return vU;
            } else if (vD != noDataValue) {
                return vD;
            }

        } catch (Exception e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return noDataValue;
    }

    private static class PositionData {
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
        double slantRange;
    }

    private boolean getPosition(final double lat, final double lon, final double alt,
                                final int x0, final int y0, final int w, final int h,
                                final PositionData data) {

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

            /*final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTimeNewton(
                    firstLineUTC, lineTimeInterval, wavelength, data.earthPoint,
                    orbit.sensorPosition, orbit.sensorVelocity);*/

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTimeNewton(
                firstLineUTC, lineTimeInterval, wavelength, data.earthPoint, orbit);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, data.earthPoint, data.sensorPos);

        final double zeroDopplerTimeWithoutBias =
                zeroDopplerTime + data.slantRange / Constants.lightSpeedInMetersPerDay;

        data.azimuthIndex = (zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval;

        if (!(data.azimuthIndex > y0 - 1 && data.azimuthIndex <= y0 + h)) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTimeWithoutBias, orbit, data.earthPoint, data.sensorPos);

        if (!srgrFlag) {
            data.rangeIndex = (data.slantRange - nearEdgeSlantRange) / rangeSpacing;
        } else {
            data.rangeIndex = SARGeocoding.computeRangeIndex(
                    srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                    zeroDopplerTimeWithoutBias, data.slantRange, nearEdgeSlantRange, srgrConvParams);
        }

        if (data.rangeIndex <= 0.0) {
            return false;
        }

        if (!nearRangeOnLeft) {
            data.rangeIndex = sourceImageWidth - 1 - data.rangeIndex;
        }

        if (!(data.rangeIndex >= x0 && data.rangeIndex < x0 + w)) {
            return false;
        }
        return true;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xmin, final int xmax, final int ymin, final int ymax,
                                         double[] latLonMinMax) throws Exception {

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("Product does not contain a geocoding");
        }
        final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(xmin, ymin), null);
        final GeoPos geoPosFirstFar = geoCoding.getGeoPos(new PixelPos(xmax, ymin), null);
        final GeoPos geoPosLastNear = geoCoding.getGeoPos(new PixelPos(xmin, ymax), null);
        final GeoPos geoPosLastFar = geoCoding.getGeoPos(new PixelPos(xmax, ymax), null);

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
     * Compute DEM traversal step sizes (in degree) in latitude and longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeDEMTraversalSampleInterval() throws Exception {

        double[] latLonMinMax = new double[4];
        computeImageGeoBoundary(0, sourceImageWidth - 1, 0, sourceImageHeight - 1, latLonMinMax);

        final double groundRangeSpacing = SARGeocoding.getRangePixelSpacing(sourceProduct);
        final double azimuthPixelSpacing = SARGeocoding.getAzimuthPixelSpacing(sourceProduct);
        final double spacing = Math.min(groundRangeSpacing, azimuthPixelSpacing);
        //final double spacing = (groundRangeSpacing + azimuthPixelSpacing)/2.0;
        final double latMin = latLonMinMax[0];
        final double latMax = latLonMinMax[1];
        double minAbsLat;
        if (latMin * latMax > 0) {
            minAbsLat = Math.min(Math.abs(latMin), Math.abs(latMax)) * Constants.DTOR;
        } else {
            minAbsLat = 0.0;
        }
        delLat = spacing / Constants.MeanEarthRadius * Constants.RTOD;
        delLon = spacing / (Constants.MeanEarthRadius * FastMath.cos(minAbsLat)) * Constants.RTOD;
        delLat = Math.min(delLat, delLon); // (delLat + delLon)/2.0;
        delLon = delLat;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(UpdateGeoRefOp.class);
        }
    }
}
