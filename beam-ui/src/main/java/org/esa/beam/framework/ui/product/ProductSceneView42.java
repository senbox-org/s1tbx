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
import com.bc.layer.AbstractLayer;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import com.bc.layer.LayerModelChangeAdapter;
import com.bc.swing.ViewPane;
import com.bc.view.ViewModel;
import com.bc.view.ViewModelChangeListener;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.impl.SelectTool;
import org.esa.beam.layer.NoDataLayer;
import org.esa.beam.layer.StyledLayer;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.io.IOException;

class ProductSceneView42 extends ProductSceneView {

    private ImageDisplay imageDisplay;

    ProductSceneView42(ProductSceneImage42 sceneImage) {
        super(sceneImage);

        setOpaque(false);
        setLayout(new BorderLayout());

        imageDisplay = new ImageDisplay(sceneImage.getBaseImage());
        ViewPane imageDisplayScroller = imageDisplay.createViewPane();
        add(imageDisplayScroller, BorderLayout.CENTER);

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        imageDisplay.setOpaque(true);
        imageDisplay.addMouseListener(popupMenuHandler);
        imageDisplay.addMouseWheelListener(new ZoomHandler());
        imageDisplay.addKeyListener(popupMenuHandler);

        setLayerModel(sceneImage.getLayerModel());
        Rectangle modelArea = sceneImage.getModelArea();
        setModelBounds(modelArea);
        getViewModel().setModelOffset(modelArea.x, modelArea.y, 1.0);
        imageDisplay.setPreferredSize(modelArea.getSize());

        setPixelInfoFactory(this);

        sceneImage.getLayerModel().addLayerModelChangeListener(new LayerModelChangeAdapter() {
            @Override
            public void handleLayerModelChanged(LayerModel layerModel) {
                fireLayerContentChanged();
            }

            @Override
            public void handleLayerChanged(LayerModel layerModel, Layer layer) {
                fireLayerContentChanged();
            }
        });

        getViewModel().addViewModelChangeListener(new ViewModelChangeListener() {
            public void handleViewModelChanged(ViewModel viewModel) {
                fireLayerViewportChanged(false);
            }
        });
    }

    ProductSceneImage42 getSceneImage42() {
        return (ProductSceneImage42) getSceneImage();
    }

    /**
     * Sets the layer properties using the given property map.
     *
     * @param propertyMap the layer property map
     */
    @Override
    public void setLayerProperties(PropertyMap propertyMap) {

        setImageProperties(propertyMap);

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
    @Override
    public void addPixelPositionListener(PixelPositionListener listener) {
        getImageDisplay().addPixelPositionListener(listener);
    }

    /**
     * Removes a pixel position listener from this image display component.
     *
     * @param listener the pixel position listener to be removed
     */
    @Override
    public void removePixelPositionListener(PixelPositionListener listener) {
        getImageDisplay().removePixelPositionListener(listener);
    }

    @Override
    public AbstractTool[] getSelectToolDelegates() {
        ArrayList<AbstractTool> toolList = new ArrayList<AbstractTool>(8);
        for (int i = 0; i < getLayerCount(); i++) {
            final Layer layer = getLayer(i);
            AbstractTool delegate = getDelegateTool(layer);
            if (delegate != null) {
                toolList.add(delegate);
            }
        }
        return toolList.toArray(new AbstractTool[toolList.size()]);
    }

    @Override
    public void disposeLayers() {
        getLayerModel().dispose();
    }

    public double getOrientation() {
        return 0;
    }

    @Override
    public AffineTransform getBaseImageToViewTransform() {
        final AffineTransform transform = new AffineTransform();

        transform.scale(getZoomFactor(), getZoomFactor());
        transform.translate(-getViewModel().getModelOffsetX(),
                            -getViewModel().getModelOffsetY());
        return transform;
    }

    @Override
    public Rectangle2D getVisibleModelBounds() {
        return new Rectangle2D.Double(getViewModel().getModelOffsetX(),
                                      getViewModel().getModelOffsetY(),
                                      getImageDisplayComponent().getWidth() / getZoomFactor(),
                                      getImageDisplayComponent().getHeight() / getZoomFactor());
    }

    @Override
    public double getZoomFactor() {
        return getViewModel().getViewScale();
    }

    @Override
    public Rectangle2D getModelBounds() {
        return getViewModel().getModelArea();
    }

    private void setModelBounds(Rectangle modelArea) {
        getViewModel().setModelArea(modelArea);
    }

    @Override
    public JComponent getImageDisplayComponent() {
        return getImageDisplay();
    }

    @Override
    public void zoom(Rectangle2D modelRect) {
        getImageDisplay().zoom(modelRect);
    }

    @Override
    public void zoom(double x, double y, double viewScale) {
        getImageDisplay().zoom(x, y, viewScale);
    }

    @Override
    public void zoom(double viewScale) {
        getImageDisplay().zoom(viewScale);
    }

    @Override
    public void zoomAll() {
        getImageDisplay().zoomAll();
    }

    @Override
    public void move(double modelOffsetX, double modelOffsetY) {
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY);
    }

