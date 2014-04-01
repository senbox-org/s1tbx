package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.io.CsvReader;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.*;
import static java.lang.System.arraycopy;

public abstract class SeadasFileReader {
    protected boolean mustFlipX;
    protected boolean mustFlipY;
    protected List<Attribute> globalAttributes;
    protected Map<Band, Variable> variableMap;
    protected NetcdfFile ncFile;
    protected SeadasProductReader productReader;
    protected int[] start = new int[2];
    protected int[] stride = new int[2];
    protected int[] count = new int[2];
    protected String sensor = null;

    protected int leadLineSkip = 0;
    protected int tailLineSkip = 0;
    protected static final SkipBadNav LAT_SKIP_BAD_NAV = new SkipBadNav() {
        @Override
        public final boolean isBadNav(double value) {
            return Double.isNaN(value) || value > 90.0 || value < -90.0;
        }
    };

    public SeadasFileReader(SeadasProductReader productReader) {
        this.productReader = productReader;
        ncFile = productReader.getNcfile();
        globalAttributes = ncFile.getGlobalAttributes();

    }

    public abstract Product createProduct() throws IOException;

    public synchronized void  readBandData(Band destBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                             int sourceHeight, int sourceStepX, int sourceStepY, ProductData destBuffer,
                             ProgressMonitor pm) throws IOException, InvalidRangeException {

        if (mustFlipY) {
            sourceOffsetY = destBand.getSceneRasterHeight() - (sourceOffsetY + sourceHeight);
        }
        if (mustFlipX) {
            sourceOffsetX = destBand.getSceneRasterWidth() - (sourceOffsetX + sourceWidth);
        }
        sourceOffsetY += leadLineSkip;
        start[0] = sourceOffsetY;
        start[1] = sourceOffsetX;
        stride[0] = sourceStepY;
        stride[1] = sourceStepX;
        count[0] = sourceHeight;
        count[1] = sourceWidth;
        Object buffer = destBuffer.getElems();
        Variable variable = variableMap.get(destBand);

        pm.beginTask("Reading band '" + variable.getShortName() + "'...", sourceHeight);
        try {
            Section section = new Section(start, count, stride);

            Array array;
            int[] newshape = {sourceHeight, sourceWidth};

            array = variable.read(section);
            if (array.getRank() > 2) {
                array = array.reshapeNoCopy(newshape);
            }
            Object storage;

            if (mustFlipX && !mustFlipY) {
                storage = array.flip(1).copyTo1DJavaArray();
            } else if (!mustFlipX && mustFlipY) {
                storage = array.flip(0).copyTo1DJavaArray();
            } else if (mustFlipX && mustFlipY) {
                storage = array.flip(0).flip(1).copyTo1DJavaArray();
            } else {
                storage = array.copyTo1DJavaArray();
            }

            arraycopy(storage, 0, buffer, 0, destBuffer.getNumElems());
        } finally {
            pm.done();
        }

    }

    final static Color LandBrown = new Color(100, 49, 12);
    final static Color LightBrown = new Color(137, 99, 31);
    final static Color FailRed = new Color(255, 0, 26);
    final static Color DeepBlue = new Color(0, 16, 143);
    final static Color BrightPink = new Color(255, 61, 245);
    final static Color LightCyan = new Color(193, 255, 254);
    final static Color NewGreen = new Color(132, 199, 101);
    final static Color Mustard = new Color(206, 204, 70);
    final static Color MediumGray = new Color(160, 160, 160);
    final static Color Purple = new Color(141, 11, 134);
    final static Color Coral = new Color(255, 0, 95);
    final static Color DarkGreen = new Color(0, 101, 28);
    final static Color TealGreen = new Color(0, 80, 79);
    final static Color LightPink = new Color(255, 208, 241);
    final static Color LightPurple = new Color(191, 143, 247);
    final static Color BurntUmber = new Color(165, 0, 11);
    final static Color TealBlue = new Color(0, 103, 144);
    final static Color Cornflower = new Color(38, 115, 245);

