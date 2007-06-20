package org.esa.beam.processor.binning.store;

import java.awt.Point;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinLocator;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 19.07.2005
 * Time: 15:06:17
 * To change this template use File | Settings | File Templates.
 */
abstract public class AbstractLinearBinStore implements BinStore {

    protected BinLocator locator;

    public void write(Point rowCol, Bin bin) {
        write(locator.rowColToIndex(rowCol), bin);
    }

    abstract public void write(int index, Bin bin);

    public void read(Point rowCol, Bin bin) {
        read(locator.rowColToIndex(rowCol), bin);
    }

    abstract public void read(int index, Bin bin);
}
