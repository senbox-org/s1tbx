package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.database.Bin;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 18.07.2005
 * Time: 15:14:11
 * To change this template use File | Settings | File Templates.
 */
public class MemoryBinStore implements BinStore {
    private float[][][] data;

    public MemoryBinStore(int width, int height, int numVarsPerBin) {
        data = new float[width][height][numVarsPerBin];
    }

    public void open() throws IOException, ProcessorException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void write(Point rowCol, Bin bin) {
        bin.save(data[rowCol.x][rowCol.y]);
    }

    public void read(Point rowCol, Bin bin) {
        bin.load(data[rowCol.x][rowCol.y]);
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }

    public void delete() throws IOException {
        data = null;
    }
}
