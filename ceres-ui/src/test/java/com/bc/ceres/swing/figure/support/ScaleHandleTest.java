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

package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.ScaleHandle;
import com.bc.ceres.swing.figure.Figure;

import java.awt.Cursor;
import java.awt.Rectangle;


public class ScaleHandleTest extends TestCase {
    public void testCursors() {
        testCursor(Cursor.E_RESIZE_CURSOR, ScaleHandle.E);
        testCursor(Cursor.NE_RESIZE_CURSOR, ScaleHandle.NE);
        testCursor(Cursor.N_RESIZE_CURSOR, ScaleHandle.N);
        testCursor(Cursor.NW_RESIZE_CURSOR, ScaleHandle.NW);
        testCursor(Cursor.W_RESIZE_CURSOR, ScaleHandle.W);
        testCursor(Cursor.SW_RESIZE_CURSOR, ScaleHandle.SW);
        testCursor(Cursor.W_RESIZE_CURSOR, ScaleHandle.W);
        testCursor(Cursor.SE_RESIZE_CURSOR, ScaleHandle.SE);
    }

    private static void testCursor(int cursorType, int handleType) {
        Figure f = new DefaultShapeFigure(new Rectangle(0, 0, 1, 1), Figure.Rank.AREA, new DefaultFigureStyle());
        final ScaleHandle scaleHandle = new ScaleHandle(f, handleType, 0, 0, new DefaultFigureStyle());
        assertEquals(Cursor.getPredefinedCursor(cursorType), scaleHandle.getCursor());
    }
}