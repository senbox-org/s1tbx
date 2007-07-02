package org.esa.beam.dataio.chris.internal;

import junit.framework.TestCase;

import java.awt.Rectangle;

/**
 * Tests for class {@link DropoutCorrection}.
 *
 * @author Ralf Quast
 * @version $Revision: 1.3 $ $Date: 2007/04/18 16:01:35 $
 */
public class DropoutCorrectionTest extends TestCase {

    private int[][] data;
    private short[][] mask;

    @Override
    protected void setUp() throws Exception {
        data = new int[][]{{1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1}};
        mask = new short[3][9];
    }

    @Override
    protected void tearDown() throws Exception {
        data = null;
        mask = null;
    }

    public void testDropoutCorrection() {
        final int adjacentBandCount = 1;

        final DropoutCorrection dropoutCorrection = new DropoutCorrection(4, adjacentBandCount, 3, 3);
        dropoutCorrection.perform(data, mask, 1, new Rectangle(0, 0, 3, 3));

        data[1][4] = 0;
        mask[1][4] = 1;

        dropoutCorrection.perform(data, mask, 1, new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[0][4]);
        assertEquals(1, data[1][4]);
        assertEquals(4, mask[1][4]);

        data[0][4] = 0;
        mask[0][4] = 1;

        dropoutCorrection.perform(data, mask, 1, new Rectangle(0, 0, 3, 3));

        assertEquals(0, data[0][4]);
        assertEquals(1, mask[0][4]);

        data[1][1] = 0;
        mask[1][1] = 1;

        dropoutCorrection.perform(data, mask, 1, new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[1][1]);
        assertEquals(4, mask[1][1]);
    }

}

