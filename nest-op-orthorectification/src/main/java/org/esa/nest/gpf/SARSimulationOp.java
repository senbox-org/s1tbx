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
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.esa.nest.eo.GeoUtils;
import org.esa.nest.eo.LocalGeometry;
import org.esa.nest.eo.SARGeocoding;
import org.jdoris.core.*;
import org.jdoris.core.Point;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

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
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
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
               defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
               label="DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label="External DEM")
    private File externalDEMFile = null;

    @Parameter(label="DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue="false", label="Simulate for SARSimTC")
    boolean isSARSimTC = false;                                    // disable extra options int the UI for SARSimTC

    @Parameter(defaultValue="false", label="Re-grid method (slower)")
    boolean reGridMethod = false;

    @Parameter(defaultValue="false", label="Orbit method")
    boolean orbitMethod = false;

    @Parameter(defaultValue="false", label="Save DEM band")
    private boolean saveDEM = false;

    @Parameter(defaultValue="false", label="Save zero height simulation")
    private boolean saveZeroHeightSimulation = false;

    @Parameter(defaultValue="false", label="Save Simulated Local Incidence Angle")
    private boolean saveLocalIncidenceAngle = false;

    @Parameter(defaultValue="false", label="Save Layover-Shadow Mask")
    private boolean saveLayoverShadowMask = false;

    public final static String demBandName = "elevation";
    public final static String zeroHeightSimulationBandName = "ZeroHeightSimulation";
    public final static String simulatedLocalIncidenceAngleBandName = "Simulated_LocalIncidenceAngle";
    public final static String layoverShadowMaskBandName = "layover_shadow_mask";

    private ElevationModel dem = null;
    private GeoCoding targetGeoCoding = null;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean srgrFlag = false;
    private boolean isElevationModelAvailable = false;

    private double rangeSpacing = 0.0;
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

    private int tileSize = 400;

    private AbstractMetadata.OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

    private static String SIMULATED_BAND_NAME = "Simulated_Intensity";

    private boolean nearRangeOnLeft = true;
    private boolean isPolsar = false;

    private double delLat = 0.0;
    private double delLon = 0.0;

    private SLCImage meta;
    private Orbit orbit;

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

            computeDEMTraversalSampleInterval();

            if(orbitMethod) {
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
                meta = new SLCImage(absRoot);
                orbit = new Orbit(absRoot, 3);
            }
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
        try {
        if(externalDEMFile != null) { // if external DEM file is specified by user

            dem = new FileElevationModel(externalDEMFile, demResamplingMethod, (float)externalDEMNoDataValue);

            demNoDataValue = (float) externalDEMNoDataValue;
            demName = externalDEMFile.getPath();

        } else {
            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
        }
        } catch(Throwable t) {
            t.printStackTrace();
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

        if (saveDEM) {
            targetBand = new Band(demBandName,
                    ProductData.TYPE_FLOAT32,
                    sourceImageWidth,
                    sourceImageHeight);

            targetBand.setUnit(Unit.METERS);
            targetProduct.addBand(targetBand);
        }
        if (saveZeroHeightSimulation) {
            targetBand = new Band(zeroHeightSimulationBandName,
                    ProductData.TYPE_FLOAT32,
                    sourceImageWidth,
                    sourceImageHeight);

            targetBand.setUnit(Unit.INTENSITY);
            targetProduct.addBand(targetBand);
        }
        if (saveLocalIncidenceAngle) {
            targetBand = new Band(simulatedLocalIncidenceAngleBandName,
                    ProductData.TYPE_FLOAT32,
                    sourceImageWidth,
                    sourceImageHeight);

            targetBand.setUnit(Unit.DEGREES);
            targetProduct.addBand(targetBand);
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

    private double computeTileOverlapPercentage(final int x0, final int y0, final int w, final int h) throws Exception {

        final PixelPos pixPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        double altMax = -32768;
        int xMax = 0, yMax = 0;
        boolean foundMax = false;
        for (int y = y0; y < y0 + h; y+=20) {
            for (int x = x0; x < x0 + w; x+=20) {
                pixPos.setLocation(x,y);
                targetGeoCoding.getGeoPos(pixPos, geoPos);
                final double alt = dem.getElevation(geoPos);
                if(alt != demNoDataValue && altMax < alt) {
                    altMax = alt;
                    xMax = x;
                    yMax = y;
                    foundMax = true;
                }
            }
        }

        if (!foundMax) {
            return 0.0;
        }

        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];

        pixPos.setLocation(xMax, yMax);
        targetGeoCoding.getGeoPos(pixPos, geoPos);
        GeoUtils.geo2xyzWGS84(geoPos.getLat(), geoPos.getLon(), altMax, earthPoint);

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                firstLineUTC, lineTimeInterval, wavelength, earthPoint, sensorPosition, sensorVelocity);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return 0.0;
        }

        final double slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime,  timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

        final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

        final int azimuthIndex = (int)((zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval + 0.5);

        double tileOverlapPercentage = (float)(azimuthIndex - yMax)/ (float)tileSize;
        if (tileOverlapPercentage >= 0.0) {
            tileOverlapPercentage += 1.5;
        } else {
            tileOverlapPercentage -= 1.5;
        }
        return tileOverlapPercentage;
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

        double tileOverlapPercentage;
        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }
            tileOverlapPercentage = computeTileOverlapPercentage(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", tileOverlapPercentage = " + tileOverlapPercentage);
        } catch(Exception e) {
            throw new OperatorException(e);
        }

        final Tile targetTile = targetTiles.get(targetProduct.getBand(SIMULATED_BAND_NAME));
        final ProductData masterBuffer = targetTile.getDataBuffer();
        ProductData demBandBuffer = null;
        ProductData zeroHeightBandBuffer = null;
        ProductData localIncidenceAngleBandBuffer = null;
        ProductData layoverShadowMaskBuffer = null;
        if (saveDEM) {
            demBandBuffer = targetTiles.get(targetProduct.getBand(demBandName)).getDataBuffer();
        }
        if (saveZeroHeightSimulation) {
            zeroHeightBandBuffer = targetTiles.get(targetProduct.getBand(zeroHeightSimulationBandName)).getDataBuffer();
        }
        if (saveLocalIncidenceAngle) {
            localIncidenceAngleBandBuffer = targetTiles.get(targetProduct.getBand(simulatedLocalIncidenceAngleBandName)).getDataBuffer();
        }
        if (saveLayoverShadowMask) {
            layoverShadowMaskBuffer = targetTiles.get(targetProduct.getBand(layoverShadowMaskBandName)).getDataBuffer();
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

        final PositionData posData = new PositionData();
        final GeoPos geoPos = new GeoPos();

        double[] slrs = null;
        double[] elev = null;
        int[] index = null;
        final boolean[] savePixel = new boolean[w];
        if (saveLayoverShadowMask) {
            slrs = new double[w];
            elev = new double[w];
            index = new int[w];
        }

        try {
         if(reGridMethod) {
             final double[] latLonMinMax = new double[4];
             computeImageGeoBoundary(x0, xmax, ymin, ymax, latLonMinMax);

             final double latMin = latLonMinMax[0];
             final double latMax = latLonMinMax[1];
             final double lonMin = latLonMinMax[2];
             final double lonMax = latLonMinMax[3];
             final int nLat = (int)((latMax - latMin)/delLat) + 1;
             final int nLon = (int)((lonMax - lonMin)/delLon) + 1;

             final double[][] tileDEM = new double[nLat+1][nLon+1];
             final double[][] neighbourDEM = new double[3][3];
             double alt;

             for (int i = 0; i < nLat; i++) {
                final double lat = latMin + i*delLat;
                for (int j = 0; j < nLon; j++) {
                    double lon = lonMin + j*delLon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }
                    if(saveZeroHeightSimulation) {
                        alt = 1;
                    } else {
                        geoPos.setLocation(lat, lon);
                        alt = dem.getElevation(geoPos);
                        if(alt == demNoDataValue)
                            continue;
                    }
                    tileDEM[i][j] = alt;

                    if(!getPosition(lat, lon, alt, x0, y0, w, h, posData))
                        continue;

                    final LocalGeometry localGeometry = new LocalGeometry(lat, lon, delLat, delLon, posData.earthPoint, posData.sensorPos);

                    final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                            SARGeocoding.NonValidIncidenceAngle};

                    int r = 0;
                    for(int ii=Math.max(0,i-1); ii<=i+1; ++ii) {
                        ii = Math.min(nLat, ii);
                        int c = 0;
                        float neighbourLat = (float)(latMin + ii*delLat);
                        for(int jj=Math.max(0,j-1); jj<=j+1; ++jj) {
                            jj = Math.min(nLon, jj);
                            neighbourDEM[r][c] = tileDEM[ii][jj];
                            if(neighbourDEM[r][c] == 0) {
                                if(saveZeroHeightSimulation) {
                                    neighbourDEM[r][c] = 1;
                                } else {
                                    geoPos.setLocation(neighbourLat, (float)(lonMin + jj*delLon));
                                    neighbourDEM[r][c] = dem.getElevation(geoPos);
                                }
                                tileDEM[ii][jj] = neighbourDEM[r][c];
                            }
                            ++c;
                        }
                        ++r;
                    }

                    SARGeocoding.computeLocalIncidenceAngle(
                            localGeometry, demNoDataValue, false, true, false, 0, 0, 0, 0, neighbourDEM,
                            localIncidenceAngles); // in degrees

                    if (localIncidenceAngles[1] == SARGeocoding.NonValidIncidenceAngle) {
                        continue;
                    }

                    final double v = computeBackscatteredPower(localIncidenceAngles[1]);

                    saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, v, x0, y0, w, h, targetTile, masterBuffer);

                    int idx = 0;
                    if(saveDEM || saveLocalIncidenceAngle)
                        idx = targetTile.getDataBufferIndex((int)posData.rangeIndex, (int)posData.azimuthIndex);

                    if(saveDEM && idx >= 0) {
                        demBandBuffer.setElemDoubleAt(idx, alt);
                    }
                    if(saveZeroHeightSimulation) {
                        saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, 1, x0, y0, w, h, targetTile, zeroHeightBandBuffer);
                    }
                    if(saveLocalIncidenceAngle && idx >= 0) {
                        localIncidenceAngleBandBuffer.setElemDoubleAt(idx, localIncidenceAngles[1]);
                    }

                    if (saveLayoverShadowMask) {
                        int rIndex = (int)posData.rangeIndex;
                        int aIndex = (int)posData.azimuthIndex;
                        index[rIndex] = targetTile.getDataBufferIndex(rIndex, aIndex);
                        if(index[rIndex] < 0) {
                            savePixel[rIndex] = false;
                        } else {
                            slrs[rIndex] = posData.slantRange;
                            elev[rIndex] = computeElevationAngle(posData.slantRange, posData.earthPoint, posData.sensorPos);
                            savePixel[rIndex] = true;
                        }
                    }
                }

                 if (saveLayoverShadowMask) {
                     computeLayoverShadow(savePixel, slrs, index, elev, layoverShadowMaskBuffer);
                }
            }
         } else {
             final double[][] localDEM = new double[ymax-ymin+2][w+2];
             final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0, ymin, w, ymax-ymin);

             if(saveZeroHeightSimulation) {
                 for (double[] aLocalDEM : localDEM) {
                     Arrays.fill(aLocalDEM, 1);
                 }
             } else {

                final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, tileGeoRef, x0, ymin, w, ymax-ymin, localDEM);
                if(!valid)
                    return;
             }

             for (int y = ymin; y < ymax; y++) {
                final int yy = y - ymin;

                for (int x = x0; x < xmax; x++) {
                    final int xx = x - x0;
                    double alt = localDEM[yy][xx];

                    if (alt == demNoDataValue)
                        continue;

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    if(!geoPos.isValid())
                        continue;

                    double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    if(orbitMethod) {
                        double[] latlon = orbit.lp2ell(new Point(x+0.5, y+0.5), meta);
                        lat = latlon[0] * MathUtils.RTOD;
                        lon = latlon[1] * MathUtils.RTOD;
                        alt = dem.getElevation(new GeoPos(lat, lon));
                    }

                    if(!getPosition(lat, lon, alt, x0, y0, w, h, posData))
                        continue;

                    final LocalGeometry localGeometry = new LocalGeometry(x, y, tileGeoRef, posData.earthPoint, posData.sensorPos);

                    final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                            SARGeocoding.NonValidIncidenceAngle};

                    SARGeocoding.computeLocalIncidenceAngle(
                            localGeometry, demNoDataValue, false, true, false, x0, ymin, x, y, localDEM,
                            localIncidenceAngles); // in degrees

                    if (localIncidenceAngles[1] == SARGeocoding.NonValidIncidenceAngle)
                        continue;

                    final double v = computeBackscatteredPower(localIncidenceAngles[1]);

                    saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, v, x0, y0, w, h, targetTile, masterBuffer);

                    int idx = 0;
                    if(saveDEM || saveLocalIncidenceAngle)
                        idx = targetTile.getDataBufferIndex((int)posData.rangeIndex, (int)posData.azimuthIndex);

                    if(saveDEM && idx >= 0) {
                        demBandBuffer.setElemDoubleAt(idx, alt);
                    }
                    if(saveZeroHeightSimulation) {
                        saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, 1, x0, y0, w, h, targetTile, zeroHeightBandBuffer);
                    }
                    if(saveLocalIncidenceAngle && idx >= 0) {
                        localIncidenceAngleBandBuffer.setElemDoubleAt(idx, localIncidenceAngles[1]);
                    }

                    if (saveLayoverShadowMask) {
                        int rIndex = (int)posData.rangeIndex;
                        int aIndex = (int)posData.azimuthIndex;
                        index[xx] = targetTile.getDataBufferIndex(rIndex, aIndex);
                        if(index[xx] < 0) {
                            savePixel[xx] = false;
                        } else {
                            slrs[xx] = posData.slantRange;
                            elev[xx] = computeElevationAngle(posData.slantRange, posData.earthPoint, posData.sensorPos);
                            savePixel[xx] = true;
                        }
                    }
                }

                if (saveLayoverShadowMask) {
                    computeLayoverShadow(savePixel, slrs, index, elev, layoverShadowMaskBuffer);
                }

            }
         }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static class PositionData {
        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];
        double azimuthIndex;
        double rangeIndex;
        double slantRange;
    }

    private boolean getPositionFromOrbit(final double lat, final double lon, final double alt,
                                         final int x0, final int y0, final int w, final int h,
                                         final PositionData data) {

        double[] phi_lam_height = {lat* org.esa.beam.util.math.MathUtils.DTOR, lon* org.esa.beam.util.math.MathUtils.DTOR, alt};
        Point pointOnDem = Ellipsoid.ell2xyz(phi_lam_height);
        //Point slaveTime = orbit.xyz2t(pointOnDem, meta);

        Point linePixel = orbit.xyz2lp(pointOnDem, meta);

        //data.earthPoint[0] = pointOnDem.x;
        //data.earthPoint[1] = pointOnDem.y;
        //data.earthPoint[2] = pointOnDem.z;

        data.azimuthIndex = linePixel.y;
        data.rangeIndex = linePixel.x;

        if(!(data.azimuthIndex > y0-1 && data.azimuthIndex < y0+h)) {
            return false;
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

    private boolean getPosition(final double lat, final double lon, final double alt,
                                final int x0, final int y0, final int w, final int h,
                                final PositionData data) {
        //if(orbitMethod)
        //    return getPositionFromOrbit(lat, lon, alt, x0, y0, w, h, data);

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTimeNewton(
                firstLineUTC, lineTimeInterval, wavelength, data.earthPoint,
                sensorPosition, sensorVelocity);

        //final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
        //        firstLineUTC, lineTimeInterval, wavelength, data.earthPoint,
        //        sensorPosition, sensorVelocity);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime, timeArray, xPosArray, yPosArray, zPosArray, data.earthPoint, data.sensorPos);

        final double zeroDopplerTimeWithoutBias =
                zeroDopplerTime + data.slantRange / Constants.lightSpeedInMetersPerDay;

        data.azimuthIndex = (zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval;

        if(!(data.azimuthIndex > y0-1 && data.azimuthIndex <= y0+h)) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(zeroDopplerTimeWithoutBias,
                timeArray, xPosArray, yPosArray, zPosArray, data.earthPoint, data.sensorPos);

        if(!srgrFlag) {
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

    private static void saveSimulatedData(final double azimuthIndex, final double rangeIndex, double v,
                                          final int x0, final int y0, final int w, final int h, final Tile targetTile,
                                          final ProductData masterBuffer) {
        final int ia0 = (int)(azimuthIndex);
        final int ia1 = ia0 + 1;
        final int ir0 = (int)(rangeIndex);
        final int ir1 = ir0 + 1;

        final double wr = rangeIndex - ir0;
        final double wa = azimuthIndex - ia0;
        final double wac = 1 - wa;

        if (ir0 >= x0) {
            final double wrc = 1 - wr;
            if(ia0 >= y0) {
                final int idx00 = targetTile.getDataBufferIndex(ir0, ia0);
                masterBuffer.setElemDoubleAt(idx00, wrc*wac*v + masterBuffer.getElemDoubleAt(idx00));
              //  masterBuffer.setElemDoubleAt(idx00, 0.25*v + masterBuffer.getElemDoubleAt(idx00));
            }
            if(ia1 < y0+h) {
                final int idx10 = targetTile.getDataBufferIndex(ir0, ia1);
                masterBuffer.setElemDoubleAt(idx10, wrc*wa*v + masterBuffer.getElemDoubleAt(idx10));
            //    masterBuffer.setElemDoubleAt(idx10, 0.25*v + masterBuffer.getElemDoubleAt(idx10));
            }
        }
        if (ir1 < x0+w) {
            if(ia0 >= y0) {
                final int idx01 = targetTile.getDataBufferIndex(ir1, ia0);
                masterBuffer.setElemDoubleAt(idx01, wr*wac*v + masterBuffer.getElemDoubleAt(idx01));
            //    masterBuffer.setElemDoubleAt(idx01, 0.25*v + masterBuffer.getElemDoubleAt(idx01));
            }
            if(ia1 < y0+h) {
                final int idx11 = targetTile.getDataBufferIndex(ir1, ia1);
                masterBuffer.setElemDoubleAt(idx11, wr*wa*v + masterBuffer.getElemDoubleAt(idx11));
            //    masterBuffer.setElemDoubleAt(idx11, 0.25*v + masterBuffer.getElemDoubleAt(idx11));
            }
        }
    }

    private void computeLayoverShadow(final boolean[] savePixel, final double[] slrs, final int[] index, final double[] elev,
                                      final ProductData layoverShadowMaskBuffer) {
        final int length = savePixel.length;
        try {
        if (nearRangeOnLeft) {

            // traverse from near range to far range to detect layover area
            double maxSlantRange = 0.0;
            for (int i=0; i < length; ++i) {
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
            for (int i=length-1; i >= 0; --i) {
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
            for (int i=0; i < length; ++i) {
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
            for (int i=length-1; i >= 0; --i) {
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
            for (int i=0; i < length; ++i) {
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
            for (int i=length-1; i >= 0; --i) {
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
        } catch(Exception e) {
            e.printStackTrace();
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
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xmin, final int xmax, final int ymin, final int ymax,
                                         double[] latLonMinMax) throws Exception {

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if(geoCoding == null) {
            throw new OperatorException("Product does not contain a geocoding");
        }
        final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(xmin, ymin), null);
        final GeoPos geoPosFirstFar  = geoCoding.getGeoPos(new PixelPos(xmax, ymin), null);
        final GeoPos geoPosLastNear  = geoCoding.getGeoPos(new PixelPos(xmin, ymax), null);
        final GeoPos geoPosLastFar   = geoCoding.getGeoPos(new PixelPos(xmax, ymax), null);

        final double[] lats  = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
        final double[] lons  = {geoPosFirstNear.getLon(), geoPosFirstFar.getLon(), geoPosLastNear.getLon(), geoPosLastFar.getLon()};
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
     * @throws Exception The exceptions.
     */
    private void computeDEMTraversalSampleInterval() throws Exception {

        double[] latLonMinMax = new double[4];
        computeImageGeoBoundary(0, sourceProduct.getSceneRasterWidth()-1, 0, sourceProduct.getSceneRasterHeight()-1,
                                latLonMinMax);

        final double groundRangeSpacing = SARGeocoding.getRangePixelSpacing(sourceProduct);
        final double azimuthPixelSpacing = SARGeocoding.getAzimuthPixelSpacing(sourceProduct);
        final double spacing = Math.min(groundRangeSpacing, azimuthPixelSpacing);
        //final double spacing = (groundRangeSpacing + azimuthPixelSpacing)/2.0;
        final double latMin = latLonMinMax[0];
        final double latMax = latLonMinMax[1];
        double minAbsLat;
        if (latMin*latMax > 0) {
            minAbsLat = Math.min(Math.abs(latMin), Math.abs(latMax)) * org.esa.beam.util.math.MathUtils.DTOR;
        } else {
            minAbsLat = 0.0;
        }
        delLat = spacing / Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.RTOD;
        delLon = spacing / (Constants.MeanEarthRadius*Math.cos(minAbsLat)) * org.esa.beam.util.math.MathUtils.RTOD;
        delLat = Math.min(delLat, delLon); // (delLat + delLon)/2.0;
        delLon = delLat;
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
