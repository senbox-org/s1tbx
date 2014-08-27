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
package com.bc.ceres.glayer.swing;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerViewInvalidationListener;
import com.bc.ceres.glayer.swing.NavControl.NavControlModel;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * A Swing component capable of drawing a collection of {@link com.bc.ceres.glayer.Layer}s.
 *
 * @author Norman Fomferra
 */
public class LayerCanvas extends JPanel implements AdjustableView {

    private static final boolean DEBUG = Boolean.getBoolean("snap.renderer.debug");

    private LayerCanvasModel model;
    private CanvasRendering canvasRendering;

    private boolean navControlShown;
    private WakefulComponent navControlWrapper;
    private boolean initiallyZoomingAll;
    private boolean zoomedAll;

    // AdjustableView properties
    private Rectangle2D maxVisibleModelBounds;
    private double minZoomFactor;
    private double maxZoomFactor;
    private double defaultZoomFactor;

    private ArrayList<Overlay> overlays;

    private final ModelChangeHandler modelChangeHandler;

    private boolean antialiasing;

    private LayerFilter layerFilter;

    public LayerCanvas() {
        this(new CollectionLayer());
    }

    public LayerCanvas(Layer layer) {
        this(layer, new DefaultViewport(true));
    }

    public LayerCanvas(final Layer layer, final Viewport viewport) {
        this(new DefaultLayerCanvasModel(layer, viewport));
    }

    public LayerCanvas(LayerCanvasModel model) {
        super(null);
        Assert.notNull(model, "model");
        setOpaque(true);
        this.modelChangeHandler = new ModelChangeHandler();
        this.model = model;
        this.model.addChangeListener(modelChangeHandler);
        this.canvasRendering = new CanvasRendering();
        this.overlays = new ArrayList<Overlay>(4);
        this.initiallyZoomingAll = true;
        this.zoomedAll = false;
        this.antialiasing = true;
        setNavControlShown(false);
        if (!model.getViewport().getViewBounds().isEmpty()) {
            setBounds(model.getViewport().getViewBounds());
        }
    }

    public LayerCanvasModel getModel() {
        return model;
    }

    public void setModel(LayerCanvasModel newModel) {
        Assert.notNull(newModel, "newModel");
        LayerCanvasModel oldModel = this.model;
        if (newModel != oldModel) {
            oldModel.removeChangeListener(modelChangeHandler);
            zoomedAll = false;
            this.model = newModel;
            if (!getBounds().isEmpty()) {
                this.model.getViewport().setViewBounds(getBounds());
            }
            updateAdjustableViewProperties();
            this.model.addChangeListener(modelChangeHandler);
            firePropertyChange("model", oldModel, newModel);
            repaint();
        }
    }

    public Layer getLayer() {
        return model.getLayer();
    }

    public LayerFilter getLayerFilter() {
        return layerFilter;
    }

    public void setLayerFilter(LayerFilter layerFilter) {
        LayerFilter oldLayerFilter = this.layerFilter;
        if (oldLayerFilter != layerFilter) {
            this.layerFilter = layerFilter;
            repaint();
            firePropertyChange("layerFilter", oldLayerFilter, layerFilter);
        }
    }

    public void dispose() {
        if (model != null) {
            model.removeChangeListener(modelChangeHandler);
        }
    }

    /**
     * Adds an overlay to the canvas.
     *
     * @param overlay An overlay
     */
    public void addOverlay(Overlay overlay) {
        overlays.add(overlay);
        repaint();
    }

    /**
     * Removes an overlay from the canvas.
     *
     * @param overlay An overlay
     */
    public void removeOverlay(Overlay overlay) {
        overlays.remove(overlay);
        repaint();
    }


    /**
     * None API. Don't use this method!
     *
     * @return true, if this canvas uses a {@link NavControl}.
     */
    public boolean isNavControlShown() {
        return navControlShown;
    }

    /**
     * Checks if anti-aliased vector graphics are enabled.
     * @return true, if enabled.
     */
    public boolean isAntialiasing() {
        return antialiasing;
    }

