package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.AbstractFigureChangeListener;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class DefaultFigureEditor extends JPanel implements FigureEditor, AdjustableView {

    private final UndoContext undoContext;
    private final DefaultRendering rendering;
    private final FigureSelectionContext figureSelectionContext;
    private Rectangle selectionRectangle;
    private Interactor interactor;

    public DefaultFigureEditor() {
        super(null);

        undoContext = new DefaultUndoContext(this);
        figureSelectionContext = new FigureSelectionContext(this);
        interactor = NullInteractor.INSTANCE;
        rendering = new DefaultRendering(new DefaultViewport(true));

        RepaintHandler repaintHandler = new RepaintHandler();
        figureSelectionContext.getFigureCollection().addListener(repaintHandler);
        figureSelectionContext.getFigureSelection().addListener(repaintHandler);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow(); // to receive key events
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                rendering.getViewport().setViewBounds(getBounds());
            }
        });

        rendering.getViewport().addListener(new ViewportListener() {
            @Override
            public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
                repaint();
            }
        });

        InteractionDispatcher interactionDispatcher = new InteractionDispatcher(this);
        interactionDispatcher.registerListeners(this);
    }

    @Override
    public SelectionContext getSelectionContext() {
        return figureSelectionContext;
    }

    @Override
    public UndoContext getUndoContext() {
        return undoContext;
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
        this.interactor.deactivate(this);
        this.interactor = interactor;
        this.interactor.activate(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            super.paintComponent(g2d);
            rendering.setGraphics(g2d);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            getFigureCollection().draw(rendering);
            getFigureSelection().draw(rendering);
            if (getSelectionRectangle() != null) {
                g2d.setPaint(StyleDefaults.SELECTION_RECT_FILL_PAINT);
                g2d.fill(getSelectionRectangle());
                g2d.setPaint(StyleDefaults.SELECTION_RECT_STROKE_PAINT);
                g2d.draw(getSelectionRectangle());
            }
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public Viewport getViewport() {
        return rendering.getViewport();
    }

    @Override
    public Rectangle2D getMaxVisibleModelBounds() {
        return getFigureCollection().getBounds();
    }

    @Override
    public double getDefaultZoomFactor() {
        return 1;
    }

    @Override
    public double getMinZoomFactor() {
        return 0.1;
    }

    @Override
    public double getMaxZoomFactor() {
        return 10;
    }

    private class RepaintHandler extends AbstractFigureChangeListener {
        @Override
        public void figureChanged(FigureChangeEvent event) {
            repaint();
        }
    }
}
