package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The class <code>ProductSceneView</code> is a high-level image display component for color index/RGB images created
 * from one or more raster datasets of a data product.
 * <p/>
 * <p>It is also capable of displaying a graticule (geographical grid) and a ROI associated with a displayed raster
 * dataset.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class ProductSceneView extends BasicView implements ProductNodeView, DrawingEditor, PropertyMapChangeListener, PixelInfoFactory {
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_GRAPHICS_ANTIALIASING = "graphics.antialiasing";
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_IMAGE_INTERPOLATION = "image.interpolation";
    /**
     * Property name for the image histogram matching type
     */
    public static final String PROPERTY_KEY_HISTOGRAM_MATCHING = "graphics.histogramMatching";
    public static String IMAGE_INTERPOLATION_NEAREST_NEIGHBOUR = "Nearest Neighbour";
    public static String IMAGE_INTERPOLATION_BILINEAR = "Bi-Linear Interpolation";
    public static String IMAGE_INTERPOLATION_BICUBIC = "Bi-Cubic Interpolation";
    public static String IMAGE_INTERPOLATION_SYSTEM_DEFAULT = "System Default";
    public static String DEFAULT_IMAGE_INTERPOLATION_METHOD = IMAGE_INTERPOLATION_SYSTEM_DEFAULT;
    public static final Color DEFAULT_IMAGE_BORDER_COLOR = new Color(204, 204, 255);
    public static final Color DEFAULT_IMAGE_BACKGROUND_COLOR = new Color(51, 51, 51);
    public static final double DEFAULT_IMAGE_BORDER_SIZE = 2.0;
    public static final int DEFAULT_IMAGE_VIEW_BORDER_SIZE = 64;
    private ProductSceneImage sceneImage;
    private ArrayList<ImageUpdateListener> imageUpdateListenerList;
    private ArrayList<LayerContentListener> layerContentListenerList;
    private ArrayList<LayerViewportListener> layerViewportListenerList;
    private RasterChangeHandler rasterChangeHandler;

    public static ProductSceneView create(ProductSceneImage sceneImage) {
        Guardian.assertNotNull("sceneImage", sceneImage);
        return new ProductSceneView45((ProductSceneImage45) sceneImage);
    }

    protected ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");

        this.sceneImage = sceneImage;

        rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        imageUpdateListenerList = new ArrayList<ImageUpdateListener>(5);
        layerContentListenerList = new ArrayList<LayerContentListener>(5);
        layerViewportListenerList = new ArrayList<LayerViewportListener>(5);
    }

    ProductSceneImage getSceneImage() {
        return sceneImage;
    }

    /**
     * Returns the currently visible product node.
     */
    public ProductNode getVisibleProductNode() {
        return getRaster();
    }

    /**
     * Creates a string containing all available information at the given pixel position. The string returned is a line
     * separated text with each line containing a key/value pair.
     *
     * @param pixelX the pixel X co-ordinate
     * @param pixelY the pixel Y co-ordinate
     * @return the info string at the given position
     */
    public String createPixelInfoString(int pixelX, int pixelY) {
        return getProduct() != null ? getProduct().createPixelInfoString(pixelX, pixelY) : "";
    }

    /**
     * Called if the property map changed. Simply calls {@link #setLayerProperties(org.esa.beam.util.PropertyMap)}.
     */
    public void propertyMapChanged(PropertyMap propertyMap) {
        setLayerProperties(propertyMap);
    }

    /**
     * @return all layer manager listeners of this layer.
     */
    public ImageUpdateListener[] getImageUpdateListeners() {
        return imageUpdateListenerList.toArray(new ImageUpdateListener[imageUpdateListenerList.size()]);
    }

    /**
     * Adds a layer manager listener to this layer.
     *
     * @param listener The listener
     */
    public void addImageUpdateListener(ImageUpdateListener listener) {
        if (listener != null && !imageUpdateListenerList.contains(listener)) {
            imageUpdateListenerList.add(listener);
        }
    }

    /**
     * Removes a layer manager listener from this layer.
     *
     * @param listener The listener
     */
    public void removeImageUpdateListener(ImageUpdateListener listener) {
        if (listener != null) {
            imageUpdateListenerList.remove(listener);
        }
    }

    protected void fireImageUpdated() {
        for (ImageUpdateListener listener : imageUpdateListenerList.toArray(new ImageUpdateListener[imageUpdateListenerList.size()])) {
            listener.handleImageUpdated(this);
        }
    }

    public void addLayerContentListener(LayerContentListener listener) {
        if (listener != null && !layerContentListenerList.contains(listener)) {
            layerContentListenerList.add(listener);
        }
    }

    public void removeLayerContentListener(LayerContentListener listener) {
        if (listener != null) {
            layerContentListenerList.remove(listener);
        }
    }

    protected void fireLayerContentChanged() {
        for (LayerContentListener listener : layerContentListenerList) {
            listener.layerContentChanged(getRaster());
        }
    }

    public void addLayerViewportListener(LayerViewportListener listener) {
        if (listener != null && !layerViewportListenerList.contains(listener)) {
            layerViewportListenerList.add(listener);
        }
    }

    public void removeLayerViewportListener(LayerViewportListener listener) {
        if (listener != null) {
            layerViewportListenerList.remove(listener);
        }
    }

    protected void fireLayerViewportChanged(boolean orientationChanged) {
        for (LayerViewportListener listener : layerViewportListenerList.toArray(new LayerViewportListener[layerViewportListenerList.size()])) {
            listener.layerViewportChanged(orientationChanged);
        }
    }

    /**
     * If the <code>preferredSize</code> has been set to a
     * non-<code>null</code> value just returns it.
     * If the UI delegate's <code>getPreferredSize</code>
     * method returns a non <code>null</code> value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the <code>preferredSize</code> property
     * @see #setPreferredSize
     * @see javax.swing.plaf.ComponentUI
     */
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        } else if (getImageDisplayComponent() != null) {
            return getImageDisplayComponent().getPreferredSize();
        } else {
            return super.getPreferredSize();
        }
    }

    @Override
    public JPopupMenu createPopupMenu(Component component) {
        return null;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        JPopupMenu popupMenu = new JPopupMenu();
        addCopyPixelInfoToClipboardMenuItem(popupMenu);
        getCommandUIFactory().addContextDependentMenuItems("image", popupMenu);
        Product product = getProduct();
        CommandUIFactory commandUIFactory = getCommandUIFactory();
        if (product.getPinGroup().getSelectedNode() != null) {
            if (commandUIFactory != null) {
                commandUIFactory.addContextDependentMenuItems("pin", popupMenu);
            }
        }
        if (commandUIFactory != null) {
            commandUIFactory.addContextDependentMenuItems("subsetFromView", popupMenu);
        }
        return popupMenu;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {

        getRaster().getProduct().removeProductNodeListener(rasterChangeHandler);
        for (int i = 0; i < getSceneImage().getRasters().length; i++) {
            final RasterDataNode raster = getSceneImage().getRasters()[i];
            if (raster instanceof RGBChannel) {
                RGBChannel rgbChannel = (RGBChannel) raster;
                rgbChannel.dispose();
            }
            getSceneImage().getRasters()[i] = null;
        }
        sceneImage = null;
        imageUpdateListenerList.clear();
        imageUpdateListenerList = null;

        if (getImageDisplayComponent() != null) {
            // ensure that imageDisplay.dispose() is run in the EDT
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    disposeImageDisplayComponent();
                }
            });
        }

        super.dispose();
    }

    /**
     * @return the associated product.
     */
    public Product getProduct() {
        return getRaster().getProduct();
    }

    public String getSceneName() {
        return getSceneImage().getName();
    }

    public ImageInfo getImageInfo() {
        return getSceneImage().getImageInfo();
    }

    public void setImageInfo(ImageInfo imageInfo) {
        getSceneImage().setImageInfo(imageInfo);
    }

    /**
     * Gets the number of raster datasets.
     *
     * @return the number of raster datasets, always <code>1</code> for single banded palette images or <code>3</code>
     *         for RGB images
     */
    public int getNumRasters() {
        return getSceneImage().getRasters().length;
    }

    /**
     * Gets the product raster with the specified index.
     *
     * @param index the zero-based product raster index
     * @return the product raster with the given index
     */
    public RasterDataNode getRaster(int index) {
        return getSceneImage().getRasters()[index];
    }

    /**
     * Gets the product raster of a single banded view.
     *
     * @return the product raster, or <code>null</code> if this is a 3-banded RGB view
     */
    public RasterDataNode getRaster() {
        return getSceneImage().getRasters()[0];
    }

    /**
     * Gets all rasters of this view.
     *
     * @return all rasters of this view, array size is either 1 or 3 (RGB)
     */
    public RasterDataNode[] getRasters() {
        return getSceneImage().getRasters();
    }

    public void setRasters(RasterDataNode[] rasters) {
        getSceneImage().setRasters(rasters);
    }

    public boolean isRGB() {
        return getSceneImage().getRasters().length >= 3;
    }

    /**
     * Adds a new figure to the drawing.
     */
    public void addFigure(Figure figure) {
        Guardian.assertNotNull("figure", figure);

        int insertMode = 0; // replace
        ToolInputEvent toolInputEvent = (ToolInputEvent) figure.getAttribute(Figure.TOOL_INPUT_EVENT_KEY);
        if (toolInputEvent != null && toolInputEvent.getMouseEvent() != null) {
            MouseEvent mouseEvent = toolInputEvent.getMouseEvent();
            if ((mouseEvent.isShiftDown())) {
                insertMode = +1; // add
            } else if ((mouseEvent.isControlDown())) {
                insertMode = -1; // subtract
            }
        }

        Figure oldFigure = getCurrentShapeFigure();

        if (insertMode == 0 || oldFigure == null) {
            setCurrentShapeFigure(figure);
            return;
        }

        Shape shape = figure.getShape();
        if (shape == null) {
            return;
        }

        Area area1 = oldFigure.getAsArea();
        Area area2 = figure.getAsArea();
        if (insertMode == 1) {
            area1.add(area2);
        } else {
            area1.subtract(area2);
        }

        setCurrentShapeFigure(ShapeFigure.createArbitraryArea(area1, figure.getAttributes()));
    }

    public abstract void updateImage(ProgressMonitor pm) throws IOException;

    public abstract boolean isNoDataOverlayEnabled();

    public abstract void setNoDataOverlayEnabled(boolean enabled);

    public abstract boolean isGraticuleOverlayEnabled();

    public abstract void setGraticuleOverlayEnabled(boolean enabled);

    public abstract boolean isPinOverlayEnabled();

    public abstract void setPinOverlayEnabled(boolean enabled);

    public abstract boolean isGcpOverlayEnabled();

    public abstract void setGcpOverlayEnabled(boolean enabled);

    public abstract boolean isROIOverlayEnabled();

    public abstract void setROIOverlayEnabled(boolean enabled);

    public abstract boolean isShapeOverlayEnabled();

    public abstract void setShapeOverlayEnabled(boolean enabled);

    public abstract RenderedImage getROIImage();

    public abstract void setROIImage(RenderedImage roiImage);

    public abstract void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception;

    public Figure getRasterROIShapeFigure() {
        if (getRaster().getROIDefinition() != null) {
            return getRaster().getROIDefinition().getShapeFigure();
        }
        return null;
    }

    public abstract Figure getCurrentShapeFigure();

    public abstract void setCurrentShapeFigure(Figure currentShapeFigure);

    public abstract void setLayerProperties(PropertyMap propertyMap);

    public abstract void addPixelPositionListener(PixelPositionListener listener);

    public abstract void removePixelPositionListener(PixelPositionListener listener);

    public abstract AbstractTool[] getSelectToolDelegates();

    public abstract void disposeLayers();

    public abstract JComponent getImageDisplayComponent();

    public abstract AffineTransform getBaseImageToViewTransform();

    /**
     * @return the visible image area in pixel coordinates
     */
    public abstract Rectangle getVisibleImageBounds();

    /**
     * @return the visible area in model coordinates
     */
    public abstract Rectangle2D getVisibleModelBounds();

    /**
     * @return the model bounds in model coordinates
     */
    public abstract Rectangle2D getModelBounds();

    public abstract double getOrientation();

    public abstract double getZoomFactor();

    public abstract void zoom(Rectangle2D modelRect);

    public abstract void zoom(double x, double y, double viewScale);

    public abstract void zoom(double viewScale);

    public abstract void zoomAll();

    public abstract void move(double modelOffsetX, double modelOffsetY);

    public abstract void synchronizeViewport(ProductSceneView view);

    public abstract RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha);

    public abstract Tool getTool();

    public abstract void setTool(Tool tool);

    public abstract void repaintTool();

    protected abstract void copyPixelInfoStringToClipboard();

    protected abstract void disposeImageDisplayComponent();

    public abstract void renderThumbnail(BufferedImage thumbnailImage) ;

    public abstract Rectangle getViewportThumbnailBounds(Rectangle thumbnailArea);

    public abstract void updateNoDataImage(ProgressMonitor pm) throws Exception;

    private void addCopyPixelInfoToClipboardMenuItem(JPopupMenu popupMenu) {
        JMenuItem menuItem = new JMenuItem("Copy Pixel-Info to Clipboard");
        menuItem.setMnemonic('C');
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copyPixelInfoStringToClipboard();
            }
        });
        popupMenu.add(menuItem);
        popupMenu.addSeparator();
    }

    public Rectangle2D getRotatedModelBounds() {
        final Rectangle2D modelBounds = getModelBounds();
        final double orientation = getOrientation();
        if (orientation != 0) {
            final AffineTransform t = new AffineTransform();
            t.rotate(orientation, modelBounds.getCenterX(), modelBounds.getCenterY());
            return t.createTransformedShape(modelBounds).getBounds2D();
        }
        return modelBounds;
    }

    public static interface ImageUpdateListener {

        void handleImageUpdated(ProductSceneView view);
    }

    /**
     * A band that is used as an RGB channel for RGB image views.
     * These bands shall not be added to {@link org.esa.beam.framework.datamodel.Product}s but they are always owned by the {@link org.esa.beam.framework.datamodel.Product}
     * passed into the constructor.
     */
    public static class RGBChannel extends VirtualBand {

        /**
         * Constructs a new RGB image view band.
         *
         * @param product    the product which takes the ownership
         * @param name       the band's name
         * @param expression the expression
         */
        public RGBChannel(final Product product, final String name, final String expression) {
            super(name,
                  ProductData.TYPE_FLOAT32,
                  product.getSceneRasterWidth(),
                  product.getSceneRasterHeight(),
                  expression);
            setOwner(product);
        }
    }

    protected class RasterChangeHandler implements ProductNodeListener {

        public void nodeChanged(final ProductNodeEvent event) {
            repaintView();
        }

        public void nodeDataChanged(final ProductNodeEvent event) {
            repaintView();
        }

        public void nodeAdded(final ProductNodeEvent event) {
            repaintView();
        }

        public void nodeRemoved(final ProductNodeEvent event) {
            repaintView();
        }

        private void repaintView() {
            repaint(100);
        }
    }

    protected final class ZoomHandler implements MouseWheelListener {

        public void mouseWheelMoved(MouseWheelEvent e) {
            int notches = e.getWheelRotation();
            double currentViewScale = getZoomFactor();
            if (notches < 0) {
                zoom(currentViewScale * 1.1f);
            } else {
                zoom(currentViewScale * 0.9f);
            }
        }
    }

    public interface LayerContentListener {
        void layerContentChanged(RasterDataNode raster);
    }

    public interface LayerViewportListener {
        void layerViewportChanged(boolean orientationChanged);
    }
}
