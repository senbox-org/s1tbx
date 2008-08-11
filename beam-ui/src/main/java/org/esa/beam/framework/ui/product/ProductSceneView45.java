package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
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
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.glevel.MaskMultiLevelImage;
import org.esa.beam.glevel.RoiMultiLevelImage;
import org.esa.beam.util.PropertyMap;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

class ProductSceneView45 extends ProductSceneView {

    private MyLayerCanvas layerCanvas;
//    private Tool currentTool;

    ProductSceneView45(ProductSceneImage45 sceneImage) {
        super(sceneImage);

        setOpaque(true);
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);
        setLayout(new BorderLayout());

        layerCanvas = new MyLayerCanvas(sceneImage.getRootLayer());
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

    private ImageLayer getBaseImageLayer() {
        final Layer layer = layerCanvas.getLayer().getChildLayerList().get(0);
        return (ImageLayer) layer;
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
        return false;  // todo - implement me! See 4.2 layer impl.
    }

    @Override
    public void setGraticuleOverlayEnabled(boolean enabled) {
        // todo - implement me! See 4.2 layer impl.
    }

    @Override
    public Figure getRasterROIShapeFigure() {
        return null;  // todo - implement me!
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
                getFigureLayer().getFigureList().add(currentShapeFigure);
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
        // todo - implement me!
    }

    @Override
    public void addPixelPositionListener(PixelPositionListener listener) {
        // todo - implement me! Use viewport.viewToModelTransform + image.modelToImageTransform to fire pixel change
    }

    @Override
    public void removePixelPositionListener(PixelPositionListener listener) {
        // todo - implement me!
    }

    /**
     * Gets tools which can handle selections.
     *
     * @return
     */
    @Override
    public AbstractTool[] getSelectToolDelegates() {
        return new AbstractTool[0];  // todo - implement me! Check: maybe this isn't even used
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
        return layerCanvas.tool;
    }

    @Override
    public void setTool(Tool tool) {
        if (tool != null && layerCanvas.tool != tool) {
            tool.setDrawingEditor(this);
            setCursor(tool.getCursor());
            layerCanvas.setTool(tool);
        }
    }

    @Override
    public void repaintTool() {
        if (layerCanvas.tool != null) {
            System.out.println("repaintTool: " + layerCanvas.tool.getClass().toString());
//            toolRepaintRequested = true;
            repaint(100);
        }
    }

//    private final void drawToolNoTransf(Graphics2D g2d) {
//        if (currentTool != null) {
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//            if (currentTool.getDrawable() != null) {
//                currentTool.getDrawable().draw(g2d);
//            }
//            // reset rendering hints ?????? TODO
//        }
//    }

    public void removeFigure(Figure figure) {
        getFigureLayer().getFigureList().remove(figure);
        if (isShapeOverlayEnabled()) {
            fireImageUpdated();
        }
    }

    public int getNumFigures() {
        return getFigureLayer().getFigureList().size();
    }

    public Figure getFigureAt(int index) {
        return getFigureLayer().getFigureList().get(index);
    }

    public Figure[] getAllFigures() {
        return getFigureLayer().getFigureList().toArray(new Figure[getNumFigures()]);
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

//    @Override
//    protected void paintComponent(Graphics g) {
//        layerCanvas.paint(g);
//        if (g instanceof Graphics2D) {
//            Graphics2D g2d = (Graphics2D) g;
//            drawToolNoTransf(g2d);
//        }
//    }

    private ImageLayer getNoDataLayer() {
        return (ImageLayer) getSceneImage45().getRootLayer().getChildLayerList().get(1);
    }

    private ProductSceneImage45.FigureLayer getFigureLayer() {
        return (ProductSceneImage45.FigureLayer) getSceneImage45().getRootLayer().getChildLayerList().get(2);
    }

    private ImageLayer getRoiLayer() {
        return (ImageLayer) getSceneImage45().getRootLayer().getChildLayerList().get(3);
    }

    private Layer getPinLayer() {
        return getSceneImage45().getRootLayer().getChildLayerList().get(5);
    }

    private Layer getGcpLayer() {
        return getSceneImage45().getRootLayer().getChildLayerList().get(6);
    }

    private static class MyLayerCanvas extends LayerCanvas {
        Tool tool;
        private int pixelX = -1;
        private int pixelY = -1;

//        private MyLayerCanvas() {
//            super();
//        }

        private MyLayerCanvas(Layer layer) {
            super(layer);
        }

//        private MyLayerCanvas(Layer layer, Viewport viewport) {
//            super(layer, viewport);
//        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;

                if (tool != null && tool.isActive()) {
                    final Viewport vp = getViewport();
                    final AffineTransform transformSave = g2d.getTransform();
                    try {
                        final AffineTransform transform = new AffineTransform();
                        transform.concatenate(vp.getModelToViewTransform());
                        g2d.setTransform(transform);
                        drawToolNoTransf(g2d);
                    } finally {
                        g2d.setTransform(transformSave);
                    }
                }
            }
        }

