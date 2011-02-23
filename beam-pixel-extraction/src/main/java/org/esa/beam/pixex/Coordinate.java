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

import com.bc.ceres.binding.converters.DateFormatConverter;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.util.Date;

public class Coordinate {

    @Parameter(pattern = "[a-zA-Z_0-9]*")
    private String name;
    @Parameter(alias = "latitude")
    private Float lat;
    @Parameter(alias = "longitude")
    private Float lon;
    @Parameter(alias = "dateTime",
               description = "The date time of the coordinate in ISO 8601 format.\n The format pattern is 'yyyy-MM-dd'T'HH:mm:ssZ'",
               converter = ISO8601Converter.class)
    private Date dateTime;

    @SuppressWarnings({"UnusedDeclaration"})
    public Coordinate() {
        // needed for serialize/de-serialize
    }

    public Coordinate(String name, Float lat, Float lon, Date dateTime) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        //noinspection AssignmentToDateFieldFromParameter
        this.dateTime = dateTime;
    }

    public String getName() {
        return name;
    }

    public Float getLat() {
        return lat;
    }

    public Float getLon() {
        return lon;
    }

    public Date getDateTime() {
        if (dateTime != null) {
            return (Date) dateTime.clone();
        }
        return null;
    }

    public static class ISO8601Converter extends DateFormatConverter {

        public ISO8601Converter() {
            super(ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        }
    }


}
