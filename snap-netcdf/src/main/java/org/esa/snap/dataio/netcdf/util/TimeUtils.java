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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.text.ParseException;

public class TimeUtils {

    private static final String ALTERNATIVE_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public static ProductData.UTC getSceneRasterTime(NetcdfFile ncFile, String dateAttrName, String timeAttrName) {
        final Attribute dateAttr = ncFile.findGlobalAttribute(dateAttrName);
        final Attribute timeAttr = ncFile.findGlobalAttribute(timeAttrName);
        final String dateTimeStr = getDateTimeString(dateAttr, timeAttr);

        if (dateTimeStr != null) {
            return parseDateTime(dateTimeStr);
        }
        return null;
    }

    public static String getDateTimeString(Attribute dateAttr, Attribute timeAttr) {
        String date = dateAttr != null ? dateAttr.getStringValue() : null;
        String time = timeAttr != null ? timeAttr.getStringValue() : null;
        if (date != null) {
            if (date.endsWith("UTC")) {
                date = date.substring(0, date.length() - 3).trim();
            }
            if (date.startsWith("UTC=")) {
                date = date.substring(4, date.length()).trim();
            }
        }
        if (time != null) {
            if (time.endsWith("UTC")) {
                time = time.substring(0, time.length() - 3).trim();
            }
            if (time.startsWith("UTC=")) {
                time = time.substring(4, time.length()).trim();
            }
        }
        if (date != null && time != null) {
            return date + " " + time;
        }
        if (date != null) {
            return date + (date.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        if (time != null) {
            return time + (time.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        return null;
    }

    public static ProductData.UTC parseDateTime(String dateTimeStr) {
        try {
            return ProductData.UTC.parse(dateTimeStr);
        } catch (ParseException ignore) {
            try {
                return ProductData.UTC.parse(dateTimeStr, Constants.DATE_TIME_PATTERN);
            } catch (ParseException ignore2) {
                try {
                    return ProductData.UTC.parse(dateTimeStr, ALTERNATIVE_DATE_TIME_PATTERN);
                } catch (ParseException ignoreAgain) {
                    SystemUtils.LOG.warning("Failed to parse time string '" + dateTimeStr + "'");
                    return null;
                }
            }
        }
    }
}
