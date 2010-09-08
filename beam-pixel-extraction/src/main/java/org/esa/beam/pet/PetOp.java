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

package org.esa.beam.pet;

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
import org.esa.beam.util.math.MathUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({
        "IOResourceOpenedButNotSafelyClosed", "MismatchedReadAndWriteOfArray",
        "FieldCanBeLocal", "UnusedDeclaration"
})
@OperatorMetadata(
        alias = "Pet",
        version = "1.0",
        authors = "Marco Peters, Thomas Storm",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Generates a CSV file from a given pixel location of source products.")
public class PetOp extends Operator {

    @SourceProducts()
    private Product[] sourceProducts;

    @TargetProperty()
    private Map<String, List<Measurement>> measurements;
    //    private Measurement[] measurements;

    @Parameter(description = "The paths to be scanned for input products. May point to a single file or a directory.")
    private File[] inputPaths;

//    @Parameter(description = "Specifies the allowed product type.", notNull = true, notEmpty = true)
//    private String productType;

//    @Parameter(alias = "rasters", itemAlias = "name",
//               description = "The raster names used for extractions. Bands, tie-point grids, and masks can be used.")
//    private String[] rasterNames;

    @Parameter(description = "Specifies if tie-points are to be exported", defaultValue = "true")
    private Boolean exportTiePoints;

    @Parameter(description = "Specifies if bands are to be exported", defaultValue = "true")
    private Boolean exportBands;

    @Parameter(description = "Specifies if masks are to be exported", defaultValue = "true")
    private Boolean exportMasks;

    @Parameter(itemAlias = "coordinate", description = "The geo-coordinates", converter = GeoPosConverter.class)
    private GeoPos[] coordinates;

    @Parameter(description = "Path to a file containing geo-coordinates")
    private File coordinatesFile;

    @Parameter(description = "Side length of surrounding square (uneven)", defaultValue = "1",
               validator = SquareSizeValidator.class)
    Integer squareSize;

    @Parameter(description = "The output directory.")
    private File outputDir;

    String[] rasterNames;
    private ProductValidator validator;
    private List<Coordinate> coordinateList;
//    private List<Measurement> measurementList;

    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && coordinates == null) {
            throw new OperatorException("No coordinates specified.");
        }
        if (outputDir != null && outputDir.isFile()) {
            outputDir = new File(outputDir.getParent());
        }
        coordinateList = new ArrayList<Coordinate>();
        if (coordinatesFile != null) {
            coordinateList.addAll(extractGeoPositions(coordinatesFile));
        }
        if (coordinates != null) {
            for (GeoPos geoPos : coordinates) {
                coordinateList.add(new Coordinate(coordinateList.size(), geoPos));
            }
        }
        validator = new ProductValidator();
        measurements = new HashMap<String, List<Measurement>>();
        if (sourceProducts != null) {
            for (Product product : sourceProducts) {
                extractMeasurements(product);
            }
        }
        if (inputPaths != null) {
            inputPaths = cleanPathNames(inputPaths);
            extractMeasurements(inputPaths);
        }
        if (outputDir != null) {
            writeOutput();
        }

//        measurements = measurementList.toArray(new Measurement[measurementList.size()]);
        setTargetProduct(createDummyProduct());
    }

    private List<Coordinate> extractGeoPositions(File coordinatesFile) {
        final List<Coordinate> coordinateList = new ArrayList<Coordinate>();
        try {
            final List<Placemark> pins = PlacemarkIO.readPlacemarks(new FileReader(coordinatesFile),
                                                                    null, // no GeoCoding needed
                                                                    PinDescriptor.INSTANCE);
            for (Placemark pin : pins) {
                final GeoPos geoPos = pin.getGeoPos();
                if (geoPos != null) {
                    final int id = coordinateList.size();
                    coordinateList.add(new Coordinate(id, pin.getName(), geoPos));
                }
            }
        } catch (IOException ignore) {
            return Collections.emptyList();
        }
        return coordinateList;
    }

    private Product createDummyProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        return product;
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
                product.dispose();
            }
        }
    }

    private void extractMeasurements(Product product) {
        if (!validator.validate(product)) {
            return;
        }

        rasterNames = getAllRasterNames(product);
        if (rasterNames.length == 0) {
            return;
        }

        for (Coordinate coordinate : coordinateList) {
            try {
                readMeasurement(product, coordinate, measurements);
            } catch (IOException e) {
                getLogger().warning(e.getMessage());
            }
        }
    }

    private static File[] cleanPathNames(File[] paths) {
        for (int i = 0; i < paths.length; i++) {
            File path = paths[i];
            paths[i] = new File(path.getPath().trim());
        }
        return paths;
    }


    void readMeasurement(Product product, Coordinate coordinate, Map<String, List<Measurement>> measurements) throws
                                                                                                              IOException {
        PixelPos centerPos = product.getGeoCoding().getPixelPos(coordinate.getGeoPos(), null);
        if (!product.containsPixel(centerPos)) {
            return;
        }
        int offset = MathUtils.floorInt(squareSize / 2);
        int upperLeftX = MathUtils.floorInt(centerPos.x - offset);
        int upperLeftY = MathUtils.floorInt(centerPos.y - offset);
        final double[] values = new double[rasterNames.length];
        Arrays.fill(values, Double.NaN);
        final int numPixels = squareSize * squareSize;
        for (int n = 0; n < numPixels; n++) {
            int x = upperLeftX + n % squareSize;
            int y = upperLeftY + n / squareSize;
            for (int i = 0; i < rasterNames.length; i++) {
                RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
                if (raster != null && product.containsPixel(x, y)) {
                    double[] temp = new double[1];
                    raster.readPixels(x, y, 1, 1, temp);
                    values[i] = temp[0];
                }
            }
            GeoPos currentGeoPos = product.getGeoCoding().getGeoPos(new PixelPos(x, y), null);
            final Measurement measure = new Measurement(coordinate.getId(), coordinate.getName(),
                                                        product.getStartTime(), currentGeoPos, values);
            String productType = product.getProductType();
            List<Measurement> measurementList = measurements.get(productType);
            if (measurementList == null) {
                measurementList = new ArrayList<Measurement>();
                measurements.put(productType, measurementList);
            }
            measurementList.add(measure);
        }
    }

    private String[] getAllRasterNames(Product product) {
        final List<RasterDataNode> allRasterList = new ArrayList<RasterDataNode>();
        if (exportBands) {
            allRasterList.addAll(Arrays.asList(product.getBands()));
        }
        if (exportTiePoints) {
            allRasterList.addAll(Arrays.asList(product.getTiePointGrids()));
        }
        if (exportMasks) {
            allRasterList.addAll(Arrays.asList(product.getMaskGroup().toArray(new Mask[0])));
        }
        String[] allRasterNames = new String[allRasterList.size()];
        for (int i = 0; i < allRasterList.size(); i++) {
            allRasterNames[i] = allRasterList.get(i).getName();
        }
        return allRasterNames;
    }

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private void writeOutput() {
        FileWriter writer = null;

        for (String productType : measurements.keySet()) {
            List<Measurement> measurementList = measurements.get(productType);
            try {
                writer = new FileWriter(new File(outputDir.getAbsolutePath(), "expix_" + productType + ".txt"));
                MeasurementWriter formatWriter = new MeasurementWriter(rasterNames);
                formatWriter.write(measurementList, writer);
            } catch (IOException e) {
                throw new OperatorException("Could not write to output file.", e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static class SquareSizeValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            if (((Integer) value) % 2 == 0) {
                throw new ValidationException("Value of squareSize must be uneven");
            }
        }
    }

    private class ProductValidator {

        public boolean validate(Product product) {
            final Logger logger = getLogger();
            if (product == null) {
                return false;
            }
//            final String type = product.getProductType();
//            if (!productType.equalsIgnoreCase(type)) {
//                final String msgPattern = "Product [%s] refused. Cause:\nType [%s] does not match specified product type [%s].";
//                logger.warning(String.format(msgPattern, product.getFileLocation(), type, productType));
//                return false;
//            }
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding == null) {
                final String msgPattern = "Product [%s] refused. Cause:\nProduct is not geo-coded.";
                logger.warning(String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            if (!geoCoding.canGetPixelPos()) {
                final String msgPattern = "Product [%s] refused. Cause:\nPixel position can not be determined.";
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
            super(PetOp.class);
        }
    }

}