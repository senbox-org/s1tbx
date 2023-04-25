package org.esa.s1tbx.sar.gpf;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

// Common helper functions that are used across various parts of the SNAP/PyRATE integration.
// Written by Alex McVittie April 2023.
public class PyRateCommons {

    // Converts format of 14May2020 to 20200414. or 2020 04 14 depending on if forPARFile is set to true or not.
    public static String bandNameDateToPyRateDate(String bandNameDate, boolean forPARFile){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM").withLocale(Locale.ENGLISH);
        TemporalAccessor accessor = formatter.parse(toSentenceCase(bandNameDate.substring(2, 5)));
        int monthNumber = accessor.get(ChronoField.MONTH_OF_YEAR);
        String month = String.valueOf(monthNumber);
        if(monthNumber < 10){
            month = "0" + month;
        }
        // Formatted as YYYYMMDD if for band/product names, YYYY MM DD if for GAMMA PAR file contents.
        String delimiter = " ".substring(forPARFile ? 0: 1);
        return bandNameDate.substring(5) + delimiter +
                month + delimiter + bandNameDate.substring(0, 2);
    }

    // Creates a tabbed variable line for GAMMA PAR files and PyRATE configuration files.
    public static String createTabbedVariableLine(String key, String value){
        return key + ":\t" + value + "\n";
    }

    // wrapper for createTabbedVariableLine(String, String)
    public static String createTabbedVariableLine(String key, int value){
        return createTabbedVariableLine(key, String.valueOf(value));
    }

    // wrapper for createTabbedVariableLine(String, String)
    public static String createTabbedVariableLine(String key, double value){
        return createTabbedVariableLine(key, String.valueOf(value));
    }

    // Makes first character upper case and the rest lowercase.
    // Convert string from HELLO to Hello. or hello to Hello.
    public static String toSentenceCase(String word){
        String firstCharacter = word.substring(0, 1);
        String rest = word.substring(1);
        return firstCharacter.toUpperCase() + rest.toLowerCase();
    }
}
