/*
 * $Id$
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerViewInvalidationListener;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultViewport;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

/**
 * <p>A Swing component capable of drawing a collection of {@link com.bc.ceres.glayer.Layer}s.
 *
 * @author Norman Fomferra
 */
public class LayerCanvas extends JComponent implements AdjustableView {

    private Layer layer;
    private Viewport viewport;
    private Rectangle2D modelArea;
    private CanvasRendering canvasRendering;

    private boolean navControlShown;
    private WakefulComponent navControlWrapper;

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

        setNavControlShown(true);
    }

    public Layer getLayer() {
        return layer;
    }

    /**
     * None API. Don't use this method!
     * @return true, if this canvas uses a {@link NavControl}.
     */
    public boolean isNavControlShown() {
        return navControlShown;
    }

    /**
     * None API. Don't use this method!
     * @param navControlShown true, if this canvas uses a {@link NavControl}.
     */
    public void setNavControlShown(boolean navControlShown) {
        boolean oldValue = this.navControlShown;
        if (oldValue != navControlShown) {
            if (navControlShown) {
                final NavControl navControl = new NavControl();
                navControl.addSelectionListener(new NavControlHandler(viewport));
                navControlWrapper = new WakefulComponent(navControl);
                add(navControlWrapper);
            } else {
                remove(navControlWrapper);
                navControlWrapper = null;
            }
            validate();
            this.navControlShown = navControlShown;
        }
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
    public void doLayout() {
        if (navControlShown && navControlWrapper != null) {
            final int w = navControlWrapper.getWidth();
            final int x = getWidth() - w - 4;
            final int y = 4;
            navControlWrapper.setLocation(x,y);
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

    private static class NavControlHandler implements NavControl.SelectionListener {
        private final Viewport viewport;

        public NavControlHandler(Viewport viewport) {
            this.viewport = viewport;
        }

        @Override
        public void handleRotate(double rotationAngle) {
            viewport.rotate(Math.toRadians(rotationAngle));
        }

        @Override
        public void handleMove(double moveDirX, double moveDirY) {
            viewport.moveViewDelta(16 * moveDirX, 16 * moveDirY);
        }

        @Override
        public void handleScale(double scaleDir) {
            final double oldZoomFactor = viewport.getZoomFactor();
            final double newZoomFactor = (1.0 + 0.1 * scaleDir) * oldZoomFactor;
//                System.out.println("LayerCanvas.handleScale():");
//                System.out.println("  scaleDir      = " + scaleDir);
//                System.out.println("  oldZoomFactor = " + oldZoomFactor);
//                System.out.println("  newZoomFactor = " + newZoomFactor);
            viewport.zoom(newZoomFactor);
        }
    }
}