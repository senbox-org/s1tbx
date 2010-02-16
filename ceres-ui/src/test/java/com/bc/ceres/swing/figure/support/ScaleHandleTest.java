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
        Figure f = new DefaultShapeFigure(new Rectangle(0, 0, 1, 1), true, new DefaultFigureStyle());
        final ScaleHandle scaleHandle = new ScaleHandle(f, handleType, 0, 0, new DefaultFigureStyle());
        assertEquals(Cursor.getPredefinedCursor(cursorType), scaleHandle.getCursor());
    }
}