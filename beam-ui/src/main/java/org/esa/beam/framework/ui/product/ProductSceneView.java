package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.ViewportScrollPane;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glevel.MaskImageMultiLevelSource;
import org.esa.beam.glevel.RoiImageMultiLevelSource;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.MathUtils;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
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
public class ProductSceneView extends BasicView implements ProductNodeView, DrawingEditor, PropertyMapChangeListener, PixelInfoFactory {
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_GRAPHICS_ANTIALIASING = "graphics.antialiasing";
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_IMAGE_INTERPOLATION = "image.interpolation";
    /**
     * Name of property which switsches display of af a navigataion control in the image view.
     */
    public static final String PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN = "image.navControlShown";

    /**
     * Property name for the image histogram matching type
     * @deprecated
     */
    @Deprecated
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
    private ArrayList<ImageUpdateListener> imageUpdateListenerList;
    private ArrayList<LayerContentListener> layerContentListenerList;
    private ArrayList<LayerViewportListener> layerViewportListenerList;
    private RasterChangeHandler rasterChangeHandler;
    
    private ProductSceneImage sceneImage;
    private LayerDisplay layerCanvas;


    public ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");

        this.sceneImage = sceneImage;

        rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        imageUpdateListenerList = new ArrayList<ImageUpdateListener>(5);
        layerContentListenerList = new ArrayList<LayerContentListener>(5);
        layerViewportListenerList = new ArrayList<LayerViewportListener>(5);
        
