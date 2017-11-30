/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.placemark.PlacemarkIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProperty;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.TimeStampExtractor;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.esa.snap.core.util.kmz.KmlDocument;
import org.esa.snap.core.util.kmz.KmlPlacemark;
import org.esa.snap.core.util.kmz.KmzExporter;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.FormatStrategy;
import org.esa.snap.measurement.writer.MeasurementFactory;
import org.esa.snap.measurement.writer.MeasurementWriter;
import org.esa.snap.pixex.aggregators.AggregatorStrategy;
import org.esa.snap.pixex.aggregators.MaxAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MeanAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MedianAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MinAggregatorStrategy;
import org.esa.snap.pixex.output.AggregatingPixExMeasurementFactory;
import org.esa.snap.pixex.output.DefaultFormatStrategy;
import org.esa.snap.pixex.output.MatchupFormatStrategy;
import org.esa.snap.pixex.output.PixExMeasurementFactory;
import org.esa.snap.pixex.output.PixExProductRegistry;
import org.esa.snap.pixex.output.PixExRasterNamesFactory;
import org.esa.snap.pixex.output.ProductRegistry;
import org.esa.snap.pixex.output.ScatterPlotDecoratingStrategy;
import org.esa.snap.pixex.output.TargetWriterFactoryAndMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
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

import static java.lang.Math.*;

/**
 * This operator is used to extract pixels from given locations and source products.
 * It can also create sub-scenes containing all locations found in the source products and create
 * KMZ files which contain the locations found in a source product.
 *
 * @author Marco Peters, Thomas Storm, Sabine Embacher
 * @since BEAM 4.9
 */
@SuppressWarnings({"MismatchedReadAndWriteOfArray", "UnusedDeclaration"})
@OperatorMetadata(
        alias = "PixEx",
        category = "Raster",
        version = "1.3",
        authors = "Marco Peters, Thomas Storm, Norman Fomferra",
        copyright = "(c) 2011 by Brockmann Consult",
        description = "Extracts pixels from given locations and source products.",
        autoWriteDisabled = true)
public class PixExOp extends Operator {

    public static final String RECURSIVE_INDICATOR = "**";
    private static final String SUB_SCENES_DIR_NAME = "subScenes";
    public static final String NO_AGGREGATION = "no aggregation";
    public static final String MEAN_AGGREGATION = "mean";
    public static final String MIN_AGGREGATION = "min";
    public static final String MAX_AGGREGATION = "max";
    public static final String MEDIAN_AGGREGATION = "median";

    @SourceProducts(description = "The source products from which pixels shall be extracted.")
    private Product[] sourceProducts;

    @TargetProperty()
    private PixExMeasurementReader measurements;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    private String[] sourceProductPaths;

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

    @Parameter(description = "Path to a CSV-file containing geo-coordinates associated with measurements according" +
                             "to BEAM CSV format specification")
    private File matchupFile;

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

    @Parameter(
            description = "If the window size is larger than 1, this parameter describes by which method a single \n" +
                          "value shall be derived from the pixels.",
            defaultValue = NO_AGGREGATION,
            valueSet = {NO_AGGREGATION, MEAN_AGGREGATION, MIN_AGGREGATION, MAX_AGGREGATION, MEDIAN_AGGREGATION})
    private String aggregatorStrategyType;

    @Parameter(description = "If set to true, sub-scenes of the regions, where pixels are found, are exported.",
            defaultValue = "false")
    private boolean exportSubScenes;

    @Parameter(description = "An additional border around the region where pixels are found.", defaultValue = "0")
    private int subSceneBorderSize;

    @Parameter(description = "If set to true, a Google KMZ file will be created, which contains the coordinates " +
                             "where pixels are found.",
            defaultValue = "false")
    private boolean exportKmz;

    @Parameter(
            description = "If set to true, the sensing start and sensing stop should be extracted from the filename " +
                          "of each input product.",
            defaultValue = "false",
            label = "Extract time from product filename")
    private boolean extractTimeFromFilename;

