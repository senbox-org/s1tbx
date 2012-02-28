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

package org.esa.beam.csv.productio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

    public HeaderImpl(File csv) throws IOException {
        parseHeader(csv);
    }

    private void parseHeader(File csv) throws IOException {
        List<String> attributeHeaderList = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(csv));
            while ((line = reader.readLine()) != null) {
                if(line.startsWith("#")) {
                    continue;
                }
                final StringTokenizer stringTokenizer = new StringTokenizer(line, "\t");
                while (stringTokenizer.hasMoreTokens()) {
                    final String token = stringTokenizer.nextToken();
                    attributeHeaderList.add(token.trim());
                }
                break;
            }
            columnCount = attributeHeaderList.size();

            int latIndex = indexOf(line, Constants.LAT_NAMES);
            int lonIndex = indexOf(line, Constants.LON_NAMES);
            int timeIndex = indexOf(line, Constants.TIME_NAMES);
            int locationNameIndex = indexOf(line, Constants.LOCATION_NAMES);

            hasLocation = latIndex >= 0 && lonIndex >= 0;
            hasTime = timeIndex >= 0;
            hasLocationName = locationNameIndex >= 0;

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        final List<AttributeHeader> tempMeasurementAttributeHeaders = new ArrayList<AttributeHeader>();
        final List<AttributeHeader> tempAttributeHeaders = new ArrayList<AttributeHeader>();
        for (final String attributeHeader : attributeHeaderList) {
            final String[] strings = attributeHeader.split(":");
            final String name = strings[0];
            final String type = strings[1];

            final AttributeHeader header = new AttributeHeader();
            header.name = name;
            header.type = type;
            tempAttributeHeaders.add(header);
            if (!isReservedField(name)) {
                tempMeasurementAttributeHeaders.add(header);
            }
        }
        measurementAttributeHeaders = tempMeasurementAttributeHeaders.toArray(new AttributeHeader[tempMeasurementAttributeHeaders.size()]);
        attributeHeaders = tempAttributeHeaders.toArray(new AttributeHeader[tempAttributeHeaders.size()]);
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

    private static int indexOf(String line, String[] possibleValues) {
        int index = -1;
        for (String possibleValue : possibleValues) {
            index = line.indexOf(possibleValue);
            if(index != -1) {
                return index;
            }
        }
        return index;
    }

    private boolean isReservedField(String name) {
        return (indexOf(name, Constants.LAT_NAMES) >= 0 ||
                indexOf(name, Constants.LON_NAMES) >= 0 ||
                indexOf(name, Constants.TIME_NAMES) >= 0 ||
                indexOf(name, Constants.LOCATION_NAMES) >= 0);
    }

    public class AttributeHeader {
        String name;
        Object type;
    }
}
