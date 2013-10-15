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
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
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
import org.esa.nest.dat.dialogs.AutoCloseOptionPane;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.CalibrationFactory;
import org.esa.nest.datamodel.Calibrator;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.*;
import org.esa.nest.util.ResourceUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * The operator generates orthorectified image using rigorous SAR simulation.
 *
 * Some major steps of the procedure are given below:
 *
 * 1. SAR simulation: Generate simulated SAR image using DEM, the geocoding and orbit state vectors from the
 *    original SAR image, and mathematical modeling of SAR imaging geometry. The simulated SAR image will have
 *    the same dimension and resolution as the original image. For detailed steps and parameters used in SAR
 *    simulation, the reader is referred to NEST help for SAR Simulation Operator.
 *
 * 2. Co-registration: The simulated SAR image (master) and the original SAR image (slave) are co-registered
 *    and a WARP function is produced. The WARP function maps each pixel in the simulated SAR image to its
 *    corresponding position in the original SAR image. For detailed steps and parameters used in co-registration,
 *    the reader is referred to NEST help for GCP Selection Operator.
 *
 * 3. Terrain correction: Traverse DEM grid that covers the imaging area. For each cell in the DEM grid, compute
 *    its corresponding pixel position in the simulated SAR image using SAR model. Then its corresponding pixel
 *    position in the original SAR image can be found with the help of the WARP function. Finally the pixel
 *    value can be obtained for the orthorectified image.
 *
 * Reference: Guide to ASAR Geocoding, Issue 1.0, 19.03.2008
 */

