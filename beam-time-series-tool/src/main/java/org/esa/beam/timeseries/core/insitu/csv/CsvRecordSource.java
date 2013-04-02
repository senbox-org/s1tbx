package org.esa.beam.timeseries.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.timeseries.core.insitu.Header;
import org.esa.beam.timeseries.core.insitu.Record;
import org.esa.beam.timeseries.core.insitu.RecordSource;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * A record source that reads from a CSV stream. Values must be separated by a TAB character, records by a NL (newline).
 * The first records must contain header names. All non-header records must use the same data type in a column.
 *
 * @author Norman
 */
public class CsvRecordSource implements RecordSource {

    private static final String[] LAT_NAMES = new String[]{"lat", "latitude", "northing"};
    private static final String[] LON_NAMES = new String[]{"lon", "long", "longitude", "easting"};
    private static final String[] TIME_NAMES = new String[]{"time", "date"};
    private static final String[] STATION_NAMES = new String[]{"name", "station", "label"};
    private final LineNumberReader reader;
    private final Header header;
    private final int recordLength;
    private final DateFormat dateFormat;
    private final int latIndex;
    private final int lonIndex;
    private final int timeIndex;
    private final int stationNameIndex;
    private final Class<?>[] attributeTypes;
    private Iterable<Record> recordIterable;
    private CsvRecordIterator csvRecordIterator;

    public CsvRecordSource(Reader reader, DateFormat dateFormat) throws IOException {
        if (reader instanceof LineNumberReader) {
            this.reader = (LineNumberReader) reader;
        } else {
            this.reader = new LineNumberReader(reader);
        }

        this.dateFormat = dateFormat;

        String[] columnNames = readTextRecords(-1).get(0);
        attributeTypes = new Class<?>[columnNames.length];

        latIndex = indexOf(columnNames, LAT_NAMES);
        lonIndex = indexOf(columnNames, LON_NAMES);
        timeIndex = indexOf(columnNames, TIME_NAMES);
        stationNameIndex = indexOf(columnNames, STATION_NAMES);

        final String[] parameterNames = getParameterNames(columnNames);
        final boolean hasLocation = latIndex >= 0 && lonIndex >= 0;
        final boolean hasTime = timeIndex >= 0;
        final boolean hasStationName = stationNameIndex >= 0;
        header = new DefaultHeader(hasLocation, hasTime, hasStationName, columnNames, parameterNames);
        recordLength = columnNames.length;
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public Iterable<Record> getRecords() {
        if (recordIterable == null) {
            recordIterable = createIterable();
        }

        if (csvRecordIterator != null) {
            csvRecordIterator.currentRecord = 0;
        }
        return recordIterable;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ignore) {
        }
    }

