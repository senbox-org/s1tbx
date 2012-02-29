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
 * A default implementation of {@link Header}.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class HeaderImpl implements Header {

    private boolean hasLocation;
    private boolean hasTime;
    private boolean hasLocationName;
    private int columnCount;
    private AttributeHeader[] measurementAttributeHeaders;
    private AttributeHeader[] attributeHeaders;

    public HeaderImpl(int columnCount, boolean hasLocation, boolean hasLocationName, boolean hasTime, AttributeHeader[] attributeHeaders, AttributeHeader[] measurementAttributeHeaders) {
        this.columnCount = columnCount;
        this.hasLocation = hasLocation;
        this.hasLocationName = hasLocationName;
        this.hasTime = hasTime;
        this.attributeHeaders = attributeHeaders;
        this.measurementAttributeHeaders = measurementAttributeHeaders;
    }

    @Override
    public boolean hasLocation() {
        return hasLocation;
    }

    @Override
    public boolean hasTime() {
        return hasTime;
    }

    @Override
    public boolean hasLocationName() {
        return hasLocationName;
    }

    @Override
    public AttributeHeader[] getMeasurementAttributeHeaders() {
        return measurementAttributeHeaders;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public AttributeHeader getAttributeHeader(int columnIndex) {
        return attributeHeaders[columnIndex];
    }

    public static class AttributeHeader {

        public AttributeHeader(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String name;
        String type;
    }
}
