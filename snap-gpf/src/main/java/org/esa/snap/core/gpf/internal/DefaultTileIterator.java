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

import org.esa.snap.core.gpf.Tile;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class DefaultTileIterator implements Iterator<Tile.Pos> {
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;
    private int x;
    private int y;
    private boolean done;

    public DefaultTileIterator(Rectangle rectangle) {
        x1 = rectangle.x;
        y1 = rectangle.y;
        x2 = x1 + rectangle.width - 1;
        y2 = y1 + rectangle.height - 1;
        x = x1;
        y = y1;
        done = x > x2 && y > y2;
    }

    @Override
    public final boolean hasNext() {
        return !done;
    }

    @Override
    public final Tile.Pos next() {
        if (done) {
            throw new NoSuchElementException();
        }
        Tile.Pos p = new Tile.Pos(x, y);
        x++;
        if (x > x2) {
            x = x1;
            y++;
            if (y > y2) {
                y = y1;
                done = true;
            }
        }
        return p;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
