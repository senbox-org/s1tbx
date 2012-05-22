/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pixex;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProperty;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.MeasurementWriter;
import org.esa.beam.pixex.output.PixExFormatStrategy;
import org.esa.beam.pixex.output.PixExMeasurementFactory;
import org.esa.beam.pixex.output.PixExProductRegistry;
import org.esa.beam.pixex.output.PixExRasterNamesFactory;
import org.esa.beam.pixex.output.PixExTargetFactory;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.esa.beam.util.kmz.KmlDocument;
import org.esa.beam.util.kmz.KmlPlacemark;
import org.esa.beam.util.kmz.KmzExporter;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This operator is used to extract pixels from given locations and source products.
 * It can also create sub-scenes containing all locations found in the source products and create
 * KMZ files which contain the locations found in a source product
 *
 * @author Marco Peters, Thomas Storm, Sabine Embacher
 * @since BEAM 4.9
 */
@SuppressWarnings({"MismatchedReadAndWriteOfArray", "UnusedDeclaration"})
@OperatorMetadata(
        alias = "PixEx",
        version = "1.0.1",
        authors = "Marco Peters, Thomas Storm, Norman Fomferra",
        copyright = "(c) 2011 by Brockmann Consult",
        description = "Extracts pixels from given locations and source products.")
public class PixExOp extends Operator implements Output {

    public static final String RECURSIVE_INDICATOR = "**";
    private static final String SUB_SCENES_DIR_NAME = "subScenes";

    @SourceProducts()
    private Product[] sourceProducts;

