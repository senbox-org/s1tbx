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

import org.esa.beam.framework.datamodel.ProductData;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
class MeasurementWriter {
    private String[] rasterNames;

    MeasurementWriter(String[] rasterNames) {
        this.rasterNames = rasterNames;
    }

    void write(List<Measurement> measurementList, Writer writer) {
        if( measurementList.isEmpty() ) {
            return;
        }
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.append("CoordinateID\tName\tLatitude\tLongitude\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\t");
        for (String name : rasterNames) {
            printWriter.append(String.format("%s\t", name));
        }
        printWriter.append("\n");
        for (Measurement measurement : measurementList) {
            final double[] values = measurement.getValues();
            final int id = measurement.getCoordinateID();
            final float lat = measurement.getLat();
            final float lon = measurement.getLon();
            final ProductData.UTC time = measurement.getStartTime();
            String timeString;
            if (time != null) {
                SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd\tHH:mm:ss", Locale.ENGLISH );
                timeString = sdf.format( time.getAsDate() );
            }else {
                timeString = " \t ";
            }

            printWriter.append(String.format("%d\t%s\t%s\t%s\t%s\t", id, measurement.getCoordinateName(), lat, lon, timeString));
            for (double value : values) {
                printWriter.append(String.format("%s\t", value));
            }
            printWriter.append("\n");
        }
    }

}
