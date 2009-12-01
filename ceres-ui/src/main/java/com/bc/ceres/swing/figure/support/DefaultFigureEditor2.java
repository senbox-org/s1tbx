package com.bc.ceres.swing.figure.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportOwner;
import com.bc.ceres.grender.support.DefaultRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.AbstractFigureChangeListener;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.RestorableEdit;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

public class DefaultFigureEditor2 implements FigureEditor {

    private final UndoContext undoContext;
    private final DefaultRendering rendering;
    private final FigureSelectionContext figureSelectionContext;
    private Rectangle selectionRectangle;
    private Interactor interactor;
    private final JComponent editorComponent;

    public DefaultFigureEditor2(JComponent editorComponent) {
        this(editorComponent, null, new DefaultFigureCollection());
    }

    public DefaultFigureEditor2(JComponent editorComponent, UndoContext undoContext, FigureCollection figureCollection) {
        Assert.notNull(editorComponent, "editorComponent");
        this.editorComponent = editorComponent;

        Viewport viewport;
        if (editorComponent instanceof ViewportOwner) {
            viewport = ((ViewportOwner) editorComponent).getViewport();
            InteractionDispatcher interactionDispatcher = new InteractionDispatcher(this);
            interactionDispatcher.registerListeners(this.editorComponent);
            editorComponent.requestFocusInWindow();
        } else {
            viewport = new DefaultViewport(true);
        }

        this.undoContext = undoContext != null ? undoContext : new DefaultUndoContext(this);
        this.figureSelectionContext = new FigureSelectionContext(this, figureCollection, new DefaultFigureSelection());
        this.interactor = NullInteractor.INSTANCE;
        this.rendering = new DefaultRendering(viewport);

        RepaintHandler repaintHandler = new RepaintHandler();
        this.figureSelectionContext.getFigureCollection().addListener(repaintHandler);
        this.figureSelectionContext.getFigureSelection().addListener(repaintHandler);
    }

    @Override
    public JComponent getEditorComponent() {
        return editorComponent;
    }

    @Override
    public void insertFigures(boolean performInsert, Figure... figures) {
        undoContext.postEdit(new FigureInsertEdit(this, performInsert, figures));
    }

    @Override
    public void deleteFigures(boolean performDelete, Figure... figures) {
        undoContext.postEdit(new FigureDeleteEdit(this, performDelete, figures));
    }

    @Override
    public void changeFigure(Figure figure, Object figureMemento, String presentationName) {
        undoContext.postEdit(new RestorableEdit(figure, figureMemento, presentationName));
    }

    @Override
    public SelectionContext getSelectionContext() {
        return figureSelectionContext;
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
        if (oldRect == null) {
            selectionRectangle = newRect;
            getEditorComponent().repaint(newRect);
        } else if (newRect == null) {
            selectionRectangle = newRect;
            getEditorComponent().repaint(oldRect);
        } else if (!oldRect.equals(newRect)) {
            selectionRectangle = newRect;
            Rectangle2D r = oldRect.createUnion(newRect);
            getEditorComponent().repaint((int) r.getX(), (int) r.getX(),
                                       (int) (1 + r.getX()), (int) (1 + r.getHeight()));
        }
    }

    @Override
    public FigureCollection getFigureCollection() {
        return figureSelectionContext.getFigureCollection();
    }

    @Override
    public FigureSelection getFigureSelection() {
        return figureSelectionContext.getFigureSelection();
    }

    @Override
    public Interactor getInteractor() {
        return interactor;
    }

    @Override
    public void setInteractor(Interactor interactor) {
        if (this.interactor != interactor) {
            this.interactor.deactivate();
            this.interactor = interactor;
            this.interactor.activate();
            getEditorComponent().setCursor(interactor.getCursor());
        }
    }

    @Override
    public void setCursor(Cursor cursor) {
        getEditorComponent().setCursor(cursor);
    }

    @Override
    public Viewport getViewport() {
        return rendering.getViewport();
    }

    public void draw(Graphics2D graphics) {
        drawFigures(graphics, false);
        drawSelectionRectangle(graphics);
    }

    public void drawFigures(Graphics2D graphics, boolean selectionOnly) {
        rendering.setGraphics(graphics);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        getFigureCollection().draw(rendering);
        getFigureSelection().draw(rendering);
    }

    public void drawSelectionRectangle(Graphics2D graphics) {
        if (getSelectionRectangle() != null) {
            graphics.setPaint(StyleDefaults.SELECTION_RECT_FILL_PAINT);
            graphics.fill(getSelectionRectangle());
            graphics.setPaint(StyleDefaults.SELECTION_RECT_STROKE_PAINT);
            graphics.draw(getSelectionRectangle());
        }
    }

    private class RepaintHandler extends AbstractFigureChangeListener {
        @Override
        public void figureChanged(FigureChangeEvent event) {
            getEditorComponent().repaint();
        }
    }
}