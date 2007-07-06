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
        final DropoutCorrection dropoutCorrection = new DropoutCorrection();

        data[1][4] = 0;
        mask[1][4] = 1;

        final Rectangle roi = new Rectangle(0, 0, 3, 3);
        dropoutCorrection.compute(new int[][]{data[1], data[0], data[2]},
                                  new short[][]{mask[1], mask[0], mask[2]}, 3, 3, roi);

        assertEquals(1, data[1][4]);
        assertEquals(4, mask[1][4]);

        data[0][0] = 0;
        mask[0][0] = 1;

        dropoutCorrection.compute(new int[][]{data[0], data[1]},
                                  new short[][]{mask[0], mask[1]}, 3, 3, roi);

        assertEquals(1, data[0][0]);
        assertEquals(4, mask[0][0]);

        data[2][8] = 0;
        mask[2][8] = 1;

        dropoutCorrection.compute(new int[][]{data[2], data[1]},
                                  new short[][]{mask[2], mask[1]}, 3, 3, roi);

        assertEquals(1, data[2][8]);
        assertEquals(4, mask[2][8]);
    }

}

