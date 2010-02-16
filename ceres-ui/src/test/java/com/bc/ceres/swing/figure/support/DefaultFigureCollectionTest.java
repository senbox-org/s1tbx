package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;
import junit.framework.TestCase;

import java.awt.geom.Rectangle2D;

public class DefaultFigureCollectionTest extends TestCase {
    public void testDefaultProperties() {
        DefaultFigureCollection fc = new DefaultFigureCollection();
        assertEquals(true, fc.isSelectable());
        assertEquals(true, fc.isCollection());
        assertEquals(Figure.Rank.NOT_SPECIFIED, fc.getRank());
        assertEquals(new Rectangle2D.Double(), fc.getBounds());
        assertEquals(1, fc.getMaxSelectionStage());
        assertEquals(0, fc.getFigureCount());
    }

    public void testThatChildChangesArePropagated() {
        DefaultFigureCollection fc = new DefaultFigureCollection();
        DefaultFigureCollection f1 = new DefaultFigureCollection();
        DefaultFigureCollection f2 = new DefaultFigureCollection();
        fc.addFigure(f1);
        fc.addFigure(f2);

        MyFigureListener listener = new MyFigureListener();
        fc.addChangeListener(listener);

        f1.addFigure(new DefaultFigureCollection());
        assertNotNull(listener.lastEvent);
        assertEquals(FigureChangeEvent.FIGURES_ADDED, listener.lastEvent.getType());
        assertEquals(f1, listener.lastEvent.getSourceFigure());
        f2.addFigure(new DefaultFigureCollection());
        assertNotNull(listener.lastEvent);
        assertEquals(FigureChangeEvent.FIGURES_ADDED, listener.lastEvent.getType());
        assertEquals(f2, listener.lastEvent.getSourceFigure());
    }

    public void testAddingAndRemovingChildren() {
        DefaultFigureCollection fc = new DefaultFigureCollection();
        assertEquals(0, fc.getFigureCount());
        assertNotNull(fc.getFigures());
        assertEquals(0, fc.getFigures().length);

        DefaultFigureCollection f1 = new DefaultFigureCollection();
        DefaultFigureCollection f2 = new DefaultFigureCollection();

        fc.addFigure(f1);
        fc.addFigure(f2);
        assertEquals(2, fc.getFigureCount());
        assertNotNull(fc.getFigures());
        assertEquals(2, fc.getFigures().length);
        assertSame(f1, fc.getFigures()[0]);
        assertSame(f2, fc.getFigures()[1]);

        fc.addFigure(f2);
        assertEquals(2, fc.getFigureCount());
        assertNotNull(fc.getFigures());
        assertEquals(2, fc.getFigures().length);
        assertSame(f1, fc.getFigures()[0]);
        assertSame(f2, fc.getFigures()[1]);

        fc.removeFigure(f1);
        assertEquals(1, fc.getFigureCount());
        assertNotNull(fc.getFigures());
        assertEquals(1, fc.getFigures().length);
        assertSame(f2, fc.getFigures()[0]);

        fc.removeAllFigures();
        assertEquals(0, fc.getFigureCount());
    }

    public void testListener() {
        DefaultFigureCollection fc = new DefaultFigureCollection();
        MyFigureListener listener = new MyFigureListener();
        fc.addChangeListener(listener);

        DefaultFigureCollection f1 = new DefaultFigureCollection();
        fc.addFigure(f1);
        assertEquals(FigureChangeEvent.FIGURES_ADDED, listener.lastEvent.getType());
        assertSame(fc, listener.lastEvent.getSourceFigure());
        assertNotNull(listener.lastEvent.getFigures());
        assertEquals(1, listener.lastEvent.getFigures().length);
        assertSame(f1, listener.lastEvent.getFigures()[0]);

        f1.setSelectable(true);
        assertEquals(FigureChangeEvent.FIGURE_CHANGED, listener.lastEvent.getType());
        assertSame(f1, listener.lastEvent.getSourceFigure());
        assertNull(listener.lastEvent.getFigures());

        DefaultFigureCollection f2 = new DefaultFigureCollection();
        fc.addFigure(f2);
        assertEquals(FigureChangeEvent.FIGURES_ADDED, listener.lastEvent.getType());
        assertSame(fc, listener.lastEvent.getSourceFigure());
        assertNotNull(listener.lastEvent.getFigures());
        assertEquals(1, listener.lastEvent.getFigures().length);
        assertSame(f2, listener.lastEvent.getFigures()[0]);

        fc.removeAllFigures();
        assertEquals(FigureChangeEvent.FIGURES_REMOVED, listener.lastEvent.getType());
        assertSame(fc, listener.lastEvent.getSourceFigure());
        assertNotNull(listener.lastEvent.getFigures());
        assertEquals(2, listener.lastEvent.getFigures().length);
        assertSame(f1, listener.lastEvent.getFigures()[0]);
        assertSame(f2, listener.lastEvent.getFigures()[1]);
    }

    private static class MyFigureListener implements FigureChangeListener {
        FigureChangeEvent lastEvent;

        @Override
        public void figureChanged(FigureChangeEvent event) {
            this.lastEvent = event;
        }
    }
}