    @Override
    public void synchronizeViewport(ProductSceneView view) {
        final Product currentProduct = getRaster().getProduct();
        final Product otherProduct = view.getRaster().getProduct();
        if (otherProduct == currentProduct ||
                otherProduct.isCompatibleProduct(currentProduct, 1.0e-3f)) {
            ((ProductSceneView42) view).setModelOffset(getViewModel().getModelOffsetX(),
                                                       getViewModel().getModelOffsetY(),
                                                       getZoomFactor());
        }

    }

    @Override
    public void renderThumbnail(BufferedImage thumbnailImage) {
        final Graphics2D graphics = thumbnailImage.createGraphics();
        final ImageDisplay painter = new ImageDisplay(getBaseImage());
        painter.setSize(thumbnailImage.getWidth(), thumbnailImage.getHeight());
        painter.setOpaque(true);
        painter.setBackground(getImageDisplayComponent().getBackground());
        painter.setForeground(getImageDisplayComponent().getForeground());
        painter.getViewModel().setViewScaleMax(null);
        painter.getViewModel().setModelArea(getModelBounds());
        painter.zoomAll();
        painter.paintComponent(graphics);
        painter.dispose();
        graphics.dispose();
    }

    /**
     * Gets the base image displayed in this view.
     *
     * @return the base image
     */
    private RenderedImage getBaseImage() {
        return getImageDisplayComponent() != null ? getImageDisplay().getImage() : null;
    }


    /**
     * Sets the base image displayed in this view.
     *
     * @param baseImage the base image
     */
    private void setBaseImage(RenderedImage baseImage) {
        Guardian.assertNotNull("baseImage", baseImage);
        getImageDisplay().setImage(baseImage);
        revalidate();
        repaint();
    }

    @Override
    public void updateImage(ProgressMonitor pm) throws IOException {
        StopWatch stopWatch = new StopWatch();
        Cursor oldCursor = UIUtils.setRootFrameWaitCursor(this);
        final RenderedImage sourceImage = getSceneImage42().createBaseImage(pm);
        setBaseImage(sourceImage);
        stopWatch.stopAndTrace("ProductSceneView.updateImage");
        fireImageUpdated();
        UIUtils.setRootFrameCursor(this, oldCursor);
    }


