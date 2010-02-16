package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

public class FigureEditorPanel extends JPanel implements FigureEditorAware, AdjustableView {


    private DefaultFigureEditor figureEditor;
    private FigureCollection figureCollection;
    private DefaultRendering rendering;
    private Viewport viewport;

    public FigureEditorPanel(UndoContext undoContext,
                             FigureCollection figureCollection,
                             FigureFactory figureFactory) {
        super(null);

        setFocusable(true);
        setRequestFocusEnabled(true);
        setBackground(Color.WHITE);

        this.figureCollection = figureCollection;
        this.viewport = new DefaultViewport(true);
        this.figureEditor = new DefaultFigureEditor(this,
                                                    this.viewport,
                                                    undoContext,
                                                    figureCollection,
                                                     figureFactory);

        rendering = new DefaultRendering(this.viewport);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                viewport.setViewBounds(getBounds());
            }
        });

        viewport.addListener(new ViewportListener() {
            @Override
            public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
                repaint();
            }
        });
    }

    /////////////////////////////////////////////////////////////////////////
    // JComponent overrides

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        rendering.setGraphics(g2d);
        try {
            figureEditor.draw(rendering);
        } finally {
            g2d.dispose();
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // FigureEditorAware implementation

    @Override
    public FigureEditor getFigureEditor() {
        return figureEditor;
    }

    /////////////////////////////////////////////////////////////////////////
    // AdjustableView implementations

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public Rectangle2D getMaxVisibleModelBounds() {
        return figureCollection.getBounds();
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
}
