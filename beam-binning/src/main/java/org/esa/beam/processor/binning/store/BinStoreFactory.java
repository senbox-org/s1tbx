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

package org.esa.beam.processor.binning.store;

import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.database.BinLocator;

import java.io.File;
import java.io.IOException;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class BinStoreFactory {

    /**
     * Hide the default constructor.
     */
    private BinStoreFactory() {
    }

    /**
     * Retrieves the one and only instance of the <code>BinStoreFactory</code>.
     * If the instance does not exist yet, it creates one.
     */
    public static BinStoreFactory getInstance() {
        return Holder.instance;
    }

    public BinStore createSpatialStore(File dbDir, String fileName, int width, int height, int numVarsPerBin) throws IOException {
        BinStore store = null;
        long size = ((long)width) * ((long)height) * ((long)numVarsPerBin);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("numVarsPerBin = " + numVarsPerBin);
        System.out.println("SIZE (in floats) = " + size);
        long memLimit = Long.getLong("beam.binning.memoryLimit.spatialStore", 20000000);
        if (size < memLimit) {
            store = new MemoryBinStore(width, height, numVarsPerBin);
        } else {
            store = new QuadTreeBinStore(dbDir, fileName, width, height, numVarsPerBin);
        }
        return store;
    }

    public BinStore createTemporalStore(L3Context context, String dbName, int numVarsPerBin) throws IOException {
        BinStore store = null;
        BinLocator locator = context.getLocator();
        // choose database implementation depending on the desired cell size
//        if (context.getGridCellSize() > BinDatabaseConstants.SEA_WIFS_CELL_SIZE * 2) {
//            store = new SimpleBinStore(context.getDatabaseDir(), dbName, locator, numVarsPerBin);
//        } else {
            store = new QuadTreeBinStore(context.getDatabaseDir(), dbName, locator.getWidth(), locator.getHeight(),
                    numVarsPerBin);
//        }
//        store = new ArrayBinStore(locator, numVarsPerBin);
        return store;
    }

    public BinStore openTemporalStore(L3Context context, String dbName) throws IOException {
        BinLocator locator = context.getLocator();
        BinStore store = null;
        String storageType = context.getStorageType();
        if (storageType.equals(QuadTreeBinStore.class.getName())) {
            store = new QuadTreeBinStore(context.getDatabaseDir(), dbName);
        } else if (storageType.equals(SimpleBinStore.class.getName())) {
            store = new SimpleBinStore(context.getDatabaseDir(), dbName, locator);
        } else {
            throw new IOException("Unsupported storage type: " + storageType);
        }
        return store;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final BinStoreFactory instance = new BinStoreFactory();
    }
}