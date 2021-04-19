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
import org.esa.s1tbx.calibration.gpf.calibrators.Sentinel1Calibrator;
import org.esa.s1tbx.calibration.gpf.support.CalibrationFactory;
import org.esa.s1tbx.calibration.gpf.support.Calibrator;
import org.esa.s1tbx.commons.CRSGeoCodingHandler;
import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.s1tbx.commons.SARUtils;
import org.esa.s1tbx.insar.gpf.support.SARPosition;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
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
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.eo.LocalGeometry;
import org.esa.snap.engine_utilities.gpf.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Raw SAR images usually contain significant geometric distortions. One of the factors that cause the
 * distortions is the ground elevation of the targets. This operator corrects the topographic distortion
 * in the raw image caused by this factor. The operator implements the Range-Doppler (RD) geocoding method.
 * <p/>
 * The method consists of the following major steps:
 * (1) Get state vectors from the metadata;
 * (2) Compute satellite position and velocity for each azimuth time by interpolating the state vectors;
 * (3) Get corner latitudes and longitudes for the source image;
 * (4) Compute [LatMin, LatMax] and [LonMin, LonMax];
 * (5) Get the range and azimuth spacings for the source image;
 * (6) Compute DEM traversal sample intervals (delLat, delLon) based on source image pixel spacing;
 * (7) Compute target geocoded image dimension;
 * (8) Repeat the following steps for each sample in the target raster [LatMax:-delLat:LatMin]x[LonMin:delLon:LonMax]:
 * (8.1) Get local elevation h(i,j) for current sample given local latitude lat(i,j) and longitude lon(i,j);
 * (8.2) Convert (lat(i,j), lon(i,j), h(i,j)) to global Cartesian coordinates p(Px, Py, Pz);
 * (8.3) Compute zero Doppler time t(i,j) for point p(Px, Py, Pz) using Doppler frequency function;
 * (8.4) Compute satellite position s(i,j) and slant range r(i,j) = |s(i,j) - p| for zero Doppler time t(i,j);
 * (8.5) Compute bias-corrected zero Doppler time tc(i,j) = t(i,j) + r(i,j)*2/c, where c is the light speed;
 * (8.6) Update satellite position s(tc(i,j)) and slant range r(tc(i,j)) = |s(tc(i,j)) - p| for time tc(i,j);
 * (8.7) Compute azimuth image index Ia using zero Doppler time tc(i,j);
 * (8.8) Compute range image index Ir using slant range r(tc(i,j)) or ground range;
 * (8.9) Compute pixel value x(Ia,Ir) using interpolation and save it for current sample.
 * <p/>
 * Reference: Guide to ASAR Geocoding, Issue 1.0, 19.03.2008
 */

@OperatorMetadata(alias = "Terrain-Correction",
        category = "Radar/Geometric/Terrain Correction",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "RD method for orthorectification")
public class RangeDopplerGeocodingOp extends Operator {

    @SourceProduct(alias = "source")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "External DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(label = "External DEM Apply EGM", defaultValue = "true")
    private Boolean externalDEMApplyEGM = true;

