/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio;

import java.io.IOException;

/**
 * Interface providing methods for parsing a csv source.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface CsvSourceParser {

    /**
     * Triggers parsing of the records. Before calling this method, {@link CsvSourceParser#parseMetadata()} must
     * have been called.
     *
     * @param offset     the offset from which to start parsing.
     * @param numRecords the number of records to parse. If -1 is given, all records are parsed.
     * @param colName    the name of the column.
     * @throws IllegalStateException if this method is called before {@link CsvSourceParser#parseMetadata()} has
     *                               been called.
     * @throws IOException           if something goes wrong.
     */
    Object[] parseRecords(int offset, int numRecords, String colName) throws IOException;

    /**
     * Triggers parsing of the records. Before calling this method, {@link CsvSourceParser#parseMetadata()} must
     * have been called.
     *
     * @param offset     the offset from which to start parsing.
     * @param numRecords the number of records to parse. If -1 is given, all records are parsed.
     * @throws IllegalStateException if this method is called before {@link CsvSourceParser#parseMetadata()} has
     *                               been called.
     * @throws IOException           if something goes wrong.
     */
    void parseRecords(int offset, int numRecords) throws IOException;

    /**
     * @return A view on the {@link CsvSource} parsed using this interface.
     * @throws IOException if something goes wrong.
     */
    CsvSource parseMetadata() throws IOException;

    /**
     * Tries to interpet the first record.
     *
     * @throws IOException if the first record cannot be interpreted.
     */
    void checkReadingFirstRecord() throws IOException;

    /**
     * Closes the parser and its associated data sources.
     */
    void close();

}
