package com.bc.ceres.figure.support;

import junit.framework.TestCase;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.bc.ceres.figure.support.AbstractFigure;
import com.bc.ceres.figure.support.AbstractFigureListener;
import com.bc.ceres.figure.Figure;
import com.bc.ceres.figure.FigureListener;
import com.bc.ceres.figure.Handle;

public class AbstractFigureTest extends TestCase {
    public void testDefaultProperties() {
        MyFigure f = new MyFigure();
        assertEquals(Figure.Rank.POLYGONAL, f.getRank());
        assertNotNull(f.getListeners());
        assertEquals(0, f.getListeners().length);
        assertEquals(new Rectangle2D.Double(0, 0, 10, 10), f.getBounds());
        assertEquals(null, f.getFigure(null));
        assertEquals(0, f.getFigureCount());
        assertEquals(0, f.getMaxSelectionLevel());
    }

    public void testThatCloneDoesNotCopyListeners() {
        MyFigure f = new MyFigure();
        f.addListener(new AbstractFigureListener() {
        });
        AbstractFigure cf = f.clone();
        assertNotNull(cf.getListeners());
        assertEquals(0, cf.getListeners().length);
    }

    public void testListeners() {
        MyFigure f = new MyFigure();
        final Figure[] figureBuf = new Figure[1];
        f.addListener(new AbstractFigureListener() {
            @Override
            public void figureChanged(Figure figure) {
                figureBuf[0] = figure;
            }
        });
        assertEquals(null, figureBuf[0]);
        f.setShape(new Rectangle(1, 2, 3, 4));
        assertEquals(f, figureBuf[0]);
    }

    public void testDisposeRemovesListeners() {
        MyFigure f = new MyFigure();
        f.addListener(new AbstractFigureListener() {
        });
        FigureListener[] listeners = f.getListeners();
        assertNotNull(listeners);
        assertTrue(listeners.length >= 1);
        f.dispose();
        listeners = f.getListeners();
        assertNotNull(listeners);
        assertEquals(0, listeners.length);
    }

    public void testGeometricOperationsAreNotSupported() {
        MyFigure f = new MyFigure();

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
        MyFigure f = new MyFigure();

        try {
            f.addFigure(new MyFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigure(0, new MyFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigures(new MyFigure(), new MyFigure());
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
            f.removeFigure(new MyFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    private static class MyFigure extends AbstractFigure {
        Shape shape = new Ellipse2D.Double(0, 0, 10, 10);

        public void setShape(Shape shape) {
            this.shape = shape;
            fireFigureChanged();
        }

        @Override
        public boolean isSelected() {
            return false; 
        }

        @Override
        public void setSelected(boolean selected) {
        }

        @Override
        public void draw(Graphics2D g2d) {
        }

        @Override
        public boolean contains(Point2D point) {
            return shape.contains(point);
        }

        @Override
        public Rectangle2D getBounds() {
            return shape.getBounds2D();
        }

        @Override
        public Rank getRank() {
            return Rank.POLYGONAL;
        }

        @Override
        public int getMaxSelectionLevel() {
            return 0;
        }

        @Override
        public Handle[] createHandles(int selectionLevel) {
            return new Handle[0];
        }

        @Override
        public Object createMemento() {
            return null;
        }

        @Override
        public void setMemento(Object memento) {
        }
    }
}