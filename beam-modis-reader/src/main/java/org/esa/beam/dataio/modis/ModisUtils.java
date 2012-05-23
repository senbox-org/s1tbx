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
package org.esa.beam.dataio.modis;

import org.esa.beam.util.DateTimeUtils;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.Range;

import java.text.ParseException;
import java.util.Date;

public class ModisUtils {


    /**
     * Decodes the "band_names" attribute string into a new band name
     *
     * @param names    a csv list of band names
     * @param layerIdx the index requested
     * @return the decoded band name
     */
    public static String decodeBandName(String names, int layerIdx) {
        String strRet = ".";
        String[] namesArray;

        namesArray = StringUtils.toStringArray(names, ",");
        strRet += namesArray[layerIdx];
        strRet = strRet.trim();

        return strRet;
    }

    /**
     * Decodes the band name extension as returned by decodeBandName to a float array containing wavelength and
     * bandwidth for the given band. ret[0] = wavelength in nm ret[1] = bandwidth in nm ret[2] = spectral band index
     *
     * @param bandExt the decoded band name
     * @param recycle the array instance to be returned, can be null
     * @return the band name extension
     */
    public static float[] decodeSpectralInformation(String bandExt, float[] recycle) {
        if ((recycle == null) || recycle.length < 3) {
            recycle = new float[3];
        }

        final int idx;
        // removes all non-number characters
        bandExt = bandExt.replaceAll("\\D", "");
        idx = Integer.parseInt(bandExt) - 1;

        recycle[0] = ModisConstants.BAND_CENTER_WAVELENGTHS[idx];
        recycle[1] = ModisConstants.BAND_WIDTHS[idx];
        recycle[2] = ModisConstants.SPECTRAL_BAND_INDEX[idx];

        return recycle;
    }

    /**
     * Extracts a value for the given key from a daac formatted metadata String.
     * Returns null if key does not exist.
     *
     * @param metaDataString the metadata string
     * @param key            the search key
     * @return the value or null if key does not existz
     */
    public static String extractValueForKey(String metaDataString, String key) {
        Guardian.assertNotNull("metaDataString", metaDataString);
        Guardian.assertNotNull("key", key);

        int pos = metaDataString.indexOf(key);
        if (pos >= 0) {
            pos = metaDataString.indexOf("VALUE", pos);
            if (pos >= 0) {
                pos = metaDataString.indexOf('=', pos);
                if (pos >= 0) {
                    int endPos = metaDataString.indexOf('\n', pos + 1);
                    if (endPos >= 0) {
                        String value = metaDataString.substring(pos + 1, endPos);
                        value = value.replace('"', ' ');
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creates a date from the date and time strings passed in. Strings must be formatted to MODIS conventions
     * date = "yyyy-mm-dd"
     * time = "hh:mm:ss.SSSSSS"
     *
     * @param date
     * @param time
     * @return a date
     */
    public static Date createDateFromStrings(final String date, final String time) throws ParseException {
        final String expectedTimeString = "00:00:00.000";
        final String dateTimeString;
        if (time.length() < expectedTimeString.length()) {
            final String perfection = expectedTimeString.substring(time.length());
            dateTimeString = date + " " + time + perfection;
        } else {
            dateTimeString = date + ' ' + time.substring(0, expectedTimeString.length());
        }
        return DateTimeUtils.stringToUTC(dateTimeString);
    }

    public static IncrementOffset getIncrementOffset(String attribute) {
        final IncrementOffset incrementOffset = new IncrementOffset();
        final Range range = getRangeFromString(attribute);

        incrementOffset.offset = (int) (range.getMin() - 1);
        incrementOffset.increment = (int) (range.getMax() - range.getMin());
        return incrementOffset;
    }

    public static Range getRangeFromString(String rangeString) {
        final Range range = new Range();
        final String[] intStringArray = StringUtils.toStringArray(rangeString, ",");

        final int start = Integer.parseInt(intStringArray[0]);
        final int stop = Integer.parseInt(intStringArray[1]);
        if (start < stop) {
            range.setMin(start);
            range.setMax(stop);
        } else {
            range.setMin(stop);
            range.setMax(start);
        }

        return range;
    }

    // package access for testing only tb 2012-05-22
    public static String extractBandName(String variableName) {
        final int slashIndex = variableName.lastIndexOf('/');
        if (slashIndex > 0) {
            return variableName.substring(slashIndex + 1, variableName.length());
        }
        return variableName;
    }
}
