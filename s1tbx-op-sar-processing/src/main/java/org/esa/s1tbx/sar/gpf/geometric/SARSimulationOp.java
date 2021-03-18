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
package org.esa.s1tbx.sar.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.s1tbx.insar.gpf.support.SARPosition;
import org.esa.s1tbx.commons.SARUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.eo.LocalGeometry;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.jlinda.core.Ellipsoid;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

/**
 * This operator generates simulated SAR image using DEM, the Geocoding and orbit state vectors from a given
 * SAR image, and mathematical modeling of SAR imaging geometry. The simulated SAR image will have the same
 * dimension and resolution as the original SAR image.
 * <p/>
 * The simulation algorithm first create a DEM image from the original SAR image. The DEM image has the same
 * dimension as the original SAR image. The value of each pixel in the DEM image is the elevation of the same
 * pixel in the original SAR image. Then, for each cell in the DEM image, its corresponding pixel position
 * (row/column indices) in the simulated SAR image is computed based on the SAR model. Finally, the backscattered
 * power for the pixel is computed using backscattering model.
 * <p/>
 * Detailed procedure is as the follows:
 * 1. Get the following parameters from the metadata of the SAR image product:
 * (1.1) radar wave length
 * (1.2) range spacing
 * (1.3) first_line_time
 * (1.4) line_time_interval
 * (1.5) slant range to 1st pixel
 * (1.6) orbit state vectors
 * (1.7) slant range to ground range conversion coefficients
 * <p/>
 * 2. Compute satellite position and velocity for each azimuth time by interpolating the state vectors;
 * <p/>
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

@OperatorMetadata(alias = "SAR-Simulation",
        category = "Radar/Geometric/Terrain Correction",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Rigorous SAR Simulation")
public final class SARSimulationOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

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

    @Parameter(label = "External DEM Apply EGM", defaultValue = "true")
    private Boolean externalDEMApplyEGM = true;

    //@Parameter(defaultValue="false", label="Simulate for SARSimTC")
    boolean isSARSimTC = true;                                    // disable extra options int the UI for SARSimTC

    //@Parameter(defaultValue="false", label="Re-grid method (slower)")
    boolean reGridMethod = false;

    //@Parameter(defaultValue="false", label="Orbit method")
    boolean orbitMethod = false;

    //@Parameter(defaultValue="false", label="Save DEM band")
    private boolean saveDEM = false;

    //@Parameter(defaultValue="false", label="Save zero height simulation")
    private boolean saveZeroHeightSimulation = false;

    //@Parameter(defaultValue="false", label="Save Simulated Local Incidence Angle")
    private boolean saveLocalIncidenceAngle = false;

    @Parameter(defaultValue = "false", label = "Save Layover-Shadow Mask")
    private boolean saveLayoverShadowMask = false;

    public final static String demBandName = "elevation";
    public final static String zeroHeightSimulationBandName = "ZeroHeightSimulation";
    public final static String simulatedLocalIncidenceAngleBandName = "Simulated_LocalIncidenceAngle";
    public final static String layoverShadowMaskBandName = "layover_shadow_mask";

    private MetadataElement absRoot = null;
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
    private double demNoDataValue = 0; // no data value for DEM
    private OrbitStateVectors orbit = null;

    private OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

    private static String SIMULATED_BAND_NAME = "Simulated_Intensity";
    public static final String externalDEMStr = "External DEM";

    private boolean nearRangeOnLeft = true;
    private boolean isPolsar = false;

    private double delLat = 0.0;
    private double delLon = 0.0;

    private SLCImage meta = null;
    private Orbit jOrbit = null;

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
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkIfMapProjected(false);

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getSourceImageDimension();

            getMetadata();

            computeSensorPositionsAndVelocities();

            createTargetProduct();

            if (demName.contains(externalDEMStr) && externalDEMFile == null) {
                throw new OperatorException("External DEM file is not specified. ");
            }

            if (!demName.contains(externalDEMStr)) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);

            computeDEMTraversalSampleInterval();

            if (orbitMethod) {
                meta = new SLCImage(absRoot, sourceProduct);
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
        wavelength = SARUtils.getRadarWavelength(absRoot);
        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        firstLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days
        lastLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days
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
     * Compute sensor position and velocity for each range line.
     */
    private void computeSensorPositionsAndVelocities() {

        orbit = new OrbitStateVectors(orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);
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
            if (demName.contains(externalDEMStr)) { // if external DEM file is specified by user

                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                ((FileElevationModel)dem).applyEarthGravitionalModel(externalDEMApplyEGM);
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

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(absTgt != null) {
            if (externalDEMFile != null) { // if external DEM file is specified by user
                AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
            } else {
                AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
            }

            absTgt.setAttributeString("DEM resampling method", demResamplingMethod);

            if (externalDEMFile != null) {
                absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
            }
        }

        targetGeoCoding = targetProduct.getSceneGeoCoding();
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
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            sourceBandNames = sourceProduct.getBandNames();
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

    private OverlapPercentage computeTileOverlapPercentage(final int x0, final int y0, final int w, final int h)
            throws Exception {

        final PixelPos pixPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();

        final int xMin = Math.max(x0 - w/2, 0);
        final int xMax = Math.min(x0 + w + w/2, sourceImageWidth);
        final int yMin = Math.max(y0 - h/2, 0);
        final int yMax = Math.min(y0 + h + h/2, sourceImageHeight);

        double tileOverlapUp = 0.0, tileOverlapDown = 0.0, tileOverlapLeft = 0.0, tileOverlapRight = 0.0;
        for (int y = yMin; y < yMax; y += 20) {
            for (int x = xMin; x < xMax; x += 20) {
                pixPos.setLocation(x, y);
                targetGeoCoding.getGeoPos(pixPos, geoPos);
                final double alt = dem.getElevation(geoPos);
                GeoUtils.geo2xyzWGS84(geoPos.getLat(), geoPos.getLon(), alt, earthPoint);

                final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                        firstLineUTC, lineTimeInterval, wavelength, earthPoint, orbit.sensorPosition, orbit.sensorVelocity);

                if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
                    continue;
                }

                double slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, earthPoint, sensorPos);

                final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

                final int azimuthIndex = (int) ((zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval + 0.5);

                slantRange = SARGeocoding.computeSlantRange(zeroDopplerTimeWithoutBias, orbit, earthPoint, sensorPos);

                double rangeIndex;
                if (!srgrFlag) {
                    rangeIndex = (slantRange - nearEdgeSlantRange) / rangeSpacing;
                } else {
                    rangeIndex = SARGeocoding.computeRangeIndex(
                            srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                            zeroDopplerTimeWithoutBias, slantRange, nearEdgeSlantRange, srgrConvParams);
                }

                final double azTileOverlapPercentage = (azimuthIndex - y) / (double) h;
                if (azTileOverlapPercentage > tileOverlapUp) {
                    tileOverlapUp = azTileOverlapPercentage;
                } else if (azTileOverlapPercentage < -tileOverlapDown) {
                    tileOverlapDown = -azTileOverlapPercentage;
                }

                final double rgTileOverlapPercentage = (rangeIndex - x) / (double) w;
                if (rangeIndex != -1) {
                    if (rgTileOverlapPercentage > tileOverlapLeft) {
                        tileOverlapLeft = rgTileOverlapPercentage;
                    } else if (rgTileOverlapPercentage < -tileOverlapRight) {
                        tileOverlapRight = -rgTileOverlapPercentage;
                    }
                }
            }
        }

        tileOverlapUp += 0.1;
        tileOverlapDown += 0.1;
        tileOverlapLeft += 0.1;
        tileOverlapRight += 0.1;

        return new OverlapPercentage(tileOverlapUp, tileOverlapDown, tileOverlapLeft, tileOverlapRight);
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

        OverlapPercentage tileOverlapPercentage = null;
        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            tileOverlapPercentage = computeTileOverlapPercentage(x0, y0, w, h);
        } catch (Exception e) {
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

        final int ymin = Math.max(y0 - (int) (h * tileOverlapPercentage.tileOverlapUp), 0);
        final int ymax = Math.min(y0 + h + (int) (h * tileOverlapPercentage.tileOverlapDown), sourceImageHeight);
        final int xmin = Math.max(x0 - (int) (w * tileOverlapPercentage.tileOverlapLeft), 0);
        final int xmax = Math.min(x0 + w + (int) (w * tileOverlapPercentage.tileOverlapRight), sourceImageWidth);

        final SARPosition sarPosition = new SARPosition(
                firstLineUTC,
                lastLineUTC,
                lineTimeInterval,
                wavelength,
                rangeSpacing,
                sourceImageWidth,
                srgrFlag,
                nearEdgeSlantRange,
                nearRangeOnLeft,
                orbit,
                srgrConvParams
        );
        sarPosition.setTileConstraints(x0, y0, w, h);
        final SARPosition.PositionData posData = new SARPosition.PositionData();
        final GeoPos geoPos = new GeoPos();

        double[] slrs = null;
        double[] elev = null;
        double[] azIndex = null;
        double[] rgIndex = null;
        boolean[] savePixel = null;

        try {
            if (reGridMethod) {
                final double[] latLonMinMax = new double[4];
                computeImageGeoBoundary(xmin, xmax, ymin, ymax, latLonMinMax);

                final double latMin = latLonMinMax[0];
                final double latMax = latLonMinMax[1];
                final double lonMin = latLonMinMax[2];
                final double lonMax = latLonMinMax[3];
                final int nLat = (int) ((latMax - latMin) / delLat) + 1;
                final int nLon = (int) ((lonMax - lonMin) / delLon) + 1;

                final double[][] tileDEM = new double[nLat + 1][nLon + 1];
                final double[][] neighbourDEM = new double[3][3];
                Double alt;

                if (saveLayoverShadowMask) {
                    slrs = new double[nLon];
                    elev = new double[nLon];
                    azIndex = new double[nLon];
                    rgIndex = new double[nLon];
                    savePixel = new boolean[nLon];
                }

                for (int i = 0; i < nLat; i++) {
                    final double lat = latMin + i * delLat;

                    if (saveLayoverShadowMask) {
                        Arrays.fill(slrs, 0.0);
                        Arrays.fill(elev, 0.0);
                        Arrays.fill(azIndex, 0.0);
                        Arrays.fill(rgIndex, 0.0);
                        Arrays.fill(savePixel, Boolean.FALSE);
                    }

                    for (int j = 0; j < nLon; j++) {
                        double lon = lonMin + j * delLon;
                        if (lon >= 180.0) {
                            lon -= 360.0;
                        }
                        if (saveZeroHeightSimulation) {
                            alt = 1.0;
                        } else {
                            geoPos.setLocation(lat, lon);
                            alt = dem.getElevation(geoPos);
                            if (alt.equals(demNoDataValue))
                                continue;
                        }
                        tileDEM[i][j] = alt;

                        GeoUtils.geo2xyzWGS84(lat, lon, alt, posData.earthPoint);
                        if (!sarPosition.getPosition(posData))
                            continue;

                        final LocalGeometry localGeometry = new LocalGeometry(
                                lat, lon, delLat, delLon, posData.earthPoint, posData.sensorPos);

                        final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                                SARGeocoding.NonValidIncidenceAngle};

                        int r = 0;
                        for (int ii = Math.max(0, i - 1); ii <= i + 1; ++ii) {
                            ii = Math.min(nLat, ii);
                            int c = 0;
                            double neighbourLat = latMin + ii * delLat;
                            for (int jj = Math.max(0, j - 1); jj <= j + 1; ++jj) {
                                jj = Math.min(nLon, jj);
                                neighbourDEM[r][c] = tileDEM[ii][jj];
                                if (neighbourDEM[r][c] == 0) {
                                    if (saveZeroHeightSimulation) {
                                        neighbourDEM[r][c] = 1;
                                    } else {
                                        geoPos.setLocation(neighbourLat, lonMin + jj * delLon);
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

                        saveSimulatedData(
                                posData.azimuthIndex, posData.rangeIndex, v, x0, y0, w, h, targetTile, masterBuffer);

                        int idx = 0;
                        if (saveDEM || saveLocalIncidenceAngle)
                            idx = targetTile.getDataBufferIndex((int) posData.rangeIndex, (int) posData.azimuthIndex);

                        if (saveDEM && idx >= 0) {
                            demBandBuffer.setElemDoubleAt(idx, alt);
                        }
                        if (saveZeroHeightSimulation) {
                            saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, 1, x0, y0, w, h, targetTile,
                                    zeroHeightBandBuffer);
                        }
                        if (saveLocalIncidenceAngle && idx >= 0) {
                            localIncidenceAngleBandBuffer.setElemDoubleAt(idx, localIncidenceAngles[1]);
                        }

                        if (saveLayoverShadowMask) {
                            int rIndex = (int) posData.rangeIndex;
                            int aIndex = (int) posData.azimuthIndex;
                            if (rIndex >= x0 && rIndex < x0 + w && aIndex >= y0 && aIndex < y0 + h) {
                                azIndex[j] = posData.azimuthIndex;
                                rgIndex[j] = posData.rangeIndex;
                                slrs[j] = posData.slantRange;
                                elev[j] = computeElevationAngle(
                                        posData.slantRange, posData.earthPoint, posData.sensorPos);
                                savePixel[j] = true;
                            } else {
                                savePixel[j] = false;
                            }
                        }
                    }

                    if (saveLayoverShadowMask) {
                        computeLayoverShadow(x0, y0, w, h, savePixel, slrs, elev, azIndex, rgIndex, targetTile, layoverShadowMaskBuffer);
                    }
                }

            } else {

                final int widthExt = xmax - xmin;
                final int heightExt = ymax - ymin;
                if (saveLayoverShadowMask) {
                    slrs = new double[widthExt];
                    elev = new double[widthExt];
                    azIndex = new double[widthExt];
                    rgIndex = new double[widthExt];
                    savePixel = new boolean[widthExt];
                }

                final double[][] localDEM = new double[heightExt + 2][widthExt + 2];
                final TileGeoreferencing tileGeoRef = new TileGeoreferencing(
                        targetProduct, xmin, ymin, widthExt, heightExt);

                if (saveZeroHeightSimulation) {
                    for (double[] aLocalDEM : localDEM) {
                        Arrays.fill(aLocalDEM, 1);
                    }
                } else {

                    final boolean valid = DEMFactory.getLocalDEM(
                            dem, demNoDataValue, demResamplingMethod, tileGeoRef, xmin, ymin, widthExt, heightExt,
                            sourceProduct, true, localDEM);

                    if (!valid)
                        return;
                }

                for (int y = ymin; y < ymax; y++) {
                    final int yy = y - ymin;

                    if (saveLayoverShadowMask) {
                        Arrays.fill(slrs, 0.0);
                        Arrays.fill(elev, 0.0);
                        Arrays.fill(azIndex, 0.0);
                        Arrays.fill(rgIndex, 0.0);
                        Arrays.fill(savePixel, Boolean.FALSE);
                    }

                    for (int x = xmin; x < xmax; x++) {
                        final int xx = x - xmin;
                        Double alt = localDEM[yy + 1][xx + 1];

                        if (alt.equals(demNoDataValue))
                            continue;

                        tileGeoRef.getGeoPos(x, y, geoPos);
                        if (!geoPos.isValid())
                            continue;

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

                        GeoUtils.geo2xyzWGS84(lat, lon, alt, posData.earthPoint);
                        if (!sarPosition.getPosition(posData))
                            continue;

                        final LocalGeometry localGeometry = new LocalGeometry(
                                x, y, tileGeoRef, posData.earthPoint, posData.sensorPos);

                        final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                                SARGeocoding.NonValidIncidenceAngle};

                        SARGeocoding.computeLocalIncidenceAngle(
                                localGeometry, demNoDataValue, false, true, false, xmin, ymin, x, y, localDEM,
                                localIncidenceAngles); // in degrees

                        if (localIncidenceAngles[1] == SARGeocoding.NonValidIncidenceAngle)
                            continue;

                        final double v = computeBackscatteredPower(localIncidenceAngles[1]);

                        saveSimulatedData(
                                posData.azimuthIndex, posData.rangeIndex, v, x0, y0, w, h, targetTile, masterBuffer);

                        int idx = 0;
                        if (saveDEM || saveLocalIncidenceAngle)
                            idx = targetTile.getDataBufferIndex((int) posData.rangeIndex, (int) posData.azimuthIndex);

                        if (saveDEM && idx >= 0) {
                            demBandBuffer.setElemDoubleAt(idx, alt);
                        }
                        if (saveZeroHeightSimulation) {
                            saveSimulatedData(posData.azimuthIndex, posData.rangeIndex, 1, x0, y0, w, h, targetTile,
                                    zeroHeightBandBuffer);
                        }
                        if (saveLocalIncidenceAngle && idx >= 0) {
                            localIncidenceAngleBandBuffer.setElemDoubleAt(idx, localIncidenceAngles[1]);
                        }

                        if (saveLayoverShadowMask) {
                            int rIndex = (int) posData.rangeIndex;
                            int aIndex = (int) posData.azimuthIndex;
                            if (rIndex >= x0 && rIndex < x0 + w && aIndex >= y0 && aIndex < y0 + h) {
                                azIndex[xx] = posData.azimuthIndex;
                                rgIndex[xx] = posData.rangeIndex;
                                slrs[xx] = posData.slantRange;
                                elev[xx] = computeElevationAngle(
                                        posData.slantRange, posData.earthPoint, posData.sensorPos);
                                savePixel[xx] = true;
                            } else {
                                savePixel[xx] = false;
                            }
                        }
                    }

                    if (saveLayoverShadowMask) {
                        computeLayoverShadow(x0, y0, w, h, savePixel, slrs, elev, azIndex, rgIndex, targetTile, layoverShadowMaskBuffer);
                    }

                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private boolean getPositionFromOrbit(final double lat, final double lon, final double alt,
                                         final int x0, final int y0, final int w, final int h,
                                         final SARPosition.PositionData data) {

        double[] phi_lam_height = {lat * Constants.DTOR, lon * Constants.DTOR, alt};
        Point pointOnDem = Ellipsoid.ell2xyz(phi_lam_height);
        //Point slaveTime = jOrbit.xyz2t(pointOnDem, meta);

        Point linePixel = jOrbit.xyz2lp(pointOnDem, meta);

        //data.earthPoint[0] = pointOnDem.x;
        //data.earthPoint[1] = pointOnDem.y;
        //data.earthPoint[2] = pointOnDem.z;

        data.azimuthIndex = linePixel.y;
        data.rangeIndex = linePixel.x;

        if (!(data.azimuthIndex > y0 - 1 && data.azimuthIndex < y0 + h)) {
            return false;
        }
        if (data.rangeIndex <= 0.0) {
            return false;
        }

        if (!nearRangeOnLeft) {
            data.rangeIndex = sourceImageWidth - 1 - data.rangeIndex;
        }

        return data.rangeIndex >= x0 && data.rangeIndex < x0 + w;
    }

    private static void saveSimulatedData(final double azimuthIndex, final double rangeIndex, double v,
                                          final int x0, final int y0, final int w, final int h, final Tile targetTile,
                                          final ProductData masterBuffer) {
        final int ia0 = (int) (azimuthIndex);
        final int ia1 = ia0 + 1;
        final int ir0 = (int) (rangeIndex);
        final int ir1 = ir0 + 1;

        final double wr = rangeIndex - ir0;
        final double wa = azimuthIndex - ia0;
        final double wac = 1 - wa;

        if (ir0 >= x0 && ir0 < x0 + w) {
            final double wrc = 1 - wr;
            if (ia0 >= y0 && ia0 < y0 + h) {
                final int idx00 = targetTile.getDataBufferIndex(ir0, ia0);
                masterBuffer.setElemDoubleAt(idx00, wrc * wac * v + masterBuffer.getElemDoubleAt(idx00));
            }
            if (ia1 >= y0 && ia1 < y0 + h) {
                final int idx10 = targetTile.getDataBufferIndex(ir0, ia1);
                masterBuffer.setElemDoubleAt(idx10, wrc * wa * v + masterBuffer.getElemDoubleAt(idx10));
            }
        }
        if (ir1 >= x0 && ir1 < x0 + w) {
            if (ia0 >= y0 && ia0 < y0 + h) {
                final int idx01 = targetTile.getDataBufferIndex(ir1, ia0);
                masterBuffer.setElemDoubleAt(idx01, wr * wac * v + masterBuffer.getElemDoubleAt(idx01));
            }
            if (ia1 >= y0 && ia1 < y0 + h) {
                final int idx11 = targetTile.getDataBufferIndex(ir1, ia1);
                masterBuffer.setElemDoubleAt(idx11, wr * wa * v + masterBuffer.getElemDoubleAt(idx11));
            }
        }
    }

    private void computeLayoverShadow(final int x0, final int y0, final int w, final int h,
                                      final boolean[] savePixel, final double[] slrs, final double[] elev,
                                      final double[] azIndex, final double[] rgIndex, final Tile targetTile,
                                      final ProductData layoverShadowMaskBuffer) {

        final int length = savePixel.length;
        try {
            if (nearRangeOnLeft) {

                // traverse from near range to far range to detect layover area
                double maxSlantRange = 0.0;
                for (int i = 0; i < length; ++i) {
                    if (savePixel[i]) {
                        if (slrs[i] > maxSlantRange) {
                            maxSlantRange = slrs[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 1);
                        }
                    }
                }

                // traverse from far range to near range to detect the remaining layover area
                double minSlantRange = maxSlantRange;
                for (int i = length - 1; i >= 0; --i) {
                    if (savePixel[i]) {
                        if (slrs[i] <= minSlantRange) {
                            minSlantRange = slrs[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 1);
                        }
                    }
                }

                // traverse from near range to far range to detect shadow area
                double maxElevAngle = 0.0;
                for (int i = 0; i < length; ++i) {
                    if (savePixel[i]) {
                        if (elev[i] > maxElevAngle) {
                            maxElevAngle = elev[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 2);
                        }
                    }
                }

            } else {

                // traverse from near range to far range to detect layover area
                double maxSlantRange = 0.0;
                for (int i = length - 1; i >= 0; --i) {
                    if (savePixel[i]) {
                        if (slrs[i] > maxSlantRange) {
                            maxSlantRange = slrs[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 1);
                        }
                    }
                }

                // traverse from far range to near range to detect the remaining layover area
                double minSlantRange = maxSlantRange;
                for (int i = 0; i < length; ++i) {
                    if (savePixel[i]) {
                        if (slrs[i] < minSlantRange) {
                            minSlantRange = slrs[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 1);
                        }
                    }
                }

                // traverse from near range to far range to detect shadow area
                double maxElevAngle = 0.0;
                for (int i = length - 1; i >= 0; --i) {
                    if (savePixel[i]) {
                        if (elev[i] > maxElevAngle) {
                            maxElevAngle = elev[i];
                        } else {
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], targetTile, layoverShadowMaskBuffer, 2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLayoverShadow(final int x0, final int y0, final int w, final int h,
                                   final double rgIndex, final double azIndex, final Tile targetTile,
                                   final ProductData layoverShadowMaskBuffer, final int value) {

        final int xMin = (int)rgIndex;
        final int xMax = Math.min(xMin + 1, x0 + w - 1);
        final int yMin = (int)azIndex;
        final int yMax = Math.min(yMin + 1, y0 + h - 1);
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {
                final int idx = targetTile.getDataBufferIndex(x, y);
                final int v0 = layoverShadowMaskBuffer.getElemIntAt(idx);
                if (v0 == 0) {
                    layoverShadowMaskBuffer.setElemIntAt(idx, value);
                } else if (v0 == 1 && value == 2){
                    layoverShadowMaskBuffer.setElemIntAt(idx, v0 + value);
                }
            }
        }
    }



    /**
     * Compute backscattered power for a given local incidence angle.
     *
     * @param localIncidenceAngle The local incidence angle (in degree).
     * @return The backscattered power.
     */
    private static double computeBackscatteredPower(final double localIncidenceAngle) {
        final double alpha = localIncidenceAngle * Constants.DTOR;
        final double cosAlpha = FastMath.cos(alpha);
        return (0.0118 * cosAlpha / FastMath.pow(FastMath.sin(alpha) + 0.111 * cosAlpha, 3));
    }

    /**
     * Compute elevation angle (in degree).
     *
     * @param slantRange The slant range.
     * @param earthPoint The coordinate for target on earth surface.
     * @param sensorPos  The coordinate for satellite position.
     * @return The elevation angle in degree.
     */
    private static double computeElevationAngle(
            final double slantRange, final PosVector earthPoint, final PosVector sensorPos) {

        final double H2 = sensorPos.x * sensorPos.x + sensorPos.y * sensorPos.y + sensorPos.z * sensorPos.z;
        final double R2 = earthPoint.x * earthPoint.x + earthPoint.y * earthPoint.y + earthPoint.z * earthPoint.z;

        return FastMath.acos((slantRange * slantRange + H2 - R2) / (2 * slantRange * Math.sqrt(H2))) * Constants.RTOD;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int xmin, final int xmax, final int ymin, final int ymax,
                                         double[] latLonMinMax) throws Exception {

        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
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
        computeImageGeoBoundary(0, sourceProduct.getSceneRasterWidth() - 1, 0, sourceProduct.getSceneRasterHeight() - 1,
                latLonMinMax);

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

    private static class OverlapPercentage {
        final double tileOverlapUp;
        final double tileOverlapDown;
        final double tileOverlapLeft;
        final double tileOverlapRight;

        OverlapPercentage(final double tileOverlapUp, final double tileOverlapDown,
                                 final double tileOverlapLeft, final double tileOverlapRight) {
            this.tileOverlapUp = tileOverlapUp;
            this.tileOverlapDown = tileOverlapDown;
            this.tileOverlapLeft = tileOverlapLeft;
            this.tileOverlapRight = tileOverlapRight;
        }
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
            super(SARSimulationOp.class);
        }
    }
}
