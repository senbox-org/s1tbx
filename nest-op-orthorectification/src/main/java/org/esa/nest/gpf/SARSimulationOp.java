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
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
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
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.esa.nest.eo.GeoUtils;
import org.esa.nest.eo.LocalGeometry;
import org.esa.nest.eo.SARGeocoding;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This operator generates simulated SAR image using DEM, the Geocoding and orbit state vectors from a given
 * SAR image, and mathematical modeling of SAR imaging geometry. The simulated SAR image will have the same
 * dimension and resolution as the original SAR image.
 *
 * The simulation algorithm first create a DEM image from the original SAR image. The DEM image has the same
 * dimension as the original SAR image. The value of each pixel in the DEM image is the elevation of the same
 * pixel in the original SAR image. Then, for each cell in the DEM image, its corresponding pixel position
 * (row/column indices) in the simulated SAR image is computed based on the SAR model. Finally, the backscattered
 * power for the pixel is computed using backscattering model.
 *
 * Detailed procedure is as the follows:
 * 1. Get the following parameters from the metadata of the SAR image product:
 * (1.1) radar wave length
 * (1.2) range spacing
 * (1.3) first_line_time
 * (1.4) line_time_interval
 * (1.5) slant range to 1st pixel
 * (1.6) orbit state vectors
 * (1.7) slant range to ground range conversion coefficients
 *
 * 2. Compute satellite position and velocity for each azimuth time by interpolating the state vectors;
 *
 * 3. Repeat the following steps for each cell in the DEM image:
 * (3.1) Get latitude, longitude and elevation for the cell;
 * (3.2) Convert (latitude, longitude, elevation) to Cartesian coordinate P(X, Y, Z);
 * (3.3) Compute zero Doppler time t for point P(x, y, z) using Doppler frequency function;
 * (3.3) Compute SAR sensor position S(X, Y, Z) at time t;
 * (3.4) Compute slant range r = |S - P|;
 * (3.5) Compute bias-corrected zero Doppler time tc = t + r*2/c, where c is the light speed;
 * (3.6) Update satellite position S(tc) and slant range r(tc) = |S(tc) - P| for the bias-corrected zero Doppler time tc;
 * (3.7) Compute azimuth index Ia in the source image using zero Doppler time tc;
 * (3.8) Compute range index Ir in the source image using slant range r(tc);
 * (3.9) Compute local incidence angle;
 * (3.10)Compute backscattered power and save it as value for pixel ((int)Ia, (int)Ir);
 */

@OperatorMetadata(alias="SAR-Simulation",
                  category = "Geometry\\Terrain Correction",
                  description="Rigorous SAR Simulation")