        setOpaque(true);
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR); // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        setLayout(new BorderLayout());
        layerCanvas = new LayerDisplay(sceneImage.getRootLayer(), this);
        final ViewportScrollPane scrollPane = new ViewportScrollPane(layerCanvas);
        add(scrollPane, BorderLayout.CENTER);

        final boolean navControlShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, true);
        layerCanvas.setNavControlShown(navControlShown);
        layerCanvas.setPreferredSize(new Dimension(400, 400));

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        layerCanvas.addMouseListener(popupMenuHandler);
        layerCanvas.addKeyListener(popupMenuHandler);
        layerCanvas.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                final Viewport viewport = layerCanvas.getViewport();
                final int wheelRotation = e.getWheelRotation();
                final double newZoomFactor = viewport.getZoomFactor() * Math.pow(1.1, wheelRotation);
                viewport.zoom(newZoomFactor);
            }
        });

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

    ProductSceneImage getSceneImage() {
        return sceneImage;
    }
    
    public Layer getRootLayer() {
        return sceneImage.getRootLayer();
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

    public void updateImage(ProgressMonitor pm) throws IOException {
        getBaseImageLayer().regenerate();
        fireImageUpdated();
    }
    
    public ImageLayer getBaseImageLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if (layer.getName().equals(getSceneImage().getName())) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }
    
    public boolean isNoDataOverlayEnabled() {
        final ImageLayer noDataLayer = getNoDataLayer();
        return noDataLayer != null && noDataLayer.isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        if (isNoDataOverlayEnabled() != enabled) {
            getNoDataLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    public boolean isGraticuleOverlayEnabled() {
        final GraticuleLayer graticuleLayer = getGraticuleLayer();
        return graticuleLayer != null && graticuleLayer.isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        if (isGraticuleOverlayEnabled() != enabled) {
            getGraticuleLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    public boolean isPinOverlayEnabled() {
        final Layer pinLayer = getPinLayer();
        return pinLayer != null && pinLayer.isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        if (isPinOverlayEnabled() != enabled) {
            getPinLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }
    public boolean isGcpOverlayEnabled() {
        final Layer gcpLayer = getGcpLayer();
        return gcpLayer != null && gcpLayer.isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        if (isGcpOverlayEnabled() != enabled) {
            getGcpLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    public boolean isShapeOverlayEnabled() {
        final FigureLayer figureLayer = getFigureLayer();
        return figureLayer != null && figureLayer.isVisible();
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        if (isShapeOverlayEnabled() != enabled) {
            getFigureLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    public boolean isROIOverlayEnabled() {
        final ImageLayer roiLayer = getRoiLayer();
        return roiLayer != null && roiLayer.isVisible();
    }

    public void setROIOverlayEnabled(boolean enabled) {
        if (isROIOverlayEnabled() != enabled) {
            getRoiLayer().setVisible(enabled);
            fireImageUpdated();
        }
    }

    public RenderedImage getROIImage() {
        final ImageLayer roiLayer = getRoiLayer();

        if (roiLayer == null) {
            return null;
        }

        final RenderedImage roiImage = roiLayer.getImage(0);

        // for compatibility to 42
        if (roiImage == MultiLevelSource.NULL) {
            return null;
        }

        return roiImage;
    }

    public void setROIImage(RenderedImage roiImage) {
        // used by MagicStick only
        ImageLayer roiLayer = getRoiLayer();
        if (roiLayer != null) {
            MultiLevelModel model = roiLayer.getMultiLevelSource().getModel();
            roiLayer.setMultiLevelSource(new DefaultMultiLevelSource(roiImage, model));
            fireImageUpdated();
        }
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        final ImageLayer roiLayer = getRoiLayer();
        if (roiLayer != null) {
            if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
                // todo - get color from style, set color
                final MultiLevelSource multiLevelSource = RoiImageMultiLevelSource.create(getRaster(), Color.RED, new AffineTransform());
                roiLayer.setMultiLevelSource(multiLevelSource);
            } else {
                roiLayer.setMultiLevelSource(MultiLevelSource.NULL);
            }

            fireImageUpdated();
        }
    }

    public Figure getRasterROIShapeFigure() {
        if (getRaster().getROIDefinition() != null) {
            return getRaster().getROIDefinition().getShapeFigure();
        }
        return null;
    }

    public Figure getCurrentShapeFigure() {
        return getNumFigures() > 0 ? getFigureAt(0) : null;
    }

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
     * @param configuration
     */
    public void setLayerProperties(PropertyMap configuration) {

        layerCanvas.setNavControlShown(configuration.getPropertyBool(PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, true));

//        setImageProperties(propertyMap); TODO implement

        final FigureLayer figureLayer = getFigureLayer();
        if (figureLayer != null) {
            figureLayer.setStyleProperties(configuration);
        }
//        getNoDataLayer().setStyle(noDataStyle );
        final GraticuleLayer graticuleLayer = getGraticuleLayer();
        if (graticuleLayer != null) {
            graticuleLayer.setStyleProperties(configuration);
        }
//        getPinLayer().setStyleProperties(propertyMap);
//        getGcpLayer().setStyleProperties(propertyMap);

        fireImageUpdated();
    }

    public void addPixelPositionListener(PixelPositionListener listener) {
        layerCanvas.addPixelPositionListener(listener);
    }

    public void removePixelPositionListener(PixelPositionListener listener) {
        layerCanvas.removePixelPositionListener(listener);
    }

    /**
     * Gets tools which can handle selections.
     *
     * @return
     */
    public AbstractTool[] getSelectToolDelegates() {
        // is used for the selection tool, which can be specified for each layer
        // has been introduced for IAVISA (IFOV selection)  (nf, 2008)
        return new AbstractTool[0];
    }

    public void disposeLayers() {
        getSceneImage().getRootLayer().dispose();
    }

    public JComponent getImageDisplayComponent() {
        return layerCanvas;
    }

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

    /**
     * @return the visible image area in pixel coordinates
     */
    public Rectangle getVisibleImageBounds() {
        final ImageLayer imageLayer = getBaseImageLayer();

        if (imageLayer != null) {
            final RenderedImage image = imageLayer.getImage();
            final Area imageArea = new Area(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            final Area visibleImageArea = new Area(imageLayer.getModelToImageTransform().createTransformedShape(getVisibleModelBounds()));
            imageArea.intersect(visibleImageArea);
            return imageArea.getBounds();
        }

        return null;
    }

    /**
     * @return the visible area in model coordinates
     */
    public Rectangle2D getVisibleModelBounds() {
        final Viewport viewport = layerCanvas.getViewport();
        return viewport.getViewToModelTransform().createTransformedShape(viewport.getBounds()).getBounds2D();
    }

    /**
     * @return the model bounds in model coordinates
     */
    public Rectangle2D getModelBounds() {
        return layerCanvas.getLayer().getBounds();
    }

    public double getOrientation() {
        return layerCanvas.getViewport().getOrientation();
    }

    public double getZoomFactor() {
        return layerCanvas.getViewport().getZoomFactor();
    }

    public void zoom(Rectangle2D modelRect) {
        layerCanvas.getViewport().zoom(modelRect);
    }

    public void zoom(double x, double y, double viewScale) {
        layerCanvas.getViewport().zoom(x, y, viewScale);
    }

    public void zoom(double viewScale) {
        layerCanvas.getViewport().zoom(viewScale);
    }

    public void zoomAll() {
        zoom(layerCanvas.getLayer().getBounds());
    }

    public void move(double modelOffsetX, double modelOffsetY) {
        layerCanvas.getViewport().move(modelOffsetX, modelOffsetY);
    }

    public void synchronizeViewport(ProductSceneView view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {

            Rectangle2D visibleModelBounds = getVisibleModelBounds();
            view.move(visibleModelBounds.getX(), visibleModelBounds.getY());
            view.zoom(getZoomFactor());
            view.layerCanvas.getViewport().rotate(getOrientation());
        }
    }

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

        getSceneImage().getRootLayer().render(imageRendering);
        return bi;
    }

    public Tool getTool() {
        return layerCanvas.getTool();
    }

    public void setTool(Tool tool) {
        if (tool != null && layerCanvas.getTool() != tool) {
            tool.setDrawingEditor(this);
            setCursor(tool.getCursor());
            layerCanvas.setTool(tool);
        }
    }

    public void repaintTool() {
        if (layerCanvas.getTool() != null) {
            repaint(100);
        }
    }

    // TODO remove ??? UNUSED
    public void removeFigure(Figure figure) {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            figureLayer.getFigureList().remove(figure);
            if (isShapeOverlayEnabled()) {
                fireImageUpdated();
            }
        }
    }

    // used only internaly --> private ???
    public int getNumFigures() {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            return figureLayer.getFigureList().size();
        }

        return 0;
    }

    // used only internaly --> private ???
    public Figure getFigureAt(int index) {
        return getFigureLayer().getFigureList().get(index);
    }

    // TODO remove ??? UNUSED
    public Figure[] getAllFigures() {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            return figureLayer.getFigureList().toArray(new Figure[getNumFigures()]);
        }

        return new Figure[0];
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
    
    protected void copyPixelInfoStringToClipboard() {
        String text = layerCanvas.createPixelInfoString(this);
        SystemUtils.copyToClipboard(text);
    }

    protected void disposeImageDisplayComponent() {
        layerCanvas.dispose();
    }

    // todo - move to somewhere in navigation tool view package
    public void renderThumbnail(BufferedImage thumbnailImage) {
        final BufferedImageRendering imageRendering = new BufferedImageRendering(thumbnailImage);

        final Graphics2D graphics = imageRendering.getGraphics();
        graphics.setBackground(getBackground());
        graphics.clearRect(0, 0, thumbnailImage.getWidth(), thumbnailImage.getHeight());

        configureThumbnailViewport(imageRendering.getViewport());
        getSceneImage().getRootLayer().render(imageRendering);
    }



    // todo - move to somewhere in navigation tool view package
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

    // todo - move to somewhere in navigation tool view package
    private void configureThumbnailViewport(Viewport thumbnailViewport) {
        thumbnailViewport.setMaxZoomFactor(-1);
        thumbnailViewport.zoom(getRotatedModelBounds());
        thumbnailViewport.moveViewDelta(thumbnailViewport.getBounds().x, thumbnailViewport.getBounds().y);
        thumbnailViewport.rotate(getOrientation());
    }

    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
        final String expression = getRaster().getValidMaskExpression();
        final ImageLayer noDataLayer = getNoDataLayer();

        if (noDataLayer != null) {
            if (expression != null) {
                // todo - get color from style, set color
                final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), Color.ORANGE, expression,
                        true, new AffineTransform());
                noDataLayer.setMultiLevelSource(multiLevelSource);
            } else {
                noDataLayer.setMultiLevelSource(MultiLevelSource.NULL);
            }

            fireImageUpdated();
        }
    }

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
    
    private ImageLayer getNoDataLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("No-data")) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }

    private FigureLayer getFigureLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if ("Figures".equals(layer.getName())) {
                return (FigureLayer) layer;
            }
        }

        return null;
    }

    private ImageLayer getRoiLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("ROI")) {
                return (ImageLayer) layer;
            }
        }

        return null;
    }

    private GraticuleLayer getGraticuleLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if (layer.getName().startsWith("Graticule")) {
                return (GraticuleLayer) layer;
            }
        }

        return null;
    }

    private Layer getPinLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if ("Pins".equals(layer.getName())) {
                return layer;
            }
        }

        return null;
    }

    private Layer getGcpLayer() {
        for (final Layer layer : getSceneImage().getRootLayer().getChildLayerList()) {
            if ("GCPs".equals(layer.getName())) {
                return layer;
            }
        }

        return null;
    }
}
