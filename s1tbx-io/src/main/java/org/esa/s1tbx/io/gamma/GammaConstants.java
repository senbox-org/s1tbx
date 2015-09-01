package org.esa.s1tbx.io.gamma;

public class GammaConstants {

    public static final String HEADER_KEY_NAME = "title";
    public static final String HEADER_KEY_SAMPLES = "range_samples";
    public static final String HEADER_KEY_SAMPLES1 = "range_samp_1";
    public static final String HEADER_KEY_LINES = "azimuth_lines";
    public static final String HEADER_KEY_LINES1 = "az_samp_1";
    public static final String HEADER_KEY_BANDS = "bands";
    public static final String HEADER_KEY_HEADER_OFFSET = "line_header_size";
    public static final String HEADER_KEY_DATA_TYPE = "image_format";
    public static final String HEADER_KEY_SENSOR_TYPE = "sensor";
    public static final String HEADER_KEY_BYTE_ORDER = "byte order";
    public static final String HEADER_KEY_BAND_NAMES = "band names";
    public static final String HEADER_KEY_DESCRIPTION = "sensor";

    public static final String HEADER_KEY_DATE = "date";
    public static final String HEADER_KEY_START_TIME = "start_time";
    public static final String HEADER_KEY_END_TIME = "end_time";
    public static final String HEADER_KEY_LINE_TIME_INTERVAL = "azimuth_line_time";

    public static final String HEADER_KEY_RADAR_FREQUENCY = "radar_frequency";
    public static final String HEADER_KEY_PRF = "prf";
    public static final String HEADER_KEY_RANGE_LOOKS = "range_looks";
    public static final String HEADER_KEY_AZIMUTH_LOOKS = "azimuth_looks";

    public final static String SLC_EXTENSION = ".rslc";
    public final static String PAR_EXTENSION = ".par";

    private GammaConstants() {
    }
}
