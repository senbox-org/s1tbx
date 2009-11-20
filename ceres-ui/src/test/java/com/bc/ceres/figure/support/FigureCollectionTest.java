package com.bc.ceres.figure.support;

import junit.framework.TestCase;

import java.awt.geom.Rectangle2D;

import com.bc.ceres.figure.Figure;
import com.bc.ceres.figure.FigureChangeEvent;
import com.bc.ceres.figure.support.FigureCollection;
import com.bc.ceres.figure.FigureChangeListener;

public class FigureCollectionTest extends TestCase {
    public void testDefaultProperties() {
        FigureCollection fc = new FigureCollection();
        assertEquals(Figure.Rank.COLLECTION, fc.getRank());
        assertEquals(new Rectangle2D.Double(), fc.getBounds());
        assertEquals(1, fc.getMaxSelectionLevel());
        assertEquals(0, fc.getFigureCount());
    }

    public void testThatChildChangesArePropagated() {
        FigureCollection fc = new FigureCollection();
        FigureCollection f1 = new FigureCollection();
        FigureCollection f2 = new FigureCollection();
        fc.addFigure(f1);
        fc.addFigure(f2);

        MyFigureListener listener = new MyFigureListener();
        fc.addListener(listener);

        assertEquals("", listener.trace);
        f1.addFigure(new FigureCollection());
        assertEquals("c", listener.trace);
        f2.addFigure(new FigureCollection());
        assertEquals("cc", listener.trace);
    }

    public void testAddingAndRemovingChildren() {
        FigureCollection fc = new FigureCollection();
        assertEquals(0, fc.getFigureCount());
        assertNotNull(fc.getFigures());
        assertEquals(0, fc.getFigures().length);

        FigureCollection f1 = new FigureCollection();
        FigureCollection f2 = new FigureCollection();

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

        fc.removeFigures();
        assertEquals(0, fc.getFigureCount());
    }

    public void testListener() {
        FigureCollection fc = new FigureCollection();
        MyFigureListener listener = new MyFigureListener();
        fc.addListener(listener);

        FigureCollection f1 = new FigureCollection();
        fc.addFigure(f1);
        assertEquals("ca", listener.trace);
        assertSame(fc, listener.figure);
        assertSame(fc, listener.parent);
        assertEquals(1, listener.children.length);
        assertSame(f1, listener.children[0]);

        FigureCollection f2 = new FigureCollection();
        fc.addFigure(f2);
        assertEquals("caca", listener.trace);
        assertSame(fc, listener.figure);
        assertSame(fc, listener.parent);
        assertEquals(1, listener.children.length);
        assertSame(f2, listener.children[0]);

        fc.removeFigures();
        assertEquals("cacacr", listener.trace);
        assertSame(fc, listener.figure);
        assertSame(fc, listener.parent);
        assertEquals(2, listener.children.length);
        assertSame(f1, listener.children[0]);
        assertSame(f2, listener.children[1]);
    }

    private static class MyFigureListener implements FigureChangeListener {
        String trace = "";
        Figure figure;
        Figure parent;
        Figure[] children;

        @Override
        public void figureChanged(FigureChangeEvent event) {
            trace += "c";
            this.figure = event.getFigure();
        }

        @Override
        public void figuresAdded(FigureChangeEvent event) {
            trace += "a";
            this.parent = event.getFigure();
            this.children = event.getChilds();
        }

        @Override
        public void figuresRemoved(FigureChangeEvent event) {
            trace += "r";
            this.parent = event.getFigure();
            this.children = event.getChilds();
        }
    }
}
