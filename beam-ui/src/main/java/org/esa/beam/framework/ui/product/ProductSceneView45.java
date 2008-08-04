package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

class ProductSceneView45 extends ProductSceneView {

    private LayerCanvas layerCanvas;

    ProductSceneView45(ProductSceneImage45 sceneImage) {
        super(sceneImage);

        setOpaque(true);
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);
        setLayout(new BorderLayout());

        layerCanvas = new LayerCanvas(sceneImage.getRootLayer());
        final ViewportScrollPane scrollPane = new ViewportScrollPane(layerCanvas);
        add(scrollPane, BorderLayout.CENTER);

        layerCanvas.setPreferredSize(new Dimension(400, 400));

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
            public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
                fireLayerViewportChanged(orientationChanged);
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
        final BufferedImageRendering imageRendering = new BufferedImageRendering(thumbnailImage);

        final Graphics2D graphics = imageRendering.getGraphics();
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, thumbnailImage.getWidth(), thumbnailImage.getHeight());

        configureThumbnailViewport(imageRendering.getViewport());
        getSceneImage45().getRootLayer().render(imageRendering);
    }

    @Override
    public Rectangle getViewportThumbnailBounds(Rectangle thumbnailArea) {
        final Viewport thumbnailViewport = new DefaultViewport(thumbnailArea);
        thumbnailViewport.setMaxZoomFactor(-1);
        thumbnailViewport.zoom(getRotatedModelBounds());
        thumbnailViewport.moveViewDelta(thumbnailViewport.getBounds().x, thumbnailViewport.getBounds().y);
        thumbnailViewport.rotate(getOrientation());
        final Viewport canvasViewport = layerCanvas.getViewport();
        final Point2D modelOffset = canvasViewport.getViewToModelTransform().transform(canvasViewport.getBounds().getLocation(), null);

        final Point2D tnOffset = thumbnailViewport.getModelToViewTransform().transform(modelOffset, null);
        double scale = DefaultViewport.getScale(canvasViewport.getViewToModelTransform())
                * DefaultViewport.getScale(thumbnailViewport.getModelToViewTransform());

        return new Rectangle((int) Math.floor(tnOffset.getX()),
                             (int) Math.floor(tnOffset.getY()),
                             (int) Math.floor(canvasViewport.getBounds().width * scale),
                             (int) Math.floor(canvasViewport.getBounds().height * scale));
    }

    private void configureThumbnailViewport(Viewport thumbnailViewport) {
        thumbnailViewport.setMaxZoomFactor(-1);
        thumbnailViewport.zoom(getRotatedModelBounds());
        thumbnailViewport.rotate(getOrientation());
        thumbnailViewport.moveViewDelta(thumbnailViewport.getBounds().x, thumbnailViewport.getBounds().y);
    }

    @Override
    public void updateImage(ProgressMonitor pm) throws IOException {
        getBaseImageLayer().regenerate();
        fireImageUpdated();
    }

    private ImageLayer getBaseImageLayer() {
        final Layer layer = layerCanvas.getLayer().getChildLayers().get(0);
        return (ImageLayer) layer;
    }

    @Override
    public Rectangle2D getModelBounds() {
        return getSceneImage45().getRootLayer().getBounds();
    }

    @Override
    public Rectangle2D getVisibleModelBounds() {
        final Viewport viewport = layerCanvas.getViewport();
        return viewport.getViewToModelTransform().createTransformedShape(viewport.getBounds()).getBounds2D();
    }

    @Override
    public Rectangle getVisibleImageBounds() {
        final RenderedImage image = getBaseImageLayer().getImage();
        final Area imageArea = new Area(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        final Area visibleImageArea = new Area(getBaseImageLayer().getModelToImageTransform().createTransformedShape(getVisibleModelBounds()));
        imageArea.intersect(visibleImageArea);
        return imageArea.getBounds();
    }

    @Override
    public void zoom(double viewScale) {
        layerCanvas.getViewport().zoom(viewScale);
    }

    @Override
    public void zoom(Rectangle2D modelRect) {
        layerCanvas.getViewport().zoom(modelRect);
    }

    @Override
    public void zoom(double x, double y, double viewScale) {
        layerCanvas.getViewport().zoom(x, y, viewScale);
    }

    @Override
    public void zoomAll() {
        zoom(layerCanvas.getLayer().getBounds());
    }

    public double getOrientation() {
        return layerCanvas.getViewport().getOrientation();
    }

    @Override
    public double getZoomFactor() {
        return layerCanvas.getViewport().getZoomFactor();
    }

    @Override
    public void move(double modelOffsetX, double modelOffsetY) {
        layerCanvas.getViewport().move(modelOffsetX, modelOffsetY);
    }


    @Override
    public void disposeLayers() {
        getSceneImage45().getRootLayer().dispose();
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
    public AffineTransform getBaseImageToViewTransform() {
        return null;  // todo - implement me!
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
    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
        // todo - implement me!
    }

}
