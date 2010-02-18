/*
 * $Id: QuadTreeElement.java,v 1.1 2006/09/11 10:47:32 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;
import java.util.Vector;

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
