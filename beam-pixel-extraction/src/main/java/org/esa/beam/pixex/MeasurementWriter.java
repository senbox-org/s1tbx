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

import org.esa.beam.framework.datamodel.ProductData;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.List;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
class MeasurementWriter {

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss");

    static void write(List<Measurement> measurementList, Writer writer, String[] rasterNames, String expression,
                      boolean exportExpressionResult) {
        if (measurementList.isEmpty()) {
            return;
        }

        PrintWriter printWriter = new PrintWriter(writer);

        if (expression != null && exportExpressionResult) {
            printWriter.append("Expression result\t");
        }
        printWriter.append(
                "ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\t");
        for (String name : rasterNames) {
            printWriter.append(String.format("%s\t", name));
        }
        printWriter.println();
        for (Measurement measurement : measurementList) {
            if (expression == null || exportExpressionResult || measurement.isValid()) {
                final int productId = measurement.getProductId();
                final float pixelX = measurement.getPixelX();
                final float pixelY = measurement.getPixelY();
                final int coordinateID = measurement.getCoordinateID();
                final float lat = measurement.getLat();
                final float lon = measurement.getLon();
                final ProductData.UTC time = measurement.getTime();
                final Number[] values = measurement.getValues();
                String timeString;
                if (time != null) {
                    timeString = DATE_FORMAT.format(time.getAsDate());
                } else {
                    timeString = " \t ";
                }

                if (expression != null && exportExpressionResult) {
                    printWriter.append(String.format("%s\t", String.valueOf(measurement.isValid())));
                }
                printWriter.append(String.format("%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t",
                                                 productId, coordinateID,
                                                 measurement.getCoordinateName(), lat, lon, pixelX, pixelY,
                                                 timeString));
                for (Number value : values) {
                    printWriter.append(String.format("%s\t", value));
                }
                printWriter.println();
            }
        }
    }

}
