/*
 * $Id: ProductSceneView.java,v 1.3 2006/12/15 08:41:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import com.bc.swing.ViewPane;
import com.bc.view.ViewModel;
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
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.esa.beam.layer.StyledLayer;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;
import org.esa.beam.util.StopWatch;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The class <code>ProductSceneView</code> is a high-level image display component for color index/RGB images created
 * from one or more raster datasets of a data product.
 * <p/>
 * <p>It is also capable of displaying a graticule (geographical grid) and a ROI associated with a displayed raster
 * dataset.
 */
public class ProductSceneView extends BasicView implements ProductNodeView,
        DrawingEditor,
        PropertyMapChangeListener,
        PixelInfoFactory {

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

    // TODO IMAGING 4.5: Move these style settings away
    // TODO IMAGING 4.5: Layer.getStyle(), SVG property names!
    public static String IMAGE_INTERPOLATION_NEAREST_NEIGHBOUR = "Nearest Neighbour";
    public static String IMAGE_INTERPOLATION_BILINEAR = "Bi-Linear Interpolation";
    public static String IMAGE_INTERPOLATION_BICUBIC = "Bi-Cubic Interpolation";
    public static String IMAGE_INTERPOLATION_SYSTEM_DEFAULT = "System Default";
    public static String DEFAULT_IMAGE_INTERPOLATION_METHOD = IMAGE_INTERPOLATION_SYSTEM_DEFAULT;
    public static final Color DEFAULT_IMAGE_BORDER_COLOR = new Color(204, 204, 255);
    public static final Color DEFAULT_IMAGE_BACKGROUND_COLOR = new Color(51, 51, 51);
    public static final double DEFAULT_IMAGE_BORDER_SIZE = 2.0;
    public static final int DEFAULT_IMAGE_VIEW_BORDER_SIZE = 64;

    private ImageDisplay imageDisplay;
    private ProductSceneImage sceneImage;
    private ArrayList<ImageUpdateListener> imageUpdateListenerList;
    private RasterChangeHandler rasterChangeHandler;

    public ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");
        setOpaque(false);
        this.sceneImage = sceneImage;
        setSourceImage(sceneImage.getImage());
        setLayerModel(sceneImage.getLayerModel());
        Rectangle modelArea = sceneImage.getModelArea();
        getViewModel().setModelArea(modelArea);
        getViewModel().setModelOffset(modelArea.x, modelArea.y, 1.0);
        getImageDisplayComponent().setPreferredSize(modelArea.getSize());
        setPixelInfoFactory(this);

        rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        imageUpdateListenerList = new ArrayList<ImageUpdateListener>(5);
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

    public void updateImage(ProgressMonitor pm) throws IOException {
        StopWatch stopWatch = new StopWatch();
        Cursor oldCursor = UIUtils.setRootFrameWaitCursor(this);
        final RenderedImage sourceImage = createImage(pm);
        setSourceImage(sourceImage);
        stopWatch.stopAndTrace("ProductSceneView.updateImage");
        fireImageUpdated();
        UIUtils.setRootFrameCursor(this, oldCursor);
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
        addStandardPopupMenuItems(popupMenu);
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
        for (int i = 0; i < sceneImage.getRasters().length; i++) {
            final RasterDataNode raster = sceneImage.getRasters()[i];
            if (raster instanceof RGBChannel) {
                RGBChannel rgbChannel = (RGBChannel) raster;
                rgbChannel.dispose();
            }
            sceneImage.getRasters()[i] = null;
        }
        sceneImage = null;
        imageUpdateListenerList.clear();
        imageUpdateListenerList = null;

        if (getImageDisplayComponent() != null) {
            // ensure that imageDisplay.dispose() is run in the EDT
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    disposeImageDisplayComponent();
                    imageDisplay = null;
                }
            });
        }

        super.dispose();
    }

    /**
     * The product scene image associated with this view.
     *
     * @return the product scene image
     */
    public ProductSceneImage getScene() {
        return sceneImage;
    }

    /**
     * @return the associated product.
     */
    public Product getProduct() {
        return getRaster().getProduct();
    }

    /**
     * Gets the number of raster datasets.
     *
     * @return the number of raster datasets, always <code>1</code> for single banded palette images or <code>3</code>
     *         for RGB images
     */
    public int getNumRasters() {
        return sceneImage.getRasters().length;
    }

    /**
     * Gets the product raster with the specified index.
     *
     * @param index the zero-based product raster index
     * @return the product raster with the given index
     */
    public RasterDataNode getRaster(int index) {
        return sceneImage.getRasters()[index];
    }

    /**
     * Gets the product raster of a single banded view.
     *
     * @return the product raster, or <code>null</code> if this is a 3-banded RGB view
     */
    public RasterDataNode getRaster() {
        return sceneImage.getRasters()[0];
    }

    /**
     * Gets all rasters of this view.
     *
     * @return all rasters of this view, array size is either 1 or 3 (RGB)
     */
    public RasterDataNode[] getRasters() {
        return sceneImage.getRasters();
    }

    public boolean isRGB() {
        return sceneImage.getRasters().length >= 3;
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

    public boolean isNoDataOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getNoDataLayer().isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getNoDataLayer().setVisible(enabled);
    }

    public boolean isGraticuleOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getGraticuleLayer().isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getGraticuleLayer().setVisible(enabled);
    }

    public boolean isPinOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getPinLayer().isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getPinLayer().setVisible(enabled);
    }

    public boolean isGcpOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getGcpLayer().isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getGcpLayer().setVisible(enabled);
    }

    public boolean isROIOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getROILayer().isVisible();
    }

    public void setROIOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getROILayer().setVisible(enabled);
    }

    public boolean isShapeOverlayEnabled() {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().isVisible();
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        // TODO IMAGING 4.5
        sceneImage.getFigureLayer().setVisible(enabled);
    }

    public RenderedImage getROIImage() {
        // TODO IMAGING 4.5
        return sceneImage.getROILayer().getImage();
    }

    public void setROIImage(RenderedImage roiImage) {
        // TODO IMAGING 4.5
        sceneImage.getROILayer().setImage(roiImage);
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        // TODO IMAGING 4.5
        sceneImage.getROILayer().updateImage(recreate, pm);
    }

    public Figure getRasterROIShapeFigure() {
        // TODO IMAGING 4.5
        return sceneImage.getROILayer().getRasterROIShapeFigure();
    }

    public Figure getCurrentShapeFigure() {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getNumFigures() > 0 ? sceneImage.getFigureLayer().getFigureAt(0) : null;
    }

    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        setShapeOverlayEnabled(true);
        final Figure oldShapeFigure = getCurrentShapeFigure();
        if (currentShapeFigure != oldShapeFigure) {
            if (oldShapeFigure != null) {
                // TODO IMAGING 4.5
                sceneImage.getFigureLayer().removeFigure(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                // TODO IMAGING 4.5
                sceneImage.getFigureLayer().addFigure(currentShapeFigure);
            }
        }
    }

    /**
     * Removes a figure from the drawing.
     */
    public void removeFigure(Figure figure) {
        // TODO IMAGING 4.5
        sceneImage.getFigureLayer().removeFigure(figure);
    }

    /**
     * Gets all figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getAllFigures() {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getAllFigures();
    }

    /**
     * Gets the figure at the specified index.
     *
     * @return the figure, never <code>null</code>
     */
    public Figure getFigureAt(int index) {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getFigureAt(index);
    }

    /**
     * Gets all figures having an attribute with the given name.
     *
     * @param name the attribute name
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getFiguresWithAttribute(String name) {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getFiguresWithAttribute(name);
    }

    /**
     * Gets all figures having an attribute with the given name and value.
     *
     * @param name  the attribute name, must not be <code>null</code>
     * @param value the attribute value, must not be <code>null</code>
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getFiguresWithAttribute(String name, Object value) {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getFiguresWithAttribute(name, value);
    }

    /**
     * Returns the number of figures.
     */
    public int getNumFigures() {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getNumFigures();
    }

    /**
     * Gets all selected figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getSelectedFigures() {
        // TODO IMAGING 4.5
        return sceneImage.getFigureLayer().getSelectedFigures();
    }


    /**
     * Sets the layer properties using the given property map.
     *
     * @param propertyMap the layer property map
     */
    public void setLayerProperties(PropertyMap propertyMap) {

        setImageProperties(propertyMap);

        // TODO IMAGING 4.5
        final LayerModel layerModel = getLayerModel();
        final boolean suspendedOld = layerModel.isLayerModelChangeFireingSuspended();
        layerModel.setLayerModelChangeFireingSuspended(true);

        int layerCount = layerModel.getLayerCount();
        for (int i = 0; i < layerCount; i++) {
            Layer layer = layerModel.getLayer(i);
            if (layer instanceof StyledLayer) {
                StyledLayer styledLayer = (StyledLayer) layer;
                styledLayer.setStyleProperties(propertyMap);
            }
        }

        layerModel.setLayerModelChangeFireingSuspended(suspendedOld);
        layerModel.fireLayerModelChanged();
    }

    /**
     * Adds a new pixel position listener to this image display component.
     *
     * @param listener the pixel position listener to be added
     */
    public void addPixelPositionListener(PixelPositionListener listener) {
        // TODO IMAGING 4.5
        getImageDisplay().addPixelPositionListener(listener);
    }

    /**
     * Removes a pixel position listener from this image display component.
     *
     * @param listener the pixel position listener to be removed
     */
    public void removePixelPositionListener(PixelPositionListener listener) {
        // TODO IMAGING 4.5
        getImageDisplay().removePixelPositionListener(listener);
    }

    public LayerModel getLayerModel() {
        // TODO IMAGING 4.5
        return getImageDisplay().getLayerModel();
    }

    public ViewModel getViewModel() {
        // TODO IMAGING 4.5
        return getImageDisplay().getViewModel();
    }

    public int getImageWidth() {
        // TODO IMAGING 4.5
        return getImageDisplay().getImageWidth();
    }

    public int getImageHeight() {
        // TODO IMAGING 4.5
        return getImageDisplay().getImageHeight();
    }

    public JComponent getImageDisplayComponent() {
        // TODO IMAGING 4.5
        return getImageDisplay();
    }

    public void zoom(Rectangle rect) {
        // TODO IMAGING 4.5
        getImageDisplay().zoom(rect);
    }

    public void zoom(double x, double y, double viewScale) {
        // TODO IMAGING 4.5
        getImageDisplay().zoom(x, y, viewScale);
    }

    public void zoom(double viewScale) {
        // TODO IMAGING 4.5
        getImageDisplay().zoom(viewScale);
    }

    public void zoomAll() {
        // TODO IMAGING 4.5
        getImageDisplay().zoomAll();
    }

    /**
     * Gets the source image displayed in this view.
     *
     * @return the source image
     */
    public RenderedImage getSourceImage() {
        // TODO IMAGING 4.5
        return getImageDisplayComponent() != null ? getImageDisplay().getImage() : null;
    }


    /**
     * Gets the source image to be displayed in this view.
     *
     * @param sourceImage the source image
     */
    public void setSourceImage(RenderedImage sourceImage) {
        Guardian.assertNotNull("sourceImage", sourceImage);
        if (getImageDisplayComponent() == null) {
            initUI(sourceImage);
        }
        // TODO IMAGING 4.5
        getImageDisplay().setImage(sourceImage);
        revalidate();
        repaint();
    }

    /**
     * Returns the current tool for this drawing.
     */
    public Tool getTool() {
        // TODO IMAGING 4.5
        return getImageDisplay().getTool();
    }

    /**
     * Sets the current tool for this drawing.
     */
    public void setTool(Tool tool) {
        if (tool != null) {
            tool.setDrawingEditor(this);
            setCursor(tool.getCursor());
        }
        // TODO IMAGING 4.5
        getImageDisplay().setTool(tool);
    }

    /**
     * Draws the tool in an interactive mode.
     */
    public void repaintTool() {
        // TODO IMAGING 4.5
        getImageDisplay().repaintTool();
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private void setLayerModel(LayerModel layerModel) {
        // TODO IMAGING 4.5
        getImageDisplay().setLayerModel(layerModel);
    }

    private void setImageProperties(PropertyMap propertyMap) {
        // todo 3 nf,nf - 1) move display properties of imageDisplay to imageLayer
        // todo 3 nf/nf - 2) move the following code to ImageLayer.setProperties
        // todo 3 nf,nf - 3) use _imageLayer.setProperties(propertyMap); instead

        final boolean pixelBorderShown = propertyMap.getPropertyBool("pixel.border.shown", true);
        final boolean imageBorderShown = propertyMap.getPropertyBool("image.border.shown", true);
        final float imageBorderSize = (float) propertyMap.getPropertyDouble("image.border.size",
                                                                            DEFAULT_IMAGE_BORDER_SIZE);
        final Color imageBorderColor = propertyMap.getPropertyColor("image.border.color", DEFAULT_IMAGE_BORDER_COLOR);
        final Color backgroundColor = propertyMap.getPropertyColor("image.background.color",
                                                                   DEFAULT_IMAGE_BACKGROUND_COLOR);
        final boolean antialiasing = propertyMap.getPropertyBool(PROPERTY_KEY_GRAPHICS_ANTIALIASING, false);
        final String interpolation = propertyMap.getPropertyString(PROPERTY_KEY_IMAGE_INTERPOLATION,
                                                                   DEFAULT_IMAGE_INTERPOLATION_METHOD);

        // TODO IMAGING 4.5
        getImageDisplay().setPixelBorderShown(pixelBorderShown);
        getImageDisplay().setImageBorderShown(imageBorderShown);
        getImageDisplay().setImageBorderSize(imageBorderSize);
        getImageDisplay().setImageBorderColor(imageBorderColor);
        getImageDisplay().setBackground(backgroundColor);
        getImageDisplay().setAntialiasing(antialiasing ?
                RenderingHints.VALUE_ANTIALIAS_ON :
                RenderingHints.VALUE_ANTIALIAS_OFF);
        getImageDisplay().setInterpolation(interpolation.equalsIgnoreCase(IMAGE_INTERPOLATION_BICUBIC) ?
                RenderingHints.VALUE_INTERPOLATION_BICUBIC :
                interpolation.equalsIgnoreCase(IMAGE_INTERPOLATION_BILINEAR) ?
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                        interpolation.equalsIgnoreCase(IMAGE_INTERPOLATION_NEAREST_NEIGHBOUR) ?
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR :
                                null);
    }


    private void copyPixelInfoStringToClipboard() {
        // TODO IMAGING 4.5
        getImageDisplay().copyPixelInfoStringToClipboard();
    }

    private void initUI(RenderedImage sourceImage) {
        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);

        imageDisplay = new ImageDisplay(sourceImage);

        getImageDisplayComponent().setOpaque(true);
        getImageDisplayComponent().addMouseListener(popupMenuHandler);
        getImageDisplayComponent().addMouseWheelListener(new ZoomHandler());
        getImageDisplayComponent().addKeyListener(popupMenuHandler);

        setLayout(new BorderLayout());
        // TODO IMAGING 4.5
        ViewPane imageDisplayScroller = getImageDisplay().createViewPane();
        add(imageDisplayScroller, BorderLayout.CENTER);
    }

    private ImageDisplay getImageDisplay() {
        // TODO IMAGING 4.5
        return imageDisplay;
    }

    private void disposeImageDisplayComponent() {
        // TODO IMAGING 4.5
        getImageDisplay().dispose();
    }

    private PixelInfoFactory getPixelInfoFactory() {
        // TODO IMAGING 4.5
        return getImageDisplay().getPixelInfoFactory();
    }

    private void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        // TODO IMAGING 4.5
        getImageDisplay().setPixelInfoFactory(pixelInfoFactory);
    }

    private static void addStandardPopupMenuItems(JPopupMenu popupMenu) {
        popupMenu.addSeparator();
    }

    private RenderedImage createImage(ProgressMonitor pm) throws IOException {
        return sceneImage.createImage(pm);
    }

    private void addCopyPixelInfoToClipboardMenuItem(JPopupMenu popupMenu) {
        if (getPixelInfoFactory() != null) {
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
    }

    private void fireImageUpdated() {
        for (ImageUpdateListener listener : imageUpdateListenerList) {
            listener.handleImageUpdated(this);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Inner/Nested Interfaces/Classes

    public static interface ImageUpdateListener {

        void handleImageUpdated(ProductSceneView view);
    }

    /**
     * A band that is used as an RGB channel for RGB image views.
     * These bands shall not be added to {@link Product}s but they are always owned by the {@link Product}
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

    private class RasterChangeHandler implements ProductNodeListener {

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

    private final class ZoomHandler implements MouseWheelListener {

        public void mouseWheelMoved(MouseWheelEvent e) {
            int notches = e.getWheelRotation();
            double currentViewScale = getViewModel().getViewScale();
            if (notches < 0) {
                zoom(currentViewScale * 1.1f);
            } else {
                zoom(currentViewScale * 0.9f);
            }
        }
    }
}
