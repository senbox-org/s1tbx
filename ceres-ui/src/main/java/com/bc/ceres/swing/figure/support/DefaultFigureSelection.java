package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Handle;

import java.awt.Graphics2D;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class DefaultFigureSelection extends DefaultFigureCollection implements FigureSelection {

    private Handle[] handles;
    private Handle selectedHandle;
    private int selectionStage;

    public DefaultFigureSelection() {
        this.selectionStage = 0;
        this.handles = NO_HANDLES;
    }

    @Override
    public int getSelectionStage() {
        return selectionStage;
    }

    @Override
    public void setSelectionStage(int selectionStage) {
        if (this.selectionStage != selectionStage) {
            this.selectionStage = selectionStage;
            if (selectionStage == 0) {
                removeAllFigures();
            }
            updateHandles();
            fireFigureChanged();
        }
    }

    @Override
    public Handle[] getHandles() {
        return handles.clone();
    }

    @Override
    public Handle getSelectedHandle() {
        return selectedHandle;
    }

    @Override
    public void setSelectedHandle(Handle handle) {
        if (this.selectedHandle != handle) {
            if (this.selectedHandle != null) {
                this.selectedHandle.setSelected(false);
            }
            this.selectedHandle = handle;
            if (this.selectedHandle != null) {
                this.selectedHandle.setSelected(true);
            }
            fireFigureChanged();
        }
    }

    @Override
    public boolean isSelectable() {
        // selections are not selectable.
        return false;
    }

    @Override
    public boolean isSelected() {
        // selections are never selected.
        return false;
    }

    @Override
    public void setSelected(boolean selected) {
        // selections cannot be selected.
    }

    @Override
    public String getPresentationName() {
        return getFigureCount() == 0 ? "" : getFigureCount() == 1 ? "Figure" : "Figures";
    }

    @Override
    public Object getSelectedValue() {
        return getFigureCount() > 0 ? getFigure(0) : null;
    }

    @Override
    public Object[] getSelectedValues() {
        return getFigures();
    }

    @Override
    public boolean isEmpty() {
        return getFigureCount() == 0;
    }

    @Override
    public Transferable createTransferable(boolean copy) {
        return new FigureTransferable(getFigures(), copy);
    }

    @Override
    protected boolean addFigureImpl(int index, Figure figure) {
        if (figure.isSelectable()) {
            figure.setSelected(true);
            return super.addFigureImpl(index, figure);
        }
        return false;
    }

    @Override
    protected Figure[] addFiguresImpl(Figure[] figures) {
        figures = filterSelectableFigures(figures);
        for (Figure figure : figures) {
            figure.setSelected(true);
        }
        return super.addFiguresImpl(figures);
    }

    @Override
    protected boolean removeFigureImpl(Figure figure) {
        boolean removed = super.removeFigureImpl(figure);
        if (removed) {
            figure.setSelected(false);
        }
        return removed;
    }

    @Override
    protected Figure[] removeFiguresImpl() {
        Figure[] removedFigures = super.removeFiguresImpl();
        for (Figure figure : removedFigures) {
            figure.setSelected(false);
        }
        return removedFigures;
    }

    /**
     * Notifies this object that it is no longer the clipboard owner.
     * This method will be called when another application or another
     * object within this application asserts ownership of the clipboard.
     *
     * @param clipboard the clipboard that is no longer owned
     * @param contents  the contents which this owner had placed on the clipboard
     */
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        System.out.println("lostOwnership: clipboard = " + clipboard + ", contents = " + contents);
        if (contents instanceof FigureTransferable) {
            FigureTransferable figureTransferable = (FigureTransferable) contents;
            if (figureTransferable.isSnapshot()) {
                figureTransferable.dispose();
            }
        }
    }

    @Override
    public FigureSelection clone() {
        final DefaultFigureSelection figureSelection = (DefaultFigureSelection) super.clone();
        figureSelection.handles = NO_HANDLES;
        figureSelection.selectedHandle = null;
        figureSelection.selectionStage = 0;
        return figureSelection;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[figureCount=" + getFigureCount() + "]";
    }

    @Override
    public Figure[] removeAllFigures() {
        disposeHandles();
        selectionStage = 0;
        return super.removeAllFigures();
    }

    @Override
    public void draw(Rendering rendering) {
        if (getFigureCount() == 0 || getSelectionStage() < 1) {
            return;
        }

        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();

        AffineTransform m2v = vp.getModelToViewTransform();

        final Figure[] figures = getFigures();
        if (figures.length > 1) {
            for (int i = 1; i < figures.length; i++) {
                Figure figure = figures[i];
                g.setPaint(StyleDefaults.MULTI_SELECTION_STROKE_PAINT);
                g.setStroke(StyleDefaults.MULTI_SELECTION_STROKE);
                g.draw(getExtendedBounds(figure, m2v));
            }
            g.setPaint(StyleDefaults.SELECTION_STROKE_PAINT);
            g.setStroke(StyleDefaults.FIRST_OF_MULTI_SELECTION_STROKE);
            g.draw(getExtendedBounds(figures[0], m2v));
        }
        g.setPaint(StyleDefaults.SELECTION_STROKE_PAINT);
        g.setStroke(StyleDefaults.SELECTION_STROKE);
        g.draw(getBounds(this, m2v));

        if (handles != null) {
            for (Handle handle : handles) {
                handle.draw(rendering);
            }
        }
    }

    private static Rectangle2D getExtendedBounds(Figure figure, AffineTransform m2v) {
        return getExtendedBounds(getBounds(figure, m2v));
    }

    private static Rectangle2D getBounds(Figure figure, AffineTransform m2v) {
        return m2v.createTransformedShape(figure.getBounds()).getBounds2D();
    }

    static Rectangle2D getExtendedBounds(Rectangle2D bounds) {
        return new Rectangle2D.Double(bounds.getX() - 0.5 * StyleDefaults.SELECTION_EXTEND_SIZE,
                                      bounds.getY() - 0.5 * StyleDefaults.SELECTION_EXTEND_SIZE,
                                      bounds.getWidth() + StyleDefaults.SELECTION_EXTEND_SIZE,
                                      bounds.getHeight() + StyleDefaults.SELECTION_EXTEND_SIZE);
    }

    private void updateHandles() {
        disposeHandles();
        if (this.selectionStage != 0) {
            handles = createHandles(this.selectionStage);
        }
    }

    private void disposeHandles() {
        for (Handle handle : handles) {
            handle.dispose();
        }
        handles = NO_HANDLES;
        selectedHandle = null;
    }

    private static Figure[] filterSelectableFigures(Figure[] figures) {
        boolean allSelectable = true;
        for (Figure figure : figures) {
            if (!figure.isSelectable()) {
                allSelectable = false;
                break;
            }
        }
        if (!allSelectable) {
            ArrayList<Figure> selectableFigures = new ArrayList<Figure>(figures.length);
            for (Figure figure : figures) {
                if (figure.isSelectable()) {
                    selectableFigures.add(figure);
                }
            }
            figures = selectableFigures.toArray(new Figure[selectableFigures.size()]);
        }
        return figures;
    }

}
