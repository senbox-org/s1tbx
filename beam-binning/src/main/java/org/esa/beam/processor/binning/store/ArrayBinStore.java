package org.esa.beam.processor.binning.store;

import java.io.IOException;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinLocator;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 19.07.2005
 * Time: 15:14:37
 * To change this template use File | Settings | File Templates.
 */
public class ArrayBinStore extends AbstractLinearBinStore {
    private int numVarsPerBin;
    private float[] tempBinContent;
    private float[] data;

    public ArrayBinStore(BinLocator locator, int numVarsPerBin) {
        this.locator = locator;
        this.numVarsPerBin = numVarsPerBin;
        tempBinContent = new float[numVarsPerBin];
        data = new float[locator.getNumCells() * numVarsPerBin];
    }

    public void write(int index, Bin bin) {
        bin.save(tempBinContent);
        System.arraycopy(tempBinContent, 0, data, index * numVarsPerBin, numVarsPerBin);
    }

    public void read(int index, Bin bin) {
        System.arraycopy(data, index * numVarsPerBin, tempBinContent, 0, numVarsPerBin);
        bin.load(tempBinContent);
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }

    public void delete() throws IOException {
        data = null;
    }
}
