package com.bc.ceres.swing.figure;

import junit.framework.TestCase;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class AbstractFigureTest extends TestCase {

    public void testDefaultProperties() {
        Figure f = new AbstractFigureImpl();
        assertNotNull(f.getListeners());
        assertEquals(false, f.isSelectable());
        assertEquals(0, f.getListeners().length);
        assertEquals(null, f.getFigure(null));
        assertEquals(0, f.getFigureCount());
        assertEquals(0, f.getMaxSelectionLevel());
        assertNull(f.getFigure(null));
        assertNotNull(f.getFigures(null));
        assertEquals(0, f.getFigures(null).length);
        assertNotNull(f.getFigures());
        assertEquals(0, f.getFigures().length);
        assertEquals(0, f.getMaxSelectionLevel());
        assertNotNull(f.createHandles(1));
        assertEquals(0, f.createHandles(1).length);
    }

    public void testThatCloneDoesNotCopyListeners() {
        Figure f = new AbstractFigureImpl();
        f.addListener(new AbstractFigureChangeListener() {
        });
        AbstractFigure cf = (AbstractFigure) f.clone();
        assertNotNull(cf.getListeners());
        assertEquals(0, cf.getListeners().length);
    }

    public void testListeners() {
        AbstractFigureImpl f = new AbstractFigureImpl();
        final Figure[] figureBuf = new Figure[1];
        f.addListener(new AbstractFigureChangeListener() {
            @Override
            public void figureChanged(FigureChangeEvent event) {
                figureBuf[0] = event.getFigure();
            }
        });
        assertEquals(null, figureBuf[0]);
        f.postChangeEvent();
        assertEquals(f, figureBuf[0]);
    }

    public void testDisposeRemovesListeners() {
        Figure f = new AbstractFigureImpl();
        f.addListener(new AbstractFigureChangeListener() {
        });
        FigureChangeListener[] listeners = f.getListeners();
        assertNotNull(listeners);
        assertTrue(listeners.length >= 1);
        f.dispose();
        listeners = f.getListeners();
        assertNotNull(listeners);
        assertEquals(0, listeners.length);
    }

    public void testGeometricOperationsAreNotSupported() {
        Figure f = new AbstractFigureImpl();

        try {
            f.move(0, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.rotate(null, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.scale(null, 0, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.getVertex(0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.setVertex(0, null);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    public void testThatChildrenAreNotSupported() {
        Figure f = new AbstractFigureImpl();

        try {
            f.addFigure(new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigure(0, new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigures(new TestFigure(), new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.getFigure(0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.removeFigure(new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }
    }

}