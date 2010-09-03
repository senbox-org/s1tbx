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
import org.esa.beam.dataio.placemark.PlacemarkReader;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private Measurement[] measurements;

    @Parameter(description = "The path of the input product(s). May point to a single file or a directory.")
    private File inputPath;

    @Parameter(description = "Specifies if the given path shall be searched for data products recursively.",
               defaultValue = "false")
    private Boolean recursive;

    @Parameter(description = "Specifies the allowed product type.", notNull = true, notEmpty = true)
    private String productType;

    @Parameter(alias = "rasters", itemAlias = "name",
               description = "The raster names used for extractions. Bands, tie-point grids, and masks can be used.")
    private String[] rasterNames;

    @Parameter(itemAlias = "coordinate", description = "The geo-coordinates", converter = GeoPosConverter.class)
    private GeoPos[] coordinates;

    @Parameter(description = "Path to a file containing geo-coordinates")
    private File coordinatesFile;

    @Parameter(description = "Side length of surrounding square (uneven)", defaultValue = "1",
               validator = SquareSizeValidator.class)
    private Integer squareSize;

    @Parameter(description = "The output file.")
    private File outputFile;

    private ProductValidator validator;
    private List<Coordinate> coordinateList;
    private List<Measurement> measurementList;


    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && coordinates == null) {
            throw new OperatorException("No coordinates specified.");
        }
        if (outputFile != null && outputFile.isDirectory()) {
            outputFile = new File(outputFile, "output.txt");
        }
        coordinateList = new ArrayList<Coordinate>();
        if (coordinatesFile != null) {
            coordinateList.addAll(extractGeoPositions(coordinatesFile));
        }
        if (coordinates != null) {
            for (GeoPos coordinate : coordinates) {
                coordinateList.add(new Coordinate(coordinateList.size(), " ", coordinate));
            }
        }
        validator = new ProductValidator();
        measurementList = new ArrayList<Measurement>();
        if (sourceProducts != null) {
            for (Product product : sourceProducts) {
                extractMeasurements(product);
            }
        }
        if (inputPath != null) {
            extractMeasurements(inputPath);
        }
        if (outputFile != null) {
            writeOutput();
        }

        measurements = measurementList.toArray(new Measurement[measurementList.size()]);
        setTargetProduct(createDummyProduct());
    }

    private List<Coordinate> extractGeoPositions(File coordinatesFile) {
        final List<Coordinate> coordinateList = new ArrayList<Coordinate>();
        try {
            final Placemark[] pins = PlacemarkReader.readPlacemarks(coordinatesFile, null, PinDescriptor.INSTANCE);
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

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private void writeOutput() {
        final MeasurementWriter formatWriter = new MeasurementWriter();
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
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

    private void extractMeasurements(File path) {
        if (path.isDirectory()) {
            final File[] subFiles = path.listFiles();
            for (File file : subFiles) {
                if (file.isFile() || recursive) {
                    extractMeasurements(file);
                }
            }
        } else {
            Product product = null;
            try {
                product = ProductIO.readProduct(path);
                extractMeasurements(product);
            } catch (IOException ignore) {
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        }
    }

    private void extractMeasurements(Product product) {
        if (!validator.validate(product)) {
            return;
        }
        final List<RasterDataNode> rasterList = getValidRasterList(product);
        int offset = MathUtils.floorInt(squareSize / 2);

        for (Coordinate coordinate : coordinateList) {
            PixelPos centerPos = product.getGeoCoding().getPixelPos(coordinate.getGeoPos(), null);
            if (product.containsPixel(centerPos)) {
                PixelPos upperLeftPos = new PixelPos(centerPos.x - offset, centerPos.y - offset);
                try {
                    readMeasurement(coordinate, upperLeftPos, rasterList);
                } catch (IOException e) {
                    getLogger().warning(e.getMessage());
                }
            }
        }
    }

    private List<RasterDataNode> getValidRasterList(Product product) {
        final List<RasterDataNode> allRasterList = new ArrayList<RasterDataNode>();
        allRasterList.addAll(Arrays.asList(product.getTiePointGrids()));
        allRasterList.addAll(Arrays.asList(product.getBands()));
        allRasterList.addAll(Arrays.asList(product.getMaskGroup().toArray(new Mask[0])));
        final List<RasterDataNode> validRasterList = new ArrayList<RasterDataNode>();
        if (rasterNames != null && rasterNames.length > 0) {
            final List<String> rasterNameList = Arrays.asList(rasterNames);
            for (RasterDataNode raster : allRasterList) {
                if (rasterNameList.contains(raster.getName())) {
                    validRasterList.add(raster);
                }
            }
        }
        return validRasterList;
    }

    private void readMeasurement(Coordinate coordinate, PixelPos pixelPos, List<RasterDataNode> rasters) throws IOException {
        int x = MathUtils.floorInt(pixelPos.x);
        int y = MathUtils.floorInt(pixelPos.y);
        final double[] values = new double[rasters.size()];
        final int numPixels = squareSize * squareSize;
        final List<String> names = new ArrayList<String>();
        for (RasterDataNode raster : rasters) {
            names.add(raster.getName());
        }
        for (int n = 0; n < numPixels; n++) {
            x += n % squareSize;
            y += n / squareSize;
            for (int i = 0; i < rasters.size(); i++) {
                double[] temp = new double[1];
                rasters.get(i).readPixels(x, y, 1, 1, temp);
                values[i] = temp[0];
            }
            GeoPos currentGeoPos = rasters.get(0).getProduct().getGeoCoding().getGeoPos( new PixelPos(x, y), null );
            final Measurement measure = new Measurement(coordinate.getId(), coordinate.getName(), names, rasters.get(0).getProduct().getStartTime(),
                                                        currentGeoPos, values);
            measurementList.add(measure);
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
            final String type = product.getProductType();
            if (!productType.equalsIgnoreCase(type)) {
                final String msgPattern = "Product [%s] refused. Cause:\nType [%s] does not match specified product type [%s].";
                logger.warning(String.format(msgPattern, product.getFileLocation(), type, productType));
                return false;
            }
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
