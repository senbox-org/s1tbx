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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.resamp.Resampling;
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
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.EarthGravitationalModel96;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.*;
import org.esa.nest.util.Constants;
import org.esa.nest.util.GeoUtils;
import org.esa.nest.util.MathUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Raw SAR images usually contain significant geometric distortions. One of the factors that cause the
 * distortions is the ground elevation of the targets. This operator corrects the topographic distortion
 * in the raw image caused by this factor. The operator implements the Range-Doppler (RD) geocoding method.
 *
 * The method consis of the following major steps:
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
 *
 * Reference: Guide to ASAR Geocoding, Issue 1.0, 19.03.2008
 */

@OperatorMetadata(alias="Terrain-Correction", category = "Geometry\\Terrain Correction", description="RD method for orthorectification")
public class RangeDopplerGeocodingOp extends Operator {

    public static final String PRODUCT_SUFFIX = "_TC";

    @SourceProduct(alias="source")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private
    String[] sourceBandNames = null;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"}, description = "The digital elevation model.",
               defaultValue="SRTM 3Sec", label="Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(label="External DEM")
    private File externalDEMFile = null;

    @Parameter(label="DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label="DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label="Image Resampling Method")
    private String imgResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(description = "The pixel spacing in meters", defaultValue = "0", label="Pixel Spacing (m)")
    private double pixelSpacingInMeter = 0;

    @Parameter(description = "The pixel spacing in degrees", defaultValue = "0", label="Pixel Spacing (deg)")
    private double pixelSpacingInDegree = 0;

    @Parameter(description = "The coordinate reference system in well known text format", defaultValue="WGS84(DD)")
    private String mapProjection = "WGS84(DD)";

    @Parameter(defaultValue="true", label="Mask out areas with no elevation", description = "Mask the sea with no data value (faster)")
    private boolean nodataValueAtSea = true;

    @Parameter(defaultValue="false", label="Save DEM as band")
    private boolean saveDEM = false;

    @Parameter(defaultValue="false", label="Save local incidence angle as band")
    private boolean saveLocalIncidenceAngle = false;

    @Parameter(defaultValue="false", label="Save projected local incidence angle as band")
    private boolean saveProjectedLocalIncidenceAngle = false;

    @Parameter(defaultValue="true", label="Save selected source band")
    private boolean saveSelectedSourceBand = true;

    @Parameter(defaultValue="false", label="Apply radiometric normalization")
    private boolean applyRadiometricNormalization = false;

    @Parameter(defaultValue="false", label="Save Sigma0 as a band")
    private boolean saveSigmaNought = false;

    @Parameter(defaultValue="false", label="Save Gamma0 as a band")
    private boolean saveGammaNought = false;

    @Parameter(defaultValue="false", label="Save Beta0 as a band")
    private boolean saveBetaNought = false;

