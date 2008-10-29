/*
 * Created at 12.07.2004 06:45:55 Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.DefaultLayerCanvasModel;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glayer.swing.LayerCanvasModel;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.event.MouseInputAdapter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class NavigationCanvas2 extends NavigationCanvas {

    private LayerCanvas thumbnailCanvas;
    private static final DefaultLayerCanvasModel NULL_MODEL = new DefaultLayerCanvasModel(new Layer(), new DefaultViewport());
    private ObservedViewportHandler observedViewportHandler;
    private Rectangle visibleArea;
    private boolean adjustingObservedViewport;

    public NavigationCanvas2(NavigationToolView navigationWindow) {
        super(navigationWindow);
        setOpaque(true);
        thumbnailCanvas = new LayerCanvas();
        thumbnailCanvas.setRenderCustomizer(new Layer.RenderFilter() {
            @Override
            public boolean canRender(Layer layer) {
                return layer instanceof ImageLayer;
            }
        });
        thumbnailCanvas.addOverlay(new LayerCanvas.Overlay() {
            public void paintOverlay(LayerCanvas canvas, Graphics2D g) {
                if (visibleArea != null) {
                    g.setColor(new Color(getForeground().getRed(), getForeground().getGreen(),
                                         getForeground().getBlue(), 82));
                    g.fillRect(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height);
                    g.setColor(getForeground());
                    g.draw3DRect(visibleArea.x - 1, visibleArea.y - 1, visibleArea.width + 2, visibleArea.height + 2, true);
                    g.draw3DRect(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height, false);
                }
            }
        });
        add(thumbnailCanvas);

        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        observedViewportHandler = new ObservedViewportHandler();
    }

    @Override
    public boolean isUpdatingImageDisplay() {
        return false;
    }

    @Override
    public void doLayout() {
        thumbnailCanvas.setBounds(getBounds());
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        thumbnailCanvas.getViewport().setViewBounds(new Rectangle(x, y, width, height));
        thumbnailCanvas.zoomAll();
    }

    @Override
    public void handleViewChanged(ProductSceneView oldView, ProductSceneView newView) {
        if (oldView != null) {
            Viewport observedViewport = oldView.getLayerCanvas().getViewport();
            observedViewport.addListener(observedViewportHandler);
        }
        if (newView != null) {
            Viewport observedViewport = newView.getLayerCanvas().getViewport();
            observedViewport.addListener(observedViewportHandler);
            Viewport thumbnailViewport = new DefaultViewport(getBounds(), observedViewport.isModelYAxisDown());
            LayerCanvasModel thumbnailCanvasModel = new DefaultLayerCanvasModel(newView.getRootLayer(), thumbnailViewport);
            thumbnailCanvasModel.getViewport().setOrientation(observedViewport.getOrientation());
            thumbnailCanvas.setModel(thumbnailCanvasModel);
        } else {
            thumbnailCanvas.setModel(NULL_MODEL);
        }
    }

    @Override
    public void updateImage() {
    }

    @Override
    public void updateSlider() {
        Viewport viewport = getNavigationWindow().getCurrentView().getLayerCanvas().getViewport();
        Rectangle viewBounds = viewport.getViewBounds();
        AffineTransform m2vTN = thumbnailCanvas.getViewport().getModelToViewTransform();
        AffineTransform v2mVP = viewport.getViewToModelTransform();
        visibleArea = m2vTN.createTransformedShape(v2mVP.createTransformedShape(viewBounds)).getBounds();
        Rectangle repaintArea = new Rectangle(visibleArea);
        repaintArea.grow(10,10);
        repaint(repaintArea);
    }

    private void adjustObservedViewport() {
        ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view != null) {
            Point location = visibleArea.getLocation();
            thumbnailCanvas.getViewport().getViewToModelTransform().transform(location, location);
            adjustingObservedViewport = true;
            getNavigationWindow().setModelOffset(location.getX(), location.getY());
            adjustingObservedViewport = false;
        }
    }

    private class ObservedViewportHandler implements ViewportListener {

        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            if (!adjustingObservedViewport) {
                if (orientationChanged) {
                    thumbnailCanvas.getViewport().setOrientation(viewport.getOrientation());
                    thumbnailCanvas.zoomAll();
                }
                updateSlider();
            }
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        private Point pickPoint;
        private Point sliderPoint;

        @Override
        public void mousePressed(MouseEvent e) {
            pickPoint = e.getPoint();
            if (!visibleArea.contains(pickPoint)) {
                visibleArea.x = pickPoint.x - visibleArea.width / 2;
                visibleArea.y = pickPoint.y - visibleArea.height / 2;
                repaint();
            }
            sliderPoint = visibleArea.getLocation();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            repaint();
            adjustObservedViewport();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            visibleArea.x = sliderPoint.x + (e.getX() - pickPoint.x);
            visibleArea.y = sliderPoint.y + (e.getY() - pickPoint.y);
            repaint();
            adjustObservedViewport();
        }
    }
}