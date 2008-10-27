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

import java.awt.Graphics2D;
import java.awt.Rectangle;

public class NavigationCanvas2 extends NavigationCanvas {

    private LayerCanvas thumbnailCanvas;
    private static final DefaultLayerCanvasModel NULL_MODEL = new DefaultLayerCanvasModel(new Layer(), new DefaultViewport());
    private ObservedViewportHandler observedViewportHandler;

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
            public void paint(LayerCanvas canvas, Graphics2D graphics) {
                graphics.drawOval(0,0,canvas.getWidth(), canvas.getHeight());
            }
        });
        observedViewportHandler = new ObservedViewportHandler();
        add(thumbnailCanvas);
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
    public void handleViewChanged() {
        ProductSceneView view = getNavigationWindow().getCurrentView();
        if (view != null) {
            // todo - remove observedViewportHandler from old view (nf - 24.10.2008)
            Viewport observedViewport = view.getLayerCanvas().getViewport();
            observedViewport.addListener(observedViewportHandler);
            Viewport thumbnailViewport = new DefaultViewport(getBounds(), observedViewport.isModelYAxisDown());
            LayerCanvasModel thumbnailCanvasModel = new DefaultLayerCanvasModel(view.getRootLayer(), thumbnailViewport);
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
        // todo
    }

    private class ObservedViewportHandler implements ViewportListener {

        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            if (orientationChanged) {
                thumbnailCanvas.getViewport().setOrientation(viewport.getOrientation());
                thumbnailCanvas.zoomAll();
            }
        }
    }
}