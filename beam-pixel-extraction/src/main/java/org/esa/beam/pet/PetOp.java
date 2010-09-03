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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
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
import java.util.List;

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
    private Object csv;

    @Parameter(description = "The path of the input product(s). May point to a single file or a directory.")
    private File inputPath;

    @Parameter(description = "Specifies if the given path shall be searched for data products recursively.",
               defaultValue = "false")
    private Boolean recursive;

    @Parameter(description = "Specifies the allowed product type.", notNull = true, notEmpty = true)
    private String productType;


//    @Parameter(alias = "Latitude", description = "The latitude coordinate", notNull = true, interval = "[-90.0, 90.0]")
//    private Float lat;
//
//    @Parameter(alias = "Longitude", description = "The longitude coordinate", notNull = true,
//               interval = "[-180.0, 180.0]")
//    private Float lon;


    // todo load position list from file
    @Parameter(itemAlias = "coordinate", description = "The geo-coordinates", notNull = true, converter = GeoPosConverter.class)
    private GeoPos[] coordinates;


    @Parameter(description = "Side length of surrounding square (uneven)", defaultValue = "1",
               validator = SquareSizeValidator.class)
    private Integer squareSize;

    @Parameter(description = "The output file.")
    private File outputFile;

    // todo bandNames
    private String[] bandNames;

    private ProductValidator validator;

    private List<Measurement> measurements;

    @Override
    public void initialize() throws OperatorException {
        if(outputFile != null && outputFile.isDirectory()) {
            outputFile = new File(outputFile, "output.csv");
        }

        validator = new ProductValidator();
        measurements = new ArrayList<Measurement>();
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
        
        setTargetProduct(createDummyProduct());
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
            formatWriter.write(measurements, writer);
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
            }finally {
                if(product != null) {
                    product.dispose();
                }
            }
        }
    }

    private void extractMeasurements(Product product) {
        if (!validator.validate(product)) {
            return;
        }
        final List<RasterDataNode> rasters = new ArrayList<RasterDataNode>();
        rasters.addAll(Arrays.asList(product.getTiePointGrids()));
        rasters.addAll(Arrays.asList(product.getBands()));

        int offset = MathUtils.floorInt(squareSize / 2);

        for (GeoPos geoPos : coordinates) {
            PixelPos centerPos = product.getGeoCoding().getPixelPos(geoPos, null);
            if (product.containsPixel(centerPos)) {
                PixelPos upperLeftPos = new PixelPos(centerPos.x - offset, centerPos.y - offset);
                List<String> exceptions = new ArrayList<String>();

                for (RasterDataNode raster : rasters) {
                    try {
                        readMeasurement(geoPos, upperLeftPos, raster);
                    } catch (IOException e) {
                        exceptions.add(e.getMessage());
                    }
                }
                if (!exceptions.isEmpty()) {
                    logExceptions(exceptions);
                }
            }
        }
    }

    private void readMeasurement(GeoPos geoPos, PixelPos pixelPos, RasterDataNode raster) throws IOException {
        int x = MathUtils.floorInt(pixelPos.x);
        int y = MathUtils.floorInt(pixelPos.y);
        final double[] values = new double[squareSize * squareSize];
        raster.readPixels(x, y, squareSize, squareSize, values);
        final Measurement measure = new Measurement(raster.getName(), raster.getProduct().getStartTime(),
                                                    geoPos, values);
        measurements.add(measure);
    }

    private void logExceptions(List<String> exceptions) {
        // todo implement
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
            // todo log why a product is refused
            if (product == null) {
                return false;
            }
            if (!productType.equalsIgnoreCase(product.getProductType())) {
                return false;
            }
            final GeoCoding geoCoding = product.getGeoCoding();
            return !(geoCoding == null || !geoCoding.canGetGeoPos());
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
