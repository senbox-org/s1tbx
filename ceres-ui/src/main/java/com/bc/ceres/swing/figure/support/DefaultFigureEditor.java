package com.bc.ceres.swing.figure.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportOwner;
import com.bc.ceres.grender.support.DefaultRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.AbstractFigureChangeListener;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class DefaultFigureEditor implements FigureEditor{

    private final UndoContext undoContext;
    private final DefaultRendering rendering;
    private Rectangle selectionRectangle;
    private Interactor interactor;
    private final JComponent editorComponent;
    private FigureCollection figureCollection;
    private final FigureSelection figureSelection;
    private final SelectionChangeSupport selectionChangeSupport;


    public DefaultFigureEditor(JComponent editorComponent) {
        this(editorComponent, null, new DefaultFigureCollection());
    }

    public DefaultFigureEditor(JComponent editorComponent,
                               UndoContext undoContext,
                               FigureCollection figureCollection) {
        Assert.notNull(editorComponent, "editorComponent");
        this.editorComponent = editorComponent;

        Viewport viewport;
        if (editorComponent instanceof ViewportOwner) {
            viewport = ((ViewportOwner) editorComponent).getViewport();
            InteractionDispatcher interactionDispatcher = new InteractionDispatcher(this);
            interactionDispatcher.registerListeners(this.editorComponent);
        } else {
            viewport = new DefaultViewport(true);
        }
        editorComponent.setFocusable(true);

        this.undoContext = undoContext != null ? undoContext : new DefaultUndoContext(this);
        this.interactor = NullInteractor.INSTANCE;
        this.rendering = new DefaultRendering(viewport);
        this.figureCollection = figureCollection;
        this.figureSelection = new DefaultFigureSelection();
        this.figureSelection.addChangeListener(new FigureSelectionMulticaster());
        this.selectionChangeSupport = new SelectionChangeSupport(this);

        RepaintHandler repaintHandler = new RepaintHandler();
        this.figureCollection.addChangeListener(repaintHandler);
        this.figureSelection.addChangeListener(repaintHandler);
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
            selectionRectangle = newRect;
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


    public void setFigureCollection(FigureCollection figureCollection) {
        if (this.figureCollection != figureCollection) {
            figureSelection.removeFigures();
            setSelectionRectangle(null);
            this.figureCollection = figureCollection;
        }
    }

     @Override
     public FigureCollection getFigureCollection() {
         return figureCollection;
     }

     @Override
     public FigureSelection getFigureSelection() {
         return figureSelection;
     }

     @Override
     public Selection getSelection() {
         return figureSelection;
     }

     @Override
     public void setSelection(Selection selection) {
         // todo - implement (select all figures that are equal to the ones in selection)
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
         if (figures != null && figures.length  > 0) {
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
         if (figures.length  > 0) {
             deleteFigures(true, figures);
         }
     }

     @Override
     public boolean canInsert(Transferable contents) {
         return contents.isDataFlavorSupported(FigureTransferable.FIGURES_DATA_FLAVOR);
     }

     @Override
     public void selectAll() {
         figureSelection.removeFigures();
         figureSelection.addFigures(getFigureCollection().getFigures());
         figureSelection.setSelectionStage(figureSelection.getMaxSelectionStage());
     }

     @Override
     public boolean canSelectAll() {
         return getFigureCollection().getFigureCount() > 0;
     }

     private class FigureSelectionMulticaster extends AbstractFigureChangeListener {
         @Override
         public void figuresAdded(FigureChangeEvent event) {
             selectionChangeSupport.fireSelectionChange(DefaultFigureEditor.this, figureSelection);
         }

         @Override
         public void figuresRemoved(FigureChangeEvent event) {
             selectionChangeSupport.fireSelectionChange(DefaultFigureEditor.this, figureSelection);
         }
     }

    @Override
    public Interactor getInteractor() {
        return interactor;
    }

    @Override
    public void setInteractor(Interactor interactor) {
        if (this.interactor != interactor) {
            this.interactor = interactor;
            getEditorComponent().setCursor(interactor.getCursor());
        }
    }

    @Override
    public Viewport getViewport() {
        return rendering.getViewport();
    }

    public void draw(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rendering.setGraphics(graphics);
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

    private class RepaintHandler extends AbstractFigureChangeListener {
        @Override
        public void figureChanged(FigureChangeEvent event) {
            getEditorComponent().repaint();
        }
    }

}