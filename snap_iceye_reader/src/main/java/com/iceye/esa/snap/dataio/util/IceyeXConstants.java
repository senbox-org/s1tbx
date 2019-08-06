package com.iceye.esa.snap.dataio.util;

/**
 * @author Ahmad Hamouda
 */
public class IceyeXConstants {
    public static final String ICEYE_PLUGIN_DESCRIPTION = "ICEYE Products";
    public static final String ICEYE_FILE_PREFIX = "ICEYE";
    //    IceyeProductProductReader
    public static final String PRODUCT = "product_name";
    public static final String PRODUCT_TYPE = "product_type";
    public static final String SPH_DESCRIPTOR = "product_level";
    public static final String MISSION = "satellite_name";
    public static final String ACQUISITION_MODE = "acquisition_mode";
    public static final String ANTENNA_POINTING = "look_side";
    public static final String BEAMS_DEFAULT_VALUE = "";
    public static final String PROCESSING_SYSTEM_IDENTIFIER = "processor_version";
    public static final String CYCLE = "orbit_repeat_cycle";
    public static final String REL_ORBIT = "orbit_relative_number";
    public static final String ABS_ORBIT = "orbit_absolute_number";
    public static final String STATE_VECTOR_TIME = "state_vector_time_utc";
    public static final String INCIDENCE_ANGLES = "local_incidence_angle";
    public static final int SLICE_NUM_DEFAULT_VALUE = 99999;
    public static final int DATA_TAKE_ID_DEFAULT_VALUE = 99999;
    public static final String GEO_REFERENCE_SYSTEM_DEFAULT_VALUE = "WGS84";
    public static final String GEO_REFERENCE_SYSTEM = "geo_ref_system";
    public static final String FIRST_LINE_TIME = "zerodoppler_start_utc";
    public static final String LAST_LINE_TIME = "zerodoppler_end_utc";
    public static final String FIRST_NEAR = "coord_first_near";
    public static final String FIRST_FAR = "coord_first_far";
    public static final String LAST_NEAR = "coord_last_near";
    public static final String LAST_FAR = "coord_last_far";
    public static final String PASS = "orbit_direction";
    public static final String MDS1_TX_RX_POLAR = "polarization";
    public static final String AZIMUTH_LOOKS = "azimuth_looks";
    public static final String RANGE_LOOKS = "range_looks";
    public static final String SLANT_RANGE_SPACING = "slant_range_spacing";
    public static final String AZIMUTH_GROUND_SPACING = "azimuth_ground_spacing";
    public static final String PULSE_REPETITION_FREQUENCY = "processing_prf";
    public static final String RADAR_FREQUENCY = "carrier_frequency";
    public static final String LINE_TIME_INTERVAL = "azimuth_time_interval";
    public static final String NUM_OUTPUT_LINES = "number_of_azimuth_samples";
    public static final String NUM_SAMPLES_PER_LINE = "number_of_range_samples";
    public static final int SUBSET_OFFSET_X_DEFAULT_VALUE = 0;
    public static final int SUBSET_OFFSET_Y_DEFAULT_VALUE = 0;
    public static final String AVG_SCENE_HEIGHT = "avg_scene_height";
    public static final double LAT_PIXEL_RES_DEFAULT_VALUE = 99999.0;
    public static final double LON_PIXEL_RES_DEFAULT_VALUE = 99999.0;
    public static final String FIRST_PIXEL_TIME = "first_pixel_time";
    public static final int ANT_ELEV_CORR_FLAG_DEFAULT_VALUE = 1;
    public static final String ANT_ELEV_CORR_FLAG = "ant_elev_corr_flag";
    public static final int RANGE_SPREAD_COMP_FLAG_DEFAULT_VALUE = 1;
    public static final String RANGE_SPREAD_COMP_FLAG = "range_spread_comp_flag";
    public static final int REPLICA_POWER_CORR_FLAG_DEFAULT_VALUE = 0;
    public static final int ABS_CALIBRATION_FLAG_DEFAULT_VALUE = 0;
    public static final String CALIBRATION_FACTOR = "calibration_factor";
    public static final int INC_ANGLE_COMP_FLAG_DEFAULT_VALUE = 0;
    public static final double REF_INC_ANGLE_DEFAULT_VALUE = 99999.0;
    public static final double REF_SLANT_RANGE_DEFAULT_VALUE = 99999.0;
    public static final double REF_SLANT_RANGE_EXP_DEFAULT_VALUE = 99999.0;
    public static final double RESCALING_FACTOR_DEFAULT_VALUE = 99999.0;
    public static final String RANGE_SAMPLING_RATE = "range_sampling_rate";
    public static final String RANGE_BANDWIDTH = "chirp_bandwidth";
    public static final String AZIMUTH_BANDWIDTH = "total_processed_bandwidth_azimuth";
    public static final int MULTI_LOOK_FLAG_DEFAULT_VALUE = 0;
    public static final int CO_REGISTERED_STACK_DEFAULT_VALUE = 0;
    public static final String ORBIT_VECTOR_N_X_POS = "posX";
    public static final String ORBIT_VECTOR_N_Y_POS = "posY";
    public static final String ORBIT_VECTOR_N_Z_POS = "posZ";
    public static final String ORBIT_VECTOR_N_X_VEL = "velX";
    public static final String ORBIT_VECTOR_N_Y_VEL = "velY";
    public static final String ORBIT_VECTOR_N_Z_VEL = "velZ";
    public static final String ACQUISITION_START_UTC = "acquisition_start_utc";
    public static final String ACQUISITION_END_UTC = "acquisition_end_utc";
    public static final String NUMBER_OF_STATE_VECTORS = "number_of_state_vectors";
    public static final String DC_ESTIMATE_COEFFS = "dc_estimate_coeffs";
    public static final String S_I = "s_i";
    public static final String S_Q = "s_q";
    public static final String S_AMPLITUDE = "s_amplitude";
    public static final String ICEYE_PROCESSOR_NAME_PREFIX = "ICEYE_P_";
    public static final String PROC_TIME_UTC = "processing_time";
    public static final String SLANT_RANGE_TO_FIRST_PIXEL = "slant_range_to_first_pixel";
    public static final String RIGHT = "right";
    public static final String ASCENDING = "ascending";
    public static final String DESCENDING = "descending";
    public static final String GRSR_GROUND_RANGE_ORIGIN = "grsr_ground_range_origin";
    public static final String GRSR_COEFFICIENTS = "grsr_coefficients";
    public static final String RANGE_SPACING = "range_spacing";
    public static final String COORD_CENTER = "coord_center";
    public static final String INCIDENCE_NEAR = "incidence_near";
    public static final String DC_ESTIMATE_POLY_ORDER = "dc_estimate_poly_order";
    public static final String DC_REFERENCE_PIXEL_TIME = "dc_reference_pixel_time";
    public static final String DC_ESTIMATE_TIME_UTC = "dc_estimate_time_utc";
    public static final String GRSR_ZERO_DOPPLER_TIME = "grsr_zero_doppler_time";
    public static final String INCIDENCE_FAR = "incidence_far";
    public static final String AZIMUTH_SPACING = "azimuth_spacing";
    public static final String GDALMETADATA = "<GDALMetadata";
    public static final String GRD = "grd";
    public static final String SLC = "slc";
    public static final String COMPLEX = "COMPLEX";
    public static final String DETECTED = "DETECTED";
    //    IceyeProductReaderPlugIn
    private static final String ICEYE_FORMAT_NAMES = "IceyeProduct";
    private static final String ICEYE_FORMAT_FILE_EXTENSIONS = "h5";

    private IceyeXConstants() {
        //not allowed to instantiated
    }

    public static String[] getIceyeFormatNames() {
        return ICEYE_FORMAT_NAMES.split(",");
    }

    public static String[] getIceyeFormatFileExtensions() {
        return ICEYE_FORMAT_FILE_EXTENSIONS.split(",");
    }
}
