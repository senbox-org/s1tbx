package org.esa.beam.processor.binning.database;

import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.store.BinStore;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 18.07.2005
 * Time: 10:09:19
 * To change this template use File | Settings | File Templates.
 */
abstract public class AbstractBinDatabase implements BinDatabase {

    // some key values for the properties file
    protected static final String COL_MIN_KEY = "colMin";
    protected static final String ROW_MIN_KEY = "rowMin";
    protected static final String COL_MAX_KEY = "colMax";
    protected static final String ROW_MAX_KEY = "rowMax";

    protected BinStore store;
    protected BinLocator locator;
    protected L3Context context;
    protected int[] numVarsPerBin;

    protected int rowMin;
    protected int rowMax;
    protected int colMin;
    protected int colMax;

    protected int width;

    protected Logger logger;

    public void flush() throws IOException {
        store.flush();
    }

    public void delete() throws IOException {
        store.delete();
    }

    /**
     * Sets the per band needed variables.
     */
    public void setNumVarsPerBand(int[] numVarsPerBand) {
        this.numVarsPerBin = numVarsPerBand;
    }

    /**
     * Creates a bin that can hold the in this databse stored data.
     *
     * @return a new created bin.
     */
    public Bin createBin() {
        return new FloatArrayBin(numVarsPerBin);
    }

    /**
     * Retrieves the row offset (in grid coordinate system) to the first bin containing data.
     */
    public int getRowOffset() {
        return rowMin;
    }

    /**
     * Retrieves the col offset (in grid coordinate system) to the first bin containing data.
     */
    public int getColOffset() {
        return colMin;
    }

    /**
     * Retrieves the width of the accumulating db
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retrieves the height of the accumulating db
     */
    public int getHeight() {
        return rowMax - rowMin + 1;
    }

    public String getStorageType() {
        return store.getClass().getName();
    }

    /**
     * Creates a file filter for the database files.
     */
    static public FileFilter createDbFileFilter() {
        return new BeamFileFilter(BinDatabaseConstants.FILE_EXTENSION_DESCRIPTION, BinDatabaseConstants.FILE_EXTENSION,
                                  BinDatabaseConstants.FILE_EXTENSION_DESCRIPTION + BinDatabaseConstants.FILE_EXTENSION);
    }

    /**
     * Initializes the internal minimum maximum tracing fields.
     */
    protected void initializeMinMax() {
        rowMin = Integer.MAX_VALUE;
        colMin = Integer.MAX_VALUE;
        rowMax = Integer.MIN_VALUE;
        colMax = Integer.MIN_VALUE;
    }

    /**
     * Sum up the vars needed for each bin into one number.
     *
     * @return the sum of all the needed vars.
     */
    protected int sumVarsPerBin() {
        int result = 0;
        for (int bandIndex = 0; bandIndex < numVarsPerBin.length; bandIndex++) {
            result += numVarsPerBin[bandIndex];
        }
        return result;
    }
}
