package com.iceye.esa.snap.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.iceye.esa.snap.dataio.util.IceyeXConstants;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.netcdf.NetCDFReader;
import org.esa.s1tbx.io.netcdf.NetCDFUtils;
import org.esa.s1tbx.io.netcdf.NetcdfConstants;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ahmad Hamouda
 */
public class IceyeProductReader extends SARReader {

    private final Map<Band, Variable> bandMap = new HashMap<>(10);
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private NetcdfFile netcdfFile = null;
    private Product product = null;
    private boolean isComplex = false;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public IceyeProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    private static String getPolarization(final Product product, NetcdfFile netcdfFile) {

        final MetadataElement globalElem = AbstractMetadata.getOriginalProductMetadata(product).getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        try {
            if (globalElem != null) {
                final String polStr = netcdfFile.getRootGroup().findVariable(IceyeXConstants.MDS1_TX_RX_POLAR).readScalarString();
                if (!polStr.isEmpty())
                    return polStr;
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
        return null;
    }

    private static void createUniqueBandName(final Product product, final Band band, final String origName) {
        int cnt = 1;
        band.setName(origName);
        while (product.getBand(band.getName()) != null) {
            band.setName(origName + cnt);
            ++cnt;
        }
    }

    private static void addIncidenceAnglesSlantRangeTime(final Product product, final MetadataElement bandElem, NetcdfFile netcdfFile) {
        try {
            if (bandElem == null) return;

            final int gridWidth = 11;
            final int gridHeight = 11;
            final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
            final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

            final double[] incidenceAngles = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.INCIDENCE_ANGLES).read().getStorage();

            final double nearRangeAngle = incidenceAngles[0];
            final double farRangeAngle = incidenceAngles[incidenceAngles.length - 1];

            final double firstRangeTime = netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_PIXEL_TIME).readScalarDouble()*Constants.sTOns;
            final double samplesPerLine = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_SAMPLES_PER_LINE).readScalarDouble();
            final double rangeSamplingRate = netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SAMPLING_RATE).readScalarDouble();
            final double lastRangeTime = firstRangeTime + samplesPerLine / rangeSamplingRate * Constants.sTOns;

            final float[] incidenceCorners = new float[]{(float) nearRangeAngle, (float) farRangeAngle, (float) nearRangeAngle, (float) farRangeAngle};
            final float[] slantRange = new float[]{(float) firstRangeTime, (float) lastRangeTime, (float) firstRangeTime, (float) lastRangeTime};

