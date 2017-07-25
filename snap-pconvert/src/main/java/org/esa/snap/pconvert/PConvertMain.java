/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.pconvert;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.jai.JAIUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * This class represents the BEAM Product Converter Tool. It is designed as a simple command line tool. The executable
 * is called <code>pconvert</code>. Please use the help option in order to find out all possibilities of this tool.
 * <p>In the following cases <code>pconvert</code> prints a usage help text to the console:
 * <p>
 * <pre>
 *    pconvert
 *    pconvert -?
 *    pconvert -h
 *    pconvert -help
 * </pre>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class PConvertMain {

    private static final String EXE_NAME = "pconvert";
    private static final String EXE_VERSION = "1.4";
    private static final int[] DEFAULT_RGB_BAND_INDICES = new int[]{8, 5, 2};
    private static final double[] DEFAULT_HISTO_SKIP_PERCENTAGE = new double[]{1, 4};
    private static final String DEFAULT_FORMAT_EXT = "dim";

    private File[] _inputFiles;
    private File _outputDir;
    private String _formatExt;
    private String _formatName;
    private boolean _imageFormat;
    private int[] _bandIndices;
    private int[] _maxOutputResolution;
    private double[] _histoSkipRatios;
    private File _rgbProfile;
    private File _colorPalette;
    private String _histogramMatching;
    private Color _noDataColor;
    private Integer _forcedWidth;
    private Integer _forcedHeight;

    /////////////////////////////////////////////////////////////////////////
    // Usage
    /////////////////////////////////////////////////////////////////////////

    private static void printUsage() {
        StringBuffer sb = new StringBuffer(1024);
        sb.append(EXE_NAME + " version " + EXE_VERSION);
        sb.append("\n");
        sb.append("Usage: " + EXE_NAME + " [<options>] <file-1> [<file-2> <file-3> ...]\n");
        sb.append("\n");
        sb.append("  where the <file-i> are the input data products and <options>\n");
        sb.append("  can be a combination of the following options:\n");
        sb.append("\n");
        sb.append("  -f or --format <ext>\n");
        sb.append("     Specifies output format and file extension,\n");
        sb.append("     possible values for <ext> are\n");
        sb.append("       For product conversion:\n");
        sb.append("         dim  - BEAM-DIMAP product format\n");
        sb.append("         h5   - HDF5 product format\n");
        sb.append("         tifp - GeoTIFF product format\n");
        sb.append("       For image conversion:\n");
        sb.append("         png  - Portable Network Graphics image format\n");
        sb.append("         jpg  - JPEG image format\n");
        sb.append("         tif  - GeoTIFF image format\n");
        sb.append("         bmp  - Microsoft Bitmap image format\n");
        sb.append("       Note:\n");
        sb.append("         If image conversion is selected the product must at least\n");
        sb.append("         contain three bands to create an image.\n");
        sb.append("         If this is not the case, you must use one of the options -b or -p\n");
        sb.append("         to define the image content.\n");
        sb.append("     The default value is \"-f " + DEFAULT_FORMAT_EXT + "\"\n");
        sb.append("  -b or --bands <i> or <iR>,<iG>,<iB> or <i1>,<i2>,<i3>,<i4>...\n");
        sb.append("     Don't use this option in combination with option -p.\n");
        sb.append("     Specifies indices of the bands to be exported as a comma separated\n");
        sb.append("     index list, 1 (one) corresponds to the first band.\n");
        sb.append("     For image output, the number of bands should be 1 (greyscale) or\n");
        sb.append("     3 (RGB), the default value is \"-b " + StringUtils.arrayToCsv(
                DEFAULT_RGB_BAND_INDICES) + "\" (optimized for MERIS).\n");
        sb.append("     For product output, the default value includes all bands.\n");
        sb.append("\n");
        sb.append("  -p or --rgb-profile <file-path>\n");
        sb.append("     Valid for greyscale or RGB image output only.\n");
        sb.append("     Don't use this option in combination with option -b.\n");
        sb.append("     Specifies the file path to a text file containing an mathematic\n");
        sb.append("     band expression for each of the RGB channels.\n");
        sb.append("     The syntax of the file is as follows:\n");
        sb.append("         red = <red-expression>\n");
        sb.append("         green = <green-expression>\n");
        sb.append("         blue = <blue-expression>\n");
        sb.append("     It is also possible to use r, g and b instead of red, green and blue.\n");
        sb.append("     Empty lines and lines beginning with the '#' character are ignored.\n");
        sb.append("\n");
        sb.append("  -s or --histo-skip <lower>,<upper>\n");
        sb.append("     Valid for greyscale or RGB image output only.\n");
        sb.append("     Specifies the amount of pixels in percent to be skipped from the\n");
        sb.append("     lower resp. upper end of each of the histograms of the R,G and B\n");
        sb.append("     channels. For image output, the default value is \"-s " + StringUtils.arrayToCsv(
                DEFAULT_HISTO_SKIP_PERCENTAGE) + "\"\n");
        sb.append("     For product output, the option is ignored.\n");
        sb.append("\n");
        sb.append("  -m or --histo-match <algorithm>\n");
        sb.append("     Valid for greyscale or RGB image output only.\n");
        sb.append("     Specifies the histogram matching algorithm to be applied.\n");
        sb.append("     Possible values for <algorithm> are:\n");
        sb.append("         " + ImageInfo.HISTOGRAM_MATCHING_OFF + " - no histogram matching\n");
        sb.append("         " + ImageInfo.HISTOGRAM_MATCHING_EQUALIZE + " - force an equalized output histogram\n");
        sb.append("         " + ImageInfo.HISTOGRAM_MATCHING_NORMALIZE + " - force a normalized output histogram\n");
        sb.append("     the default value is \"-m " + ImageInfo.HISTOGRAM_MATCHING_OFF + "\".\n");
        sb.append("\n");
        sb.append("  -c or --color-palette <file-path>\n");
        sb.append("     Valid only for image output of a single band.\n");
        sb.append("     Specifies the file path to a text file containing a colour\n");
        sb.append("     palette definition.\n");
        sb.append("\n");
        sb.append("  -n or --no-data-color <red>,<green>,<blue>[,<alpha>]\n");
        sb.append("     Valid for greyscale or RGB image output only.\n");
        sb.append("     Specifies the colour that should be used for the no-data layer.\n");
        sb.append("     The alpha value is optional. All component values have to be between\n");
        sb.append("     0 and 255. An alpha value of 255 means fully opaque and 0 means\n");
        sb.append("     fully transparent.\n");
        sb.append("\n");
        sb.append("  -r or --max-res <x-res>,<y-res>\n");
        sb.append("     Specifies the maximum image output size in pixels, for example 512,512.\n");
        sb.append("     By default, the full product scene size is taken.\n");
        sb.append("     This option can't be combined with -H or -W\n");
        sb.append("\n");
        sb.append("  -W or --width  <width>\n");
        sb.append("     Forces the specified image output width in pixels, for example 512.\n");
        sb.append("     The image aspect ratio will be preserved.\n");
        sb.append("     This option can't be combined with -r or -H\n");
        sb.append("\n");
        sb.append("  -H or --height  <height>\n");
        sb.append("     Forces the specified image output height in pixels, for example 512.\n");
        sb.append("     The image aspect ratio will be preserved.\n");
        sb.append("     This option can't be combined with -r or -W\n");
        sb.append("\n");
        sb.append("  -o or --outdir <dir-path>\n");
        sb.append("     Specifies the output directory.\n");
        sb.append("     The default value is the current working directory.\n");
        sb.append("\n");
        sb.append("  -d or --debug\n");
        sb.append("     Turns the debug mode on.\n");
        sb.append("\n");
        sb.append("  -? or -h or --help\n");
        sb.append("     Prints this usage help.\n");
        sb.append("\n");
        exit(sb.toString(), 0);
    }

    /////////////////////////////////////////////////////////////////////////
    // Main
    /////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH); // Force usage of english locale
        SystemUtils.init3rdPartyLibs(PConvertMain.class);
        new PConvertMain(args).run();
    }

    /////////////////////////////////////////////////////////////////////////
    // Constructor parses the arguments
    /////////////////////////////////////////////////////////////////////////

    public PConvertMain(String[] args) {

        _formatExt = DEFAULT_FORMAT_EXT;
        _rgbProfile = null;
        _colorPalette = null;
        _histogramMatching = ImageInfo.HISTOGRAM_MATCHING_OFF;
        _noDataColor = null;

        String bandIndicesStr = null;
        String histoSkipPercentStr = null;
        List<File> fileList = new LinkedList<File>();
        String maxResStr = null;
        String forcedWidthStr = null;
        String forcedHeightStr = null;
        String noDataColorStr = null;

        /////////////////////////////////////////////////////////////////////////
        // Parse command line

        for (int i = 0; i < args.length; i++) {
            if (isOption(args, i, 'b', "bands")) {
                bandIndicesStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'c', "color-palette")) {
                _colorPalette = new File(getOptionArg(args, i));
                i++;
            } else if (isOption(args, i, 'd', "debug")) {
                Debug.setEnabled(true);
            } else if (isOption(args, i, 'f', "format")) {
                _formatExt = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'h', "help")
                    || isOption(args, i, '?', "help")) {
                printUsage();
            } else if (isOption(args, i, 'H', "height")) {
                forcedHeightStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'm', "histo-match")) {
                _histogramMatching = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'n', "no-data-color")) {
                noDataColorStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'o', "outdir")) {
                _outputDir = new File(getOptionArg(args, i));
                i++;
            } else if (isOption(args, i, 'p', "rgb-profile")) {
                _rgbProfile = new File(getOptionArg(args, i));
                i++;
            } else if (isOption(args, i, 'r', "max-res")) {
                maxResStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 's', "histo-skip")) {
                histoSkipPercentStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i, 'W', "width")) {
                forcedWidthStr = getOptionArg(args, i);
                i++;
            } else if (isOption(args, i)) {
                error("unknown option '" + args[i] + "'");
            } else {
                fileList.add(new File(args[i]));
            }
        }

        /////////////////////////////////////////////////////////////////////////
        // Get input files

        _inputFiles = new File[fileList.size()];
        fileList.toArray(_inputFiles);
        if (_inputFiles.length == 0) {
            printUsage();
        }

        /////////////////////////////////////////////////////////////////////////
        // Check output directory

        if (_outputDir != null && !_outputDir.exists()) {
            error("output directory not found: " + _outputDir.getAbsolutePath());
        }

        if (_rgbProfile != null && !_rgbProfile.exists()) {
            error("RGB channels file not found");
        }
        if (_colorPalette != null && !_colorPalette.exists()) {
            error("Color palette definition file not found");
        }

        /////////////////////////////////////////////////////////////////////////
        // Get output format

        _formatName = null;
        _imageFormat = true;
        if (_formatExt.equalsIgnoreCase("dim")) {
            _formatName = DimapProductWriterPlugIn.DIMAP_FORMAT_NAME;
            _imageFormat = false;
        } else if (_formatExt.equalsIgnoreCase("h5")) {
            _formatName = "HDF5";
            _imageFormat = false;
        } else if (_formatExt.equalsIgnoreCase("tifp")) {
            _formatExt = "tif";
            _formatName = "GeoTIFF";
            _imageFormat = false;
        } else if (_formatExt.equalsIgnoreCase("bmp")) {
            _formatName = "BMP";
            _imageFormat = true;
        } else if (_formatExt.equalsIgnoreCase("jpg")) {
            _formatName = "JPEG";
            _imageFormat = true;
        } else if (_formatExt.equalsIgnoreCase("png")) {
            _formatName = "PNG";
            _imageFormat = true;
        } else if (_formatExt.equalsIgnoreCase("tif")) {
            _formatName = "TIFF";
            _imageFormat = true;
        } else {
            String[] extensions = ProductIO.getProductWriterExtensions(_formatExt);
            if (extensions != null && extensions.length > 0) {
                _formatName = _formatExt;
                _formatExt = extensions[0].substring(1); // strip leading DOT off
                _imageFormat = false;
            }
        }
        if (_formatName == null) {
            error("unknown output format '" + _formatExt + "'");
        }

        /////////////////////////////////////////////////////////////////////////
        // Get band indices

        if (bandIndicesStr != null) {
            try {
                _bandIndices = StringUtils.toIntArray(bandIndicesStr, ",");
                if (_imageFormat && !(_bandIndices.length == 1 || _bandIndices.length == 3)) {
                    error("invalid number of image band indices in '" + bandIndicesStr + "'");
                }
            } catch (IllegalArgumentException e) {
                error("invalid band index in '" + bandIndicesStr + "'");
            }
        }
        if (noDataColorStr != null) {
            try {
                _noDataColor = StringUtils.parseColor(noDataColorStr);
            } catch (Exception e) {
                error("invalid no-data-color in '" + noDataColorStr + "'");
            }
        }
        if (_rgbProfile != null && !_imageFormat) {
            error("RGB-profile is only valid for image output");
        }
        if (_rgbProfile != null && _colorPalette != null) {
            error("RGB-profile and color palette definition can be specified at the same time");
        }
        if (_rgbProfile != null && _bandIndices != null) {
            error("RGB-profile and band indices cannot be given at the same time");
        }
        if (_colorPalette != null && !_imageFormat) {
            error("Color palette definition is only valid for image output");
        }
        if (_colorPalette != null && (_bandIndices != null && _bandIndices.length != 1)) {
            error("Color palette definition can only be applied on single bands. Select only one with the -b option");
        }
        if (_colorPalette != null && _bandIndices == null) {
            _bandIndices = new int[]{1};
        }
        if (_imageFormat && _bandIndices == null && _rgbProfile == null) {
            _bandIndices = _imageFormat ? DEFAULT_RGB_BAND_INDICES : null;
        }
        if (_bandIndices != null) {
            for (int i = 0; i < _bandIndices.length; i++) {
                _bandIndices[i]--;
                if (_bandIndices[i] < 0) {
                    error("invalid " + i + ". band index in '" + bandIndicesStr + "'");
                }
            }
        }
        if (!_imageFormat && _noDataColor != null) {
            error("No-Data colour is only valid for image output");
        }

        /////////////////////////////////////////////////////////////////////////
        // Get histogram lower/upper skip rations for contrast stretch

        _histoSkipRatios = _imageFormat ? DEFAULT_HISTO_SKIP_PERCENTAGE : null;
        if (histoSkipPercentStr != null) {
            try {
                _histoSkipRatios = StringUtils.toDoubleArray(histoSkipPercentStr, ",");
                if (_histoSkipRatios.length != 2) {
                    error("invalid contrast stretch range '" + histoSkipPercentStr + "'");
                }
            } catch (IllegalArgumentException e) {
                error("invalid contrast stretch range '" + histoSkipPercentStr + "'");
            }
        }
        if (_histoSkipRatios != null) {
            for (int i = 0; i < _histoSkipRatios.length; i++) {
                _histoSkipRatios[i] /= 100.0F;
                if (_histoSkipRatios[i] < 0.0F || _histoSkipRatios[i] > 1.0F) {
                    error("invalid contrast stretch range '" + histoSkipPercentStr + "'");
                }
            }
            if (_histoSkipRatios[0] >= 1.0 - _histoSkipRatios[1]) {
                error("invalid contrast stretch range '" + histoSkipPercentStr + "'");
            }
            if (_histoSkipRatios[1] >= 1.0 - _histoSkipRatios[0]) {
                error("invalid contrast stretch range '" + histoSkipPercentStr + "'");
            }
        }

        /////////////////////////////////////////////////////////////////////////
        // Get histogram lower/upper skip rations for contrast stretch

        if (!(ImageInfo.HISTOGRAM_MATCHING_OFF.equals(_histogramMatching) ||
                ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equals(_histogramMatching) ||
                ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equals(_histogramMatching))) {
            error("invalid histogram matching '" + _histogramMatching + "'");
        }

        /////////////////////////////////////////////////////////////////////////
        // Get maximum resolution in pixels

        _maxOutputResolution = null;
        if (maxResStr != null) {
            try {
                _maxOutputResolution = StringUtils.toIntArray(maxResStr, ",");
                if (_maxOutputResolution.length != 2) {
                    error("invalid maximum resolution '" + maxResStr + "'");
                }
            } catch (IllegalArgumentException e) {
                error("invalid maximum resolution '" + maxResStr + "'");
            }
        }

        /////////////////////////////////////////////////////////////////////////
        // Get forced width and height

        _forcedWidth = null;
        if (forcedWidthStr != null) {
            try {
                _forcedWidth = new Integer(forcedWidthStr);
                if (_forcedWidth <= 1) {
                    error("invalid forced image width '" + forcedWidthStr + "'");
                }
            } catch (IllegalArgumentException e) {
                error("invalid forced image width '" + forcedWidthStr + "'");
            }
        }

        _forcedHeight = null;
        if (forcedHeightStr != null) {
            try {
                _forcedHeight = new Integer(forcedHeightStr);
                if (_forcedHeight <= 1) {
                    error("invalid forced image height '" + forcedHeightStr + "'");
                }
            } catch (IllegalArgumentException e) {
                error("invalid forced image height '" + forcedHeightStr + "'");
            }
        }

        int n = 0;
        n += _maxOutputResolution != null ? 1 : 0;
        n += _forcedWidth != null ? 1 : 0;
        n += _forcedHeight != null ? 1 : 0;
        if (n > 1) {
            error("only one of maximum image resolution, forced image width and height can be specified");
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Run
    /////////////////////////////////////////////////////////////////////////

    public void run() {

        /////////////////////////////////////////////////////////////////////////
        // Convert all input products

        for (File inputFile : _inputFiles) {

            Product product = null;
            try {
                log("reading file " + inputFile.getPath());
                product = ProductIO.readProduct(inputFile);
            } catch (IOException e) {
                error("I/O error while reading input product: " + e.getMessage());
                Debug.trace(e);
            }

            if (product != null) {
                try {
                    String filename = FileUtils.createValidFilename(product.getName());
                    File outputFile = new File(_outputDir, filename);
                    outputFile = FileUtils.exchangeExtension(outputFile, "." + _formatExt);
                    if (_imageFormat) {
                        convertToImage(product, outputFile);
                    } else {
                        product = convertToProduct(product, outputFile);
                    }
                } catch (IOException e) {
                    error("I/O error while writing output file: " + e.getMessage());
                    Debug.trace(e);
                } finally {
                    product.dispose();
                }
            } else {
                warn("no appropriate reader found for input file format");
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Convert to Image
    /////////////////////////////////////////////////////////////////////////

    private void convertToImage(Product product, File outputFile) throws IOException {
        assert product != null;
        assert outputFile != null;

        RenderedImage image = null;

        // maybe load RGB profile
        if (_rgbProfile != null) {
            try {
                // to replace getAbsolutPath() replaced by getPath()?
                log("loading RGB profile from '" + _rgbProfile.getAbsolutePath() + "'...");
                final RGBImageProfile rgbImageProfile = RGBImageProfile.loadProfile(_rgbProfile);
                _bandIndices = createRGBBands(product, rgbImageProfile);
            } catch (IOException e) {
                error("failed to load RGB profile: " + e.getMessage());
            }
        }

        //maybe load Color Palette Def
        ColorPaletteDef colorPaletteDef = null;
        if (_colorPalette != null) {
            try {
                log("loading colour palette from: " + _colorPalette.getAbsolutePath());
                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(_colorPalette);
            } catch (IOException e) {
                error("failed to load colour palette: " + e.getMessage());
            }
        }

        // ensure that bands are valid and all have image display info assigned
        product = createProductSubset(product, _maxOutputResolution, null, outputFile);
        for (int i = 0; i < _bandIndices.length; i++) {
            final int index = _bandIndices[i];
            if (index < 0 || index >= product.getNumBands()) {
                error("invalid RGB band index: " + (i + 1));
            }
            final Band band = product.getBandAt(index);
            log("creating histogram for band '" + band.getName() + "'...");
            final ImageInfo imageInfo = band.createDefaultImageInfo(_histoSkipRatios, ProgressMonitor.NULL);
            band.setImageInfo(imageInfo);
            if (colorPaletteDef != null) {
                if (band.getIndexCoding() != null) {
                    band.getImageInfo().setColors(colorPaletteDef.getColors());
                } else {
                    Stx stx = band.getStx();
                    band.getImageInfo().setColorPaletteDef(colorPaletteDef,
                                                           stx.getMinimum(),
                                                           stx.getMaximum(), false);
                }
            }
            if (_noDataColor != null) {
                band.getImageInfo().setNoDataColor(_noDataColor);
            }
        }

        // create image
        try {
            log("creating RGB image...");
            final Band[] bands = new Band[_bandIndices.length];
            for (int i = 0; i < bands.length; i++) {
                bands[i] = product.getBandAt(_bandIndices[i]);
            }

            ImageInfo imageInfo = ProductUtils.createImageInfo(bands, true, ProgressMonitor.NULL);
            if (imageInfo.getNoDataColor().getAlpha() < 255 && "BMP".equalsIgnoreCase(_formatName)) {
                if (_noDataColor != null) {
                    imageInfo.setNoDataColor(_noDataColor);
                } else {
                    imageInfo.setNoDataColor(Color.BLACK);
                }
            }
            imageInfo.setHistogramMatching(ImageInfo.getHistogramMatching(_histogramMatching));
            image = ProductUtils.createRgbImage(bands, imageInfo, ProgressMonitor.NULL);
            if (image.getColorModel().hasAlpha() && "BMP".equalsIgnoreCase(_formatName)) {
                error("failed to write image: BMP does not support transparency");
                return;
            }
        } catch (Exception e) {
            Debug.trace(e);
            error("failed to create image: " + e.getMessage());
        }

        // force requested image size
        image = createScaledImage(image, _forcedWidth, _forcedHeight);

        // write image file using JAI's filestore operation
        try {
            boolean geoTIFFWritten = false;
            if (_formatName.equals("TIFF")) {
                // todo - IMPORTANT NOTE: if image has been scaled, geo-coding info is wrong for GeoTIFF!!!
                geoTIFFWritten = writeGeoTIFFImage(product, image, outputFile);
            }
            if (!geoTIFFWritten) {
                if ("JPG".equalsIgnoreCase(_formatExt)) {
                    image = BandSelectDescriptor.create(image, new int[]{0, 1, 2}, null);
                }
                writePlainImage(image, outputFile);
            }
        } catch (Exception e) {
            Debug.trace(e);
            error("failed to write image: " + e.getMessage());
        }
    }

    private void writePlainImage(RenderedImage image, File outputFile) {
        log("writing RGB image to '" + outputFile + "'...");
        JAI.create("filestore", image, outputFile.getPath(), _formatName, null);
    }

    private boolean writeGeoTIFFImage(Product product, RenderedImage image, File outputFile) throws IOException {
        boolean geoTIFFWritten = false;
        final GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(product);
        if (metadata != null) {
            log("writing RGB GeoTIFF image to '" + outputFile + "'...");
            GeoTIFF.writeImage(image, outputFile, metadata);
            geoTIFFWritten = true;
        }
        return geoTIFFWritten;
    }

    private static int[] createRGBBands(final Product product, final RGBImageProfile rgbImageProfile) {
        return new int[]{
                getBandIndex(product, rgbImageProfile.getRedExpression(), "virtual_red"),
                getBandIndex(product, rgbImageProfile.getGreenExpression(), "virtual_green"),
                getBandIndex(product, rgbImageProfile.getBlueExpression(), "virtual_blue")
        };
    }

    private static int getBandIndex(Product product, String expression, String virtualBandName) {
        final int index;
        if (product.getBand(expression) != null) {
            index = product.getBandIndex(expression);
        } else {
            final VirtualBand virtualBand = new VirtualBand(virtualBandName,
                                                            ProductData.TYPE_FLOAT32,
                                                            product.getSceneRasterWidth(),
                                                            product.getSceneRasterHeight(),
                                                            expression);
            product.addBand(virtualBand);
            index = product.getBandIndex(virtualBand.getName());
        }
        return index;
    }

    // todo - move this to JAI utils
    public static RenderedImage createScaledImage(RenderedImage sourceImage, Integer forcedWidth,
                                                  Integer forcedHeight) {

        final int iw = sourceImage.getWidth();
        final int ih = sourceImage.getHeight();

        double scale = 1.0;
        if (forcedWidth != null && forcedWidth != iw) {
            scale = forcedWidth / (double) iw;
        } else if (forcedHeight != null && forcedHeight != ih) {
            scale = forcedHeight / (double) ih;
        }

        if (scale != 1.0) {
            return JAIUtils.createScaleOp(sourceImage, scale, scale, 0.0, 0.0,
                                          Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
        } else {
            return sourceImage;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Convert to Product
    /////////////////////////////////////////////////////////////////////////

    private Product convertToProduct(Product product, File outputFile) throws IOException {
        assert product != null;
        assert outputFile != null;

        product = createProductSubset(product, _maxOutputResolution, _bandIndices, outputFile);
        try {
            log("writing a data product of size " + product.getSceneRasterWidth() + " x " +
                        product.getSceneRasterHeight() + " pixels to '" +
                        outputFile.getPath() + "'...");
            ProductIO.writeProduct(product, outputFile, _formatName, false, ProgressMonitor.NULL);
        } catch (IOException e) {
            Debug.trace(e);
            error("failed to write product: " + e.getMessage());
        }
        return product;
    }

    /////////////////////////////////////////////////////////////////////////
    // Subset Helpers
    /////////////////////////////////////////////////////////////////////////

    private static Product createProductSubset(Product product, int[] maxOutputResolution, int[] bandIndices,
                                               File outputFile) throws IOException {
        if (maxOutputResolution != null || bandIndices != null) {
            ProductSubsetDef productSubsetDef = new ProductSubsetDef();
            if (maxOutputResolution != null) {
                final int w = product.getSceneRasterWidth();
                final int h = product.getSceneRasterHeight();
                int wMax = Math.min(w, maxOutputResolution[0]);
                int hMax = Math.min(h, maxOutputResolution[1]);
                int xStep = w >= wMax ? w / wMax : 1;
                int yStep = h >= hMax ? h / hMax : 1;
                int step = Math.max(xStep, yStep);
                productSubsetDef.setSubSampling(step, step);
            }
            if (bandIndices != null) {
                for (int i = 0; i < bandIndices.length; i++) {
                    final int index = bandIndices[i];
                    if (index >= 0 && index < product.getNumBands()) {
                        Band band = product.getBandAt(index);
                        productSubsetDef.addNodeName(band.getName());
                    } else {
                        warn("ignoring invalid " + i + ". band index for input product '" + outputFile.getPath() + "'");
                    }
                }
            }
            if (!productSubsetDef.isEntireProductSelected()) {
                product = product.createSubset(productSubsetDef, null, null);
                final String[] messages = ProductUtils.removeInvalidExpressions(product);
                for (String message : messages) {
                    warn(message);
                }
            }
        }
        return product;
    }

    /////////////////////////////////////////////////////////////////////////
    // Console output
    /////////////////////////////////////////////////////////////////////////


    private static void log(String message) {
        System.out.println(message);
    }

    private static void warn(String message) {
        System.err.println("warning: " + message);
    }

    private static void error(String message) {
        exit("error: " + message, 1);
    }

    private static void exit(String message, int exitCode) {
        System.err.println(message);
        System.exit(exitCode);
    }

    /////////////////////////////////////////////////////////////////////////
    // Command Line Argument Parsing
    /////////////////////////////////////////////////////////////////////////


    private static boolean isOption(String[] args, int i) {
        return args[i].startsWith("-");
    }

    private static boolean isOption(String[] args, int i, char shortID, String longID) {
        return args[i].equals("-" + shortID) || args[i].equals("--" + longID);
    }

    private static String getOptionArg(String[] args, int i) {
        if (i < args.length - 1 && !isOption(args, i + 1)) {
            return args[i + 1];
        } else {
            error("missing argument for option '" + args[i] + "'");
            return null;
        }
    }
}
