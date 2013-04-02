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

package org.esa.beam.opendap.utils;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.util.TimeStampExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides collections of default patterns for time stamp extraction.
 * Also, it has a static method which allows for the detection of valid
 * patterns for a given filename.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 *
 */
public class PatternProvider {

    public static final String[] DATE_PATTERNS = {
            "yyyyMMdd_hhmmss",
            "yyyyMMdd:hhmmss",
            "yyyyMMddhhmmss",
            "yyyyMMddhh",
            "yyyyMMdd",
            "yyyyMM"
    };

    public static final String[] FILENAME_PATTERNS = {
            "*${startDate}*",
            "*${startDate}*${endDate}*"
    };

    public static List<String[]> recommendPatterns(String fileName) {
        fileName = replaceColon(fileName);
        List<String[]> patternCombinations = new ArrayList<String[]>();
        for(String dateString : DATE_PATTERNS) {
            dateString = replaceColon(dateString);
            final Pattern datePattern = convertDateStringToPattern(dateString);
            Matcher matcher = datePattern.matcher(fileName);
            if(matcher.matches()) {
                TimeStampExtractor timeStampExtractor = new TimeStampExtractor(dateString, FILENAME_PATTERNS[0]);
                try{
                    timeStampExtractor.extractTimeStamps(fileName);
                    patternCombinations.add(new String[]{insertColon(dateString), FILENAME_PATTERNS[0]});
                    try{
                        timeStampExtractor = new TimeStampExtractor(insertColon(dateString), FILENAME_PATTERNS[1]);
                        timeStampExtractor.extractTimeStamps(fileName);
                        patternCombinations.add(new String[]{dateString, FILENAME_PATTERNS[1]});
                    } catch (ValidationException e) {
                        //do nothing
                    }
                } catch (ValidationException e) {
                    //do nothing
                }
            }
        }
        return patternCombinations;
    }

    private static String replaceColon(String replace) {
        return replace.replace(":", "colon");
    }

    private static String insertColon(String replace) {
        return replace.replace("colon", ":");
    }

    private static Pattern convertDateStringToPattern(String datePattern) {
        final String validSign = "[\\w\\. -]";
        final String anyTimesModifier = "*";
        final String starSignPattern = validSign + anyTimesModifier;

        final String dateMatcher = getDateMatcher(datePattern);

        return Pattern.compile(starSignPattern + dateMatcher + starSignPattern);
    }

    private static String getDateMatcher(String datePattern) {
        final String yearPattern = "yyyy";
        final String monthPattern = "MM";
        final String dayPattern = "dd";
        final String hourPattern = "hh";
        final String minutePattern = "mm";
        final String secondPattern = "ss";

        final String yearMatcher = "(\\\\d{" + yearPattern.length() + "})";
        final String monthMatcher = "(\\\\d{" + monthPattern.length() + "})";
        final String dayMatcher = "(\\\\d{" + dayPattern.length() + "})";
        final String hourMatcher = "(\\\\d{" + hourPattern.length() + "})";
        final String minuteMatcher = "(\\\\d{" + minutePattern.length() + "})";
        final String secondMatcher = "(\\\\d{" + secondPattern.length() + "})";

        String dateMatcher = datePattern.replaceAll(yearPattern, yearMatcher);
        dateMatcher = dateMatcher.replaceAll(monthPattern, monthMatcher);
        dateMatcher = dateMatcher.replaceAll(dayPattern, dayMatcher);
        dateMatcher = dateMatcher.replaceAll(hourPattern, hourMatcher);
        dateMatcher = dateMatcher.replaceAll(minutePattern, minuteMatcher);
        dateMatcher = dateMatcher.replaceAll(secondPattern, secondMatcher);
        return dateMatcher;
    }

}