    @Parameter(
            description = "Describes how a date/time section inside a product filename should be interpreted. " +
                          "E.G. yyyyMMdd_HHmmss",
            validator = TimeStampExtractor.DateInterpretationPatternValidator.class,
            defaultValue = "yyyyMMdd",
            label = "Date/Time pattern")
    private String dateInterpretationPattern;

    @Parameter(description = "Describes how the filename of a product should be interpreted.",
            validator = TimeStampExtractor.FilenameInterpretationPatternValidator.class,
            defaultValue = "*${startDate}*${endDate}*",
            label = "Time extraction pattern in filename")
    private String filenameInterpretationPattern;

    @Parameter(defaultValue = "false", description = "Determines if the original input measurements shall be " +
                                                     "included in the output.")
    private boolean includeOriginalInput;

    @Parameter(description = "Array of 2-tuples of variable names; " +
                             "for each of these tuples a scatter plot will be exported.", notNull = false,
            itemAlias = "variableCombination")
    private VariableCombination[] scatterPlotVariableCombinations;

    private List<Coordinate> coordinateList;
    private boolean isTargetProductInitialized;
    private int timeDelta;
    private int calendarField = -1;
    private MeasurementWriter measurementWriter;
    private File subScenesDir;
    private KmlDocument kmlDocument;
    private ArrayList<String> knownKmzPlacemarks;
    private TimeStampExtractor timeStampExtractor;
    private AggregatorStrategy aggregatorStrategy;
    private FormatStrategy formatStrategy;

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static Coordinate.OriginalValue[] getOriginalValues(SimpleFeature feature) {
        List<AttributeDescriptor> originalAttributeDescriptors = (List<AttributeDescriptor>) feature.getFeatureType().getUserData().get(
                "originalAttributeDescriptors");
        final Coordinate.OriginalValue[] originalValues;
        if (originalAttributeDescriptors == null) {
            originalValues = new Coordinate.OriginalValue[0];
        } else {
            originalValues = new Coordinate.OriginalValue[originalAttributeDescriptors.size()];
        }
        List<Object> attributes = (List<Object>) feature.getUserData().get("originalAttributes");
        for (int j = 0; j < originalValues.length; j++) {
            String value = "";
            if (attributes.get(j) != null) {
                value = attributes.get(j).toString();
            }
            originalValues[j] = new Coordinate.OriginalValue(originalAttributeDescriptors.get(j).getLocalName(),
                                                             value);
        }
        return originalValues;
    }

    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && (coordinates == null || coordinates.length == 0) && matchupFile == null) {
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
            knownKmzPlacemarks = new ArrayList<>();
        }

        if (extractTimeFromFilename) {
            timeStampExtractor = new TimeStampExtractor(dateInterpretationPattern, filenameInterpretationPattern);
        }

        initAggregatorStrategy();

        Set<File> sourceProductFileSet = getSourceProductFileSet(this.sourceProductPaths, getLogger());
        coordinateList = initCoordinateList();
        Measurement[] originalMeasurements = createOriginalMeasurements(coordinateList);
        parseTimeDelta(timeDifference);
        final PixExRasterNamesFactory rasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints,
                                                                                       exportMasks, aggregatorStrategy);

        final PixExProductRegistry productRegistry = new PixExProductRegistry(outputFilePrefix, outputDir);
        formatStrategy = initFormatStrategy(rasterNamesFactory, originalMeasurements,
                                            productRegistry);
        MeasurementFactory measurementFactory;
        if (aggregatorStrategy == null || windowSize == 1) {
            measurementFactory = new PixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                             productRegistry);
        } else {
            measurementFactory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                        productRegistry, aggregatorStrategy);
        }
        TargetWriterFactoryAndMap targetFactory = new TargetWriterFactoryAndMap(outputFilePrefix, outputDir);

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
                File outFile = new File(outputDir, outputFilePrefix + "_coordinates.kmz");
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
                    kmzExporter.export(kmlDocument, zos, ProgressMonitor.NULL);
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Problem writing KMZ file.", e);
                }
            }

            if (!measurementsFound) {
                getLogger().log(Level.WARNING, "No measurements extracted.");
            }

        } finally {
            measurementWriter.close();
        }

        measurements = new PixExMeasurementReader(outputDir);
    }

    @SuppressWarnings("unchecked")
    private Measurement[] createOriginalMeasurements(List<Coordinate> coordinateList) {
        if (!includeOriginalInput &&
            (scatterPlotVariableCombinations == null || scatterPlotVariableCombinations.length == 0)) {
            return null;
        }
        Measurement[] result = new Measurement[coordinateList.size()];
        for (int i = 0; i < coordinateList.size(); i++) {
            Coordinate coordinate = coordinateList.get(i);
            Coordinate.OriginalValue[] originalValues = coordinate.getOriginalValues();
            Object[] values = new Object[0];
            String[] originalVariableNames = new String[0];
            if (originalValues != null) {
                values = new Object[originalValues.length];
                originalVariableNames = new String[originalValues.length];
                for (int valueIndex = 0; valueIndex < originalValues.length; valueIndex++) {
                    final Coordinate.OriginalValue originalValue = originalValues[valueIndex];
                    values[valueIndex] = originalValue.value;
                    originalVariableNames[valueIndex] = originalValue.variableName;
                }
            }
            result[i] = new Measurement(coordinate.getID(), "", -1, -1, -1, null,
                                        new GeoPos(coordinate.getLat(), coordinate.getLon()),
                                        values, originalVariableNames, true);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object[] getAttributeValues(SimpleFeature feature) {
        List<Object> attributes = (List<Object>) feature.getUserData().get("originalAttributes");
        Object[] values = new Object[attributes.size()];
        values[0] = feature.getID();
        for (int i1 = 1; i1 < values.length; i1++) {
            values[i1] = attributes.get(i1);
        }
        return values;
    }

    private FormatStrategy initFormatStrategy(PixExRasterNamesFactory rasterNamesFactory,
                                              Measurement[] originalMeasurements, ProductRegistry productRegistry) {
        FormatStrategy decoratedStrategy;
        if (includeOriginalInput) {
            decoratedStrategy = new MatchupFormatStrategy(originalMeasurements, rasterNamesFactory, windowSize,
                                                          expression, exportExpressionResult);
        } else {
            decoratedStrategy = new DefaultFormatStrategy(rasterNamesFactory, windowSize, expression,
                                                          exportExpressionResult);
        }
        if (scatterPlotVariableCombinations != null && scatterPlotVariableCombinations.length != 0) {
            return new ScatterPlotDecoratingStrategy(originalMeasurements, decoratedStrategy,
                                                     scatterPlotVariableCombinations, rasterNamesFactory,
                                                     productRegistry, outputDir, outputFilePrefix);
        }
        return decoratedStrategy;
    }

    private void initAggregatorStrategy() {
        if (windowSize == 1) {
            aggregatorStrategy = null;
            return;
        }
        switch (aggregatorStrategyType) {
            case MEAN_AGGREGATION:
                aggregatorStrategy = new MeanAggregatorStrategy();
                break;
            case MIN_AGGREGATION:
                aggregatorStrategy = new MinAggregatorStrategy();
                break;
            case MAX_AGGREGATION:
                aggregatorStrategy = new MaxAggregatorStrategy();
                break;
            case MEDIAN_AGGREGATION:
                aggregatorStrategy = new MedianAggregatorStrategy();
                break;
            case NO_AGGREGATION:
                aggregatorStrategy = null;
                break;
        }
    }

    public static Set<File> getSourceProductFileSet(String[] sourceProductPaths, Logger logger) {
        Set<File> sourceProductFileSet = new TreeSet<>();
        String[] paths = trimSourceProductPaths(sourceProductPaths);
        if (paths != null && paths.length != 0) {
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
        if (considerTimeDifference(timeDifference) && coordinate.getDateTime() != null) {
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
        boolean isAnyPixelValid = isAnyPixelInWindowValid(upperLeftX, upperLeftY, validData);
        if (isAnyPixelValid) {
            measurementWriter.writeMeasurements(centerX, centerY, coordinateID, coordinate.getName(), product,
                                                validData);
            return true;
        }
        return false;
    }

    private PixelPos getPixelPosition(Product product, Coordinate coordinate) {
        return product.getSceneGeoCoding().getPixelPos(new GeoPos(coordinate.getLat(), coordinate.getLon()), null);
    }

    private boolean isAnyPixelInWindowValid(int upperLeftX, int upperLeftY, Raster validData) {
        final int numPixels = windowSize * windowSize;
        for (int n = 0; n < numPixels; n++) {
            int x = upperLeftX + n % windowSize;
            int y = upperLeftY + n / windowSize;
            final boolean isPixelValid = validData.getSample(x, y, 0) != 0;
            if (isPixelValid) {
                return true;
            }
        }
        return false;
    }

    PlanarImage createValidMaskImage(Product product) {
        if (expression != null && product.isCompatibleBandArithmeticExpression(expression)) {
            return VirtualBandOpImage.builder(expression, product)
                    .dataType(ProductData.TYPE_UINT8)
                    .fillValue(0)
                    .create();
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

    private void parseTimeDelta(String timeDifference) {
        if (!considerTimeDifference(timeDifference)) {
            return;
        }
        this.timeDelta = Integer.parseInt(timeDifference.substring(0, timeDifference.length() - 1));
        final String s = timeDifference.substring(timeDifference.length() - 1).toUpperCase();
        switch (s) {
            case "D":
                calendarField = Calendar.DATE;
                break;
            case "H":
                calendarField = Calendar.HOUR;
                break;
            case "M":
                calendarField = Calendar.MINUTE;
                break;
            default:
                calendarField = Calendar.DATE;
                break;
        }
    }

    private boolean considerTimeDifference(String timeDifference) {
        return !StringUtils.isNullOrEmpty(timeDifference);
    }

    private List<Coordinate> initCoordinateList() {
        List<Coordinate> list = new ArrayList<>();
        if (coordinatesFile != null) {
            list.addAll(extractCoordinates(coordinatesFile));
        }
        if (coordinates != null) {
            list.addAll(Arrays.asList(coordinates));
        }
        if (matchupFile != null) {
            list.addAll(extractMatchupCoordinates(matchupFile));
        }
        for (int i = 0; i < list.size(); i++) {
            final Coordinate coordinate = list.get(i);
            coordinate.setID(i + 1);
        }
        return list;
    }

    static List<Coordinate> extractMatchupCoordinates(File matchupFile) {
        final List<Coordinate> result = new ArrayList<>();
        List<SimpleFeature> simpleFeatures;
        try {
            simpleFeatures = PixExOpUtils.extractFeatures(matchupFile);
        } catch (IOException e) {
            SystemUtils.LOG.warning(
                    String.format("Unable to read matchups from file '%s'. Reason: %s",
                                  matchupFile.getAbsolutePath(), e.getMessage()));
            return result;
        }
        for (SimpleFeature extendedFeature : simpleFeatures) {
            try {
                final Coordinate.OriginalValue[] originalValues = getOriginalValues(extendedFeature);
                final GeoPos geoPos;
                geoPos = PixExOpUtils.getGeoPos(extendedFeature);
                final Date dateTime = (Date) extendedFeature.getAttribute(Placemark.PROPERTY_NAME_DATETIME);
                result.add(new Coordinate(extendedFeature.getID(), geoPos.lat, geoPos.lon, dateTime, originalValues));
            } catch (IOException e) {
                SystemUtils.LOG.warning(e.getMessage());
            }
        }
        return result;
    }

    private List<Coordinate> extractCoordinates(File coordinatesFile) {
        final List<Coordinate> extractedCoordinates = new ArrayList<>();
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
            measurementsFound |= extractMeasurements(file);
        }
        return measurementsFound;
    }

    private boolean extractMeasurements(File file) {
        try {
            final Product product = ProductIO.readProduct(file);
            if (product == null) {
                getLogger().warning("Unable to read product from file '" + file.getAbsolutePath() + "'.");
                return false;
            }
            try {
                return extractMeasurements(product);
            } finally {
                product.dispose();
            }
        } catch (Exception e) {
            final Logger logger = getLogger();
            logger.warning("Unable to extract measurements from product file '" + file.getAbsolutePath() + "'.");
            logger.log(Level.WARNING, e.getMessage());
            logger.log(Level.FINER, e.getMessage(), e);
        }
        return false;
    }

    private boolean extractMeasurements(Product product) {

        if (!isAbleToExtractPixels(product)) {
            return false;
        }

        ProductData.UTC[] oldTimeStamps = new ProductData.UTC[2];
        oldTimeStamps[0] = product.getStartTime();
        oldTimeStamps[1] = product.getEndTime();
        try {
            File file = product.getFileLocation();
            if (extractTimeFromFilename && file != null) {
                String fileName = file.getName();
                final ProductData.UTC[] timeStamps = timeStampExtractor.extractTimeStamps(fileName);
                product.setStartTime(timeStamps[0]);
                product.setEndTime(timeStamps[1]);
            }
        } catch (ValidationException e) {
            throw new OperatorException(e);
        }

        final PlanarImage validMaskImage = createValidMaskImage(product);
        try {
            List<Coordinate> matchedCoordinates = new ArrayList<>();

            boolean coordinatesFound = false;
            for (Coordinate coordinate : coordinateList) {
                try {
                    final boolean measurementExtracted = extractMeasurement(product, coordinate, coordinate.getID(),
                                                                            validMaskImage);
                    coordinatesFound |= measurementExtracted;
                    if (measurementExtracted && (exportSubScenes || exportKmz)) {
                        matchedCoordinates.add(coordinate);
                    }
                } catch (IOException e) {
                    getLogger().warning(e.getMessage());
                }
            }
            formatStrategy.finish();
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
                            final Point2D.Double position = new Point2D.Double(matchedCoordinate.getLon(),
                                                                               matchedCoordinate.getLat());
                            kmlDocument.addChild(new KmlPlacemark(coordinateName, null, position));
                            knownKmzPlacemarks.add(coordinateName);
                        }

                    }
                }
            }
            return coordinatesFound;
        } finally {
            validMaskImage.dispose();
            product.setStartTime(oldTimeStamps[0]);
            product.setEndTime(oldTimeStamps[1]);
        }
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

    private static class ProductComparator implements Comparator<Product> {

        @Override
        public int compare(Product p1, Product p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

    private boolean isAbleToExtractPixels(Product product) {
        final Logger logger = getLogger();
        if (product == null) {
            return false;
        }
        if (product.isMultiSize()) {
            final String msgPattern = "Product [%s] refused. Cause: Product has rasters of different size. " +
                                      "Please consider resampling it so that all rasters have the same size.";
            logger.warning(String.format(msgPattern, product.getFileLocation()));
            return false;
        }
        final GeoCoding geoCoding = product.getSceneGeoCoding();
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

    private static String[] trimSourceProductPaths(String[] sourceProductPaths) {
        final String[] paths;
        if (sourceProductPaths != null) {
            paths = sourceProductPaths.clone();
        } else {
            paths = null;
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

    public static class VariableCombination {

        public VariableCombination() {
        }

        @Parameter(description = "The name of the variable from the original measurements")
        public String originalVariableName;

        @Parameter(description = "The name of the variable from the product")
        public String productVariableName;

    }
}
