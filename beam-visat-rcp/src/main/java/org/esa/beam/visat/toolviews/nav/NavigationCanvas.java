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

package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.DefaultLayerCanvasModel;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glayer.swing.LayerCanvasModel;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class NavigationCanvas extends JPanel {

    private final NavigationToolView navigationWindow;
    private LayerCanvas thumbnailCanvas;
    private static final DefaultLayerCanvasModel NULL_MODEL = new DefaultLayerCanvasModel(new CollectionLayer(),
                                                                                          new DefaultViewport());
    private ObservedViewportHandler observedViewportHandler;
    private Rectangle2D moveSliderRect;
    private boolean adjustingObservedViewport;
    private boolean debug = false;

    public NavigationCanvas(NavigationToolView navigationWindow) {
        super(new BorderLayout());
        setOpaque(true);
        this.navigationWindow = navigationWindow;
        thumbnailCanvas = new LayerCanvas();
        thumbnailCanvas.setBackground(ProductSceneView.DEFAULT_IMAGE_BACKGROUND_COLOR);
        thumbnailCanvas.setLayerFilter(new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer instanceof ImageLayer;
            }
        });
        thumbnailCanvas.addOverlay(new LayerCanvas.Overlay() {
            @Override
            public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
                if (moveSliderRect != null && !moveSliderRect.isEmpty()) {
                    Graphics2D g = rendering.getGraphics();
                    g.setColor(new Color(getForeground().getRed(), getForeground().getGreen(),
                                         getForeground().getBlue(), 82));
                    final Rectangle bounds = moveSliderRect.getBounds();
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                    g.setColor(getForeground());
                    g.draw3DRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2, true);
                    g.draw3DRect(bounds.x, bounds.y, bounds.width, bounds.height, false);
                }
            }
        });
        add(thumbnailCanvas, BorderLayout.CENTER);

        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        observedViewportHandler = new ObservedViewportHandler();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        thumbnailCanvas.getViewport().setViewBounds(new Rectangle(x, y, width, height));
        thumbnailCanvas.zoomAll();
        updateMoveSliderRect();
    }

    /**
     * This method ignores the given parameter. It is an empty implementation to
     * prevent from setting borders on this canvas.
     *
     * @param border is ignored
     */
    @Override
    public void setBorder(Border border) {
    }

    void handleViewChanged(ProductSceneView oldView, ProductSceneView newView) {
        if (debug) {
            System.out.println("NavigationCanvas.handleViewChanged(): " + System.currentTimeMillis());
            System.out.println("  oldView = " + (oldView == null ? "null" : oldView.getSceneName()));
            System.out.println("  newView = " + (newView == null ? "null" : newView.getSceneName()));
        }
        if (oldView != null) {
            Viewport observedViewport = oldView.getLayerCanvas().getViewport();
            observedViewport.removeListener(observedViewportHandler);
        }
        if (newView != null) {
            Viewport observedViewport = newView.getLayerCanvas().getViewport();
            observedViewport.addListener(observedViewportHandler);
            final Rectangle bounds;
            if (getBounds().isEmpty()) {
                bounds = new Rectangle(0, 0, 100, 100);
            } else {
                bounds = getBounds();
            }
            Viewport thumbnailViewport = new DefaultViewport(bounds, observedViewport.isModelYAxisDown());
            thumbnailViewport.setOrientation(observedViewport.getOrientation());
            LayerCanvasModel thumbnailCanvasModel = new DefaultLayerCanvasModel(newView.getRootLayer(),
                                                                                thumbnailViewport);
            thumbnailCanvas.setModel(thumbnailCanvasModel);
            thumbnailCanvas.zoomAll();
        } else {
            thumbnailCanvas.setModel(NULL_MODEL);
        }
        updateMoveSliderRect();
    }

    private void updateMoveSliderRect() {
        ProductSceneView currentView = getNavigationWindow().getCurrentView();
        if (currentView != null) {
            Viewport viewport = currentView.getLayerCanvas().getViewport();
            Rectangle viewBounds = viewport.getViewBounds();
            AffineTransform m2vTN = thumbnailCanvas.getViewport().getModelToViewTransform();
            AffineTransform v2mVP = viewport.getViewToModelTransform();
            moveSliderRect = m2vTN.createTransformedShape(v2mVP.createTransformedShape(viewBounds)).getBounds2D();
        } else {
            moveSliderRect = new Rectangle2D.Double();
        }
        if (debug) {
            System.out.println("NavigationCanvas.updateMoveSliderRect(): " + System.currentTimeMillis());
            if (currentView != null) {
                Viewport viewport = currentView.getLayerCanvas().getViewport();
                System.out.println(
                        "  currentView    = " + currentView.getSceneName() + ", viewBounds = " + viewport.getViewBounds() + ", viewBounds = " + viewport.getViewBounds());
            } else {
                System.out.println("  currentView    = null");
            }
            System.out.println("  moveSliderRect = " + moveSliderRect);
        }
        repaint();
    }

    private void handleMoveSliderRectChanged() {
        ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view != null) {
            adjustingObservedViewport = true;
            Point2D location = new Point2D.Double(moveSliderRect.getMinX(), moveSliderRect.getMinY());
            thumbnailCanvas.getViewport().getViewToModelTransform().transform(location, location);
            getNavigationWindow().setModelOffset(location.getX(), location.getY());
            adjustingObservedViewport = false;
        }
    }

    private NavigationToolView getNavigationWindow() {
        return navigationWindow;
    }

    private class ObservedViewportHandler implements ViewportListener {

        @Override
        public void handleViewportChanged(Viewport observedViewport, boolean orientationChanged) {
            if (!adjustingObservedViewport) {
                if (orientationChanged) {
                    thumbnailCanvas.getViewport().setOrientation(observedViewport.getOrientation());
                    thumbnailCanvas.zoomAll();
                }
                updateMoveSliderRect();
            }
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        private Point pickPoint;
        private Point sliderPoint;

        @Override
        public void mousePressed(MouseEvent e) {
            pickPoint = e.getPoint();
            if (!moveSliderRect.contains(pickPoint)) {
                final int x1 = pickPoint.x - moveSliderRect.getBounds().width / 2;
                final int y1 = pickPoint.y - moveSliderRect.getBounds().height / 2;
                moveSliderRect.setRect(x1, y1, moveSliderRect.getWidth(), moveSliderRect.getHeight());
                repaint();
            }
            sliderPoint = moveSliderRect.getBounds().getLocation();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            repaint();
            handleMoveSliderRectChanged();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final int x1 = sliderPoint.x + (e.getX() - pickPoint.x);
            final int y1 = sliderPoint.y + (e.getY() - pickPoint.y);
            moveSliderRect.setRect(x1, y1, moveSliderRect.getWidth(), moveSliderRect.getHeight());
            repaint();
            handleMoveSliderRectChanged();
        }
    }

}
