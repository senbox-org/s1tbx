package org.esa.beam.processor.binning.store;

import java.io.File;
import java.io.IOException;

import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.database.BinLocator;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 19.07.2005
 * Time: 17:59:34
 * To change this template use File | Settings | File Templates.
 */
public class BinStoreFactory {

    private static BinStoreFactory _instance = null;

    /**
     * Hide the default constructor.
     */
    private BinStoreFactory() {
    }

    /**
     * Retrieves the one and only instance of the <code>BinStoreFactory</code>.
     * If the instance does not exist yet, it creates one.
     */
    static public BinStoreFactory getInstance() {
        if (_instance == null) {
            _instance = new BinStoreFactory();
        }

        return _instance;
    }

    public BinStore createSpatialStore(File dbDir, String fileName, int width, int height, int numVarsPerBin) throws IOException {
        BinStore store = null;
        long size = width * height * numVarsPerBin;
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("numVarsPerBin = " + numVarsPerBin);
        System.out.println("SIZE (in floats) = " + size);
        if (size < 20E6) {
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
}