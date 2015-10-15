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
package org.esa.snap.core.util.math;

import junit.framework.TestCase;

/**
 * Tests for class {@link Array}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ArrayTest extends TestCase {

    public void testDoubleCopyTo() {
        final Array array = new Array.Double(new double[]{1, 2, 3, 4, 5});
        final double[] dest = new double[3];

        array.copyTo(1, dest, 0, 3);

        assertEquals(2.0, dest[0], 0.0);
        assertEquals(3.0, dest[1], 0.0);
        assertEquals(4.0, dest[2], 0.0);
    }

    public void testFloatCopyTo() {
        final Array array = new Array.Float(new float[]{1, 2, 3, 4, 5});
        final double[] dest = new double[3];

        array.copyTo(1, dest, 0, 3);

        assertEquals(2.0, dest[0], 0.0);
        assertEquals(3.0, dest[1], 0.0);
        assertEquals(4.0, dest[2], 0.0);
    }
}