public final class SARSimulationOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
               description = "The digital elevation model.",
               defaultValue="SRTM 3Sec", label="Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                           ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                           ResamplingFactory.CUBIC_CONVOLUTION_NAME,
                           ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
                           ResamplingFactory.BISINC_INTERPOLATION_NAME},
               defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
               label="DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(label="External DEM")
    private File externalDEMFile = null;

    @Parameter(label="DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue="false", label="Save Layover-Shadow Mask as band")
    private boolean saveLayoverShadowMask = false;

    public final static String layoverShadowMaskBandName = "layover_shadow_mask";

    private ElevationModel dem = null;
    private GeoCoding targetGeoCoding = null;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean srgrFlag = false;
    private boolean isElevationModelAvailable = false;
    private boolean overlapComputed = false;

    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lastLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private double nearEdgeSlantRange = 0.0; // in m
    private double wavelength = 0.0; // in m
    private float demNoDataValue = 0; // no data value for DEM
    private double[][] sensorPosition = null; // sensor position for all range lines
    private double[][] sensorVelocity = null; // sensor velocity for all range lines
    private double[] timeArray = null;
    private double[] xPosArray = null;
    private double[] yPosArray = null;
    private double[] zPosArray = null;

    private int tileSize = 100;
    private float tileOverlapPercentage = 0.0f;

    private AbstractMetadata.OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

    private static String SIMULATED_BAND_NAME = "Simulated_Intensity";

    private boolean nearRangeOnLeft = true;
    private boolean isPolsar = false;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if(OperatorUtils.isMapProjected(sourceProduct)) {
                throw new OperatorException("Source product already map projected");
            }

            getSourceImageDimension();

            getMetadata();

            computeSensorPositionsAndVelocities();

            createTargetProduct();

            if(externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);
        } catch(Throwable e) {
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
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        wavelength = OperatorUtils.getRadarFrequency(absRoot);
        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day
        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        } else {
            nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);

        isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;
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
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if(isElevationModelAvailable) return;
        if(externalDEMFile != null) { // if external DEM file is specified by user

            dem = new FileElevationModel(externalDEMFile,
                                         ResamplingFactory.createResampling(demResamplingMethod),
                                         (float)externalDEMNoDataValue);

            demNoDataValue = (float) externalDEMNoDataValue;
            demName = externalDEMFile.getPath();

        } else {
            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
        }
        isElevationModelAvailable = true;
    }

    /**
     * Compute sensor position and velocity for each range line from the orbit state vectors using
     * cubic WARP polynomial.
     */
    private void computeSensorPositionsAndVelocities() {

        final int numVectorsUsed = Math.min(orbitStateVectors.length, 5);
        timeArray = new double[numVectorsUsed];
        xPosArray = new double[numVectorsUsed];
        yPosArray = new double[numVectorsUsed];
        zPosArray = new double[numVectorsUsed];
        sensorPosition = new double[sourceImageHeight][3]; // xPos, yPos, zPos
        sensorVelocity = new double[sourceImageHeight][3]; // xVel, yVel, zVel

        SARGeocoding.computeSensorPositionsAndVelocities(
                orbitStateVectors, timeArray, xPosArray, yPosArray, zPosArray,
                sensorPosition, sensorVelocity, firstLineUTC, lineTimeInterval, sourceImageHeight);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        addSelectedBands();

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if(externalDEMFile != null) { // if external DEM file is specified by user
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
        } else {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        absTgt.setAttributeString("DEM resampling method", demResamplingMethod);

        if(externalDEMFile != null) {
            absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
        }

        targetGeoCoding = targetProduct.getGeoCoding();
        
        // set the tile width to be the image width to reduce tiling effect
        //if (saveLayoverShadowMask) {
            targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), tileSize);
        //}
    }

    private void addSelectedBands() {

        // add simulated band first (which will be the master in GCP selection in SAR Sim TC)
        Band targetBand = new Band(SIMULATED_BAND_NAME,
                                   ProductData.TYPE_FLOAT32,
                                   sourceImageWidth,
                                   sourceImageHeight);

        targetBand.setUnit(Unit.INTENSITY);
        targetProduct.addBand(targetBand);

        // add selected slave bands
        boolean bandSlected = false;
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                String unit = band.getUnit();
                if(unit==null || unit.contains(Unit.INTENSITY)) {
                    bandNameList.add(band.getName());
                }
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
            bandSlected = false;
        } else {
            bandSlected = true;
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
            String unit = srcBand.getUnit();
            if(unit == null) {
                unit = Unit.AMPLITUDE;
            }

            if (!isPolsar && (unit.contains(Unit.IMAGINARY) || unit.contains(Unit.REAL) || unit.contains(Unit.PHASE))) {
                if (bandSlected) {
                    throw new OperatorException("Please select amplitude or intensity band");
                } else {
                    continue;
                }
            }

            targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
            targetBand.setSourceImage(srcBand.getSourceImage());
        }

        // add layover/shadow mask band
        if (saveLayoverShadowMask) {
            targetBand = new Band(layoverShadowMaskBandName,
                                  ProductData.TYPE_INT8,
                                  sourceImageWidth,
                                  sourceImageHeight);

            targetBand.setUnit(Unit.BIT);
            targetProduct.addBand(targetBand);
        }
    }

    private synchronized void computeTileOverlapPercentage(final int h) throws Exception {

        if(overlapComputed) {
            return;
        }

        final int x = sourceImageWidth/2;
        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixPos = new PixelPos();
        tileSize = h;

        int y;
        double alt = 0.0;
        for (y = tileSize - 1; y < sourceImageHeight; y++) {
            pixPos.setLocation(x+0.5f,y+0.5f);
            targetGeoCoding.getGeoPos(pixPos, geoPos);

            alt = dem.getElevation(geoPos);
            if (alt != demNoDataValue) {
                break;
            }
        }

        pixPos.setLocation(x,y);
        targetGeoCoding.getGeoPos(pixPos, geoPos);
        GeoUtils.geo2xyzWGS84(geoPos.getLat(), geoPos.getLon(), alt, earthPoint);

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                firstLineUTC, lineTimeInterval, wavelength, earthPoint, sensorPosition, sensorVelocity);

        final double slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime,  timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

        final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

        final int azimuthIndex = (int)((zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval + 0.5);

        tileOverlapPercentage = (float)(azimuthIndex - y)/ (float)tileSize;
        if (tileOverlapPercentage >= 0.0) {
            tileOverlapPercentage += 0.05;
        } else {
            tileOverlapPercentage -= 0.05;
        }
        overlapComputed = true;
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
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }
            if(!overlapComputed) {
                computeTileOverlapPercentage(h);
            }
        } catch(Exception e) {
            throw new OperatorException(e);
        }

        final Tile targetTile = targetTiles.get(targetProduct.getBand(SIMULATED_BAND_NAME));
        final ProductData masterBuffer = targetTile.getDataBuffer();
        ProductData layoverShadowMaskBuffer = null;
        if (saveLayoverShadowMask) {
            layoverShadowMaskBuffer = targetTiles.get(targetProduct.getBand("layover_shadow_mask")).getDataBuffer();
        }

        int ymin, ymax;
        if (tileOverlapPercentage >= 0.0f) {
            ymin = Math.max(y0 - (int)(tileSize*tileOverlapPercentage), 0);
            ymax = y0 + h;
        } else {
            ymin = y0;
            ymax = y0 + h + (int)(tileSize*Math.abs(tileOverlapPercentage));
        }
        final int xmax = x0 + w;

        final Map<Integer, Double> zeroDopplerTimeMap = new HashMap<Integer, Double>(ymax);

        final float[][] localDEM = new float[ymax-ymin+2][w+2];
        try {
            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0, ymin, w, ymax-ymin);

            final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, tileGeoRef, x0, ymin, w, ymax-ymin, localDEM);
            if(!valid)
                return;

            final GeoPos geoPos = new GeoPos();
            final double[] earthPoint = new double[3];
            final double[] sensorPos = new double[3];
            for (int y = ymin; y < ymax; y++) {
                double[] slrs = null;
                double[] elev = null;
                int[] index = null;
                final boolean[] savePixel = new boolean[w];
                if (saveLayoverShadowMask) {
                    slrs = new double[w];
                    elev = new double[w];
                    index = new int[w];
                }

                for (int x = x0; x < xmax; x++) {
                    final int xx = x - x0;
                    final double alt = localDEM[y-ymin+1][xx+1];

                    if (alt == demNoDataValue) {
                        savePixel[xx] = false;
                        continue;
                    }

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    if(!geoPos.isValid()) {
                        savePixel[xx] = false;
                        continue;  
                    }
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    GeoUtils.geo2xyzWGS84(lat, lon, alt, earthPoint);

                    Double zeroDopplerTime = zeroDopplerTimeMap.get(y);
                    if(zeroDopplerTime == null) {
                        zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                            firstLineUTC, lineTimeInterval, wavelength, earthPoint,
                            sensorPosition, sensorVelocity);
                        zeroDopplerTimeMap.put(y, zeroDopplerTime);
                    }

                 /*   Double zeroDopplerTime = RangeDopplerGeocodingOp.getEarthPointZeroDopplerTime(
                            firstLineUTC, lineTimeInterval, wavelength, earthPoint,
                            sensorPosition, sensorVelocity);     */

                    double slantRange = SARGeocoding.computeSlantRange(
                            zeroDopplerTime, timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

                    final double zeroDopplerTimeWithoutBias =
                            zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

                    double azimuthIndex = (zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval;

                    slantRange = SARGeocoding.computeSlantRange(zeroDopplerTimeWithoutBias,
                            timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

                    double rangeIndex = SARGeocoding.computeRangeIndex(
                            srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                            zeroDopplerTimeWithoutBias, slantRange, nearEdgeSlantRange, srgrConvParams);

                    if (rangeIndex <= 0.0) {
                        continue;
                    }

                    if (!nearRangeOnLeft) {
                        rangeIndex = sourceImageWidth - 1 - rangeIndex;
                    }

                    // fudge
                    //azimuthIndex -= 2;
                    //rangeIndex -= 2;

                    if (!(rangeIndex >= x0 && rangeIndex < x0+w && azimuthIndex > y0-1 && azimuthIndex < y0+h)) {
                        savePixel[xx] = false;
                        continue;   
                    }

                    if (saveLayoverShadowMask) {
                        slrs[xx] = slantRange;
                        elev[xx] = computeElevationAngle(slantRange, earthPoint, sensorPos);
                    }

                    final LocalGeometry localGeometry = new LocalGeometry(x, y, tileGeoRef, earthPoint, sensorPos);

                    final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                            SARGeocoding.NonValidIncidenceAngle};

                    SARGeocoding.computeLocalIncidenceAngle(
                            localGeometry, demNoDataValue, true, true, false, x0, ymin, x, y, localDEM,
                            localIncidenceAngles); // in degrees

                    if (localIncidenceAngles[0] == SARGeocoding.NonValidIncidenceAngle) {
                        savePixel[xx] = false;
                        continue;
                    }

                    final double v = computeBackscatteredPower(localIncidenceAngles[1]);

                    saveSimulatedData(azimuthIndex, rangeIndex, v, x0, y0, w, h, targetTile, masterBuffer);

                    if (saveLayoverShadowMask) {
                        rangeIndex = Math.round(rangeIndex);
                        azimuthIndex = Math.round(azimuthIndex);
                        if (rangeIndex >= x0 && rangeIndex < x0+w && azimuthIndex >= y0 && azimuthIndex < y0+h) {
                            index[xx] = targetTile.getDataBufferIndex((int)rangeIndex, (int)azimuthIndex);
                            savePixel[xx] = true;
                        } else {
                            savePixel[xx] = false;
                        }
                    }
                }

                if (!saveLayoverShadowMask) {
                    continue;
                }

                if (nearRangeOnLeft) {

                    // traverse from near range to far range to detect layover area
                    double maxSlantRange = 0.0;
                    for (int x = x0; x < x0 + w; x++) {
                        final int i = x - x0;
                        if (savePixel[i]) {
                            if (slrs[i] > maxSlantRange) {
                                maxSlantRange = slrs[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i], 1);
                            }
                        }
                    }

                    // traverse from far range to near range to detect the remaining layover area
                    double minSlantRange = maxSlantRange;
                    for (int x = x0 + w - 1; x >= x0; x--) {
                        int i = x - x0;
                        if (savePixel[i]) {
                            if (slrs[i] < minSlantRange) {
                                minSlantRange = slrs[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i], 1);
                            }
                        }
                    }

                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int x = x0; x < x0 + w; x++) {
                        int i = x - x0;
                        if (savePixel[i]) {
                            if (elev[i] > maxElevAngle) {
                                maxElevAngle = elev[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i],
                                                                    2 + layoverShadowMaskBuffer.getElemIntAt(index[i]));
                            }
                        }
                    }

                } else {

                    // traverse from near range to far range to detect layover area
                    double maxSlantRange = 0.0;
                    for (int x = x0 + w - 1; x >= x0; x--) {
                        final int i = x - x0;
                        if (savePixel[i]) {
                            if (slrs[i] > maxSlantRange) {
                                maxSlantRange = slrs[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i], 1);
                            }
                        }
                    }

                    // traverse from far range to near range to detect the remaining layover area
                    double minSlantRange = maxSlantRange;
                    for (int x = x0; x < x0 + w; x++) {
                        int i = x - x0;
                        if (savePixel[i]) {
                            if (slrs[i] < minSlantRange) {
                                minSlantRange = slrs[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i], 1);
                            }
                        }
                    }

                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int x = x0 + w - 1; x >= x0; x--) {
                        int i = x - x0;
                        if (savePixel[i]) {
                            if (elev[i] > maxElevAngle) {
                                maxElevAngle = elev[i];
                            } else {
                                layoverShadowMaskBuffer.setElemIntAt(index[i],
                                                                    2 + layoverShadowMaskBuffer.getElemIntAt(index[i]));
                            }
                        }
                    }
                }

            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static void saveSimulatedData(final double azimuthIndex, final double rangeIndex, final double v,
                                          final int x0, final int y0, final int w, final int h, final Tile targetTile,
                                          final ProductData masterBuffer) {
        final int ia0 = (int)azimuthIndex;
        final int ia1 = ia0 + 1;
        final int ir0 = (int)rangeIndex;
        final int ir1 = ir0 + 1;

        final double wr = rangeIndex - ir0;
        final double wa = azimuthIndex - ia0;
        final double wac = 1 - wa;

        if (ir0 >= x0) {
            final double wrc = 1 - wr;
            if(ia0 >= y0) {
                final int idx00 = targetTile.getDataBufferIndex(ir0, ia0);
                masterBuffer.setElemDoubleAt(idx00, wrc*wac*v + masterBuffer.getElemDoubleAt(idx00));
            }
            if(ia1 < y0+h) {
                final int idx10 = targetTile.getDataBufferIndex(ir0, ia1);
                masterBuffer.setElemDoubleAt(idx10, wrc*wa*v + masterBuffer.getElemDoubleAt(idx10));
            }
        }
        if (ir1 < x0+w) {
            if(ia0 >= y0) {
                final int idx01 = targetTile.getDataBufferIndex(ir1, ia0);
                masterBuffer.setElemDoubleAt(idx01, wr*wac*v + masterBuffer.getElemDoubleAt(idx01));
            }
            if(ia1 < y0+h) {
                final int idx11 = targetTile.getDataBufferIndex(ir1, ia1);
                masterBuffer.setElemDoubleAt(idx11, wr*wa*v + masterBuffer.getElemDoubleAt(idx11));
            }
        }
    }

    /**
     * Compute backscattered power for a given local incidence angle.
     * @param localIncidenceAngle The local incidence angle (in degree).
     * @return The backscattered power.
     */
    private static double computeBackscatteredPower(final double localIncidenceAngle) {
        final double alpha = localIncidenceAngle*org.esa.beam.util.math.MathUtils.DTOR;
        final double cosAlpha = FastMath.cos(alpha);
        return (0.0118*cosAlpha / Math.pow(FastMath.sin(alpha) + 0.111*cosAlpha, 3));
    }

    /**
     * Compute elevation angle (in degree).
     * @param slantRange The slant range.
     * @param earthPoint The coordinate for target on earth surface.
     * @param sensorPos The coordinate for satellite position.
     * @return The elevation angle in degree.
     */
    private static double computeElevationAngle(
            final double slantRange, final double[] earthPoint, final double[] sensorPos) {

        final double H2 = sensorPos[0]*sensorPos[0] + sensorPos[1]*sensorPos[1] + sensorPos[2]*sensorPos[2];
        final double R2 = earthPoint[0]*earthPoint[0] + earthPoint[1]*earthPoint[1] + earthPoint[2]*earthPoint[2];

        return FastMath.acos((slantRange*slantRange + H2 - R2)/(2*slantRange*Math.sqrt(H2)))*
                org.esa.beam.util.math.MathUtils.RTOD;
    }
    

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SARSimulationOp.class);
            setOperatorUI(SARSimulationOpUI.class);
        }
    }
}
