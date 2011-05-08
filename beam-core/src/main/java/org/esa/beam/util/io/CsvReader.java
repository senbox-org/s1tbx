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
package org.esa.beam.util.io;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

/**
 * A <code>CsvReader</code> instance is used read text files with CSV (comma separated values) format.
 * <p/>
 * <_p> This reader interpretes each line of a text file as record (also empty lines). The fields of the record are
 * delimited by a single separator character which must not necessarily be a comma.
 *
 * @author Norman Fomferra
 * @version 1.0
 */
public class CsvReader extends LineNumberReader {

    /**
     * The column separator characters.
     */
    private final char[] _separators;
    /**
     * Empty lines ignored?
     */
    private final boolean _ignoreEmptyLines;
    /**
     * The comment prefix string (if any)
     */
    private final String _commentPrefix;

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
     * @param commentPrefix    if not <code>null</code>, the prefix string for commment lines
     */
    public CsvReader(Reader reader, char[] separators, boolean ignoreEmptyLines, String commentPrefix) {
        super(reader);
        _separators = separators;
        _ignoreEmptyLines = ignoreEmptyLines;
        _commentPrefix = commentPrefix;
    }

    /**
     * Gets the column separator characters.
     */
    public final char[] getSeparators() {
        return _separators;
    }

    /**
     * Are empty lines ignored?
     */
    public final boolean ignoresEmptyLines() {
        return _ignoreEmptyLines;
    }

    /**
     * Gets the comment prefix string (if any).
     */
    public final String getCommentPrefix() {
        return _commentPrefix;
    }

    /**
     * Reads a record info from the database. A record is represented by a complete line in the CSV formatted text
     * file.
     * <p/>
     * <_p> Leading and trailing whitespaces removed from each column value. For empty lines, the method returns an
     * array of the length zero. The method returns <code>null</code> if the end of file has been reached.
     *
     * @return a record containing the tokens delimitted by the separator character passed to the constructor
     *
     * @throws IOException if an I/O error occurs
     */
    public String[] readRecord() throws IOException {

        String line;

        while (true) {
            line = readLine();
            if (line == null) {
                return null;
            }
            line = line.trim();
            if (ignoresEmptyLines() && line.length() == 0) {
                // Ok, next line
            } else if (getCommentPrefix() != null && line.startsWith(getCommentPrefix())) {
                // Ok, next line
            } else {
                break;
            }
        }

        StringTokenizer st = new StringTokenizer(line, new String(getSeparators()));

        String[] tokens = new String[st.countTokens()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = st.nextToken().trim();
        }

        return tokens;
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
     *
     * @throws IOException if an I/O error occurs
     */
    public List<String[]> readStringRecords() throws IOException {
        Vector<String[]> vector = new Vector<String[]>(256);
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
}

