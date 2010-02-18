package org.esa.beam.processor.binning.store;

import java.awt.Point;
import java.io.IOException;

import junit.framework.TestCase;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.FloatArrayBin;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 19.07.2005
 * Time: 15:48:56
 * To change this template use File | Settings | File Templates.
 */
public class MemoryBinStoreTest extends TestCase {
    private BinStore store;
    private Bin bin;
    private Point point0;
    private Point point1;

    @Override
    public void setUp() {
        store = new MemoryBinStore(2, 2, 5);
        bin = new FloatArrayBin(new int[]{2, 3});
        point0 = new Point(0, 0);
        point1 = new Point(1, 1);
    }

    public void testAll() throws IOException {

        bin.load(new float[]{1f, 2f, 3f, 4f, 5f});
        store.write(point0, bin);
        assertTrue("contains data", bin.containsData());
        bin.clear();
        assertFalse("contains no data", bin.containsData());
        store.read(point1, bin);
        assertFalse("contains no data", bin.containsData());
        store.read(point0, bin);
        assertTrue("contains data", bin.containsData());
        bin.setBandIndex(0);
        assertEquals("1f", 1f, bin.read(0), 0.00001f);
        assertEquals("2f", 2f, bin.read(1), 0.00001f);
        bin.setBandIndex(1);
        assertEquals("3f", 3f, bin.read(0), 0.00001f);
        assertEquals("4f", 4f, bin.read(1), 0.00001f);
        assertEquals("5f", 5f, bin.read(2), 0.00001f);
    }
}