    protected void addFlagsAndMasks(Product product) {
        try {
            sensor = product.getMetadataRoot().getElement("Global_Attributes").getAttribute("Sensor_Name").getData().getElemString();
        } catch (Exception ignore) {
            try{
                sensor = product.getMetadataRoot().getElement("Global_Attributes").getAttribute("instrument").getData().getElemString();
            } catch(Exception ignored) {}
        }

        Band QFBand = product.getBand("l2_flags");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("L2Flags");
            flagCoding.addFlag("ATMFAIL", 0x01, "Atmospheric correction failure");
            flagCoding.addFlag("LAND", 0x02, "Land");
            flagCoding.addFlag("PRODWARN", 0x04, "One (or more) product algorithms generated a warning");
            flagCoding.addFlag("HIGLINT", 0x08, "High glint determined");
            flagCoding.addFlag("HILT", 0x10, "High (or saturating) TOA radiance");
            flagCoding.addFlag("HISATZEN", 0x20, "Large satellite zenith angle");
            flagCoding.addFlag("COASTZ", 0x40, "Shallow water (<30m)");
            flagCoding.addFlag("SPARE8", 0x80, "Unused");
            flagCoding.addFlag("STRAYLIGHT", 0x100, "Straylight determined");
            flagCoding.addFlag("CLDICE", 0x200, "Cloud/Ice determined");
            flagCoding.addFlag("COCCOLITH", 0x400, "Coccolithophores detected");
            flagCoding.addFlag("TURBIDW", 0x800, "Turbid water determined");
            flagCoding.addFlag("HISOLZEN", 0x1000, "High solar zenith angle");
            flagCoding.addFlag("SPARE14", 0x2000, "Unused");
            flagCoding.addFlag("LOWLW", 0x4000, "Low Lw @ 555nm (possible cloud shadow)");
            flagCoding.addFlag("CHLFAIL", 0x8000, "Chlorophyll algorithm failure");
            flagCoding.addFlag("NAVWARN", 0x10000, "Navigation suspect");
            flagCoding.addFlag("ABSAER", 0x20000, "Absorbing Aerosols determined");
            flagCoding.addFlag("SPARE19", 0x40000, "Unused");
            flagCoding.addFlag("MAXAERITER", 0x80000, "Maximum iterations reached for NIR iteration");
            flagCoding.addFlag("MODGLINT", 0x100000, "Moderate glint determined");
            flagCoding.addFlag("CHLWARN", 0x200000, "Chlorophyll out-of-bounds (<0.01 or >100 mg m^-3)");
            flagCoding.addFlag("ATMWARN", 0x400000, "Atmospheric correction warning; Epsilon out-of-bounds");
            flagCoding.addFlag("SPARE24", 0x800000, "Unused");
            flagCoding.addFlag("SEAICE", 0x1000000, "Sea ice determined");
            flagCoding.addFlag("NAVFAIL", 0x2000000, "Navigation failure");
            flagCoding.addFlag("FILTER", 0x4000000, "Insufficient data for smoothing filter");
            flagCoding.addFlag("SSTWARN", 0x8000000, "Sea surface temperature suspect");
            flagCoding.addFlag("SSTFAIL", 0x10000000, "Sea surface temperature algorithm failure");
            flagCoding.addFlag("HIPOL", 0x20000000, "High degree of polariztion determined");
            flagCoding.addFlag("PRODFAIL", 0x40000000, "One (or more) product algorithms produced a failure");
            flagCoding.addFlag("SPARE32", 0x80000000, "Unused");


            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("ATMFAIL", "Atmospheric correction failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.ATMFAIL",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("LAND", "Land",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.LAND",
                    LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("PRODWARN", "One (or more) product algorithms generated a warning",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.PRODWARN",
                    DeepBlue, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HILT", "High (or saturating) TOA radiance",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.HILT",
                    Color.GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("HIGLINT", "High glint determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.HIGLINT",
                    BrightPink, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISATZEN", "Large satellite zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.HISATZEN",
                    LightCyan, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("COASTZ", "Shallow water (<30m)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.COASTZ",
                    BurntUmber, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("STRAYLIGHT", "Straylight determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.STRAYLIGHT",
                    Color.YELLOW, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLDICE", "Cloud/Ice determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.CLDICE",
                    Color.WHITE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("COCCOLITH", "Coccolithophores detected",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.COCCOLITH",
                    Color.CYAN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("TURBIDW", "Turbid water determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.TURBIDW",
                    LightBrown, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISOLZEN", "High solar zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.HISOLZEN",
                    Purple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("LOWLW", "Low Lw @ 555nm (possible cloud shadow)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.LOWLW",
                    Cornflower, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CHLFAIL", "Chlorophyll algorithm failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.CHLFAIL",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVWARN", "Navigation suspect",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.NAVWARN",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ABSAER", "Absorbing Aerosols determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.ABSAER",
                    Color.ORANGE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("MAXAERITER", "Maximum iterations reached for NIR correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.MAXAERITER",
                    MediumGray, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("MODGLINT", "Moderate glint determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.MODGLINT",
                    LightPurple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CHLWARN", "Chlorophyll out-of-bounds (<0.01 or >100 mg m^-3)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.CHLWARN",
                    Color.LIGHT_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ATMWARN", "Atmospheric correction warning; Epsilon out-of-bounds",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.ATMWARN",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SEAICE", "Sea ice determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.SEAICE",
                    Color.DARK_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVFAIL", "Navigation failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.NAVFAIL",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("FILTER", "Insufficient data for smoothing filter",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.FILTER",
                    Color.LIGHT_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTWARN", "Sea surface temperature suspect",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.SSTWARN",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTFAIL", "Sea surface temperature algorithm failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.SSTFAIL",
                    FailRed, 0.1));
            product.getMaskGroup().add(Mask.BandMathsType.create("HIPOL", "High degree of polariztion determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.HIPOL",
                    Color.PINK, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("PRODFAIL", "One (or more) product algorithms produced a failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l2_flags.PRODFAIL",
                    FailRed, 0.1));

        }
        Band QFBandSST = product.getBand("flags_sst");
        if (QFBandSST != null) {
            FlagCoding flagCoding = new FlagCoding("SST_Flags");
            flagCoding.addFlag("ISMASKED", 0x01, "Pixel was already masked");
            flagCoding.addFlag("BTBAD", 0x02, "Bad Brightness Temperatures");
            flagCoding.addFlag("BTRANGE", 0x04, "Brightness Temperatures outside valid range");
            flagCoding.addFlag("BTDIFF", 0x08, "Brightness Temperatures are too different");
            flagCoding.addFlag("SSTRANGE", 0x10, "Computed SST outside valid range");
            flagCoding.addFlag("SSTREFDIFF", 0x20, "Computed SST too different from reference SST");
            flagCoding.addFlag("SST4DIFF", 0x40, "Computed SST too different from computed 4 micron SST");
            flagCoding.addFlag("SST4VDIFF", 0x80, "Computed SST very different from computed 4 micron SST");
//            flagCoding.addFlag("SST3DIFF", 0x40, "Computed SST too different from computed triple-window SST");
//            flagCoding.addFlag("SST3VDIFF", 0x80, "Computed SST very different from computed triple-window SST");
            flagCoding.addFlag("BTNONUNIF", 0x100, "Spatially Non-uniform Brightness Temperatures");
            flagCoding.addFlag("BTVNONUNIF", 0x200, "Very Spatially Non-uniform Brightness Temperatures");
            flagCoding.addFlag("BT4REFDIFF", 0x400, "4 micron Brightness Temperature differs from reference");
            flagCoding.addFlag("REDNONUNIF", 0x800, "Spatially Non-uniform red band");
            flagCoding.addFlag("HISENZ", 0x1000, "High sensor zenith angle");
            flagCoding.addFlag("VHISENZ", 0x2000, "Very High sensor zenith angle");
            flagCoding.addFlag("SSTREFVDIFF", 0x4000, "Computed SST very different from reference SST)");
            flagCoding.addFlag("CLOUD", 0x8000, "Cloud Detected");
            if (sensor.equalsIgnoreCase("AVHRR")) {
                flagCoding.addFlag("SUNLIGHT", 0x40, "Stray sunlight detected (AVHRR)");
                flagCoding.addFlag("ASCEND", 0x80, "AVHRR in ascending node (daytime)");
                flagCoding.addFlag("GLINT", 0x400, "Sun glint detected (AVHRR)");
            }

            product.getFlagCodingGroup().add(flagCoding);
            QFBandSST.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("ISMASKED", "Pixel was already masked",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.ISMASKED",
                    LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTBAD", "Bad Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTBAD",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTRANGE", "Brightness Temperatures outside valid range",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTRANGE",
                    DeepBlue, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTDIFF", "Brightness Temperatures are too different",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTDIFF",
                    Color.GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTRANGE", "Computed SST outside valid range",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTRANGE",
                    BrightPink, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTREFDIFF", "Computed SST too different from reference SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTREFDIFF",
                    LightCyan, 0.5));
            if (sensor.equalsIgnoreCase("AVHRR")){
                product.getMaskGroup().add(Mask.BandMathsType.create("SUNLIGHT", "Stray sunlight detected (AVHRR)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.SUNLIGHT",
                        BurntUmber, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create("ASCEND", "AVHRR in ascending node (daytime)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.ASCEND",
                        LightBrown, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create("GLINT", "Sun glint detected (AVHRR)",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.GLINT",
                        Color.YELLOW, 0.5));

            } else {
                product.getMaskGroup().add(Mask.BandMathsType.create("SST4DIFF", "Computed SST too different from computed 4 micron SST",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.SST4DIFF",
                        BurntUmber, 0.5));
                product.getMaskGroup().add(Mask.BandMathsType.create("SST4VDIFF", "Computed SST very different from computed 4 micron SST",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.SST4VDIFF",
                        LightBrown, 0.2));
                product.getMaskGroup().add(Mask.BandMathsType.create("BT4REFDIFF", "4 micron Brightness Temperature differs from reference",
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(), "flags_sst.BT4REFDIFF",
                        Color.YELLOW, 0.5));
            }
            product.getMaskGroup().add(Mask.BandMathsType.create("BTNONUNIF", "Spatially Non-uniform Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTNONUNIF",
                    Color.ORANGE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTVNONUNIF", "Very Spatially Non-uniform Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTVNONUNIF",
                    Color.CYAN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("REDNONUNIF", "Spatially Non-uniform red band",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.REDNONUNIF",
                    Purple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISENZ", "High sensor zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.HISENZ",
                    Cornflower, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("VHISENZ", "Very High sensor zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.VHISENZ",
                    LightPurple, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTREFVDIFF", "Computed SST very different from reference SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTREFVDIFF",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD", "Cloud Detected",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.CLOUD",
                    Color.WHITE, 0.5));
        }
        Band QFBandSST4 = product.getBand("flags_sst4");
        if (QFBandSST4 != null) {
            FlagCoding flagCoding = new FlagCoding("SST4_Flags");
            flagCoding.addFlag("ISMASKED", 0x01, "Pixel was already masked");
            flagCoding.addFlag("BTBAD", 0x02, "Bad Brightness Temperatures");
            flagCoding.addFlag("BTRANGE", 0x04, "Brightness Temperatures outside valid range");
            flagCoding.addFlag("BTDIFF", 0x08, "Brightness Temperatures are too different");
            flagCoding.addFlag("SSTRANGE", 0x10, "Computed SST outside valid range");
            flagCoding.addFlag("SSTREFDIFF", 0x20, "Computed SST too different from reference SST");
            flagCoding.addFlag("SST4DIFF", 0x40, "Computed SST too different from computed 4 micron SST");
            flagCoding.addFlag("SST4VDIFF", 0x80, "Computed SST very different from computed 4 micron SST");
//            flagCoding.addFlag("SST3DIFF", 0x40, "Computed SST too different from computed triple-window SST");
//            flagCoding.addFlag("SST3VDIFF", 0x80, "Computed SST very different from computed triple-window SST");
            flagCoding.addFlag("BTNONUNIF", 0x100, "Spatially Non-uniform Brightness Temperatures");
            flagCoding.addFlag("BTVNONUNIF", 0x200, "Very Spatially Non-uniform Brightness Temperatures");
            flagCoding.addFlag("BT4REFDIFF", 0x400, "4 micron Brightness Temperature differs from reference");
            flagCoding.addFlag("REDNONUNIF", 0x800, "Spatially Non-uniform red band");
            flagCoding.addFlag("HISENZ", 0x1000, "High sensor zenith angle");
            flagCoding.addFlag("VHISENZ", 0x2000, "Very High sensor zenith angle");
            flagCoding.addFlag("SSTREFVDIFF", 0x4000, "Computed SST very different from reference SST)");
            flagCoding.addFlag("CLOUD", 0x8000, "Cloud Detected");

            product.getFlagCodingGroup().add(flagCoding);
            QFBandSST4.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("ISMASKED", "Pixel was already masked",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.ISMASKED",
                    LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTBAD", "Bad Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTBAD",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTRANGE", "Brightness Temperatures outside valid range",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTRANGE",
                    DeepBlue, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTDIFF", "Brightness Temperatures are too different",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTDIFF",
                    Color.GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTRANGE", "Computed SST outside valid range",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTRANGE",
                    BrightPink, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTREFDIFF", "Computed SST too different from reference SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTREFDIFF",
                    LightCyan, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SST4DIFF", "Computed SST too different from computed 4 micron SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SST4DIFF",
                    BurntUmber, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SST4VDIFF", "Computed SST very different from computed 4 micron SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SST4VDIFF",
                    LightBrown, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTNONUNIF", "Spatially Non-uniform Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTNONUNIF",
                    Color.ORANGE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("BTVNONUNIF", "Very Spatially Non-uniform Brightness Temperatures",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BTVNONUNIF",
                    Color.CYAN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BT4REFDIFF", "4 micron Brightness Temperature differs from reference",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.BT4REFDIFF",
                    Color.YELLOW, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("REDNONUNIF", "Spatially Non-uniform red band",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.REDNONUNIF",
                    Purple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISENZ", "High sensor zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.HISENZ",
                    Cornflower, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("VHISENZ", "Very High sensor zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.VHISENZ",
                    LightPurple, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTREFVDIFF", "Computed SST very different from reference SST",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.SSTREFVDIFF",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD", "Cloud Detected",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags_sst.CLOUD",
                    Color.WHITE, 0.5));
        }

        Band QFBandSSTqual = product.getBand("qual_sst");
        if (QFBandSSTqual != null) {

            product.getMaskGroup().add(Mask.BandMathsType.create("Best (SST)", "Highest quality SST retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 0",
                    SeadasFileReader.Cornflower, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Good (SST)", "Good quality SST retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 1",
                    SeadasFileReader.LightPurple, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Questionable (SST)", "Questionable quality SST retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 2",
                    SeadasFileReader.BurntUmber, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bad (SST)", "Bad quality SST retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 3",
                    SeadasFileReader.FailRed, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("No SST Retrieval", "No SST retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 4",
                    SeadasFileReader.FailRed, 0.6));
        }
        Band QFBandSSTqual4 = product.getBand("qual_sst4");
        if (QFBandSSTqual4 != null) {

            product.getMaskGroup().add(Mask.BandMathsType.create("Best (SST4)", "Highest quality SST4 retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 0",
                    SeadasFileReader.Cornflower, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Good (SST4)", "Good quality SST4 retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 1",
                    SeadasFileReader.LightPurple, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Questionable (SST4)", "Questionable quality SST4 retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 2",
                    SeadasFileReader.BurntUmber, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bad (SST4)", "Bad quality SST4 retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 3",
                    SeadasFileReader.FailRed, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("No SST4 Retrieval", "No SST4 retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 4",
                    SeadasFileReader.FailRed, 0.6));
        }
    }

    public Map<Band, Variable> addBands(Product product,
                                        List<Variable> variables) {
        final Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        for (Variable variable : variables) {
            Band band = addNewBand(product, variable);
            if (band != null) {
                bandToVariableMap.put(band, variable);
            }
        }
        setSpectralBand(product);

        return bandToVariableMap;
    }

    protected void setSpectralBand(Product product) {
        int spectralBandIndex = 0;
        for (String name : product.getBandNames()) {
            Band band = product.getBandAt(product.getBandIndex(name));
            if (name.matches("\\w+_\\d{3,}")) {
                String[] parts = name.split("_");
                String wvlstr = parts[parts.length - 1].trim();
                //Some bands have the wvl portion in the middle...
                if (!wvlstr.matches("^\\d{3,}")) {
                    wvlstr = parts[parts.length - 2].trim();
                }
                final float wavelength = Float.parseFloat(wvlstr);
                band.setSpectralWavelength(wavelength);
                band.setSpectralBandIndex(spectralBandIndex++);
            }
        }
    }

    protected Band addNewBand(Product product, Variable variable) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band = null;

        int variableRank = variable.getRank();
        if (variableRank == 2) {
            final int[] dimensions = variable.getShape();
            final int height = dimensions[0] - leadLineSkip - tailLineSkip;
            final int width = dimensions[1];
            if (height == sceneRasterHeight && width == sceneRasterWidth) {
                final String name = variable.getShortName();
                final int dataType = getProductDataType(variable);
                band = new Band(name, dataType, width, height);

                product.addBand(band);

                try {
                    Attribute fillValue = variable.findAttribute("_FillValue");
                    if (fillValue == null){
                        fillValue = variable.findAttribute("bad_value_scaled");
                    }
                    band.setNoDataValue((double) fillValue.getNumericValue().floatValue());
                    band.setNoDataValueUsed(true);
                } catch (Exception ignored) { }

                final List<Attribute> list = variable.getAttributes();
                double[] validMinMax = {0.0,0.0};
                for (Attribute hdfAttribute : list) {
                    final String attribName = hdfAttribute.getShortName();
                    if ("units".equals(attribName)) {
                        band.setUnit(hdfAttribute.getStringValue());
                    } else if ("long_name".equals(attribName)) {
                        band.setDescription(hdfAttribute.getStringValue());
                    } else if ("slope".equals(attribName)) {
                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                    } else if ("intercept".equals(attribName)) {
                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                    } else if ("scale_factor".equals(attribName)) {
                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                    } else if ("add_offset".equals(attribName)) {
                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                    } else if (attribName.startsWith("valid_")){
                        if ("valid_min".equals(attribName)){
                            validMinMax[0] = hdfAttribute.getNumericValue(0).doubleValue();
                        } else if ("valid_max".equals(attribName)){
                            validMinMax[1] = hdfAttribute.getNumericValue(0).doubleValue();
                        } else if ("valid_range".equals(attribName)){
                            validMinMax[0] = hdfAttribute.getNumericValue(0).doubleValue();
                            validMinMax[1] = hdfAttribute.getNumericValue(1).doubleValue();
                        }
                    }
                }
                if (validMinMax[0] != validMinMax[1]){
                    double[] minmax = {0.0,0.0};
                    minmax[0] = validMinMax[0];
                    minmax[1] = validMinMax[1];

                    if (band.getScalingFactor() != 1.0) {
                        minmax[0] *= band.getScalingFactor();
                        minmax[1] *= band.getScalingFactor();
                    }
                    if (band.getScalingOffset() != 0.0) {
                        minmax[0] += band.getScalingOffset();
                        minmax[1] += band.getScalingOffset();
                    }
                    String validExp = format("%s >= %.2f && %s <= %.2f", name, minmax[0], name, minmax[1]);
                    band.setValidPixelExpression(validExp);//.format(name, validMinMax[0], name, validMinMax[1]));
                }
            }
        }
        return band;
    }

    public void addGlobalMetadata(Product product) {
        final MetadataElement globalElement = new MetadataElement("Global_Attributes");
        addAttributesToElement(globalAttributes, globalElement);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(globalElement);
    }

    public void addInputParamMetadata(Product product) throws ProductIOException {

        Variable inputParams = ncFile.findVariable("Input_Parameters");
        if (inputParams != null) {
            final MetadataElement inputParamsMeta = new MetadataElement("Input_Parameters");
            Array array;
            try {
                array = inputParams.read();
            } catch (IOException e) {
                throw new ProductIOException(e.getMessage());
            }

            String[] lines = array.toString().split("\n");
            for (String line : lines) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    final String name = parts[0].trim();
                    final String value = parts[1].trim();
                    final ProductData data = ProductData.createInstance(ProductData.TYPE_ASCII, value);
                    final MetadataAttribute attribute = new MetadataAttribute(name, data, true);
                    inputParamsMeta.addAttribute(attribute);


                }
            }

            final MetadataElement metadataRoot = product.getMetadataRoot();
            metadataRoot.addElement(inputParamsMeta);


        }
    }

    public void addScientificMetadata(Product product) throws ProductIOException {

        Group group = ncFile.findGroup("Scan-Line_Attributes");
        if (group == null){
            group = ncFile.findGroup("scan_line_attributes");
        }
        if (group != null) {
            final MetadataElement scanLineAttrib = getMetadataElementSave(product, "Scan_Line_Attributes");
            handleMetadataGroup(group, scanLineAttrib);
        }

        group = ncFile.findGroup("Sensor_Band_Parameters");
        if (group == null){
            group = ncFile.findGroup("sensor_band_parameters");
        }
        if (group != null) {
            final MetadataElement sensorBandParam = getMetadataElementSave(product, "Sensor_Band_Parameters");
            handleMetadataGroup(group, sensorBandParam);
        }
    }

    public void addBandMetadata(Product product) throws ProductIOException {
        Group group = ncFile.findGroup("geophysical_data");
        if (group == null){
            group = ncFile.findGroup("Geophysical_Data");
        }
        if (productReader.getProductType() == SeadasProductReader.ProductType.Level2_Aquarius) {
            group = ncFile.findGroup("Aquarius_Data");
        }
        if (productReader.getProductType() == SeadasProductReader.ProductType.Level1B_HICO) {
            group = ncFile.findGroup("products");
        }

        if (group != null) {
            final MetadataElement bandAttributes = new MetadataElement("Band_Attributes");
            List<Variable> variables = group.getVariables();
            for (Variable variable : variables) {
                final String name = variable.getShortName();
                final MetadataElement sdsElement = new MetadataElement(name + ".attributes");
                final int dataType = getProductDataType(variable);
                final MetadataAttribute prodtypeattr = new MetadataAttribute("data_type", dataType);

                sdsElement.addAttribute(prodtypeattr);
                bandAttributes.addElement(sdsElement);

                final List<Attribute> list = variable.getAttributes();
                for (Attribute varAttribute : list) {
                    addAttributeToElement(sdsElement, varAttribute);
                }
            }
            final MetadataElement metadataRoot = product.getMetadataRoot();
            metadataRoot.addElement(bandAttributes);
        }
    }

    public void computeLatLonBandData(int height, int width, Band latBand, Band lonBand,
                                      final float[] latRawData, final float[] lonRawData,
                                      final int[] colPoints) {

        float[] latFloats = new float[height*width];
        float[] lonFloats = new float[height*width];
        final int rawWidth = colPoints.length;

        int colPointIdx = 0;
        int p1 = colPoints[colPointIdx] - 1;
        int p2 = colPoints[++colPointIdx] - 1;

        for (int x = 0; x < width; x++) {
            if (x == p2 && colPointIdx < rawWidth - 1) {
                p1 = p2;
                p2 = colPoints[++colPointIdx] - 1;
            }
            final int steps = p2 - p1;
            final double step = 1.0 / steps;
            final double weight = step * (x - p1);
            for (int y = 0; y < height; y++) {
                final int rawPos2 = y * rawWidth + colPointIdx;
                final int rawPos1 = rawPos2 - 1;
                final int pos = y * width + x;
                latFloats[pos] = computeGeoPixel(latRawData[rawPos1], latRawData[rawPos2], weight);
                lonFloats[pos] = computeGeoPixel(lonRawData[rawPos1], lonRawData[rawPos2], weight);
            }
        }

        latBand.setDataElems(latFloats);
        lonBand.setDataElems(lonFloats);
    }

    public float computeGeoPixel(final float a, final float b, final double weight) {
        if ((b - a) > 180) {
            final float b2 = b - 360;
            final double v = a + (b2 - a) * weight;
            if (v >= -180) {
                return (float) v;
            } else {
                return (float) (v + 360);
            }
        } else {
            return (float) (a + (b - a) * weight);
        }
    }


    public boolean getDefaultFlip() throws ProductIOException {
        boolean startNodeAscending = false;
        boolean endNodeAscending = false;
        try {
            Attribute start_node = findAttribute("Start_Node");
            if (start_node == null){
                start_node = findAttribute("startDirection");
            }
            String startAttr = start_node.getStringValue();

            if (startAttr != null) {
                startNodeAscending = startAttr.equalsIgnoreCase("Ascending");
            }
            Attribute end_node = findAttribute("End_Node");
            if (end_node == null){
                end_node = findAttribute("startDirection");
            }
            String endAttr = end_node.getStringValue();

            if (endAttr != null) {
                endNodeAscending = endAttr.equalsIgnoreCase("Ascending");
            }

        } catch (Exception ignored) { }

        return (startNodeAscending && endNodeAscending);
    }


    protected static HashMap<String, String> readTwoColumnTable(String resourceName) {
        final InputStream stream = SeadasProductReader.class.getResourceAsStream(resourceName);
        if (stream != null) {
            try {
                HashMap<String, String> validExpressionMap = new HashMap<String, String>(32);
                final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{';'});
                final List<String[]> table = csvReader.readStringRecords();
                for (String[] strings : table) {
                    if (strings.length == 2) {
                        validExpressionMap.put(strings[0], strings[1]);
                    }
                }
                return validExpressionMap;
            } catch (IOException e) {
                // ?
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ok
                }
            }
        }
        return new HashMap<String, String>(0);
    }

    public static void reverse(float[] data) {
        final int n = data.length;
        for (int i = 0; i < n/2; i++) {
            float temp = data[i];
            data[i] = data[n - i - 1];
            data[n - i - 1] = temp;
        }
    }

    public Attribute findAttribute(String name, List<Attribute> attributesList) {
        for (Attribute a : attributesList) {
            if (name.equalsIgnoreCase(a.getShortName()))
                return a;
        }
        return null;
    }

    public Attribute findAttribute(String name) {
        return findAttribute(name, globalAttributes);
    }

    public String getStringAttribute(String key, List<Attribute> attributeList) throws ProductIOException {
        Attribute attribute = findAttribute(key, attributeList);
        if (attribute == null || attribute.getLength() != 1) {
            throw new ProductIOException("Global attribute '" + key + "' is missing.");
        } else {
            return attribute.getStringValue().trim();
        }
    }

    public String getStringAttribute(String key) throws ProductIOException {
        return getStringAttribute(key, globalAttributes);
    }

    public int getIntAttribute(String key, List<Attribute> globalAttributes) throws ProductIOException {
        Attribute attribute = findAttribute(key, globalAttributes);
        if (attribute == null) {
            throw new ProductIOException("Global attribute '" + key + "' is missing.");
        } else {
            return attribute.getNumericValue(0).intValue();
        }
    }

    public int getIntAttribute(String key) throws ProductIOException {
        return getIntAttribute(key, globalAttributes);
    }

    public float getFloatAttribute(String key, List<Attribute> globalAttributes) throws ProductIOException {
        Attribute attribute = findAttribute(key, globalAttributes);
        if (attribute == null) {
            throw new ProductIOException("Global attribute '" + key + "' is missing.");
        } else {
            return attribute.getNumericValue(0).floatValue();
        }
    }

    public float getFloatAttribute(String key) throws ProductIOException {
        return getFloatAttribute(key, globalAttributes);
    }

    private ProductData.UTC getUTCAttribute(String key, List<Attribute> globalAttributes) {
        Attribute attribute = findAttribute(key, globalAttributes);
        Boolean isModis = false;
        Boolean isISO = false;
        try {
            isModis = findAttribute("MODIS_Resolution", globalAttributes).isString();
        } catch (Exception ignored) { }
        try {
            isISO = findAttribute("time_coverage_start", globalAttributes).isString();
        } catch (Exception ignored) { }

        if (attribute != null) {
            String timeString = attribute.getStringValue().trim();

            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyDDDHHmmssSSS");

            final DateFormat dateFormatISO = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // only needed as a stop-gap to handle an intermediate version of l2gen metadata
            final DateFormat dateFormatISOnopunc = ProductData.UTC.createDateFormat("yyyyMMdd'T'HHmmss'Z'");

            final DateFormat dateFormatModis = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
            final DateFormat dateFormatOcts = ProductData.UTC.createDateFormat("yyyyMMdd HH:mm:ss.SSSSSS");
            try {
                if (isISO) {
                    Date date;
                    try {
                        date = dateFormatISO.parse(timeString);
                    } catch (Exception e) {
                        date = dateFormatISOnopunc.parse(timeString);
                    }

//                    String milliSeconds = timeString.substring(timeString.length());
                    return ProductData.UTC.create(date, 0);
                } else if (isModis) {
                    final Date date = dateFormatModis.parse(timeString);
                    String milliSeconds = timeString.substring(timeString.length() - 3);
                    return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
                } else if (productReader.getProductType() == SeadasProductReader.ProductType.Level1A_OCTS) {
                    final Date date = dateFormatOcts.parse(timeString);
                    String milliSeconds = timeString.substring(timeString.length() - 3);
                    return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
                } else  {
                    final Date date = dateFormat.parse(timeString);
                    String milliSeconds = timeString.substring(timeString.length() - 3);
                    return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    public ProductData.UTC getUTCAttribute(String key) {
        return getUTCAttribute(key, globalAttributes);
    }

    private MetadataElement getMetadataElementSave(Product product, String name) {
        final MetadataElement metadataElement = product.getMetadataRoot().getElement(name);
        final MetadataElement namedElem;
        if (metadataElement == null) {
            namedElem = new MetadataElement(name);
            product.getMetadataRoot().addElement(namedElem);
        } else {
            namedElem = metadataElement;
        }
        return namedElem;
    }

    private void handleMetadataGroup(Group group, MetadataElement metadataElement) throws ProductIOException {
        List<Variable> variables = group.getVariables();
        for (Variable variable : variables) {
            final String name = variable.getShortName();
            final int dataType = getProductDataType(variable);
            Array array;
            try {
                array = variable.read();
            } catch (IOException e) {
                throw new ProductIOException(e.getMessage());
            }
            final ProductData data = ProductData.createInstance(dataType, array.getStorage());
            final MetadataAttribute attribute = new MetadataAttribute("data", data, true);

            final MetadataElement sdsElement = new MetadataElement(name);
            sdsElement.addAttribute(attribute);
            metadataElement.addElement(sdsElement);

            final List<Attribute> list = variable.getAttributes();
            for (Attribute hdfAttribute : list) {
                final String attribName = hdfAttribute.getShortName();
                if ("units".equals(attribName)) {
                    attribute.setUnit(hdfAttribute.getStringValue());
                } else if ("long_name".equals(attribName)) {
                    attribute.setDescription(hdfAttribute.getStringValue());
                } else {
                    addAttributeToElement(sdsElement, hdfAttribute);
                }
            }
        }
    }

    protected void addAttributesToElement(List<Attribute> globalAttributes, final MetadataElement element) {
        for (Attribute attribute : globalAttributes) {
            //if (attribute.getName().contains("EV")) {
            if (attribute.getShortName().matches(".*(EV|Value|Bad|Nois|Electronics|Dead|Detector).*")) {
            } else {
                addAttributeToElement(element, attribute);
            }
        }
    }

    protected void addAttributeToElement(final MetadataElement element, final Attribute attribute) {
        final MetadataAttribute metadataAttribute = attributeToMetadata(attribute);
        element.addAttribute(metadataAttribute);
    }

    MetadataAttribute attributeToMetadata(Attribute attribute) {
        final int productDataType = getProductDataType(attribute.getDataType(), false, false);
        if (productDataType != -1) {
            ProductData productData;
            if (attribute.isString()) {
                productData = ProductData.createInstance(attribute.getStringValue());
            } else if (attribute.isArray()) {
                productData = ProductData.createInstance(productDataType, attribute.getLength());
                productData.setElems(attribute.getValues().getStorage());
            } else {
                productData = ProductData.createInstance(productDataType, 1);
                productData.setElems(attribute.getValues().getStorage());
            }
            return new MetadataAttribute(attribute.getShortName(), productData, true);
        }
        return null;
    }

    public static int getProductDataType(Variable variable) {
        return getProductDataType(variable.getDataType(), variable.isUnsigned(), true);
    }

    public static int getProductDataType(DataType dataType, boolean unsigned, boolean rasterDataOnly) {
        if (dataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (dataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (dataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (dataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (dataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (dataType == DataType.CHAR) {
                // return ProductData.TYPE_ASCII; TODO - handle this case
            } else if (dataType == DataType.STRING) {
                return ProductData.TYPE_ASCII;
            }
        }
        return -1;
    }

    public int convertToFlagMask(String name) {
        if (name.matches("f\\d\\d_name")) {
            final String number = name.substring(1, 3);
            final int i = Integer.parseInt(number) - 1;
            if (i >= 0) {
                return 1 << i;
            }
        }
        return 0;
    }

    public ProductData readData(Variable variable) throws ProductIOException {
        final int dataType = getProductDataType(variable);
        Array array;
        try {
            array = variable.read();

        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
        return ProductData.createInstance(dataType, array.getStorage());
    }


    float[] flatten2DimArray(float[][] twoDimArray) {
        // Converts an array of two dimensions into a single dimension array row by row.
        float[] flatArray = new float[twoDimArray.length * twoDimArray[0].length];
        for (int row = 0; row < twoDimArray.length; row++) {
            int offset = row * twoDimArray[row].length;
            arraycopy(twoDimArray[row], 0, flatArray, offset, twoDimArray[row].length);
        }
        return flatArray;
    }

    protected synchronized void invalidateLines(SkipBadNav skipBadNav, Variable latitude) throws IOException {
        final int[] shape = latitude.getShape();
        try {
            int lineCount = shape[0];
            final int[] start = new int[]{0,0};
            // just grab the first and last pixel for each scan line
            final int[] stride = new int[]{1,shape[1]-1};
            final int[] count = new int[]{shape[0],shape[1]};
            Section section = new Section(start, count, stride);
            Array array;
            synchronized (ncFile) {
                array = latitude.read(section);
            }
            for (int i = 0; i < lineCount; i++) {
                int ix = i * 2;
                float valstart = array.getFloat(ix);
                float valend = array.getFloat(ix+1);
                if (skipBadNav.isBadNav(valstart) || skipBadNav.isBadNav(valend)) {
                    leadLineSkip++;
                } else {
                    break;
                }
            }
            for (int i = lineCount; i-- > 0; ) {
                int ix = i * 2;
                float valstart = array.getFloat(ix);
                float valend = array.getFloat(ix+1);
                if (skipBadNav.isBadNav(valstart) || skipBadNav.isBadNav(valend)) {
                    tailLineSkip++;
                } else {
                    break;
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage());
        }
    }

    private interface SkipBadNav {
        boolean isBadNav(double value);
    }

}
