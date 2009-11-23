package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractFigureChangeListener;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Interaction;
import com.bc.ceres.swing.figure.interactions.NullInteraction;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.support.SelectionChangeSupport;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultRendering;
import com.bc.ceres.grender.support.DefaultViewport;

import javax.swing.JPanel;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class DefaultFigureEditor extends JPanel implements FigureEditor {

    private final FigureCollection figureCollection;
    private final FigureSelection figureSelection;
    private final SelectionChangeSupport selectionChangeSupport;
    private final UndoContext undoContext;
    private Rectangle selectionRectangle;
    private Interaction interaction;
    private Viewport viewport;
    private DefaultRendering rendering;

    public DefaultFigureEditor() {
        super(new BorderLayout());

        selectionChangeSupport = new SelectionChangeSupport(this);
        undoContext = new DefaultUndoContext(this);
        interaction = NullInteraction.INSTANCE;
        rendering = new DefaultRendering((Graphics2D) getGraphics(), new DefaultViewport());

        figureCollection = new DefaultFigureCollection();
        figureSelection = new DefaultFigureSelection();

        figureCollection.addListener(new AbstractFigureChangeListener() {
            @Override
            public void figureChanged(FigureChangeEvent event) {
                repaint();
            }
        });

        figureSelection.addListener(new AbstractFigureChangeListener() {


            @Override
            public void figureChanged(FigureChangeEvent event) {
                repaint();
            }

            @Override
            public void figuresAdded(FigureChangeEvent event) {
                selectionChangeSupport.fireSelectionChange(DefaultFigureEditor.this, figureSelection);
            }

            @Override
            public void figuresRemoved(FigureChangeEvent event) {
                selectionChangeSupport.fireSelectionChange(DefaultFigureEditor.this, figureSelection);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow(); // to receive key events
            }
        });

        InteractionDispatcher interactionDispatcher = new InteractionDispatcher(this);
        addMouseListener(interactionDispatcher);
        addMouseMotionListener(interactionDispatcher);
        addKeyListener(interactionDispatcher);
        setFocusable(true); // to fire key events
    }

    @Override
    public boolean canUndo() {
        return undoContext.canUndo();
    }

    @Override
    public void undo() {
        undoContext.undo();
    }

    @Override
    public boolean canRedo() {
        return undoContext.canRedo();
    }

    @Override
    public void redo() {
        undoContext.redo();
    }

    @Override
    public void postEdit(UndoableEdit edit) {
        undoContext.postEdit(edit);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        undoContext.addUndoableEditListener(listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        undoContext.removeUndoableEditListener(listener);
    }

    @Override
    public UndoableEditListener[] getUndoableEditListeners() {
        return undoContext.getUndoableEditListeners();
    }

    @Override
    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    @Override
    public void setSelectionRectangle(Rectangle rectangle) {
        if (selectionRectangle != rectangle
                && (selectionRectangle == null || !selectionRectangle.equals(rectangle))) {
            selectionRectangle = rectangle;
            repaint();
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
    public Interaction getInteraction() {
        return interaction;
    }

    @Override
    public void setInteraction(Interaction interaction) {
        this.interaction.deactivate();
        this.interaction.setFigureEditor(null);
        this.interaction = interaction;
        this.interaction.setFigureEditor(this);
        this.interaction.activate();
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
        postEdit(new FigureInsertEdit(this, figures));
    }

    @Override
    public boolean canDeleteSelection() {
        return !getFigureSelection().isEmpty();
    }

    @Override
    public void deleteSelection() {
        deleteFigures(getFigureSelection().getFigures());
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return contents.isDataFlavorSupported(FigureTransferable.FIGURES_DATA_FLAVOR);
    }

    @Override
    public void selectAll() {
        figureSelection.removeFigures();
        figureSelection.addFigures(getFigureCollection().getFigures());
        figureSelection.setSelectionLevel(1);
    }

    @Override
    public boolean canSelectAll() {
        return getFigureCollection().getFigureCount() > 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        rendering.setGraphics(g2d);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        figureCollection.draw(rendering);
        figureSelection.draw(rendering);
        if (getSelectionRectangle() != null) {
            g2d.setPaint(StyleDefaults.SELECTION_RECT_FILL_PAINT);
            g2d.fill(getSelectionRectangle());
            g2d.setPaint(StyleDefaults.SELECTION_RECT_DRAW_PAINT);
            g2d.draw(getSelectionRectangle());
        }
    }

    private void deleteFigures(Figure[] figuresToDelete) {
        postEdit(new FigureDeleteEdit(this, figuresToDelete));
    }
}
