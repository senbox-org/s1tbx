package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;

import org.esa.beam.processor.binning.database.Bin;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 15.07.2005
 * Time: 15:21:42
 * To change this template use File | Settings | File Templates.
 */
public interface BinStore {

    public void write(Point rowCol, Bin bin) throws IOException;

    public void read(Point rowCol, Bin bin) throws IOException;

    public void flush() throws IOException;

    public void close() throws IOException;

    public void delete() throws IOException;
}
