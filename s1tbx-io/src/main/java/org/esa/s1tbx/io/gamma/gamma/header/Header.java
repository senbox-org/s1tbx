/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma.header;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.util.Map;

public class Header {

    private static final String UNKNOWN_SENSOR_TYPE = "Unknown Sensor Type";
    private final HeaderParser headerParser;

    private final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd-SSSSSSSS");

    public Header(final BufferedReader reader) throws IOException {
        headerParser = HeaderParser.parse(reader);
    }

    public ByteOrder getJavaByteOrder() {
        if (getByteOrder() == 0) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    public String getName() {
        return headerParser.getString(GammaConstants.HEADER_KEY_NAME, "");
    }

    public int getNumSamples() {
        int val = headerParser.getInt(GammaConstants.HEADER_KEY_SAMPLES, 0);
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_SAMPLES1, 0);
        }
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_WIDTH, 0);
        }
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_NCOLUMNS);
        }
        return val;
    }

    public int getNumLines() {
        int val = headerParser.getInt(GammaConstants.HEADER_KEY_LINES, 0);
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_LINES1, 0);
        }
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_HEIGHT, 0);
        }
        if (val == 0) {
            val = headerParser.getInt(GammaConstants.HEADER_KEY_NLINES);
        }
        return val;
    }

    public int getNumBands() {
        return headerParser.getInt(GammaConstants.HEADER_KEY_BANDS, 0);
    }

    public int getHeaderOffset() {
        return headerParser.getInt(GammaConstants.HEADER_KEY_HEADER_OFFSET, 0);
    }

    public String getDataType() {
        return headerParser.getString(GammaConstants.HEADER_KEY_DATA_TYPE, "Unknown");
    }

    public String getSensorType() {
        return headerParser.getString(GammaConstants.HEADER_KEY_SENSOR_TYPE, UNKNOWN_SENSOR_TYPE);
    }

    public int getByteOrder() {
        return headerParser.getInt(GammaConstants.HEADER_KEY_BYTE_ORDER, 0);
    }

    public String[] getBandNames() {
        return headerParser.getStrings(GammaConstants.HEADER_KEY_BAND_NAMES);
    }

    public String getDescription() {
        return headerParser.getString(GammaConstants.HEADER_KEY_DESCRIPTION, null);
    }

    public double getRadarFrequency() {
        return headerParser.getDouble(GammaConstants.HEADER_KEY_RADAR_FREQUENCY, 0);
    }

    public double getPRF() {
        return headerParser.getDouble(GammaConstants.HEADER_KEY_PRF, 0);
    }

    public int getRangeLooks() {
        return headerParser.getInt(GammaConstants.HEADER_KEY_RANGE_LOOKS, 1);
    }

    public int getAzimuthLooks() {
        return headerParser.getInt(GammaConstants.HEADER_KEY_AZIMUTH_LOOKS, 1);
    }

    public double getLineTimeInterval() {
        return headerParser.getDouble((GammaConstants.HEADER_KEY_LINE_TIME_INTERVAL), 0);
    }

    public ProductData.UTC getStartTime() {
        String timeStr = null;
        final String dateStr = headerParser.getString(GammaConstants.HEADER_KEY_DATE, null);
        if (dateStr != null) {
            String[] dateValues = StringUtils.split(dateStr, new char[]{' '}, true);
            if (dateValues.length > 2) {
                String year = dateValues[0];
                String month = dateValues[1];
                String day = dateValues[2];
                double startTime = headerParser.getDouble(GammaConstants.HEADER_KEY_START_TIME);

                timeStr = year + '-' + month + '-' + day + '-' + startTime;
            }
        }
        return AbstractMetadata.parseUTC(timeStr, dateFormat);
    }

    public ProductData.UTC getEndTime() {
        String timeStr = null;
        final String dateStr = headerParser.getString(GammaConstants.HEADER_KEY_DATE, null);
        if (dateStr != null) {
            String[] dateValues = StringUtils.split(dateStr, new char[]{' '}, true);
            if (dateValues.length > 2) {
                String year = dateValues[0];
                String month = dateValues[1];
                String day = dateValues[2];
                double endTime = headerParser.getDouble(GammaConstants.HEADER_KEY_END_TIME);

                timeStr = year + '-' + month + '-' + day + '-' + endTime;
            }
        }
        return AbstractMetadata.parseUTC(timeStr, dateFormat);
    }

    public MetadataElement getAsMetadata() {
        MetadataElement headerElem = new MetadataElement("Header");
        for (Map.Entry<String, String> entry : headerParser.getHeaderEntries()) {
            // empty strings are not allowed
            String value = entry.getValue().isEmpty() ? " " : entry.getValue();
            headerElem.setAttributeString(entry.getKey(), value);
        }
        return headerElem;
    }
}

