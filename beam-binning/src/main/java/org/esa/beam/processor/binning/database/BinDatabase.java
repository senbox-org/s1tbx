package org.esa.beam.processor.binning.database;

import java.awt.Point;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 15.07.2005
 * Time: 15:01:50
 * To change this template use File | Settings | File Templates.
 */
public interface BinDatabase {
//    public void open() throws IOException;
    public void flush() throws IOException;
//    public void close() throws IOException;
    public void delete() throws IOException;

    /**
     * Creates a bin that can hold the in this databse stored data.
     *
     * @return a new created bin.
     */
    public Bin createBin();

    public void read(Point point, Bin bin) throws IOException;
    public void write(Point point, Bin bin) throws IOException;

    public int getRowOffset();
    public int getColOffset();

    public int getWidth();
    public int getHeight();
}
