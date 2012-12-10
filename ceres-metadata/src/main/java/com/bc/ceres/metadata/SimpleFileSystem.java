/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.metadata;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * An abstraction of the used filesystem.
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public interface SimpleFileSystem {

    /**
     * Returns a reader for the given path.
     *
     * @param path The path in the filesystem
     * @return a reader instance
     * @throws IOException If an I/O error occurs
     */
    Reader createReader(String path) throws IOException;

    /**
     * Returns a writer for the given path.
     *
     * @param path The path in the filesystem
     * @return a writer instance
     * @throws IOException If an I/O error occurs
     */
    Writer createWriter(String path) throws IOException;

    /**
     * Lists all elements inside the given directory path.
     *
     * @param path The path in the filesystem
     * @return An array of strings naming the files and directories in the
     *         directory denoted by this directory path.  The array will be
     *         empty if the directory is empty.  Returns {@code null} if
     *         this path does not denote a directory.
     * @throws IOException If an I/O error occurs
     */
    String[] list(String path) throws IOException;

    /**
     * Checks, if the given path is a file or not (then e.g. a directory)
     *
     * @param path to a file or directory
     * @return true, if given path is a file on the fs
     */
    boolean isFile(String path);
}