            final float[] fineAngles = new float[gridWidth * gridHeight];
            final float[] fineTimes = new float[gridWidth * gridHeight];

            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRange, fineTimes);

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);

            final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineTimes);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(slantRangeGrid);
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private static void addGeocodingFromMetadata(final Product product, final MetadataElement bandElem, NetcdfFile netcdfFile) {
        if (bandElem == null) return;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        try {
            double[] firstNear = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_NEAR).read().getStorage();
            double[] firstFar = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_FAR).read().getStorage();
            double[] lastNear = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.LAST_NEAR).read().getStorage();
            double[] lastFar = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.LAST_FAR).read().getStorage();


            final double latUL = firstNear[2];
            final double lonUL = firstNear[3];
            final double latUR = firstFar[2];
            final double lonUR = firstFar[3];
            final double latLL = lastNear[2];
            final double lonLL = lastNear[3];
            final double latLR = lastFar[2];
            final double lonLR = lastFar[3];

            absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SPACING).readScalarDouble());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    netcdfFile.getRootGroup().findVariable(IceyeXConstants.AZIMUTH_SPACING).readScalarDouble());

            final double[] latCorners = new double[]{latUL, latUR, latLL, latLR};
            final double[] lonCorners = new double[]{lonUL, lonUR, lonLL, lonLR};

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    private void initReader() {
        product = null;
        netcdfFile = null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     */
    @Override
    protected Product readProductNodesImpl() {
        try {

            final File inputFile = ReaderUtils.getFileFromInput(getInput());

            initReader();

            final NetcdfFile tempNetcdfFile = NetcdfFile.open(inputFile.getPath());
            if (tempNetcdfFile == null) {
                close();
                throw new IllegalFileFormatException(inputFile.getName() +
                        " Could not be interpreted by the reader.");
            }
            if (tempNetcdfFile.getRootGroup().getVariables().isEmpty()) {
                close();
                throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                        "be interpreted as remote sensing bands.");  /*I18N*/
            }
            this.netcdfFile = tempNetcdfFile;

            final String productType = this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.PRODUCT_TYPE).readScalarString();
            final int rasterWidth = this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_SAMPLES_PER_LINE).readScalarInt();
            final int rasterHeight = this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_OUTPUT_LINES).readScalarInt();

            product = new Product(inputFile.getName(),
                    productType,
                    rasterWidth, rasterHeight,
                    this);
            product.setFileLocation(inputFile);
            StringBuilder description = new StringBuilder();
            description.append(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.PRODUCT).readScalarString()).append(" - ");
            description.append(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.PRODUCT_TYPE).readScalarString()).append(" - ");
            description.append(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.SPH_DESCRIPTOR).readScalarString()).append(" - ");
            description.append(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.MISSION).readScalarString());
            product.setDescription(description.toString());
            product.setStartTime(ProductData.UTC.parse(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.ACQUISITION_START_UTC).readScalarString(), standardDateFormat));
            product.setEndTime(ProductData.UTC.parse(this.netcdfFile.getRootGroup().findVariable(IceyeXConstants.ACQUISITION_END_UTC).readScalarString(), standardDateFormat));
            addMetadataToProduct();
            addBandsToProduct();
            addTiePointGridsToProduct();
            addGeoCodingToProduct();
            addCommonSARMetadata(product);
            addDopplerCentroidCoefficients();
            setQuicklookBandName(product);

            product.getGcpGroup();
            product.setModified(false);

            return product;
        } catch (Exception e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            product = null;
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

    private void addMetadataToProduct() {

        final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
        NetCDFUtils.addAttributes(origMetadataRoot, NetcdfConstants.GLOBAL_ATTRIBUTES_NAME,
                netcdfFile.getGlobalAttributes());

        for (Variable variable : netcdfFile.getVariables()) {
            NetCDFUtils.addVariableMetadata(origMetadataRoot, variable, 5000);
        }

        addAbstractedMetadataHeader(product.getMetadataRoot());
    }

    private void addAbstractedMetadataHeader(MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        try {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, netcdfFile.getRootGroup().findVariable(IceyeXConstants.PRODUCT).readScalarString());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, netcdfFile.getRootGroup().findVariable(IceyeXConstants.PRODUCT_TYPE).readScalarString());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, netcdfFile.getRootGroup().findVariable(IceyeXConstants.SPH_DESCRIPTOR).readScalarString());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, netcdfFile.getRootGroup().findVariable(IceyeXConstants.MISSION).readScalarString());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, netcdfFile.getRootGroup().findVariable(IceyeXConstants.ACQUISITION_MODE).readScalarString());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, netcdfFile.getRootGroup().findVariable(IceyeXConstants.ANTENNA_POINTING).readScalarString());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS, IceyeXConstants.BEAMS_DEFAULT_VALUE);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, ProductData.UTC.parse(netcdfFile.getRootGroup().findVariable(IceyeXConstants.PROC_TIME_UTC).readScalarString(), standardDateFormat));


            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, IceyeXConstants.ICEYE_PROCESSOR_NAME_PREFIX + netcdfFile.getRootGroup().findVariable(IceyeXConstants.PROCESSING_SYSTEM_IDENTIFIER).readScalarFloat());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, netcdfFile.getRootGroup().findVariable(IceyeXConstants.CYCLE).readScalarInt());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, netcdfFile.getRootGroup().findVariable(IceyeXConstants.REL_ORBIT).readScalarInt());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, netcdfFile.getRootGroup().findVariable(IceyeXConstants.ABS_ORBIT).readScalarInt());

            double[] localIncidenceAngles = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.INCIDENCE_ANGLES).read().getStorage();
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, localIncidenceAngles[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, localIncidenceAngles[localIncidenceAngles.length - 1]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slice_num, IceyeXConstants.SLICE_NUM_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.data_take_id, IceyeXConstants.DATA_TAKE_ID_DEFAULT_VALUE);
            String geoRefSystem = IceyeXConstants.GEO_REFERENCE_SYSTEM_DEFAULT_VALUE;
            if (netcdfFile.getRootGroup().findVariable(IceyeXConstants.GEO_REFERENCE_SYSTEM) != null) {
                geoRefSystem = netcdfFile.getRootGroup().findVariable(IceyeXConstants.GEO_REFERENCE_SYSTEM).readScalarString();
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system, geoRefSystem);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, ProductData.UTC.parse(netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_LINE_TIME).readScalarString(), standardDateFormat));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, ProductData.UTC.parse(netcdfFile.getRootGroup().findVariable(IceyeXConstants.LAST_LINE_TIME).readScalarString(), standardDateFormat));
            double[] firstNear = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_NEAR).read().getStorage();
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, firstNear[2]);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, firstNear[3]);
            double[] firstFar = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_FAR).read().getStorage();
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, firstFar[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, firstFar[3]);
            double[] lastNear = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.LAST_NEAR).read().getStorage();
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lastNear[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lastNear[3]);
            double[] lastFar = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.LAST_FAR).read().getStorage();
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lastFar[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lastFar[3]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, netcdfFile.getRootGroup().findVariable(IceyeXConstants.PASS).readScalarString());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, netcdfFile.getRootGroup().findVariable(IceyeXConstants.MDS1_TX_RX_POLAR).readScalarString());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, netcdfFile.getRootGroup().findVariable(IceyeXConstants.AZIMUTH_LOOKS).readScalarFloat());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_LOOKS).readScalarFloat());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SPACING).readScalarFloat());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, netcdfFile.getRootGroup().findVariable(IceyeXConstants.AZIMUTH_SPACING).readScalarFloat());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, netcdfFile.getRootGroup().findVariable(IceyeXConstants.PULSE_REPETITION_FREQUENCY).readScalarFloat());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, netcdfFile.getRootGroup().findVariable(IceyeXConstants.RADAR_FREQUENCY).readScalarDouble() / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, netcdfFile.getRootGroup().findVariable(IceyeXConstants.LINE_TIME_INTERVAL).readScalarDouble());
            final int rasterWidth = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_SAMPLES_PER_LINE).readScalarInt();
            final int rasterHeight = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_OUTPUT_LINES).readScalarInt();
            double totalSize = (rasterHeight * rasterWidth * 2 * 2) / (1024.0f * 1024.0f);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, totalSize);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_OUTPUT_LINES).readScalarInt());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_SAMPLES_PER_LINE).readScalarInt());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_x, IceyeXConstants.SUBSET_OFFSET_X_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.subset_offset_y, IceyeXConstants.SUBSET_OFFSET_Y_DEFAULT_VALUE);
            if (isComplex) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 0);
            } else {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height, netcdfFile.getRootGroup().findVariable(IceyeXConstants.AVG_SCENE_HEIGHT).readScalarDouble());

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lat_pixel_res, IceyeXConstants.LAT_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.lon_pixel_res, IceyeXConstants.LON_PIXEL_RES_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel, (netcdfFile.getRootGroup().findVariable(IceyeXConstants.FIRST_PIXEL_TIME).readScalarDouble() / 2) * 299792458.0);

            int antElevCorrFlag = IceyeXConstants.ANT_ELEV_CORR_FLAG_DEFAULT_VALUE;
            if (netcdfFile.getRootGroup().findVariable(IceyeXConstants.ANT_ELEV_CORR_FLAG) != null) {
                antElevCorrFlag = netcdfFile.getRootGroup().findVariable(IceyeXConstants.ANT_ELEV_CORR_FLAG).readScalarInt();
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevCorrFlag);

            int rangeSpreadCompFlag = IceyeXConstants.RANGE_SPREAD_COMP_FLAG_DEFAULT_VALUE;
            if (netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SPREAD_COMP_FLAG) != null) {
                rangeSpreadCompFlag = netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SPREAD_COMP_FLAG).readScalarInt();
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rangeSpreadCompFlag);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, IceyeXConstants.REPLICA_POWER_CORR_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, IceyeXConstants.ABS_CALIBRATION_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor, netcdfFile.getRootGroup().findVariable(IceyeXConstants.CALIBRATION_FACTOR).readScalarDouble());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, IceyeXConstants.INC_ANGLE_COMP_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle, IceyeXConstants.REF_INC_ANGLE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range, IceyeXConstants.REF_SLANT_RANGE_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp, IceyeXConstants.REF_SLANT_RANGE_EXP_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor, IceyeXConstants.RESCALING_FACTOR_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_SAMPLING_RATE).readScalarDouble() / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, netcdfFile.getRootGroup().findVariable(IceyeXConstants.RANGE_BANDWIDTH).readScalarDouble() / 1e6);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, netcdfFile.getRootGroup().findVariable(IceyeXConstants.AZIMUTH_BANDWIDTH).readScalarDouble());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.multilook_flag, IceyeXConstants.MULTI_LOOK_FLAG_DEFAULT_VALUE);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, IceyeXConstants.CO_REGISTERED_STACK_DEFAULT_VALUE);


            addOrbitStateVectors(absRoot);
        } catch (ParseException | IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot) {

        try {
            final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);


            final int numPoints = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUMBER_OF_STATE_VECTORS).readScalarInt();
            char[] stateVectorTime = (char[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.STATE_VECTOR_TIME).read().getStorage();
            int utcDimension = netcdfFile.getRootGroup().findVariable(IceyeXConstants.STATE_VECTOR_TIME).getDimension(2).getLength();
            final double[] satellitePositionX = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_X_POS).read().getStorage();
            final double[] satellitePositionY = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_Y_POS).read().getStorage();
            final double[] satellitePositionZ = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_Z_POS).read().getStorage();
            final double[] satelliteVelocityX = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_X_VEL).read().getStorage();
            final double[] satelliteVelocityY = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_Y_VEL).read().getStorage();
            final double[] satelliteVelocityZ = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.ORBIT_VECTOR_N_Z_VEL).read().getStorage();
            int start = 0;
            String utc = new String(Arrays.copyOfRange(stateVectorTime, 0, utcDimension - 1));
            ProductData.UTC stateVectorUTC = ProductData.UTC.parse(utc, standardDateFormat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, stateVectorUTC);
            for (int i = 0; i < numPoints; i++) {
                utc = new String(Arrays.copyOfRange(stateVectorTime, start, start + utcDimension - 1));
                ProductData.UTC vectorUTC = ProductData.UTC.parse(utc, standardDateFormat);

                final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i + 1));
                orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, vectorUTC);

                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, satellitePositionX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, satellitePositionY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, satellitePositionZ[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, satelliteVelocityX[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, satelliteVelocityY[i]);
                orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, satelliteVelocityZ[i]);

                orbitVectorListElem.addElement(orbitVectorElem);
                start += utcDimension;
            }
        } catch (IOException | ParseException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private void addDopplerCentroidCoefficients() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, 0.0);
        try {

            int dimensionColumn = netcdfFile.getRootGroup().findVariable(IceyeXConstants.DC_ESTIMATE_COEFFS).getDimension(1).getLength();
            double[] coefValueS = (double[]) netcdfFile.getRootGroup().findVariable(IceyeXConstants.DC_ESTIMATE_COEFFS).read().getStorage();

            for (int i = 0; i < dimensionColumn; i++) {
                final double coefValue = coefValueS[i];
                final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
                dopplerListElem.addElement(coefElem);
                AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                        ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private String getSampleType() {
        try {
            if ("slc".equalsIgnoreCase(netcdfFile.getRootGroup().findVariable(IceyeXConstants.SPH_DESCRIPTOR).readScalarString())) {
                isComplex = true;
                return "COMPLEX";
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
        isComplex = false;
        return "DETECTED";
    }

    private void addBandsToProduct() {
        int cnt = 1;
        Map<String, Variable> variables = new HashMap<>();
        Variable siBand = netcdfFile.getRootGroup().findVariable(IceyeXConstants.S_I);
        Variable sqBand = netcdfFile.getRootGroup().findVariable(IceyeXConstants.S_Q);
        Variable amplitudeBand = netcdfFile.getRootGroup().findVariable(IceyeXConstants.S_AMPLITUDE);
        if (siBand != null) {
            variables.put(IceyeXConstants.S_I, siBand);
        }
        if (sqBand != null) {
            variables.put(IceyeXConstants.S_Q, sqBand);
        }
        if (amplitudeBand != null) {
            variables.put(IceyeXConstants.S_AMPLITUDE, amplitudeBand);
        }
        try {

            final int width = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_SAMPLES_PER_LINE).readScalarInt();
            final int height = netcdfFile.getRootGroup().findVariable(IceyeXConstants.NUM_OUTPUT_LINES).readScalarInt();

            String cntStr = "";
            if (variables.size() > 1) {
                final String polStr = getPolarization(product, netcdfFile);
                if (polStr != null) {
                    cntStr = "_" + polStr;
                } else {
                    cntStr = "_" + cnt;
                }
            }

            if (isComplex) {
                final Band bandI = NetCDFUtils.createBand(variables.get(IceyeXConstants.S_I), width, height);
                createUniqueBandName(product, bandI, "i" + cntStr);
                bandI.setUnit(Unit.REAL);
                product.addBand(bandI);
                bandMap.put(bandI, variables.get(IceyeXConstants.S_I));

                final Band bandQ = NetCDFUtils.createBand(variables.get(IceyeXConstants.S_Q), width, height);
                createUniqueBandName(product, bandQ, "q" + cntStr);
                bandQ.setUnit(Unit.IMAGINARY);
                product.addBand(bandQ);
                bandMap.put(bandQ, variables.get(IceyeXConstants.S_Q));

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
            } else {
                final Band band = NetCDFUtils.createBand(variables.get(IceyeXConstants.S_AMPLITUDE), width, height);
                createUniqueBandName(product, band, "Amplitude" + cntStr);
                band.setUnit(Unit.AMPLITUDE);
                product.addBand(band);
                bandMap.put(band, variables.get(IceyeXConstants.S_AMPLITUDE));

                createVirtualIntensityBand(product, band, cntStr);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

        }
    }

    private void addTiePointGridsToProduct() {

        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        addIncidenceAnglesSlantRangeTime(product, bandElem, netcdfFile);
        addGeocodingFromMetadata(product, bandElem, netcdfFile);
    }

    private MetadataElement getBandElement(final Band band) {
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final Variable variable = bandMap.get(band);
        final String varName = variable.getShortName();
        MetadataElement bandElem = null;
        for (MetadataElement elem : root.getElements()) {
            if (elem.getName().equalsIgnoreCase(varName)) {
                bandElem = elem;
                break;
            }
        }
        return bandElem;
    }

    private void addGeoCodingToProduct() throws IOException {
        if (product.getSceneGeoCoding() == null) {
            NetCDFReader.setTiePointGeoCoding(product);
        }
        if (product.getSceneGeoCoding() == null) {
            NetCDFReader.setPixelGeoCoding(product);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final int sceneHeight = product.getSceneRasterHeight();
        final int sceneWidth = product.getSceneRasterWidth();

        final Variable variable = bandMap.get(destBand);

        sourceHeight = Math.min(sourceHeight, sceneHeight-sourceOffsetY);
        destHeight = Math.min(destHeight, sceneHeight-sourceOffsetY);
        sourceWidth = Math.min(sourceWidth, sceneWidth-sourceOffsetX);
        destWidth = Math.min(destWidth, sceneWidth-destOffsetX);
        final int[] origin = {sourceOffsetY, sourceOffsetX};
        final int[] shape = {sourceHeight, sourceWidth};

        pm.beginTask("Reading util from band " + destBand.getName(), destHeight);
        try {
            Array srcArray = null;
            synchronized (netcdfFile) {
                srcArray = variable.read(origin, shape);
            }

            for (int y = 0; y < destHeight; y++) {
                System.arraycopy(srcArray.getStorage(), 0, destBuffer.getElems(), y * destWidth, destWidth);
            }
        } catch (Exception e) {
            final IOException ioException = new IOException(e);
            ioException.initCause(e);
            throw ioException;
        } finally {
            pm.done();
        }
    }

}
