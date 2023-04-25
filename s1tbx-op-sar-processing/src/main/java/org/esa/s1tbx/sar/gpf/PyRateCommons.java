package org.esa.s1tbx.sar.gpf;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

// Common helper functions that are used across various parts of the SNAP/PyRATE integration.
public class PyRateCommons {

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

    public static String createTabbedVariableLine(String key, String value){
        return key + ":\t" + value + "\n";
    }

    public static String createTabbedVariableLine(String key, int value){
        return createTabbedVariableLine(key, String.valueOf(value));
    }

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
