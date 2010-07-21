/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.chris.internal;

import junit.framework.TestCase;
import org.esa.beam.dataio.chris.Flags;

import java.awt.Rectangle;

/**
 * Tests for class {@link DropoutCorrection}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
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

        final Rectangle region = new Rectangle(0, 0, 3, 3);
        dropoutCorrection.compute(new int[][]{data[1], data[0], data[2]},
                                  new short[][]{mask[1], mask[0], mask[2]}, 3, 3, region);

        assertEquals(Flags.DROPOUT.getMask(), data[1][4]);
        assertEquals(Flags.DROPOUT_CORRECTED.getMask(), mask[1][4]);

        data[0][0] = 0;
        mask[0][0] = 1;

        dropoutCorrection.compute(new int[][]{data[0], data[1]},
                                  new short[][]{mask[0], mask[1]}, 3, 3, region);

        assertEquals(Flags.DROPOUT.getMask(), data[0][0]);
        assertEquals(Flags.DROPOUT_CORRECTED.getMask(), mask[0][0]);

        data[2][8] = 0;
        mask[2][8] = 1;

        dropoutCorrection.compute(new int[][]{data[2], data[1]},
                                  new short[][]{mask[2], mask[1]}, 3, 3, region);

        assertEquals(Flags.DROPOUT.getMask(), data[2][8]);
        assertEquals(Flags.DROPOUT_CORRECTED.getMask(), mask[2][8]);
    }
}

