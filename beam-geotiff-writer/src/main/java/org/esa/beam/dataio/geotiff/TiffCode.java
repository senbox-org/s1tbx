package org.esa.beam.dataio.geotiff;


/**
 * TIFF codes for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffCode {

    // Compression Codes
    public static final int COMPRESSION_UNCOMPRESSED = 1;
    public static final int COMPRESSION_CCITT_1D = 2;
    public static final int COMPRESSION_GROUP3_FAX = 3;
    public static final int COMPRESSION_GROUP4_FAX = 4;
    public static final int COMPRESSION_LZW = 5;
    public static final int COMPRESSION_PACKBITS = 32773;

    // PhotometricInterpretaion Codes
    public static final TiffShort PHOTOMETRIC_WHITE_IS_ZERO = new TiffShort(0);
    public static final TiffShort PHOTOMETRIC_BLACK_IS_ZERO = new TiffShort(1);
    public static final TiffShort PHOTOMETRIC_RGB = new TiffShort(2);
    public static final TiffShort PHOTOMETRIC_RGB_PALETTE = new TiffShort(3);
    public static final TiffShort PHOTOMETRIC_TRANSPARENCY_MASK = new TiffShort(4);
    public static final TiffShort PHOTOMETRIC_CMYK = new TiffShort(5);
    public static final TiffShort PHOTOMETRIC_YCBCR = new TiffShort(6);
    public static final TiffShort PHOTOMETRIC_CIELAB = new TiffShort(8);

    //Resolution Unit
    /**
     * No absolute unit of measurement. Used for images that may have a
     * non-square aspect ratio but no meaningful absolute dimensions.
     */
    public static final short RESOLUTION_UNIT_NONE = 1;
    /**
     * Unit measurement inch.
     */
    public static final short RESOLUTION_UNIT_INCH = 2;
    /**
     * Unit measurement centimeter.
     */
    public static final short RESOLUTION_UNIT_CENTIMETER = 3;

    //Sample Format
    /**
     * Unsigned integer data.
     */
    public static final TiffShort SAMPLE_FORMAT_UINT = new TiffShort(1);
    /**
     * Two's complement signed integer data.
     */
    public static final TiffShort SAMPLE_FORMAT_INT = new TiffShort(2);
    /**
     * IEEE floating point data [IEEE].
     */
    public static final TiffShort SAMPLE_FORMAT_FLOAT = new TiffShort(3);
    /**
     * Undefined data format.
     */
    public static final TiffShort SAMPLE_FORMAT_UNDEFINED = new TiffShort(4);

    //Planar Configuration
    public static final TiffShort PLANAR_CONFIG_CHUNKY = new TiffShort(1);
    public static final TiffShort PLANAR_CONFIG_PLANAR = new TiffShort(2);

    //Extra Samples
    public static final TiffShort EXTRA_SAMPLES_UNSPEC_DATA = new TiffShort(0);
    public static final TiffShort EXTRA_SAMPLES_ASSOC_ALPHA_DATA = new TiffShort(1);
    public static final TiffShort EXTRA_SAMPLES_UNASSOC_ALPHA_DATA = new TiffShort(2);

    public static TiffShort getSampleFormat(final TiffShort tiffType) {
        switch (tiffType.getValue()) {
        case TiffType.SBYTE_TYPE:
        case TiffType.SSHORT_TYPE:
        case TiffType.SLONG_TYPE:
            return TiffCode.SAMPLE_FORMAT_INT;
        case TiffType.BYTE_TYPE:
        case TiffType.SHORT_TYPE:
        case TiffType.LONG_TYPE:
            return TiffCode.SAMPLE_FORMAT_UINT;
        case TiffType.FLOAT_TYPE:
        case TiffType.DOUBLE_TYPE:
            return TiffCode.SAMPLE_FORMAT_FLOAT;
        case TiffType.UNDEFINED_TYPE:
            return TiffCode.SAMPLE_FORMAT_UNDEFINED;
        default:
            throw new IllegalArgumentException("unknown tiffType");
        }
    }

}