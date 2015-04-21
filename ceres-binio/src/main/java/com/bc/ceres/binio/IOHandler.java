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

package com.bc.ceres.binio;

import java.io.IOException;

/**
 * Provides the mechanism to read and write byte data at a specified position
 * within a random access data file, stream or memory buffer.
 */
public interface IOHandler {

    /**
     * Reads a sequence of up to {@code data.length} bytes into the
     * given array starting from the the given position. If the position
     * is greater than the file's current size then no bytes are read.
     * <p>
     * A read operation might not fill the array, and in fact it might not
     * read any bytes at all.  Whether or not it does so, depends upon the
     * state of the file, e.g. only bytes that remain in the file starting from
     * the given position will be read.
     *
     * @param context  The I/O context.
     * @param data     The data array into which bytes are to be transferred.
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative.
     * @throws IOException If an I/O error occurs.
     */
    void read(DataContext context, byte[] data, long position) throws IOException;

    /**
     * Writes a sequence of up to {@code data.length} bytes to the file,
     * starting at the given position. If the given
     * position is greater than the file's current size then the file will be
     * grown to accommodate the new bytes; the values of any bytes between the
     * previous end-of-file and the newly-written bytes are unspecified.
     *
     * @param context  The I/O context.
     * @param data     The data array from which bytes are to be transferred.
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative.
     * @throws IOException If an I/O error occurs.
     */
    void write(DataContext context, byte[] data, long position) throws IOException;
    
    /**
     * Returns the current size of the backing file, array or stream.
     * <p>This value may change, while writing to a growing file or array.
     * If the value is unknown {@code -1} will be returned.
     * 
     * @return Te maximum position.
     * @throws IOException If an I/O error occurs.
     */
    long getMaxPosition() throws IOException;
}
