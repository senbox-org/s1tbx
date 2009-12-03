package com.bc.ceres.swing.figure;

import junit.framework.TestCase;

import java.awt.Shape;
import java.awt.Point;

public class AbstractFigureTest extends TestCase {

    public void testDefaultProperties() {
        Figure f = new AbstractFigureImpl();
        assertNotNull(f.getChangeListeners());
        assertEquals(false, f.isSelectable());
        assertEquals(0, f.getChangeListeners().length);
        assertEquals(null, f.getFigure(null));
        assertEquals(0, f.getFigureCount());
        assertEquals(0, f.getMaxSelectionStage());
        assertNull(f.getFigure(null));
        assertNotNull(f.getFigures((Shape) null));
        assertEquals(0, f.getFigures(null).length);
        assertNotNull(f.getFigures());
        assertEquals(0, f.getFigures().length);
        assertEquals(0, f.getMaxSelectionStage());
        assertNotNull(f.createHandles(1));
        assertEquals(0, f.createHandles(1).length);
    }

    public void testThatCloneDoesNotCopyListeners() {
        Figure f = new AbstractFigureImpl();
        f.addChangeListener(new AbstractFigureChangeListener() {
        });
        AbstractFigure cf = (AbstractFigure) f.clone();
        assertNotNull(cf.getChangeListeners());
        assertEquals(0, cf.getChangeListeners().length);
    }

    public void testListeners() {
        AbstractFigureImpl f = new AbstractFigureImpl();
        final Figure[] figureBuf = new Figure[1];
        f.addChangeListener(new AbstractFigureChangeListener() {
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
        f.addChangeListener(new AbstractFigureChangeListener() {
        });
        FigureChangeListener[] listeners = f.getChangeListeners();
        assertNotNull(listeners);
        assertTrue(listeners.length >= 1);
        f.dispose();
        listeners = f.getChangeListeners();
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
            fail("NullPointerException expected!");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            f.setVertex(0, new Point());
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