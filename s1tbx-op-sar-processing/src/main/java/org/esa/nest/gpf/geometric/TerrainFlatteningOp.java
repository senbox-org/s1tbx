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
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.nest.dataio.dem.ElevationModelDescriptor;
import org.esa.nest.dataio.dem.ElevationModelRegistry;
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
import org.esa.nest.gpf.InputProductValidator;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.Maths;

import java.awt.*;
import java.io.File;
import java.util.Map;

/**
 * This operator implements the terrain flattening algorithm proposed by
 * David Small. For details, see the paper below and the references therein.
 * David Small, "Flattening Gamma: Radiometric Terrain Correction for SAR imagery",
 * IEEE Transaction on Geoscience and Remote Sensing, Vol. 48, No. 8, August 2011.
 */

@OperatorMetadata(alias = "Terrain-Flattening",
        category = "SAR Processing/Geometric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Terrain Flattening")
public final class TerrainFlatteningOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec",
            label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    private ElevationModel dem = null;
    private FileElevationModel fileElevationModel = null;
    private TiePointGrid latitudeTPG = null;
    private TiePointGrid longitudeTPG = null;

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
    private SARGeocoding.Orbit orbit = null;
    private int polyDegree = 2; // degree of fitting polynomial

    private double noDataValue = 0;
    private double beta0 = 0;

    private int tileSize = 100;
    private float tileOverlapPercentage = 0.0f;

    private OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;

    private boolean nearRangeOnLeft = true;

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
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfMapProjected();

            getMetadata();

            getTiePointGrid();

            getSourceImageDimension();

            computeSensorPositionsAndVelocities();

            createTargetProduct();

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);

            noDataValue = sourceProduct.getBands()[0].getNoDataValue();

            beta0 = azimuthSpacing * rangeSpacing;

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
        if (fileElevationModel != null) {
            fileElevationModel.dispose();
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     *
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        wavelength = SARUtils.getRadarFrequency(absRoot);
        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day
        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        } else {
            nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final String mission = RangeDopplerGeocodingOp.getMissionType(absRoot);
        final String pass = absRoot.getAttributeString("PASS");
        if (mission.equals("RS2") && pass.contains("DESCENDING")) {
            nearRangeOnLeft = false;
        }
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Compute sensor position and velocity for each range line.
     */
    private void computeSensorPositionsAndVelocities() {

        orbit = new SARGeocoding.Orbit(orbitStateVectors, polyDegree, firstLineUTC, lineTimeInterval, sourceImageHeight);
    }

    /**
     * Get incidence angle and slant range time tie point grids.
     */
    private void getTiePointGrid() {
        latitudeTPG = OperatorUtils.getLatitude(sourceProduct);
        if (latitudeTPG == null) {
            throw new OperatorException("Product without latitude tie point grid");
        }

        longitudeTPG = OperatorUtils.getLongitude(sourceProduct);
        if (longitudeTPG == null) {
            throw new OperatorException("Product without longitude tie point grid");
        }

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

        if (externalDEMFile != null && fileElevationModel == null) { // if external DEM file is specified by user
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
        } else {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        absTgt.setAttributeString("DEM resampling method", demResamplingMethod);
        absTgt.setAttributeInt(AbstractMetadata.abs_calibration_flag, 1);

        if (externalDEMFile != null) {
            absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
        }

        // set the tile width to the image width to reduce tiling effect
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), tileSize);
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);
        for (Band band : sourceBands) {
            ProductUtils.copyBand(band.getName(), sourceProduct, band.getName(), targetProduct, false);
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

        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            if (!overlapComputed) {
                computeTileOverlapPercentage(tileSize);
            }

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final double[][] simulatedImage = new double[h][w];
            // System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final boolean validSimulation = generateSimulatedImage(x0, y0, w, h, simulatedImage);
            if (!validSimulation) {
                return;
            }

            outputNormalizedImage(x0, y0, w, h, simulatedImage, targetTiles, targetRectangle);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Generate simulated image for normalization.
     *
     * @param x0             X coordinate of the upper left corner pixel of given tile.
     * @param y0             Y coordinate of the upper left corner pixel of given tile.
     * @param w              Width of given tile.
     * @param h              Height of given tile.
     * @param simulatedImage The simulated image.
     * @return Boolean flag indicating if the simulation is successful.
     */
    private boolean generateSimulatedImage(
            final int x0, final int y0, final int w, final int h, double[][] simulatedImage) {

        try {
            int ymin = 0;
            int ymax = 0;
            if (tileOverlapPercentage >= 0.0f) {
                ymin = Math.max(y0 - (int) (tileSize * tileOverlapPercentage), 0);
                ymax = y0 + h;
            } else {
                ymin = y0;
                ymax = y0 + h + (int) (tileSize * Math.abs(tileOverlapPercentage));
            }

            final TerrainData terrainData = new TerrainData(w, ymax - ymin);
            final boolean valid = getLocalDEM(x0, ymin, w, ymax - ymin, terrainData);
            if (!valid) {
                return false;
            }

            final double[] earthPoint = new double[3];
            final double[] sensorPos = new double[3];
            for (int y = ymin; y < ymax; y++) {

                final double[] azimuthIndex = new double[w];
                final double[] rangeIndex = new double[w];
                final double[] illuminatedArea = new double[w];
                final double[] elevationAngle = new double[w];
                final boolean[] savePixel = new boolean[w];

                for (int x = x0; x < x0 + w; x++) {
                    final int i = x - x0;
                    final int xx = x - x0 + 1;
                    final int yy = y - ymin + 1;

                    final double alt = terrainData.localDEM[yy][xx];
                    if (alt == demNoDataValue) {
                        savePixel[i] = false;
                        continue;
                    }

                    GeoUtils.geo2xyzWGS84(terrainData.latPixels[yy][xx], terrainData.lonPixels[yy][xx], alt, earthPoint);

                    final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                            firstLineUTC, lineTimeInterval, wavelength, earthPoint,
                            orbit.sensorPosition, orbit.sensorVelocity);

                    double slantRange = SARGeocoding.computeSlantRange(
                            zeroDopplerTime - firstLineUTC, orbit.xPosCoeff, orbit.yPosCoeff, orbit.zPosCoeff, earthPoint, sensorPos);

                    final double zeroDopplerTimeWithoutBias =
                            zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

                    azimuthIndex[i] = (zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval;

                    slantRange = SARGeocoding.computeSlantRange(
                            zeroDopplerTimeWithoutBias - firstLineUTC, orbit.xPosCoeff, orbit.yPosCoeff, orbit.zPosCoeff, earthPoint, sensorPos);

                    rangeIndex[i] = SARGeocoding.computeRangeIndex(
                            srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                            zeroDopplerTimeWithoutBias, slantRange, nearEdgeSlantRange, srgrConvParams);

                    if (rangeIndex[i] <= 0.0) {
                        continue;
                    }

                    if (!nearRangeOnLeft) {
                        rangeIndex[i] = sourceImageWidth - 1 - rangeIndex[i];
                    }

                    final LocalGeometry localGeometry = new LocalGeometry(x, y, earthPoint, sensorPos, terrainData, xx, yy);

                    illuminatedArea[i] = computeLocalIlluminatedArea(x0, ymin, x, y, localGeometry,
                            terrainData.localDEM, demNoDataValue);

                    if (illuminatedArea[i] == noDataValue) {
                        savePixel[i] = false;
                        continue;
                    }

                    elevationAngle[i] = computeElevationAngle(slantRange, earthPoint, sensorPos);

                    savePixel[i] = rangeIndex[i] >= x0 && rangeIndex[i] < x0 + w &&
                            azimuthIndex[i] > y0 - 1 && azimuthIndex[i] < y0 + h;
                }

                if (nearRangeOnLeft) {
                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int x = x0; x < x0 + w; x++) {
                        int i = x - x0;
                        if (savePixel[i] && elevationAngle[i] > maxElevAngle) {
                            maxElevAngle = elevationAngle[i];
                            saveLocalIlluminatedArea(x0, y0, w, h, illuminatedArea[i], azimuthIndex[i],
                                    rangeIndex[i], simulatedImage);
                        }
                    }

                } else {
                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int x = x0 + w - 1; x >= x0; x--) {
                        int i = x - x0;
                        if (savePixel[i] && elevationAngle[i] > maxElevAngle) {
                            maxElevAngle = elevationAngle[i];
                            saveLocalIlluminatedArea(x0, y0, w, h, illuminatedArea[i], azimuthIndex[i],
                                    rangeIndex[i], simulatedImage);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
        return true;
    }

    /**
     * Output normalized image.
     *
     * @param x0              X coordinate of the upper left corner pixel of given tile.
     * @param y0              Y coordinate of the upper left corner pixel of given tile.
     * @param w               Width of given tile.
     * @param h               Height of given tile.
     * @param simulatedImage  The simulated image.
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     */
    private void outputNormalizedImage(final int x0, final int y0, final int w, final int h,
                                       final double[][] simulatedImage, final Map<Band, Tile> targetTiles,
                                       final Rectangle targetRectangle) {

        final Band[] targetBands = targetProduct.getBands();
        final int numBands = targetBands.length;
        final ProductData[] targetData = new ProductData[numBands];
        Tile targetTile = null;
        for (int i = 0; i < numBands; i++) {
            targetTile = targetTiles.get(targetBands[i]);
            targetData[i] = targetTile.getDataBuffer();
        }
        final TileIndex trgIndex = new TileIndex(targetTile);

        final Tile[] sourceTile = new Tile[numBands];
        final ProductData[] sourceData = new ProductData[numBands];
        for (int i = 0; i < numBands; i++) {
            final Band srcBand = sourceProduct.getBand(targetBands[i].getName());
            sourceTile[i] = getSourceTile(srcBand, targetRectangle);
            sourceData[i] = sourceTile[i].getDataBuffer();
        }

        for (int n = 0; n < numBands; n++) {
            for (int y = y0; y < y0 + h; y++) {
                final int yy = y - y0;
                trgIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {
                    final int xx = x - x0;
                    final int idx = trgIndex.getIndex(x);
                    if (simulatedImage[yy][xx] != noDataValue && simulatedImage[yy][xx] != 0.0) {
                        targetData[n].setElemDoubleAt(idx, sourceData[n].getElemDoubleAt(idx) / simulatedImage[yy][xx]);
                    } else {
                        targetData[n].setElemDoubleAt(idx, noDataValue);
                    }
                }
            }
        }
    }

    /**
     * Get elevation model.
     *
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) {
            return;
        }

        if (externalDEMFile != null && fileElevationModel == null) { // if external DEM file is specified by user

            fileElevationModel = new FileElevationModel(externalDEMFile,
                    ResamplingFactory.createResampling(demResamplingMethod),
                    (float) externalDEMNoDataValue);

            demNoDataValue = (float) externalDEMNoDataValue;
            demName = externalDEMFile.getPath();

        } else {

            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
            if (demDescriptor == null) {
                throw new OperatorException("The DEM '" + demName + "' is not supported.");
            }

            if (demDescriptor.isInstallingDem()) {
                throw new OperatorException("The DEM '" + demName + "' is currently being installed.");
            }

            dem = demDescriptor.createDem(ResamplingFactory.createResampling(demResamplingMethod));
            if (dem == null) {
                throw new OperatorException("The DEM '" + demName + "' has not been installed.");
            }

            demNoDataValue = dem.getDescriptor().getNoDataValue();
        }
        isElevationModelAvailable = true;
    }

    private synchronized void computeTileOverlapPercentage(final int tileSize) throws Exception {

        if (overlapComputed) {
            return;
        }

        final int x = sourceImageWidth / 2;
        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];
        final GeoPos geoPos = new GeoPos();
        int y;
        double alt = 0.0;
        for (y = tileSize - 1; y < sourceImageHeight; y++) {
            geoPos.setLocation((float) latitudeTPG.getPixelDouble(x, y), (float) longitudeTPG.getPixelDouble(x, y));

            if (externalDEMFile == null) {
                alt = dem.getElevation(geoPos);
            } else {
                alt = fileElevationModel.getElevation(geoPos);
            }

            if (alt != demNoDataValue) {
                break;
            }
        }

        GeoUtils.geo2xyzWGS84(latitudeTPG.getPixelDouble(x, y), longitudeTPG.getPixelDouble(x, y), alt, earthPoint);

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                firstLineUTC, lineTimeInterval, wavelength, earthPoint, orbit.sensorPosition, orbit.sensorVelocity);

        final double slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime - firstLineUTC, orbit.xPosCoeff, orbit.yPosCoeff, orbit.zPosCoeff, earthPoint, sensorPos);

        final double zeroDopplerTimeWithoutBias = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

        final int azimuthIndex = (int) ((zeroDopplerTimeWithoutBias - firstLineUTC) / lineTimeInterval + 0.5);

        tileOverlapPercentage = (float) (azimuthIndex - y) / (float) tileSize;
        if (tileOverlapPercentage >= 0.0) {
            tileOverlapPercentage += 0.05;
        } else {
            tileOverlapPercentage -= 0.05;
        }
        overlapComputed = true;
    }

    /**
     * Read DEM for current tile.
     *
     * @param x0          The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0          The y coordinate of the pixel at the upper left corner of current tile.
     * @param tileHeight  The tile height.
     * @param tileWidth   The tile width.
     * @param terrainData The DEM for the tile.
     * @return false if all values are no data
     * @throws Exception from dem
     */
    private boolean getLocalDEM(final int x0, final int y0, final int tileWidth, final int tileHeight,
                                final TerrainData terrainData) throws Exception {

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.
        final GeoPos geoPos = new GeoPos();
        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        double alt;
        boolean valid = false;
        for (int y = y0 - 1; y < maxY; y++) {
            final int yy = y - y0 + 1;
            for (int x = x0 - 1; x < maxX; x++) {
                final int xx = x - x0 + 1;

                final double lat = latitudeTPG.getPixelFloat(x + 0.5f, y + 0.5f);
                final double lon = longitudeTPG.getPixelFloat(x + 0.5f, y + 0.5f);
                geoPos.setLocation((float) lat, (float) lon);

                if (externalDEMFile == null) {
                    alt = dem.getElevation(geoPos);
                } else {
                    alt = fileElevationModel.getElevation(geoPos);
                }

                terrainData.localDEM[yy][xx] = alt;
                terrainData.latPixels[yy][xx] = lat;
                terrainData.lonPixels[yy][xx] = lon;

                if (alt != demNoDataValue)
                    valid = true;
            }
        }
        if (fileElevationModel != null) {
            //fileElevationModel.clearCache();
        }

        return valid;
    }

    /**
     * Distribute the local illumination area to the 4 adjacent pixels using bi-linear distribution.
     *
     * @param x0              The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0              The y coordinate of the pixel at the upper left corner of current tile.
     * @param w               The tile width.
     * @param h               The tile height.
     * @param illuminatedArea The illuminated area.
     * @param azimuthIndex    Azimuth pixel index for the illuminated area.
     * @param rangeIndex      Range pixel index for the illuminated area.
     * @param simulatedImage  Buffer for the simulated image.
     */
    private void saveLocalIlluminatedArea(final int x0, final int y0, final int w, final int h,
                                          final double illuminatedArea, final double azimuthIndex,
                                          final double rangeIndex, final double[][] simulatedImage) {

        final int ia0 = (int) azimuthIndex;
        final int ia1 = ia0 + 1;
        final int ir0 = (int) rangeIndex;
        final int ir1 = ir0 + 1;

        final double wr = rangeIndex - ir0;
        final double wa = azimuthIndex - ia0;
        final double wac = 1 - wa;

        if (ir0 >= x0) {
            final double wrc = 1 - wr;
            if (ia0 >= y0)
                simulatedImage[ia0 - y0][ir0 - x0] += wrc * wac * illuminatedArea / beta0;
            if (ia1 < y0 + h)
                simulatedImage[ia1 - y0][ir0 - x0] += wrc * wa * illuminatedArea / beta0;
        }
        if (ir1 < x0 + w) {
            if (ia0 >= y0)
                simulatedImage[ia0 - y0][ir1 - x0] += wr * wac * illuminatedArea / beta0;
            if (ia1 < y0 + h)
                simulatedImage[ia1 - y0][ir1 - x0] += wr * wa * illuminatedArea / beta0;
        }
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
            final double slantRange, final double[] earthPoint, final double[] sensorPos) {

        final double H2 = sensorPos[0] * sensorPos[0] + sensorPos[1] * sensorPos[1] + sensorPos[2] * sensorPos[2];
        final double R2 = earthPoint[0] * earthPoint[0] + earthPoint[1] * earthPoint[1] + earthPoint[2] * earthPoint[2];

        return FastMath.acos((slantRange * slantRange + H2 - R2) / (2 * slantRange * Math.sqrt(H2))) *
                org.esa.beam.util.math.MathUtils.RTOD;
    }

    /**
     * Compute local illuminated area for given point.
     *
     * @param xMin           Start of the simulated area in range direction.
     * @param yMin           Start of the simulated area in azimuth direction.
     * @param x              X coordinate of given point.
     * @param y              Y coordinate of given point.
     * @param lg             Local geometry information.
     * @param localDEM       The digital elevation model.
     * @param demNoDataValue Invalid DEM value.
     * @return The computed local illuminated area.
     */
    private double computeLocalIlluminatedArea(final int xMin, final int yMin, final int x, final int y,
                                               final LocalGeometry lg, final double[][] localDEM,
                                               final double demNoDataValue) {

        final int yy = y - yMin + 1; // add 1 because localDEM is larger than tile by 1 pixel in each direction
        final int xx = x - xMin + 1;

        final double h00 = localDEM[yy][xx];
        final double h01 = localDEM[yy - 1][xx];
        final double h10 = localDEM[yy][xx + 1];
        final double h11 = localDEM[yy - 1][xx + 1];

        if (h00 == demNoDataValue || h01 == demNoDataValue || h10 == demNoDataValue || h11 == demNoDataValue) {
            return noDataValue;
        }

        final double[] t00 = new double[3];
        final double[] t01 = new double[3];
        final double[] t10 = new double[3];
        final double[] t11 = new double[3];

        GeoUtils.geo2xyzWGS84(lg.t00Lat, lg.t00Lon, h00, t00);
        GeoUtils.geo2xyzWGS84(lg.t01Lat, lg.t01Lon, h01, t01);
        GeoUtils.geo2xyzWGS84(lg.t10Lat, lg.t10Lon, h10, t10);
        GeoUtils.geo2xyzWGS84(lg.t11Lat, lg.t11Lon, h11, t11);

        // compute slant range direction
        final double[] s = {lg.sensorPos[0] - lg.centerPoint[0],
                lg.sensorPos[1] - lg.centerPoint[1],
                lg.sensorPos[2] - lg.centerPoint[2]};

        Maths.normalizeVector(s);

        // project points t00, t01, t10 and t11 to the plane that perpendicular to slant range
        final double t00s = Maths.innerProduct(t00, s);
        final double t01s = Maths.innerProduct(t01, s);
        final double t10s = Maths.innerProduct(t10, s);
        final double t11s = Maths.innerProduct(t11, s);

        final double[] p00 = {t00[0] - t00s * s[0], t00[1] - t00s * s[1], t00[2] - t00s * s[2]};
        final double[] p01 = {t01[0] - t01s * s[0], t01[1] - t01s * s[1], t01[2] - t01s * s[2]};
        final double[] p10 = {t10[0] - t10s * s[0], t10[1] - t10s * s[1], t10[2] - t10s * s[2]};
        final double[] p11 = {t11[0] - t11s * s[0], t11[1] - t11s * s[1], t11[2] - t11s * s[2]};

        // compute distances between projected points
        final double p00p01 = distance(p00, p01);
        final double p00p10 = distance(p00, p10);
        final double p11p01 = distance(p11, p01);
        final double p11p10 = distance(p11, p10);
        final double p10p01 = distance(p10, p01);

        // compute semi-perimeters of two triangles: p00-p01-p10 and p11-p01-p10
        final double h1 = 0.5 * (p00p01 + p00p10 + p10p01);
        final double h2 = 0.5 * (p11p01 + p11p10 + p10p01);

        // compute the illuminated area
        return Math.sqrt(h1 * (h1 - p00p01) * (h1 - p00p10) * (h1 - p10p01)) +
                Math.sqrt(h2 * (h2 - p11p01) * (h2 - p11p10) * (h2 - p10p01));
    }


    private static double distance(final double[] p1, final double[] p2) {
        return Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) +
                (p1[1] - p2[1]) * (p1[1] - p2[1]) +
                (p1[2] - p2[2]) * (p1[2] - p2[2]));
    }


    public static class LocalGeometry {
        public final double t00Lat;
        public final double t00Lon;
        public final double t01Lat;
        public final double t01Lon;
        public final double t10Lat;
        public final double t10Lon;
        public final double t11Lat;
        public final double t11Lon;
        public final double[] sensorPos;
        public final double[] centerPoint;

        public LocalGeometry(final int x, final int y, final double[] earthPoint, final double[] sensPos,
                             final TerrainData terrainData, final int xx, final int yy) {

            t00Lat = terrainData.latPixels[yy][xx];
            t00Lon = terrainData.lonPixels[yy][xx];
            t01Lat = terrainData.latPixels[yy - 1][xx];
            t01Lon = terrainData.lonPixels[yy - 1][xx];
            t10Lat = terrainData.latPixels[yy][xx + 1];
            t10Lon = terrainData.lonPixels[yy][xx + 1];
            t11Lat = terrainData.latPixels[yy + 1][xx + 1];
            t11Lon = terrainData.lonPixels[yy + 1][xx + 1];
            centerPoint = earthPoint;
            sensorPos = sensPos;
        }
    }

    private static class TerrainData {
        final double[][] localDEM;
        final double[][] latPixels;
        final double[][] lonPixels;

        public TerrainData(int w, int h) {
            localDEM = new double[h + 2][w + 2];
            latPixels = new double[h + 2][w + 2];
            lonPixels = new double[h + 2][w + 2];
        }
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
            super(TerrainFlatteningOp.class);
        }
    }
}