    private Iterable<Record> createIterable() {
        return new Iterable<Record>() {
            @Override
            public Iterator<Record> iterator() {
                if (csvRecordIterator == null) {
                    try {
                        csvRecordIterator = new CsvRecordIterator(readTextRecords(recordLength));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return csvRecordIterator;
            }
        };
    }

    private String[] getParameterNames(String[] columnNames) {
        final int[] sortedIndices = {latIndex, lonIndex, timeIndex, stationNameIndex};
        Arrays.sort(sortedIndices);

        final List<String> parameterNames = new ArrayList<String>();
        Collections.addAll(parameterNames, columnNames);
        for (int i = sortedIndices.length - 1; i >= 0; i--) {
            final int index = sortedIndices[i];
            if (index > -1) {
                parameterNames.remove(index);
            }
        }

        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    /**
     * Converts a string array into an array of object which are either a number ({@link Double}), a text ({@link String}),
     * a date/time value ({@link java.util.Date}). Empty text is converted to {@code null}.
     *
     * @param textValues The text values to convert.
     * @param types      The types.
     * @param dateFormat The date format to be used.
     *
     * @return The array of converted objects.
     */
    private static Object[] toObjects(String[] textValues, Class<?>[] types, DateFormat dateFormat) {
        final Object[] values = new Object[textValues.length];
        for (int i = 0; i < textValues.length; i++) {
            final String text = textValues[i];
            if (text != null && !text.isEmpty()) {
                final Object value;
                final Class<?> type = types[i];
                if (type != null) {
                    value = parse(text, type, dateFormat);
                } else {
                    value = parse(text, dateFormat);
                    if (value != null) {
                        types[i] = value.getClass();
                    }
                }
                values[i] = value;
            }
        }
        return values;
    }

    private static int indexOf(String[] textValues, String[] possibleValues) {
        for (String possibleValue : possibleValues) {
            for (int index = 0; index < textValues.length; index++) {
                if (possibleValue.equalsIgnoreCase(textValues[index])) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String[] splitRecordLine(String line, int recordLength) {
        int pos2;
        int pos1 = 0;
        ArrayList<String> strings = new ArrayList<String>(256);
        while ((pos2 = line.indexOf('\t', pos1)) >= 0) {
            strings.add(line.substring(pos1, pos2).trim());
            if (recordLength > 0 && strings.size() >= recordLength) {
                break;
            }
            pos1 = pos2 + 1;
        }
        strings.add(line.substring(pos1).trim());
        if (recordLength > 0) {
            return strings.toArray(new String[recordLength]);
        } else {
            return strings.toArray(new String[strings.size()]);
        }
    }

    private List<String[]> readTextRecords(int recordLength) throws IOException {
        final List<String[]> result = new ArrayList<String[]>();
        String line;
        while ((line = reader.readLine()) != null) {
            String trimLine = line.trim();
            if (!trimLine.startsWith("#") && !trimLine.isEmpty()) {
                result.add(splitRecordLine(line, recordLength));
                if (recordLength < 0) {
                    return result;
                }
            }
        }
        return result;
    }

    private static Object parse(String text, Class<?> type, DateFormat dateFormat) {
        if (type.equals(Double.class)) {
            try {
                return parseDouble(text);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        } else if (type.equals(String.class)) {
            return text;
        } else if (type.equals(Date.class)) {
            try {
                return dateFormat.parse(text);
            } catch (ParseException e) {
                return new Date(0L);
            }
        } else {
            throw new IllegalStateException("Unhandled data type: " + type);
        }
    }

    private static Object parse(String text, DateFormat dateFormat) {
        try {
            return parseDouble(text);
        } catch (NumberFormatException e) {
            try {
                return dateFormat.parse(text);
            } catch (ParseException e1) {
                return text;
            }
        }
    }

    private static Double parseDouble(String text) {
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            if (text.equalsIgnoreCase("nan")) {
                return Double.NaN;
            } else if (text.equalsIgnoreCase("inf") || text.equalsIgnoreCase("infinity")) {
                return Double.POSITIVE_INFINITY;
            } else if (text.equalsIgnoreCase("-inf") || text.equalsIgnoreCase("-infinity")) {
                return Double.NEGATIVE_INFINITY;
            } else {
                throw e;
            }
        }
    }

    private class CsvRecordIterator extends RecordIterator {

        List<String[]> records;
        private int currentRecord;

        private CsvRecordIterator(List<String[]> records) {
            currentRecord = 0;
            this.records = records;
        }

        @Override
        protected Record getNextRecord() {
            if (records.size() <= currentRecord) {
                return null;
            }
            String[] record = records.get(currentRecord);
            currentRecord++;

            if (getHeader().getColumnNames().length != record.length) {
                System.out.println("too few values " + Arrays.toString(record));
            }

            final Object[] values = toObjects(record, attributeTypes, dateFormat);

            final GeoPos location;
            if (header.hasLocation() && values[latIndex] instanceof Number && values[lonIndex] instanceof Number) {
                location = new GeoPos(((Number) values[latIndex]).floatValue(),
                                      ((Number) values[lonIndex]).floatValue());
            } else {
                location = null;
            }

            final Date time;
            if (header.hasTime() && values[timeIndex] instanceof Date) {
                time = values[timeIndex] instanceof Date ? (Date) values[timeIndex] : null;
            } else {
                time = null;
            }

            final String stationName;
            if (header.hasStationName()) {
                stationName = (String) values[stationNameIndex];
            } else {
                stationName = time.toString();
            }

            return new DefaultRecord(location, time, stationName, values);
        }

    }
}
