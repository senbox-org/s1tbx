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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.InteractionDispatcher;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.SelectionChangeSupport;
import com.bc.ceres.swing.undo.RestorableEdit;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A default implementation of a figure editor.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DefaultFigureEditor extends ExtensibleObject implements FigureEditor {

    private final UndoContext undoContext;
    private Rectangle selectionRectangle;
    private Interactor interactor;
    private final JComponent editorComponent;
    private FigureCollection figureCollection;
    private final FigureSelection figureSelection;
    private final SelectionChangeSupport selectionChangeSupport;
    private final Viewport viewport;
    private FigureFactory figureFactory;
    private FigureStyle defaultLineStyle;
    private FigureStyle defaultPolygonStyle;

    public DefaultFigureEditor(JComponent editorComponent) {
        this(editorComponent, new DefaultViewport(true), null, new DefaultFigureCollection(), new DefaultFigureFactory());
    }

    public DefaultFigureEditor(JComponent editorComponent,
                               Viewport viewport,
                               UndoContext undoContext,
                               FigureCollection figureCollection,
                               FigureFactory figureFactory) {
        Assert.notNull(editorComponent, "editorComponent");
        Assert.notNull(viewport, "viewport");
        Assert.notNull(figureCollection, "figureCollection");

        this.editorComponent = editorComponent;
        this.editorComponent.setFocusable(true);

        this.viewport = viewport;

        this.interactor = NullInteractor.INSTANCE;

        InteractionDispatcher interactionDispatcher = new InteractionDispatcher(this);
        interactionDispatcher.registerListeners(this.editorComponent);

        this.undoContext = undoContext != null ? undoContext : new DefaultUndoContext(this);
        this.figureCollection = figureCollection;
        this.figureSelection = new DefaultFigureSelection();
        this.figureSelection.addChangeListener(new FigureSelectionMulticaster());
        this.selectionChangeSupport = new SelectionChangeSupport(this);

        RepaintHandler repaintHandler = new RepaintHandler();
        this.figureCollection.addChangeListener(repaintHandler);
        this.figureSelection.addChangeListener(repaintHandler);

        this.figureFactory = figureFactory;

        this.defaultLineStyle = DefaultFigureStyle.createLineStyle(new Color(255, 255, 255, 200),
                                                                   new BasicStroke(1.5f));
        this.defaultPolygonStyle = DefaultFigureStyle.createPolygonStyle(new Color(0, 0, 255, 200),
                                                                         new Color(255, 255, 255, 200),
                                                                         new BasicStroke(1.0f));
    }

    @Override
    public UndoContext getUndoContext() {
        return undoContext;
    }

    @Override
    public JComponent getEditorComponent() {
        return editorComponent;
    }

    @Override
    public void insertFigures(boolean performInsert, Figure... figures) {
        getUndoContext().postEdit(new FigureInsertEdit(this, performInsert, figures));
    }

    @Override
    public void deleteFigures(boolean performDelete, Figure... figures) {
        getUndoContext().postEdit(new FigureDeleteEdit(this, performDelete, figures));
    }

    @Override
    public void changeFigure(Figure figure, Object figureMemento, String presentationName) {
        getUndoContext().postEdit(new RestorableEdit(figure, figureMemento, presentationName));
    }

    @Override
    public SelectionContext getSelectionContext() {
        return this;
    }

    @Override
    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    @Override
    public void setSelectionRectangle(Rectangle newRect) {
        Rectangle oldRect = selectionRectangle;
        if (newRect == oldRect) {
            return;
        }
        Rectangle2D repaintRect = null;
        if (oldRect == null) {
            selectionRectangle = newRect;
            repaintRect = newRect;
        } else if (newRect == null) {
            selectionRectangle = null;
            getEditorComponent().repaint(oldRect);
            repaintRect = oldRect;
        } else if (!oldRect.equals(newRect)) {
            selectionRectangle = newRect;
            repaintRect = oldRect.createUnion(newRect);
        }
        if (repaintRect != null) {
            getEditorComponent().repaint((int) (repaintRect.getX() - 2),
                                         (int) (repaintRect.getY() - 2),
                                         (int) (repaintRect.getWidth() + 4),
                                         (int) (repaintRect.getHeight()) + 4);
        }
    }


    @Override
    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    public void setFigureCollection(FigureCollection figureCollection) {
        if (this.figureCollection != figureCollection) {
            figureSelection.removeAllFigures();
            setSelectionRectangle(null);
            this.figureCollection = figureCollection;
        }
    }

    @Override
    public FigureSelection getFigureSelection() {
        return figureSelection;
    }

    @Override
    public FigureFactory getFigureFactory() {
        return figureFactory;
    }

    public void setFigureFactory(FigureFactory figureFactory) {
        this.figureFactory = figureFactory;
    }

    @Override
    public Selection getSelection() {
        return figureSelection;
    }

    @Override
    public void setSelection(Selection selection) {
        Object[] selectedValues = selection.getSelectedValues();
        ArrayList<Figure> selectedFigures = new ArrayList<>();
        for (Object selectedValue : selectedValues) {
            if (selectedValue instanceof Figure) {
                Figure selectedFigure = (Figure) selectedValue;
                if (figureCollection.contains(selectedFigure)) {
                    selectedFigures.add(selectedFigure);
                }
            }
        }
        figureSelection.removeAllFigures();
        figureSelection.addFigures(selectedFigures.toArray(new Figure[selectedFigures.size()]));
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.addSelectionChangeListener(listener);
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.removeSelectionChangeListener(listener);
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionChangeSupport.getSelectionChangeListeners();
    }

    @Override
    public void insert(Transferable contents) throws IOException, UnsupportedFlavorException {
        Figure[] figures = (Figure[]) contents.getTransferData(FigureTransferable.FIGURES_DATA_FLAVOR);
        if (figures != null && figures.length > 0) {
            insertFigures(true, figures);
        }
    }

    @Override
    public boolean canDeleteSelection() {
        return !getFigureSelection().isEmpty();
    }

    @Override
    public void deleteSelection() {
        Figure[] figures = getFigureSelection().getFigures();
        if (figures.length > 0) {
            deleteFigures(true, figures);
        }
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return contents.isDataFlavorSupported(FigureTransferable.FIGURES_DATA_FLAVOR);
    }

    @Override
    public void selectAll() {
        figureSelection.removeAllFigures();
        figureSelection.addFigures(getFigureCollection().getFigures());
        figureSelection.setSelectionStage(figureSelection.getMaxSelectionStage());
    }

    @Override
    public boolean canSelectAll() {
        return getFigureCollection().getFigureCount() > 0;
    }

    @Override
    public Interactor getInteractor() {
        return interactor;
    }

    @Override
    public void setInteractor(Interactor interactor) {
        if (this.interactor != interactor) {
            if (interactor != null) {
                interactor.deactivate();
            }
            this.interactor = interactor;
            if (this.interactor != null) {
                this.interactor.activate();
                getEditorComponent().setCursor(interactor.getCursor());
            } else {
                getEditorComponent().setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public FigureStyle getDefaultLineStyle() {
        return defaultLineStyle;
    }

    public void setDefaultLineStyle(FigureStyle defaultLineStyle) {
        Assert.notNull(defaultLineStyle, "defaultLineStyle");
        this.defaultLineStyle = defaultLineStyle;
    }

    @Override
    public FigureStyle getDefaultPolygonStyle() {
        return defaultPolygonStyle;
    }

    public void setDefaultPolygonStyle(FigureStyle defaultPolygonStyle) {
        Assert.notNull(defaultPolygonStyle, "defaultPolygonStyle");
        this.defaultPolygonStyle = defaultPolygonStyle;
    }

    /**
     * Calls
     * <pre>
     * drawFigureCollection(rendering);
     * drawFigureSelection(rendering);
     * drawSelectionRectangle(rendering);
     * </pre>
     *
     * @param rendering The rendering.
     */
    public void draw(Rendering rendering) {
        drawFigureCollection(rendering);
        drawFigureSelection(rendering);
        drawSelectionRectangle(rendering);
    }

    public void drawFigureCollection(Rendering rendering) {
        getFigureCollection().draw(rendering);
    }

    public void drawFigureSelection(Rendering rendering) {
        getFigureSelection().draw(rendering);
    }

    public void drawSelectionRectangle(Rendering rendering) {
        if (getSelectionRectangle() != null) {
            Graphics2D graphics = rendering.getGraphics();
            graphics.setPaint(StyleDefaults.SELECTION_RECT_FILL_PAINT);
            graphics.fill(getSelectionRectangle());
            graphics.setPaint(StyleDefaults.SELECTION_RECT_STROKE_PAINT);
            graphics.draw(getSelectionRectangle());
        }
    }

    private class RepaintHandler implements FigureChangeListener {
        @Override
        public void figureChanged(FigureChangeEvent event) {
            getEditorComponent().repaint();
        }
    }

    private class FigureSelectionMulticaster implements FigureChangeListener {

        @Override
        public void figureChanged(FigureChangeEvent event) {
            if (event.getType() == FigureChangeEvent.FIGURES_ADDED
                    || event.getType() == FigureChangeEvent.FIGURES_REMOVED) {
                selectionChangeSupport.fireSelectionChange(DefaultFigureEditor.this, figureSelection);
            }
        }
    }

}