        private void drawToolNoTransf(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (tool.getDrawable() != null) {
                System.out.println("DRAW_TOOL:" + tool.getClass().toString());
                tool.getDrawable().draw(g2d);
            }
            // reset rendering hints ?????? TODO
        }

        private void setTool(Tool tool) {
            Tool oldTool = this.tool;
            this.tool = tool;
            firePropertyChange("tool", oldTool, tool);
        }

        final synchronized void fireToolEvent(MouseEvent e) {
//        Debug.trace("fireToolEvent "  + e);
            if (tool != null) {
//                ToolInputEvent toolInputEvent = createToolInputEvent(e);
//                tool.handleEvent(toolInputEvent);
            }
        }

        private void setPixelPos(MouseEvent e, boolean showBorder) {
            Point p = e.getPoint();
            final Point2D modelP = getViewport().getViewToModelTransform().transform(p, null);
            int pixelX = (int) Math.floor(modelP.getX());
            int pixelY = (int) Math.floor(modelP.getY());
            if (pixelX != this.pixelX || pixelY != this.pixelY) {
//                if (isPixelBorderDisplayEnabled() && (showBorder || pixelBorderDrawn)) {
//                    drawPixelBorder(pixelX, pixelY, showBorder);
//                }
                setPixelPos(e, pixelX, pixelY);
            }
        }


        final void setPixelPos(MouseEvent e, int pixelX, int pixelY) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            if (e.getID() != MouseEvent.MOUSE_EXITED) {
//                firePixelPosChanged(e, this.pixelX, this.pixelY);
            } else {
//                firePixelPosNotAvailable();
            }
        }

        private final class PixelPosUpdater implements MouseInputListener {


            /**
             * Invoked when the mouse has been clicked on a component.
             */
            public final void mouseClicked(MouseEvent e) {
                updatePixelPos(e, false);
            }

            /**
             * Invoked when the mouse enters a component.
             */
            public final void mouseEntered(MouseEvent e) {
                updatePixelPos(e, false);
            }

            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public final void mousePressed(MouseEvent e) {
                updatePixelPos(e, false);
            }

            /**
             * Invoked when a mouse button has been released on a component.
             */
            public final void mouseReleased(MouseEvent e) {
                updatePixelPos(e, false);
            }

            /**
             * Invoked when the mouse exits a component.
             */
            public final void mouseExited(MouseEvent e) {
                updatePixelPos(e, false);
            }

            /**
             * Invoked when a mouse button is pressed on a component and then dragged.  Mouse drag events will continue
             * to be delivered to the component where the first originated until the mouse button is released
             * (regardless of whether the mouse position is within the bounds of the component).
             */
            public final void mouseDragged(MouseEvent e) {
                updatePixelPos(e, true);
            }

            /**
             * Invoked when the mouse button has been moved on a component (with no buttons no down).
             */
            public final void mouseMoved(MouseEvent e) {
                updatePixelPos(e, true);
            }


            private void updatePixelPos(MouseEvent e, boolean showBorder) {
                setPixelPos(e, showBorder);
                fireToolEvent(e);
            }
        }

    }
}
