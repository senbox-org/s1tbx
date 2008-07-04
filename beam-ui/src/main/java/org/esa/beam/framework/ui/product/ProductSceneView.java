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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.Assert;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.ui.BasicImageView;
import org.esa.beam.framework.ui.PixelInfoFactory;
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

import javax.swing.JPopupMenu;
import java.awt.*;
import java.awt.event.MouseEvent;
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
public class ProductSceneView extends BasicImageView implements ProductNodeView,
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

    // todo nf/nf - move the following constants to a better place

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
    private RasterChangeHandler rasterChangeHandler;

    public ProductSceneView(ProductSceneImage sceneImage) {
        Assert.notNull(sceneImage, "sceneImage");
        this.sceneImage = sceneImage;
        setSourceImage(sceneImage.getImage());
        getImageDisplay().setLayerModel(sceneImage.getLayerModel());
        Rectangle modelArea = sceneImage.getModelArea();
        getImageDisplay().getViewModel().setModelArea(modelArea);
        getImageDisplay().getViewModel().setModelOffset(modelArea.x, modelArea.y, 1.0);
        getImageDisplay().setPreferredSize(modelArea.getSize());
        setPixelInfoFactory(this);

        rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        imageUpdateListenerList = new ArrayList<ImageUpdateListener>();
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
     * @return  the associated product.
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
     * @deprecated since BEAM 4.2, use {@link #getRaster(int)}
     */
    @Deprecated
    public RasterDataNode getRasterAt(int index) {
        return getRaster(index);
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
     * Gets the red product raster of a 3-banded RGB view.
     *
     * @return the red product raster, or <code>null</code> if this is single band view
     */
    @Deprecated
    public RasterDataNode getRedRaster() {
        return isRGB() ? sceneImage.getRasters()[0] : null;
    }

    /**
     * Gets the green product raster of a 3-banded RGB view.
     *
     * @return the green product raster, or <code>null</code> if this is single band view
     */
    @Deprecated
    public RasterDataNode getGreenRaster() {
        return isRGB() ? sceneImage.getRasters()[1] : null;
    }

    /**
     * Gets the blue product raster of a 3-banded RGB view.
     *
     * @return the blue product raster, or <code>null</code> if this is single band view
     */
    @Deprecated
    public RasterDataNode getBlueRaster() {
        return isRGB() ? sceneImage.getRasters()[2] : null;
    }

    /**
     * Gets all rasters of this view.
     *
     * @return all rasters of this view, array size is either 1 or 3 (RGB)
     */
    public RasterDataNode[] getRasters() {
        return sceneImage.getRasters();
    }

    /**
     * Sets the rasters of this product scene view
     *
     * @param rasters the rasters to be set
     */
    @Deprecated
    public void setRasters(final RasterDataNode[] rasters) {
        if (sceneImage.getRasters().length == 1) {
            sceneImage.getRasters()[0] = rasters[0];
        } else if (sceneImage.getRasters().length == 3) {
            sceneImage.getRasters()[0] = rasters[0];
            sceneImage.getRasters()[1] = rasters[1];
            sceneImage.getRasters()[2] = rasters[2];
        }
    }

    /**
     * Sets the RGB rasters to this product scene view
     *
     * @param redRaster
     * @param greenRaster
     * @param blueRaster
     */
    @Deprecated
    public void setRasters(RasterDataNode redRaster, RasterDataNode greenRaster, RasterDataNode blueRaster) {
        if (sceneImage.getRasters().length == 1) {
            sceneImage.getRasters()[0] = redRaster;
        } else if (sceneImage.getRasters().length == 3) {
            sceneImage.getRasters()[0] = redRaster;
            sceneImage.getRasters()[1] = greenRaster;
            sceneImage.getRasters()[2] = blueRaster;
        }
    }

    public boolean isMono() {
        return sceneImage.getRasters().length == 1;
    }

    public boolean isRGB() {
        return sceneImage.getRasters().length >= 3;
    }

    /**
     * @deprecated since BEAM 4.2, no replacement
     * @return {@code sceneImage.getImageInfo().getHistogramMatching().toString()}
     */
    @Deprecated
    public String getHistogramMatching() {
        return sceneImage.getImageInfo().getHistogramMatching().toString();
    }

    /**
     * @deprecated since BEAM 4.2, no replacement
     * @param histogramMatching  histogram matching
     */
    @Deprecated
    public void setHistogramMatching(String histogramMatching) {
        sceneImage.getImageInfo().setHistogramMatching(ImageInfo.getHistogramMatching(histogramMatching));
    }

    public boolean isNoDataOverlayEnabled() {
        return sceneImage.getNoDataLayer().isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        sceneImage.getNoDataLayer().setVisible(enabled);
    }

    public boolean isGraticuleOverlayEnabled() {
        return sceneImage.getGraticuleLayer().isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        sceneImage.getGraticuleLayer().setVisible(enabled);
    }

    public boolean isPinOverlayEnabled() {
        return sceneImage.getPinLayer().isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        sceneImage.getPinLayer().setVisible(enabled);
    }

    public boolean isGcpOverlayEnabled() {
        return sceneImage.getGcpLayer().isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        sceneImage.getGcpLayer().setVisible(enabled);
    }

    public boolean isROIOverlayEnabled() {
        return sceneImage.getROILayer().isVisible();
    }

    public void setROIOverlayEnabled(boolean enabled) {
        sceneImage.getROILayer().setVisible(enabled);
    }

    public boolean isShapeOverlayEnabled() {
        return sceneImage.getFigureLayer().isVisible();
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        sceneImage.getFigureLayer().setVisible(enabled);
    }

    public Figure getCurrentShapeFigure() {
        return sceneImage.getFigureLayer().getNumFigures() > 0 ? sceneImage.getFigureLayer().getFigureAt(0) : null;
    }

    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        setShapeOverlayEnabled(true);
        final Figure oldShapeFigure = getCurrentShapeFigure();
        if (currentShapeFigure != oldShapeFigure) {
            if (oldShapeFigure != null) {
                sceneImage.getFigureLayer().removeFigure(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                sceneImage.getFigureLayer().addFigure(currentShapeFigure);
            }
        }
    }

    public RenderedImage getROIImage() {
        return sceneImage.getROILayer().getImage();
    }

    public void setROIImage(RenderedImage roiImage) {
        sceneImage.getROILayer().setImage(roiImage);
    }

    /**
     * @deprecated in 4.1, use {@link #updateROIImage(boolean,com.bc.ceres.core.ProgressMonitor)} instead
     */
    public void updateROIImage(boolean recreate) throws Exception {
        updateROIImage(recreate, ProgressMonitor.NULL);
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        sceneImage.getROILayer().updateImage(recreate, pm);
    }

    public Figure getRasterROIShapeFigure() {
        return sceneImage.getROILayer().getRasterROIShapeFigure();
    }

    @Override
    public JPopupMenu createPopupMenu(Component component) {
        return null;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        JPopupMenu popupMenu = super.createPopupMenu(event.getComponent());
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

    public void updateImage(ProgressMonitor pm) throws IOException {
        StopWatch stopWatch = new StopWatch();
        Cursor oldCursor = UIUtils.setRootFrameWaitCursor(this);
        final RenderedImage sourceImage = createImage(pm);
        setSourceImage(sourceImage);
        stopWatch.stopAndTrace("ProductSceneView.updateImage");
        fireImageUpdated();
        UIUtils.setRootFrameCursor(this, oldCursor);
    }

    private RenderedImage createImage(ProgressMonitor pm) throws IOException {
        return sceneImage.createImage(pm);
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
     * Sets the layer properties using the given property map.
     *
     * @param propertyMap the layer property map
     */
    public void setLayerProperties(PropertyMap propertyMap) {

        setImageProperties(propertyMap);

        final LayerModel layerModel = getImageDisplay().getLayerModel();
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
     * @deprecated
     */
    public void setImageProperties(PropertyMap propertyMap) {
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

    /**
     * Returns the current tool for this drawing.
     */
    public Tool getTool() {
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
        getImageDisplay().setTool(tool);
    }

    /**
     * Draws the tool in an interactive mode.
     */
    public void repaintTool() {
        getImageDisplay().repaintTool();
    }

    /**
     * Displays a status message. If <code>null</code> is passed to this method, the status message is reset or
     * cleared.
     *
     * @param message the message to be displayed
     */
    public void setStatusMessage(String message) {
        getImageDisplay().setStatusMessage(message);
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

    /**
     * Removes a figure from the drawing.
     */
    public void removeFigure(Figure figure) {
        sceneImage.getFigureLayer().removeFigure(figure);
    }

    /**
     * Gets all figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getAllFigures() {
        return sceneImage.getFigureLayer().getAllFigures();
    }

    /**
     * Gets the figure at the specified index.
     *
     * @return the figure, never <code>null</code>
     */
    public Figure getFigureAt(int index) {
        return sceneImage.getFigureLayer().getFigureAt(index);
    }

    /**
     * Gets all figures having an attribute with the given name.
     *
     * @param name the attribute name
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getFiguresWithAttribute(String name) {
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
        return sceneImage.getFigureLayer().getFiguresWithAttribute(name, value);
    }

    /**
     * Returns the number of figures.
     */
    public int getNumFigures() {
        return sceneImage.getFigureLayer().getNumFigures();
    }

    /**
     * Gets all selected figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getSelectedFigures() {
        return sceneImage.getFigureLayer().getSelectedFigures();
    }

    /**
     * Gets all layer manager listeners of this layer.
     */
    public ImageUpdateListener[] getImageUpdateListeners() {
        return imageUpdateListenerList.toArray(new ImageUpdateListener[imageUpdateListenerList.size()]);
    }

    /**
     * Adds a layer manager listener to this layer.
     */
    public void addImageUpdateListener(ImageUpdateListener listener) {
        if (listener != null && !imageUpdateListenerList.contains(listener)) {
            imageUpdateListenerList.add(listener);
        }
    }

    /**
     * Removes a layer manager listener from this layer.
     */
    public void removeImageUpdateListener(ImageUpdateListener listener) {
        if (listener != null) {
            imageUpdateListenerList.remove(listener);
        }
    }

    public void fireImageUpdated() {
        for (ImageUpdateListener a_imageUpdateListenerList : imageUpdateListenerList) {
            a_imageUpdateListenerList.handleImageUpdated(this);
        }
    }

    public static interface ImageUpdateListener {

        void handleImageUpdated(ProductSceneView view);
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
//            setCheckInvalids(true);
//            setNoDataValue(0);
//            setNoDataValueUsed(true);
        }
    }
}
