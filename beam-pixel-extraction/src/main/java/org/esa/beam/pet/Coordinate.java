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

import org.esa.beam.framework.gpf.annotations.Parameter;

public class Coordinate {

    @Parameter(pattern = "[a-zA-Z_0-9]*")
    private String name;
    @Parameter(alias = "latitude")
    private Float lat;
    @Parameter(alias = "longitude")
    private Float lon;

    @SuppressWarnings({"UnusedDeclaration"})
    public Coordinate() {
        // needed for serialize/de-serialize
    }

    public Coordinate(String name, Float lat, Float lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
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
}
