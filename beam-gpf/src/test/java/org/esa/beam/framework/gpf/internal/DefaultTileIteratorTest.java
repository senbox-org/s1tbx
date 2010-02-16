package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.Tile;

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
