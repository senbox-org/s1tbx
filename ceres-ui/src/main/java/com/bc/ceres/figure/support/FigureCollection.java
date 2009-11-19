package com.bc.ceres.figure.support;

import com.bc.ceres.figure.support.AbstractFigure;
import com.bc.ceres.figure.support.AbstractFigureListener;
import com.bc.ceres.figure.Handle;
import com.bc.ceres.figure.Figure;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FigureCollection extends AbstractFigure {

    protected static final Handle[] NO_HANDLES = new Handle[0];

    private List<Figure> figureList;
    private Set<Figure> figureSet;
    private Rectangle2D bounds;
    private BoundsUpdater boundsUpdater;
    private ChangeDelegate changeDelegate;

    public FigureCollection() {
        this(new Figure[0]);
    }

    public FigureCollection(Figure[] figures) {
        List<Figure> list = Arrays.asList(figures);
        this.figureList = new ArrayList<Figure>(list);
        this.figureSet = new HashSet<Figure>(list);
        this.changeDelegate = new ChangeDelegate();
        this.boundsUpdater = new BoundsUpdater();
        addListener(boundsUpdater);
    }

    @Override
    public boolean isSelected() {
        if (getFigureCount() == 0) {
            return false;
        }
        for (Figure figure : figureList) {
            if (!figure.isSelected()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setSelected(boolean selected) {
        for (Figure figure : figureList) {
            figure.setSelected(selected);
        }
    }

    @Override
    public Rank getRank() {
        return Rank.COLLECTION;
    }

    @Override
    public synchronized void dispose() {
        removeFiguresImpl();
        super.dispose();
    }

    @Override
    public synchronized FigureCollection clone() {
        final FigureCollection figureCollection = (FigureCollection) super.clone();
        figureCollection.figureList = new ArrayList<Figure>(figureList);
        figureCollection.boundsUpdater = new BoundsUpdater();
        figureCollection.addListener(figureCollection.boundsUpdater);
        return figureCollection;
    }

    @Override
    public int getMaxSelectionLevel() {
        return 1;
    }

    @Override
    public synchronized Handle[] createHandles(int selectionLevel) {
        if (getFigureCount() == 0) {
            return NO_HANDLES;
        } else if (getFigureCount() == 1) {
            return getFigure(0).createHandles(selectionLevel);
        } else {
            return createScaleHandles(0.0);
        }
    }

    @Override
    public boolean contains(Point2D point) {
        return getBounds().contains(point);
    }

    @Override
    public synchronized boolean contains(Figure figure) {
        return figureSet.contains(figure);
    }

    @Override
    public synchronized int getFigureCount() {
        return figureList.size();
    }

    @Override
    public synchronized Figure getFigure(int index) {
        return figureList.get(index);
    }

    @Override
    public synchronized Figure getFigure(Point2D p) {
        Figure selectedShape = null;
        for (Figure figure : getFigures()) {
            if (figure.contains(p)) {
                selectedShape = figure;
            }
        }
        return selectedShape;
    }

    @Override
    public synchronized Figure[] getFigures(Rectangle2D rectangle) {
        ArrayList<Figure> containedFigures = new ArrayList<Figure>(getFigureCount());
        for (Figure figure : getFigures()) {
            if (rectangle.contains(figure.getBounds())) {
                containedFigures.add(figure);
            }
        }
        return containedFigures.toArray(new Figure[containedFigures.size()]);
    }

    @Override
    public synchronized Figure[] getFigures() {
        return figureList.toArray(new Figure[figureList.size()]);
    }

    @Override
    protected synchronized boolean addFigureImpl(int index, Figure figure) {
        figureSet.add(figure);
        figureList.add(index, figure);
        figure.addListener(changeDelegate);
        return true;
    }

    @Override
    protected synchronized boolean addFigureImpl(Figure figure) {
        figureSet.add(figure);
        figureList.add(figure);
        figure.addListener(changeDelegate);
        return true;
    }

    @Override
    protected boolean removeFigureImpl(Figure figure) {
        boolean b = figureSet.remove(figure);
        if (b) {
            figureList.remove(figure);
            figure.removeListener(changeDelegate);
        }
        return b;
    }

    @Override
    protected synchronized Figure[] removeFiguresImpl() {
        Figure[] allFigures = getFigures();
        for (Figure figure : allFigures) {
            figure.removeListener(changeDelegate);
        }
        figureSet.clear();
        figureList.clear();
        return allFigures;
    }

    @Override
    public synchronized Rectangle2D getBounds() {
        if (bounds == null) {
            bounds = computeBounds();
        }
        return bounds;
    }

    @Override
    public synchronized void move(double dx, double dy) {
        if (getFigureCount() > 0) {
            for (Figure figure : figureList) {
                figure.move(dx, dy);
            }
            fireFigureChanged();
        }
    }

    @Override
    public synchronized void scale(Point2D refPoint, double sx, double sy) {
        if (getFigureCount() > 0) {
            for (Figure figure : figureList) {
                figure.scale(refPoint, sx, sy);
            }
            fireFigureChanged();
        }
    }

    @Override
    public synchronized void rotate(Point2D point, double theta) {
        if (getFigureCount() > 0) {
            for (Figure figure : figureList) {
                figure.rotate(point, theta);
            }
            fireFigureChanged();
        }
    }

    @Override
    public synchronized void draw(Graphics2D g2d) {
        for (Figure figure : figureList) {
            figure.draw(g2d);
        }
    }

    @Override
    public synchronized Object createMemento() {
        if (getFigureCount() == 0) {
            return null;
        }
        Map<Figure, Object> mementoMap = new HashMap<Figure, Object>(figureList.size());
        for (Figure figure : figureList) {
            mementoMap.put(figure, figure.createMemento());
        }
        return mementoMap;
    }

    @Override
    public synchronized void setMemento(Object memento) {
        if (memento != null) {
            Map<Figure, Object> mementoMap = (Map<Figure, Object>) memento;
            for (Entry<Figure, Object> entry : mementoMap.entrySet()) {
                Figure figure = entry.getKey();
                figure.setMemento(entry.getValue());
            }
            fireFigureChanged();
        }
    }

    protected synchronized Rectangle2D computeBounds() {
        final Rectangle2D bounds = new Rectangle2D.Double();
        if (getFigureCount() > 0) {
            bounds.setRect(figureList.get(0).getBounds());
            for (int i = 1; i < figureList.size(); i++) {
                Figure figure = figureList.get(i);
                bounds.add(figure.getBounds());
            }
        }
        return bounds;
    }

    private synchronized void nullBounds() {
        bounds = null;
    }

    private class BoundsUpdater extends AbstractFigureListener {
        @Override
        public void figureChanged(Figure f) {
            nullBounds();
        }
    }

    private class ChangeDelegate extends AbstractFigureListener {
        @Override
        public void figureChanged(Figure f) {
            fireFigureChanged();
        }
    }
}