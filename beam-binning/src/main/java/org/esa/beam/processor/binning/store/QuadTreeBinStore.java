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

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.util.Guardian;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class QuadTreeBinStore implements BinStore {
    // the maximum tile size (width and height) in bins
    private static final int TILE_SIZE = 64;

    private QuadTreeFile qtFile;

    private float[] tempBinContent;

    /**
     * Creates a new QuadTreeBinStore.
     *
     * @param dbDir
     * @param dbName
     * @param width
     * @param height
     * @param numVarsPerBin
     * @throws IOException
     */
    public QuadTreeBinStore(File dbDir, String dbName, int width, int height, int numVarsPerBin) throws IOException {
        final int numBuffer = width / TILE_SIZE + 1;

        qtFile = new QuadTreeFile();
        File quadTreeDbDir = new File(dbDir, dbName);
        qtFile.create(quadTreeDbDir, width, height, TILE_SIZE, numBuffer, numVarsPerBin);
        qtFile.open(quadTreeDbDir);

        tempBinContent = new float[numVarsPerBin];
    }

    /**
     * Opens an existing QuadTreeBinStore.
     *
     * @param dbDir
     * @param dbName
     * @throws IOException
     */
    public QuadTreeBinStore(File dbDir, String dbName) throws  IOException {
        File quadTreeDbDir = new File(dbDir, dbName);
        qtFile = new QuadTreeFile();
        checkLocation(quadTreeDbDir);

        qtFile.open(quadTreeDbDir);

        tempBinContent = new float[qtFile.getNumberOfVariables()];
    }

    public void write(Point rowCol, Bin bin) throws IOException {
        bin.save(tempBinContent);
        qtFile.write(rowCol, tempBinContent);
    }

    public void read(Point rowCol, Bin bin) throws IOException {
        qtFile.read(rowCol, tempBinContent);
        bin.load(tempBinContent);
    }

    /**
     * Flushes buffered content to disk.
     */
    public void flush() throws IOException {
        qtFile.flush();
    }

    /**
     * Closes the database
     */
    public void close() throws IOException {
        qtFile.close();
    }

    /**
     * Deletes the database.
     */
    public void delete() throws IOException {
        qtFile.close();
        qtFile.delete();
    }

    /**
     * Checks whether the directory passed in is a valid directory for a quad tree file.
     *
     * @param location the desired location of the quad tree file
     */
    private void checkLocation(File location) throws IOException {
        Guardian.assertNotNull("location", location);

        if (!location.exists()) {
            throw new IOException("database location does not exist: '" + location.toString() + "'");
        }

        if (!location.isDirectory()) {
            throw new IOException("database location must be a directory: '" + location.toString() + "'");
        }
    }
}
