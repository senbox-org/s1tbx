package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.swing.LayerCanvas;

import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.*;

import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.PixelInfoFactory;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.util.PropertyMap;

import javax.swing.*;

class ProductSceneView45 extends ProductSceneView {

    LayerCanvas layerCanvas; 

    ProductSceneView45(ProductSceneImage45 sceneImage) {
        super(sceneImage);
        // layerCanvas = new LayerCanvas(sceneImage.getLayer());

    }

    public boolean isNoDataOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public boolean isGraticuleOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public boolean isPinOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setPinOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public boolean isGcpOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public boolean isROIOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setROIOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public boolean isShapeOverlayEnabled() {
        return false;  // todo - implement me!
    }

    public void setShapeOverlayEnabled(boolean enabled) {
        // todo - implement me!
    }

    public RenderedImage getROIImage() {
        return null;  // todo - implement me!
    }

    public void setROIImage(RenderedImage roiImage) {
        // todo - implement me!
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws Exception {
        // todo - implement me!
    }

    public Figure getRasterROIShapeFigure() {
        return null;  // todo - implement me!
    }

    public Figure getCurrentShapeFigure() {
        return null;  // todo - implement me!
    }

    public void setCurrentShapeFigure(Figure currentShapeFigure) {
        // todo - implement me!
    }

    public void setLayerProperties(PropertyMap propertyMap) {
        // todo - implement me!
    }

    public void addPixelPositionListener(PixelPositionListener listener) {
        // todo - implement me!
    }

    public void removePixelPositionListener(PixelPositionListener listener) {
        // todo - implement me!
    }

    public AbstractTool[] getSelectToolDelegates() {
        return new AbstractTool[0];  // todo - implement me!
    }

    public void disposeLayerModel() {
        // todo - implement me!
    }

    public AffineTransform getBaseImageToViewTransform() {
        return null;  // todo - implement me!
    }

    public Rectangle2D getVisibleModelBounds() {
        return null;  // todo - implement me!
    }

    public double getViewScale() {
        return 0;  // todo - implement me!
    }

    public Rectangle2D getModelBounds() {
        return null;  // todo - implement me!
    }

    public JComponent getImageDisplayComponent() {
        return null;  // todo - implement me!
    }

    public void zoom(Rectangle rect) {
        // todo - implement me!
    }

    public void zoom(double x, double y, double viewScale) {
        // todo - implement me!
    }

    public void zoom(double viewScale) {
        // todo - implement me!
    }

    public void zoomAll() {
        // todo - implement me!
    }

    @Deprecated
    public void setModelOffset(double modelOffsetX, double modelOffsetY) {
        // todo - implement me!
    }

    public void synchronizeViewport(ProductSceneView view) {
        // todo - implement me!
    }

    public RenderedImage getBaseImage() {
        return null;  // todo - implement me!
    }

    public void setBaseImage(RenderedImage baseImage) {
        // todo - implement me!
    }

    public int getBaseImageWidth() {
        return 0;  // todo - implement me!
    }

    public int getBaseImageHeight() {
        return 0;  // todo - implement me!
    }

    public RenderedImage createSnapshotImage(boolean entireImage, boolean useAlpha) {
        return null;  // todo - implement me!
    }

    public Tool getTool() {
        return null;  // todo - implement me!
    }

    public void setTool(Tool tool) {
        // todo - implement me!
    }

    public void repaintTool() {
        // todo - implement me!
    }

    public void removeFigure(Figure figure) {
        // todo - implement me!
    }

    public int getNumFigures() {
        return 0;  // todo - implement me!
    }

    public Figure getFigureAt(int index) {
        return null;  // todo - implement me!
    }

    public Figure[] getAllFigures() {
        return new Figure[0];  // todo - implement me!
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

    protected void copyPixelInfoStringToClipboard() {
        // todo - implement me!
    }

    protected void disposeImageDisplayComponent() {
        // todo - implement me!
    }

    protected PixelInfoFactory getPixelInfoFactory() {
        return null;  // todo - implement me!
    }

    protected void setPixelInfoFactory(PixelInfoFactory pixelInfoFactory) {
        // todo - implement me!
    }

    public RenderedImage updateNoDataImage(ProgressMonitor pm) throws Exception {
        return null;  // todo - implement me!
    }
}
