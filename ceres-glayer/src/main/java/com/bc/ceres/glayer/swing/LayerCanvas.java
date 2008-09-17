package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerViewInvalidationListener;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.grender.swing.AdjustableView;
import com.bc.ceres.grender.swing.NavControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

/**
 * A preliminary UI class.
 * <p>A Swing component capable of drawing a collection of {@link com.bc.ceres.glayer.Layer}s.
 * </p>
 *
 * @author Norman Fomferra
 */
public class LayerCanvas extends JComponent implements AdjustableView {

    private Layer layer;
    private Viewport viewport;
    private Rectangle2D modelArea;
    private CanvasRendering canvasRendering;

    public LayerCanvas() {
        this(new Layer());
    }

    public LayerCanvas(Layer layer) {
        this(layer, new DefaultViewport());
    }

    public LayerCanvas(final Layer layer, final Viewport viewport) {
        setOpaque(false);

        this.layer = layer;
        this.viewport = viewport;
        this.modelArea = layer.getBounds(); // todo - check: register PCL for "layer.bounds" ?

        this.canvasRendering = new CanvasRendering();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                viewport.setBounds(getBounds());
            }

            @Override
            public void componentResized(ComponentEvent event) {
                viewport.setBounds(getBounds());
            }
        });

        layer.addListener(new LayerViewInvalidationListener() {
            @Override
            public void handleViewInvalidation(Layer layer, Rectangle2D modelRegion) {
                // todo - convert modelRegion to viewRegion and call repaint(viewRegion)
                repaint();
            }
        });

        viewport.addListener(new ViewportListener() {
            @Override
            public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
                repaint();
            }
        });

        final NavControl navControl = new NavControl();
        navControl.setBounds(0, 0, 120, 120);
        add(navControl);
        navControl.addSelectionListener(new NavControl.SelectionListener() {
            @Override
            public void handleRotate(double rotationAngle) {
                viewport.rotate(Math.toRadians(rotationAngle));
            }

            @Override
            public void handleMove(double moveDirX, double moveDirY) {
                viewport.moveViewDelta(16 * moveDirX, 16 * moveDirY);
            }
        });
    }

    public Layer getLayer() {
        return layer;
    }

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public Rectangle2D getModelBounds() {
        return modelArea;
    }

    @Override
    protected void paintChildren(Graphics g) {
        final Graphics2D g2D = (Graphics2D) g;
        final Composite oldComposite = g2D.getComposite();
        try {
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            super.paintChildren(g);
        } finally {
            if (oldComposite != null) {
                g2D.setComposite(oldComposite);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // ensure clipping is set
        if (g.getClipBounds() == null) {
            g.setClip(getX(), getY(), getWidth(), getHeight());
        }

        // paint background
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        }

        // todo - check: create new rendering if 'g' changes? (e.g. printing!)
        canvasRendering.setGraphics2D((Graphics2D) g);
        layer.render(canvasRendering);
    }


    private class CanvasRendering implements InteractiveRendering {
        private Graphics2D graphics2D;

        public CanvasRendering() {
        }

        @Override
        public Graphics2D getGraphics() {
            return graphics2D;
        }

        void setGraphics2D(Graphics2D graphics2D) {
            this.graphics2D = graphics2D;
        }

        @Override
        public Viewport getViewport() {
            return viewport;
        }

        @Override
        public Rectangle getBounds() {
            return LayerCanvas.this.getBounds();
        }

        @Override
        public void invalidateRegion(Rectangle region) {
            repaint(region.x, region.y, region.width, region.height);
        }

        @Override
        public void invokeLater(Runnable runnable) {
            SwingUtilities.invokeLater(runnable);
        }
    }
}