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

/**
 * Interface representing the header row of a csv product file.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface Header {
    /**
     * @return {@code true}, if records that conform to this header return location values (see {@link Record#getLocation()}).
     */
    boolean hasLocation();

    /**
     * @return {@code true}, if records that conform to this header return time values (see {@link Record#getTime()}).
     */
    boolean hasTime();

    /**
     * @return {@code true}, if records that conform to this header return station name values (see {@link Record#getLocationName()}).
     */
    boolean hasLocationName();

    /**
     * @return The array of those attribute names, which are not recognized as reserved fields, such as geographical
     * position, location name, and date/time.
     */
    HeaderImpl.AttributeHeader[] getMeasurementAttributeHeaders();

    /**
     * @return The number of all columns, including reserved fields, such as geographical position, location name,
     * and date/time.
     */
    int getColumnCount();

    /**
     * Returns the attribute header for the given column index.
     * @param columnIndex The column index to return the attribute header for.
     * @return The attribute header for the given column index.
     */
    HeaderImpl.AttributeHeader getAttributeHeader(int columnIndex);
    
}
