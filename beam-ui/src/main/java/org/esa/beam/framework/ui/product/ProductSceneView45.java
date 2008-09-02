package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.grender.swing.ViewportScrollPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glevel.MaskMultiLevelImage;
import org.esa.beam.glevel.RoiMultiLevelImage;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

public class ProductSceneView45 extends ProductSceneView {

    private LayerDisplay layerCanvas;

    ProductSceneView45(ProductSceneImage45 sceneImage) {
        super(sceneImage);

        setOpaque(true);
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);
        setLayout(new BorderLayout());

        layerCanvas = new LayerDisplay(sceneImage.getRootLayer(), this);
        final ViewportScrollPane scrollPane = new ViewportScrollPane(layerCanvas);
        add(scrollPane, BorderLayout.CENTER);

        layerCanvas.setPreferredSize(new Dimension(400, 400));

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        layerCanvas.addMouseListener(popupMenuHandler);
        layerCanvas.addKeyListener(popupMenuHandler);

        // todo - this change management is for compatibility reasons only, need better control here!!!
        layerCanvas.getLayer().addListener(new LayerListener() {
            @Override
            public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
                fireLayerContentChanged();
            }

            @Override
            public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
                fireLayerContentChanged();
            }

            @Override
            public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
                fireLayerContentChanged();
            }

            @Override
            public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
                fireLayerContentChanged();
            }
        });

        layerCanvas.getViewport().addListener(new ViewportListener() {
            @Override
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
        configureThumbnailViewport(thumbnailViewport);
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
        thumbnailViewport.moveViewDelta(thumbnailViewport.getBounds().x, thumbnailViewport.getBounds().y);
        thumbnailViewport.rotate(getOrientation());
    }

    @Override
    public void updateImage(ProgressMonitor pm) throws IOException {
        getBaseImageLayer().regenerate();
        fireImageUpdated();
    }

    ImageLayer getBaseImageLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if (layer.getName().equals(getSceneImage45().getName())) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }

    @Override
    public Rectangle2D getModelBounds() {
        return layerCanvas.getLayer().getBounds();
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

    @Override
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
        return getNoDataLayer().isVisible();
    }


    @Override
    public void setNoDataOverlayEnabled(boolean enabled) {
        if (isNoDataOverlayEnabled() != enabled) {
            getNoDataLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
        final String expression = getRaster().getValidMaskExpression();
        if (expression != null) {
            // todo - get color from style, set color
            final LevelImage levelImage = new MaskMultiLevelImage(getRaster().getProduct(), Color.ORANGE, expression,
                    true, new AffineTransform());
            getNoDataLayer().setLevelImage(levelImage);
        } else {
            getNoDataLayer().setLevelImage(LevelImage.NULL);
        }

        fireImageUpdated();
    }

    @Override
    public boolean isROIOverlayEnabled() {
        return getRoiLayer().isVisible();
    }

    @Override
    public void setROIOverlayEnabled(boolean enabled) {
        if (isROIOverlayEnabled() != enabled) {
            getRoiLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
            // todo - get color from style, set color
            final LevelImage levelImage = new RoiMultiLevelImage(getRaster(), Color.RED, new AffineTransform());
            getRoiLayer().setLevelImage(levelImage);
        } else {
            getRoiLayer().setLevelImage(LevelImage.NULL);
        }

        fireImageUpdated();
    }

    @Override
    public RenderedImage getROIImage() {
        final RenderedImage roiImage = getRoiLayer().getImage(0);

        // for compatibility to 42
        if (roiImage == LevelImage.NULL) {
            return null;
        }

        return roiImage;
    }

    @Override
    public void setROIImage(RenderedImage roiImage) {
        // used by MagicStick only
        getRoiLayer().setLevelImage(new DefaultMultiLevelImage(roiImage, new AffineTransform(), 0));
        fireImageUpdated();
    }

    @Override
    public boolean isPinOverlayEnabled() {
        return getPinLayer().isVisible();
    }

    @Override
    public void setPinOverlayEnabled(boolean enabled) {
        if (isPinOverlayEnabled() != enabled) {
            getPinLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public boolean isGcpOverlayEnabled() {
        return getGcpLayer().isVisible();
    }

    @Override
    public void setGcpOverlayEnabled(boolean enabled) {
        if (isGcpOverlayEnabled() != enabled) {
            getGcpLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public boolean isShapeOverlayEnabled() {
        return getFigureLayer().isVisible();
    }

    @Override
    public void setShapeOverlayEnabled(boolean enabled) {
        if (isShapeOverlayEnabled() != enabled) {
            getFigureLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public boolean isGraticuleOverlayEnabled() {
        return getGraticuleLayer().isVisible();
    }

    @Override
    public void setGraticuleOverlayEnabled(boolean enabled) {
        if (isGraticuleOverlayEnabled() != enabled) {
            getGraticuleLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    @Override
    public Figure getCurrentShapeFigure() {
        return getNumFigures() > 0 ? getFigureAt(0) : null;
    }

    @Override
    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        setShapeOverlayEnabled(true);
        final Figure oldShapeFigure = getCurrentShapeFigure();
        if (currentShapeFigure != oldShapeFigure) {
            if (oldShapeFigure != null) {
                getFigureLayer().getFigureList().remove(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                getFigureLayer().addFigure(currentShapeFigure);
            }
        }
    }

    /**
     * Called after VISAT preferences have changed.
     * This behaviour is deprecated since we want to uswe separate style editors for each layers.
     *
     * @param propertyMap
     */
    @Override
    public void setLayerProperties(PropertyMap propertyMap) {

//        setImageProperties(propertyMap); TODO implement

        getFigureLayer().setStyleProperties(propertyMap);
//        getNoDataLayer().setStyle(noDataStyle );
        getGraticuleLayer().setStyleProperties(propertyMap);
//        getPinLayer().setStyleProperties(propertyMap);
//        getGcpLayer().setStyleProperties(propertyMap);

        fireImageUpdated();
    }

    @Override
    public void addPixelPositionListener(PixelPositionListener listener) {
        layerCanvas.addPixelPositionListener(listener);
    }

    @Override
    public void removePixelPositionListener(PixelPositionListener listener) {
        layerCanvas.removePixelPositionListener(listener);
    }

    /**
     * Gets tools which can handle selections.
     *
     * @return
     */
    @Override
    public AbstractTool[] getSelectToolDelegates() {
        return new AbstractTool[0];  // todo - implement me! Check: maybe this isn't even used
        // is used for the selection tool, which can be specified for each layer
        // has been introduced for IAVISA (IFOV selection)
    }

    @Override
    public AffineTransform getBaseImageToViewTransform() {
        AffineTransform viewToModelTransform = layerCanvas.getViewport().getViewToModelTransform();
        AffineTransform modelToImageTransform = getBaseImageLayer().getModelToImageTransform();
        viewToModelTransform.concatenate(modelToImageTransform);
        try {
            return viewToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void synchronizeViewport(ProductSceneView view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {

            Rectangle2D visibleModelBounds = getVisibleModelBounds();
            view.move(visibleModelBounds.getX(), visibleModelBounds.getY());
            view.zoom(getZoomFactor());
            ProductSceneView45 view45 = (ProductSceneView45) view;
            view45.layerCanvas.getViewport().rotate(getOrientation());
        }
    }


    @Override
    public RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha) {
        final Rectangle2D bounds;
        if (entireImage) {
            bounds = getBaseImageLayer().getBounds();
        } else {
            bounds = getVisibleModelBounds();
        }
        final int imageWidth = MathUtils.floorInt(bounds.getWidth());
        final int imageHeight = MathUtils.floorInt(bounds.getHeight());
        final int imageType = useAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        final BufferedImage bi = new BufferedImage(imageWidth, imageHeight, imageType);
        final BufferedImageRendering imageRendering = new BufferedImageRendering(bi);

        final Graphics2D graphics = imageRendering.getGraphics();
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        Viewport snapshotVp = imageRendering.getViewport();
        snapshotVp.setMaxZoomFactor(-1);
        snapshotVp.zoom(bounds);
        snapshotVp.moveViewDelta(snapshotVp.getBounds().x, snapshotVp.getBounds().y);

        getSceneImage45().getRootLayer().render(imageRendering);
        return bi;
    }

    // TODO remove ??? UNUSED
    @Override
    public Tool getTool() {
        return layerCanvas.getTool();
    }

    @Override
    public void setTool(Tool tool) {
        if (tool != null && layerCanvas.getTool() != tool) {
            tool.setDrawingEditor(this);
            setCursor(tool.getCursor());
            layerCanvas.setTool(tool);
        }
    }

    @Override
    public void repaintTool() {
        if (layerCanvas.getTool() != null) {
            repaint(100);
        }
    }

    // TODO remove ??? UNUSED
    public void removeFigure(Figure figure) {
        getFigureLayer().getFigureList().remove(figure);
        if (isShapeOverlayEnabled()) {
            fireImageUpdated();
        }
    }

    // used only internaly --> private ???
    public int getNumFigures() {
        return getFigureLayer().getFigureList().size();
    }

    // used only internaly --> private ???
    public Figure getFigureAt(int index) {
        return getFigureLayer().getFigureList().get(index);
    }

    // TODO remove ??? UNUSED
    public Figure[] getAllFigures() {
        return getFigureLayer().getFigureList().toArray(new Figure[getNumFigures()]);
    }

    //TODO remove ??? UNUSED
    public Figure[] getSelectedFigures() {
        return new Figure[0];
    }

    // TODO remove ??? UNUSED
    public Figure[] getFiguresWithAttribute(String name) {
        return new Figure[0];
    }

    // TODO remove ??? UNUSED
    public Figure[] getFiguresWithAttribute(String name, Object value) {
        return new Figure[0];
    }

    @Override
    protected void copyPixelInfoStringToClipboard() {
        String text = layerCanvas.createPixelInfoString(this);
        SystemUtils.copyToClipboard(text);
    }

    private ImageLayer getNoDataLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("No-data")) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }

    private FigureLayer getFigureLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if ("Figures".equals(layer.getName())) {
                return (FigureLayer) layer;
            }
        }

        return null;
    }

    private ImageLayer getRoiLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("ROI")) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }

    private GraticuleLayer getGraticuleLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("Graticule")) {
                return (GraticuleLayer) layer;
            }
        }

        return null;
    }

    private Layer getPinLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if ("Pins".equals(layer.getName())) {
                return layer;
            }
        }

        return null;
    }

    private Layer getGcpLayer() {
        for (final Layer layer : getSceneImage45().getRootLayer().getChildLayerList()) {
            if ("GCPs".equals(layer.getName())) {
                return layer;
            }
        }

        return null;
    }
}
