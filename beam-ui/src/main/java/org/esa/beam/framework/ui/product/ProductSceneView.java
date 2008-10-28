package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
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
import org.esa.beam.util.MouseEventFilterFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;
import org.esa.beam.util.SystemUtils;

import javax.swing.JComponent;
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
 * @version $revision$ $date$
 */
public class ProductSceneView extends BasicView implements ProductNodeView, DrawingEditor, PropertyMapChangeListener, PixelInfoFactory {
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
     * Property name for the image histogram matching type
     *
     * @deprecated
     */
    @Deprecated
    public static final String PROPERTY_KEY_HISTOGRAM_MATCHING = "graphics.histogramMatching";

    @Deprecated
    public static String IMAGE_INTERPOLATION_NEAREST_NEIGHBOUR = "Nearest Neighbour";
    @Deprecated
    public static String IMAGE_INTERPOLATION_BILINEAR = "Bi-Linear Interpolation";
    @Deprecated
    public static String IMAGE_INTERPOLATION_BICUBIC = "Bi-Cubic Interpolation";
    @Deprecated
    public static String IMAGE_INTERPOLATION_SYSTEM_DEFAULT = "System Default";
    @Deprecated
    public static String DEFAULT_IMAGE_INTERPOLATION_METHOD = IMAGE_INTERPOLATION_SYSTEM_DEFAULT;

    //    public static final Color DEFAULT_IMAGE_BORDER_COLOR = new Color(204, 204, 255);
    public static final Color DEFAULT_IMAGE_BACKGROUND_COLOR = new Color(51, 51, 51);
    //    public static final double DEFAULT_IMAGE_BORDER_SIZE = 2.0;
    public static final int DEFAULT_IMAGE_VIEW_BORDER_SIZE = 64;


    private ProductSceneImage sceneImage;
    private LayerCanvas layerCanvas;

    // todo - (re)move following variable after BEAM 4.5 (nf - 28.10.2008)
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
    // }}

    private Tool tool;

    private ComponentAdapter layerCanvasComponentHandler;
    private MouseInputListener layerCanvasMouseInputHandler;
    private KeyListener layerCanvasKeyHandler;
    private RasterChangeHandler rasterChangeHandler;
    private boolean scrollBarsShown;
    private AdjustableViewScrollPane scrollPane;

    public ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");

