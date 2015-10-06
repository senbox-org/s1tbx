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
 * Test methods for class {@link IntervalPartition}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class IntervalPartitionTest extends TestCase {

    public void testConstructor() {
        try {
            new IntervalPartition((double[]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new IntervalPartition(new double[0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IntervalPartition(new double[2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IntervalPartition(1.0, 0.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        IntervalPartition partition = new IntervalPartition(0.0, 1.0);

        assertEquals(2, partition.getCardinal());

        assertEquals(0.0, partition.get(0), 0.0);
        assertEquals(1.0, partition.get(1), 0.0);
    }

    public void testCreateArray() {
        try {
            IntervalPartition.createArray((double[]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            IntervalPartition.createArray(new double[0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[]{1.0, 0.0});
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray((double[][]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            IntervalPartition.createArray(new double[0][]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[1][0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[1][2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[]{0.0, 1.0}, new double[]{1.0, 0.0});
            fail();
        } catch (IllegalArgumentException expected) {
        }

        final IntervalPartition[] partitions = IntervalPartition.createArray(new double[]{0.0, 1.0}, new double[]{2.0, 3.0});

        assertEquals(2, partitions.length);

        assertEquals(2, partitions[0].getCardinal());
        assertEquals(2, partitions[1].getCardinal());

        assertEquals(0.0, partitions[0].get(0), 0.0);
        assertEquals(1.0, partitions[0].get(1), 0.0);
        assertEquals(2.0, partitions[1].get(0), 0.0);
        assertEquals(3.0, partitions[1].get(1), 0.0);
    }
}
