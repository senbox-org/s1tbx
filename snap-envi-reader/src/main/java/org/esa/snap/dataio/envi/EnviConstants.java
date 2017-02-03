package org.esa.snap.dataio.envi;

public class EnviConstants {

    public static final String HEADER_KEY_SAMPLES = "samples";
    public static final String HEADER_KEY_LINES = "lines";
    public static final String HEADER_KEY_BANDS = "bands";
    public static final String HEADER_KEY_HEADER_OFFSET = "header offset";
    public static final String HEADER_KEY_FILE_TYPE = "file type";
    public static final String HEADER_KEY_DATA_TYPE = "data type";
    public static final String HEADER_KEY_INTERLEAVE = "interleave";
    public static final String HEADER_KEY_SENSOR_TYPE = "sensor type";
    public static final String HEADER_KEY_BYTE_ORDER = "byte order";
    public static final String HEADER_KEY_MAP_INFO = "map info";
    public static final String HEADER_KEY_PROJECTION_INFO = "projection info";
    public static final String HEADER_KEY_WAVELENGTH_UNITS = "wavelength units";
    public static final String HEADER_KEY_WAVELENGTH = "wavelength";
    public static final String HEADER_KEY_FWHM = "fwhm";
    public static final String HEADER_KEY_BAND_NAMES = "band names";
    public static final String HEADER_KEY_DATA_OFFSET_VALUES = "data offset values";
    public static final String HEADER_KEY_DATA_GAIN_VALUES = "data gain values";
    public static final String HEADER_KEY_DATA_IGNORE_VALUE = "data ignore value";
    public static final String HEADER_KEY_DESCRIPTION = "description";
    public static final String HEADER_KEY_CLASSES = "classes";
    public static final String HEADER_KEY_CLASS_LOOKUP = "class lookup";
    public static final String HEADER_KEY_CLASS_NAMES = "class names";
    public static final String FIRST_LINE = "ENVI";
    public static final String FORMAT_NAME = "ENVI";
    public static final String DESCRIPTION = "ENVI Data Products";
    public static final String PROJECTION_NAME_WGS84 = "Geographic Lat/Lon";
    public static final String DATUM_NAME_WGS84 = "WGS-84";

    public static final String HDR_EXTENSION = ".hdr";
    public static final String ZIP_EXTENSION = ".zip";
    static final String[] VALID_EXTENSIONS = {HDR_EXTENSION, ZIP_EXTENSION};

    // image_extensions in prioritised order
    // The default from ENVI is right now ".dat", before it was "", we have used ".img"
    // order only important if there is more than one data file
    static final String[] IMAGE_EXTENSIONS = {".dat", "", ".img", ".bin", ".bip", ".bil", ".bsq"};

    public static final int TYPE_ID_BYTE = 1;
    public static final int TYPE_ID_INT16 = 2;
    public static final int TYPE_ID_INT32 = 3;
    public static final int TYPE_ID_FLOAT32 = 4;
    public static final int TYPE_ID_FLOAT64 = 5;
    public static final int TYPE_ID_COMPLEXFLOAT32 = 6;
    public static final int TYPE_ID_COMPLEXFLOAT64 = 9;
    public static final int TYPE_ID_UINT16 = 12;
    public static final int TYPE_ID_UINT32 = 13;

    private EnviConstants() {
    }
}
