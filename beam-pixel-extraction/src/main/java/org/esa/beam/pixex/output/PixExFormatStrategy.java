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

package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.FormatStrategy;
import org.esa.beam.util.StringUtils;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PixExFormatStrategy implements FormatStrategy {

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss");

    private final RasterNamesFactory rasterNamesFactory;
    private final String expression;
    private final int windowSize;
    private final boolean exportExpressionResult;

    public PixExFormatStrategy(final RasterNamesFactory rasterNamesFactory, final int windowSize,
                               final String expression, final boolean exportExpressionResult) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.expression = expression;
        this.windowSize = windowSize;
        this.exportExpressionResult = exportExpressionResult;
    }

    @Override
    public void writeHeader(PrintWriter writer, Product product) {
        writer.printf("# BEAM pixel extraction export table%n");

        writer.printf("#%n");
        writer.printf(Locale.ENGLISH, "# Window size: %d%n", windowSize);
        if (expression != null) {
            writer.printf("# Expression: %s%n", expression);
        }

        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.printf(Locale.ENGLISH, "# Created on:\t%s%n%n", dateFormat.format(new Date()));

        boolean includeExpressionInTable = expression != null && exportExpressionResult;

        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        if (product != null) {
            ArrayList<Float> wavelengthList = new ArrayList<Float>();
            for (String rasterName : rasterNames) {
                RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
                if (rasterDataNode instanceof Band) {
                    Band band = (Band) rasterDataNode;
                    wavelengthList.add(band.getSpectralWavelength());
                }
            }
            if (!wavelengthList.isEmpty()) {
                Float[] wavelengthArray = wavelengthList.toArray(new Float[wavelengthList.size()]);
                String patternStart = "# Wavelength:";
                String patternPadding = "\t \t \t \t \t \t \t \t \t" + (includeExpressionInTable ? " \t" : "");
                writer.printf(Locale.ENGLISH, patternStart + patternPadding + "%s%n",
                              StringUtils.arrayToString(wavelengthArray, "\t"));
            }
        }


        if (includeExpressionInTable) {
            writer.print("Expression result\t");
        }
        writer.print("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)");

        for (String name : rasterNames) {
            writer.printf(Locale.ENGLISH, "\t%s", name);
        }
        writer.println();
    }

    @Override
    public void writeMeasurements(PrintWriter writer, Measurement[] measurements) {
        try {
            for (Measurement measurement : measurements) {
                write(writer, measurement);
            }
        } finally {
            writer.flush();
        }
    }

    private void write(PrintWriter writer, Measurement measurement) {
        if (expression == null || exportExpressionResult || measurement.isValid()) {
            final boolean withExpression = expression != null && exportExpressionResult;
            writeLine(writer, measurement, withExpression);
        }
    }

    public static void writeLine(PrintWriter writer, Measurement measurement, boolean withExpression) {
        if (withExpression) {
            writer.printf(Locale.ENGLISH, "%s\t", String.valueOf(measurement.isValid()));
        }
        final ProductData.UTC time = measurement.getTime();
        String timeString;
        if (time != null) {
            timeString = DATE_FORMAT.format(time.getAsDate());
        } else {
            timeString = " \t ";
        }
        writer.printf(Locale.ENGLISH,
                      "%d\t%d\t%s\t%.6f\t%.6f\t%.3f\t%.3f\t%s",
                      measurement.getProductId(), measurement.getCoordinateID(),
                      measurement.getCoordinateName(),
                      measurement.getLat(), measurement.getLon(),
                      measurement.getPixelX(), measurement.getPixelY(),
                      timeString);
        final Number[] values = measurement.getValues();
        for (Number value : values) {
            if (Double.isNaN(value.doubleValue())) {
                writer.printf(Locale.ENGLISH, "\t%s", "");
            } else {
                writer.printf(Locale.ENGLISH, "\t%s", value);
            }
        }
        writer.println();
    }

}
