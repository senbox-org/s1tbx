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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.ProductData;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DateRangeParser {

    static ProductData.UTC[] tryToGetDateRange(String productName) {
        final String singleDateMonthPattern = "\\d{6}_.*";
        final String startEndPattern = "\\d{8}_\\d{8}_.*";
        final String singleDateDayPattern = "\\d{8}_.*";
        final String singleDateDayPattern_leadingCharacters = ".*_\\d{8}_.*";
        if (productName.matches(singleDateMonthPattern)) {
            return parseSingleMonthDate(productName);
        } else if (productName.matches(startEndPattern)) {
            return parseStartEndDate(productName);
        } else if (productName.matches(singleDateDayPattern)) {
            return parseSingleDayDate(productName);
        } else if (productName.matches(singleDateDayPattern_leadingCharacters)) {
            return parseSingleDayDate(productName);
        }
        throw new IllegalArgumentException("Unable to derive date from product name '" + productName + "'.");
    }

    private static ProductData.UTC[] parseSingleMonthDate(String productName) {
        return parseSingleDayDate(productName, "yyyyMM");
    }

    private static ProductData.UTC[] parseSingleDayDate(String productName) {
        return parseSingleDayDate(productName, "yyyyMMdd");
    }

    private static ProductData.UTC[] parseSingleDayDate(String productName, String pattern) {
        final int beginIndex = findPaternStart(productName, pattern);
        return parseSingleDayDate(productName, pattern, beginIndex);
    }

    private static int findPaternStart(String productName, String pattern) {
        final String regex = "\\d{" + pattern.length() + "}";
        final Matcher matcher = Pattern.compile(regex).matcher(productName);
        matcher.find();
        return matcher.start();
    }

    private static ProductData.UTC[] parseSingleDayDate(String productName, String pattern, int beginIndex) {
        try {
            final String dayDate = productName.substring(beginIndex, beginIndex + pattern.length());
            ProductData.UTC startTime = ProductData.UTC.parse(dayDate, pattern);
            return new ProductData.UTC[]{startTime, startTime};
        } catch (ParseException ignored) {
            // should never come here
        }
        return new ProductData.UTC[2];
    }

    private static ProductData.UTC[] parseStartEndDate(String productName) {
        try {
            final String startDay = productName.substring(0, 8);
            final String endDay = productName.substring(9, 17);
            return new ProductData.UTC[]{
                        ProductData.UTC.parse(startDay, "yyyyMMdd"),
                        ProductData.UTC.parse(endDay, "yyyyMMdd")
            };
        } catch (ParseException ignored) {
            // should never come here
        }
        return new ProductData.UTC[2];
    }

}