    @Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label = "DEM Resampling Method",
            valueSet = {
                    ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                    ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                    ResamplingFactory.CUBIC_CONVOLUTION_NAME,
                    ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BISINC_11_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BISINC_21_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
                    DEMFactory.DELAUNAY_INTERPOLATION
            })
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label = "Image Resampling Method",
            valueSet = {
                    ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                    ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                    ResamplingFactory.CUBIC_CONVOLUTION_NAME,
                    ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BISINC_11_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BISINC_21_POINT_INTERPOLATION_NAME,
                    ResamplingFactory.BICUBIC_INTERPOLATION_NAME
            })
    private String imgResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(description = "The pixel spacing in meters", defaultValue = "0", label = "Pixel Spacing (m)")
    private double pixelSpacingInMeter = 0;

    @Parameter(description = "The pixel spacing in degrees", defaultValue = "0", label = "Pixel Spacing (deg)")
    private double pixelSpacingInDegree = 0;

    @Parameter(description = "The coordinate reference system in well known text format", defaultValue = "WGS84(DD)")
    private String mapProjection = "WGS84(DD)";

    @Parameter(description = "Force the image grid to be aligned with a specific point", defaultValue = "false")
    private boolean alignToStandardGrid = false;

    @Parameter(description = "x-coordinate of the standard grid's origin point", defaultValue = "0")
    private double standardGridOriginX = 0;

    @Parameter(description = "y-coordinate of the standard grid's origin point", defaultValue = "0")
    private double standardGridOriginY = 0;

    @Parameter(defaultValue = "true", label = "Mask out areas with no elevation", description = "Mask the sea with no data value (faster)")
    private boolean nodataValueAtSea = true;

    @Parameter(defaultValue = "false", label = "Save DEM as band")
    private boolean saveDEM = false;

    @Parameter(defaultValue = "false", label = "Save latitude and longitude as band")
    private boolean saveLatLon = false;

    @Parameter(defaultValue = "false", label = "Save incidence angle from ellipsoid as band")
    private boolean saveIncidenceAngleFromEllipsoid = false;

    @Parameter(defaultValue = "false", label = "Save local incidence angle as band")
    private boolean saveLocalIncidenceAngle = false;

    @Parameter(defaultValue = "false", label = "Save projected local incidence angle as band")
    private boolean saveProjectedLocalIncidenceAngle = false;

    @Parameter(defaultValue = "true", label = "Save selected source band")
    private boolean saveSelectedSourceBand = true;

    @Parameter(defaultValue = "false", label = "Save layover shadow mask")
    private boolean saveLayoverShadowMask = false;

    @Parameter(defaultValue = "false", label = "Output complex data")
    private boolean outputComplex = false;

    @Parameter(defaultValue = "false", label = "Apply radiometric normalization")
    private boolean applyRadiometricNormalization = false;

    @Parameter(defaultValue = "false", label = "Save Sigma0 as a band")
    private boolean saveSigmaNought = false;

    @Parameter(defaultValue = "false", label = "Save Gamma0 as a band")
    private boolean saveGammaNought = false;

    @Parameter(defaultValue = "false", label = "Save Beta0 as a band")
    private boolean saveBetaNought = false;

    @Parameter(valueSet = {SARGeocoding.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, SARGeocoding.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label = "")
    private String incidenceAngleForSigma0 = SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {SARGeocoding.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, SARGeocoding.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label = "")
    private String incidenceAngleForGamma0 = SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {CalibrationOp.LATEST_AUX, CalibrationOp.PRODUCT_AUX, CalibrationOp.EXTERNAL_AUX},
            description = "The auxiliary file", defaultValue = CalibrationOp.LATEST_AUX, label = "Auxiliary File")
    private String auxFile = CalibrationOp.LATEST_AUX;

    @Parameter(description = "The antenne elevation pattern gain auxiliary data file.", label = "External Aux File")
    private File externalAuxFile = null;

    private MetadataElement absRoot = null;
    private ElevationModel dem = null;
    private Band elevationBand = null;
    private double demNoDataValue = 0.0f; // no data value for DEM
    private GeoCoding targetGeoCoding = null;

    private boolean srgrFlag = false;
    private boolean isElevationModelAvailable = false;
    private boolean usePreCalibrationOp = false;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int targetImageWidth = 0;
    private int targetImageHeight = 0;
    private int margin = 0;

    private double avgSceneHeight = 0.0; // in m
    private double wavelength = 0.0; // in m
    private double rangeSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lastLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private double nearEdgeSlantRange = 0.0; // in m

    private CoordinateReferenceSystem targetCRS;
    private double delLat = 0.0;
    private double delLon = 0.0;
    private OrbitStateVectors orbit = null;

    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    private OrbitStateVector[] orbitStateVectors = null;
    private final HashMap<String, Band[]> targetBandNameToSourceBand = new HashMap<>();
    private final Map<String, Boolean> targetBandApplyRadiometricNormalizationFlag = new HashMap<>();
    private final Map<String, Boolean> targetBandApplyRetroCalibrationFlag = new HashMap<>();
    private TiePointGrid incidenceAngle = null;
    private static final int INVALID_SUB_SWATH_INDEX = -1;

    private Resampling imgResampling = null;

    boolean useAvgSceneHeight = false;
    private Calibrator calibrator = null;
    private boolean orthoDataProduced = false;  // check if any ortho data is actually produced
    private boolean processingStarted = false;
    private boolean isPolsar = false;

    private boolean nearRangeOnLeft = true; // temp fix for descending Radarsat2
    private String mission = null;
    private boolean skipBistaticCorrection = false;

    private boolean isLayoverShadowMaskAvailable = false;
    private byte[][] layoverShadowMask = null;

    public static final String externalDEMStr = "External DEM";
    private static final String PRODUCT_SUFFIX = "_TC";

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
            validator.checkIfSARProduct();
            validator.checkIfMapProjected(false);
            validator.checkIfTOPSARBurstProduct(false);

            checkUserInput();

            getSourceImageDimension();

            getMetadata();

            if (useAvgSceneHeight) {
                saveSigmaNought = false;
                saveBetaNought = false;
                saveGammaNought = false;
                saveDEM = false;
                saveLocalIncidenceAngle = false;
                saveProjectedLocalIncidenceAngle = false;
            }

            imgResampling = ResamplingFactory.createResampling(imgResamplingMethod);
            if (imgResampling == null) {
                throw new OperatorException("Resampling method " + imgResamplingMethod + " is invalid");
            }

            createTargetProduct();

            computeSensorPositionsAndVelocities();

            if (saveSigmaNought) {
                calibrator = CalibrationFactory.createCalibrator(sourceProduct);

                if (calibrator instanceof Sentinel1Calibrator) {
                    final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
                    final Set<String> polList = new HashSet<>();
                    for (Band band : sourceBands) {
                        polList.add(OperatorUtils.getBandPolarization(band.getName(), absRoot));
                    }
                    final String[] selectedPolarisations = polList.toArray(new String[polList.size()]);

                    Sentinel1Calibrator cal = (Sentinel1Calibrator) calibrator;
                    cal.setUserSelections(sourceProduct,
                                          selectedPolarisations, saveSigmaNought, saveGammaNought, saveBetaNought, false);
                }

                calibrator.setAuxFileFlag(auxFile);
                calibrator.setExternalAuxFile(externalAuxFile);
                calibrator.initialize(this, sourceProduct, targetProduct, true, true);
                calibrator.setIncidenceAngleForSigma0(incidenceAngleForSigma0);
            }

            updateTargetProductMetadata();

            if (!demName.contains(externalDEMStr) && !useAvgSceneHeight) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            if (demName.contains(externalDEMStr) && externalDEMFile == null) {
                throw new OperatorException("External DEM file is not specified. ");
            }

            if (!useAvgSceneHeight) {
                DEMFactory.validateDEM(demName, sourceProduct);
            }

            margin = getMargin();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public void dispose() throws OperatorException {
        if (dem != null) {
            dem.dispose();
        }

        if (!orthoDataProduced && processingStarted) {
            final String errMsg = getId() + " error: no valid output was produced. Please verify the DEM";
            SystemUtils.LOG.warning(errMsg);
        }
    }

    private void checkUserInput() {

        if (!saveSelectedSourceBand && !applyRadiometricNormalization) {
            throw new OperatorException("Please select output band for terrain corrected image");
        }

        if (!applyRadiometricNormalization) {
            saveSigmaNought = false;
            saveGammaNought = false;
            saveBetaNought = false;
        }

        if (saveBetaNought || saveGammaNought ||
                (saveSigmaNought && incidenceAngleForSigma0.contains(SARGeocoding.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) ||
                (saveSigmaNought && incidenceAngleForSigma0.contains(SARGeocoding.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM))) {
            saveSigmaNought = true;
            saveProjectedLocalIncidenceAngle = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(SARGeocoding.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) ||
                (saveSigmaNought && incidenceAngleForSigma0.contains(SARGeocoding.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID))) {
            saveIncidenceAngleFromEllipsoid = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(SARGeocoding.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) ||
                (saveSigmaNought && incidenceAngleForSigma0.contains(SARGeocoding.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM))) {
            saveLocalIncidenceAngle = true;
        }

        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
    }

    /**
     * Retrieve required data from Abstracted Metadata
     *
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {
        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = getMissionType(absRoot);

        skipBistaticCorrection = absRoot.getAttributeInt(AbstractMetadata.bistatic_correction_applied, 0) == 1;

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);

        wavelength = SARUtils.getRadarWavelength(absRoot);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        if (rangeSpacing <= 0.0) {
            throw new OperatorException("Invalid input for range pixel spacing: " + rangeSpacing);
        }

        firstLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days
        lastLineUTC = AbstractMetadata.parseUTC(absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days
        lineTimeInterval = (lastLineUTC - firstLineUTC) / (sourceImageHeight - 1); // in days
        if (lineTimeInterval == 0.0) {
            throw new OperatorException("Invalid input for Line Time Interval: " + lineTimeInterval);
        }

        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        if (orbitStateVectors == null || orbitStateVectors.length == 0) {
            throw new OperatorException("Invalid Obit State Vectors");
        }

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
            if (srgrConvParams == null || srgrConvParams.length == 0) {
                throw new OperatorException("Invalid SRGR Coefficients");
            }
        } else {
            nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);
        }

        // used for retro-calibration or when useAvgSceneHeight is true
        avgSceneHeight = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.avg_scene_height);

        MetadataAttribute attribute = absRoot.getAttribute("retro-calibration performed flag");
        if (attribute != null) {
            usePreCalibrationOp = true;
            if (!applyRadiometricNormalization) {
                throw new OperatorException("Apply radiometric normalization must be selected.");
            }
        } else {
            final boolean multilookFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.multilook_flag);
            if (applyRadiometricNormalization && (mission.equals("ERS1") || mission.equals("ERS2")) && !multilookFlag) {
                throw new OperatorException("For radiometric normalization of ERS product, please first use\n" +
                                                    "  'Remove Antenna Pattern' operator to remove calibration factors applied and apply ADC,\n" +
                                                    "  then apply 'Range-Doppler Terrain Correction' operator; or use one of the following\n" +
                                                    "  user graphs: 'RemoveAntPat_Orthorectify' or 'RemoveAntPat_Multilook_Orthorectify'.");
            }
        }

        nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);

        isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;
    }

    /**
     * Get the mission type.
     *
     * @param absRoot the AbstractMetadata
     * @return the mission string
     */
    public static String getMissionType(final MetadataElement absRoot) {
        return absRoot.getAttributeString(AbstractMetadata.MISSION);
    }

    /**
     * Get elevation model.
     *
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) return;
        if (demName.contains(externalDEMStr) && externalDEMFile != null) { // if external DEM file is specified by user

            dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
            ((FileElevationModel) dem).applyEarthGravitionalModel(externalDEMApplyEGM);
            demNoDataValue = externalDEMNoDataValue;
            demName = externalDEMFile.getName();

        } else {

            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
        }

        if (elevationBand != null) {
            elevationBand.setNoDataValue(demNoDataValue);
            elevationBand.setNoDataValueUsed(true);
        }

        isElevationModelAvailable = true;
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    protected String getProductSuffix() {
        return PRODUCT_SUFFIX;
    }

    private void createTargetProduct() {
        try {
            if (pixelSpacingInMeter <= 0.0 && pixelSpacingInDegree <= 0) {
                pixelSpacingInMeter = Math.max(SARGeocoding.getAzimuthPixelSpacing(sourceProduct),
                                               SARGeocoding.getRangePixelSpacing(sourceProduct));
                pixelSpacingInDegree = SARGeocoding.getPixelSpacingInDegree(pixelSpacingInMeter);
            }
            if (pixelSpacingInMeter <= 0.0) {
                pixelSpacingInMeter = SARGeocoding.getPixelSpacingInMeter(pixelSpacingInDegree);
            }
            if (pixelSpacingInDegree <= 0) {
                pixelSpacingInDegree = SARGeocoding.getPixelSpacingInDegree(pixelSpacingInMeter);
            }
            delLat = pixelSpacingInDegree;
            delLon = pixelSpacingInDegree;

            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(sourceProduct, mapProjection,
                                                                           pixelSpacingInDegree, pixelSpacingInMeter,
                                                                           alignToStandardGrid, standardGridOriginX, standardGridOriginY);

            targetCRS = crsHandler.getTargetCRS();

            targetProduct = new Product(sourceProduct.getName() + getProductSuffix(),
                                        sourceProduct.getProductType(), crsHandler.getTargetWidth(), crsHandler.getTargetHeight());
            targetProduct.setSceneGeoCoding(crsHandler.getCrsGeoCoding());

            targetImageWidth = targetProduct.getSceneRasterWidth();
            targetImageHeight = targetProduct.getSceneRasterHeight();

            addSelectedBands();

            targetGeoCoding = targetProduct.getSceneGeoCoding();

            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
            ProductUtils.copyVectorData(sourceProduct, targetProduct);

            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());
            targetProduct.setDescription(sourceProduct.getDescription());

            try {
                ProductUtils.copyIndexCodings(sourceProduct, targetProduct);
            } catch (Exception e) {
                if (!imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {
                    throw new OperatorException("Use Nearest Neighbour with Classifications: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Compute sensor position and velocity for each range line.
     */
    private void computeSensorPositionsAndVelocities() {

        orbit = new OrbitStateVectors(orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
        boolean noBandsSelected = sourceBandNames == null || sourceBandNames.length == 0;

        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();
            String targetBandName;

            if (unit != null && !isPolsar && !outputComplex &&
                    (unit.equals(Unit.REAL) || unit.equals(Unit.IMAGINARY))) {

                if (i == sourceBands.length - 1) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !((unit.equals(Unit.REAL) && nextUnit.equals(Unit.IMAGINARY)) ||
                        (unit.equals(Unit.IMAGINARY) && nextUnit.equals(Unit.REAL)))) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final Band[] srcBands = new Band[2];
                srcBands[0] = srcBand;
                srcBands[1] = sourceBands[i + 1];
                final String pol = OperatorUtils.getBandPolarization(srcBand.getName(), absRoot);
                final String suffix = OperatorUtils.getSuffixFromBandName(srcBand.getName());

                if (saveSigmaNought) {
                    if (suffix != null && !suffix.isEmpty() && !isPolsar) {
                        if (pol != null && !pol.isEmpty() && !suffix.contains(pol) && !suffix.contains(pol.toUpperCase())) {
                            targetBandName = "Sigma0_" + suffix + '_' + pol.toUpperCase();
                        } else {
                            targetBandName = "Sigma0_" + suffix;
                        }
                    } else {
                        targetBandName = "Sigma0";
                    }
                    if (addTargetBand(targetBandName, Unit.INTENSITY, srcBand) != null) {
                        targetBandNameToSourceBand.put(targetBandName, srcBands);
                        targetBandApplyRadiometricNormalizationFlag.put(targetBandName, true);
                        if (usePreCalibrationOp) {
                            targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                        } else {
                            targetBandApplyRetroCalibrationFlag.put(targetBandName, true);
                        }
                    }
                }

                if (saveSelectedSourceBand) {
                    if (suffix != null && !suffix.isEmpty() && !isPolsar) {
                        if (pol != null && !pol.isEmpty() && !suffix.contains(pol) && !suffix.contains(pol.toUpperCase())) {
                            targetBandName = "Intensity_" + suffix + '_' + pol.toUpperCase();
                        } else {
                            targetBandName = "Intensity_" + suffix;
                        }
                    } else {
                        targetBandName = "Intensity";
                    }
                    if (addTargetBand(targetBandName, Unit.INTENSITY, srcBand) != null) {
                        targetBandNameToSourceBand.put(targetBandName, srcBands);
                        targetBandApplyRadiometricNormalizationFlag.put(targetBandName, false);
                        targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                    }
                }
                ++i;

            } else {

                final Band[] srcBands = {srcBand};
                final String pol = OperatorUtils.getBandPolarization(srcBand.getName(), absRoot);
                final String suffix = OperatorUtils.getSuffixFromBandName(srcBand.getName());
                if (saveSigmaNought) {
                    targetBandName = "Sigma0";
                    if (suffix != null && !suffix.isEmpty() && !isPolsar) {
                        if (pol != null && !pol.isEmpty() && !suffix.contains(pol) && !suffix.contains(pol.toUpperCase())) {
                            targetBandName += '_' + suffix + '_' + pol.toUpperCase();
                        } else {
                            targetBandName += '_' + suffix;
                        }
                    }
                    if (addTargetBand(targetBandName, Unit.INTENSITY, srcBand) != null) {
                        targetBandNameToSourceBand.put(targetBandName, srcBands);
                        targetBandApplyRadiometricNormalizationFlag.put(targetBandName, true);
                        if (usePreCalibrationOp) {
                            targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                        } else {
                            targetBandApplyRetroCalibrationFlag.put(targetBandName, true);
                        }
                    }
                }

                if (saveSelectedSourceBand) {
                    targetBandName = srcBand.getName();
                    if (pol != null && !pol.isEmpty() && !isPolsar && !srcBand.getName().toLowerCase().contains(pol)) {
                        targetBandName += '_' + pol.toUpperCase();
                    }
                    int dataType = ProductData.TYPE_FLOAT32;
                    // use original dataType for nearest neighbour and indexCoding bands
                    if (imgResampling.equals(Resampling.NEAREST_NEIGHBOUR))
                        dataType = srcBand.getDataType();
                    if (addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                                      targetBandName, unit, srcBand, dataType) != null) {
                        targetBandNameToSourceBand.put(targetBandName, srcBands);
                        targetBandApplyRadiometricNormalizationFlag.put(targetBandName, false);
                        targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                    }

                    if (outputComplex && noBandsSelected && srcBand.getUnit().equals(Unit.IMAGINARY)) { // add virtual bands

                        int idx = sourceProduct.getBandIndex(srcBand.getName());
                        Band band = sourceProduct.getBandAt(idx + 1);
                        if (band != null && band instanceof VirtualBand) {
                            VirtualBand srcVirtBand = (VirtualBand) band;

                            final VirtualBand virtBand = new VirtualBand(srcVirtBand.getName(), srcVirtBand.getDataType(),
                                                                         targetImageWidth, targetImageHeight, srcVirtBand.getExpression());
                            virtBand.setUnit(srcVirtBand.getUnit());
                            virtBand.setDescription(srcVirtBand.getDescription());
                            virtBand.setNoDataValue(srcVirtBand.getNoDataValue());
                            virtBand.setNoDataValueUsed(srcVirtBand.isNoDataValueUsed());
                            virtBand.setOwner(targetProduct);
                            targetProduct.addBand(virtBand);
                        }
                    }
                }
            }
        }

        if (saveDEM) {
            elevationBand = addTargetBand("elevation", Unit.METERS, null);
        }

        if (saveLatLon) {
            addTargetBand("latitude", Unit.DEGREES, null);
            addTargetBand("longitude", Unit.DEGREES, null);
        }

        if (saveLocalIncidenceAngle) {
            addTargetBand("localIncidenceAngle", Unit.DEGREES, null);
        }

        if (saveProjectedLocalIncidenceAngle) {
            addTargetBand("projectedLocalIncidenceAngle", Unit.DEGREES, null);
        }

        if (saveIncidenceAngleFromEllipsoid) {
            addTargetBand("incidenceAngleFromEllipsoid", Unit.DEGREES, null);
        }

        if (saveSigmaNought && !incidenceAngleForSigma0.contains(SARGeocoding.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM)) {
            CalibrationFactory.createSigmaNoughtVirtualBand(targetProduct, incidenceAngleForSigma0);
        }

        if (saveGammaNought) {
            CalibrationFactory.createGammaNoughtVirtualBand(targetProduct, incidenceAngleForGamma0);
        }

        if (saveBetaNought) {
            CalibrationFactory.createBetaNoughtVirtualBand(targetProduct);
        }

        if (saveLayoverShadowMask) {
            addTargetBand(targetProduct, targetImageWidth, targetImageHeight,"layoverShadowMask",
                    Unit.BIT, null, ProductData.TYPE_INT8);
        }
    }

    private Band addTargetBand(final String bandName, final String bandUnit, final Band sourceBand) {
        return addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                             bandName, bandUnit, sourceBand, ProductData.TYPE_FLOAT32);
    }

    static Band addTargetBand(final Product targetProduct, final int targetImageWidth, final int targetImageHeight,
                              final String bandName, final String bandUnit, final Band sourceBand,
                              final int dataType) {

        if (targetProduct.getBand(bandName) == null) {

            final Band targetBand = new Band(bandName, dataType, targetImageWidth, targetImageHeight);

            targetBand.setUnit(bandUnit);
            if (sourceBand != null) {
                targetBand.setDescription(sourceBand.getDescription());
                targetBand.setNoDataValue(sourceBand.getNoDataValue());
            }
            targetBand.setNoDataValueUsed(true);
            targetProduct.addBand(targetBand);
            return targetBand;
        }

        return null;
    }

    /**
     * Update metadata in the target product.
     *
     * @throws OperatorException The exception.
     */
    private void updateTargetProductMetadata() throws Exception {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.srgr_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);

        final GeoPos geoPosFirstNear = targetGeoCoding.getGeoPos(new PixelPos(0, 0), null);
        final GeoPos geoPosFirstFar = targetGeoCoding.getGeoPos(new PixelPos(targetImageWidth - 1, 0), null);
        final GeoPos geoPosLastNear = targetGeoCoding.getGeoPos(new PixelPos(0, targetImageHeight - 1), null);
        final GeoPos geoPosLastFar = targetGeoCoding.getGeoPos(new PixelPos(targetImageWidth - 1, targetImageHeight - 1), null);

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_lat, geoPosFirstNear.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_lat, geoPosFirstFar.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_lat, geoPosLastNear.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_lat, geoPosLastFar.getLat());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_long, geoPosFirstNear.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_long, geoPosFirstFar.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_long, geoPosLastNear.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_long, geoPosLastFar.getLon());
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(targetProduct));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.map_projection, targetCRS.getName().getCode());
        if (!useAvgSceneHeight) {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.is_terrain_corrected, 1);
            if (externalDEMFile != null) { // if external DEM file is specified by user
                AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
            } else {
                AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
            }
        }

        // map projection too
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.geo_ref_system, "WGS84");
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lat_pixel_res, delLat);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lon_pixel_res, delLon);

        if (pixelSpacingInMeter > 0.0 &&
                Double.compare(pixelSpacingInMeter, SARGeocoding.getPixelSpacing(sourceProduct)) != 0) {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, pixelSpacingInMeter);
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, pixelSpacingInMeter);
        }

        // save look directions for 5 range lines
        final MetadataElement lookDirectionListElem = new MetadataElement("Look_Direction_List");
        final int numOfDirections = 5;
        for (int i = 1; i <= numOfDirections; ++i) {
            SARGeocoding.addLookDirection("look_direction", lookDirectionListElem, i, numOfDirections, sourceImageWidth,
                                          sourceImageHeight, firstLineUTC, lineTimeInterval, nearRangeOnLeft, sourceProduct.getSceneGeoCoding());
        }
        absTgt.addElement(lookDirectionListElem);
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

        try {
            processingStarted = true;
            try {
                if (!isElevationModelAvailable) {
                    getElevationModel();
                }
            } catch (Exception e) {
                throw new OperatorException(e);
            }

            if (saveLayoverShadowMask && !isLayoverShadowMaskAvailable) {
                createLayoverShadowMask();
            }

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0 - 1, y0 - 1, w + 2, h + 2);

            double[][] localDEM = new double[h + 2][w + 2];
            if (useAvgSceneHeight) {
                DEMFactory.fillDEM(localDEM, avgSceneHeight);
            } else {
                final boolean valid = DEMFactory.getLocalDEM(
                        dem, demNoDataValue, demResamplingMethod, tileGeoRef, x0, y0, w, h, sourceProduct,
                        nodataValueAtSea, localDEM);
                if (!valid && nodataValueAtSea) {
                    for (Band targetBand : targetTiles.keySet()) {
                        ProductData data = targetTiles.get(targetBand).getRawSamples();
                        double nodatavalue = targetBand.getNoDataValue();
                        final int length = data.getNumElems();
                        for (int i = 0; i < length; ++i) {
                            data.setElemDoubleAt(i, nodatavalue);
                        }
                    }
                    return;
                }
            }

            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h, tileGeoRef, localDEM);

            final GeoPos geoPos = new GeoPos();
            final PositionData posData = new PositionData();
            final int srcMaxRange = sourceImageWidth - 1;
            final int srcMaxAzimuth = sourceImageHeight - 1;
            ProductData demBuffer = null, latBuffer = null, lonBuffer = null, localIncidenceAngleBuffer = null,
                    projectedLocalIncidenceAngleBuffer = null, incidenceAngleFromEllipsoidBuffer = null,
                    layoverShadowMaskBuffer = null;

            final List<TileData> tgtTileList = new ArrayList<>();
            final Set<Band> keySet = targetTiles.keySet();
            for (Band targetBand : keySet) {

                if (targetBand.getName().equals("elevation")) {
                    demBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("latitude")) {
                    latBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("longitude")) {
                    lonBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("localIncidenceAngle")) {
                    localIncidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("projectedLocalIncidenceAngle")) {
                    projectedLocalIncidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("incidenceAngleFromEllipsoid")) {
                    incidenceAngleFromEllipsoidBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("layoverShadowMask")) {
                    layoverShadowMaskBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                final Band[] srcBands = targetBandNameToSourceBand.get(targetBand.getName());
                Tile sourceTileI = null, sourceTileQ = null;
                if (sourceRectangle != null) {
                    sourceTileI = getSourceTile(srcBands[0], sourceRectangle);
                    sourceTileQ = srcBands.length > 1 ? getSourceTile(srcBands[1], sourceRectangle) : null;
                }

                final TileData td = new TileData(targetTiles.get(targetBand), srcBands, isPolsar, outputComplex,
                        targetBand.getName(), getBandUnit(targetBand.getName()), absRoot, calibrator, imgResampling,
                        sourceTileI, sourceTileQ);

                td.applyRadiometricNormalization = targetBandApplyRadiometricNormalizationFlag.get(targetBand.getName());
                td.applyRetroCalibration = targetBandApplyRetroCalibrationFlag.get(targetBand.getName());
                tgtTileList.add(td);
            }

            final TileData[] tgtTiles = tgtTileList.toArray(new TileData[tgtTileList.size()]);
            for (TileData tileData : tgtTiles) {
                if (sourceRectangle != null) {
                    try {
                        final Band[] srcBands = targetBandNameToSourceBand.get(tileData.bandName);
                        tileData.imgResamplingRaster.setSourceTiles(getSourceTile(srcBands[0], sourceRectangle),
                                srcBands.length > 1 ? getSourceTile(srcBands[1], sourceRectangle) : null);
                    } catch (Exception e) {
                        tileData.imgResamplingRaster.setSourceTiles(null, null);
                    }
                } else {
                    tileData.imgResamplingRaster.setSourceTiles(null, null);
                }
            }

            final int maxY = y0 + h;
            final int maxX = x0 + w;

            final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

            final GeoPos posFirst = targetProduct.getSceneGeoCoding().getGeoPos(new PixelPos(0,0), null);
            final GeoPos posLast = targetProduct.getSceneGeoCoding().getGeoPos(new PixelPos(0,targetImageHeight), null);
            int diffLat = (int)Math.abs(posFirst.lat - posLast.lat);

            for (int y = y0; y < maxY; y++) {
                final int yy = y - y0 + 1;
                for (int x = x0; x < maxX; x++) {
                    final int index = tgtTiles[0].targetTile.getDataBufferIndex(x, y);

                    Double alt = localDEM[yy][x - x0 + 1];
                    if (alt.equals(demNoDataValue) && !useAvgSceneHeight) {
                        if (nodataValueAtSea) {
                            saveNoDataValueToTarget(index, tgtTiles, demBuffer);
                            continue;
                        }
                    }

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    if (alt.equals(demNoDataValue) && !nodataValueAtSea) { // get corrected elevation for 0
                        alt = (double) egm.getEGM(lat, lon);
                    }

                    if (!getPosition(lat, lon, alt, posData)) {
                        saveNoDataValueToTarget(index, tgtTiles, demBuffer);
                        continue;
                    }

                    if (!SARGeocoding.isValidCell(posData.rangeIndex, posData.azimuthIndex, lat, lon, diffLat,
                            sourceProduct.getSceneGeoCoding(), srcMaxRange, srcMaxAzimuth, posData.sensorPos)) {
                        saveNoDataValueToTarget(index, tgtTiles, demBuffer);
                    } else {

                        final double[] localIncidenceAngles = {SARGeocoding.NonValidIncidenceAngle,
                                SARGeocoding.NonValidIncidenceAngle};

                        if (saveLocalIncidenceAngle || saveProjectedLocalIncidenceAngle || saveSigmaNought) {

                            final LocalGeometry localGeometry = new LocalGeometry(
                                    x, y, tileGeoRef, posData.earthPoint, posData.sensorPos);

                            SARGeocoding.computeLocalIncidenceAngle(
                                    localGeometry, demNoDataValue, saveLocalIncidenceAngle, saveProjectedLocalIncidenceAngle,
                                    saveSigmaNought, x0, y0, x, y, localDEM, localIncidenceAngles); // in degrees

                            if (saveLocalIncidenceAngle && localIncidenceAngles[0] != SARGeocoding.NonValidIncidenceAngle) {
                                localIncidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[0]);
                            }

                            if (saveProjectedLocalIncidenceAngle &&
                                    localIncidenceAngles[1] != SARGeocoding.NonValidIncidenceAngle) {
                                projectedLocalIncidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[1]);
                            }
                        }

                        if (saveDEM) {
                            demBuffer.setElemDoubleAt(index, alt);
                        }
                        if (saveLatLon) {
                            latBuffer.setElemDoubleAt(index, lat);
                            lonBuffer.setElemDoubleAt(index, lon);
                        }

                        if (saveIncidenceAngleFromEllipsoid && incidenceAngle != null) {
                            incidenceAngleFromEllipsoidBuffer.setElemDoubleAt(
                                    index, incidenceAngle.getPixelDouble(posData.rangeIndex, posData.azimuthIndex));
                        }

                        if (saveLayoverShadowMask) {
                            layoverShadowMaskBuffer.setElemIntAt(index,
                                    layoverShadowMask[(int)(posData.azimuthIndex + 0.5)][(int)(posData.rangeIndex + 0.5)]);
                        }

                        double satelliteHeight = 0;
                        double sceneToEarthCentre = 0;
                        if (saveSigmaNought) {
                            satelliteHeight = Math.sqrt(posData.sensorPos.x * posData.sensorPos.x +
                                                                posData.sensorPos.y * posData.sensorPos.y + posData.sensorPos.z * posData.sensorPos.z);

                            sceneToEarthCentre = Math.sqrt(posData.earthPoint.x * posData.earthPoint.x +
                                                                   posData.earthPoint.y * posData.earthPoint.y + posData.earthPoint.z * posData.earthPoint.z);
                        }

                        for (TileData tileData : tgtTiles) {
                            int[] subSwathIndex = {INVALID_SUB_SWATH_INDEX};
                            double v = getPixelValue(posData.azimuthIndex, posData.rangeIndex, tileData, subSwathIndex);

                            if (v != tileData.noDataValue && tileData.applyRadiometricNormalization) {
                                if (localIncidenceAngles[1] != SARGeocoding.NonValidIncidenceAngle) {
                                    v = calibrator.applyCalibration(
                                            v, posData.rangeIndex, posData.azimuthIndex, posData.slantRange,
                                            satelliteHeight, sceneToEarthCentre, localIncidenceAngles[1],
                                            tileData.bandName, tileData.bandPolar, tileData.bandUnit, subSwathIndex);
                                    // use projected incidence angle
                                } else {
                                    //v = tileData.noDataValue;
                                    saveNoDataValueToTarget(index, tgtTiles, demBuffer);
                                    continue;
                                }
                            }

                            tileData.tileDataBuffer.setElemDoubleAt(index, v);
                        }
                        orthoDataProduced = true;
                    }
                }
            }
            localDEM = null;

        } catch (Throwable e) {
            orthoDataProduced = true; //to prevent multiple error messages
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void createLayoverShadowMask() {

        if (isLayoverShadowMaskAvailable) return;

        final Dimension tileSize = new Dimension(sourceImageWidth, 10);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(sourceProduct, tileSize, 0);
        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Creating Layover/Shadow Mask... ", tileRectangles.length);
        final ThreadExecutor executor = new ThreadExecutor();

        layoverShadowMask = new byte[sourceImageHeight][sourceImageWidth];

        try {
            for (final Rectangle rectangle : tileRectangles) {
                final ThreadRunnable worker = new ThreadRunnable() {

                    @Override
                    public void process() {
                        final int x0 = rectangle.x;
                        final int y0 = rectangle.y;
                        final int w = rectangle.width;
                        final int h = rectangle.height;
                        final int xMax = x0 + w;
                        final int yMax = y0 + h;

                        final double[][] localDEM = new double[h + 2][w + 2];
                        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(sourceProduct, x0, y0, w, h);
                        try {
                            final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, demResamplingMethod,
                                    tileGeoRef, x0, y0, w, h, sourceProduct, true, localDEM);

                            if (!valid) {
                                saveLayoverShadowMask = false;
                                System.out.println("Cannot create layover/shadow mask due to the absent of DEM");
                                return;
                            }
                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException(getId(), e);
                        }

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
                        float[] slrs = new float[w];
                        float[] elev = new float[w];
                        float[] azIndex = new float[w];
                        float[] rgIndex = new float[w];
                        boolean[] savePixel = new boolean[w];

                        for (int y = y0; y < yMax; ++y) {
                            final int yy = y - y0;
                            Arrays.fill(slrs, 0.0f);
                            Arrays.fill(elev, 0.0f);
                            Arrays.fill(azIndex, 0.0f);
                            Arrays.fill(rgIndex, 0.0f);
                            Arrays.fill(savePixel, Boolean.FALSE);

                            for (int x = x0; x < xMax; ++x) {
                                final int xx = x - x0;
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

                                GeoUtils.geo2xyzWGS84(lat, lon, alt, posData.earthPoint);
                                if (!sarPosition.getPosition(posData))
                                    continue;

                                int rIndex = (int) posData.rangeIndex;
                                int aIndex = (int) posData.azimuthIndex;
                                if (rIndex >= 0 && rIndex < sourceImageWidth && aIndex >= 0 && aIndex < sourceImageHeight) {
                                    azIndex[xx] = (float)posData.azimuthIndex;
                                    rgIndex[xx] = (float)posData.rangeIndex;
                                    slrs[xx] = (float)posData.slantRange;
                                    elev[xx] = computeElevationAngle(posData.slantRange, posData.earthPoint, posData.sensorPos);
                                    savePixel[xx] = true;
                                } else {
                                    savePixel[xx] = false;
                                }
                            }
                            computeLayoverShadow(x0, y0, w, h, savePixel, slrs, elev, azIndex, rgIndex);
                        }
                    }
                };
                executor.execute(worker);
                status.worked(1);

            }
            executor.complete();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            status.done();
        }

        isLayoverShadowMaskAvailable = true;
    }

    private static float computeElevationAngle(
            final double slantRange, final PosVector earthPoint, final PosVector sensorPos) {

        final double H2 = sensorPos.x * sensorPos.x + sensorPos.y * sensorPos.y + sensorPos.z * sensorPos.z;
        final double R2 = earthPoint.x * earthPoint.x + earthPoint.y * earthPoint.y + earthPoint.z * earthPoint.z;

        return (float)(FastMath.acos((slantRange * slantRange + H2 - R2) / (2 * slantRange * Math.sqrt(H2))) * Constants.RTOD);
    }


    private void computeLayoverShadow(final int x0, final int y0, final int w, final int h,
                                      final boolean[] savePixel, final float[] slrs, final float[] elev,
                                      final float[] azIndex, final float[] rgIndex) {

        final byte byte1 = 1;
        final byte byte2 = 2;
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte1);
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte1);
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte2);
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte1);
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte1);
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
                            saveLayoverShadow(x0, y0, w, h, rgIndex[i], azIndex[i], byte2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLayoverShadow(final int x0, final int y0, final int w, final int h,
                                   final float rgIndex, final float azIndex, final byte value) {

        final int xMin = (int)rgIndex;
        final int xMax = Math.min(xMin + 1, x0 + w - 1);
        final int yMin = (int)azIndex;
        final int yMax = Math.min(yMin + 1, y0 + h - 1);
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {

                synchronized (layoverShadowMask) {
                    if (layoverShadowMask[y][x] == 0) {
                        layoverShadowMask[y][x] = value;
                    } else if (layoverShadowMask[y][x] == 1 && value == 2){
                        layoverShadowMask[y][x] += value;
                    }
                }
            }
        }
    }

    private void saveNoDataValueToTarget(final int index, final TileData[] tgtTiles, final ProductData demBuffer) {
        if (saveDEM) {
            demBuffer.setElemDoubleAt(index, demNoDataValue);
        }
        for (TileData tileData : tgtTiles) {
            tileData.tileDataBuffer.setElemDoubleAt(index, tileData.noDataValue);
        }
    }

    private Rectangle getSourceRectangle(final int x0, final int y0, final int w, final int h,
                                         final TileGeoreferencing tileGeoRef, final double[][] localDEM) {
/*
        final PixelPos[] tgtCorners = {new PixelPos(x0, y0), new PixelPos(x0 + w - 1, y0),
                new PixelPos(x0, y0 + h - 1), new PixelPos(x0 + w - 1, y0 + h - 1)};

        final double[] tgtCornerElevations = {localDEM[1][1], localDEM[1][w], localDEM[h][1], localDEM[h][w]};

        int xMax = -Integer.MAX_VALUE;
        int xMin = Integer.MAX_VALUE;
        int yMax = -Integer.MAX_VALUE;
        int yMin = Integer.MAX_VALUE;

        PositionData posData = new PositionData();
        GeoPos geoPos = new GeoPos();
        for (int i = 0; i < 4; i++) {

            tileGeoRef.getGeoPos(tgtCorners[i], geoPos);
            final Double alt = tgtCornerElevations[i];
            if (alt.equals(demNoDataValue)) {
                return null;
            }

            if (!getPosition(geoPos.lat, geoPos.lon, alt, posData)) {
                return null;
            }

            if (xMax < posData.rangeIndex) {
                xMax = (int) Math.ceil(posData.rangeIndex);
            }

            if (xMin > posData.rangeIndex) {
                xMin = (int) Math.floor(posData.rangeIndex);
            }

            if (yMax < posData.azimuthIndex) {
                yMax = (int) Math.ceil(posData.azimuthIndex);
            }

            if (yMin > posData.azimuthIndex) {
                yMin = (int) Math.floor(posData.azimuthIndex);
            }
        }

        xMin = Math.max(xMin - margin, 0);
        xMax = Math.min(xMax + margin, sourceImageWidth - 1);
        yMin = Math.max(yMin - margin, 0);
        yMax = Math.min(yMax + margin, sourceImageHeight - 1);
        return new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
*/
        final int numPointsPerRow = 5;
        final int numPointsPerCol = 5;
        final int xOffset = w / (numPointsPerRow - 1);
        final int yOffset = h / (numPointsPerCol - 1);

        int xMax = Integer.MIN_VALUE;
        int xMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;

        PositionData posData = new PositionData();
        GeoPos geoPos = new GeoPos();
        for (int i = 0; i < numPointsPerCol; i++) {
            final int y = (i == numPointsPerCol - 1? y0 + h - 1 : y0 + i*yOffset);

            for (int j = 0; j < numPointsPerRow; ++j) {
                final int x = (j == numPointsPerRow - 1? x0 + w - 1 : x0 + j*xOffset);

                tileGeoRef.getGeoPos(new PixelPos(x, y), geoPos);

                final Double alt = localDEM[y - y0 + 1][x - x0 + 1];
                if (alt.equals(demNoDataValue)) {
                    continue;
                }

                if (!getPosition(geoPos.lat, geoPos.lon, alt, posData)) {
                    continue;
                }

                if (xMax < posData.rangeIndex) {
                    xMax = (int) Math.ceil(posData.rangeIndex);
                }

                if (xMin > posData.rangeIndex) {
                    xMin = (int) Math.floor(posData.rangeIndex);
                }

                if (yMax < posData.azimuthIndex) {
                    yMax = (int) Math.ceil(posData.azimuthIndex);
                }

                if (yMin > posData.azimuthIndex) {
                    yMin = (int) Math.floor(posData.azimuthIndex);
                }
            }
        }

        xMin = Math.max(xMin - margin, 0);
        xMax = Math.min(xMax + margin, sourceImageWidth - 1);
        yMin = Math.max(yMin - margin, 0);
        yMax = Math.min(yMax + margin, sourceImageHeight - 1);

        if (xMin > xMax || yMin > yMax) {
            return null;
        }
        return new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);

    }

    private int getMargin() {

        if (imgResampling == Resampling.BILINEAR_INTERPOLATION) {
            return 1;
        } else if (imgResampling == Resampling.NEAREST_NEIGHBOUR) {
            return 1;
        } else if (imgResampling == Resampling.CUBIC_CONVOLUTION) {
            return 2;
        } else if (imgResampling == Resampling.BISINC_5_POINT_INTERPOLATION) {
            return 3;
        } else if (imgResampling == Resampling.BISINC_11_POINT_INTERPOLATION) {
            return 6;
        } else if (imgResampling == Resampling.BISINC_21_POINT_INTERPOLATION) {
            return 11;
        } else if (imgResampling == Resampling.BICUBIC_INTERPOLATION) {
            return 2;
        } else {
            throw new OperatorException("Unhandled interpolation method");
        }
    }

    private boolean getPosition(final double lat, final double lon, final double alt, final PositionData data) {

        GeoUtils.geo2xyzWGS84(lat, lon, alt, data.earthPoint);

        double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(firstLineUTC,
                                                                           lineTimeInterval, wavelength, data.earthPoint, orbit.sensorPosition, orbit.sensorVelocity);

        if (Double.compare(zeroDopplerTime, SARGeocoding.NonValidZeroDopplerTime) == 0) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, data.earthPoint, data.sensorPos);

        if (!skipBistaticCorrection) { // skip bistatic correction for COSMO, TerraSAR-X and RadarSAT-2
            zeroDopplerTime += data.slantRange / Constants.lightSpeedInMetersPerDay;
            data.slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbit, data.earthPoint, data.sensorPos);
        }

        data.rangeIndex = SARGeocoding.computeRangeIndex(srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC,
                                                         rangeSpacing, zeroDopplerTime, data.slantRange, nearEdgeSlantRange, srgrConvParams);

        if (data.rangeIndex == -1.0) {
            return false;
        }

        if (!nearRangeOnLeft) {
            data.rangeIndex = sourceImageWidth - 1 - data.rangeIndex;
        }

        data.azimuthIndex = (zeroDopplerTime - firstLineUTC) / lineTimeInterval;
        return true;
    }

    /**
     * Get unit for the source band corresponding to the given target band.
     *
     * @param bandName The target band name.
     * @return The source band unit.
     */
    private Unit.UnitType getBandUnit(String bandName) {
        final Band[] srcBands = targetBandNameToSourceBand.get(bandName);
        return Unit.getUnitType(srcBands[0]);
    }

    /**
     * Compute orthorectified pixel value for given pixel.
     *
     * @param azimuthIndex  The azimuth index for pixel in source image.
     * @param rangeIndex    The range index for pixel in source image.
     * @param tileData      The source tile information.
     * @param subSwathIndex The subSwath index.
     * @return The pixel value.
     */
    private double getPixelValue(final double azimuthIndex, final double rangeIndex,
                                 final TileData tileData, final int[] subSwathIndex) {

        try {
            boolean computeNewSourceRectangle = false;
            if (tileData.imgResamplingRaster.sourceRectangle == null) {
                computeNewSourceRectangle = true;
            } else {
                final int xMin = tileData.imgResamplingRaster.sourceRectangle.x + margin;
                final int yMin = tileData.imgResamplingRaster.sourceRectangle.y + margin;
                final int xMax = xMin + tileData.imgResamplingRaster.sourceRectangle.width - 1 - 2 * margin;
                final int yMax = yMin + tileData.imgResamplingRaster.sourceRectangle.height - 1 - 2 * margin;
                if (rangeIndex < xMin || rangeIndex > xMax || azimuthIndex < yMin || azimuthIndex > yMax) {
                    computeNewSourceRectangle = true;
                }
            }

            Band[] srcBands = null;
            if (computeNewSourceRectangle) {
                final int x0 = Math.max((int)rangeIndex - margin, 0);
                final int y0 = Math.max((int)azimuthIndex - margin, 0);
                final int xMax = Math.min(x0 + 2 * margin + 1, sourceImageWidth);
                final int yMax = Math.min(y0 + 2 * margin + 1, sourceImageHeight);
                final int w = xMax - x0;
                final int h = yMax - y0;
                Rectangle srcRect = new Rectangle(x0, y0, w, h);

                srcBands = targetBandNameToSourceBand.get(tileData.bandName);
                tileData.imgResamplingRaster.setSourceTiles(getSourceTile(srcBands[0], srcRect),
                                                            srcBands.length > 1 ? getSourceTile(srcBands[1], srcRect) : null);
            }

            tileData.imgResamplingRaster.setRangeAzimuthIndices(rangeIndex, azimuthIndex);

            imgResampling.computeCornerBasedIndex(rangeIndex, azimuthIndex,
                                                  sourceImageWidth, sourceImageHeight, tileData.imgResamplingIndex);

            double v = imgResampling.resample(tileData.imgResamplingRaster, tileData.imgResamplingIndex);

            subSwathIndex[0] = tileData.imgResamplingRaster.getSubSwathIndex();

            if (computeNewSourceRectangle) {
                tileData.imgResamplingRaster.setSourceTiles(tileData.sourceTileI, tileData.sourceTileQ);
            }

            return v;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
            return 0;
        }
    }

    /**
     * Set flag for radiometric correction. This function is for unit test only.
     *
     * @param flag The flag.
     */
    void setApplyRadiometricCalibration(final boolean flag) {
        saveSelectedSourceBand = !flag;
        applyRadiometricNormalization = flag;
        saveSigmaNought = flag;
    }

    void setSourceBandNames(final String[] names) {
        sourceBandNames = names;
    }

    public static class TileData {
        final Tile targetTile;
        final ProductData tileDataBuffer;
        final String bandName;
        final String bandPolar;
        final Unit.UnitType bandUnit;
        final double noDataValue;
        final Band[] srcBands;
        final boolean isPolsar;
        private final Calibrator calibrator;
        boolean applyRadiometricNormalization = false;
        boolean applyRetroCalibration = false;
        final boolean computeIntensity;

        final ResamplingRaster imgResamplingRaster;
        final Resampling.Index imgResamplingIndex;
        final Tile sourceTileI;
        final Tile sourceTileQ;

        TileData(final Tile tile, final Band[] srcBands, final boolean isPolsar, final boolean outputComplex,
                 final String name, final Unit.UnitType unit, final MetadataElement absRoot, final Calibrator calibrator,
                 final Resampling imgResampling, final Tile sourceTileI, final Tile sourceTileQ) {

            this.targetTile = tile;
            this.tileDataBuffer = tile.getDataBuffer();
            this.bandName = name;
            this.srcBands = srcBands;
            this.isPolsar = isPolsar;
            this.noDataValue = srcBands[0].getNoDataValue();
            this.bandPolar = OperatorUtils.getBandPolarization(srcBands[0].getName(), absRoot);
            this.bandUnit = unit;
            this.calibrator = calibrator;
            this.computeIntensity = !isPolsar && !outputComplex &&
                    (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY);

            this.imgResamplingRaster = new ResamplingRaster(this);
            imgResamplingIndex = imgResampling.createIndex();
            this.sourceTileI = sourceTileI;
            this.sourceTileQ = sourceTileQ;
        }
    }

    public static class ResamplingRaster implements Resampling.Raster {

        private double rangeIndex = 0.0;
        private double azimuthIndex = 0.0;
        private TileData tileData = null;
        private Rectangle sourceRectangle = null;
        private Tile sourceTileI = null;
        private Tile sourceTileQ = null;
        private ProductData dataBufferI = null;
        private ProductData dataBufferQ = null;
        private int subSwathIndex = -1;

        ResamplingRaster(final TileData tileData) {
            this.tileData = tileData;
        }

        void setRangeAzimuthIndices(final double rangeIndex, final double azimuthIndex) {
            this.rangeIndex = rangeIndex;
            this.azimuthIndex = azimuthIndex;
        }

        void setSourceTiles(final Tile sourceTileI, final Tile sourceTileQ) {

            if (sourceTileI != null) {
                this.sourceTileI = sourceTileI;
                this.dataBufferI = sourceTileI.getDataBuffer();
                this.sourceRectangle = sourceTileI.getRectangle();
            }

            if (sourceTileQ != null) {
                this.sourceTileQ = sourceTileQ;
                this.dataBufferQ = sourceTileQ.getDataBuffer();
            }
        }

        public final int getWidth() {
            return sourceTileI.getWidth();
        }

        public final int getHeight() {
            return sourceTileI.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) {

            final int[][] subSwathIndices = new int[y.length][x.length];
            boolean allPixelsFromSameSubSwath = true;
            boolean allValid = true;

            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {

                    final int index = sourceTileI.getDataBufferIndex(x[j], y[i]);
                    double v = dataBufferI.getElemDoubleAt(index);
                    if (tileData.noDataValue != 0 && (v == tileData.noDataValue)) {
                        samples[i][j] = tileData.noDataValue;
                        allValid = false;
                        continue;
                    }

                    samples[i][j] = v;

                    if (tileData.computeIntensity) {

                        final double vq = dataBufferQ.getElemDoubleAt(index);
                        if (tileData.noDataValue != 0 && vq == tileData.noDataValue) {
                            samples[i][j] = tileData.noDataValue;
                            allValid = false;
                            continue;
                        }

                        samples[i][j] = v * v + vq * vq;
                    }

                    final int[] subSwathIndex = {-1};
                    if (tileData.applyRetroCalibration) {
                        samples[i][j] = tileData.calibrator.applyRetroCalibration(
                                x[j], y[i], samples[i][j], tileData.bandPolar, tileData.bandUnit, subSwathIndex);

                        subSwathIndices[i][j] = subSwathIndex[0];
                        if (subSwathIndex[0] != subSwathIndices[0][0]) {
                            allPixelsFromSameSubSwath = false;
                        }
                    }
                }
            }

            if (allPixelsFromSameSubSwath) {
                this.subSwathIndex = subSwathIndices[0][0];

            } else {

                int xIdx = -1, yIdx = -1;
                for (int i = 0; i < y.length; i++) {
                    if (Math.abs(azimuthIndex - y[i]) <= 0.5) {
                        yIdx = i;
                        break;
                    }
                }

                for (int j = 0; j < x.length; j++) {
                    if (Math.abs(rangeIndex - x[j]) <= 0.5) {
                        xIdx = j;
                        break;
                    }
                }

                if (xIdx != -1 && yIdx != -1) {
                    this.subSwathIndex = subSwathIndices[yIdx][xIdx];
                    double sample = samples[yIdx][xIdx];
                    for (int i = 0; i < y.length; i++) {
                        for (int j = 0; j < x.length; j++) {
                            samples[i][j] = sample;
                        }
                    }
                } else {
                    throw new OperatorException("Invalid x and y input for getSamples");
                }
            }
            return allValid;
        }

        int getSubSwathIndex() {
            return this.subSwathIndex;
        }
    }

    private void debugPrintMetadata() {
        final Logger log = SystemUtils.LOG;

        log.info("firstLineUTC: " + firstLineUTC);
        log.info("lastLineUTC: " + lastLineUTC);
        log.info("lineTimeInterval: " + lineTimeInterval);
        log.info("nearEdgeSlantRange: " + nearEdgeSlantRange);
        log.info("wavelength: " + wavelength);
        log.info("pixelSpacingInMeter: " + pixelSpacingInMeter);
        log.info("pixelSpacingInDegree: " + pixelSpacingInDegree);
    }

    private void debugPrintPixel(int x, int y, double alt, double lat, double lon,
                                 final PosVector earthPoint,
                                 double slantRange, double zeroDopplerTime,
                                 OrbitStateVectors orbit,
                                 double rangeIndex, double azimuthIndex) {
        final Logger log = SystemUtils.LOG;

        debugPrintMetadata();

        log.info("---------------------------------");
        log.info("x: " + x + " y: " + y + " alt: " + alt + " lat: " + lat + " lon: " + lon);
        log.info("earthPoint: " + earthPoint.x + ',' + earthPoint.y + ',' + earthPoint.z);
        log.info("slantRange: " + slantRange);
        log.info("zeroDopplerTime: " + zeroDopplerTime);
        log.info("rangeIndex: " + rangeIndex);
        log.info("azimuthIndex: " + azimuthIndex);
        log.info("---------------------------------");

        final int max = 3;
        for (int i = 0; i < max; ++i) {
            PosVector sensorPos = orbit.sensorPosition[i];
            log.info("sensorPos: " + sensorPos.x + ", " + sensorPos.y + ", " + sensorPos.z);
        }
        for (int i = 0; i < max; ++i) {
            PosVector sensorVel = orbit.sensorVelocity[i];
            log.info("sensorVel: " + sensorVel.x + ", " + sensorVel.y + ", " + sensorVel.z);
        }
        for (int i = 0; i < max; ++i) {
            OrbitStateVector orb = orbit.orbitStateVectors[i];
            log.info("orbitStateVector: " + orb.time.format() +
                             " pos: " + orb.x_pos + ", " + orb.y_pos + ", " + orb.z_pos +
                             " vel: " + orb.x_vel + ", " + orb.y_vel + ", " + orb.z_vel);
        }
        log.info("---------------------------------");
    }

    private static class PositionData {
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
        double slantRange;
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
            super(RangeDopplerGeocodingOp.class);
        }
    }
}
