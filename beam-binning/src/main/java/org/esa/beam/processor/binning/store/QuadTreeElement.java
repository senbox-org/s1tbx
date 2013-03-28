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
package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;
import java.util.Vector;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
interface QuadTreeElement {

    /**
     * Loads the element. Parses the filenames vector for a valid filename.
     *
     * @param filenames a vector of available file names in the database.
     */
    public void load(Vector filenames) throws IOException;

    /**
     * Reads the data at the given location.
     * <p/>
     * @param rowcol a Point designating the read location.
     *
     * @param data the array to be filled with the data
     */
    public void read(Point rowcol, float[] data) throws IOException;

    /**
     * Writes the data to the given location.
     * <p/>
     * param rowcol a Point designating the write location.
     *
     * @param data the data to be written
     */
    public void write(Point rowcol, float[] data) throws IOException;

    /**
     * Closes the quad tree element
     */
    public void close() throws IOException;

    /**
     * Flushes the data of the element to disk.
     */
    public void flush() throws IOException;

}