    @TargetProperty()
    private PixExMeasurementReader measurements;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).")
    private String[] sourceProductPaths;

    @Parameter(
            description = "Deprecated since version 1.0, use parameter 'sourceProductPaths' instead.")
    @Deprecated
    private File[] inputPaths;

    @Parameter(description = "Specifies if bands are to be exported", defaultValue = "true")
    private Boolean exportBands;

    @Parameter(description = "Specifies if tie-points are to be exported", defaultValue = "true")
    private Boolean exportTiePoints;

    @Parameter(description = "Specifies if masks are to be exported", defaultValue = "true")
    private Boolean exportMasks;

    @Parameter(description = "The geo-coordinates", itemAlias = "coordinate")
    private Coordinate[] coordinates;

    @Parameter(description = "The acceptable time difference compared to the time given for a coordinate.\n" +
            "The format is a number followed by (D)ay, (H)our or (M)inute. If no time difference is provided, " +
            "all input products are considered regardless of their time.",
               defaultValue = "")
    private String timeDifference = "";

    @Parameter(description = "Path to a file containing geo-coordinates. BEAM's placemark files can be used.")
    private File coordinatesFile;

    @Parameter(description = "Side length of surrounding window (uneven)", defaultValue = "1",
               validator = WindowSizeValidator.class)
    private Integer windowSize;

    @Parameter(description = "The output directory.", notNull = true)
    private File outputDir;

    @Parameter(description = "The prefix is used to name the output files.", defaultValue = "pixEx")
    private String outputFilePrefix;

    @Parameter(description = "Band maths expression (optional). Defines valid pixels.")
    private String expression;

    @Parameter(description = "If true, the expression result is exported per pixel, otherwise the expression \n" +
            "is used as filter (all pixels in given window must be valid).",
               defaultValue = "true")
    private Boolean exportExpressionResult;

    @Parameter(description = "If set to true, sub-scenes of the regions, where pixels are found, are exported.",
               defaultValue = "false")
    private boolean exportSubScenes;

    @Parameter(description = "An additional border around the region where pixels are found.", defaultValue = "0")
    private int subSceneBorderSize;

    @Parameter(description = "If set to true, a Google KMZ file will be created, which contains the coordinates " +
            "where pixels are found.",
               defaultValue = "false")
    private boolean exportKmz;

    @Parameter(description = "If set to true, the sensing start and sensing stop should be extracted from the filename " +
            "of each input product.",
               defaultValue = "false",
               label = "Extract time from product filename")
    private boolean extractTimeFromFilename;

    @Parameter(description = "Describes how a date/time section inside a product filename should be interpreted. E.G. yyyyMMdd_hhmmss",
               validator = TimeStampExtractor.DateInterpretationPatternValidator.class,
               defaultValue = "yyyyMMdd",
               label = "Date/Time pattern")
    private String dateInterpretationPattern;

    @Parameter(description = "Describes how the filename of a product should be interpreted.",
               validator = TimeStampExtractor.FilenameInterpretationPatternValidator.class,
               defaultValue = "*${date}*${date}*",
               label = "Time extraction pattern in filename")
    private String filenameInterpretationPattern;

    private ProductValidator validator;
    private List<Coordinate> coordinateList;
    private boolean isTargetProductInitialized;
    private int timeDelta;
    private int calendarField = -1;
    private MeasurementWriter measurementWriter;
    private File subScenesDir;
    private KmlDocument kmlDocument;
    private ArrayList<String> knownKmzPlacemarks;
    private TimeStampExtractor timeStampExtractor;

    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && (coordinates == null || coordinates.length == 0)) {
            throw new OperatorException("No coordinates specified.");
        }
        if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
            throw new OperatorException("Output directory does not exist and could not be created.");
        }
        if (exportSubScenes) {
            subScenesDir = new File(outputDir, SUB_SCENES_DIR_NAME);
            if (!subScenesDir.exists() && !subScenesDir.mkdirs()) {
                throw new OperatorException("Directory for sub-scenes does not exist and could not be created.");
            }
        }
        if (exportKmz) {
            kmlDocument = new KmlDocument("placemarks", null);
            knownKmzPlacemarks = new ArrayList<String>();
        }

        if (extractTimeFromFilename) {
            timeStampExtractor = new TimeStampExtractor(dateInterpretationPattern, filenameInterpretationPattern);
        }

        Set<File> sourceProductFileSet = getSourceProductFileSet(this.sourceProductPaths, this.inputPaths, getLogger());

        coordinateList = initCoordinateList();
        parseTimeDelta(timeDifference);

        validator = new ProductValidator();

        final PixExRasterNamesFactory rasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints,
                                                                                       exportMasks);
        final PixExFormatStrategy formatStrategy = new PixExFormatStrategy(rasterNamesFactory, windowSize, expression,
                                                                           exportExpressionResult);
        final PixExProductRegistry productRegistry = new PixExProductRegistry(outputFilePrefix, outputDir);
        PixExMeasurementFactory measurementFactory = new PixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                 productRegistry);
        PixExTargetFactory targetFactory = new PixExTargetFactory(outputFilePrefix, outputDir);

        measurementWriter = new MeasurementWriter(measurementFactory, targetFactory, formatStrategy);

        try {
            boolean measurementsFound = false;
            if (sourceProducts != null) {
                Arrays.sort(sourceProducts, new ProductComparator());
                for (Product product : sourceProducts) {
                    measurementsFound |= extractMeasurements(product);
                }
            }
            if (!sourceProductFileSet.isEmpty()) {
                measurementsFound |= extractMeasurements(sourceProductFileSet);
            }

            setDummyTargetProduct();

            if (exportKmz && measurementsFound) {
                KmzExporter kmzExporter = new KmzExporter();
                ZipOutputStream zos = null;
                try {
                    FileOutputStream fos = new FileOutputStream(
                            new File(outputDir, outputFilePrefix + "_coordinates.kmz"));
                    zos = new ZipOutputStream(fos);
                    kmzExporter.export(kmlDocument, zos, ProgressMonitor.NULL);
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Problem writing KMZ file.", e);
                } finally {
                    if (zos != null) {
                        try {
                            zos.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        } finally {
            measurementWriter.close();
        }

        measurements = new PixExMeasurementReader(outputDir);
    }

    public static Set<File> getSourceProductFileSet(String[] sourceProductPaths1, File[] inputPaths1, Logger logger) {
        Set<File> sourceProductFileSet = new TreeSet<File>();
        String[] paths = getSourceProductPaths(sourceProductPaths1, inputPaths1);
        if (paths != null) {
            for (String path : paths) {
                try {
                    WildcardMatcher.glob(path, sourceProductFileSet);
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                }
            }
            if (sourceProductFileSet.isEmpty()) {
                logger.log(Level.WARNING, "No valid source product path found.");
            }
        }
        return sourceProductFileSet;
    }

    @Override
    public void dispose() {
        try {
            measurements.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    int getTimeDelta() {
        return timeDelta;
    }

    int getCalendarField() {
        return calendarField;
    }

    Iterator<Measurement> getMeasurements() {
        return measurements;
    }

    private boolean extractMeasurement(Product product, Coordinate coordinate,
                                       int coordinateID, RenderedImage validMaskImage) throws IOException {
        PixelPos centerPos = getPixelPosition(product, coordinate);
        if (!product.containsPixel(centerPos)) {
            return false;
        }
        if (coordinate.getDateTime() != null) {
            final ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, centerPos.y);
            if (scanLineTime == null || !isPixelInTimeSpan(coordinate, timeDelta, calendarField, scanLineTime)) {
                return false;
            }
        }
        int offset = MathUtils.floorInt(windowSize / 2);
        final int centerX = MathUtils.floorInt(centerPos.x);
        final int centerY = MathUtils.floorInt(centerPos.y);
        final int upperLeftX = centerX - offset;
        final int upperLeftY = centerY - offset;
        final Raster validData = validMaskImage.getData(new Rectangle(upperLeftX, upperLeftY, windowSize, windowSize));
        boolean areAllPixelsValid = areAllPixelsInWindowValid(upperLeftX, upperLeftY, validData);
        if (areAllPixelsValid || exportExpressionResult) {
            measurementWriter.writeMeasurements(centerX, centerY, coordinateID, coordinate.getName(), product,
                                                validData);
            return true;
        }
        return false;
    }

    private PixelPos getPixelPosition(Product product, Coordinate coordinate) {
        return product.getGeoCoding().getPixelPos(new GeoPos(coordinate.getLat(), coordinate.getLon()), null);
    }

    private boolean areAllPixelsInWindowValid(int upperLeftX, int upperLeftY, Raster validData) {
        final int numPixels = windowSize * windowSize;
        for (int n = 0; n < numPixels; n++) {
            int x = upperLeftX + n % windowSize;
            int y = upperLeftY + n / windowSize;
            final boolean isPixelValid = validData.getSample(x, y, 0) != 0;
            if (!isPixelValid) {
                return false;
            }
        }
        return true;
    }


    PlanarImage createValidMaskImage(Product product) {
        if (expression != null && product.isCompatibleBandArithmeticExpression(expression)) {
            return VirtualBandOpImage.create(expression, ProductData.TYPE_UINT8, 0,
                                             product, ResolutionLevel.MAXRES);
        } else {
            return ConstantDescriptor.create((float) product.getSceneRasterWidth(),
                                             (float) product.getSceneRasterHeight(),
                                             new Byte[]{-1}, null);
        }
    }

    private boolean isPixelInTimeSpan(Coordinate coordinate, int timeDiff, int calendarField,
                                      ProductData.UTC timeAtPixel) {
        if (timeDifference.isEmpty()) {
            return true;
        }

        final Calendar currentDate = timeAtPixel.getAsCalendar();

        final Calendar lowerTimeBound = (Calendar) currentDate.clone();
        lowerTimeBound.add(calendarField, -timeDiff);
        final Calendar upperTimeBound = (Calendar) currentDate.clone();
        upperTimeBound.add(calendarField, timeDiff);

        Calendar coordinateCal = ProductData.UTC.createCalendar();
        coordinateCal.setTime(coordinate.getDateTime());

        return lowerTimeBound.compareTo(coordinateCal) <= 0 && upperTimeBound.compareTo(coordinateCal) >= 0;
    }

    private void parseTimeDelta(String timeDelta) {
        if (timeDifference.isEmpty()) {
            return;
        }
        this.timeDelta = Integer.parseInt(timeDelta.substring(0, timeDelta.length() - 1));
        final String s = timeDelta.substring(timeDelta.length() - 1).toUpperCase();
        if ("D".equals(s)) {
            calendarField = Calendar.DATE;
        } else if ("H".equals(s)) {
            calendarField = Calendar.HOUR;
        } else if ("M".equals(s)) {
            calendarField = Calendar.MINUTE;
        } else {
            calendarField = Calendar.DATE;
        }

    }

    private List<Coordinate> initCoordinateList() {
        List<Coordinate> list = new ArrayList<Coordinate>();
        if (coordinatesFile != null) {
            list.addAll(extractCoordinates(coordinatesFile));
        }
        if (coordinates != null) {
            list.addAll(Arrays.asList(coordinates));
        }
        return list;
    }

    private List<Coordinate> extractCoordinates(File coordinatesFile) {
        final List<Coordinate> extractedCoordinates = new ArrayList<Coordinate>();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(coordinatesFile);
            final List<Placemark> pins = PlacemarkIO.readPlacemarks(fileReader,
                                                                    null, // no GeoCoding needed
                                                                    PinDescriptor.getInstance());
            for (Placemark pin : pins) {
                final GeoPos geoPos = pin.getGeoPos();
                if (geoPos != null) {
                    final Date dateTimeValue = (Date) pin.getFeature().getAttribute(Placemark.PROPERTY_NAME_DATETIME);
                    final Coordinate coordinate = new Coordinate(pin.getName(), geoPos.lat, geoPos.lon, dateTimeValue);
                    extractedCoordinates.add(coordinate);
                }
            }
        } catch (IOException cause) {
            throw new OperatorException(cause);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return extractedCoordinates;
    }

    private boolean extractMeasurements(Set<File> fileSet) {
        boolean measurementsFound = false;
        for (File file : fileSet) {
            if (file.isDirectory()) {
                final File[] subFiles = file.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        if (subFile.isFile()) {
                            measurementsFound |= extractMeasurements(subFile);
                        }
                    }
                }
            } else {
                measurementsFound |= extractMeasurements(file);
            }
        }
        return measurementsFound;
    }

    private boolean extractMeasurements(File file) {
        Product product = null;
        try {
            product = ProductIO.readProduct(file);
            if (product == null) {
                getLogger().warning("Unable to read product from file '" + file.getAbsolutePath() + "'.");
                return false;
            }
            if (extractTimeFromFilename) {
                final ProductData.UTC[] timeStamps = timeStampExtractor.extractTimeStamps(file.getName());
                product.setStartTime(timeStamps[0]);
                product.setEndTime(timeStamps[1]);
            }
            return extractMeasurements(product);
        } catch (ValidationException e) {
            throw new OperatorException(e);
        } catch (IOException e) {
            getLogger().warning("Unable to read product from file '" + file.getAbsolutePath() + "'.");
        } finally {
            if (product != null) {
                product.dispose();
            }
        }
        return false;
    }

    private boolean extractMeasurements(Product product) {

        boolean coordinatesFound = false;
        if (!validator.validate(product)) {
            return coordinatesFound;
        }

        final PlanarImage validMaskImage = createValidMaskImage(product);
        try {
            List<Coordinate> matchedCoordinates = new ArrayList<Coordinate>();

            for (int i = 0; i < coordinateList.size(); i++) {
                Coordinate coordinate = coordinateList.get(i);
                try {
                    final boolean measurementExtracted = extractMeasurement(product, coordinate, i + 1, validMaskImage);
                    if (measurementExtracted && (exportSubScenes || exportKmz)) {
                        matchedCoordinates.add(coordinate);
                    }
                } catch (IOException e) {
                    getLogger().warning(e.getMessage());
                }
            }
            coordinatesFound = !matchedCoordinates.isEmpty();
            if (coordinatesFound) {
                if (exportSubScenes) {
                    try {
                        exportSubScene(product, matchedCoordinates);
                    } catch (IOException e) {
                        getLogger().log(Level.WARNING,
                                        "Could not export sub-scene for product: " + product.getFileLocation(), e);
                    }
                }
                if (exportKmz) {
                    for (Coordinate matchedCoordinate : matchedCoordinates) {
                        final String coordinateName = matchedCoordinate.getName();
                        if (!knownKmzPlacemarks.contains(coordinateName)) {
                            final Point2D.Float position = new Point2D.Float(matchedCoordinate.getLon(),
                                                                             matchedCoordinate.getLat());
                            kmlDocument.addChild(new KmlPlacemark(coordinateName, null, position));
                            knownKmzPlacemarks.add(coordinateName);
                        }

                    }
                }
            }
        } finally {
            validMaskImage.dispose();
        }
        return coordinatesFound;
    }

    private void exportSubScene(Product product, List<Coordinate> coordinates) throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef(product.getName() + "_subScene");

        int x1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE;
        int y1 = Integer.MAX_VALUE;
        int y2 = Integer.MIN_VALUE;
        int width = (windowSize - 1) / 2;
        for (Coordinate coordinate : coordinates) {
            final PixelPos pixelPos = getPixelPosition(product, coordinate);
            x1 = min(x1, (int) floor(pixelPos.x - width));
            x2 = max(x2, (int) floor(pixelPos.x + width));
            y1 = min(y1, (int) floor(pixelPos.y - width));
            y2 = max(y2, (int) floor(pixelPos.y + width));
        }
        Rectangle region = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
        region.grow(subSceneBorderSize, subSceneBorderSize);
        final Rectangle productBounds = new Rectangle(0, 0, product.getSceneRasterWidth(),
                                                      product.getSceneRasterHeight());
        Rectangle finalRegion = productBounds.intersection(region);
        subsetDef.setRegion(finalRegion);
        final Product subset = ProductSubsetBuilder.createProductSubset(product, subsetDef, null, null);
        final String[] extension = ProductIO.getProductWriterExtensions(ProductIO.DEFAULT_FORMAT_NAME);
        final File productFile = new File(subScenesDir, product.getName() + extension[0]);
        ProductIO.writeProduct(subset, productFile.getAbsolutePath(), ProductIO.DEFAULT_FORMAT_NAME);
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    private static class DirectoryFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    private static class ProductComparator implements Comparator<Product> {

        @Override
        public int compare(Product p1, Product p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

    private class ProductValidator {

        public boolean validate(Product product) {
            final Logger logger = getLogger();
            if (product == null) {
                return false;
            }
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding == null) {
                final String msgPattern = "Product [%s] refused. Cause: Product is not geo-coded.";
                logger.warning(String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            if (!geoCoding.canGetPixelPos()) {
                final String msgPattern = "Product [%s] refused. Cause: Pixel position can not be determined.";
                logger.warning(String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            return true;
        }
    }

    public static String[] getSourceProductPaths(String[] sourceProductPaths, File[] deprecatedInputPaths) {
        final String[] paths;
        if (deprecatedInputPaths != null) {
            if (sourceProductPaths != null) {
                paths = new String[deprecatedInputPaths.length + sourceProductPaths.length];
            } else {
                paths = new String[deprecatedInputPaths.length];
            }
            for (int i = 0; i < deprecatedInputPaths.length; i++) {
                paths[i] = deprecatedInputPaths[i].getPath();
            }
            if (sourceProductPaths != null) {
                System.arraycopy(sourceProductPaths, 0, paths, deprecatedInputPaths.length, sourceProductPaths.length);
            }
        } else {
            if (sourceProductPaths != null) {
                paths = sourceProductPaths.clone();
            } else {
                paths = null;
            }
        }
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].trim();
            }
        }
        return paths;
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PixExOp.class);
        }
    }

    public static class WindowSizeValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            if (((Integer) value) % 2 == 0) {
                throw new ValidationException("Value of 'windowSize' must be uneven");
            }
        }
    }
}