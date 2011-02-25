/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProperty;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({
                          "IOResourceOpenedButNotSafelyClosed", "MismatchedReadAndWriteOfArray",
                          "FieldCanBeLocal", "UnusedDeclaration"
                  })
@OperatorMetadata(
        alias = "PixEx",
        version = "1.0",
        authors = "Marco Peters, Thomas Storm",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Generates a CSV file from a given pixel location and source products.")
public class PixExOp extends Operator {

    public static final String RECURSIVE_INDICATOR = "**";

    @SourceProducts()
    private Product[] sourceProducts;

    @TargetProperty()
    private Map<String, List<Measurement>> measurements;

    @Parameter(
            description = "The paths to be scanned for input products. May point to a single file or a directory.\n" +
                          "If path ends with '**' the directory is scanned recursively.")
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
                             "The format is a number followed by (D)ay, (H)our or (M)inute.",
               defaultValue = "1D")
    private String timeDifference;

    @Parameter(description = "Path to a file containing geo-coordinates. BEAM's placemark files can be used.")
    private File coordinatesFile;

    @Parameter(description = "Side length of surrounding window (uneven)", defaultValue = "1",
               validator = WindowSizeValidator.class)
    private Integer windowSize;


    @Parameter(description = "The output directory. If not specified the output is written to std.out.")
    private File outputDir;

    @Parameter(description = "The prefix is used to name the output files.", defaultValue = "pixEx")
    private String outputFilePrefix;

    @Parameter(description = "Band maths expression (optional)")
    private String expression;

    @Parameter(description = "If true, the expression result is exported, otherwise the expression is used as filter.",
               defaultValue = "true")
    private Boolean exportExpressionResult;

    private Map<String, String[]> rasterNamesMap = new HashMap<String, String[]>(37);
    private ProductValidator validator;
    private List<Coordinate> coordinateList;
    private boolean isTargetProductInitialized = false;
    private List<ProductDescription> productLocationList = new ArrayList<ProductDescription>();
    private Integer productId = 0;
    private int timeDelta = 0;
    private int calendarField = -1;


    int getTimeDelta() {
        return timeDelta;
    }

    int getCalendarField() {
        return calendarField;
    }

    Map<String, List<Measurement>> getMeasurements() {
        return measurements;
    }

    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && coordinates == null) {
            throw new OperatorException("No coordinates specified.");
        }
        if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
            throw new OperatorException("Output directory does not exist and could not be created.");
        }
        coordinateList = initCoordinateList();
        parseTimeDelta(timeDifference);

        validator = new ProductValidator();
        measurements = new HashMap<String, List<Measurement>>();
        if (sourceProducts != null) {
            for (Product product : sourceProducts) {
                extractMeasurements(product);
            }
        }
        if (inputPaths != null) {
            inputPaths = getParsedInputPaths(inputPaths);
            if (inputPaths.length == 0) {
                getLogger().log(Level.WARNING, "No valid input path found.");
            }
            extractMeasurements(inputPaths);
        }
        try {
            if (outputDir != null) {
                writeOutputToFile();
            }
            StringWriter writer = new StringWriter();
            writeOutputToWriter(writer);
            if (outputDir == null) {
                System.out.print(writer.toString());
            }
        } catch (IOException e) {
            throw new OperatorException("Could not write output.", e);
        }

        if (!isTargetProductInitialized) {
            setDummyProduct();
        }
    }

    void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }

    void setRasterNamesMap(Map<String, String[]> rasterNamesMap) {
        this.rasterNamesMap = rasterNamesMap;
    }

    void readMeasurement(Product product, Coordinate coordinate, int coordinateID,
                         Map<String, List<Measurement>> measurements, RenderedImage validMaskImage) throws IOException {
        PixelPos centerPos = product.getGeoCoding().getPixelPos(new GeoPos(coordinate.getLat(), coordinate.getLon()),
                                                                null);
        if (!product.containsPixel(centerPos)) {
            return;
        }
        final ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, centerPos.y);
        if (coordinate.getDateTime() != null) {
            if (scanLineTime == null || !isPixelInTimeSpan(coordinate, timeDelta, calendarField, scanLineTime)) {
                return;
            }
        }

        String productType = product.getProductType();
        int id = getProductId(product);
        String[] rasterNames = rasterNamesMap.get(productType);
        int offset = MathUtils.floorInt(windowSize / 2);
        int upperLeftX = MathUtils.floorInt(centerPos.x - offset);
        int upperLeftY = MathUtils.floorInt(centerPos.y - offset);
        final Number[] values = new Number[rasterNames.length];
        Arrays.fill(values, Double.NaN);
        final int numPixels = windowSize * windowSize;
        final Raster validData = validMaskImage.getData(new Rectangle(upperLeftX, upperLeftY, windowSize, windowSize));
        for (int n = 0; n < numPixels; n++) {
            int x = upperLeftX + n % windowSize;
            int y = upperLeftY + n / windowSize;

            for (int i = 0; i < rasterNames.length; i++) {
                RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
                if (raster != null && product.containsPixel(x, y)) {
                    final int type = raster.getDataType();
                    if (raster.isFloatingPointType()) {
                        double[] temp = new double[1];
                        raster.readPixels(x, y, 1, 1, temp);
                        values[i] = temp[0];
                    } else {
                        int[] temp = new int[1];
                        raster.readPixels(x, y, 1, 1, temp);
                        if (raster instanceof Mask) {
                            values[i] = temp[0] == 0 ? 0 : 1; // normalize to 0 for false and 1 for true
                        } else {
                            if (raster.getDataType() == ProductData.TYPE_UINT32) {
                                values[i] = temp[0] & 0xffffL;
                            } else {
                                values[i] = temp[0];
                            }
                        }
                    }
                }
            }
            final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
            GeoPos currentGeoPos = product.getGeoCoding().getGeoPos(pixelPos, null);
            boolean isValid = validData.getSample(x, y, 0) != 0;

            final Measurement measure = new Measurement(coordinateID, coordinate.getName(), id,
                                                        pixelPos.x, pixelPos.y, scanLineTime, currentGeoPos, values,
                                                        isValid);
            List<Measurement> measurementList = measurements.get(productType);
            if (measurementList == null) {
                measurementList = new ArrayList<Measurement>();
                measurements.put(productType, measurementList);
            }
            measurementList.add(measure);
        }
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
        final Calendar currentDate = timeAtPixel.getAsCalendar();

        final Calendar lowerTimeBound = (Calendar) currentDate.clone();
        lowerTimeBound.add(calendarField, -timeDiff);
        final Calendar upperTimeBound = (Calendar) currentDate.clone();
        upperTimeBound.add(calendarField, timeDiff);

        Calendar coordinateCal = ProductData.UTC.createCalendar();
        coordinateCal.setTime(coordinate.getDateTime());

        return lowerTimeBound.compareTo(coordinateCal) <= 0 && upperTimeBound.compareTo(coordinateCal) >= 0;
    }

    void parseTimeDelta(String timeDelta) {
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

    private int getProductId(Product product) {
        final ProductDescription productDescription = ProductDescription.create(product);
        if (!productLocationList.contains(productDescription)) {
            productLocationList.add(productDescription);
        }
        return productLocationList.indexOf(productDescription);
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
                                                                    PinDescriptor.INSTANCE);
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

    private void extractMeasurements(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                final File[] subFiles = file.listFiles();
                for (File subFile : subFiles) {
                    if (subFile.isFile()) {
                        extractMeasurements(subFile);
                    }
                }
            } else {
                extractMeasurements(file);
            }
        }
    }

    private void extractMeasurements(File file) {
        Product product = null;
        try {
            product = ProductIO.readProduct(file);
            extractMeasurements(product);
        } catch (IOException ignore) {
        } finally {
            if (product != null) {
                if (isTargetProductInitialized) {
                    product.dispose();
                } else {
                    setTargetProduct(product);
                    isTargetProductInitialized = true;
                }
            }
        }
    }

    private void extractMeasurements(Product product) {
        if (!validator.validate(product)) {
            return;
        }

        if (!rasterNamesMap.containsKey(product.getProductType())) {
            rasterNamesMap.put(product.getProductType(), getAllRasterNames(product));
        }

        final PlanarImage validMaskImage = createValidMaskImage(product);
        try {
            for (int i = 0, coordinateListSize = coordinateList.size(); i < coordinateListSize; i++) {
                Coordinate coordinate = coordinateList.get(i);
                try {
                    readMeasurement(product, coordinate, i + 1, measurements, validMaskImage);
                } catch (IOException e) {
                    getLogger().warning(e.getMessage());
                }
            }
        } finally {
            validMaskImage.dispose();
        }
    }

    private String[] getAllRasterNames(Product product) {
        final List<String> allRasterList = new ArrayList<String>();
        if (exportBands) {
            Collections.addAll(allRasterList, product.getBandNames());
        }
        if (exportTiePoints) {
            Collections.addAll(allRasterList, product.getTiePointGridNames());
        }
        if (exportMasks) {
            Collections.addAll(allRasterList, product.getMaskGroup().getNodeNames());
        }
        return allRasterList.toArray(new String[allRasterList.size()]);
    }

    private void setDummyProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private void writeOutputToFile() throws IOException {
        FileWriter writer = null;

        for (String productType : measurements.keySet()) {
            List<Measurement> measurementList = measurements.get(productType);
            try {
                final String fileName = String.format("%s_%s.txt", outputFilePrefix, productType);
                writer = new FileWriter(new File(outputDir.getAbsolutePath(), fileName));
                String[] rasterNames = rasterNamesMap.get(productType);
                writeHeader(writer);
                MeasurementWriter.write(measurementList, writer, rasterNames, expression, exportExpressionResult);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        final String fileName = String.format("%s_productIdMap.txt", outputFilePrefix);
        writer = new FileWriter(new File(outputDir.getAbsolutePath(), fileName));
        try {
            writeProductIdMap(writer);
        } finally {
            writer.close();
        }
    }

    private void writeOutputToWriter(Writer writer) {
        writeHeader(writer);
        for (String productType : measurements.keySet()) {
            List<Measurement> measurementList = measurements.get(productType);
            final PrintWriter printWriter = new PrintWriter(writer);
            try {
                String[] rasterNames = rasterNamesMap.get(productType);
                printWriter.printf("# %s%n", productType);
                MeasurementWriter.write(measurementList, printWriter, rasterNames, expression, exportExpressionResult);
                printWriter.println();
            } finally {
                printWriter.close();
            }
        }
        writeProductIdMap(writer);
    }

    private void writeHeader(Writer writer) {
        final PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("# BEAM pixel extraction export table");
        printWriter.println('#');
        printWriter.printf("# Window size: %d%n", windowSize);
        if (expression != null) {
            printWriter.printf("# Expression: %s%n", expression);
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        printWriter.printf("# Created on:\t%s%n", dateFormat.format(new Date()));
        printWriter.println();
    }

    private void writeProductIdMap(Writer writer) {
        final PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("# Product ID Map");
        printWriter.println();
        printWriter.println("ProductID\tProductType\tProductLocation");
        for (int id = 0; id < productLocationList.size(); id++) {
            ProductDescription productDescription = productLocationList.get(id);
            final String productType = productDescription.getProductType();
            final String productLocation = productDescription.getProductLocation();
            printWriter.printf("%d\t%s\t%s%n", id, productType, productLocation);
        }
    }

    static File[] getParsedInputPaths(File[] filePaths) {
        final ArrayList<File> directoryList = new ArrayList<File>();
        for (File file : filePaths) {
            String trimmedPath = file.getPath().trim();
            if (trimmedPath.endsWith(RECURSIVE_INDICATOR)) {
                trimmedPath = trimmedPath.substring(0, trimmedPath.lastIndexOf(RECURSIVE_INDICATOR));

                final File directory = new File(trimmedPath);
                collectDirectoriesRecursive(directory, directoryList);
            } else {
                directoryList.add(new File(trimmedPath));
            }
        }
        return directoryList.toArray(new File[directoryList.size()]);
    }

    private static void collectDirectoriesRecursive(File directory, ArrayList<File> directoryList) {
        if (directory.isDirectory()) {
            directoryList.add(directory);
            final File[] subDirs = directory.listFiles(new DirectoryFileFilter());
            for (File subDir : subDirs) {
                collectDirectoriesRecursive(subDir, directoryList);
            }
        }
    }

    private static class DirectoryFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
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
                throw new ValidationException("Value of squareSize must be uneven");
            }
        }
    }

    private static class ProductDescription {

        private final String productType;
        private final String productLocation;

        static ProductDescription create(Product product) {
            String location = getProductLocation(product);
            return new ProductDescription(product.getProductType(), location);
        }

        private static String getProductLocation(Product product) {
            final File fileLocation = product.getFileLocation();
            if (fileLocation != null) {
                return fileLocation.getAbsolutePath();
            } else {
                return String.format("Not saved to disk [%s]", product.getName());
            }
        }

        private ProductDescription(String productType, String productLocation) {
            this.productType = productType;
            this.productLocation = productLocation;
        }

        public String getProductType() {
            return productType;
        }

        public String getProductLocation() {
            return productLocation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ProductDescription that = (ProductDescription) o;

            if (!productLocation.equals(that.productLocation)) {
                return false;
            }
            if (!productType.equals(that.productType)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = productType.hashCode();
            result = 31 * result + productLocation.hashCode();
            return result;
        }
    }

}