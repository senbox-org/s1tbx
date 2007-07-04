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
        final int neighboringBandCount = 1;
        final DropoutCorrection dropoutCorrection = new DropoutCorrection(4, 3, 3);

        data[1][4] = 0;
        mask[1][4] = 1;

        dropoutCorrection.perform(data, mask, 1, neighboringBandCount, new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[1][4]);
        assertEquals(4, mask[1][4]);

        data[0][2] = 0;
        mask[0][2] = 1;

        dropoutCorrection.perform(data, mask, 0, neighboringBandCount, new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[0][2]);
        assertEquals(4, mask[0][2]);

        data[2][1] = 0;
        mask[2][1] = 1;

        dropoutCorrection.perform(data, mask, 2, neighboringBandCount, new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[2][1]);
        assertEquals(4, mask[2][1]);
    }

    public void testDropoutCorrection2() {
        final DropoutCorrection dropoutCorrection = new DropoutCorrection(4, 3, 3);

        data[1][4] = 0;
        mask[1][4] = 1;

        dropoutCorrection.perform(data[1], mask[1],
                                  new int[][]{data[0], data[2]},
                                  new short[][]{mask[0], mask[2]},
                                  data[1], mask[1], new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[1][4]);
        assertEquals(4, mask[1][4]);

        data[0][2] = 0;
        mask[0][2] = 1;

        dropoutCorrection.perform(data[0], mask[0],
                                  new int[][]{data[1]},
                                  new short[][]{mask[1]},
                                  data[0], mask[0], new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[0][2]);
        assertEquals(4, mask[0][2]);

        data[2][1] = 0;
        mask[2][1] = 1;

        dropoutCorrection.perform(data[2], mask[2],
                                  new int[][]{data[1]},
                                  new short[][]{mask[1]},
                                  data[2], mask[2], new Rectangle(0, 0, 3, 3));

        assertEquals(1, data[2][1]);
        assertEquals(4, mask[2][1]);
    }

}

