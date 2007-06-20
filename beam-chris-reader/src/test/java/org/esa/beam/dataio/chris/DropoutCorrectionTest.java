package org.esa.beam.dataio.chris;

import junit.framework.TestCase;

/**
 * Tests for class {@link DropoutCorrection}.
 *
 * @author Ralf Quast
 * @version $Revision: 1.3 $ $Date: 2007/04/18 16:01:35 $
 */
public class DropoutCorrectionTest extends TestCase {

    public void testDropoutCorrection() {
        final int[][] data = new int[3][9];
        final short[][] mask = new short[3][9];
        final double[] weights = new double[9];
        final int adjacentBandCount = 1;

        final DropoutCorrection dropoutCorrection = new DropoutCorrection(weights, adjacentBandCount, 3, 3);
        dropoutCorrection.perform(data, mask, 1, 0, 0, 3, 3);

        assertEquals(0.0, data[0][0], 0.0);
        assertEquals(0.0, data[0][1], 0.0);
        assertEquals(0.0, data[0][2], 0.0);
        assertEquals(0.0, data[0][3], 0.0);
        assertEquals(0.0, data[0][4], 0.0);
        assertEquals(0.0, data[0][5], 0.0);
        assertEquals(0.0, data[0][6], 0.0);
        assertEquals(0.0, data[0][7], 0.0);
        assertEquals(0.0, data[0][8], 0.0);
        assertEquals(0.0, data[1][0], 0.0);

        assertEquals(0, mask[0][0]);
        assertEquals(0, mask[0][1]);
        assertEquals(0, mask[0][2]);
        assertEquals(0, mask[0][3]);
        assertEquals(0, mask[0][4]);
        assertEquals(0, mask[0][5]);
        assertEquals(0, mask[0][6]);
        assertEquals(0, mask[0][7]);
        assertEquals(0, mask[0][8]);
        assertEquals(0, mask[1][0]);

        data[1][4] = 1;
        mask[1][4] = 1;

        dropoutCorrection.perform(data, mask, 1, 0, 0, 3, 3);

        assertEquals(0.0, data[0][4], 0.0);
        assertEquals(0, data[1][4]);
        assertEquals(1, mask[1][4]);

        weights[0] = 1.0;
        weights[1] = 1.0;
        weights[2] = 1.0;
        weights[3] = 1.0;
        weights[5] = 1.0;
        weights[6] = 1.0;
        weights[7] = 1.0;
        weights[8] = 1.0;

        data[0][4] = 1;
        mask[0][4] = 1;

        dropoutCorrection.perform(data, mask, 1, 0, 0, 3, 3);

        assertEquals(0.0, data[1][4], 0.0);
        assertEquals(5, mask[1][4]);

        data[1][1] = 1;
        mask[1][1] = 1;
        mask[1][4] = 0;

        dropoutCorrection.perform(data, mask, 1, 0, 0, 3, 3);

        assertEquals(0.0, data[1][1], 0.0);
        assertEquals(5, mask[1][1]);
    }


}