    @Override
    public Rectangle getVisibleImageBounds() {

        final int imageWidth = getBaseImage().getWidth();
        final int imageHeight = getBaseImage().getHeight();
        final Rectangle2D bounds2D = getVisibleModelBounds();
        int x = (int) Math.round(bounds2D.getX());
        int y = (int) Math.round(bounds2D.getY());
        int width = (int) Math.round(bounds2D.getWidth());
        int height = (int) Math.round(bounds2D.getHeight());

        if (x < 0) {
            width += x;
            x = 0;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        Rectangle bounds = null;
        if (x <= imageWidth && y <= imageHeight && width >= 1 && height >= 1) {
            final int xMax = x + width;
            if (xMax > imageWidth) {
                width += imageWidth - xMax;
                x = imageWidth - width;
            }
            final int yMax = y + height;
            if (yMax > imageHeight) {
                height += imageHeight - yMax;
                y = imageHeight - height;
            }
            bounds = new Rectangle(x, y, width, height);
        }
        return bounds;
    }


    @Override
    public RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha) {
        boolean oldOpaque = getImageDisplayComponent().isOpaque();
        final BufferedImage bi;
        try {
            getImageDisplayComponent().setOpaque(false);
            final int imageType = useAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
            if (entireImage) {
                final double modelOffsetXOld = getViewModel().getModelOffsetX();
                final double modelOffsetYOld = getViewModel().getModelOffsetY();
                final double viewScaleOld = getZoomFactor();
                try {
                    getViewModel().setModelOffset(0, 0, 1.0);
                    bi = new BufferedImage(getBaseImage().getWidth(),
                                           getBaseImage().getHeight(),
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
        return bi;
    }


    /**
     * Returns the current tool for this drawing.
     */
    @Override
    public Tool getTool() {
        return getImageDisplay().getTool();
    }

    /**
     * Sets the current tool for this drawing.
     */
    @Override
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
    @Override
    public void repaintTool() {
        getImageDisplay().repaintTool();
    }

    /////////////////////////////////////////////////////////////////////////
    // Package/Private

    private void setModelOffset(double modelOffsetX, double modelOffsetY, double viewScale) {
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY, viewScale);
    }


    private int getLayerCount() {
        return getLayerModel().getLayerCount();
    }

    private Layer getLayer(int index) {
        return getLayerModel().getLayer(index);
    }


    LayerModel getLayerModel() {
        return getImageDisplay().getLayerModel();
    }

    private ViewModel getViewModel() {
        return getImageDisplay().getViewModel();
    }


    private void setLayerModel(LayerModel layerModel) {
        getImageDisplay().setLayerModel(layerModel);
    }

    private NoDataLayer getNoDataLayer() {
        for (int i = 0; i < getLayerCount(); i++) {
            final Layer layer = getLayer(i);
            if (layer instanceof NoDataLayer) {
                return (NoDataLayer) layer;
            }
        }
        return null;
    }

    private AbstractTool getDelegateTool(Layer layer) {
        AbstractTool delegate = null;
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


    @Override
    protected void copyPixelInfoStringToClipboard() {
        getImageDisplay().copyPixelInfoStringToClipboard();
    }

    private ImageDisplay getImageDisplay() {
        return imageDisplay;
    }

    @Override
    protected void disposeImageDisplayComponent() {
        if (getImageDisplayComponent() != null) {
            getImageDisplay().dispose();
            imageDisplay = null;
        }
    }

    @Override
    protected PixelInfoFactory getPixelInfoFactory() {
        return getImageDisplay().getPixelInfoFactory();
    }

    @Override
    protected void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        getImageDisplay().setPixelInfoFactory(pixelInfoFactory);
    }

    @Override
    public void updateNoDataImage(ProgressMonitor pm) throws Exception {
        final NoDataLayer noDataLayer = getNoDataLayer();
        if (getRaster().isValidMaskUsed()) {
            noDataLayer.updateImage(true, pm);
        } else {
            noDataLayer.setImage(null);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Inner/Nested Interfaces/Classes

    @Override
    public boolean isNoDataOverlayEnabled() {
        return getSceneImage42().getNoDataLayer().isVisible();
    }

    @Override
    public void setNoDataOverlayEnabled(boolean enabled) {
        getSceneImage42().getNoDataLayer().setVisible(enabled);
    }

    @Override
    public boolean isGraticuleOverlayEnabled() {
        return getSceneImage42().getGraticuleLayer().isVisible();
    }

    @Override
    public void setGraticuleOverlayEnabled(boolean enabled) {
        getSceneImage42().getGraticuleLayer().setVisible(enabled);
    }

    @Override
    public boolean isPinOverlayEnabled() {
        return getSceneImage42().getPinLayer().isVisible();
    }

    @Override
    public void setPinOverlayEnabled(boolean enabled) {
        getSceneImage42().getPinLayer().setVisible(enabled);
    }

    @Override
    public boolean isGcpOverlayEnabled() {
        return getSceneImage42().getGcpLayer().isVisible();
    }

    @Override
    public void setGcpOverlayEnabled(boolean enabled) {
        getSceneImage42().getGcpLayer().setVisible(enabled);
    }

    @Override
    public boolean isROIOverlayEnabled() {
        return getSceneImage42().getROILayer().isVisible();
    }

    @Override
    public void setROIOverlayEnabled(boolean enabled) {
        getSceneImage42().getROILayer().setVisible(enabled);
    }

    @Override
    public boolean isShapeOverlayEnabled() {
        return getSceneImage42().getFigureLayer().isVisible();
    }

    @Override
    public void setShapeOverlayEnabled(boolean enabled) {
        getSceneImage42().getFigureLayer().setVisible(enabled);
    }

    @Override
    public RenderedImage getROIImage() {
        return getSceneImage42().getROILayer().getImage();
    }

    @Override
    public void setROIImage(RenderedImage roiImage) {
        getSceneImage42().getROILayer().setImage(roiImage);
    }

    @Override
    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        getSceneImage42().getROILayer().updateImage(recreate, pm);
    }

    @Override
    public Figure getRasterROIShapeFigure() {
        return getSceneImage42().getROILayer().getRasterROIShapeFigure();
    }

    @Override
    public Figure getCurrentShapeFigure() {
        return getSceneImage42().getFigureLayer().getNumFigures() > 0 ? getSceneImage42().getFigureLayer().getFigureAt(0) : null;
    }

    @Override
    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        setShapeOverlayEnabled(true);
        final Figure oldShapeFigure = getCurrentShapeFigure();
        if (currentShapeFigure != oldShapeFigure) {
            if (oldShapeFigure != null) {
                getSceneImage42().getFigureLayer().removeFigure(oldShapeFigure);
            }
            if (currentShapeFigure != null) {
                getSceneImage42().getFigureLayer().addFigure(currentShapeFigure);
            }
        }
    }

    public void removeFigure(Figure figure) {
        getSceneImage42().getFigureLayer().removeFigure(figure);
    }

    public Figure[] getAllFigures() {
        return getSceneImage42().getFigureLayer().getAllFigures();
    }

    public Figure getFigureAt(int index) {
        return getSceneImage42().getFigureLayer().getFigureAt(index);
    }

    public Figure[] getFiguresWithAttribute(String name) {
        return getSceneImage42().getFigureLayer().getFiguresWithAttribute(name);
    }

    public Figure[] getFiguresWithAttribute(String name, Object value) {
        return getSceneImage42().getFigureLayer().getFiguresWithAttribute(name, value);
    }

    public int getNumFigures() {
        return getSceneImage42().getFigureLayer().getNumFigures();
    }

    public Figure[] getSelectedFigures() {
        return getSceneImage42().getFigureLayer().getSelectedFigures();
    }
}
