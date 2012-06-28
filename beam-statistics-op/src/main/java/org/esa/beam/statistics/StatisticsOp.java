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

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.statistics.calculators.StatisticsCalculator;

import java.io.File;
import java.text.ParseException;

/**
 * An operator that is used to compute statistics for any number of source products, restricted to regions given by an
 * ESRI shapefile.
 * <p/>
 * Its output is an ASCII file where the statistics are mapped to the source regions.
 * <p/>
 * Unlike most other operators, that can compute single {@link org.esa.beam.framework.gpf.Tile tiles},
 * the statistics operator processes all of its source products in its {@link #initialize()} method.
 *
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
                  version = "1.0",
                  authors = "Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes statistics for any number of source products.")
public class StatisticsOp extends Operator implements Output {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @SourceProducts(description =
                            "The source products to be considered for statistics computation. If not given, the " +
                            "parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    @Parameter(description = "The considered geographical region(s) given as polygons within an ESRI shape file")
    File shapefile;

    @Parameter(description =
                       "The start date. If not given, taken from the 'oldest' source product. Products that have " +
                       "a start date before the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    @Parameter(description =
                       "The end date. If not given, taken from the 'youngest' source product. Products that have " +
                       "an end date after the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;



    @Override
    public void initialize() throws OperatorException {





    }

    public static class BandConfiguration {

        @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
                                 "be provided.")
        String sourceBandName;

        @Parameter(description = "The band maths expression serving as input band. If empty, parameter 'sourceBandName'" +
                                 "must be provided.")
        String expression;

        @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
                                 "computation.")
        String validPixelExpression;

        @Parameter(description = "The name of the calculator that shall be used for this band.", converter = StatisticsCalculatorConverter.class)
        StatisticsCalculator statisticsCalculator;

    }

    static class UtcConverter implements Converter<ProductData.UTC> {

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, DATETIME_PATTERN);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Class<ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

    }

    static class StatisticsCalculatorConverter implements Converter<StatisticsCalculator> {

        @Override
        public StatisticsCalculator parse(String text) throws ConversionException {
            return null;
        }

        @Override
        public String format(StatisticsCalculator value) {
            return null;
        }

        @Override
        public Class<StatisticsCalculator> getValueType() {
            return StatisticsCalculator.class;
        }
    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(StatisticsOp.class);
        }
    }

}