@OperatorMetadata(alias="SARSim-Terrain-Correction",
        category = "Geometric\\Terrain Correction",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Orthorectification with SAR simulation")
public class SARSimTerrainCorrectionOp extends Operator {

    public static final String PRODUCT_SUFFIX = "_SimTC";
    
    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The RMS threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "1.0",
                label="RMS Threshold")
    private float rmsThreshold = 1.0f;

    @Parameter(description = "The order of WARP polynomial function", valueSet = {"1", "2", "3"}, defaultValue = "1",
                label="Warp Polynomial Order")
    private int warpPolynomialOrder = 1;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label="Image Resampling Method")
    private String imgResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    @Parameter(description = "The pixel spacing in meters", defaultValue = "0", label="Pixel Spacing (m)")
    private double pixelSpacingInMeter = 0;

    @Parameter(description = "The pixel spacing in degrees", defaultValue = "0", label="Pixel Spacing (deg)")
    private double pixelSpacingInDegree = 0;

    @Parameter(description = "The coordinate reference system in well known text format")
    private String mapProjection;

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

    @Parameter(valueSet = {Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID,
            Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label="")
    private String incidenceAngleForSigma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID,
            Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM},
            defaultValue = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label="")
    private String incidenceAngleForGamma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    @Parameter(valueSet = {CalibrationOp.LATEST_AUX, CalibrationOp.PRODUCT_AUX, CalibrationOp.EXTERNAL_AUX},
            description = "The auxiliary file", defaultValue=CalibrationOp.LATEST_AUX, label="Auxiliary File")
    private String auxFile = CalibrationOp.LATEST_AUX;

    @Parameter(description = "The antenne elevation pattern gain auxiliary data file.", label="External Aux File")
    private File externalAuxFile = null;

    @Parameter(description = "Show range and azimuth shifts file in a text viewer", defaultValue = "false", label="Show Range and Azimuth Shifts")
    private boolean openShiftsFile = false;

    @Parameter(description = "Show the Residuals file in a text viewer", defaultValue = "false", label = "Show Residuals")
    private boolean openResidualsFile = false;

    private ProductNodeGroup<Placemark> masterGCPGroup = null;
    private MetadataElement absRoot = null;
    private ElevationModel dem = null;
    private String demResamplingMethod;

    private boolean srgrFlag = false;
    private boolean saveLayoverShadowMask = false;
    private boolean saveIncidenceAngleFromEllipsoid = false;
    private boolean isElevationModelAvailable = false;
    private boolean usePreCalibrationOp = false;
    private boolean warpDataAvailable = false;
    private boolean fileOutput = false;

    private String demName = null;
    private Band elevationBand = null;

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
    private float demNoDataValue = 0; // no data value for DEM
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
    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private final Map<String, Boolean> targetBandapplyRadiometricNormalizationFlag = new HashMap<String, Boolean>();
    private final Map<String, Boolean> targetBandApplyRetroCalibrationFlag = new HashMap<String, Boolean>();
    private final Map<Band, WarpOp.WarpData> warpDataMap = new HashMap<Band, WarpOp.WarpData>(10);
    private String processedSlaveBand;

    private TiePointGrid incidenceAngle = null;
    private TiePointGrid latitude = null;
    private TiePointGrid longitude = null;

    private static final double NonValidZeroDopplerTime = -99999.0;
    private static final int INVALID_SUB_SWATH_INDEX = -1;

    private Resampling imgResampling = null;
    private CoordinateReferenceSystem targetCRS;

    private boolean useAvgSceneHeight = false;
    private Calibrator calibrator = null;
    private Band maskBand = null;
    private boolean skipBistaticCorrection = false;

    private boolean orthoDataProduced = false;  // check if any ortho data is actually produced
    private boolean processingStarted = false;
    private boolean isPolsar = false;
    private String mission = null;

    private boolean nearRangeOnLeft = true; // temp fix for descending Radarsat2
    private int maxIterations = 20;

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

            maskBand = sourceProduct.getBand(SARSimulationOp.layoverShadowMaskBandName);

            checkUserInput();

            getSourceImageDimension();

            getMetadata();

            getTiePointGrid();

            if (useAvgSceneHeight) {
                saveSigmaNought = false;
                saveGammaNought = false;
                saveBetaNought = false;
                saveDEM = false;
                saveLocalIncidenceAngle = false;
                saveProjectedLocalIncidenceAngle = false;
            }

            imgResampling = ResamplingFactory.createResampling(imgResamplingMethod);

            createTargetProduct();

            if(!useAvgSceneHeight) {
                getElevationModel();
            }

            processedSlaveBand = absRoot.getAttributeString("processed_slave");

            computeSensorPositionsAndVelocities();

            if (saveSigmaNought) {
                calibrator = CalibrationFactory.createCalibrator(sourceProduct);
                calibrator.setAuxFileFlag(auxFile);
                calibrator.setExternalAuxFile(externalAuxFile);
                calibrator.initialize(this, sourceProduct, targetProduct, true, true);
                calibrator.setIncidenceAngleForSigma0(incidenceAngleForSigma0);
            }

            updateTargetProductMetadata();

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
            throw new OperatorException("Please selecte output band for terrain corrected image");
        }

        if (!applyRadiometricNormalization) {
            saveSigmaNought = false;
            saveGammaNought = false;
            saveBetaNought = false;
        }

        if (saveBetaNought || saveGammaNought ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID))) {
            saveSigmaNought = true;
            saveProjectedLocalIncidenceAngle = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID)) ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID))) {
            saveIncidenceAngleFromEllipsoid = true;
        }

        if ((saveGammaNought && incidenceAngleForGamma0.contains(Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM)) ||
            (saveSigmaNought && incidenceAngleForSigma0.contains(Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM))) {
            saveLocalIncidenceAngle = true;
        }

        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
    }

    /**
     * Retrieve required data from Abstracted Metadata
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {
        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        mission = RangeDopplerGeocodingOp.getMissionType(absRoot);

        if (mission.contains("CSKS") || mission.contains("TSX") || mission.equals("RS2") || mission.contains("SENTINEL")) {
            skipBistaticCorrection = true;
        }

        srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);

        wavelength = OperatorUtils.getRadarFrequency(absRoot);

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

        // used for retro-calibration or when useAvgSceneHeight is true
        avgSceneHeight = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.avg_scene_height);

        MetadataAttribute attribute = absRoot.getAttribute("retro-calibration performed flag");
        if (attribute != null) {
            usePreCalibrationOp = true;
            if (!applyRadiometricNormalization) {
                throw new OperatorException("Apply radiometric normalization must be selected.");
            }
//            if (saveSelectedSourceBand) {
//                throw new OperatorException("Selected source band cannot be saved.");
//            }
        } else {
            if (applyRadiometricNormalization && mission.equals("ERS")) {
                throw new OperatorException("For radiometric normalization of ERS product, please use one of the following\n" +
                        " user graphs: 'RemoveAntPat_SARSim_GCPSelection' or 'RemoveAntPat_Multilook_SARSim_GCPSelection',\n" +
                        " then apply 'SARSim Terrain Correction' operator to the output in the Graph Builder.");
            }
        }

        nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);

        isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;
    }

    /**
     * Get elevation model.
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if(isElevationModelAvailable) return;

        demName = absRoot.getAttributeString(AbstractMetadata.DEM);
        demResamplingMethod = absRoot.getAttributeString("DEM resampling method");
        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {

            final File externalDemFile = new File(demName);
            dem = new FileElevationModel(externalDemFile, demResamplingMethod, demNoDataValue);
            demName = externalDemFile.getName();
            demNoDataValue = (float)absRoot.getAttributeDouble("external DEM no data value");
        } else {
            dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
            demNoDataValue = dem.getDescriptor().getNoDataValue();
        }

        if(elevationBand != null) {
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
     * Create target product.
     * @throws OperatorException The exception.
     */
    private void createTargetProduct() throws Exception {

        if (pixelSpacingInMeter <= 0.0) {
            pixelSpacingInMeter = Math.max(SARGeocoding.getAzimuthPixelSpacing(sourceProduct),
                                           SARGeocoding.getRangePixelSpacing(sourceProduct));
            pixelSpacingInDegree = SARGeocoding.getPixelSpacingInDegree(pixelSpacingInMeter);
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

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setDescription(sourceProduct.getDescription());

        try {
            OperatorUtils.copyIndexCodings(sourceProduct, targetProduct);
        } catch(Exception e) {
            if(!imgResampling.equals(Resampling.NEAREST_NEIGHBOUR)) {
                throw new OperatorException("Use Nearest Neighbour with Classificaitons: "+e.getMessage());
            }
        }

        addLayoverShadowBitmasks(targetProduct);
    }

    private static void addLayoverShadowBitmasks(final Product product) {
        final Band layoverShadowBand = product.getBand(SARSimulationOp.layoverShadowMaskBandName);
        if(layoverShadowBand != null) {
            final String maskName = layoverShadowBand.getName();
            final BitmaskDef layover = new BitmaskDef("Layover",
                        "", maskName + " == 1", Color.GREEN, 0.5f);
            final BitmaskDef shadow = new BitmaskDef("Shadow",
                        "", maskName + " == 2", Color.BLUE, 0.5f);
            final BitmaskDef layoverShadow = new BitmaskDef("Layover_and_Shadow",
                        "", maskName + " == 3", Color.YELLOW, 0.5f);

            product.addBitmaskDef(layover);
            product.addBitmaskDef(shadow);
            product.addBitmaskDef(layoverShadow);
        }
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = sourceProduct.getBands();
        if (sourceBands.length == 1) {
            throw new OperatorException("Source product should include a simulated intensity band. Only "+sourceBands[0].getName()+" found");
        }

        String targetBandName;
        for (int i = 1; i < sourceBands.length; i++) { // skip master band (i=0, simulated image)

            final Band srcBand = sourceBands[i];
            String bandName = srcBand.getName();
            if (bandName.contains(SARSimulationOp.layoverShadowMaskBandName)) {
                saveLayoverShadowMask = true;
                continue;
            }

            final String unit = srcBand.getUnit();
            if(unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            if (!isPolsar && (unit.contains(Unit.PHASE) || unit.contains(Unit.REAL) || unit.contains(Unit.IMAGINARY))) {
                throw new OperatorException("Only amplitude or intensity band should be used for orthorectification");
            }

            final String[] srcBandNames = {bandName};
            if (saveSigmaNought) {
                if (bandName.contains("HH")) {
                    targetBandName = "Sigma0_HH";
                } else if (bandName.contains("VV")) {
                    targetBandName = "Sigma0_VV";
                } else if (bandName.contains("HV")) {
                    targetBandName = "Sigma0_HV";
                } else if (bandName.contains("VH")) {
                    targetBandName = "Sigma0_VH";
                } else {
                    targetBandName = "Sigma0";
                }

                if (RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                        targetBandName, Unit.INTENSITY, srcBand, ProductData.TYPE_FLOAT32) != null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                    targetBandapplyRadiometricNormalizationFlag.put(targetBandName, true);
                    if (usePreCalibrationOp) {
                        targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                    } else {
                        targetBandApplyRetroCalibrationFlag.put(targetBandName, true);
                    }
                }
            }

            if (saveSelectedSourceBand) {
                targetBandName = bandName;
                int dataType = ProductData.TYPE_FLOAT32;
                // use original dataType for nearest neighbour and indexCoding bands
                if(imgResampling.equals(Resampling.NEAREST_NEIGHBOUR))
                    dataType = srcBand.getDataType();
                if (RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                                                          targetBandName, unit, srcBand, dataType) != null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                    targetBandapplyRadiometricNormalizationFlag.put(targetBandName, false);
                    targetBandApplyRetroCalibrationFlag.put(targetBandName, false);
                }
            }
        }

        if(saveDEM) {
            elevationBand = RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                    "elevation", Unit.METERS, null, ProductData.TYPE_FLOAT32);
        }

        if(saveLocalIncidenceAngle) {
            RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                    "localIncidenceAngle", Unit.DEGREES, null, ProductData.TYPE_FLOAT32);
        }

        if(saveProjectedLocalIncidenceAngle) {
            RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                    "projectedLocalIncidenceAngle", Unit.DEGREES, null, ProductData.TYPE_FLOAT32);
        }

        if (saveLayoverShadowMask) {
            final Band layoverShadowingMasksBand = new Band(SARSimulationOp.layoverShadowMaskBandName,
                                                     ProductData.TYPE_INT8,
                                                     targetImageWidth,
                                                     targetImageHeight);
            layoverShadowingMasksBand.setUnit(Unit.AMPLITUDE);
            targetProduct.addBand(layoverShadowingMasksBand);
        }

        if (saveIncidenceAngleFromEllipsoid) {
            RangeDopplerGeocodingOp.addTargetBand(targetProduct, targetImageWidth, targetImageHeight,
                    "incidenceAngleFromEllipsoid", Unit.DEGREES, null, ProductData.TYPE_FLOAT32);
        }

        if (saveSigmaNought && !incidenceAngleForSigma0.contains(Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM)) {
            CalibrationFactory.createSigmaNoughtVirtualBand(targetProduct, incidenceAngleForSigma0);
        }

        if (saveGammaNought) {
            CalibrationFactory.createGammaNoughtVirtualBand(targetProduct, incidenceAngleForGamma0);
        }

        if (saveBetaNought) {
            CalibrationFactory.createBetaNoughtVirtualBand(targetProduct);
        }
    }

    /**
     * Update metadata in the target product.
     * @throws OperatorException The exception.
     */
    private void updateTargetProductMetadata() throws OperatorException {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.srgr_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);

        final GeoCoding targetGeoCoding = targetProduct.getGeoCoding();
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
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        // map projection too
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.geo_ref_system, "WGS84");
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lat_pixel_res, delLat);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.lon_pixel_res, delLon);

        if (pixelSpacingInMeter > 0.0) {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, pixelSpacingInMeter);
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, pixelSpacingInMeter);
        }

        // save look directions for 5 range lines
        final MetadataElement lookDirectionListElem = new MetadataElement("Look_Direction_List");
        final int numOfDirections = 5;
        for(int i=1; i <= numOfDirections; ++i) {
            SARGeocoding.addLookDirection("look_direction", lookDirectionListElem, i, numOfDirections,
                    sourceImageWidth, sourceImageHeight, firstLineUTC, lineTimeInterval, nearRangeOnLeft, latitude,
                    longitude);
        }
        absTgt.addElement(lookDirectionListElem);
    }

    private synchronized void getWarpData(final Set<Band> keySet, final Rectangle targetRectangle) {
        if (warpDataAvailable) {
            return;
        }

        // find first real slave band
        for(Band targetBand : keySet) {
            if(targetBand.getName().equals(processedSlaveBand) || targetBand.getName().contains("Sigma0")) {
                final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
                final Band srcBand = sourceProduct.getBand(srcBandNames[0]);
                if(srcBand != null) {
                    final Tile sourceRaster = getSourceTile(srcBand, targetRectangle);
                    break;
                }
            }
        }

        // for all slave bands or band pairs compute a warp
        final Band masterBand = sourceProduct.getBandAt(0);
        masterGCPGroup = sourceProduct.getGcpGroup(masterBand);
        final int numSrcBands = sourceProduct.getNumBands();
        boolean appendFlag = false;
        for(int i = 1; i < numSrcBands; ++i) { // loop through all slave bands

            final Band srcBand = sourceProduct.getBandAt(i);
            final String unit = srcBand.getUnit();
            if(unit != null && unit.contains(Unit.BIT)) // skip layover_shadow_mask band
                continue;

            ProductNodeGroup<Placemark> slaveGCPGroup = sourceProduct.getGcpGroup(srcBand);
            if(slaveGCPGroup.getNodeCount() < 3) {
                // find others for same slave product
                for(Band band : sourceProduct.getBands()) {
                    if(band != srcBand && band != masterBand) {
                        slaveGCPGroup = sourceProduct.getGcpGroup(band);
                        if (slaveGCPGroup.getNodeCount() >= 3)        // only one band should have GCPs
                            break;
                    }
                }
            }

            final WarpOp.WarpData warpData = new WarpOp.WarpData(slaveGCPGroup);
            warpDataMap.put(srcBand, warpData);

            WarpOp.computeWARPPolynomialFromGCPs(sourceProduct, srcBand, warpPolynomialOrder, masterGCPGroup,
                                                 maxIterations, rmsThreshold, appendFlag, warpData);

            if (!appendFlag) {
                appendFlag = true;
            }
        }

        announceGCPWarning();

        warpDataAvailable = true;
    }

    private void announceGCPWarning() {
        String msg = "";
        for(Band srcBand : sourceProduct.getBands()) {
            final WarpOp.WarpData warpData = warpDataMap.get(srcBand);
            if(warpData != null && warpData.notEnoughGCPs) {
                msg += srcBand.getName() +" does not have enough valid GCPs for the warp\n";
                openResidualsFile = true;
            }
        }
        if(!msg.isEmpty()) {
            System.out.println(msg);
            if(VisatApp.getApp() != null) {
                AutoCloseOptionPane.showWarningDialog("Some bands did not coregister", msg);
            }
        }
    }

    private synchronized void outputResidualAndShiftFiles() {
        if (fileOutput) {
            return;
        }

        if(openShiftsFile) {
            final File shiftsFile = getFile(sourceProduct, "_shift.txt");
            if(Desktop.isDesktopSupported() && shiftsFile.exists()) {
                try {
                    Desktop.getDesktop().open(shiftsFile);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    // do nothing
                }
            }
        }

        if (openResidualsFile) {
            final File residualsFile = getFile(sourceProduct, "_residual.txt");
            if(Desktop.isDesktopSupported() && residualsFile.exists()) {
                try {
                    Desktop.getDesktop().open(residualsFile);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    // do nothing
                }
            }
        }

        fileOutput = true;
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

        SARGeocoding.computeSensorPositionsAndVelocities(orbitStateVectors, timeArray, xPosArray, yPosArray, zPosArray,
                sensorPosition, sensorVelocity, firstLineUTC, lineTimeInterval, sourceImageHeight);
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
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int ymax = y0 + h;
        final int xmax = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final GeoPos geoPos = new GeoPos();
        final double[] earthPoint = new double[3];
        final double[] sensorPos = new double[3];
        final int srcMaxRange = sourceImageWidth - 1;
        final int srcMaxAzimuth = sourceImageHeight - 1;
        ProductData demBuffer = null;
        ProductData localIncidenceAngleBuffer = null;
        ProductData projectedLocalIncidenceAngleBuffer = null;
        ProductData layoverShadowingMasksBuffer = null;
        ProductData incidenceAngleFromEllipsoidBuffer = null;

        final Set<Band> keySet = targetTiles.keySet();

        if(!warpDataAvailable) {
            getWarpData(keySet, targetRectangle);
            outputResidualAndShiftFiles();
        }

        final List<RangeDopplerGeocodingOp.TileData> trgTileList = new ArrayList<RangeDopplerGeocodingOp.TileData>();
        for(Band targetBand : keySet) {

            if(targetBand.getName().equals("elevation")) {
                demBuffer = targetTiles.get(targetBand).getDataBuffer();
                continue;
            }

            if(targetBand.getName().equals("localIncidenceAngle")) {
                localIncidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                continue;
            }

            if(targetBand.getName().equals("projectedLocalIncidenceAngle")) {
                projectedLocalIncidenceAngleBuffer = targetTiles.get(targetBand).getDataBuffer();
                continue;
            }

            if(targetBand.getName().equals(SARSimulationOp.layoverShadowMaskBandName)) {
                layoverShadowingMasksBuffer = targetTiles.get(targetBand).getDataBuffer();
                continue;
            }

            if (targetBand.getName().equals("incidenceAngleFromEllipsoid")) {
                incidenceAngleFromEllipsoidBuffer = targetTiles.get(targetBand).getDataBuffer();
                continue;
            }

            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band[] srcBands = new Band[] { sourceProduct.getBand(srcBandNames[0]),
                    srcBandNames.length > 1 ? sourceProduct.getBand(srcBandNames[1]) : null};

            final RangeDopplerGeocodingOp.TileData td = new RangeDopplerGeocodingOp.TileData(
                    targetTiles.get(targetBand), srcBands, isPolsar,
                    targetBand.getName(), getBandUnit(targetBand.getName()), absRoot, calibrator, imgResampling);

            td.applyRadiometricNormalization = targetBandapplyRadiometricNormalizationFlag.get(targetBand.getName());
            td.applyRetroCalibration = targetBandApplyRetroCalibrationFlag.get(targetBand.getName());
            trgTileList.add(td);
        }
        final RangeDopplerGeocodingOp.TileData[] trgTiles = trgTileList.toArray(new RangeDopplerGeocodingOp.TileData[trgTileList.size()]);
        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0-1, y0-1, w+2, h+2);

        try {
            final double[][] localDEM = new double[h+2][w+2];
            if(useAvgSceneHeight) {
                DEMFactory.fillDEM(localDEM, (float) avgSceneHeight);
            } else {
                final boolean valid = DEMFactory.getLocalDEM(
                        dem, demNoDataValue, demResamplingMethod, tileGeoRef, x0, y0, w, h, sourceProduct, true, localDEM);
                if(!valid)
                    return;
            }

            for (int y = y0; y < ymax; y++) {
                final int yy = y-y0+1;

                for (int x = x0; x < xmax; x++) {

                    final int index = trgTiles[0].targetTile.getDataBufferIndex(x, y);

                    final double alt = localDEM[yy][x-x0+1];

                    if(saveDEM) {
                        demBuffer.setElemDoubleAt(index, alt);
                    }

                    if (!useAvgSceneHeight && alt == demNoDataValue) {
                        //saveNoDataValueToTarget(index, trgTiles);
                        continue;
                    }

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    if(!geoPos.isValid()) {
                        continue;
                    }
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    GeoUtils.geo2xyzWGS84(lat, lon, alt, earthPoint);

                    final double zeroDopplerTime = getEarthPointZeroDopplerTime(earthPoint);

                    if (Double.compare(zeroDopplerTime, NonValidZeroDopplerTime) == 0) {
                        //saveNoDataValueToTarget(index, trgTiles);
                        continue;
                    }

                    double slantRange = SARGeocoding.computeSlantRange(
                            zeroDopplerTime, timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);

                    double zeroDoppler = zeroDopplerTime;
                    if (!skipBistaticCorrection) {
                        // skip bistatic correction for COSMO, TerraSAR-X and RadarSAT-2
                        zeroDoppler = zeroDopplerTime + slantRange / Constants.lightSpeedInMetersPerDay;

                        slantRange = SARGeocoding.computeSlantRange(
                            zeroDoppler, timeArray, xPosArray, yPosArray, zPosArray, earthPoint, sensorPos);
                    }

                    final double azimuthIndex = (zeroDoppler - firstLineUTC) / lineTimeInterval;

                    double rangeIndex = SARGeocoding.computeRangeIndex(srgrFlag, sourceImageWidth, firstLineUTC, lastLineUTC,
                            rangeSpacing, zeroDoppler, slantRange, nearEdgeSlantRange, srgrConvParams);

                    // temp fix for descending Radarsat2
                    if (!nearRangeOnLeft) {
                        rangeIndex = srcMaxRange - rangeIndex;
                    }

                    if (!SARGeocoding.isValidCell(rangeIndex, azimuthIndex, lat, lon, latitude, longitude,
                            srcMaxRange, srcMaxAzimuth, sensorPos)) {
                        //saveNoDataValueToTarget(index, trgTiles);
                    } else {
                        final double[] localIncidenceAngles =
                                {SARGeocoding.NonValidIncidenceAngle, SARGeocoding.NonValidIncidenceAngle};

                        if (saveLocalIncidenceAngle || saveProjectedLocalIncidenceAngle || saveSigmaNought) {

                            final LocalGeometry localGeometry = new LocalGeometry(x, y, tileGeoRef, earthPoint, sensorPos);

                            SARGeocoding.computeLocalIncidenceAngle(
                                    localGeometry, demNoDataValue, saveLocalIncidenceAngle, saveProjectedLocalIncidenceAngle,
                                    saveSigmaNought, x0, y0, x, y, localDEM, localIncidenceAngles); // in degrees

                            if (saveLocalIncidenceAngle && localIncidenceAngles[0] != SARGeocoding.NonValidIncidenceAngle) {
                                localIncidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[0]);
                            }

                            if (saveProjectedLocalIncidenceAngle && localIncidenceAngles[1] != SARGeocoding.NonValidIncidenceAngle) {
                                projectedLocalIncidenceAngleBuffer.setElemDoubleAt(index, localIncidenceAngles[1]);
                            }
                        }

                        if (saveLayoverShadowMask) {
                            final Rectangle srcRect = new Rectangle((int)(rangeIndex+0.5), (int)(azimuthIndex+0.5), 1, 1);
                            final Tile sourceTile = getSourceTile(maskBand, srcRect);
                            final int m = sourceTile.getDataBuffer().getElemIntAt(sourceTile.getDataBufferIndex(
                                    (int)(rangeIndex+0.5), (int)(azimuthIndex+0.5)));
                            layoverShadowingMasksBuffer.setElemIntAt(index, m);
                        }

                        if (saveIncidenceAngleFromEllipsoid) {
                            incidenceAngleFromEllipsoidBuffer.setElemDoubleAt(
                                    index, incidenceAngle.getPixelFloat((float)rangeIndex, (float)azimuthIndex));
                        }

                        for(RangeDopplerGeocodingOp.TileData tileData : trgTiles) {

                            final Unit.UnitType bandUnit = getBandUnit(tileData.bandName);
                            final String[] srcBandName = targetBandNameToSourceBandName.get(tileData.bandName);
                            final Band srcBand = sourceProduct.getBand(srcBandName[0]);
                            final PixelPos pixelPos = new PixelPos();
                            final WarpOp.WarpData warpData = warpDataMap.get(srcBand);
                            if (warpData.notEnoughGCPs) {
                                continue;
                            }
                            WarpOp.getWarpedCoords(warpData, warpPolynomialOrder,
                                                   rangeIndex, azimuthIndex, pixelPos);
                            if (pixelPos.x < 0.0 || pixelPos.x >= srcMaxRange || pixelPos.y < 0.0 || pixelPos.y >= srcMaxAzimuth) {
                                tileData.tileDataBuffer.setElemDoubleAt(index, tileData.noDataValue);
                                continue;
                            }

                            final int[] subSwathIndex = {INVALID_SUB_SWATH_INDEX};
                            double v = getPixelValue(pixelPos.y, pixelPos.x, tileData, subSwathIndex);

                            if (v != tileData.noDataValue && tileData.applyRadiometricNormalization) {

                                if (localIncidenceAngles[1] != SARGeocoding.NonValidIncidenceAngle) {
                                    final double satelliteHeight = Math.sqrt(
                                            sensorPos[0]*sensorPos[0] + sensorPos[1]*sensorPos[1] + sensorPos[2]*sensorPos[2]);

                                    final double sceneToEarthCentre = Math.sqrt(
                                            earthPoint[0]*earthPoint[0] + earthPoint[1]*earthPoint[1] + earthPoint[2]*earthPoint[2]);

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
        } catch(Throwable e) {
            orthoDataProduced = true; //to prevent multiple error messages
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Save noDataValue to target pixel with given index.
     * @param index The pixel index in target image.
     * @param trgTiles The target tiles.
     */
    private static void saveNoDataValueToTarget(final int index, RangeDopplerGeocodingOp.TileData[] trgTiles) {
        for(RangeDopplerGeocodingOp.TileData tileData : trgTiles) {
            tileData.tileDataBuffer.setElemDoubleAt(index, tileData.noDataValue);
        }
    }

    /**
     * Compute zero Doppler time for given erath point.
     * @param earthPoint The earth point in xyz cooordinate.
     * @return The zero Doppler time in days if it is found, -1 otherwise.
     * @throws OperatorException The operator exception.
     */
    private double getEarthPointZeroDopplerTime(final double[] earthPoint) throws OperatorException {

        // binary search is used in finding the zero doppler time
        int lowerBound = 0;
        int upperBound = sensorPosition.length - 1;
        double lowerBoundFreq = getDopplerFrequency(lowerBound, earthPoint);
        double upperBoundFreq = getDopplerFrequency(upperBound, earthPoint);

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
            midFreq = getDopplerFrequency(mid, earthPoint);
            if (Double.compare(midFreq, 0.0) == 0) {
                return firstLineUTC + mid*lineTimeInterval;
            } else if (midFreq*lowerBoundFreq > 0.0) {
                lowerBound = mid;
                lowerBoundFreq = midFreq;
            } else if (midFreq*upperBoundFreq > 0.0) {
                upperBound = mid;
                upperBoundFreq = midFreq;
            }
        }

        final double y0 = lowerBound - lowerBoundFreq*(upperBound - lowerBound)/(upperBoundFreq - lowerBoundFreq);
        return firstLineUTC + y0*lineTimeInterval;
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     * @param y The index for given range line.
     * @param earthPoint The earth point in xyz coordinate.
     * @return The Doppler frequency in Hz.
     */
    private double getDopplerFrequency(final int y, final double[] earthPoint) {

        if (y < 0 || y > sourceImageHeight - 1) {
            throw new OperatorException("Invalid range line index: " + y);
        }
        
        final double xVel = sensorVelocity[y][0];
        final double yVel = sensorVelocity[y][1];
        final double zVel = sensorVelocity[y][2];
        final double xDiff = earthPoint[0] - sensorPosition[y][0];
        final double yDiff = earthPoint[1] - sensorPosition[y][1];
        final double zDiff = earthPoint[2] - sensorPosition[y][2];
        final double distance = Math.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff);

        return 2.0 * (xVel*xDiff + yVel*yDiff + zVel*zDiff) / (distance*wavelength);
    }

    /**
     * Get unit for the source band corresponding to the given target band.
     * @param bandName The target band name.
     * @return The source band unit.
     */
    private Unit.UnitType getBandUnit(final String bandName) {
        final String[] srcBandNames = targetBandNameToSourceBandName.get(bandName);
        return Unit.getUnitType(sourceProduct.getBand(srcBandNames[0]));
    }

    /**
     * Compute orthorectified pixel value for given pixel.
     * @param azimuthIndex The azimuth index for pixel in source image.
     * @param rangeIndex The range index for pixel in source image.
     * @param tileData The source tile information.
     * @param subSwathIndex The subswath index.
     * @return The pixel value.
     */
    private double getPixelValue(final double azimuthIndex, final double rangeIndex,
                                 final RangeDopplerGeocodingOp.TileData tileData,
                                 final int[] subSwathIndex) {

        try {
            final int x0 = (int)(rangeIndex + 0.5);
            final int y0 = (int)(azimuthIndex + 0.5);
            Rectangle srcRect = null;
            Tile sourceTileI, sourceTileQ = null;

            if (imgResampling == Resampling.NEAREST_NEIGHBOUR) {

                srcRect = new Rectangle(x0, y0, 1, 1);

            } else if (imgResampling == Resampling.BILINEAR_INTERPOLATION) {

                srcRect = new Rectangle(Math.max(0, x0 - 1), Math.max(0, y0 - 1), 3, 3);

            } else if (imgResampling == Resampling.CUBIC_CONVOLUTION) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else if (imgResampling == Resampling.BISINC_INTERPOLATION) {

                srcRect = new Rectangle(Math.max(0, x0 - 3), Math.max(0, y0 - 3), 6, 6);

            } else if (imgResampling == Resampling.BICUBIC_INTERPOLATION) {

                srcRect = new Rectangle(Math.max(0, x0 - 2), Math.max(0, y0 - 2), 5, 5);

            } else {
                throw new OperatorException("Unhandled interpolation method");
            }

            final String[] srcBandNames = targetBandNameToSourceBandName.get(tileData.bandName);
            sourceTileI = getSourceTile(sourceProduct.getBand(srcBandNames[0]), srcRect);
            if (srcBandNames.length > 1) {
                sourceTileQ = getSourceTile(sourceProduct.getBand(srcBandNames[1]), srcRect);
            }

            tileData.imgResamplingRaster.set(rangeIndex, azimuthIndex, sourceTileI, sourceTileQ);

            imgResampling.computeIndex(rangeIndex + 0.5, azimuthIndex + 0.5,
                                       sourceImageWidth, sourceImageHeight, tileData.imgResamplingIndex);

            double v = imgResampling.resample(tileData.imgResamplingRaster, tileData.imgResamplingIndex);

            subSwathIndex[0] = tileData.imgResamplingRaster.getSubSwathIndex();

            return v;

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return 0;
    }

    private void outputGCPShifts(
            final WarpOp.WarpData warpData, final String bandName, boolean appendFlag)
            throws OperatorException {

        final File shiftsFile = getFile(sourceProduct, "_shift.txt");
        PrintStream p = null; // declare a print stream object

        try {
            final FileOutputStream out = new FileOutputStream(shiftsFile.getAbsolutePath(), appendFlag);

            // Connect print stream to the output stream
            p = new PrintStream(out);

            p.println();
            p.println("Band: " + bandName);
            p.println();
            p.print("Range and Azimuth Shifts for Valid GCPs:");
            p.println();

            p.println();
            p.println("No. | Range Shift (m) | Azimuth Shift (m) |");
            p.println("-------------------------------------------");

            double meanRangeShift = 0.0;
            double meanAzimuthShift = 0.0;
            for (int i = 0; i < warpData.numValidGCPs; i++) {

                // get final slave GCP position
                final Placemark sPin = warpData.slaveGCPList.get(i);
                final PixelPos sGCPPos = sPin.getPixelPos();

                // get initial slave GCP position
                // Note: master GCP position is the same as the initial slave GCP position because master and slave
                //       now share the same geocoding.
                final Placemark mPin = masterGCPGroup.get(sPin.getName());
                final PixelPos mGCPPos = mPin.getPixelPos();

                // compute range and azimuth shifts
                final double rangeShift = Math.abs(sGCPPos.x - mGCPPos.x) * rangeSpacing; // in m
                final double azimuthShift = Math.abs(sGCPPos.y - mGCPPos.y) * azimuthSpacing; // in m

                meanRangeShift += rangeShift;
                meanAzimuthShift += azimuthShift;

                p.format("%2d  |%16.3f |%18.3f |", i, rangeShift, azimuthShift);
                p.println();
            }

            if (warpData.numValidGCPs > 0) {
                meanRangeShift /= warpData.numValidGCPs;
                meanAzimuthShift /= warpData.numValidGCPs;
            } else {
                p.println("No valid GCP is available.");
            }

            p.println();
            p.format("Mean Range Shift = %8.3f", meanRangeShift);
            p.println();
            p.format("Mean Azimuth Shift = %8.3f", meanAzimuthShift);
            p.println();

        } catch(IOException exc) {
            throw new OperatorException(exc);
        } finally {
            if(p != null)
                p.close();
        }
    }

    private static File getFile(final Product sourceProduct, final String name) {
        final String fileName = sourceProduct.getName() + name;
        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if(!appUserDir.exists()) {
            appUserDir.mkdirs();
        }
        return new File(appUserDir.toString(), fileName);
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
            super(SARSimTerrainCorrectionOp.class);
            setOperatorUI(SARSimTerrainCorrectionOpUI.class);
        }
    }
}
