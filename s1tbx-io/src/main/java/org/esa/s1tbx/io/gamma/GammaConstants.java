package org.esa.s1tbx.io.gamma;

class GammaConstants {

    static final String HEADER_KEY_NAME = "title";
    static final String HEADER_KEY_SAMPLES = "range_samples";
    static final String HEADER_KEY_SAMPLES1 = "range_samp_1";
    static final String HEADER_KEY_WIDTH = "width";
    static final String HEADER_KEY_NCOLUMNS = "ncolumns";
    static final String HEADER_KEY_LINES = "azimuth_lines";
    static final String HEADER_KEY_LINES1 = "az_samp_1";
    static final String HEADER_KEY_HEIGHT = "height";
    static final String HEADER_KEY_NLINES = "nlines";
    static final String HEADER_KEY_BANDS = "bands";
    static final String HEADER_KEY_HEADER_OFFSET = "line_header_size";
    static final String HEADER_KEY_DATA_TYPE = "image_format";
    static final String HEADER_KEY_SENSOR_TYPE = "sensor";
    static final String HEADER_KEY_BYTE_ORDER = "byte order";
    static final String HEADER_KEY_BAND_NAMES = "band names";
    static final String HEADER_KEY_DESCRIPTION = "sensor";

    static final String HEADER_KEY_DATE = "date";
    static final String HEADER_KEY_START_TIME = "start_time";
    static final String HEADER_KEY_CENTER_TIME = "center_time";
    static final String HEADER_KEY_END_TIME = "end_time";
    static final String HEADER_KEY_LINE_TIME_INTERVAL = "azimuth_line_time";

    static final String HEADER_KEY_RADAR_FREQUENCY = "radar_frequency";
    static final String HEADER_KEY_PRF = "prf";
    static final String HEADER_KEY_RANGE_LOOKS = "range_looks";
    static final String HEADER_KEY_AZIMUTH_LOOKS = "azimuth_looks";
    static final String HEADER_KEY_IMAGE_GEOMETRY = "image_geometry";
    static final String HEADER_KEY_CENTER_LATITUDE = "center_latitude";
    static final String HEADER_KEY_CENTER_LONGITUDE = "center_longitude";
    static final String HEADER_KEY_RANGE_PIXEL_SPACING = "range_pixel_spacing";
    static final String HEADER_KEY_AZIMUTH_PIXEL_SPACING = "azimuth_pixel_spacing";

    static final String HEADER_KEY_NUM_STATE_VECTORS = "number_of_state_vectors";
    static final String HEADER_KEY_TIME_FIRST_STATE_VECTORS = "time_of_first_state_vector";
    static final String HEADER_KEY_STATE_VECTOR_INTERVAL = "state_vector_interval";
    static final String HEADER_KEY_STATE_VECTOR_POSITION = "state_vector_position";
    static final String HEADER_KEY_STATE_VECTOR_VELOCITY = "state_vector_velocity";

    final static String SLC_EXTENSION = ".rslc";
    final static String PAR_EXTENSION = ".par";

    private GammaConstants() {
    }
}
