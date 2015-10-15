/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex;

import com.bc.ceres.binding.converters.DateFormatConverter;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.util.Date;

/**
 * A coordinate is composed by a name, altitude, longitude and can optionally have a date and an arbitrary set of
 * measurement values.
 */
public class Coordinate {

    @Parameter(pattern = "[a-zA-Z_0-9]*")
    private String name;
    @Parameter(alias = "latitude")
    private Double lat;
    @Parameter(alias = "longitude")
    private Double lon;
    @Parameter(alias = "dateTime",
               description = "The date time of the coordinate in ISO 8601 format.\n The format pattern is 'yyyy-MM-dd'T'HH:mm:ssZ'",
               converter = ISO8601Converter.class)
    private Date dateTime;
    @Parameter(description = "Original values associated with this coordinate.", itemAlias = "originalValue")
    private OriginalValue[] originalValues;

    private int id;

    @SuppressWarnings({"UnusedDeclaration"})
    public Coordinate() {
        // needed for serialize/de-serialize
    }

    public Coordinate(String name, Double lat, Double lon, Date dateTime) {
        this(name, lat, lon, dateTime, new OriginalValue[0]);
    }

    public Coordinate(String name, Double lat, Double lon, Date dateTime, OriginalValue[] originalValues) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        //noinspection AssignmentToDateFieldFromParameter
        this.dateTime = dateTime;
        this.originalValues = originalValues;
    }

    public String getName() {
        return name;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Date getDateTime() {
        if (dateTime != null) {
            return (Date) dateTime.clone();
        }
        return null;
    }

    public OriginalValue[] getOriginalValues() {
        return originalValues;
    }

    public int getID() {
        return id;
    }

    public void setID(int ID) {
        this.id = ID;
    }

    public static class ISO8601Converter extends DateFormatConverter {

        public ISO8601Converter() {
            super(ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        }
    }

    public static class OriginalValue {

        @Parameter(description = "The name of the variable the original value is associated with.")
        String variableName;

        @Parameter(description = "The original value.")
        String value;

        @SuppressWarnings({"UnusedDeclaration"})
        public OriginalValue() {
            // needed for serialize/de-serialize
        }

        public OriginalValue(String variableName, String value) {
            this.variableName = variableName;
            this.value = value;
        }

        public String getVariableName() {
            return variableName;
        }

        public String getValue() {
            return value;
        }
    }
}
