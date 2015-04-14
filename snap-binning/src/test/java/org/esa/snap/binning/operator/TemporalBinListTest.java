package org.esa.snap.binning.operator;

import org.esa.snap.binning.TemporalBin;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class TemporalBinListTest {


    @Test
    public void testList() throws Exception {
        int numberOfBins = 1000;
        TemporalBinList binList = new TemporalBinList(numberOfBins, 5, 300);
        try {
            for (int i = 0; i < numberOfBins; i++) {
                binList.add(new TemporalBin(i, 2));
            }

            assertEquals(0, binList.get(0).getIndex());
            assertEquals(300, binList.get(300).getIndex());
            assertEquals(999, binList.get(999).getIndex());
        } finally {
            binList.close();
        }
    }

    @Test
    public void testComputeBinsPerFile() throws Exception {
        assertEquals(15000, TemporalBinList.computeBinsPerFile(235000, 100, 15000));
        assertEquals(23500, TemporalBinList.computeBinsPerFile(2350000, 100, TemporalBinList.DEFAULT_BINS_PER_FILE));
    }
}
