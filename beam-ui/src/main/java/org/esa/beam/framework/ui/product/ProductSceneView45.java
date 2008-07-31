package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.grender.swing.ViewportScrollPane;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.util.PropertyMap;

import javax.swing.JComponent;
import javax.swing.plaf.DimensionUIResource;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

class ProductSceneView45 extends ProductSceneView {

    LayerCanvas layerCanvas;

    ProductSceneView45(ProductSceneImage45 sceneImage) {
        super(sceneImage);

        setOpaque(true);
        setLayout(new BorderLayout());

        layerCanvas = new LayerCanvas(sceneImage.getRootLayer());
        final ViewportScrollPane scrollPane = new ViewportScrollPane(layerCanvas);
        add(scrollPane, BorderLayout.CENTER);

        layerCanvas.setPreferredSize(new Dimension(400,400));

        setPixelInfoFactory(this);

        // todo - this change management is for compatibility reasons only, need better control here!!!
        layerCanvas.getLayer().addListener(new LayerListener() {
            public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
                fireLayerContentChanged();
            }

            public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
                fireLayerContentChanged();
            }

            public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
                fireLayerContentChanged();
            }

            public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
                fireLayerContentChanged();
            }
        });

        layerCanvas.getViewport().addListener(new ViewportListener() {
            public void handleViewportChanged(Viewport viewport) {
                fireLayerViewportChanged();
            }
        });

    }

    public Layer getRootLayer() {
        return getSceneImage45().getRootLayer();
    }

    ProductSceneImage45 getSceneImage45() {
        return (ProductSceneImage45) getSceneImage();
    }

    @Override
    public Rectangle2D getModelBounds() {
        return getSceneImage45().getRootLayer().getBounds();
    }

    @Override
    public JComponent getImageDisplayComponent() {
        return layerCanvas;
    }

    @Override
    protected PixelInfoFactory getPixelInfoFactory() {
        return this;
    }

    @Override
    protected void disposeImageDisplayComponent() {
    }

    @Override
    public void renderThumbnail(BufferedImage thumbnailImage) {
        final BufferedImageRendering imageRendering = new BufferedImageRendering(thumbnailImage, new DefaultViewport());
        getSceneImage45().getRootLayer().render(imageRendering);
    }

    @Override
    public boolean isNoDataOverlayEnabled() {
        return false;  // todo - implement me!
    }


    @Override
    public void setNoDataOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public boolean isGraticuleOverlayEnabled() {
        return false;  // todo - implement me!
    }

    @Override
    public void setGraticuleOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public boolean isPinOverlayEnabled() {
        return false;  // todo - implement me!
    }

    @Override
    public void setPinOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public boolean isGcpOverlayEnabled() {
        return false;  // todo - implement me!
    }

    @Override
    public void setGcpOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public boolean isROIOverlayEnabled() {
        return false;  // todo - implement me!
    }

    @Override
    public void setROIOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public boolean isShapeOverlayEnabled() {
        return false;  // todo - implement me!
    }

    @Override
    public void setShapeOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    @Override
    public RenderedImage getROIImage() {
        return null;  // todo - implement me!
    }

    @Override
    public void setROIImage(RenderedImage roiImage) {
        // todo - implement me!
    }

    @Override
    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        // todo - implement me!
    }

    @Override
    public Figure getRasterROIShapeFigure() {
        return null;  // todo - implement me!
    }

    @Override
    public Figure getCurrentShapeFigure() {
        return null;  // todo - implement me!
    }

    @Override
    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        // todo - implement me!
    }

    @Override
    public void setLayerProperties(PropertyMap propertyMap) {
        // todo - implement me!
    }

    @Override
    public void addPixelPositionListener(PixelPositionListener listener) {
        // todo - implement me!
    }

    @Override
    public void removePixelPositionListener(PixelPositionListener listener) {
        // todo - implement me!
    }

    @Override
    public AbstractTool[] getSelectToolDelegates() {
        return new AbstractTool[0];  // todo - implement me!
    }

    @Override
    public void disposeLayers() {
        // todo - implement me!
    }

    @Override
    public AffineTransform getBaseImageToViewTransform() {
        return null;  // todo - implement me!
    }

    @Override
    public Rectangle2D getVisibleModelBounds() {
        return null;  // todo - implement me!
    }

    @Override
    public double getViewScale() {
        return 0;  // todo - implement me!
    }

    @Override
    public void zoom(Rectangle rect) {
        // todo - implement me!
    }

    @Override
    public void zoom(double x, double y, double viewScale) {
        // todo - implement me!
    }

    @Override
    public void zoom(double viewScale) {
        // todo - implement me!
    }

    @Override
    public void zoomAll() {
        // todo - implement me!
    }

    @Override
    @Deprecated
    public void setModelOffset(double modelOffsetX, double modelOffsetY) {
        // todo - implement me!
    }

    @Override
    public void synchronizeViewport(ProductSceneView view) {
        // todo - implement me!
    }


    @Override
    public RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha) {
        return null;  // todo - implement me!
    }

    @Override
    public Tool getTool() {
        return null;  // todo - implement me!
    }

    @Override
    public void setTool(Tool tool) {
        // todo - implement me!
    }

    @Override
    public void repaintTool() {
        // todo - implement me!
    }

    public void removeFigure(Figure figure) {
        // todo - implement me!
    }

    public int getNumFigures() {
        return 0;  // todo - implement me!
    }

    public Figure getFigureAt(int index) {
        return null;  // todo - implement me!
    }

    public Figure[] getAllFigures() {
        return new Figure[0];  // todo - implement me!
    }

    public Figure[] getSelectedFigures() {
        return new Figure[0];  // todo - implement me!
    }

    public Figure[] getFiguresWithAttribute(String name) {
        return new Figure[0];  // todo - implement me!
    }

    public Figure[] getFiguresWithAttribute(String name, Object value) {
        return new Figure[0];  // todo - implement me!
    }

    @Override
    protected void copyPixelInfoStringToClipboard() {
        // todo - implement me!
    }

    @Override
    protected void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        // todo - implement me!
    }

    @Override
    public void updateImage(ProgressMonitor pm) throws IOException {
        // todo - implement me!
    }

    @Override
    public Rectangle getVisibleImageBounds() {
        return null;  // todo - implement me!
    }

    @Override
    public RenderedImage updateNoDataImage(ProgressMonitor pm) throws Exception {
        return null;  // todo - implement me!
    }

}