        setOpaque(true);
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR); // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        setLayout(new BorderLayout());

        this.pixelBorderShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_PIXEL_BORDER_SHOWN, true);

        this.sceneImage = sceneImage;
        this.baseImageLayer = sceneImage.getBaseImageLayer();
        this.pixelBorderViewScale = 2.0;
        this.pixelPositionListeners = new Vector<PixelPositionListener>();

        this.layerCanvas = new LayerCanvas(sceneImage.getRootLayer(), new DefaultViewport(isModelYAxisDown(baseImageLayer)));

        final boolean navControlShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_IMAGE_NAV_CONTROL_SHOWN, true);
        this.layerCanvas.setNavControlShown(navControlShown);
        this.layerCanvas.setPreferredSize(new Dimension(400, 400));

        this.scrollBarsShown = sceneImage.getConfiguration().getPropertyBool(PROPERTY_KEY_IMAGE_SCROLL_BARS_SHOWN, false);
        if (scrollBarsShown) {
            scrollPane = new AdjustableViewScrollPane(layerCanvas);
            add(scrollPane, BorderLayout.CENTER);
        }else {
            add(layerCanvas, BorderLayout.CENTER);
        }

        registerLayerCanvasListeners();

        this.rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);
    }

    ProductSceneImage getSceneImage() {
        return sceneImage;
    }

    public Layer getRootLayer() {
        return sceneImage.getRootLayer();
    }

    public LayerCanvas getLayerCanvas() {
        return layerCanvas;
    }

    /**
     * Returns the currently visible product node.
     */
    @Override
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

        if (getImageDisplayComponent() != null) {
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
    @Override
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

    public boolean isNoDataOverlayEnabled() {
        final ImageLayer noDataLayer = getNoDataLayer();
        return noDataLayer != null && noDataLayer.isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        if (isNoDataOverlayEnabled() != enabled) {
            getNoDataLayer().setVisible(enabled);
        }
    }

    public ImageLayer getBaseImageLayer() {
        return getSceneImage().getBaseImageLayer();
    }

    public boolean isGraticuleOverlayEnabled() {
        final GraticuleLayer graticuleLayer = getGraticuleLayer();
        return graticuleLayer != null && graticuleLayer.isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        if (isGraticuleOverlayEnabled() != enabled) {
            getGraticuleLayer().setVisible(enabled);
        }
    }

    public boolean isPinOverlayEnabled() {
        final Layer pinLayer = getPinLayer();
        return pinLayer != null && pinLayer.isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        if (isPinOverlayEnabled() != enabled) {
            getPinLayer().setVisible(enabled);
        }
    }

    public boolean isGcpOverlayEnabled() {
        final Layer gcpLayer = getGcpLayer();
        return gcpLayer != null && gcpLayer.isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        if (isGcpOverlayEnabled() != enabled) {
            getGcpLayer().setVisible(enabled);
        }
    }

    public boolean isShapeOverlayEnabled() {
        final FigureLayer figureLayer = getFigureLayer();
        return figureLayer != null && figureLayer.isVisible();
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        if (isShapeOverlayEnabled() != enabled) {
            getFigureLayer().setVisible(enabled);
        }
    }

    public boolean isROIOverlayEnabled() {
        final ImageLayer roiLayer = getRoiLayer();
        return roiLayer != null && roiLayer.isVisible();
    }

    public void setROIOverlayEnabled(boolean enabled) {
        if (isROIOverlayEnabled() != enabled) {
            getRoiLayer().setVisible(enabled);
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
                getFigureLayer().removeFigure(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                getFigureLayer().addFigure(currentShapeFigure);
            }
        }
    }

    public boolean areScrollBarsShown() {
        return scrollBarsShown;
    }

    public void setScrollBarsShown(boolean scrollBarsShown) {
        if (scrollBarsShown != this.scrollBarsShown) {
            this.scrollBarsShown = scrollBarsShown;
            if (scrollBarsShown) {
                remove(layerCanvas);
                scrollPane = new AdjustableViewScrollPane(layerCanvas);
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

        final ImageLayer imageLayer = getBaseImageLayer();
        if (imageLayer != null) {
            ProductSceneImage.setBaseImageLayerStyle(configuration, imageLayer);
        }

        final Layer noDataLayer = getNoDataLayer();
        if (noDataLayer != null) {
            ProductSceneImage.setNoDataLayerStyle(configuration, noDataLayer);
        }
        final Layer roiLayer = getRoiLayer();
        if (roiLayer != null) {
            ProductSceneImage.setRoiLayerStyle(configuration, roiLayer);
        }
        final Layer pinLayer = getPinLayer();
        if (pinLayer != null) {
            ProductSceneImage.setPinLayerStyle(configuration, pinLayer);
        }
        final Layer gcpLayer = getGcpLayer();
        if (gcpLayer != null) {
            ProductSceneImage.setGcpLayerStyle(configuration, gcpLayer);
        }
        final FigureLayer figureLayer = getFigureLayer();
        if (figureLayer != null) {
            ProductSceneImage.setFigureLayerStyle(configuration, figureLayer);
        }
        final GraticuleLayer graticuleLayer = getGraticuleLayer();
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

    /**
     * Gets tools which can handle selections.
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

    public void zoom(double viewScale) {
        layerCanvas.getViewport().setZoomFactor(viewScale);
    }

    public void zoomAll() {
        zoom(layerCanvas.getLayer().getModelBounds());
    }

    public void move(double modelOffsetX, double modelOffsetY) {
        layerCanvas.getViewport().move(modelOffsetX, modelOffsetY);
    }

    public void synchronizeViewport(ProductSceneView view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {

            Viewport viewPortToChange = view.layerCanvas.getViewport();
            Viewport myViewPort = layerCanvas.getViewport();
            viewPortToChange.synchronizeWith(myViewPort);
        }
    }


    @Override
    public Tool getTool() {
        return tool;
    }

    @Override
    public void setTool(Tool tool) {
        if (this.tool != tool) {
            if (tool != null) {
                tool.setDrawingEditor(this);
                setCursor(tool.getCursor());
            }
            this.tool = tool;
        }
    }

    @Override
    public void repaintTool() {
        if (getTool() != null) {
            repaint(100);
        }
    }

    // TODO remove ??? UNUSED
    @Override
    public void removeFigure(Figure figure) {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            figureLayer.removeFigure(figure);
        }
    }

    // used only internaly --> private ???
    @Override
    public int getNumFigures() {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            return figureLayer.getFigureList().size();
        }

        return 0;
    }

    // used only internaly --> private ???
    @Override
    public Figure getFigureAt(int index) {
        return getFigureLayer().getFigureList().get(index);
    }

    // TODO remove ??? UNUSED
    @Override
    public Figure[] getAllFigures() {
        final FigureLayer figureLayer = getFigureLayer();

        if (figureLayer != null) {
            return figureLayer.getFigureList().toArray(new Figure[getNumFigures()]);
        }

        return new Figure[0];
    }

    //TODO remove ??? UNUSED
    @Override
    public Figure[] getSelectedFigures() {
        return new Figure[0];
    }

    // TODO remove ??? UNUSED
    @Override
    public Figure[] getFiguresWithAttribute(String name) {
        return new Figure[0];
    }

    // TODO remove ??? UNUSED
    @Override
    public Figure[] getFiguresWithAttribute(String name, Object value) {
        return new Figure[0];
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
    public void updateImage(){
        getBaseImageLayer().regenerate();
    }

    @Deprecated
    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
         updateNoDataImage();
    }

    // used by PropertyEditor
    public void updateNoDataImage()  {
        final String expression = getRaster().getValidMaskExpression();
        final ImageLayer noDataLayer = getNoDataLayer();

        if (noDataLayer != null) {
            if (expression != null) {
                final Style style = noDataLayer.getStyle();
                final Color color = (Color) style.getProperty("color");
                final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(),
                                                                                           color, expression, true, noDataLayer.getImageToModelTransform());
                noDataLayer.setMultiLevelSource(multiLevelSource);
            } else {
                noDataLayer.setMultiLevelSource(MultiLevelSource.NULL);
            }
        }
    }

    @Deprecated
    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        updateROIImage();
    }

    // used by PropertyEditor
    public void updateROIImage() {
        final ImageLayer roiLayer = getRoiLayer();
        if (roiLayer != null) {
            if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
                final Color color = (Color) roiLayer.getStyle().getProperty("color");
                final MultiLevelSource multiLevelSource = RoiImageMultiLevelSource.create(getRaster(),
                                                                                          color, roiLayer.getImageToModelTransform());
                roiLayer.setMultiLevelSource(multiLevelSource);
            } else {
                roiLayer.setMultiLevelSource(MultiLevelSource.NULL);
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

    private ImageLayer getNoDataLayer() {
        return getSceneImage().getNoDataLayer();
    }

    private FigureLayer getFigureLayer() {
        return getSceneImage().getFigureLayer();
    }

    private ImageLayer getRoiLayer() {
        return getSceneImage().getRoiLayer();
    }

    private GraticuleLayer getGraticuleLayer() {
        return getSceneImage().getGraticuleLayer();
    }

    private Layer getPinLayer() {
        return getSceneImage().getPinLayer();
    }

    private Layer getGcpLayer() {
        return getSceneImage().getGcpLayer();
    }

    private static boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
    }


    private void registerLayerCanvasListeners() {
        layerCanvasComponentHandler = new LayerCanvasComponentHandler();
        layerCanvasMouseInputHandler = MouseEventFilterFactory.createFilter(new PixelPosUpdater());
        layerCanvasKeyHandler = new LayerCanvasKeyHandler();

        layerCanvas.addComponentListener(layerCanvasComponentHandler);
        layerCanvas.addMouseListener(layerCanvasMouseInputHandler);
        layerCanvas.addMouseMotionListener(layerCanvasMouseInputHandler);
        layerCanvas.addKeyListener(layerCanvasKeyHandler);

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        this.layerCanvas.addMouseListener(popupMenuHandler);
        this.layerCanvas.addKeyListener(popupMenuHandler);
        this.layerCanvas.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                final Viewport viewport = layerCanvas.getViewport();
                final int wheelRotation = e.getWheelRotation();
                final double newZoomFactor = viewport.getZoomFactor() * Math.pow(1.1, wheelRotation);
                viewport.setZoomFactor(newZoomFactor);
            }
        });
    }

    private void deregisterLayerCanvasListeners() {
        getRaster().getProduct().removeProductNodeListener(rasterChangeHandler);
        layerCanvas.removeComponentListener(layerCanvasComponentHandler);
        layerCanvas.removeMouseListener(layerCanvasMouseInputHandler);
        layerCanvas.removeMouseMotionListener(layerCanvasMouseInputHandler);
        layerCanvas.removeKeyListener(layerCanvasKeyHandler);
    }

    private void fireToolEvent(MouseEvent e) {
        if (tool != null) {
            ToolInputEvent toolInputEvent = createToolInputEvent(e);
            tool.handleEvent(toolInputEvent);
        }
    }

    private ToolInputEvent createToolInputEvent(MouseEvent e) {
        return new ToolInputEvent(layerCanvas, e, pixelX, pixelY, isPixelPosValid(levelPixelX, levelPixelY, level));
    }

    private ToolInputEvent createToolInputEvent(KeyEvent e) {
        return new ToolInputEvent(layerCanvas, e, pixelX, pixelY, isPixelPosValid(levelPixelX, levelPixelY, level));
    }

    private boolean isPixelPosValid(int currentPixelX, int currentPixelY, int currentLevel) {
        return currentPixelX >= 0 && currentPixelX < baseImageLayer.getImage(currentLevel).getWidth() && currentPixelY >= 0
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;

            if (tool != null && tool.isActive()) {
                final Viewport vp = getLayerCanvas().getViewport();
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
        if (tool.getDrawable() != null) {
            tool.getDrawable().draw(g2d);
        }
    }


    private void setPixelPos(MouseEvent e, boolean showBorder) {
        Point p = e.getPoint();
        Viewport viewport = getLayerCanvas().getViewport();
        final int currentLevel = baseImageLayer.getLevel(viewport);
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
                (getTool() == null || getTool().getDrawable() != null) &&
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

    protected final class ZoomHandler implements MouseWheelListener {

        @Override
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
         * Invoked when a mouse button is pressed on a component and then
         * dragged. Mouse drag events will continue to be delivered to the
         * component where the first originated until the mouse button is
         * released (regardless of whether the mouse position is within the
         * bounds of the component).
         */
        public final void mouseDragged(MouseEvent e) {
            updatePixelPos(e, true);
        }

        /**
         * Invoked when the mouse button has been moved on a component (with no
         * buttons no down).
         */
        public final void mouseMoved(MouseEvent e) {
            updatePixelPos(e, true);
        }

        private void updatePixelPos(MouseEvent e, boolean showBorder) {
            setPixelPos(e, showBorder);
            fireToolEvent(e);
        }
    }

    private class LayerCanvasComponentHandler extends ComponentAdapter {

        /**
             * Invoked when the component's size changes.
         */
        @Override
        public void componentResized(ComponentEvent e) {
        }

        /**
             * Invoked when the component has been made invisible.
         */
        @Override
        public void componentHidden(ComponentEvent e) {
            firePixelPosNotAvailable();
        }
    }

    private class LayerCanvasKeyHandler implements KeyListener {

        /**
             * Invoked when a key has been pressed.
         */
        public void keyPressed(KeyEvent e) {
            if (tool != null) {
                tool.handleEvent(createToolInputEvent(e));
            }
        }

        /**
             * Invoked when a key has been released.
         */
        public void keyReleased(KeyEvent e) {
            if (tool != null) {
                tool.handleEvent(createToolInputEvent(e));
            }
        }

        /**
             * Invoked when a key has been typed. This event occurs when a key
         * press is followed by a key dispose.
         */
        public void keyTyped(KeyEvent e) {
            if (tool != null) {
                tool.handleEvent(createToolInputEvent(e));
            }
        }
    }
}
