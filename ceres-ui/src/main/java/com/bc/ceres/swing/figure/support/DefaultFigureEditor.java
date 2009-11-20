package com.bc.ceres.swing.figure.support;

import com.bc.ceres.figure.Figure;
import com.bc.ceres.figure.FigureChangeEvent;
import com.bc.ceres.figure.AbstractFigureChangeListener;
import com.bc.ceres.figure.FigureCollection;
import com.bc.ceres.figure.FigureSelection;
import com.bc.ceres.figure.support.FigureTransferable;
import com.bc.ceres.figure.support.StyleDefaults;
import com.bc.ceres.figure.support.DefaultFigureCollection;
import com.bc.ceres.figure.support.DefaultFigureSelection;
import com.bc.ceres.selection.Selection;
import com.bc.ceres.selection.SelectionChangeListener;
import com.bc.ceres.selection.support.SelectionChangeSupport;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.Interaction;
import com.bc.ceres.swing.figure.interactions.NullInteraction;

import javax.swing.JPanel;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
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
    private final UndoManager undoManager;
    private final UndoableEditSupport undoableEditSupport;
    private Rectangle selectionRectangle;
    private Interaction interaction;

    public DefaultFigureEditor() {
        super(new BorderLayout());

        selectionChangeSupport = new SelectionChangeSupport(this);
        undoManager = new UndoManager();
        undoableEditSupport = new UndoableEditSupport(this);
        interaction = NullInteraction.INSTANCE;
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

        addUndoableEditListener(undoManager);
    }

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    @Override
    public void postEdit(UndoableEdit edit) {
        undoableEditSupport.postEdit(edit);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        undoableEditSupport.addUndoableEditListener(listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        undoableEditSupport.removeUndoableEditListener(listener);
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        figureCollection.draw(g2d);
        figureSelection.draw(g2d);
        if (getSelectionRectangle() != null) {
            g2d.setPaint(StyleDefaults.SELECTION_FILL_PAINT);
            g2d.fill(getSelectionRectangle());
        }
    }

    private void deleteFigures(Figure[] figuresToDelete) {
        if (undoableEditSupport.getUpdateLevel() != 0) {
            throw new IllegalStateException("undoableEditSupport.getUpdateLevel() != 0");
        }
        postEdit(new FigureDeleteEdit(this, figuresToDelete));
    }
}
