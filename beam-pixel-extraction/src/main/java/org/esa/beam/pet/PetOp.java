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
import org.esa.beam.framework.datamodel.Mask;
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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    @Parameter(alias = "rasters", itemAlias = "name",
               description = "The raster names used for extractions. Bands, tie-point grids, and masks can be used.")
    private String[] rasterNames;

    // todo load position list from file
    @Parameter(itemAlias = "coordinate", description = "The geo-coordinates", notNull = true,
               converter = GeoPosConverter.class)
    private GeoPos[] coordinates;

    @Parameter(description = "Side length of surrounding square (uneven)", defaultValue = "1",
               validator = SquareSizeValidator.class)
    private Integer squareSize;

    @Parameter(description = "The output file.")
    private File outputFile;

    private ProductValidator validator;

    private List<Measurement> measurements;

    @Override
    public void initialize() throws OperatorException {
        if (outputFile != null && outputFile.isDirectory()) {
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

        for (GeoPos geoPos : coordinates) {
            PixelPos centerPos = product.getGeoCoding().getPixelPos(geoPos, null);
            if (product.containsPixel(centerPos)) {
                PixelPos upperLeftPos = new PixelPos(centerPos.x - offset, centerPos.y - offset);
                List<String> exceptions = new ArrayList<String>();

                for (RasterDataNode raster : rasterList) {
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
            final Logger logger = getLogger();
            if (product == null) {
                return false;
            }
            final String type = product.getProductType();
            if (!productType.equalsIgnoreCase(type)) {
                final String msgPattern = "Product [%s] refused. Cause:\nType [%s] does not match specified product type [%s].";
                logger.log(Level.WARNING, String.format(msgPattern, product.getFileLocation(), type, productType));
                return false;
            }
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding == null) {
                final String msgPattern = "Product [%s] refused. Cause:\nProduct is not geo-coded.";
                logger.log(Level.WARNING, String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            if(!geoCoding.canGetPixelPos()) {
                final String msgPattern = "Product [%s] refused. Cause:\nPixel position can not be determined.";
                logger.log(Level.WARNING, String.format(msgPattern, product.getFileLocation()));
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
