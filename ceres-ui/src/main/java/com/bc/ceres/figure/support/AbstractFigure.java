package com.bc.ceres.figure.support;

import com.bc.ceres.figure.Figure;
import com.bc.ceres.figure.FigureListener;
import com.bc.ceres.figure.Handle;
import com.bc.ceres.figure.support.FigureStyle;
import com.bc.ceres.figure.support.RotateHandle;
import com.bc.ceres.figure.support.ScaleHandle;
import com.bc.ceres.figure.support.UIDefaults;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFigure implements Figure {

    private List<FigureListener> listenerList;
    private static final String OPERATION_NOT_SUPPORTED = "Operation not supported.";

    protected AbstractFigure() {
    }

    @Override
    public boolean contains(Point2D point) {
        return false;
    }

    @Override
    public boolean contains(Figure figure) {
        return false;
    }

    @Override
    public int getFigureCount() {
        return 0;
    }

    @Override
    public Figure getFigure(Point2D p) {
        return null;
    }

    @Override
    public Figure[] getFigures(Rectangle2D rectangle) {
        return new Figure[0];
    }

    @Override
    public Figure[] getFigures() {
        return new Figure[0];
    }

    @Override
    public synchronized boolean addFigure(Figure figure) {
        if (!contains(figure)) {
            boolean added = addFigureImpl(figure);
            if (added) {
                fireFigureChanged();
                fireFiguresAdded(figure);
            }
            return added;
        }
        return addFigure(getFigureCount(), figure);
    }

    @Override
    public boolean addFigure(int index, Figure figure) {
        if (!contains(figure)) {
            boolean added = addFigureImpl(index, figure);
            if (added) {
                fireFigureChanged();
                fireFiguresAdded(figure);
            }
            return added;
        }
        return false;
    }

    @Override
    public synchronized Figure[] addFigures(Figure... figures) {
        Figure[] added = addFiguresImpl(figures);
        if (added.length > 0) {
            fireFigureChanged();
            fireFiguresAdded(added);
        }
        return added;
    }

    @Override
    public synchronized boolean removeFigure(Figure figure) {
        boolean removed = removeFigureImpl(figure);
        if (removed) {
            fireFigureChanged();
            fireFiguresRemoved(figure);
        }
        return removed;
    }

    @Override
    public Figure[] removeFigures(Figure... figures) {
        if (getFigureCount() > 0 && figures.length > 0) {
            Figure[] removed = removeFiguresImpl(figures);
            fireFigureChanged();
            fireFiguresRemoved(removed);
            return removed;
        }
        return new Figure[0];
    }


    @Override
    public Figure[] removeFigures() {
        if (getFigureCount() > 0) {
            Figure[] figures = removeFiguresImpl();
            fireFigureChanged();
            fireFiguresRemoved(figures);
            return figures;
        }
        return new Figure[0];
    }

    @Override
    public Figure getFigure(int index) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    protected boolean addFigureImpl(int index, Figure figure) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    protected boolean addFigureImpl(Figure figure) {
        return addFigureImpl(getFigureCount(), figure);
    }

    protected Figure[] addFiguresImpl(Figure[] figures) {
        ArrayList<Figure> added = new ArrayList<Figure>(figures.length);
        for (Figure figure : figures) {
            if (addFigureImpl(figure)) {
                added.add(figure);
            }
        }
        return added.toArray(new Figure[added.size()]);
    }

    protected boolean removeFigureImpl(Figure figure) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    protected Figure[] removeFiguresImpl(Figure[] figures) {
        ArrayList<Figure> removed = new ArrayList<Figure>(figures.length);
        for (Figure figure : figures) {
            if (removeFigureImpl(figure)) {
                removed.add(figure);
            }
        }
        return removed.toArray(new Figure[removed.size()]);
    }

    protected Figure[] removeFiguresImpl() {
        return removeFiguresImpl(getFigures());
    }

    @Override
    public void move(double dx, double dy) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void scale(Point2D point, double sx, double sy) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void rotate(Point2D point, double theta) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public synchronized double[] getVertex(int index) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public synchronized void setVertex(int index, double[] newSeg) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public synchronized void addListener(FigureListener listener) {
        if (listenerList == null) {
            listenerList = new ArrayList<FigureListener>(3);
        }
        listenerList.add(listener);
    }

    @Override
    public synchronized void removeListener(FigureListener listener) {
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }

    @Override
    public synchronized FigureListener[] getListeners() {
        if (listenerList != null) {
            return listenerList.toArray(new FigureListener[listenerList.size()]);
        } else {
            return new FigureListener[0];
        }
    }

    @Override
    public synchronized void dispose() {
        if (listenerList != null) {
            listenerList.clear();
            listenerList = null;
        }
    }

    @Override
    public AbstractFigure clone() {
        try {
            final AbstractFigure figure = (AbstractFigure) super.clone();
            figure.listenerList = null;
            return figure;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void fireFigureChanged() {
        final FigureListener[] listeners = getListeners();
        for (FigureListener listener : listeners) {
            listener.figureChanged(this);
        }
    }

    protected void fireFiguresAdded(Figure... figures) {
        final FigureListener[] listeners = getListeners();
        for (FigureListener listener : listeners) {
            listener.figuresAdded(this, figures);
        }
    }

    protected void fireFiguresRemoved(Figure... figures) {
        final FigureListener[] listeners = getListeners();
        for (FigureListener listener : listeners) {
            listener.figuresRemoved(this, figures);
        }
    }

    protected Handle[] createScaleHandles(double distance) {
        final ArrayList<Handle> handleList = new ArrayList<Handle>(8);

        FigureStyle handleStyle = getHandleStyle();

        handleList.add(new RotateHandle(this, handleStyle));

        handleList.add(new ScaleHandle(this, ScaleHandle.NW, -distance, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.NE, +distance, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.SE, +distance, +distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.SW, -distance, +distance, handleStyle));

        handleList.add(new ScaleHandle(this, ScaleHandle.N, 0.0, -distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.E, +distance, 0.0, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.S, 0.0, +distance, handleStyle));
        handleList.add(new ScaleHandle(this, ScaleHandle.W, -distance, 0.0, handleStyle));

        return handleList.toArray(new Handle[handleList.size()]);
    }

    protected FigureStyle getHandleStyle() {
        return UIDefaults.HANDLE_STYLE;
    }

    protected FigureStyle getSelectedHandleStyle() {
        return UIDefaults.SELECTED_HANDLE_STYLE;
    }

}