    @Parameter(valueSet = {USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label="")
    private String incidenceAngleForSigma0 = USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label="")
    private String incidenceAngleForGamma0 = USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {CalibrationOp.LATEST_AUX, CalibrationOp.PRODUCT_AUX, CalibrationOp.EXTERNAL_AUX},
            description = "The auxiliary file", defaultValue=CalibrationOp.LATEST_AUX, label="Auxiliary File")
    private String auxFile = CalibrationOp.LATEST_AUX;

    @Parameter(description = "The antenne elevation pattern gain auxiliary data file.", label="External Aux File")
    private File externalAuxFile = null;

    private MetadataElement absRoot = null;
    private ElevationModel dem = null;
    private GeoCoding targetGeoCoding = null;

    private boolean srgrFlag = false;
    private boolean saveIncidenceAngleFromEllipsoid = false;
    private boolean isElevationModelAvailable = false;
    private boolean usePreCalibrationOp = false;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int targetImageWidth = 0;
    private int targetImageHeight = 0;

    private double avgSceneHeight = 0.0; // in m
    private double wavelength = 0.0; // in m
    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double firstLineUTC = 0.0; // in days
    private double lastLineUTC = 0.0; // in days
    private double lineTimeInterval = 0.0; // in days
    private double nearEdgeSlantRange = 0.0; // in m
    private float demNoDataValue = 0.0f; // no data value for DEM

    private CoordinateReferenceSystem targetCRS;
    private double delLat = 0.0;
    private double delLon = 0.0;

    private double[][] sensorPosition = null; // sensor position for all range lines
    private double[][] sensorVelocity = null; // sensor velocity for all range lines
    private double[] timeArray = null;
    private double[] xPosArray = null;
    private double[] yPosArray = null;
    private double[] zPosArray = null;

    private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    private AbstractMetadata.OrbitStateVector[] orbitStateVectors = null;
    private final HashMap<String, Band[]> targetBandNameToSourceBand = new HashMap<String, Band[]>();
    private final Map<String, Boolean> targetBandApplyRadiometricNormalizationFlag = new HashMap<String, Boolean>();
    private final Map<String, Boolean> targetBandApplyRetroCalibrationFlag = new HashMap<String, Boolean>();
    private TiePointGrid incidenceAngle = null;
    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private static final double NonValidZeroDopplerTime = -99999.0;
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

    public static final String USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";
    public static final String USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM = "Use local incidence angle from DEM";
    public static final String USE_INCIDENCE_ANGLE_FROM_ELLIPSOID = "Use incidence angle from Ellipsoid";
    public static final double NonValidIncidenceAngle = -99999.0;

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
                throw new OperatorException("Source product is already map projected");
            }

            checkUserInput();

            getSourceImageDimension();

            getMetadata();

            getTiePointGrid();

            if (useAvgSceneHeight) {
                saveSigmaNought = false;
                saveBetaNought = false;
                saveGammaNought = false;
                saveDEM = false;
                saveLocalIncidenceAngle = false;
                saveProjectedLocalIncidenceAngle = false;
            }

            imgResampling = ResamplingFactory.createResampling(imgResamplingMethod);

            createTargetProduct();

            computeSensorPositionsAndVelocities();

            if (saveSigmaNought) {
                calibrator = CalibrationFactory.createCalibrator(sourceProduct);
                calibrator.setAuxFileFlag(auxFile);
                calibrator.setExternalAuxFile(externalAuxFile);
                calibrator.initialize(this, sourceProduct, targetProduct, true, true);
                calibrator.setIncidenceAngleForSigma0(incidenceAngleForSigma0);
            }

            updateTargetProductMetadata();

            if(externalDEMFile == null && !useAvgSceneHeight) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            if (!useAvgSceneHeight) {
                DEMFactory.validateDEM(demName, sourceProduct);
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public void dispose() throws OperatorException {
        if (dem != null) {
            dem.dispose();
        }

        if(!orthoDataProduced && processingStarted) {
            final String errMsg = getId() +" error: no valid output was produced. Please verify the DEM";
            System.out.println(errMsg);
            if(VisatApp.getApp() != null) {
                VisatApp.getApp().setStatusBarMessage(errMsg);
            }
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

        if ( saveBetaNought || saveGammaNought ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) ) {
            saveSigmaNought = true;
            saveProjectedLocalIncidenceAngle = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID))) {
            saveIncidenceAngleFromEllipsoid = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM))) {
            saveLocalIncidenceAngle = true;
        }

        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
    }

    private void getTiePointGrid() {
        latitude = OperatorUtils.getLatitude(sourceProduct);
        if (latitude == null) {
            throw new OperatorException("Product without latitude tie point grid");
        }

        longitude = OperatorUtils.getLongitude(sourceProduct);
        if (longitude == null) {
            throw new OperatorException("Product without longitude tie point grid");
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {
        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = getMissionType(absRoot);

        if (mission.contains("CSKS") || mission.contains("TSX") || mission.equals("RS2") || mission.contains("SENTINEL")) {
            skipBistaticCorrection = true;
        }

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);

        wavelength = OperatorUtils.getRadarFrequency(absRoot);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        if (rangeSpacing <= 0.0) {
            throw new OperatorException("Invalid input for range pixel spacing: " + rangeSpacing);
        }

        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        if (azimuthSpacing <= 0.0) {
            throw new OperatorException("Invalid input for azimuth pixel spacing: " + azimuthSpacing);
        }

        firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day
        if (lastLineUTC == 0.0) {
            throw new OperatorException("Invalid input for Line Time Interval: " + lineTimeInterval);
        }

        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        if (orbitStateVectors == null || orbitStateVectors.length == 0) {
            throw new OperatorException("Invalid Obit State Vectors");
        }

        if (srgrFlag) {
            srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
            if (srgrConvParams == null) {
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

        nearRangeOnLeft = isNearRangeOnLeft(incidenceAngle, sourceImageWidth);

        isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;
    }

    public static boolean isNearRangeOnLeft(final TiePointGrid incidenceAngle, final int sourceImageWidth) {
        final double incidenceAngleToFirstPixel = incidenceAngle.getPixelDouble(0, 0);
        final double incidenceAngleToLastPixel = incidenceAngle.getPixelDouble(sourceImageWidth-1, 0);
        return (incidenceAngleToFirstPixel < incidenceAngleToLastPixel);
    }

    /**
     * Get the mission type.
     * @param absRoot the AbstractMetadata
     * @return the mission string
     */
    public static String getMissionType(final MetadataElement absRoot) {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (mission.equals("ALOS")) {
            final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE).toUpperCase();
            if(!productType.contains("1.1"))
                throw new OperatorException("Detected ALOS PALSAR products are currently not supported");
        }
        if (mission.equals("RS1")) {
            //throw new OperatorException("RadarSAT-1 product is currently not supported");
        }

        return mission;
    }

    /**
     * Get incidence angle at centre range pixel (in radian).
     * @param srcProduct The source product.
     * @throws OperatorException The exceptions.
     * @return The incidence angle.
     */
    private static double getIncidenceAngleAtCentreRangePixel(Product srcProduct) throws OperatorException {

        final int sourceImageWidth = srcProduct.getSceneRasterWidth();
        final int sourceImageHeight = srcProduct.getSceneRasterHeight();
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
        if(incidenceAngle == null) {
            throw new OperatorException("incidence_angle tie point grid not found in product");
        }
        return incidenceAngle.getPixelFloat((float)x, (float)y)*org.esa.beam.util.math.MathUtils.DTOR;
    }

    /**
     * Get elevation model.
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if(isElevationModelAvailable) return;
        if(externalDEMFile != null) { // if external DEM file is specified by user

            if (demResamplingMethod.equals(DEMFactory.DELAUNAY_INTERPOLATION))
                throw new OperatorException("Delaunay interpolation for an external DEM file is currently not supported");

            dem = new FileElevationModel(externalDEMFile,
                                             ResamplingFactory.createResampling(demResamplingMethod),
                                             (float)externalDEMNoDataValue);

            demNoDataValue = (float) externalDEMNoDataValue;
            demName = externalDEMFile.getName();

        } else {

            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
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

    private void createTargetProduct() {
        try {
            if (pixelSpacingInMeter <= 0.0) {
                pixelSpacingInMeter = Math.max(getAzimuthPixelSpacing(sourceProduct), getRangePixelSpacing(sourceProduct));
                pixelSpacingInDegree = getPixelSpacingInDegree(pixelSpacingInMeter);
            }
            delLat = pixelSpacingInDegree;
            delLon = pixelSpacingInDegree;

            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(sourceProduct, mapProjection,
                    pixelSpacingInDegree, pixelSpacingInMeter);

            targetCRS = crsHandler.getTargetCRS();

            targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                    sourceProduct.getProductType(), crsHandler.getTargetWidth(), crsHandler.getTargetHeight());
            targetProduct.setGeoCoding(crsHandler.getCrsGeoCoding());

            targetImageWidth = targetProduct.getSceneRasterWidth();
            targetImageHeight = targetProduct.getSceneRasterHeight();

            addSelectedBands();

            targetGeoCoding = targetProduct.getGeoCoding();

            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
            ProductUtils.copyVectorData(sourceProduct, targetProduct);
            targetProduct.setDescription(sourceProduct.getDescription());

            try {
                OperatorUtils.copyIndexCodings(sourceProduct, targetProduct);
            } catch(Exception e) {
                if(!imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {
                    throw new OperatorException("Use Nearest Neighbour with Classificaitons: "+e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        String targetBandName;
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();

            if (unit != null && !isPolsar &&
                    (unit.equals(Unit.REAL)||unit.equals(Unit.IMAGINARY))) {

                if (i == sourceBands.length - 1) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String nextUnit = sourceBands[i+1].getUnit();
                if (nextUnit == null || !((unit.equals(Unit.REAL) && nextUnit.equals(Unit.IMAGINARY)) ||
                                          (unit.equals(Unit.IMAGINARY) && nextUnit.equals(Unit.REAL)))) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final Band[] srcBands = new Band[2];
                srcBands[0] = srcBand;
                srcBands[1] = sourceBands[i+1];
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
                    if(imgResampling.equals(Resampling.NEAREST_NEIGHBOUR))
                        dataType = srcBand.getDataType();
                    if (addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                                      targetBandName, unit, srcBand, dataType) != null) {
                        targetBandNameToSourceBand.put(targetBandName, srcBands);
                        targetBandApplyRadiometricNormalizationFlag.put(targetBandName, false);
                        targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                    }
                }
            }
        }

        if(saveDEM) {
            final Band elevBand = addTargetBand("elevation", Unit.METERS, null);
            if(externalDEMFile != null)
                demNoDataValue = (float)externalDEMNoDataValue;
            elevBand.setNoDataValue(demNoDataValue);
            elevBand.setNoDataValueUsed(true);
        }

        if(saveLocalIncidenceAngle) {
            addTargetBand("incidenceAngle", Unit.DEGREES, null);
        }

        if(saveProjectedLocalIncidenceAngle) {
            addTargetBand("projectedIncidenceAngle", Unit.DEGREES, null);
        }

        if (saveIncidenceAngleFromEllipsoid) {
            addTargetBand("incidenceAngleFromEllipsoid", Unit.DEGREES, null);
        }

        if (saveSigmaNought && !incidenceAngleForSigma0.contains(USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM)) {
            createSigmaNoughtVirtualBand(targetProduct, incidenceAngleForSigma0);
        }

        if (saveGammaNought) {
            createGammaNoughtVirtualBand(targetProduct, incidenceAngleForGamma0);
        }

        if (saveBetaNought) {
            createBetaNoughtVirtualBand(targetProduct);
        }
    }

    private Band addTargetBand(final String bandName, final String bandUnit, final Band sourceBand) {
        return addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                bandName, bandUnit, sourceBand, ProductData.TYPE_FLOAT32);
    }

    public static Band addTargetBand(final Product targetProduct, final int targetImageWidth, final int targetImageHeight,
                                     final String bandName, final String bandUnit, final Band sourceBand,
                                     final int dataType) {

        if(targetProduct.getBand(bandName) == null) {

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
     * @throws OperatorException The exception.
     */
    private void updateTargetProductMetadata() throws Exception {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.srgr_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);

        final GeoPos geoPosFirstNear = targetGeoCoding.getGeoPos(new PixelPos(0,0), null);
        final GeoPos geoPosFirstFar = targetGeoCoding.getGeoPos(new PixelPos(targetImageWidth-1, 0), null);
        final GeoPos geoPosLastNear = targetGeoCoding.getGeoPos(new PixelPos(0,targetImageHeight-1), null);
        final GeoPos geoPosLastFar = targetGeoCoding.getGeoPos(new PixelPos(targetImageWidth-1, targetImageHeight-1), null);

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
            if(externalDEMFile != null) { // if external DEM file is specified by user
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
            Double.compare(pixelSpacingInMeter, getPixelSpacing(sourceProduct)) != 0) {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, pixelSpacingInMeter);
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, pixelSpacingInMeter);
        }

        // save look directions for 5 range lines
        final MetadataElement lookDirectionListElem = new MetadataElement("Look_Direction_List");
        final int numOfDirections = 5;
        for(int i=1; i <= numOfDirections; ++i) {
            addLookDirection("look_direction", lookDirectionListElem, i, numOfDirections, sourceImageWidth,
                    sourceImageHeight, firstLineUTC, lineTimeInterval, nearRangeOnLeft, latitude, longitude);
        }
        absTgt.addElement(lookDirectionListElem);
    }

    public static void addLookDirection(final String name, final MetadataElement lookDirectionListElem, final int index,
                                        final int num, final int sourceImageWidth, final int sourceImageHeight,
                                        final double firstLineUTC, final double lineTimeInterval,
                                        final boolean nearRangeOnLeft, final TiePointGrid latitude,
                                        final TiePointGrid longitude) {

        final MetadataElement lookDirectionElem = new MetadataElement(name+index);

        int xHead, xTail, y;
        if (num == 1) {
            y = sourceImageHeight/2;
        } else if (num > 1) {
            y = (index - 1)*sourceImageHeight / (num - 1);
        } else {
            throw new OperatorException("Invalid number of look directions");
        }

        final double time = firstLineUTC + y*lineTimeInterval;
        lookDirectionElem.setAttributeUTC("time", new ProductData.UTC(time));

        if (nearRangeOnLeft) {
            xHead = sourceImageWidth - 1;
            xTail = 0;
        } else {
            xHead = 0;
            xTail = sourceImageWidth - 1;
        }
        lookDirectionElem.setAttributeDouble("head_lat", latitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("head_lon", longitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("tail_lat", latitude.getPixelDouble(xTail, y));
        lookDirectionElem.setAttributeDouble("tail_lon", longitude.getPixelDouble(xTail, y));
        lookDirectionListElem.addElement(lookDirectionElem);
    }

    /**
     * Compute sensor position and velocity for each range line from the orbit state vectors.
     */
    private void computeSensorPositionsAndVelocities() {
        
        final int numVectorsUsed = Math.min(orbitStateVectors.length, 5);
        timeArray = new double[numVectorsUsed];
        xPosArray = new double[numVectorsUsed];
        yPosArray = new double[numVectorsUsed];
        zPosArray = new double[numVectorsUsed];
        sensorPosition = new double[sourceImageHeight][3]; // xPos, yPos, zPos
        sensorVelocity = new double[sourceImageHeight][3]; // xVel, yVel, zVel

        computeSensorPositionsAndVelocities(
                orbitStateVectors, timeArray, xPosArray, yPosArray, zPosArray,
                sensorPosition, sensorVelocity, firstLineUTC, lineTimeInterval, sourceImageHeight);
    }

    /**
     * Compute sensor position and velocity for each range line from the orbit state vectors using
     * cubic WARP polynomial.
     * @param orbitStateVectors The orbit state vectors.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param sensorPosition Sensor positions for all range lines.
     * @param sensorVelocity Sensor velocities for all range lines.
     * @param firstLineUTC The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param sourceImageHeight The source image height.
     */
    public static void computeSensorPositionsAndVelocities(AbstractMetadata.OrbitStateVector[] orbitStateVectors,
                                                           double[] timeArray, double[] xPosArray,
                                                           double[] yPosArray, double[] zPosArray,
                                                           double[][] sensorPosition, double[][] sensorVelocity,
                                                           double firstLineUTC, double lineTimeInterval,
                                                           int sourceImageHeight) {

        final int numVectors = orbitStateVectors.length;
        final int numVectorsUsed = timeArray.length;
        final int d = numVectors / numVectorsUsed;

        final double[] xVelArray = new double[numVectorsUsed];
        final double[] yVelArray = new double[numVectorsUsed];
        final double[] zVelArray = new double[numVectorsUsed];

        for (int i = 0; i < numVectorsUsed; i++) {
            timeArray[i] = orbitStateVectors[i*d].time_mjd;
            xPosArray[i] = orbitStateVectors[i*d].x_pos; // m
            yPosArray[i] = orbitStateVectors[i*d].y_pos; // m
            zPosArray[i] = orbitStateVectors[i*d].z_pos; // m
            xVelArray[i] = orbitStateVectors[i*d].x_vel; // m/s
            yVelArray[i] = orbitStateVectors[i*d].y_vel; // m/s
            zVelArray[i] = orbitStateVectors[i*d].z_vel; // m/s
        }

        // Lagrange polynomial interpolation
        for (int i = 0; i < sourceImageHeight; i++) {
            final double time = firstLineUTC + i*lineTimeInterval; // zero Doppler time (in days) for each range line
            sensorPosition[i][0] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, time);
            sensorPosition[i][1] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, time);
            sensorPosition[i][2] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, time);
            sensorVelocity[i][0] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xVelArray, time);
            sensorVelocity[i][1] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yVelArray, time);
            sensorVelocity[i][2] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zVelArray, time);
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
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        processingStarted = true;
        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }
        } catch(Exception e) {
            throw new OperatorException(e);
        }

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0, y0, w, h);

        try {
            float[][] localDEM = new float[h+2][w+2];
            if(useAvgSceneHeight) {
                DEMFactory.fillDEM(localDEM, (float)avgSceneHeight);
            } else {
                final boolean valid = DEMFactory.getLocalDEM(dem, demNoDataValue, tileGeoRef, x0, y0, w, h, localDEM);
                if(!valid && nodataValueAtSea)
                    return;
            }

            final GeoPos geoPos = new GeoPos();
            final double[] earthPoint = new double[3];
            final double[] sensorPos = new double[3];
            final int srcMaxRange = sourceImageWidth - 1;
            final int srcMaxAzimuth = sourceImageHeight - 1;
            ProductData demBuffer = null;
            ProductData incidenceAngleBuffer = null;
            ProductData projectedIncidenceAngleBuffer = null;
            ProductData incidenceAngleFromEllipsoidBuffer = null;

            final List<TileData> trgTileList = new ArrayList<TileData>();
            final Set<Band> keySet = targetTiles.keySet();
            for(Band targetBand : keySet) {

                if(targetBand.getName().equals("elevation")) {
                    demBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if(targetBand.getName().equals("incidenceAngle")) {
                    incidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if(targetBand.getName().equals("projectedIncidenceAngle")) {
                    projectedIncidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                if (targetBand.getName().equals("incidenceAngleFromEllipsoid")) {
                    incidenceAngleFromEllipsoidBuffer = targetTiles.get(targetBand).getDataBuffer();
                    continue;
                }

                final Band[] srcBands = targetBandNameToSourceBand.get(targetBand.getName());

                final TileData td = new TileData();
                td.targetTile = targetTiles.get(targetBand);
                td.tileDataBuffer = td.targetTile.getDataBuffer();
                td.bandName = targetBand.getName();
                td.noDataValue = srcBands[0].getNoDataValue();
                td.applyRadiometricNormalization = targetBandApplyRadiometricNormalizationFlag.get(targetBand.getName());
                td.applyRetroCalibration = targetBandApplyRetroCalibrationFlag.get(targetBand.getName());
                td.bandPolar = OperatorUtils.getBandPolarization(srcBands[0].getName(), absRoot);
                trgTileList.add(td);
            }

            final int maxY = y0 + h;
            final int maxX = x0 + w;
            final TileData[] trgTiles = trgTileList.toArray(new TileData[trgTileList.size()]);

            for (int y = y0; y < maxY; y++) {
                final int yy = y-y0+1;

                for (int x = x0; x < maxX; x++) {

                    final int index = trgTiles[0].targetTile.getDataBufferIndex(x, y);

                    double alt = (double)localDEM[yy][x-x0+1];

                    if(saveDEM) {
                        demBuffer.setElemDoubleAt(index, alt);
                    }

                    if(alt == demNoDataValue && !useAvgSceneHeight) {
                        if (nodataValueAtSea) {
                            //saveNoDataValueToTarget(index, trgTiles);
                            continue;
                        }
                    }

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    if(alt == demNoDataValue && !nodataValueAtSea) {
                        // get corrected elevation for 0
                        alt = EarthGravitationalModel96.instance().getEGM(lat, lon);
                    }

                    GeoUtils.geo2xyzWGS84(lat, lon, alt, earthPoint);

                    final double zeroDopplerTime = getEarthPointZeroDopplerTime(firstLineUTC,
                            lineTimeInterval, wavelength, earthPoint, sensorPosition, sensorVelocity);

                    if (Double.compare(zeroDopplerTime, NonValidZeroDopplerTime) == 0) {
                        //saveNoDataValueToTarget(index, trgTiles);
                        continue;
                    }

                    double slantRange = computeSlantRange(
                            zeroDopplerTime, timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

                    double azimuthIndex = 0.0;
                    double rangeIndex = 0.0;
                    double zeroDoppler = zeroDopplerTime;
                    if (!skipBistaticCorrection) {
                        // skip bistatic correction for COSMO, TerraSAR-X and RadarSAT-2
                        zeroDoppler = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;
                    }

                    slantRange = computeSlantRange(
                            zeroDoppler, timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

                    rangeIndex = computeRangeIndex(srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC,
                            rangeSpacing, zeroDoppler, slantRange, nearEdgeSlantRange, srgrConvParams);

                    if (rangeIndex == -1.0) {
                        //saveNoDataValueToTarget(index, trgTiles);
                        continue;
                    }

                    // temp fix for descending Radarsat2
                    if (!nearRangeOnLeft) {
                        rangeIndex = srcMaxRange - rangeIndex;
                    }

                    azimuthIndex = (zeroDoppler - firstLineUTC) / lineTimeInterval;

                    if (!isValidCell(rangeIndex, azimuthIndex, lat, lon, latitude, longitude,
                            srcMaxRange, srcMaxAzimuth, sensorPos)) {
                        //saveNoDataValueToTarget(index, trgTiles);
                    } else {
                        double[] localIncidenceAngles = {NonValidIncidenceAngle, NonValidIncidenceAngle};
                        if (saveLocalIncidenceAngle || saveProjectedLocalIncidenceAngle || saveSigmaNought) {

                            final LocalGeometry localGeometry = new LocalGeometry();
                            setLocalGeometry(x, y, tileGeoRef, earthPoint, sensorPos, localGeometry);

                            computeLocalIncidenceAngle(
                                    localGeometry, demNoDataValue, saveLocalIncidenceAngle, saveProjectedLocalIncidenceAngle,
                                    saveSigmaNought, x0, y0, x, y, localDEM, localIncidenceAngles); // in degrees

                            if (saveLocalIncidenceAngle && localIncidenceAngles[0] != NonValidIncidenceAngle) {
                                incidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[0]);
                            }

                            if (saveProjectedLocalIncidenceAngle && localIncidenceAngles[1] != NonValidIncidenceAngle) {
                                projectedIncidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[1]);
                            }
                        }

                        if (saveIncidenceAngleFromEllipsoid) {
                            incidenceAngleFromEllipsoidBuffer.setElemDoubleAt(
                                    index, (double)incidenceAngle.getPixelFloat((float)rangeIndex, (float)azimuthIndex));
                        }

                        double satelliteHeight = 0;
                        double sceneToEarthCentre = 0;
                        if (saveSigmaNought) {
                                satelliteHeight = Math.sqrt(
                                        sensorPos[0]*sensorPos[0] + sensorPos[1]*sensorPos[1] + sensorPos[2]*sensorPos[2]);

                                sceneToEarthCentre = Math.sqrt(
                                        earthPoint[0]*earthPoint[0] + earthPoint[1]*earthPoint[1] + earthPoint[2]*earthPoint[2]);
                        }

                        for(TileData tileData : trgTiles) {
                            final Unit.UnitType bandUnit = getBandUnit(tileData.bandName);
                            int[] subSwathIndex = {INVALID_SUB_SWATH_INDEX};
                            double v = getPixelValue(azimuthIndex, rangeIndex, tileData, bandUnit, subSwathIndex);

                            if (v != tileData.noDataValue && tileData.applyRadiometricNormalization) {
                                if (localIncidenceAngles[1] != NonValidIncidenceAngle) {
                                    v = calibrator.applyCalibration(
                                            v, rangeIndex, azimuthIndex, slantRange, satelliteHeight, sceneToEarthCentre,
                                            localIncidenceAngles[1], tileData.bandPolar, bandUnit, subSwathIndex); // use projected incidence angle
                                } else {
                                    v = tileData.noDataValue;
                                }
                            }
                            
                            tileData.tileDataBuffer.setElemDoubleAt(index, v);
                        }
                        orthoDataProduced = true;
                    }
                }
            }
            localDEM = null;
            
        } catch(Throwable e) {
            orthoDataProduced = true; //to prevent multiple error messages
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static boolean isValidCell(final double rangeIndex, final double azimuthIndex,
                                final double lat, final double lon,
                                final TiePointGrid latitude, final TiePointGrid longitude,
                                final int srcMaxRange, final int srcMaxAzimuth, final double[] sensorPos) {

        if (rangeIndex < 0.0 || rangeIndex >= srcMaxRange || azimuthIndex < 0.0 || azimuthIndex >= srcMaxAzimuth) {
            return  false;
        }

        final GeoPos sensorGeoPos = new GeoPos();
        GeoUtils.xyz2geo(sensorPos, sensorGeoPos, GeoUtils.EarthModel.WGS84);
        final double delLatMax = Math.abs(lat - sensorGeoPos.lat);
        double delLonMax;
        if (lon < 0 && sensorGeoPos.lon > 0) {
            delLonMax = Math.min(Math.abs(360 + lon - sensorGeoPos.lon), sensorGeoPos.lon - lon);
        } else if (lon > 0 && sensorGeoPos.lon < 0) {
            delLonMax = Math.min(Math.abs(360 + sensorGeoPos.lon - lon), lon - sensorGeoPos.lon);
        } else {
            delLonMax = Math.abs(lon - sensorGeoPos.lon);
        }

        final double delLat = Math.abs(lat - latitude.getPixelFloat((float)rangeIndex, (float)azimuthIndex));
        final double srcLon = longitude.getPixelFloat((float)rangeIndex, (float)azimuthIndex);
        double delLon;
        if (lon < 0 && srcLon > 0) {
            delLon = Math.min(Math.abs(360 + lon - srcLon), srcLon - lon);
        } else if (lon > 0 && srcLon < 0) {
            delLon = Math.min(Math.abs(360 + srcLon - lon), lon - srcLon);
        } else {
            delLon = Math.abs(lon - srcLon);
        }

        return (delLat + delLon <= delLatMax + delLonMax);
    }

    /**
     * Save noDataValue to target pixel with given index.
     * @param index The pixel index in target image.
     * @param trgTiles The target tiles.
     */
    private static void saveNoDataValueToTarget(final int index, final TileData[] trgTiles) {
        for(TileData tileData : trgTiles) {
            tileData.tileDataBuffer.setElemDoubleAt(index, tileData.noDataValue);
        }
    }

    /**
     * Compute zero Doppler time for given erath point.
     * @param firstLineUTC The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength The ragar wavelength.
     * @param earthPoint The earth point in xyz cooordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @return The zero Doppler time in days if it is found, -1 otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getEarthPointZeroDopplerTime(final double firstLineUTC,
                                                      final double lineTimeInterval, final double wavelength,
                                                      final double[] earthPoint, final double[][] sensorPosition,
                                                      final double[][] sensorVelocity) throws OperatorException {

        // binary search is used in finding the zero doppler time
        int lowerBound = 0;
        int upperBound = sensorPosition.length - 1;
        double lowerBoundFreq = getDopplerFrequency(
                lowerBound, earthPoint, sensorPosition, sensorVelocity, wavelength);
        double upperBoundFreq = getDopplerFrequency(
                upperBound, earthPoint, sensorPosition, sensorVelocity, wavelength);

        if (Double.compare(lowerBoundFreq, 0.0) == 0) {
            return firstLineUTC + lowerBound*lineTimeInterval;
        } else if (Double.compare(upperBoundFreq, 0.0) == 0) {
            return firstLineUTC + upperBound*lineTimeInterval;
        } else if (lowerBoundFreq*upperBoundFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        // start binary search
        double midFreq;
        while(upperBound - lowerBound > 1) {

            final int mid = (int)((lowerBound + upperBound)/2.0);
            midFreq = sensorVelocity[mid][0]*(earthPoint[0] - sensorPosition[mid][0]) +
                      sensorVelocity[mid][1]*(earthPoint[1] - sensorPosition[mid][1]) +
                      sensorVelocity[mid][2]*(earthPoint[2] - sensorPosition[mid][2]);

            if (midFreq*lowerBoundFreq > 0.0) {
                lowerBound = mid;
                lowerBoundFreq = midFreq;
            } else if (midFreq*upperBoundFreq > 0.0) {
                upperBound = mid;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return firstLineUTC + mid*lineTimeInterval;
            }
        }

        final double y0 = lowerBound - lowerBoundFreq*(upperBound - lowerBound)/(upperBoundFreq - lowerBoundFreq);
        return firstLineUTC + y0*lineTimeInterval;
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     * @param y The index for given range line.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @param wavelength The ragar wavelength.
     * @return The Doppler frequency in Hz.
     */
    private static double getDopplerFrequency(
            final int y, final double[] earthPoint, final double[][] sensorPosition,
            final double[][] sensorVelocity, final double wavelength) {

        final double xDiff = earthPoint[0] - sensorPosition[y][0];
        final double yDiff = earthPoint[1] - sensorPosition[y][1];
        final double zDiff = earthPoint[2] - sensorPosition[y][2];
        final double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

        return 2.0 * (sensorVelocity[y][0]*xDiff + sensorVelocity[y][1]*yDiff + sensorVelocity[y][2]*zDiff) / (distance*wavelength);
    }

    /**
     * Compute slant range distance for given earth point and given time.
     * @param time The given time in days.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPos The sensor position.
     * @return The slant range distance in meters.
     */
    public static double computeSlantRange(final double time, final double[] timeArray, final double[] xPosArray,
                                           final double[] yPosArray, final double[] zPosArray,
                                           final double[] earthPoint, final double[] sensorPos) {

        sensorPos[0] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, time);
        sensorPos[1] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, time);
        sensorPos[2] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, time);

        final double xDiff = sensorPos[0] - earthPoint[0];
        final double yDiff = sensorPos[1] - earthPoint[1];
        final double zDiff = sensorPos[2] - earthPoint[2];

        return Math.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange The slant range in meters.
     * @return The range index.
     */
    public static double computeRangeIndex(
            final boolean srgrFlag, final int sourceImageWidth, final double firstLineUTC, final double lastLineUTC,
            final double rangeSpacing, final double zeroDopplerTime, final double slantRange,
            final double nearEdgeSlantRange, final AbstractMetadata.SRGRCoefficientList[] srgrConvParams) {

        if (zeroDopplerTime < Math.min(firstLineUTC, lastLineUTC) ||
            zeroDopplerTime > Math.max(firstLineUTC, lastLineUTC)) {
            return -1.0;
        }

        if (srgrFlag) { // ground detected image

            double groundRange = 0.0;

            if (srgrConvParams.length == 1) {
                groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                                                 srgrConvParams[0].coefficients, srgrConvParams[0].ground_range_origin);
                if (groundRange < 0.0) {
                    return -1.0;
                } else {
                    return (groundRange - srgrConvParams[0].ground_range_origin) / rangeSpacing;
                }
            }
            
            int idx = 0;
            for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
                idx = i;
            }

            final double[] srgrCoefficients = new double[srgrConvParams[idx].coefficients.length];
            if (idx == srgrConvParams.length - 1) {
                idx--;
            }

            final double mu = (zeroDopplerTime - srgrConvParams[idx].timeMJD) /
                              (srgrConvParams[idx+1].timeMJD - srgrConvParams[idx].timeMJD);
            for (int i = 0; i < srgrCoefficients.length; i++) {
                srgrCoefficients[i] = MathUtils.interpolationLinear(srgrConvParams[idx].coefficients[i],
                                                                    srgrConvParams[idx+1].coefficients[i], mu);
            }
            groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                                             srgrCoefficients, srgrConvParams[idx].ground_range_origin);
            if (groundRange < 0.0) {
                return -1.0;
            } else {
                return (groundRange - srgrConvParams[idx].ground_range_origin) / rangeSpacing;
            }

        } else { // slant range image

            return (slantRange - nearEdgeSlantRange) / rangeSpacing;
        }
    }

    /**
     * Compute ground range for given slant range.
     * @param sourceImageWidth The source image width.
     * @param rangeSpacing The range spacing.
     * @param slantRange The salnt range in meters.
     * @param srgrCoeff The SRGR coefficients for converting ground range to slant range.
     *                  Here it is assumed that the polinomial is given by
     *                  c0 + c1*x + c2*x^2 + ... + cn*x^n, where {c0, c1, ..., cn} are the SRGR coefficients.
     * @param ground_range_origin The ground range origin.
     * @return The ground range in meters.
     */
    public static double computeGroundRange(final int sourceImageWidth, final double rangeSpacing,
                                            final double slantRange, final double[] srgrCoeff,
                                            final double ground_range_origin) {

        // binary search is used in finding the ground range for given slant range
        double lowerBound = ground_range_origin;
        double upperBound = ground_range_origin + sourceImageWidth*rangeSpacing;
        final double lowerBoundSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(lowerBound, srgrCoeff);
        final double upperBoundSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(upperBound, srgrCoeff);

        if (slantRange < lowerBoundSlantRange || slantRange > upperBoundSlantRange) {
            return -1.0;
        }

        // start binary search
        double midSlantRange;
        while(upperBound - lowerBound > 0.0) {

            final double mid = (lowerBound + upperBound)/2.0;
            midSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(mid, srgrCoeff);
            if (Math.abs(midSlantRange - slantRange) < 0.1) {
                return mid;
            } else if (midSlantRange < slantRange) {
                lowerBound = mid;
            } else if (midSlantRange > slantRange) {
                upperBound = mid;
            }
        }

        return -1.0;
    }

    /**
     * Get unit for the source band corresponding to the given target band.
     * @param bandName The target band name.
     * @return The source band unit.
     */
    private Unit.UnitType getBandUnit(String bandName) {
        final Band[] srcBands = targetBandNameToSourceBand.get(bandName);
        return Unit.getUnitType(srcBands[0]);
    }

    /**
     * Compute orthorectified pixel value for given pixel.
     * @param azimuthIndex The azimuth index for pixel in source image.
     * @param rangeIndex The range index for pixel in source image.
     * @param tileData The source tile information.
     * @param bandUnit The corresponding source band unit.
     * @param subSwathIndex The subSwath index.
     * @return The pixel value.
     */
    private double getPixelValue(final double azimuthIndex, final double rangeIndex,
                                 final TileData tileData, Unit.UnitType bandUnit, int[] subSwathIndex) {

        try {
            final int x0 = (int)(rangeIndex + 0.5);
            final int y0 = (int)(azimuthIndex + 0.5);
            final Band[] srcBands = targetBandNameToSourceBand.get(tileData.bandName);
            Rectangle srcRect = null;
            Tile sourceTileI, sourceTileQ = null;

            if (imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {

                srcRect = new Rectangle(x0, y0, 1, 1);

            } else if (imgResampling.equals(Resampling.BILINEAR_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 1), Math.max(0, y0 - 1), 3, 3);

            } else if (imgResampling.equals(Resampling.CUBIC_CONVOLUTION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else if (imgResampling.equals(Resampling.BISINC_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 3), Math.max(0, y0 - 3), 6, 6);

            } else if (imgResampling.equals(Resampling.BICUBIC_INTERPOLATION)) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else {
                throw new OperatorException("Unhandled interpolation method");
            }

            sourceTileI = getSourceTile(srcBands[0], srcRect);
            if (srcBands.length > 1) {
                sourceTileQ = getSourceTile(srcBands[1], srcRect);
            }

            final ResamplingRaster imgResamplingRaster = new ResamplingRaster(
                    rangeIndex, azimuthIndex, isPolsar, tileData, bandUnit, sourceTileI, sourceTileQ, calibrator);

            final Resampling resampling = imgResampling;
            final Resampling.Index imgResamplingIndex = resampling.createIndex();

            resampling.computeIndex(rangeIndex + 0.5, azimuthIndex + 0.5,
                                       sourceImageWidth, sourceImageHeight, imgResamplingIndex);

            float v = resampling.resample(imgResamplingRaster, imgResamplingIndex);

            subSwathIndex[0] = imgResamplingRaster.getSubSwathIndex();

            return v;

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return 0;
    }

    public static void setLocalGeometry(final int x, final int y, final TileGeoreferencing tileGeoRef,
                                        final double[] earthPoint, final double[] sensorPos,
                                        final LocalGeometry localGeometry) {
        final GeoPos geo = new GeoPos();

        tileGeoRef.getGeoPos(x-1, y, geo);
        localGeometry.leftPointLat  = geo.lat;
        localGeometry.leftPointLon  = geo.lon;

        tileGeoRef.getGeoPos(x+1, y, geo);
        localGeometry.rightPointLat = geo.lat;
        localGeometry.rightPointLon = geo.lon;

        tileGeoRef.getGeoPos(x, y-1, geo);
        localGeometry.upPointLat    = geo.lat;
        localGeometry.upPointLon    = geo.lon;

        tileGeoRef.getGeoPos(x, y+1, geo);
        localGeometry.downPointLat  = geo.lat;
        localGeometry.downPointLon  = geo.lon;
        localGeometry.centrePoint   = earthPoint;
        localGeometry.sensorPos     = sensorPos;
    }

    /**
     * Compute projected local incidence angle (in degree).
     * @param lg Object holding local geometry information.
     * @param saveLocalIncidenceAngle Boolean flag indicating saving local incidence angle.
     * @param saveProjectedLocalIncidenceAngle Boolean flag indicating saving projected local incidence angle.
     * @param saveSigmaNought Boolean flag indicating applying radiometric calibration.
     * @param x0 The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0 The y coordinate of the pixel at the upper left corner of current tile.
     * @param x The x coordinate of the current pixel.
     * @param y The y coordinate of the current pixel.
     * @param localDEM The local DEM.
     * @param localIncidenceAngles The local incidence angle and projected local incidence angle.
     */
    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final float demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final float[][] localDEM, final double[] localIncidenceAngles) {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.

        for (int i = 0; i < 3; i++) {
            final int yy = y-y0+i;
            for (int j = 0; j < 3; j++) {
                if (localDEM[yy][x-x0+j] == demNoDataValue) {
                    return;
                }
            }
        }

        final int yy = y - y0;
        final int xx = x - x0;
        final double rightPointHeight = (localDEM[yy][xx + 2] +
                                         localDEM[yy + 1][xx + 2] +
                                         localDEM[yy + 2][xx + 2]) / 3.0;

        final double leftPointHeight = (localDEM[yy][xx] +
                                         localDEM[yy + 1][xx] +
                                         localDEM[yy + 2][xx]) / 3.0;

        final double upPointHeight = (localDEM[yy][xx] +
                                        localDEM[yy][xx + 1] +
                                        localDEM[yy][xx + 2]) / 3.0;

        final double downPointHeight = (localDEM[yy + 2][xx] +
                                        localDEM[yy + 2][xx + 1] +
                                        localDEM[yy + 2][xx + 2]) / 3.0;

        final double[] rightPoint = new double[3];
        final double[] leftPoint = new double[3];
        final double[] upPoint = new double[3];
        final double[] downPoint = new double[3];

        GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
        GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
        GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
        GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

        final double[] a = {rightPoint[0] - leftPoint[0], rightPoint[1] - leftPoint[1], rightPoint[2] - leftPoint[2]};
        final double[] b = {downPoint[0] - upPoint[0], downPoint[1] - upPoint[1], downPoint[2] - upPoint[2]};
        final double[] c = {lg.centrePoint[0], lg.centrePoint[1], lg.centrePoint[2]};

        final double[] n = {a[1]*b[2] - a[2]*b[1],
                            a[2]*b[0] - a[0]*b[2],
                            a[0]*b[1] - a[1]*b[0]}; // ground plane normal
        MathUtils.normalizeVector(n);
        if (MathUtils.innerProduct(n, c) < 0) {
            n[0] = -n[0];
            n[1] = -n[1];
            n[2] = -n[2];
        }

        final double[] s = {lg.sensorPos[0] - lg.centrePoint[0],
                            lg.sensorPos[1] - lg.centrePoint[1],
                            lg.sensorPos[2] - lg.centrePoint[2]};
        MathUtils.normalizeVector(s);

        final double nsInnerProduct = MathUtils.innerProduct(n, s);

        if (saveLocalIncidenceAngle) { // local incidence angle
            localIncidenceAngles[0] = Math.acos(nsInnerProduct) * org.esa.beam.util.math.MathUtils.RTOD;
        }

        if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
            final double[] m = {s[1]*c[2] - s[2]*c[1], s[2]*c[0] - s[0]*c[2], s[0]*c[1] - s[1]*c[0]}; // range plane normal
            MathUtils.normalizeVector(m);
            final double mnInnerProduct = MathUtils.innerProduct(m, n);
            final double[] n1 = {n[0] - m[0]*mnInnerProduct, n[1] - m[1]*mnInnerProduct, n[2] - m[2]*mnInnerProduct};
            MathUtils.normalizeVector(n1);
            localIncidenceAngles[1] = Math.acos(MathUtils.innerProduct(n1, s)) * org.esa.beam.util.math.MathUtils.RTOD;
        }
    }

    /**
     * Get azimuth pixel spacing (in m).
     * @param srcProduct The source product.
     * @return The azimuth pixel spacing.
     * @throws Exception The exception.
     */
    public static double getAzimuthPixelSpacing(Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        return AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
    }

    /**
     * Get range pixel spacing (in m).
     * @param srcProduct The source product.
     * @return The range pixel spacing.
     * @throws Exception The exception.
     */
    public static double getRangePixelSpacing(Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return rangeSpacing;
        } else {
            return rangeSpacing/Math.sin(getIncidenceAngleAtCentreRangePixel(srcProduct));
        }
    }

    /**
     * Compute pixel spacing (in m).
     * @param srcProduct The source product.
     * @return The pixel spacing.
     * @throws Exception The exception.
     */
    public static double getPixelSpacing(Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final double azimuthSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return Math.min(rangeSpacing, azimuthSpacing);
        } else {
            return Math.min(rangeSpacing/Math.sin(getIncidenceAngleAtCentreRangePixel(srcProduct)), azimuthSpacing);
        }
    }

    /**
     * Compute pixel spacing in degrees.
     * @param pixelSpacingInMeter Pixel spacing in meters.
     * @return The pixel spacing in degrees.
     */
    public static double getPixelSpacingInDegree(double pixelSpacingInMeter) {
        return pixelSpacingInMeter / Constants.semiMajorAxis * org.esa.beam.util.math.MathUtils.RTOD;
//        return pixelSpacingInMeter / Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.RTOD;
    }

    /**
     * Compute pixel spacing in meters.
     * @param pixelSpacingInDegree Pixel spacing in degrees.
     * @return The pixel spacing in meters.
     */
    public static double getPixelSpacingInMeter(double pixelSpacingInDegree) {
        return pixelSpacingInDegree * Constants.semiMinorAxis * org.esa.beam.util.math.MathUtils.DTOR;
//        return pixelSpacingInDegree * Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.DTOR;
    }

    /**
     * Set flag for radiometric correction. This function is for unit test only.
     * @param flag The flag.
     */
    void setApplyRadiometricCalibration(boolean flag) {
        saveSelectedSourceBand = !flag;
        applyRadiometricNormalization = flag;
        saveSigmaNought = flag;
    }

    void setSourceBandNames(String[] names) {
        sourceBandNames = names;
    }

    public static class TileData {
        Tile targetTile = null;
        ProductData tileDataBuffer = null;
        String bandName = null;
        String bandPolar = "";
        double noDataValue = 0;
        boolean applyRadiometricNormalization = false;
        boolean applyRetroCalibration = false;
    }

    public static class LocalGeometry {
        public double leftPointLat;
        public double leftPointLon;
        public double rightPointLat;
        public double rightPointLon;
        public double upPointLat;
        public double upPointLon;
        public double downPointLat;
        public double downPointLon;
        public double[] sensorPos;
        public double[] centrePoint;
    }


 //================================== Create Sigma0, Gamma0 and Beta0 virtual bands ====================================

    /**
     * Create Sigma0 image as a virtual band using incidence angle from ellipsoid.
     */
    public static void createSigmaNoughtVirtualBand(Product targetProduct, String incidenceAngleForSigma0) {

        if (incidenceAngleForSigma0.contains(USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM)) {
            return;
        }

        final Band[] bands = targetProduct.getBands();
        for(Band trgBand : bands) {

            final String trgBandName = trgBand.getName();
            if (trgBand instanceof VirtualBand || !trgBandName.contains("Sigma0")) {
                continue;
            }

            String expression = null;
            String sigmaNoughtVirtualBandName = null;
            String description = null;

            if (incidenceAngleForSigma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) {

                expression = trgBandName +
                             "==" + trgBand.getNoDataValue() + "?" + trgBand.getNoDataValue() +
                             ":" + trgBandName + " / sin(projectedIncidenceAngle * PI/180.0)" +
                             " * sin(incidenceAngleFromEllipsoid * PI/180)";

                sigmaNoughtVirtualBandName = trgBandName + "_use_inci_angle_from_ellipsoid";

                description = "Sigma0 image created using inci angle from ellipsoid";

            } else if (incidenceAngleForSigma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) {

                expression = trgBandName +
                             "==" + trgBand.getNoDataValue() + "?" + trgBand.getNoDataValue() +
                             ":" + trgBandName + " / sin(projectedIncidenceAngle * PI/180.0)" +
                             " * sin(incidenceAngle * PI/180)";

                sigmaNoughtVirtualBandName = trgBandName + "_use_local_inci_angle_from_dem";

                description = "Sigma0 image created using local inci angle from DEM";
            }

            final VirtualBand band = new VirtualBand(sigmaNoughtVirtualBandName,
                                                     ProductData.TYPE_FLOAT32,
                                                     trgBand.getSceneRasterWidth(),
                                                     trgBand.getSceneRasterHeight(),
                                                     expression);
            band.setUnit(trgBand.getUnit());
            band.setDescription(description);
            targetProduct.addBand(band);
        }
    }

    /**
     * Create Gamma0 image as a virtual band.
     */
    public static void createGammaNoughtVirtualBand(Product targetProduct, String incidenceAngleForGamma0) {

        final Band[] bands = targetProduct.getBands();
        for(Band trgBand : bands) {

            final String trgBandName = trgBand.getName();
            if (trgBand instanceof VirtualBand || !trgBandName.contains("Sigma0")) {
                continue;
            }

            final String incidenceAngle;
            if (incidenceAngleForGamma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) {
                incidenceAngle = "incidenceAngleFromEllipsoid";
            } else if (incidenceAngleForGamma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) {
                incidenceAngle = "incidenceAngle";
            } else { // USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM
                incidenceAngle = "projectedIncidenceAngle";
            }

            final String expression = trgBandName +
                         "==" + trgBand.getNoDataValue() + "?" + trgBand.getNoDataValue() +
                         ":" + trgBandName + " / sin(projectedIncidenceAngle * PI/180.0)" +
                         " * sin(" + incidenceAngle + " * PI/180)" + " / cos(" + incidenceAngle + " * PI/180)";

            String gammaNoughtVirtualBandName;
            String description;
            if (incidenceAngleForGamma0.contains(USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) {
                gammaNoughtVirtualBandName = "_use_inci_angle_from_ellipsoid";
                description = "Gamma0 image created using inci angle from ellipsoid";
            } else if (incidenceAngleForGamma0.contains(USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) {
                gammaNoughtVirtualBandName = "_use_local_inci_angle_from_dem";
                description = "Gamma0 image created using local inci angle from DEM";
            } else { // USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM
                gammaNoughtVirtualBandName = "_use_projected_local_inci_angle_from_dem";
                description = "Gamma0 image created using projected local inci angle from dem";
            }

            if(trgBandName.contains("_HH")) {
                gammaNoughtVirtualBandName = "Gamma0_HH" + gammaNoughtVirtualBandName;
            } else if(trgBandName.contains("_VV")) {
                gammaNoughtVirtualBandName = "Gamma0_VV" + gammaNoughtVirtualBandName;
            } else if(trgBandName.contains("_HV")) {
                gammaNoughtVirtualBandName = "Gamma0_HV" + gammaNoughtVirtualBandName;
            } else if(trgBandName.contains("_VH")) {
                gammaNoughtVirtualBandName = "Gamma0_VH" + gammaNoughtVirtualBandName;
            } else {
                gammaNoughtVirtualBandName = "Gamma0" + gammaNoughtVirtualBandName;
            }

            final VirtualBand band = new VirtualBand(gammaNoughtVirtualBandName,
                                                     ProductData.TYPE_FLOAT32,
                                                     trgBand.getSceneRasterWidth(),
                                                     trgBand.getSceneRasterHeight(),
                                                     expression);
            band.setUnit(trgBand.getUnit());
            band.setDescription(description);
            targetProduct.addBand(band);
        }
    }

    /**
     * Create Beta0 image as a virtual band.
     */
    public static void createBetaNoughtVirtualBand(final Product targetProduct) {

        final Band[] bands = targetProduct.getBands();
        for(Band trgBand : bands) {

            final String trgBandName = trgBand.getName();
            if (trgBand instanceof VirtualBand || !trgBandName.contains("Sigma0")) {
                continue;
            }

            final String expression = trgBandName +
                         "==" + trgBand.getNoDataValue() + "?" + trgBand.getNoDataValue() +
                         ":" + trgBandName + " / sin(projectedIncidenceAngle * PI/180.0)";

            String betaNoughtVirtualBandName = "Beta0";
            final VirtualBand band = new VirtualBand(betaNoughtVirtualBandName,
                                                     ProductData.TYPE_FLOAT32,
                                                     trgBand.getSceneRasterWidth(),
                                                     trgBand.getSceneRasterHeight(),
                                                     expression);
            band.setUnit(trgBand.getUnit());
            band.setDescription("Beta0 image");
            targetProduct.addBand(band);
        }
    }

    public static class ResamplingRaster implements Resampling.Raster {

        private final double rangeIndex, azimuthIndex;
        private final boolean isPolsar;
        private final TileData tileData;
        private final Unit.UnitType bandUnit;
        private final Tile sourceTileI, sourceTileQ;
        private final double noDataValue;
        private final ProductData dataBufferI, dataBufferQ;
        private final Calibrator calibrator;
        private int subSwathIndex;

        public ResamplingRaster(final double rangeIndex, final double azimuthIndex, final boolean isPolsar,
                                final TileData tileData, Unit.UnitType bandUnit, final Tile sourceTileI,
                                final Tile sourceTileQ, final Calibrator calibrator) {

            this.rangeIndex = rangeIndex;
            this.azimuthIndex = azimuthIndex;
            this.isPolsar = isPolsar;
            this.tileData = tileData;
            this.bandUnit = bandUnit;
            this.sourceTileI = sourceTileI;
            this.sourceTileQ = sourceTileQ;

            this.dataBufferI = sourceTileI.getDataBuffer();
            if (sourceTileQ != null) {
                this.dataBufferQ = sourceTileQ.getDataBuffer();
            } else {
                this.dataBufferQ = null;
            }

            this.noDataValue = sourceTileI.getRasterDataNode().getNoDataValue();
            this.calibrator = calibrator;
        }

        public final int getWidth() {
            return sourceTileI.getWidth();
        }

        public final int getHeight() {
            return sourceTileI.getHeight();
        }

        public void getSamples(int[] x, int[] y, float[][] samples) {

            int[][] subSwathIndices = new int[y.length][x.length];
            boolean allPixelsFromSameSubSwath = true;

            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {

                    double v = (float)dataBufferI.getElemDoubleAt(sourceTileI.getDataBufferIndex(x[j], y[i]));
                    if (noDataValue != 0 && (v == noDataValue)) {
                        samples[i][j] = (float)noDataValue;
                        continue;
                    } else {
                        samples[i][j] = (float)v;
                    }

                    if (!isPolsar && (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY)) {

                        final double vq = dataBufferQ.getElemDoubleAt(sourceTileQ.getDataBufferIndex(x[j], y[i]));
                        if (noDataValue != 0 && vq == noDataValue) {
                            samples[i][j] = (float)noDataValue;
                            continue;
                        }

                        samples[i][j] = (float)(v*v + vq*vq);
                    }

                    int[] subSwathIndex = {-1};
                    if (tileData.applyRetroCalibration) {
                        samples[i][j] = (float)calibrator.applyRetroCalibration(
                                x[j], y[i], samples[i][j], tileData.bandPolar, bandUnit, subSwathIndex);

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
                    float sample = samples[yIdx][xIdx];
                    for (int i = 0; i < y.length; i++) {
                        for (int j = 0; j < x.length; j++) {
                            samples[i][j] = sample;
                        }
                    }
                } else {
                    throw new OperatorException("Invalid x and y input for getSamples");
                }
            }
        }

        public int getSubSwathIndex() {
            return this.subSwathIndex;
        }
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
            super(RangeDopplerGeocodingOp.class);
            setOperatorUI(RangeDopplerGeocodingOpUI.class);
        }
    }
}
