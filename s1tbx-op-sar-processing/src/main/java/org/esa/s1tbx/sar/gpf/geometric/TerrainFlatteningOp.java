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
import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.s1tbx.commons.SARUtils;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.*;
import org.esa.snap.engine_utilities.util.Maths;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This operator implements the terrain flattening algorithm proposed by
 * David Small. For details, see the paper below and the references therein.
 * David Small, "Flattening Gamma: Radiometric Terrain Correction for SAR imagery",
 * IEEE Transaction on Geoscience and Remote Sensing, Vol. 48, No. 8, August 2011.
 */

@OperatorMetadata(alias = "Terrain-Flattening",
        category = "Radar/Radiometric",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Terrain Flattening")
public final class TerrainFlatteningOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 1Sec HGT", label = "Digital Elevation Model")
    private String demName = "SRTM 1Sec HGT";

    @Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(label = "External DEM Apply EGM", defaultValue = "false")
    private Boolean externalDEMApplyEGM = false;

    @Parameter(defaultValue = "false", label = "Output Simulated Image")
    private Boolean outputSimulatedImage = false;

    @Parameter(description = "The additional overlap percentage", interval = "[0, 1]", label = "Additional Overlap",
            defaultValue = "0.1")
    private Double additionalOverlap = 0.1;

    @Parameter(description = "The oversampling factor", interval = "[1, 4]", label = "Oversampling Multiple",
            defaultValue = "1.0")
    private Double oversamplingMultiple = 1.0;

    private Product newSourceProduct = null;
    private ElevationModel dem = null;
    private FileElevationModel fileElevationModel = null;
    private TiePointGrid incidenceAngleTPG = null;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean srgrFlag = false;
    private boolean isElevationModelAvailable = false;
    private boolean isGRD = false;
    private boolean isPolSar = false;

    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lastLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private double nearEdgeSlantRange = 0.0; // in m
    private double wavelength = 0.0; // in m
    private double demNoDataValue = 0; // no data value for DEM
    private double overSamplingFactor = 1.0;
    private OrbitStateVectors orbit = null;
    private Resampling selectedResampling = null;
    private Double noDataValue = 0.0;
    private double aBeta = 0.0;

    private OrbitStateVector[] orbitStateVectors = null;
    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    private Band simulatedImageBand = null;
    private Band[] targetBands = null;
    private final HashMap<Band, Band> targetBandToSourceBandMap = new HashMap<>(2);
    private boolean nearRangeOnLeft = true;
    private boolean orbitOnWest = true;

    // set this flag to true to output terrain flattened sigma0
    private boolean outputSigma0 = false;
    private boolean detectShadow = false;
    private double threshold = 0.05;
    private boolean invalidSource = false;

    private static final String PRODUCT_SUFFIX = "_TF";

    enum UnitType {AMPLITUDE, INTENSITY, COMPLEX, RATIO}

    private static final String SIMULATED_IMAGE = "simulatedImage";
    private static final String[] BAND_PREFIX = new String[] { "Beta0",
            "T11", "T12", "T13", "T22", "T23", "T33",
            "C11", "C12", "C13", "C22", "C23", "C33",
            "C11", "C12", "C13", "T22"};

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product}
     * annotated with the {@link TargetProduct TargetProduct} annotation or
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
            validator.checkIfSARProduct();
            validator.checkIfMapProjected(false);

            PolBandUtils.MATRIX sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType.equals(PolBandUtils.MATRIX.T3) || sourceProductType.equals(PolBandUtils.MATRIX.C3)
                    || sourceProductType.equals(PolBandUtils.MATRIX.C2)) {
                isPolSar = true;
            } else if (!validator.isCalibrated()) {
                final  OperatorSpi spi = new CalibrationOp.Spi();
                final CalibrationOp op = (CalibrationOp) spi.createOperator();
                op.setSourceProduct(sourceProduct);
                op.setParameter("outputBetaBand", true);
                newSourceProduct = op.getTargetProduct();
            } else {
                newSourceProduct = sourceProduct;
            }

            if (demName.equals("External DEM") && externalDEMFile == null) {
                throw new OperatorException("External DEM file is not found");
            }

            if (externalDEMApplyEGM == null) {
                externalDEMApplyEGM = false;
            }

            selectedResampling = ResamplingFactory.createResampling(demResamplingMethod);
            if(selectedResampling == null) {
                throw new OperatorException("Resampling method "+ demResamplingMethod + " is invalid");
            }
            if(additionalOverlap == null) {
                additionalOverlap = 0.1;
            }
            if(oversamplingMultiple == null) {
                oversamplingMultiple = 1.0;
            }

            getMetadata();

            getTiePointGrid();

            getSourceImageDimension();

            orbit = new OrbitStateVectors(orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);

            createTargetProduct();

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, newSourceProduct);

            noDataValue = newSourceProduct.getBands()[0].getNoDataValue();

            aBeta = azimuthSpacing * rangeSpacing;

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

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(newSourceProduct);
        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        final double minSpacing = Math.min(rangeSpacing, azimuthSpacing);

        if (externalDEMFile == null) {
            if (demName.contains("SRTM 3Sec") && (rangeSpacing < 90.0 || azimuthSpacing < 90.0)) {
                overSamplingFactor = Math.ceil(90.0 / minSpacing);
            } else if (demName.contains("SRTM 1Sec HGT") && (rangeSpacing < 30.0 || azimuthSpacing < 30.0)) {
                overSamplingFactor = Math.ceil(30.0 / minSpacing);
            } else if (demName.contains("SRTM 1Sec Grid") && (rangeSpacing < 30.0 || azimuthSpacing < 30.0)) {
                overSamplingFactor = Math.ceil(30.0 / minSpacing);
            } else if (demName.contains("ASTER 1sec GDEM") && (rangeSpacing < 30.0 || azimuthSpacing < 30.0)) {
                overSamplingFactor = Math.ceil(30.0 / minSpacing);
            } else if (demName.contains("ACE30") && (rangeSpacing < 1000.0 || azimuthSpacing < 1000.0)) {
                overSamplingFactor = Math.ceil(1000.0 / minSpacing);
            } else if (demName.contains("ACE2_5Min") && (rangeSpacing < 10000.0 || azimuthSpacing < 10000.0)) {
                overSamplingFactor = Math.ceil(1000.0 / minSpacing);
            } else if (demName.contains("GETASSE30") && (rangeSpacing < 1000.0 || azimuthSpacing < 1000.0)) {
                overSamplingFactor = Math.ceil(1000.0 / minSpacing);
            }
            overSamplingFactor *= oversamplingMultiple;
        }

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        wavelength = SARUtils.getRadarWavelength(absRoot);
        firstLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days
        lastLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day
        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        } else {
            nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        final String mission = RangeDopplerGeocodingOp.getMissionType(absRoot);
        final String pass = absRoot.getAttributeString(AbstractMetadata.PASS);
        if (mission.equals("RS2") && pass.contains("DESCENDING")) {
            nearRangeOnLeft = false;
        }

        String antennaPointing = absRoot.getAttributeString(AbstractMetadata.antenna_pointing);
        if (!antennaPointing.contains("right") && !antennaPointing.contains("left")) {
            antennaPointing = "right";
        }

        if ((pass.contains("DESCENDING") && antennaPointing.contains("right")) ||
                (pass.contains("ASCENDING") && antennaPointing.contains("left"))) {
            orbitOnWest = false;
        }

//        if (mission.contains("CSKS") || mission.contains("TSX") || mission.equals("RS2") || mission.contains("SENTINEL")) {
//            skipBistaticCorrection = true;
//        }

        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if (!sampleType.contains("COMPLEX")) {
            isGRD = true;
        }
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = newSourceProduct.getSceneRasterWidth();
        sourceImageHeight = newSourceProduct.getSceneRasterHeight();
    }

    /**
     * Get tie point grids.
     */
    private void getTiePointGrid() {
        incidenceAngleTPG = OperatorUtils.getIncidenceAngle(newSourceProduct);
        if (incidenceAngleTPG == null) {
            throw new OperatorException("Cannot find the incidence angle tie point grid in the source product");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(newSourceProduct.getName() + PRODUCT_SUFFIX,
                newSourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        addSelectedBands();

        ProductUtils.copyProductNodes(newSourceProduct, targetProduct);

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
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

        final Band[] sourceBands = OperatorUtils.getSourceBands(newSourceProduct, sourceBandNames, true);

        for (final Band srcBand : sourceBands) {
            final String srcBandName = srcBand.getName();

            //beta0 or polsar product
            boolean valid = false;
            for(String validPrefix : BAND_PREFIX) {
                if(srcBandName.startsWith(validPrefix)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                continue;
            }

            if (isPolSar) {
                if (targetProduct.getBand(srcBandName) == null) {
                    Band tgtBand = targetProduct.addBand(srcBandName, ProductData.TYPE_FLOAT32);
                    tgtBand.setUnit(srcBand.getUnit());
                    tgtBand.setNoDataValue(srcBand.getNoDataValue());
                    tgtBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
                    tgtBand.setDescription(srcBand.getDescription());
                    targetBandToSourceBandMap.put(tgtBand, srcBand);
                }
            } else {

                if (!srcBandName.contains("Beta0")) {
                    throw new OperatorException("TerrainFlattening requires beta0 as input");
                }

                final String gamma0BandName = srcBandName.replaceFirst("Beta0", "Gamma0");
                final String sigma0BandName = srcBandName.replaceFirst("Beta0", "Sigma0");

                Band targetBand;
                if (targetProduct.getBand(gamma0BandName) == null) {
                    targetBand = targetProduct.addBand(gamma0BandName, ProductData.TYPE_FLOAT32);
                    targetBand.setUnit(Unit.INTENSITY);
                    targetBand.setNoDataValue(srcBand.getNoDataValue());
                    targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
                    targetBandToSourceBandMap.put(targetBand, srcBand);
                }

                if (outputSigma0 && targetProduct.getBand(sigma0BandName) == null) {
                    targetBand = targetProduct.addBand(sigma0BandName, ProductData.TYPE_FLOAT32);
                    targetBand.setUnit(Unit.INTENSITY);
                    targetBand.setNoDataValue(srcBand.getNoDataValue());
                    targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
                    targetBandToSourceBandMap.put(targetBand, srcBand);
                }
            }
        }

        if (targetProduct.getNumBands() == 0) {
            invalidSource = true;
            // Moved the following exception to computeTileStack. Add a dummy band so that computeTileStack get executed
            //throw new OperatorException("TerrainFlattening requires beta0 or T3, C3, C2 as input");
            final Band dummyBand = targetProduct.addBand("dummy", ProductData.TYPE_INT8);
            dummyBand.setUnit(Unit.AMPLITUDE);
        }

        if (outputSimulatedImage) {
            simulatedImageBand = targetProduct.addBand(SIMULATED_IMAGE, ProductData.TYPE_FLOAT32);
            simulatedImageBand.setUnit("Ratio");
        }

        targetBands = targetProduct.getBands();
        if (!isPolSar) {
            for (int i = 0; i < targetBands.length; ++i) {
                if (targetBands[i].getUnit().equals(Unit.REAL)) {
                    final String trgBandName = targetBands[i].getName();
                    final int idx = trgBandName.indexOf("_");
                    String suffix = "";
                    if (idx != -1) {
                        suffix = trgBandName.substring(trgBandName.indexOf("_"));
                    }
                    ReaderUtils.createVirtualIntensityBand(
                            targetProduct, targetBands[i], targetBands[i + 1], "Gamma0", suffix);
                }
            }
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
            if (invalidSource) {
                throw new OperatorException("TerrainFlattening requires beta0 or T3, C3, C2 as input");
            }
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final OverlapPercentage tileOverlapPercentage = computeTileOverlapPercentage(x0, y0, w, h, pm);
            if(tileOverlapPercentage == null) {
                return;
            }

            final double[][] gamma0ReferenceArea = new double[h][w];
            double[][] sigma0ReferenceArea = null;
            if (outputSigma0) {
                sigma0ReferenceArea = new double[h][w];
            }

            final boolean validSimulation = generateSimulatedImage(
                    x0, y0, w, h, tileOverlapPercentage, gamma0ReferenceArea, sigma0ReferenceArea, pm);

            if (!validSimulation) {
                return;
            }

            if (isPolSar) {
                outputNormalizedT3(x0, y0, w, h, gamma0ReferenceArea, targetTiles, targetRectangle);
            } else {
                outputNormalizedImage(x0, y0, w, h, gamma0ReferenceArea, "Gamma0", targetTiles, targetRectangle);
                if (outputSigma0) {
                    outputNormalizedImage(x0, y0, w, h, sigma0ReferenceArea, "Sigma0", targetTiles, targetRectangle);
                }
            }

            if (outputSimulatedImage) {
                outputSimulatedArea(x0, y0, w, h, gamma0ReferenceArea, simulatedImageBand, targetTiles);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Generate simulated image for normalization.
     *
     * @param x0                  X coordinate of the upper left corner pixel of given tile.
     * @param y0                  Y coordinate of the upper left corner pixel of given tile.
     * @param w                   Width of given tile.
     * @param h                   Height of given tile.
     * @param gamma0ReferenceArea The simulated image for flattened gamma0 generation.
     * @param sigma0ReferenceArea The simulated image for flattened sigma0 generation.
     * @return Boolean flag indicating if the simulation is successful.
     */
    private boolean generateSimulatedImage(final int x0, final int y0, final int w, final int h,
                                           final OverlapPercentage tileOverlapPercentage,
                                           final double[][] gamma0ReferenceArea,
                                           final double[][] sigma0ReferenceArea,
                                           final ProgressMonitor pm) {

        try {
            final int ymin = Math.max(y0 - (int) (h * tileOverlapPercentage.tileOverlapUp), 0);
            final int ymax = Math.min(y0 + h + (int) (h * tileOverlapPercentage.tileOverlapDown), sourceImageHeight);
            final int xmin = Math.max(x0 - (int) (w * tileOverlapPercentage.tileOverlapLeft), 0);
            final int xmax = Math.min(x0 + w + (int) (w * tileOverlapPercentage.tileOverlapRight), sourceImageWidth);

            final double[] latLonMinMax = new double[4];
            computeImageGeoBoundary(xmin, xmax, ymin, ymax, latLonMinMax);

            double demResolution;
            if (externalDEMFile == null) {
                demResolution = (double) dem.getDescriptor().getTileWidthInDegrees() /
                        (double) dem.getDescriptor().getTileWidth();

            } else {
                FileElevationModel filedem = (FileElevationModel)dem;
                demResolution = filedem.getPixelWidthInDegrees();
                final double minSpacing = Math.min(rangeSpacing, azimuthSpacing);
                overSamplingFactor = Math.ceil(filedem.getPixelWidthInMeters() / minSpacing) * oversamplingMultiple;
            }

            final double extralat = 20 * demResolution;
            final double extralon = 20 * demResolution;
            final double latMin = latLonMinMax[0] - extralat;
            final double latMax = latLonMinMax[1] + extralat;
            final double lonMin = latLonMinMax[2] - extralon;
            final double lonMax = latLonMinMax[3] + extralon;

            final int rows = (int) Math.round((latMax - latMin) / demResolution);
            final int cols = (int) Math.round((lonMax - lonMin) / demResolution);

            final double[][] height = new double[rows][cols];
            for (int i = 0; i < rows; ++i) {
                final double lat = latMax - i * demResolution;
                for (int j = 0; j < cols; ++j) {
                    final double lon = lonMin + j * demResolution;
                    height[i][j] = dem.getElevation(new GeoPos(lat, lon));
                }
            }
            final ResamplingRaster resamplingRaster = new ResamplingRaster(demNoDataValue, height);
            final Resampling.Index resamplingIndex = selectedResampling.createIndex();

            final double delta = demResolution / overSamplingFactor;
            final double ratio = delta / demResolution;
            final int nLat = (int) (overSamplingFactor * rows);
            final int nLon = (int) (overSamplingFactor * cols);

            final PositionData posData = new PositionData();
            for (int i = 1; i < nLat; i++) {
                if (pm.isCanceled()) {
                    return false;
                }
                final double lat = latMax - i * delta;
                final double iRatio = i * ratio;
                final double[] azimuthIndex = new double[nLon];
                final double[] rangeIndex = new double[nLon];
                final double[] gamma0Area = new double[nLon];
                final double[] elevationAngle = new double[nLon];
                final boolean[] savePixel = new boolean[nLon];
                double[] sigma0Area = null;
                if (outputSigma0) {
                    sigma0Area = new double[nLon];
                }
                final GeoUtils.Geo2xyzWGS84 geo2xyzWGS84 = new GeoUtils.Geo2xyzWGS84(lat);
                final LocalGeometry localGeometry = new LocalGeometry(lat, delta);

                for (int j = 0; j < nLon; j++) {
                    final double lon = lonMin + j * delta;
                    final double jRatio = j * ratio;
                    selectedResampling.computeCornerBasedIndex(jRatio, iRatio, cols, rows, resamplingIndex);
                    final Double alt00 = selectedResampling.resample(resamplingRaster, resamplingIndex);
                    if (Double.isNaN(alt00) || alt00.equals(demNoDataValue))
                        continue;

                    posData.earthPoint = geo2xyzWGS84.getXYZ(lon, alt00);
                    if (!getPosition(x0, y0, w, h, posData))
                        continue;

                    selectedResampling.computeCornerBasedIndex(jRatio, iRatio - ratio, cols, rows, resamplingIndex);
                    final double alt01 = selectedResampling.resample(resamplingRaster, resamplingIndex);

                    selectedResampling.computeCornerBasedIndex(jRatio + ratio, iRatio, cols, rows, resamplingIndex);
                    final double alt10 = selectedResampling.resample(resamplingRaster, resamplingIndex);

                    selectedResampling.computeCornerBasedIndex(jRatio + ratio, iRatio - ratio, cols, rows, resamplingIndex);
                    final double alt11 = selectedResampling.resample(resamplingRaster, resamplingIndex);

                    localGeometry.setLon(lon, alt00, alt01, alt10, alt11, posData);

                    if (!computeIlluminatedArea(localGeometry, demNoDataValue, noDataValue, j, gamma0Area, sigma0Area)) {
                        continue;
                    }

                    if (detectShadow) {
                        elevationAngle[j] = computeElevationAngle(posData.earthPoint, posData.sensorPos);
                    }
                    rangeIndex[j] = posData.rangeIndex;
                    azimuthIndex[j] = posData.azimuthIndex;
                    savePixel[j] = rangeIndex[j] > x0 - 1 && rangeIndex[j] < x0 + w &&
                            azimuthIndex[j] > y0 - 1 && azimuthIndex[j] < y0 + h;
                }

                if (orbitOnWest) {
                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int jj = 0; jj < nLon; jj++) {
                        if (savePixel[jj]) {
                            if (detectShadow) {
                                if (elevationAngle[jj] < maxElevAngle)
                                    continue;
                                maxElevAngle = elevationAngle[jj];
                            }
                            double sigma0AreaVal = outputSigma0 ? sigma0Area[jj] : noDataValue;
                            saveIlluminationArea(x0, y0, w, h, azimuthIndex[jj], rangeIndex[jj],
                                    gamma0Area[jj], gamma0ReferenceArea,
                                    sigma0AreaVal, sigma0ReferenceArea);
                        }
                    }

                } else {
                    // traverse from near range to far range to detect shadowing area
                    double maxElevAngle = 0.0;
                    for (int jj = nLon - 1; jj >= 0; --jj) {
                        if (savePixel[jj]) {
                            if (detectShadow) {
                                if (elevationAngle[jj] < maxElevAngle)
                                    continue;
                                maxElevAngle = elevationAngle[jj];
                            }
                            double sigma0AreaVal = outputSigma0 ? sigma0Area[jj] : noDataValue;
                            saveIlluminationArea(x0, y0, w, h, azimuthIndex[jj], rangeIndex[jj],
                                    gamma0Area[jj], gamma0ReferenceArea,
                                    sigma0AreaVal, sigma0ReferenceArea);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
        return true;
    }

    private void computeImageGeoBoundary(final int xmin, final int xmax, final int ymin, final int ymax,
                                         double[] latLonMinMax) {

        final GeoCoding geoCoding = newSourceProduct.getSceneGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("Source product does not have a geocoding");
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

    //======================================
    private boolean getPosition(final int x0, final int y0, final int w, final int h,
                                final PositionData data) {

        final double zeroDopplerTime = SARGeocoding.getZeroDopplerTime(
                lineTimeInterval, wavelength, data.earthPoint, orbit);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, data.earthPoint, data.sensorPos);

        data.azimuthIndex = (zeroDopplerTime - firstLineUTC) / lineTimeInterval;

        if (!(data.azimuthIndex >= y0 - 1 && data.azimuthIndex <= y0 + h)) {
            return false;
        }

        if (!srgrFlag) {
            data.rangeIndex = (data.slantRange - nearEdgeSlantRange) / rangeSpacing;
        } else {
            data.rangeIndex = SARGeocoding.computeRangeIndex(
                    srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                    zeroDopplerTime, data.slantRange, nearEdgeSlantRange, srgrConvParams);
        }

        if (!nearRangeOnLeft) {
            data.rangeIndex = sourceImageWidth - 1 - data.rangeIndex;
        }

        return data.rangeIndex >= x0 - 1 && data.rangeIndex <= x0 + w;
    }

    private boolean getPixPos(final double lat, final double lon, final double alt, final PixelPos endPixelPos) {

        final PosVector earthPoint = new PosVector();
        GeoUtils.geo2xyzWGS84(lat, lon, alt, earthPoint);

        final double zeroDopplerTime = SARGeocoding.getZeroDopplerTime(
                lineTimeInterval, wavelength, earthPoint, orbit);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        final PosVector sensorPos = new PosVector();
        final double slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, earthPoint, sensorPos);

        final double azimuthIndex = (zeroDopplerTime - firstLineUTC) / lineTimeInterval;

        double rangeIndex;
        if (!srgrFlag) {
            rangeIndex = (slantRange - nearEdgeSlantRange) / rangeSpacing;
        } else {
            rangeIndex = SARGeocoding.computeExtendedRangeIndex(
                    srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC, rangeSpacing,
                    zeroDopplerTime, slantRange, nearEdgeSlantRange, srgrConvParams);
        }

        if (!nearRangeOnLeft) {
            rangeIndex = sourceImageWidth - 1 - rangeIndex;
        }

        endPixelPos.setLocation(rangeIndex, azimuthIndex);
        return true;
    }

    private void outputSimulatedArea(final int x0, final int y0, final int w, final int h,
                                     final double[][] simulatedImage, final Band targetBand,
                                     final Map<Band, Tile> targetTiles) {

        final Tile targetTile = targetTiles.get(targetBand);
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(targetTile);

        for (int y = y0; y < y0 + h; y++) {
            final int yy = y - y0;
            tgtIndex.calculateStride(y);
            for (int x = x0; x < x0 + w; x++) {
                final int xx = x - x0;
                final int tgtIdx = tgtIndex.getIndex(x);
                double simVal = simulatedImage[yy][xx];
                if (simVal != noDataValue && simVal != 0.0) {
                    simVal /= aBeta;
                    if (isGRD) {
                        simVal /= FastMath.sin(incidenceAngleTPG.getPixelDouble(x, y) * Constants.DTOR);
                    }
                    targetData.setElemDoubleAt(tgtIdx, simVal);
                } else {
                    targetData.setElemDoubleAt(tgtIdx, noDataValue);
                }
            }
        }

    }

    /**
     * Output normalized image.
     *
     * @param x0                  X coordinate of the upper left corner pixel of given tile.
     * @param y0                  Y coordinate of the upper left corner pixel of given tile.
     * @param w                   Width of given tile.
     * @param h                   Height of given tile.
     * @param simulatedImage      The simulated image for flattened gamma0 generation.
     * @param bandNamePrefix      The target band namr prefix.
     * @param targetTiles         The current tiles to be computed for each target band.
     * @param targetRectangle     The area in pixel coordinates to be computed.
     */
    private void outputNormalizedImage(final int x0, final int y0, final int w, final int h,
                                       final double[][] simulatedImage, final String bandNamePrefix,
                                       final Map<Band, Tile> targetTiles, final Rectangle targetRectangle) {

        try {
            for (Band tgtBand : targetBands) {
                if (tgtBand.getName().equals(SIMULATED_IMAGE) || !tgtBand.getName().startsWith(bandNamePrefix)) {
                    continue;
                }

                final Tile targetTile = targetTiles.get(tgtBand);
                final ProductData targetData = targetTile.getDataBuffer();
                final TileIndex tgtIndex = new TileIndex(targetTile);

                final Band srcBand = targetBandToSourceBandMap.get(tgtBand);
                final Tile sourceTile = getSourceTile(srcBand, targetRectangle);
                final ProductData sourceData = sourceTile.getDataBuffer();
                final TileIndex srcIndex = new TileIndex(sourceTile);

                double v, simVal;
                for (int y = y0; y < y0 + h; y++) {
                    final int yy = y - y0;
                    tgtIndex.calculateStride(y);
                    srcIndex.calculateStride(y);

                    for (int x = x0; x < x0 + w; x++) {
                        final int xx = x - x0;
                        final int tgtIdx = tgtIndex.getIndex(x);
                        final int srcIdx = srcIndex.getIndex(x);
                        simVal = simulatedImage[yy][xx];

                        if (simVal != noDataValue) {
                            final double aGamma = aBeta / FastMath.tan(incidenceAngleTPG.getPixelDouble(x, y) * Constants.DTOR);
                            if (simVal > threshold * aGamma) {
                                simVal /= aBeta;
                                if (isGRD) {
                                    simVal /= FastMath.sin(incidenceAngleTPG.getPixelDouble(x, y) * Constants.DTOR);
                                }
                                v = sourceData.getElemDoubleAt(srcIdx);
                                targetData.setElemDoubleAt(tgtIdx, v / simVal);
                            }
                        } else {
                            targetData.setElemDoubleAt(tgtIdx, noDataValue);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void outputNormalizedT3(final int x0, final int y0, final int w, final int h,
                                    final double[][] gamma0ReferenceArea, final Map<Band, Tile> targetTiles,
                                    final Rectangle targetRectangle) {

        try {
            for (Band tgtBand : targetBands) {
                if (tgtBand.getName().equals(SIMULATED_IMAGE)) {
                    continue;
                }

                final Tile targetTile = targetTiles.get(tgtBand);
                final ProductData targetData = targetTile.getDataBuffer();
                final TileIndex tgtIndex = new TileIndex(targetTile);
                final String unit = tgtBand.getUnit();
                final double[][] simulatedImage = gamma0ReferenceArea.clone();

                final Band srcBand = targetBandToSourceBandMap.get(tgtBand);
                final Tile sourceTile = getSourceTile(srcBand, targetRectangle);
                final ProductData sourceData = sourceTile.getDataBuffer();
                final TileIndex srcIndex = new TileIndex(sourceTile);

                double v, simVal;
                int tgtIdx, srcIdx;
                for (int y = y0; y < y0 + h; y++) {
                    final int yy = y - y0;
                    tgtIndex.calculateStride(y);
                    srcIndex.calculateStride(y);
                    for (int x = x0; x < x0 + w; x++) {
                        final int xx = x - x0;
                        tgtIdx = tgtIndex.getIndex(x);
                        srcIdx = srcIndex.getIndex(x);
                        simVal = simulatedImage[yy][xx];

                        if (simVal != noDataValue) {
                            final double aGamma = aBeta / FastMath.tan(incidenceAngleTPG.getPixelDouble(x, y) * Constants.DTOR);
                            if (simVal > threshold * aGamma) {
                                simVal /= aBeta;
                                v = sourceData.getElemDoubleAt(srcIdx);
                                targetData.setElemDoubleAt(tgtIdx, v / simVal);
                            }
                        } else {
                            targetData.setElemDoubleAt(tgtIdx, noDataValue);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get elevation model.
     *
     */
    private synchronized void getElevationModel() {

        if (isElevationModelAvailable) return;
        try {
            if (externalDEMFile != null) { // if external DEM file is specified by user

                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                ((FileElevationModel) dem).applyEarthGravitionalModel(externalDEMApplyEGM);
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

    private OverlapPercentage computeTileOverlapPercentage(final int x0, final int y0, final int w, final int h,
                                                           final ProgressMonitor pm) throws Exception {

        final PixelPos pixPos = new PixelPos();
        final GeoCoding sourceGeoCoding = newSourceProduct.getSceneGeoCoding();

        final int xMin = Math.max(x0 - w/2, 0);
        final int xMax = Math.min(x0 + w + w/2, sourceImageWidth);
        final int yMin = Math.max(y0 - h/2, 0);
        final int yMax = Math.min(y0 + h + h/2, sourceImageHeight);

        double tileOverlapUp = 0.0, tileOverlapDown = 0.0, tileOverlapLeft = 0.0, tileOverlapRight = 0.0;
        for (int y = yMin; y < yMax; y += 20) {
            if(pm.isCanceled()) {
                return null;
            }
            for (int x = xMin; x < xMax; x += 20) {
                if (getTruePixelPos(x, y, pixPos, sourceGeoCoding)) {
                    tileOverlapUp = Math.max((y - pixPos.y) / h, tileOverlapUp);
                    tileOverlapDown = Math.max((pixPos.y - y) / h, tileOverlapDown);
                    tileOverlapLeft = Math.max((x - pixPos.x) / w, tileOverlapLeft);
                    tileOverlapRight = Math.max((pixPos.x - x) / w, tileOverlapRight);
                }
            }
        }

        tileOverlapUp += additionalOverlap;
        tileOverlapDown += additionalOverlap;
        tileOverlapLeft += additionalOverlap;
        tileOverlapRight += additionalOverlap;

        return new OverlapPercentage(tileOverlapUp, tileOverlapDown, tileOverlapLeft, tileOverlapRight);
    }

    private boolean getTruePixelPos(
            final double x0, final double y0, final PixelPos pixelPos, final GeoCoding srcGeoCoding)
            throws Exception {

        final int maxIterations = 100;
        final double errThreshold = 2.0;

        PixelPos startPixelPos = new PixelPos(x0, y0);
        PixelPos endPixelPos = new PixelPos();
        GeoPos currentGeoPos = new GeoPos();

        int numIter;
        for (numIter = 0; numIter < maxIterations; ++numIter) {

            final double err2 = computeError(startPixelPos, currentGeoPos, endPixelPos, srcGeoCoding, x0, y0);
            if (err2 == -1) return false;
            if (err2 < errThreshold) {
                break;
            }

            double errX = x0 - endPixelPos.x;
            double errY = y0 - endPixelPos.y;

            double alpha = 1.0;
            double tmpErr2 = err2;
            for (int i = 0; i < 4; i++) {
                final double tmpErrX = alpha*errX;
                final double tmpErrY = alpha*errY;
                final PixelPos tmpStartPixelPos = new PixelPos(startPixelPos.x + tmpErrX, startPixelPos.y + tmpErrY);
                final PixelPos tmpEndPixelPos = new PixelPos();
                final GeoPos tmpGeoPos = new GeoPos();
                tmpErr2 = computeError(tmpStartPixelPos, tmpGeoPos, tmpEndPixelPos, srcGeoCoding, x0, y0);
                if (tmpErr2 == -1) continue;
                if (tmpErr2 < err2) {
                    errX = tmpErrX;
                    errY = tmpErrY;
                    break;
                } else {
                    alpha /= 2.0;
                }
            }

            if (tmpErr2 < err2) {
                startPixelPos.x += errX;
                startPixelPos.y += errY;
            } else {
                double r1 = Math.random();
                double r2 = Math.random();
                startPixelPos.x += r1*errX;
                startPixelPos.y += r2*errY;
            }
        }

        if (numIter == maxIterations) {
            return false;
        }
        getPixPos(currentGeoPos.lat, currentGeoPos.lon, dem.getElevation(currentGeoPos), endPixelPos);

        pixelPos.x = startPixelPos.x;
        pixelPos.y = startPixelPos.y;
        return true;
    }

    private double computeError(final PixelPos startPixelPos, final GeoPos geoPos, final PixelPos endPixelPos,
                                final GeoCoding srcGeoCoding, final double x0, final double y0) throws Exception {

        startPixelPos.x = Math.min(Math.max(startPixelPos.x, 0), sourceImageWidth - 1);
        startPixelPos.y = Math.min(Math.max(startPixelPos.y, 0), sourceImageHeight - 1);

        srcGeoCoding.getGeoPos(startPixelPos, geoPos);
        final double alt = dem.getElevation(geoPos);
        if (noDataValue.equals(alt))
            return -1;

        if (!getPixPos(geoPos.lat, geoPos.lon, alt, endPixelPos))
            return -1;

        final double errX = x0 - endPixelPos.x;
        final double errY = y0 - endPixelPos.y;
        return errX*errX + errY*errY;
    }

    /**
     * Distribute the local illumination area to the 4 adjacent pixels using bi-linear distribution.
     *
     * @param x0                  The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0                  The y coordinate of the pixel at the upper left corner of current tile.
     * @param w                   The tile width.
     * @param h                   The tile height.
     * @param gamma0Area          The illuminated area.
     * @param azimuthIndex        Azimuth pixel index for the illuminated area.
     * @param rangeIndex          Range pixel index for the illuminated area.
     * @param gamma0ReferenceArea Buffer for the simulated image.
     */
    private void saveIlluminationArea(final int x0, final int y0, final int w, final int h,
                                       final double azimuthIndex, final double rangeIndex,
                                             final double gamma0Area, final double[][] gamma0ReferenceArea,
                                             final double sigma0Area, final double[][] sigma0ReferenceArea) {

        final int ia0 = (int) azimuthIndex;
        final int ia1 = ia0 + 1;
        final int ir0 = (int) rangeIndex;
        final int ir1 = ir0 + 1;

        final double wr = rangeIndex - ir0;
        final double wa = azimuthIndex - ia0;
        final double wac = 1 - wa;

        if (ir0 >= x0 && ir0 < x0 + w) {
            final double wrc = 1 - wr;
            if (ia0 >= y0 && ia0 < y0 + h) {
                gamma0ReferenceArea[ia0 - y0][ir0 - x0] += wrc * wac * gamma0Area;
                if(outputSigma0) {
                    sigma0ReferenceArea[ia0 - y0][ir0 - x0] += wrc * wac * sigma0Area;
                }
            }
            if (ia1 >= y0 && ia1 < y0 + h) {
                gamma0ReferenceArea[ia1 - y0][ir0 - x0] += wrc * wa * gamma0Area;
                if(outputSigma0) {
                    sigma0ReferenceArea[ia1 - y0][ir0 - x0] += wrc * wa * sigma0Area;
                }
            }
        }

        if (ir1 >= x0 && ir1 < x0 + w) {
            if (ia0 >= y0 && ia0 < y0 + h) {
                gamma0ReferenceArea[ia0 - y0][ir1 - x0] += wr * wac * gamma0Area;
                if(outputSigma0) {
                    sigma0ReferenceArea[ia0 - y0][ir1 - x0] += wr * wac * sigma0Area;
                }
            }
            if (ia1 >= y0 && ia1 < y0 + h) {
                gamma0ReferenceArea[ia1 - y0][ir1 - x0] += wr * wa * gamma0Area;
                if(outputSigma0) {
                    sigma0ReferenceArea[ia1 - y0][ir1 - x0] += wr * wa * sigma0Area;
                }
            }
        }
    }

    private void saveIlluminationArea2(final int x0, final int y0, final int w, final int h,
                                       final double azimuthIndex, final double rangeIndex,
                                       final double gamma0Area, final double[][] gamma0ReferenceArea,
                                       final double sigma0Area, final double[][] sigma0ReferenceArea) {

        final int ia0 = (int) azimuthIndex;
        final int ir0 = (int) rangeIndex;
        if (azimuthIndex - ia0 == 0.0 && rangeIndex - ir0 == 0.0) {
            gamma0ReferenceArea[ia0 - y0][ir0 - x0] = gamma0Area;
            return;
        }

        final int[] y = new int[4];
        y[0] = ia0 - 1;
        y[1] = ia0;
        y[2] = ia0 + 1;
        y[3] = ia0 + 2;

        final int[] x = new int[4];
        x[0] = ir0 - 1;
        x[1] = ir0;
        x[2] = ir0 + 1;
        x[3] = ir0 + 2;

        double[][] weight = new double[4][4];
        double totalWeight = 0.0;
        for (int i = 0; i < 4; ++i) {
            final double dy = y[i] - azimuthIndex;
            for (int j = 0; j < 4; ++j) {
                final double dx = x[j] - rangeIndex;
                weight[i][j] = 1.0 / (dx*dx + dy*dy);
                totalWeight += weight[i][j];
            }
        }

        for (int i = 0; i < 4; ++i) {
            if (y[i] < y0 || y[i] >= y0 + h) {
                continue;
            }
            for (int j = 0; j < 4; ++j) {
                if (x[j] < x0 || x[j] >= x0 + w) {
                    continue;
                }
                gamma0ReferenceArea[y[i] - y0][x[j] - x0] += weight[i][j] / totalWeight * gamma0Area;
            }
        }
    }

    /**
     * Compute elevation angle (in degree).
     *
     * @param earthPoint The coordinate for target on earth surface.
     * @param sensorPos  The coordinate for satellite position.
     * @return The elevation angle in degree.
     */
    private static double computeElevationAngle(final PosVector earthPoint, final PosVector sensorPos) {

        final double xDiff = sensorPos.x - earthPoint.x;
        final double yDiff = sensorPos.y - earthPoint.y;
        final double zDiff = sensorPos.z - earthPoint.z;
        final double slantRange = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
        final double H2 = sensorPos.x * sensorPos.x + sensorPos.y * sensorPos.y + sensorPos.z * sensorPos.z;
        final double R2 = earthPoint.x * earthPoint.x + earthPoint.y * earthPoint.y + earthPoint.z * earthPoint.z;

        return FastMath.acos((slantRange * slantRange + H2 - R2) / (2 * slantRange * Math.sqrt(H2))) * Constants.RTOD;
    }

    /**
     * Compute local illuminated area for given point.
     *
     * @param lg             Local geometry information.
     * @param demNoDataValue Invalid DEM value.
     * @return True if computed local illuminated area != nodatavalue.
     */
    private boolean computeIlluminatedArea(
            final LocalGeometry lg, final Double demNoDataValue, final double noDataValue, final int j,
            final double[] gamma0Area, final double[] sigma0Area) {

        if (demNoDataValue.equals(lg.t00Height) || demNoDataValue.equals(lg.t01Height) ||
                demNoDataValue.equals(lg.t10Height) || demNoDataValue.equals(lg.t11Height)) {
            gamma0Area[j] = noDataValue;
            if(outputSigma0) {
                sigma0Area[j] = noDataValue;
            }
            return false;
        }

        final PosVector t00 = lg.t00geo2xyzWGS84.getXYZ(lg.t00Lon, lg.t00Height);
        final PosVector t01 = lg.t01geo2xyzWGS84.getXYZ(lg.t01Lon, lg.t01Height);
        final PosVector t10 = lg.t00geo2xyzWGS84.getXYZ(lg.t10Lon, lg.t10Height);
        final PosVector t11 = lg.t01geo2xyzWGS84.getXYZ(lg.t11Lon, lg.t11Height);

        // compute slant range direction
        final PosVector s = new PosVector(
                lg.sensorPos.x - lg.centerPoint.x,
                lg.sensorPos.y - lg.centerPoint.y,
                lg.sensorPos.z - lg.centerPoint.z);

        Maths.normalizeVector(s);

        // project points t00, t01, t10 and t11 to the plane that perpendicular to slant range
        final double t00s = Maths.innerProduct(t00, s);
        final double t01s = Maths.innerProduct(t01, s);
        final double t10s = Maths.innerProduct(t10, s);
        final double t11s = Maths.innerProduct(t11, s);

        final double[] p00 = {t00.x - t00s * s.x, t00.y - t00s * s.y, t00.z - t00s * s.z};
        final double[] p01 = {t01.x - t01s * s.x, t01.y - t01s * s.y, t01.z - t01s * s.z};
        final double[] p10 = {t10.x - t10s * s.x, t10.y - t10s * s.y, t10.z - t10s * s.z};
        final double[] p11 = {t11.x - t11s * s.x, t11.y - t11s * s.y, t11.z - t11s * s.z};

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
        gamma0Area[j] =  Math.sqrt(h1 * (h1 - p00p01) * (h1 - p00p10) * (h1 - p10p01)) +
                Math.sqrt(h2 * (h2 - p11p01) * (h2 - p11p10) * (h2 - p10p01));

        if(outputSigma0) {
            final double[] T00 = {t00.x, t00.y, t00.z};
            final double[] T01 = {t01.x, t01.y, t01.z};
            final double[] T10 = {t10.x, t10.y, t10.z};
            final double[] T11 = {t11.x, t11.y, t11.z};

            // compute distances between projected points
            final double T00T01 = distance(T00, T01);
            final double T00T10 = distance(T00, T10);
            final double T11T01 = distance(T11, T01);
            final double T11T10 = distance(T11, T10);
            final double T10T01 = distance(T10, T01);

            // compute semi-perimeters of two triangles: T00-T01-T10 and T11-T01-T10
            final double hh1 = 0.5 * (T00T01 + T00T10 + T10T01);
            final double hh2 = 0.5 * (T11T01 + T11T10 + T10T01);

            // compute the illuminated area
            sigma0Area[j] = Math.sqrt(hh1 * (hh1 - T00T01) * (hh1 - T00T10) * (hh1 - T10T01)) +
                    Math.sqrt(hh2 * (hh2 - T11T01) * (hh2 - T11T10) * (hh2 - T10T01));
        }
        return true;
    }

    private static double distance(final double[] p1, final double[] p2) {
        return Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) +
                (p1[1] - p2[1]) * (p1[1] - p2[1]) +
                (p1[2] - p2[2]) * (p1[2] - p2[2]));
    }


    public static class LocalGeometry {
        final double t00Lat;
        double t00Lon;
        double t00Height;
        double t01Lat;
        double t01Lon;
        double t01Height;
        double t10Lat;
        double t10Lon;
        double t10Height;
        double t11Lat;
        double t11Lon;
        double t11Height;
        PosVector sensorPos;
        PosVector centerPoint;
        private double delta;
        GeoUtils.Geo2xyzWGS84 t00geo2xyzWGS84;
        GeoUtils.Geo2xyzWGS84 t01geo2xyzWGS84;

        LocalGeometry(final int x0, final int y0, final int x, final int y,
                             final TileGeoreferencing tileGeoRef, final double[][] localDEM,
                             final PositionData posData) {

            final GeoPos geo = new GeoPos();
            final int yy = y - y0 + 1;
            final int xx = x - x0 + 1;

            tileGeoRef.getGeoPos(x, y, geo);
            this.t00Lat = geo.lat;
            this.t00Lon = geo.lon;
            this.t00Height = localDEM[yy][xx];

            tileGeoRef.getGeoPos(x, y - 1, geo);
            this.t01Lat = geo.lat;
            this.t01Lon = geo.lon;
            this.t01Height = localDEM[yy - 1][xx];

            tileGeoRef.getGeoPos(x + 1, y, geo);
            this.t10Lat = geo.lat;
            this.t10Lon = geo.lon;
            this.t10Height = localDEM[yy][xx + 1];

            tileGeoRef.getGeoPos(x + 1, y - 1, geo);
            this.t11Lat = geo.lat;
            this.t11Lon = geo.lon;
            this.t11Height = localDEM[yy - 1][xx + 1];

            this.centerPoint = posData.earthPoint;
            this.sensorPos = posData.sensorPos;
        }

        LocalGeometry(final double lat, final double del) {
            this.delta = del;

            this.t00Lat = lat;

            this.t01Lat = lat + del;

            this.t10Lat = lat;

            this.t11Lat = lat + del;

            this.t00geo2xyzWGS84 = new GeoUtils.Geo2xyzWGS84(t00Lat);
            this.t01geo2xyzWGS84 = new GeoUtils.Geo2xyzWGS84(t01Lat);
        }

        void setLon(final double lon, final double alt00, final double alt01, final double alt10, final double alt11,
                    final PositionData posData) {

            this.t00Lon = lon;
            this.t00Height = alt00;

            this.t01Lon = lon;
            this.t01Height = alt01; //dem.getElevation(new GeoPos(t01Lat, t01Lon));

            this.t10Lon = lon + delta;
            this.t10Height = alt10;//dem.getElevation(new GeoPos(t10Lat, t10Lon));

            this.t11Lon = lon + delta;
            this.t11Height = alt11;//dem.getElevation(new GeoPos(t11Lat, t11Lon));

            this.centerPoint = posData.earthPoint;
            this.sensorPos = posData.sensorPos;
        }
    }

    private static class PositionData {
        PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
        double slantRange;
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

    private static class ResamplingRaster implements Resampling.Raster {

        private final double[][] data;
        private final double noDataValue;

        public ResamplingRaster(final double demNoDataValue, final double[][] data) {
            this.data = data;
            this.noDataValue = demNoDataValue;
        }

        public final int getWidth() {
            return data[0].length;
        }

        public final int getHeight() {
            return data.length;
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;

            try {
                double val;
                int i = 0;
                while (i < y.length) {
                    int j = 0;
                    while (j < x.length) {
                        val = data[y[i]][x[j]];
                        if (noDataValue == val) {
                            val = Double.NaN;
                            allValid = false;
                        }
                        samples[i][j] = val;
                        ++j;
                    }
                    ++i;
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
                allValid = false;
            }

            return allValid;
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
            super(TerrainFlatteningOp.class);
        }
    }
}
