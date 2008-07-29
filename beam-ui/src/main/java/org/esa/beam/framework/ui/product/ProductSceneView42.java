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
import com.bc.layer.AbstractLayer;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import com.bc.layer.LayerModelChangeAdapter;
import com.bc.swing.ViewPane;
import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.impl.SelectTool;
import org.esa.beam.layer.NoDataLayer;
import org.esa.beam.layer.StyledLayer;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * The class <code>ProductSceneView</code> is a high-level image display component for color index/RGB images created
 * from one or more raster datasets of a data product.
 * <p/>
 * <p>It is also capable of displaying a graticule (geographical grid) and a ROI associated with a displayed raster
 * dataset.
 */
public class ProductSceneView42 extends ProductSceneView {

    private ImageDisplay imageDisplay;

    public ProductSceneView42(ProductSceneImage sceneImage) {
        super(sceneImage);
        Assert.notNull(sceneImage, "sceneImage");
        setLayerModel(sceneImage.getLayerModel());
        Rectangle modelArea = sceneImage.getModelArea();
        setModelBounds(modelArea);
        getViewModel().setModelOffset(modelArea.x, modelArea.y, 1.0);
        getImageDisplayComponent().setPreferredSize(modelArea.getSize());

        sceneImage.getLayerModel().addLayerModelChangeListener(new LayerModelChangeAdapter() {
            public void handleLayerModelChanged(LayerModel layerModel) {
                fireLayerContentChanged();
            }


            public void handleLayerChanged(LayerModel layerModel, Layer layer) {
                fireLayerContentChanged();
            }
        }
        );

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

    public AbstractTool[] getSelectToolDelegates() {
        ArrayList<AbstractTool> toolList = new ArrayList<AbstractTool>(8);
        for (int i = 0; i < getLayerCount(); i++) {
            // TODO IMAGING 4.5
            final Layer layer = getLayer(i);
            AbstractTool delegate = getDelegateTool(layer);
            if (delegate != null) {
                toolList.add(delegate);
            }
        }
        return toolList.toArray(new AbstractTool[toolList.size()]);
    }

    public void disposeLayerModel() {
        // TODO IMAGING 4.5
        getLayerModel().dispose();
    }

    public AffineTransform getBaseImageToViewTransform() {
        // TODO IMAGING 4.5
        final AffineTransform transform = new AffineTransform();
        transform.scale(getViewScale(), getViewScale());
        transform.translate(-getViewModel().getModelOffsetX(),
                            -getViewModel().getModelOffsetY());
        return transform;
    }

    public Rectangle2D getVisibleModelBounds() {
        // TODO IMAGING 4.5
        return new Rectangle2D.Double(getViewModel().getModelOffsetX(),
                                      getViewModel().getModelOffsetY(),
                                      getImageDisplayComponent().getWidth() / getViewScale(),
                                      getImageDisplayComponent().getHeight() / getViewScale());
    }

    public double getViewScale() {
        // TODO IMAGING 4.5
        return getViewModel().getViewScale();
    }

    public Rectangle2D getModelBounds() {
        // TODO IMAGING 4.5
        return getViewModel().getModelArea();
    }

    private void setModelBounds(Rectangle modelArea) {
        // TODO IMAGING 4.5
        getViewModel().setModelArea(modelArea);
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

    @Deprecated
    public void setModelOffset(double modelOffsetX, double modelOffsetY) {
        // TODO IMAGING 4.5
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY);
    }

    public void synchronizeViewport(ProductSceneView42 view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {
            // TODO IMAGING 4.5
            view.setModelOffset(
                    getViewModel().getModelOffsetX(),
                    getViewModel().getModelOffsetY(),
                    getViewScale());
        }

    }


    /**
     * Gets the base image displayed in this view.
     *
     * @return the base image
     */
    public RenderedImage getBaseImage() {
        // TODO IMAGING 4.5
        return getImageDisplayComponent() != null ? getImageDisplay().getImage() : null;
    }


    /**
     * Sets the base image displayed in this view.
     *
     * @param baseImage the base image
     */
    public void setBaseImage(RenderedImage baseImage) {
        Guardian.assertNotNull("baseImage", baseImage);
        if (getImageDisplayComponent() == null) {
            initUI(baseImage);
        }
        // TODO IMAGING 4.5
        getImageDisplay().setImage(baseImage);
        revalidate();
        repaint();
    }

    public int getBaseImageWidth() {
        // TODO IMAGING 4.5
        return getImageDisplay().getImageWidth();
    }

    public int getBaseImageHeight() {
        // TODO IMAGING 4.5
        return getImageDisplay().getImageHeight();
    }

    public RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha) {
        ////////////////////////////////////////////////////////////////////
        // << TODO IMAGING 4.5
        boolean oldOpaque = getImageDisplayComponent().isOpaque();
        final BufferedImage bi;
        try {
            getImageDisplayComponent().setOpaque(false);
            final int imageType = useAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
            if (entireImage) {
                final double modelOffsetXOld = getViewModel().getModelOffsetX();
                final double modelOffsetYOld = getViewModel().getModelOffsetY();
                final double viewScaleOld = getViewScale();
                try {
                    getViewModel().setModelOffset(0, 0, 1.0);
                    bi = new BufferedImage(getBaseImageWidth(),
                                           getBaseImageHeight(),
                                           imageType);
                    getImageDisplayComponent().paint(bi.createGraphics());
                } finally {
                    getViewModel().setModelOffset(modelOffsetXOld, modelOffsetYOld, viewScaleOld);
                }
            } else {
                bi = new BufferedImage(getImageDisplayComponent().getWidth(),
                                       getImageDisplayComponent().getHeight(),
                                       imageType);
                getImageDisplayComponent().paint(bi.createGraphics());
            }
        } finally {
            getImageDisplayComponent().setOpaque(oldOpaque);
        }
        // >> TODO IMAGING 4.5
        ////////////////////////////////////////////////////////////////////
        return bi;
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
    // Package/Private

    @Deprecated
    private void setModelOffset(double modelOffsetX, double modelOffsetY, double viewScale) {
        // TODO IMAGING 4.5
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY, viewScale);
    }


