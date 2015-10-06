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
package org.esa.snap.core.util.io;

import com.bc.ceres.core.Assert;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A <code>CsvReader</code> instance is used read text files with CSV (comma separated values) format.
 * <p>
 * This reader interprets each line of a text file as record (also empty lines). The fields of the record are
 * delimited by a single separator character which must not necessarily be a comma.
 *
 * @author Norman Fomferra
 * @version 1.1
 */
public class CsvReader extends LineNumberReader {

    private static final String WHITESPACE_REGEX = "\\s+";
    private static final String REGEX_TAB = "\\t";
    private static final String REGEX_CHARS = "[\\^$.|?*+()";
    private static final String[] EMPTY_RECORD = new String[0];

    // todo - actually we only want a single character here, deprecate the array and its accessors (nf, 2012-04-12)
    /**
     * The column separator characters.
     */
    private final char[] separators;
    /**
     * Empty lines ignored?
     */
    private final boolean ignoreEmptyLines;
    /**
     * The comment prefix string (if any)
     */
    private final String commentPrefix;

    private final Pattern regExPattern;
    private boolean matchingWhiteSpaces;

    /**
     * Constructs a CSV reader using the given column separator characters. The new reader will not ignore empty lines
     * and does not recognize comment lines.
     *
     * @param reader     the underlying reader to be wrapped
     * @param separators the column separator characters
     */
    public CsvReader(Reader reader, char[] separators) {
        this(reader, separators, false, null);
    }

    /**
     * Constructs a CSV reader using the given column separator characters and the format properties.
     *
     * @param reader           the underlying reader to be wrapped
     * @param separators       the column separator characters
     * @param ignoreEmptyLines if <code>true</code>, empty lines are ignored
     * @param commentPrefix    if not <code>null</code>, the prefix string for comment lines
     */
    public CsvReader(Reader reader, char[] separators, boolean ignoreEmptyLines, String commentPrefix) {
        super(reader);
        Assert.notNull(separators, "separators");
        Assert.argument(separators.length > 0, "separators.length > 0");
        this.separators = separators.clone();
        this.ignoreEmptyLines = ignoreEmptyLines;
        this.commentPrefix = commentPrefix;
        this.regExPattern = createPattern(separators);
        this.matchingWhiteSpaces = regExPattern.pattern().equals(WHITESPACE_REGEX);
    }

    /**
     * @return The column separator characters.
     */
    public final char[] getSeparators() {
        return separators;
    }

    /**
     * @return Are empty lines ignored?
     */
    public final boolean ignoresEmptyLines() {
        return ignoreEmptyLines;
    }

    /**
     * Gets the comment prefix string (if any).
     * @return The comment prefix string, if any. {@code null} otherwise.
     */
    public final String getCommentPrefix() {
        return commentPrefix;
    }

    /**
     * Reads a record info from the database. A record is represented by a complete line in the CSV formatted text
     * file.
     * <p>
     * Leading and trailing whitespaces removed from each column value. For empty lines, the method returns an
     * array of the length zero. The method returns <code>null</code> if the end of file has been reached.
     *
     * @return a record containing the tokens delimited by the separator character passed to the constructor
     * @throws IOException if an I/O error occurs
     */
    public String[] readRecord() throws IOException {
        while (true) {
            String line = readLine();
            if (line == null) {
                return null;
            } else if (getCommentPrefix() == null
                    || !line.startsWith(getCommentPrefix())) {
                String[] record = split(line);
                if (record.length != 0
                        || !ignoresEmptyLines()) {
                    return record;
                }
            }
        }
    }

    public double[] readDoubleRecord() throws IOException {
        String[] stringRecord = readRecord();
        if (stringRecord == null) {
            return null;
        }
        double[] doubleRecord = new double[stringRecord.length];
        for (int i = 0; i < doubleRecord.length; i++) {
            try {
                doubleRecord[i] = Double.parseDouble(stringRecord[i]);
            } catch (NumberFormatException e) {
                throw new IOException(e);
            }
        }
        return doubleRecord;
    }

    /**
     * Reads the complete file from the current position on. If the <code>readLineRecord</code> has not previously been
     * called the method reads all records from the beginning of the file. For empty files, the method returns an vector
     * having a zero size.
     *
     * @return an vector of <code>String[]</code> records containing the tokens delimitted by the separator character
     *         passed to the constructor
     * @throws IOException if an I/O error occurs
     */
    public List<String[]> readStringRecords() throws IOException {
        ArrayList<String[]> vector = new ArrayList<String[]>(256);
        String[] record;
        while ((record = readRecord()) != null) {
            vector.add(record);
        }
        vector.trimToSize();
        return vector;
    }

    public List<double[]> readDoubleRecords() throws IOException {
        ArrayList<double[]> vector = new ArrayList<double[]>(256);
        double[] record;
        while ((record = readDoubleRecord()) != null) {
            vector.add(record);
        }
        vector.trimToSize();
        return vector;
    }

    private String[] split(String line) {
        if (line.isEmpty()) {
            return EMPTY_RECORD;
        }
        if (matchingWhiteSpaces) {
            line = line.trim();
            if (line.isEmpty()) {
                return EMPTY_RECORD;
            }
            return regExPattern.split(line, 0);
        } else {
            return trim(regExPattern.split(line, -1));
        }
    }

    private String[] trim(String[] splits) {
        for (int i = 0; i < splits.length; i++) {
            splits[i] = splits[i].trim();
        }
        return splits;
    }

    private static Pattern createPattern(char[] separators) {
        String regex = createRegex(separators);
        return Pattern.compile(regex);
    }

    private static String createRegex(char[] separators) {
        String s = new String(separators);
        if (s.indexOf(' ') >= 0 && s.trim().isEmpty()) {
            return WHITESPACE_REGEX;
        } else if (separators.length == 1) {
            return encode(separators[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (char separator : separators) {
                sb.append(encode(separator));
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private static String encode(char separator) {
        if (separator == '\t') {
            return REGEX_TAB;
        }
        int i = REGEX_CHARS.indexOf(separator);
        return i >= 0 ? "\\" + separator : "" + separator;
    }

}
