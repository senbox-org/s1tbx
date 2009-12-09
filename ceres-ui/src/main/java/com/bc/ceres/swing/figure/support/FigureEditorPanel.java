package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
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
    private Viewport viewport;

    public FigureEditorPanel() {
        this(null);
    }

    public FigureEditorPanel(UndoContext undoContext) {
        this(undoContext, new DefaultFigureCollection());
    }

    public FigureEditorPanel(UndoContext undoContext, FigureCollection figureCollection) {
        super(null);

        setFocusable(true);
        setRequestFocusEnabled(true);
        setBackground(Color.WHITE);

        this.figureCollection = figureCollection;
        this.viewport = new DefaultViewport(true);
        this.figureEditor = new DefaultFigureEditor(this, undoContext, figureCollection);

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
        try {
            figureEditor.draw(g2d);
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