    @Deprecated
    private int getLayerCount() {
        // TODO IMAGING 4.5
        return getLayerModel().getLayerCount();
    }

    @Deprecated
    private Layer getLayer(int index) {
        // TODO IMAGING 4.5
        return getLayerModel().getLayer(index);
    }


    @Deprecated
    LayerModel getLayerModel() {
        // TODO IMAGING 4.5
        return getImageDisplay().getLayerModel();
    }

    @Deprecated
    private ViewModel getViewModel() {
        // TODO IMAGING 4.5
        return getImageDisplay().getViewModel();
    }


    @Deprecated
    private void setLayerModel(LayerModel layerModel) {
        // TODO IMAGING 4.5
        getImageDisplay().setLayerModel(layerModel);
    }

    @Deprecated
    private NoDataLayer getNoDataLayer() {
        // TODO IMAGING 4.5
        for (int i = 0; i < getLayerCount(); i++) {
            final Layer layer = getLayer(i);
            if (layer instanceof NoDataLayer) {
                return (NoDataLayer) layer;
            }
        }
        return null;
    }

    @Deprecated
    private AbstractTool getDelegateTool(Layer layer) {
        AbstractTool delegate = null;
        // TODO IMAGING 4.5
        if (layer instanceof AbstractLayer) {
            AbstractLayer abstractLayer = (AbstractLayer) layer;
            final Object value = abstractLayer.getPropertyValue(SelectTool.SELECT_TOOL_PROPERTY_NAME);
            if (value instanceof AbstractTool) {
                delegate = (AbstractTool) value;
            }
        }
        return delegate;
    }

    private void setImageProperties(PropertyMap propertyMap) {
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


    protected void copyPixelInfoStringToClipboard() {
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


    @Deprecated
    private ImageDisplay getImageDisplay() {
        // TODO IMAGING 4.5
        return imageDisplay;
    }

    protected void disposeImageDisplayComponent() {
        // TODO IMAGING 4.5
        if (getImageDisplayComponent() != null) {
            getImageDisplay().dispose();
            imageDisplay = null;
        }
    }

    protected PixelInfoFactory getPixelInfoFactory() {
        // TODO IMAGING 4.5
        return getImageDisplay().getPixelInfoFactory();
    }

    protected void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        // TODO IMAGING 4.5
        getImageDisplay().setPixelInfoFactory(pixelInfoFactory);
    }

    public RenderedImage updateNoDataImage(ProgressMonitor pm) throws Exception {
        final NoDataLayer noDataLayer = getNoDataLayer();
        if (getRaster().isValidMaskUsed()) {
            return noDataLayer.updateImage(true, pm);
        } else {
            noDataLayer.setImage(null);
            return null;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Inner/Nested Interfaces/Classes

}
