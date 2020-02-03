package org.esa.snap.binning.reader;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;

import static org.junit.Assert.*;

public class CacheEntryTest {

    private ArrayByte testArray;
    private CacheEntry cacheEntry;

    @Before
    public void setUp() {
        testArray = new ArrayByte(new int[]{1, 2});
        cacheEntry = new CacheEntry(testArray);
    }

    @Test
    public void testConstruction() {
        final Array data = cacheEntry.getData();
        assertSame(testArray, data);
        assertTrue(0 < cacheEntry.getLastAccess());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testGetDataUpdatesAccessTime() throws InterruptedException {
        final long initialTime = cacheEntry.getLastAccess();

        Thread.sleep(20);

        cacheEntry.getData();
        final long lastAccess = cacheEntry.getLastAccess();

        assertTrue(initialTime < lastAccess);
    }
}
