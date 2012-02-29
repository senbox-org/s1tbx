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

package org.esa.beam.csv.dataio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Interface representing a view on a data row within a csv product file.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface Record {

    /**
     * @return The location as (lat,lon) point or {@code null} if the location is not available (see {@link Header#hasLocation()}).
     *         The location is usually represented in form of one or more attribute values.
     */
    GeoPos getLocation();

    /**
     * @return The UTC time as instance of {@link ProductData.UTC} or {@code null} if the time is not available (see {@link Header#hasTime()}).
     */
    ProductData.UTC getTime();

    /**
     * @return The attribute values according to {@link Header#getMeasurementAttributeHeaders()}.
     *         The array will be empty if this record doesn't have any attributes.
     */
    Object[] getAttributeValues();

    /**
     * @return The name of the location related to this measurement
     */
    String getLocationName();

}
