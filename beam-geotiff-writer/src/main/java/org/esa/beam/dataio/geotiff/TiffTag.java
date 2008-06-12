package org.esa.beam.dataio.geotiff;


/**
 * TIFF tags for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffTag {

    public final static short NewSubfileType = 254;
    public final static short SubfileType = 255;
    public final static TiffShort IMAGE_WIDTH = new TiffShort(256);
    public final static TiffShort IMAGE_LENGTH = new TiffShort(257);
    public final static TiffShort BITS_PER_SAMPLE = new TiffShort(258);
    public final static TiffShort COMPRESSION = new TiffShort(259);
    public final static TiffShort PHOTOMETRIC_INTERPRETATION = new TiffShort(262);
    public final static short Threshholding = 263;
    public final static short CellWidth = 264;
    public final static short CellLength = 265;
    public final static short FillOrder = 266;
    public final static short DocumentName = 269;
    public final static short ImageDescription = 270;
    public final static short Make = 271;
    public final static short Model = 272;
    public final static TiffShort STRIP_OFFSETS = new TiffShort(273);
    public final static short Orientation = 274;
    public final static TiffShort SAMPLES_PER_PIXEL = new TiffShort(277);
    public final static TiffShort ROWS_PER_STRIP = new TiffShort(278);
    public final static TiffShort STRIP_BYTE_COUNTS = new TiffShort(279);
    public final static short MinSampleValue = 280;
    public final static short MaxSampleValue = 281;
    public final static TiffShort X_RESOLUTION = new TiffShort(282);
    public final static TiffShort Y_RESOLUTION = new TiffShort(283);
    public final static TiffShort PLANAR_CONFIGURATION = new TiffShort(284);
    public final static short PageName = 285;
    public final static short XPosition = 286;
    public final static short YPosition = 287;
    public final static short FreeOffsets = 288;
    public final static short FreeByteCounts = 289;
    public final static short GrayResponseUnit = 290;
    public final static short GrayResponseCurve = 291;
    public final static short T4Options = 292;
    public final static short T6Options = 293;
    public final static TiffShort RESOLUTION_UNIT = new TiffShort(296);
    public final static short PageNumber = 297;
    public final static short TransferFunction = 301;
    public final static short Software = 305;
    public final static short DateTime = 306;
    public final static short Artist = 315;
    public final static short HostComputer = 316;
    public final static short Predictor = 317;
    public final static short WhitePoint = 318;
    public final static short PrimaryChromaticities = 319;
    public final static short ColorMap = 320;
    public final static short HalftoneHints = 321;
    public final static short TileWidth = 322;
    public final static short TileLength = 323;
    public final static short TileOffsets = 324;
    public final static short TileByteCounts = 325;
    public final static short InkSet = 332;
    public final static short InkNames = 333;
    public final static short NumberOfInks = 334;
    public final static short DotRange = 336;
    public final static short TargetPrinter = 337;
    public final static TiffShort EXTRA_SAMPLES = new TiffShort(338);
    public final static TiffShort SAMPLE_FORMAT = new TiffShort(339);
    public final static short SMinSampleValue = 340;
    public final static short SMaxSampleValue = 341;
    public final static short TransferRange = 342;
    public final static short JPEGProc = 512;
    public final static short JPEGInterchangeFormat = 513;
    public final static short JPEGInterchangeFormatLngth = 514;
    public final static short JPEGRestartInterval = 515;
    public final static short JPEGLosslessPredictors = 517;
    public final static short JPEGPointTransforms = 518;
    public final static short JPEGQTables = 519;
    public final static short JPEGDCTables = 520;
    public final static short JPEGACTables = 521;
    public final static short YCbCrCoefficients = 529;
    public final static short YCbCrSubSampling = 530;
    public final static short YCbCrPositioning = 531;
    public final static short ReferenceBlackWhite = 532;
    public final static int Copyright = 33432;
    public final static TiffShort ModelTiepointTag = new TiffShort(33922);
    public final static TiffShort ModelPixelScaleTag = new TiffShort(33550);
    public final static TiffShort ModelTransformationTag = new TiffShort(34264);
    public final static TiffShort GeoKeyDirectoryTag = new TiffShort(34735);
    public final static TiffShort GeoDoubleParamsTag = new TiffShort(34736);
    public final static TiffShort GeoAsciiParamsTag = new TiffShort(34737);
}