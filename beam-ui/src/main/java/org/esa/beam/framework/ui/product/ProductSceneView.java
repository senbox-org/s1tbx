package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorHolder;
import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.MaskCollectionLayer;
import org.esa.beam.glayer.NoDataLayerType;
import org.esa.beam.glevel.MaskImageMultiLevelSource;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Vector;

/**
 * The class <code>ProductSceneView</code> is a high-level image display component for color index/RGB images created
 * from one or more raster datasets of a data product.
 * <p/>
 * <p>It is also capable of displaying a graticule (geographical grid) and a ROI associated with a displayed raster
 * dataset.
 *
 * @author Norman Fomferra
 * @version $ Revision: $ $ Date: $
 */
public class ProductSceneView extends BasicView
        implements FigureEditorHolder, ProductNodeView, PropertyMapChangeListener, PixelInfoFactory, LayerContext {

    public static final String BASE_IMAGE_LAYER_ID = "org.esa.beam.layers.baseImage";
    public static final String NO_DATA_LAYER_ID = "org.esa.beam.layers.noData";
    public static final String BITMASK_LAYER_ID = "org.esa.beam.layers.bitmask";
    public static final String MASKS_LAYER_ID = MaskCollectionLayer.ID;
    public static final String ROI_LAYER_ID = "org.esa.beam.layers.roi";
    public static final String GRATICULE_LAYER_ID = "org.esa.beam.layers.graticule";
    public static final String GCP_LAYER_ID = "org.esa.beam.layers.gcp";
    public static final String PIN_LAYER_ID = "org.esa.beam.layers.pin";
    public static final String FIGURE_LAYER_ID = "org.esa.beam.layers.figure";

    /**
     * Property name for the pixel border
     */
    public static final String PROPERTY_KEY_PIXEL_BORDER_SHOWN = "pixel.border.shown";
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_GRAPHICS_ANTIALIASING = "graphics.antialiasing";
    /**
     * Property name for antialiased graphics drawing
     */
    public static final String PROPERTY_KEY_IMAGE_INTERPOLATION = "image.interpolation";
    /**
     * Name of property which switches display of af a navigataion control in the image view.
     */
    public static final String PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN = "image.navControlShown";
    /**
     * Name of property which switches display of af a navigataion control in the image view.
     */
    public static final String PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN = "image.scrollBarsShown";

    /**
     * Name of property of image info
     */
    public static final String PROPERTY_NAME_IMAGE_INFO = "imageInfo";

    /**
     * Name of property of image info
     */
    public static final String PROPERTY_NAME_SELECTED_LAYER = "selectedLayer";

    public static final Color DEFAULT_IMAGE_BACKGROUND_COLOR = new Color(51, 51, 51);
    public static final int DEFAULT_IMAGE_VIEW_BORDER_SIZE = 64;


    private ProductSceneImage sceneImage;
    private FigureEditorLayerCanvas layerCanvas;

    // todo - (re)move following variables, they don't belong to here (nf - 28.10.2008)
    // {{
    private final ImageLayer baseImageLayer;
    private int pixelX = -1;
    private int pixelY = -1;
    private int levelPixelX = -1;
    private int levelPixelY = -1;
    private int level = 0;
    private boolean pixelBorderShown; // can it be shown?
    private boolean pixelBorderDrawn; // has it been drawn?
    private double pixelBorderViewScale;
    private final Vector<PixelPositionListener> pixelPositionListeners;

    private Layer selectedLayer;

    private ComponentAdapter layerCanvasComponentHandler;
    private LayerCanvasMouseHandler layerCanvasMouseHandler;
    private RasterChangeHandler rasterChangeHandler;
    private boolean scrollBarsShown;
    private AdjustableViewScrollPane scrollPane;

    private UndoContext undoContext;

    public ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");

        setOpaque(true);
        setLayout(new BorderLayout());
        // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);

        this.pixelBorderShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_PIXEL_BORDER_SHOWN, true);

        this.sceneImage = sceneImage;
        this.baseImageLayer = sceneImage.getBaseImageLayer();
        this.pixelBorderViewScale = 2.0;
        this.pixelPositionListeners = new Vector<PixelPositionListener>();

        // todo - use global application undo context
        undoContext = new DefaultUndoContext(this);

        this.layerCanvas = new FigureEditorLayerCanvas(sceneImage.getRootLayer(),
                                                       new DefaultViewport(isModelYAxisDown(baseImageLayer)),
                                                       undoContext);

        final boolean navControlShown = sceneImage.getConfiguration().getPropertyBool(
                PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, true);
        this.layerCanvas.setNavControlShown(navControlShown);
        this.layerCanvas.setPreferredSize(new Dimension(400, 400));

        this.scrollBarsShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN,
                                                                             false);
        if (scrollBarsShown) {
            this.scrollPane = createScrollPane();
            add(scrollPane, BorderLayout.CENTER);
        } else {
            add(layerCanvas, BorderLayout.CENTER);
        }

        registerLayerCanvasListeners();

        this.rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        setMaskOverlayEnabled(true);
    }

    @Override
    public FigureEditor getFigureEditor() {
        return layerCanvas.getFigureEditor();
    }

    private AdjustableViewScrollPane createScrollPane() {
        AbstractButton zoomAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomAll13.gif"),
                                                                      false);
        zoomAllButton.setFocusable(false);
        zoomAllButton.setFocusPainted(false);
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLayerCanvas().zoomAll();
            }
        });

        AdjustableViewScrollPane scrollPane = new AdjustableViewScrollPane(layerCanvas);
        // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        scrollPane.setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);
        scrollPane.setCornerComponent(zoomAllButton);
        return scrollPane;
    }

    ProductSceneImage getSceneImage() {
        return sceneImage;
    }

    /**
     * @return The root layer.
     */
    @Override
    public Layer getRootLayer() {
        return sceneImage.getRootLayer();
    }

    /**
     * The coordinate reference system (CRS) used by all the layers in this context.
     * May be used by a {@link com.bc.ceres.glayer.LayerType} in order to decide whether
     * the source can provide a new layer instance for this context.
     *
     * @return The CRS. May be {@code null}.
     */
    @Override
    public Object getCoordinateReferenceSystem() {
        return sceneImage.getCoordinateReferenceSystem();
    }

    @Deprecated
    public LayerContext getLayerContext() {
        return sceneImage;
    }

    public LayerCanvas getLayerCanvas() {
        return layerCanvas;
    }

    /**
     * Returns the currently visible product node.
     */
    @Override
    public ProductNode getVisibleProductNode() {
        if (isRGB()) {
            return null;
        }
        return getRaster();
    }

    /**
     * Creates a string containing all available information at the given pixel position. The string returned is a line
     * separated text with each line containing a key/value pair.
     *
     * @param pixelX the pixel X co-ordinate
     * @param pixelY the pixel Y co-ordinate
     *
     * @return the info string at the given position
     */
    @Override
    public String createPixelInfoString(int pixelX, int pixelY) {
        return getProduct() != null ? getProduct().createPixelInfoString(pixelX, pixelY) : "";
    }

    /**
     * Called if the property map changed. Simply calls {@link #setLayerProperties(org.esa.beam.util.PropertyMap)}.
     */
    @Override
    public void propertyMapChanged(PropertyMap propertyMap) {
        setLayerProperties(propertyMap);
    }

    /**
     * If the <code>preferredSize</code> has been set to a
     * non-<code>null</code> value just returns it.
     * If the UI delegate's <code>getPreferredSize</code>
     * method returns a non <code>null</code> value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the <code>preferredSize</code> property
     *
     * @see #setPreferredSize
     * @see javax.swing.plaf.ComponentUI
     */
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        } else {
            if (getLayerCanvas() != null) {
                return getLayerCanvas().getPreferredSize();
            } else {
                return super.getPreferredSize();
            }
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
    public synchronized void dispose() {
        if (pixelPositionListeners != null) {
            pixelPositionListeners.clear();
        }

        deregisterLayerCanvasListeners();

        for (int i = 0; i < getSceneImage().getRasters().length; i++) {
            final RasterDataNode raster = getSceneImage().getRasters()[i];
            if (raster instanceof RGBChannel) {
                RGBChannel rgbChannel = (RGBChannel) raster;
                rgbChannel.dispose();
            }
            getSceneImage().getRasters()[i] = null;
        }
        sceneImage = null;

        if (getLayerCanvas() != null) {
            // ensure that imageDisplay.dispose() is run in the EDT
            SwingUtilities.invokeLater(new Runnable() {
                @Override
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
        final ImageInfo oldImageInfo = getImageInfo();
        getSceneImage().setImageInfo(imageInfo);
        firePropertyChange(PROPERTY_NAME_IMAGE_INFO, oldImageInfo, imageInfo);
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
     *
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

    public boolean isNoDataOverlayEnabled() {
        final Layer noDataLayer = getNoDataLayer(false);
        return noDataLayer != null && noDataLayer.isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        if (isNoDataOverlayEnabled() != enabled) {
            getNoDataLayer(true).setVisible(enabled);
        }
    }

    public ImageLayer getBaseImageLayer() {
        return getSceneImage().getBaseImageLayer();
    }

    public boolean isGraticuleOverlayEnabled() {
        final GraticuleLayer graticuleLayer = getGraticuleLayer(false);
        return graticuleLayer != null && graticuleLayer.isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        if (isGraticuleOverlayEnabled() != enabled) {
            getGraticuleLayer(true).setVisible(enabled);
        }
    }

    public boolean isPinOverlayEnabled() {
        final Layer pinLayer = getPinLayer(false);
        return pinLayer != null && pinLayer.isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        if (isPinOverlayEnabled() != enabled) {
            getPinLayer(true).setVisible(enabled);
        }
    }

    public boolean isGcpOverlayEnabled() {
        final Layer gcpLayer = getGcpLayer(false);
        return gcpLayer != null && gcpLayer.isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        if (isGcpOverlayEnabled() != enabled) {
            getGcpLayer(true).setVisible(enabled);
        }
    }

    @Deprecated
    public boolean isBitmaskOverlayEnabled() {
        final Layer bitmaskLayer = getBitmaskLayer(false);
        return bitmaskLayer != null && bitmaskLayer.isVisible();
    }

    @Deprecated
    public void setBitmaskOverlayEnabled(boolean enabled) {
        if (isBitmaskOverlayEnabled() != enabled) {
            getBitmaskLayer(true).setVisible(enabled);
        }
    }

    public boolean isMaskOverlayEnabled() {
        final Layer layer = getMaskCollectionLayer(false);
        return layer != null && layer.isVisible();
    }

    public void setMaskOverlayEnabled(boolean enabled) {
        if (isMaskOverlayEnabled() != enabled) {
            getMaskCollectionLayer(true).setVisible(enabled);
        }
    }

    public boolean isShapeOverlayEnabled() {
        final FigureLayer figureLayer = getFigureLayer(false);
        return figureLayer != null && figureLayer.isVisible();
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        if (isShapeOverlayEnabled() != enabled) {
            getFigureLayer(true).setVisible(enabled);
        }
    }

    @Deprecated
    public Figure getCurrentShapeFigure() {
        return null;
    }

    @Deprecated
    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        /*
        setShapeOverlayEnabled(true);
        final Figure oldShapeFigure = getCurrentShapeFigure();
        if (currentShapeFigure != oldShapeFigure) {
            if (oldShapeFigure != null && getFigureLayer(false) != null) {
                getFigureLayer(false).removeFigure(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                getFigureLayer(true).addFigure(currentShapeFigure);
            }
        }
        */
    }

    public boolean areScrollBarsShown() {
        return scrollBarsShown;
    }

    public void setScrollBarsShown(boolean scrollBarsShown) {
        if (scrollBarsShown != this.scrollBarsShown) {
            this.scrollBarsShown = scrollBarsShown;
            if (scrollBarsShown) {
                remove(layerCanvas);
                scrollPane = createScrollPane();
                add(scrollPane, BorderLayout.CENTER);
            } else {
                remove(scrollPane);
                scrollPane = null;
                add(layerCanvas, BorderLayout.CENTER);
            }
            invalidate();
            validate();
            repaint();
        }
    }

    /**
     * Called after VISAT preferences have changed.
     * This behaviour is deprecated since we want to uswe separate style editors for each layers.
     *
     * @param configuration the configuration.
     */
    public void setLayerProperties(PropertyMap configuration) {
        setScrollBarsShown(configuration.getPropertyBool(PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN, false));
        layerCanvas.setNavControlShown(configuration.getPropertyBool(PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, true));
        layerCanvas.setBackground(
                configuration.getPropertyColor("image.background.color", DEFAULT_IMAGE_BACKGROUND_COLOR));

        final ImageLayer imageLayer = getBaseImageLayer();
        if (imageLayer != null) {
            ProductSceneImage.setBaseImageLayerStyle(configuration, imageLayer);
        }
        final Layer pinLayer = getPinLayer(false);
        if (pinLayer != null) {
            ProductSceneImage.setPinLayerStyle(configuration, pinLayer);
        }
        final Layer gcpLayer = getGcpLayer(false);
        if (gcpLayer != null) {
            ProductSceneImage.setGcpLayerStyle(configuration, gcpLayer);
        }
        final FigureLayer figureLayer = getFigureLayer(false);
        if (figureLayer != null) {
            ProductSceneImage.setFigureLayerStyle(configuration, figureLayer);
        }
        final GraticuleLayer graticuleLayer = getGraticuleLayer(false);
        if (graticuleLayer != null) {
            ProductSceneImage.setGraticuleLayerStyle(configuration, graticuleLayer);
        }
    }

    /**
     * Adds a new pixel position listener to this image display component. If
     * the component already contains the given listener, the method does
     * nothing.
     *
     * @param listener the pixel position listener to be added
     */
    public final void addPixelPositionListener(PixelPositionListener listener) {
        if (listener == null) {
            return;
        }
        if (pixelPositionListeners.contains(listener)) {
            return;
        }
        pixelPositionListeners.add(listener);
    }

    /**
     * Removes a pixel position listener from this image display component.
     *
     * @param listener the pixel position listener to be removed
     */
    public final void removePixelPositionListener(PixelPositionListener listener) {
        if (listener == null || pixelPositionListeners.isEmpty()) {
            return;
        }
        pixelPositionListeners.remove(listener);
    }

    public Layer getSelectedLayer() {
        return selectedLayer;
    }

    public void setSelectedLayer(Layer layer) {
        Layer oldLayer = selectedLayer;
        if (oldLayer != layer) {
            selectedLayer = layer;
            firePropertyChange(PROPERTY_NAME_SELECTED_LAYER, oldLayer, selectedLayer);
        }
    }

    public void disposeLayers() {
        getSceneImage().getRootLayer().dispose();
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
            final Area visibleImageArea = new Area(
                    imageLayer.getModelToImageTransform().createTransformedShape(getVisibleModelBounds()));
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
        return viewport.getViewToModelTransform().createTransformedShape(viewport.getViewBounds()).getBounds2D();
    }

    /**
     * @return the model bounds in model coordinates
     */
    public Rectangle2D getModelBounds() {
        return layerCanvas.getLayer().getModelBounds();
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
        layerCanvas.getViewport().setZoomFactor(viewScale, x, y);
    }

    public void synchronizeViewport(ProductSceneView view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {

            Viewport viewPortToChange = view.layerCanvas.getViewport();
            Viewport myViewPort = layerCanvas.getViewport();
            viewPortToChange.setTransform(myViewPort);
        }
    }


    protected void copyPixelInfoStringToClipboard() {
        SystemUtils.copyToClipboard(createPixelInfoString(pixelX, pixelY));
    }

    protected void disposeImageDisplayComponent() {
        layerCanvas.dispose();
    }

    @Deprecated
    public void updateImage(ProgressMonitor pm) throws IOException {
        updateImage();
    }

    // only called from VISAT
    public void updateImage() {
        getBaseImageLayer().regenerate();
    }

    @Deprecated
    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
        updateNoDataImage();
    }

    // used by PropertyEditor
    public void updateNoDataImage() {
        // change configuration of layer ; not setting MultiLevelSource
        final String expression = getRaster().getValidMaskExpression();
        final ImageLayer noDataLayer = (ImageLayer) getNoDataLayer(false);
        if (noDataLayer != null) {
            if (expression != null) {
                final Color color = (Color) noDataLayer.getConfiguration().getValue(
                        NoDataLayerType.PROPERTY_NAME_COLOR);
                final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(),
                                                                                           color, expression, true,
                                                                                           getBaseImageLayer().getImageToModelTransform());
                noDataLayer.setMultiLevelSource(multiLevelSource);
            } else {
                noDataLayer.setMultiLevelSource(MultiLevelSource.NULL);
            }
        }
    }

    private void addCopyPixelInfoToClipboardMenuItem(JPopupMenu popupMenu) {
        JMenuItem menuItem = new JMenuItem("Copy Pixel-Info to Clipboard");
        menuItem.setMnemonic('C');
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                copyPixelInfoStringToClipboard();
            }
        });
        popupMenu.add(menuItem);
        popupMenu.addSeparator();
    }

    public int getFirstImageLayerIndex() {
        return sceneImage.getFirstImageLayerIndex();
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
            setModified(false);
        }
    }

    private final class RasterChangeHandler implements ProductNodeListener {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeDataChanged(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            repaintView();
        }

        private void repaintView() {
            repaint(100);
        }
    }

    private Layer getNoDataLayer(boolean create) {
        return getSceneImage().getNoDataLayer(create);
    }

    private FigureLayer getFigureLayer(boolean create) {
        return getSceneImage().getFigureLayer(create);
    }

    @Deprecated
    private Layer getBitmaskLayer(boolean create) {
        return getSceneImage().getBitmaskLayer(create);
    }

    private Layer getMaskCollectionLayer(boolean create) {
        return getSceneImage().getMaskCollectionLayer(create);
    }

    private GraticuleLayer getGraticuleLayer(boolean create) {
        return getSceneImage().getGraticuleLayer(create);
    }

    private Layer getPinLayer(boolean create) {
        return getSceneImage().getPinLayer(create);
    }

    private Layer getGcpLayer(boolean create) {
        return getSceneImage().getGcpLayer(create);
    }

    private static boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
    }

    private void registerLayerCanvasListeners() {
        layerCanvasComponentHandler = new LayerCanvasComponentHandler();
        layerCanvasMouseHandler = new LayerCanvasMouseHandler();

        layerCanvas.addComponentListener(layerCanvasComponentHandler);
        layerCanvas.addMouseListener(layerCanvasMouseHandler);
        layerCanvas.addMouseMotionListener(layerCanvasMouseHandler);
        layerCanvas.addMouseWheelListener(layerCanvasMouseHandler);

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        layerCanvas.addMouseListener(popupMenuHandler);
        layerCanvas.addKeyListener(popupMenuHandler);
    }

    private void deregisterLayerCanvasListeners() {
        getRaster().getProduct().removeProductNodeListener(rasterChangeHandler);
        layerCanvas.removeComponentListener(layerCanvasComponentHandler);
        layerCanvas.removeMouseListener(layerCanvasMouseHandler);
        layerCanvas.removeMouseMotionListener(layerCanvasMouseHandler);
    }

    public boolean isPixelPosValid() {
        return isPixelPosValid(levelPixelX, levelPixelY, level);
    }

    private boolean isPixelPosValid(int currentPixelX, int currentPixelY, int currentLevel) {
        return currentPixelX >= 0 && currentPixelX < baseImageLayer.getImage(
                currentLevel).getWidth() && currentPixelY >= 0
                && currentPixelY < baseImageLayer.getImage(currentLevel).getHeight();
    }

    private void firePixelPosChanged(MouseEvent e, int currentPixelX, int currentPixelY, int currentLevel) {
        boolean pixelPosValid = isPixelPosValid(currentPixelX, currentPixelY, currentLevel);
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosChanged(baseImageLayer, currentPixelX, currentPixelY, currentLevel, pixelPosValid, e);
        }
    }

    private void firePixelPosNotAvailable() {
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosNotAvailable();
        }
    }

    private void setPixelPos(MouseEvent e, boolean showBorder) {
        Point2D p = new Point2D.Double(e.getX() + 0.5, e.getY() + 0.5);

        Viewport viewport = getLayerCanvas().getViewport();
        int currentLevel = baseImageLayer.getLevel(viewport);
        AffineTransform v2mTransform = viewport.getViewToModelTransform();
        final Point2D modelP = v2mTransform.transform(p, null);

        AffineTransform m2iTransform = baseImageLayer.getModelToImageTransform();
        Point2D imageP = m2iTransform.transform(modelP, null);
        pixelX = (int) Math.floor(imageP.getX());
        pixelY = (int) Math.floor(imageP.getY());

        AffineTransform m2iLevelTransform = baseImageLayer.getModelToImageTransform(currentLevel);
        Point2D imageLevelP = m2iLevelTransform.transform(modelP, null);
        int currentPixelX = (int) Math.floor(imageLevelP.getX());
        int currentPixelY = (int) Math.floor(imageLevelP.getY());
        if (currentPixelX != levelPixelX || currentPixelY != levelPixelY || currentLevel != level) {
            if (isPixelBorderDisplayEnabled() && (showBorder || pixelBorderDrawn)) {
                drawPixelBorder(currentPixelX, currentPixelY, currentLevel, showBorder);
            }
            levelPixelX = currentPixelX;
            levelPixelY = currentPixelY;
            level = currentLevel;
            if (e.getID() != MouseEvent.MOUSE_EXITED) {
                firePixelPosChanged(e, levelPixelX, levelPixelY, level);
            } else {
                firePixelPosNotAvailable();
            }
        }
    }

    private boolean isPixelBorderDisplayEnabled() {
        return pixelBorderShown &&
                getLayerCanvas().getViewport().getZoomFactor() >= pixelBorderViewScale;
    }

    private void drawPixelBorder(int currentPixelX, int currentPixelY, int currentLevel, boolean showBorder) {
        final Graphics g = getGraphics();
        g.setXORMode(Color.white);
        if (pixelBorderDrawn) {
            drawPixelBorder(g, levelPixelX, levelPixelY, level);
            pixelBorderDrawn = false;
        }
        if (showBorder) {
            drawPixelBorder(g, currentPixelX, currentPixelY, currentLevel);
            pixelBorderDrawn = true;
        }
        g.setPaintMode();
        g.dispose();
    }

    private void drawPixelBorder(final Graphics g, final int x, final int y, final int l) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform i2m = getBaseImageLayer().getImageToModelTransform(l);
            AffineTransform m2v = getLayerCanvas().getViewport().getModelToViewTransform();
            Rectangle imageRect = new Rectangle(x, y, 1, 1);
            Shape modelRect = i2m.createTransformedShape(imageRect);
            Shape transformedShape = m2v.createTransformedShape(modelRect);
            g2d.draw(transformedShape);
        }
    }

    private final class LayerCanvasMouseHandler implements MouseInputListener, MouseWheelListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updatePixelPos(e, true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updatePixelPos(e, true);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            Viewport viewport = layerCanvas.getViewport();
            int wheelRotation = e.getWheelRotation();
            double oldZoomFactor = viewport.getZoomFactor();
            double newZoomFactor = oldZoomFactor * Math.pow(1.1, wheelRotation);
            viewport.setZoomFactor(newZoomFactor);
        }

        private void updatePixelPos(MouseEvent e, boolean showBorder) {
            setPixelPos(e, showBorder);
        }
    }

    private class LayerCanvasComponentHandler extends ComponentAdapter {

        /**
         * Invoked when the component has been made invisible.
         */
        @Override
        public void componentHidden(ComponentEvent e) {
            firePixelPosNotAvailable();
        }
    }

    private static class FigureEditorLayerCanvas extends LayerCanvas implements FigureEditorHolder {

        private final FigureCollection figureCollection;
        private final DefaultFigureEditor figureEditor;

        private FigureEditorLayerCanvas(Layer rootLayer, Viewport viewport, UndoContext undoContext) {
            super(rootLayer, viewport);
            // todo - use FigureCollection from FigureLayer(s)!
            figureCollection = new DefaultFigureCollection();

            figureEditor = new DefaultFigureEditor(this, undoContext, figureCollection);

            addOverlay(new LayerCanvas.Overlay() {
                @Override
                public void paintOverlay(LayerCanvas canvas, Graphics2D graphics) {
                    // todo - figureEditor.drawFigures(graphics, true);
                    figureEditor.drawFigures(graphics, false);
                    figureEditor.drawSelectionRectangle(graphics);
                }
            });
        }

        @Override
        public FigureEditor getFigureEditor() {
            return figureEditor;
        }

    }
}
