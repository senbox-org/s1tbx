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

package org.esa.beam.csv.dataio;

/**
 * Interface providing methods for parsing a csv product source.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface CsvProductSourceParser {

    /**
     * Triggers parsing of the records. Before calling this method, {@link CsvProductSourceParser#parse()} must
     * have been called.
     *
     * @throws IllegalStateException         if this method is called before {@link CsvProductSourceParser#parse()} has
     *                                       been called.
     * @throws CsvProductFile.ParseException if something goes wrong.
     */
    void parseRecords() throws CsvProductFile.ParseException;

    /**
     * @return A view on the {@link CsvProductSource} parsed using this interface.
     *
     * @throws CsvProductFile.ParseException if something goes wrong.
     */
    CsvProductSource parse() throws CsvProductFile.ParseException;

}