    /**
     * Enables / disables anti-aliased vector graphics.
     * @param antialiasing true, if enabled.
     */
    public void setAntialiasing(boolean antialiasing) {
        boolean oldValue = this.antialiasing;
        if (oldValue != antialiasing) {
            this.antialiasing = antialiasing;
            firePropertyChange("antialiasing", oldValue, antialiasing);
            repaint();
        }
    }

    /**
     * None API. Don't use this method!
     *
     * @param navControlShown true, if this canvas uses a {@link NavControl}.
     */
    public void setNavControlShown(boolean navControlShown) {
        boolean oldValue = this.navControlShown;
        if (oldValue != navControlShown) {
            if (navControlShown) {
                final NavControl navControl = new NavControl(new NavControlModelImpl(getViewport()));
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

    public boolean isInitiallyZoomingAll() {
        return initiallyZoomingAll;
    }

    public void setInitiallyZoomingAll(boolean initiallyZoomingAll) {
        this.initiallyZoomingAll = initiallyZoomingAll;
    }

    public void zoomAll() {
        getViewport().zoom(getMaxVisibleModelBounds());
    }

    /////////////////////////////////////////////////////////////////////////
    // AdjustableView implementation

    @Override
    public Viewport getViewport() {
        return model.getViewport();
    }

    @Override
    public Rectangle2D getMaxVisibleModelBounds() {
        return maxVisibleModelBounds;
    }

    @Override
    public double getMinZoomFactor() {
        return minZoomFactor;
    }

    @Override
    public double getMaxZoomFactor() {
        return maxZoomFactor;
    }

    @Override
    public double getDefaultZoomFactor() {
        return defaultZoomFactor;
    }

    private void updateAdjustableViewProperties() {
        maxVisibleModelBounds = computeMaxVisibleModelBounds(getLayer().getModelBounds(),
                                                             getViewport().getOrientation());
        minZoomFactor = computeMinZoomFactor(getViewport().getViewBounds(), maxVisibleModelBounds);
        Layer layer = getLayer();
        double minScale = computeMinImageToModelScale(layer);
        if (minScale > 0.0) {
            defaultZoomFactor = 1.0 / minScale;
            maxZoomFactor = 32.0 / minScale; // empiric!
        } else {
            defaultZoomFactor = minZoomFactor;
            maxZoomFactor = 1000.0 * minZoomFactor;
        }
        if (DEBUG) {
            System.out.println("LayerCanvas.updateAdjustableViewProperties():");
            System.out.println("  zoomFactor            = " + getViewport().getZoomFactor());
            System.out.println("  minZoomFactor         = " + minZoomFactor);
            System.out.println("  maxZoomFactor         = " + maxZoomFactor);
            System.out.println("  defaultZoomFactor     = " + defaultZoomFactor);
            System.out.println("  maxVisibleModelBounds = " + maxVisibleModelBounds);
        }
    }

    static double computeMinZoomFactor(Rectangle2D viewBounds, Rectangle2D maxVisibleModelBounds) {
        double vw = viewBounds.getWidth();
        double vh = viewBounds.getHeight();
        double mw = maxVisibleModelBounds.getWidth();
        double mh = maxVisibleModelBounds.getHeight();
        double sw = mw > 0.0 ? vw / mw : 0.0;
        double sh = mh > 0.0 ? vh / mh : 0.0;
        double s;
        if (sw > 0.0 && sh > 0.0) {
            s = Math.min(sw, sh);
        } else if (sw > 0.0) {
            s = sw;
        } else if (sh > 0.0) {
            s = sh;
        } else {
            s = 0.0;
        }
        return 0.5 * s;
    }

    static double computeMinImageToModelScale(Layer layer) {
        return computeMinImageToModelScale(layer, 0.0);
    }

    private static double computeMinImageToModelScale(Layer layer, double minScale) {
        if (layer instanceof ImageLayer) {
            ImageLayer imageLayer = (ImageLayer) layer;
            if (imageLayer.getModelBounds() != null) {
                AffineTransform i2m = imageLayer.getImageToModelTransform();
                double scale = Math.sqrt(Math.abs(i2m.getDeterminant()));
                if (scale > 0.0 && (minScale <= 0.0 || scale < minScale)) {
                    minScale = scale;
                }
            }
        }
        for (Layer childLayer : layer.getChildren()) {
            minScale = computeMinImageToModelScale(childLayer, minScale);
        }
        return minScale;
    }


    public static Rectangle2D computeMaxVisibleModelBounds(Rectangle2D modelBounds, double orientation) {
        if (modelBounds == null) {
            return new Rectangle();
        }
        if (orientation == 0.0) {
            return modelBounds;
        } else {
            final AffineTransform t = new AffineTransform();
            t.rotate(orientation, modelBounds.getCenterX(), modelBounds.getCenterY());
            return t.createTransformedShape(modelBounds).getBounds2D();
        }
    }

    // AdjustableView implementation
    /////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////
    // JComponent overrides

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        getViewport().setViewBounds(getBounds());
    }

    @Override
    public void doLayout() {
        if (navControlShown && navControlWrapper != null) {
            // Use the following code to align the nav. control to the RIGHT (nf, 18.09,.2008)
            //            navControlWrapper.setLocation(getWidth() - navControlWrapper.getWidth() - 4, 4);
            navControlWrapper.setLocation(4, 4);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        long t0 = DEBUG ? System.nanoTime() : 0L;

        if (initiallyZoomingAll && !zoomedAll && maxVisibleModelBounds != null && !maxVisibleModelBounds.isEmpty()) {
            zoomedAll = true;
            zoomAll();
        }

        final Graphics2D g2d = (Graphics2D) g;
        final Object antiAliasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final Object textAntiAliasing = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

        if (antialiasing) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        try {
            super.paintComponent(g);

            canvasRendering.setGraphics2D((Graphics2D) g);
            getLayer().render(canvasRendering, layerFilter);

            if (!isPaintingForPrint()) {
                for (Overlay overlay : overlays) {
                    overlay.paintOverlay(this, canvasRendering);
                }
            }
        } finally {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAliasing);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntiAliasing);
        }

        if (DEBUG) {
            double dt = (System.nanoTime() - t0) / (1000.0 * 1000.0);
            System.out.println("LayerCanvas.paintComponent() took " + dt + " ms");
        }
    }

    // JComponent overrides
    /////////////////////////////////////////////////////////////////////////

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
            return getModel().getViewport();
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

    private static class NavControlModelImpl implements NavControlModel {

        private final Viewport viewport;

        public NavControlModelImpl(Viewport viewport) {
            this.viewport = viewport;
        }

        @Override
        public double getCurrentAngle() {
            return Math.toDegrees(viewport.getOrientation());
        }

        @Override
        public void handleRotate(double rotationAngle) {
            viewport.setOrientation(Math.toRadians(rotationAngle));
        }

        @Override
        public void handleMove(double moveDirX, double moveDirY) {
            viewport.moveViewDelta(16 * moveDirX, 16 * moveDirY);
        }

        @Override
        public void handleScale(double scaleDir) {
            final double oldZoomFactor = viewport.getZoomFactor();
            final double newZoomFactor = (1.0 + 0.1 * scaleDir) * oldZoomFactor;
            viewport.setZoomFactor(newZoomFactor);
        }

    }

    public interface Overlay {

        void paintOverlay(LayerCanvas canvas, Rendering rendering);
    }

    private class ModelChangeHandler extends LayerViewInvalidationListener implements LayerCanvasModel.ChangeListener {

        @Override
        public void handleViewInvalidation(Layer layer, Rectangle2D modelRegion) {
            updateAdjustableViewProperties();
            if (modelRegion != null) {
                AffineTransform m2v = getViewport().getModelToViewTransform();
                Rectangle viewRegion = m2v.createTransformedShape(modelRegion).getBounds();
                repaint(viewRegion);
            } else {
                repaint();
            }
        }

        @Override
        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            updateAdjustableViewProperties();
            repaint();
        }
    }
}