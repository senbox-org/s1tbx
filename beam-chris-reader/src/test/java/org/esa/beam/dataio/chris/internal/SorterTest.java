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

/**
 * Test methods for class {@link Sorter}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SorterTest extends TestCase {

    public void testNthElement() {

        try {
            Sorter.nthElement(null, 0);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Sorter.nthElement(new double[0], 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            Sorter.nthElement(new double[]{1.0, 2.0}, -1);
            fail();
        } catch (Exception expected) {
        }

        try {
            Sorter.nthElement(new double[]{1.0, 2.0}, 2);
            fail();
        } catch (Exception expected) {
        }

        assertEquals(1.0, Sorter.nthElement(new double[]{1.0}, 0), 1.0);
        assertEquals(1.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 0), 0.0);
        assertEquals(2.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 1), 0.0);
        assertEquals(3.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 2), 0.0);
        assertEquals(4.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 3), 0.0);
    }

}
