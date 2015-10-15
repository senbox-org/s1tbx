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

package org.esa.snap.core.gpf.internal;

import junit.framework.TestCase;
import org.esa.snap.core.gpf.Tile;

import java.awt.Rectangle;
import java.util.NoSuchElementException;

public class DefaultTileIteratorTest extends TestCase {

    public void testNextWithOnePixel() {
        DefaultTileIterator tileIterator = new DefaultTileIterator(new Rectangle(0, 0, 1, 1));
        assertTrue(tileIterator.hasNext());
        tileIterator.next();
        assertFalse(tileIterator.hasNext());
    }

    public void testNextWith3x3Pixels() {
        DefaultTileIterator tileIterator = new DefaultTileIterator(new Rectangle(2, 5, 3, 3));
        int[][] expected = new int[][]{
                /*i=0*/ {2 + 0, 5 + 0},
                /*i=1*/ {2 + 1, 5 + 0},
                /*i=2*/ {2 + 2, 5 + 0},
                /*i=3*/ {2 + 0, 5 + 1},
                /*i=4*/ {2 + 1, 5 + 1},
                /*i=5*/ {2 + 2, 5 + 1},
                /*i=6*/ {2 + 0, 5 + 2},
                /*i=7*/ {2 + 1, 5 + 2},
                /*i=8*/ {2 + 2, 5 + 2}
        };

        for (int i = 0; i < expected.length; i++) {
            int[] ep = expected[i];
            assertTrue(tileIterator.hasNext());
            Tile.Pos p = tileIterator.next(); // x=0,y=0
            assertEquals("i=" + i, new Tile.Pos(ep[0], ep[1]), p);
        }

        assertFalse(tileIterator.hasNext());
        try {
            tileIterator.next(); // --> Error
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {
            // OK, expected.
        }
    }

    public void testRemove() {
        DefaultTileIterator tileIterator = new DefaultTileIterator(new Rectangle(0, 0, 9, 4));
        try {
            tileIterator.remove(); // --> Error
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // OK, expected.
        }
    }
}
