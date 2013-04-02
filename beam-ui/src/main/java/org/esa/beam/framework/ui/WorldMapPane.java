/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glayer.swing.WakefulComponent;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class displays a world map specified by the {@link WorldMapPaneDataModel}.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapPane extends JPanel {

    private LayerCanvas layerCanvas;
    private Layer worldMapLayer;
    private final WorldMapPaneDataModel dataModel;
    private boolean navControlShown;
    private WakefulComponent navControlWrapper;
    private PanSupport panSupport;
    private MouseHandler mouseHandler;
    private Set<ZoomListener> zoomListeners;

    public WorldMapPane(WorldMapPaneDataModel dataModel) {
        this(dataModel, null);
    }

    public WorldMapPane(WorldMapPaneDataModel dataModel, LayerCanvas.Overlay overlay) {
        this.dataModel = dataModel;
        layerCanvas = new LayerCanvas();
        this.panSupport = new DefaultPanSupport(layerCanvas);
        this.zoomListeners = new HashSet<ZoomListener>();
        getLayerCanvas().getModel().getViewport().setModelYAxisDown(false);
        if (overlay == null) {
            getLayerCanvas().addOverlay(new BoundaryOverlayImpl(dataModel));
        } else {
            getLayerCanvas().addOverlay(overlay);
        }
        final Layer rootLayer = getLayerCanvas().getLayer();

        final Dimension dimension = new Dimension(400, 200);
        final Viewport viewport = getLayerCanvas().getViewport();
        viewport.setViewBounds(new Rectangle(dimension));

        setPreferredSize(dimension);
        setSize(dimension);
        setLayout(new BorderLayout());
        add(getLayerCanvas(), BorderLayout.CENTER);

        dataModel.addModelChangeListener(new ModelChangeListener());

        worldMapLayer = dataModel.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
        installLayerCanvasNavigation(getLayerCanvas());
        getLayerCanvas().getLayer().getChildren().add(worldMapLayer);
        zoomAll();
        setNavControlVisible(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                AffineTransform transform = getLayerCanvas().getViewport().getModelToViewTransform();

                double minX = getLayerCanvas().getMaxVisibleModelBounds().getMinX();
                double minY = getLayerCanvas().getMaxVisibleModelBounds().getMinY();
                double maxX = getLayerCanvas().getMaxVisibleModelBounds().getMaxX();
                double maxY = getLayerCanvas().getMaxVisibleModelBounds().getMaxY();

                final Point2D upperLeft = transform.transform(new Point2D.Double(minX, minY), null);
                final Point2D lowerRight = transform.transform(new Point2D.Double(maxX, maxY), null);
                /*
                * We need to give the borders a minimum width/height of 1 because otherwise the intersection
                * operation would not work
                */
                Rectangle2D northBorder = new Rectangle2D.Double(upperLeft.getX(), upperLeft.getY(),
                                                                 lowerRight.getX() - upperLeft.getX(), 1);
                Rectangle2D southBorder = new Rectangle2D.Double(upperLeft.getX(), lowerRight.getY(),
                                                                 lowerRight.getX() - upperLeft.getX(), 1);
                Rectangle2D westBorder = new Rectangle2D.Double(upperLeft.getX(), lowerRight.getY(), 1,
                                                                upperLeft.getY() - lowerRight.getY());
                Rectangle2D eastBorder = new Rectangle2D.Double(lowerRight.getX(), lowerRight.getY(), 1,
                                                                upperLeft.getY() - lowerRight.getY());

                boolean isWorldMapFullyVisible = getLayerCanvas().getBounds().intersects(northBorder) ||
                                                 getLayerCanvas().getBounds().intersects(southBorder) ||
                                                 getLayerCanvas().getBounds().intersects(westBorder) ||
                                                 getLayerCanvas().getBounds().intersects(eastBorder);
                if (isWorldMapFullyVisible) {
                    zoomAll();
                }
            }
        });
    }

    @Override
    public void doLayout() {
        if (navControlShown && navControlWrapper != null) {
            navControlWrapper.setLocation(getWidth() - navControlWrapper.getWidth() - 4, 4);
        }
        super.doLayout();
    }

    public Product getSelectedProduct() {
        return dataModel.getSelectedProduct();
    }

    public Product[] getProducts() {
        return dataModel.getProducts();
    }

    public float getScale() {
        return (float) getLayerCanvas().getViewport().getZoomFactor();
    }

    public void zoomToProduct(Product product) {
        if (product == null || product.getGeoCoding() == null) {
            return;
        }
        final GeneralPath[] generalPaths = getGeoBoundaryPaths(product);
        Rectangle2D modelArea = new Rectangle2D.Double();
        final Viewport viewport = getLayerCanvas().getViewport();
        for (GeneralPath generalPath : generalPaths) {
            final Rectangle2D rectangle2D = generalPath.getBounds2D();
            if (modelArea.isEmpty()) {
                if (!viewport.isModelYAxisDown()) {
                    modelArea.setFrame(rectangle2D.getX(), rectangle2D.getMaxY(),
                                       rectangle2D.getWidth(), rectangle2D.getHeight());
                }
                modelArea = rectangle2D;
            } else {
                modelArea.add(rectangle2D);
            }
        }
        Rectangle2D modelBounds = modelArea.getBounds2D();
        modelBounds.setFrame(modelBounds.getX() - 2, modelBounds.getY() - 2,
                             modelBounds.getWidth() + 4, modelBounds.getHeight() + 4);

        modelBounds = cropToMaxModelBounds(modelBounds);

        viewport.zoom(modelBounds);
        fireScrolled();
    }

    public void zoomAll() {
        getLayerCanvas().getViewport().zoom(worldMapLayer.getModelBounds());
        fireScrolled();
    }

    /**
     * None API. Don't use this method!
     *
     * @param navControlShown true, if this canvas uses a navigation control.
     */
    public void setNavControlVisible(boolean navControlShown) {
        boolean oldValue = this.navControlShown;
        if (oldValue != navControlShown) {
            if (navControlShown) {
                final Action[] overlayActions = getOverlayActions();
                final ButtonOverlayControl navControl = new ButtonOverlayControl(overlayActions.length, overlayActions);
                navControlWrapper = new WakefulComponent(navControl);
                navControlWrapper.setMinAlpha(0.3f);
                getLayerCanvas().add(navControlWrapper);
            } else {
                getLayerCanvas().remove(navControlWrapper);
                navControlWrapper = null;
            }
            validate();
            this.navControlShown = navControlShown;
        }
    }

    public void setPanSupport(PanSupport panSupport) {
        layerCanvas.removeMouseListener(mouseHandler);
        layerCanvas.removeMouseMotionListener(mouseHandler);

        this.panSupport = panSupport;
        mouseHandler = new MouseHandler();
        layerCanvas.addMouseListener(mouseHandler);
        layerCanvas.addMouseMotionListener(mouseHandler);
    }

    public LayerCanvas getLayerCanvas() {
        return layerCanvas;
    }

    static GeneralPath[] getGeoBoundaryPaths(Product product) {
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        return ProductUtils.createGeoBoundaryPaths(product, null, step);
    }

    public boolean addZoomListener(ZoomListener zoomListener) {
        return zoomListeners.add(zoomListener);
    }

    public boolean removeZoomListener(ZoomListener zoomListener) {
        return zoomListeners.remove(zoomListener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        navControlWrapper.setEnabled(enabled);
    }

    protected Action[] getOverlayActions() {
        return new Action[]{new ZoomAllAction(), new ZoomToSelectedAction()};
    }

    private void fireScrolled() {
        for (ZoomListener zoomListener : zoomListeners) {
            zoomListener.zoomed();
        }
    }

    private void updateUiState(PropertyChangeEvent evt) {
        if (WorldMapPaneDataModel.PROPERTY_LAYER.equals(evt.getPropertyName())) {
            exchangeWorldMapLayer();
        }
        if (WorldMapPaneDataModel.PROPERTY_PRODUCTS.equals(evt.getPropertyName())) {
            repaint();
        }
        if (WorldMapPaneDataModel.PROPERTY_SELECTED_PRODUCT.equals(evt.getPropertyName()) ||
            WorldMapPaneDataModel.PROPERTY_AUTO_ZOOM_ENABLED.equals(evt.getPropertyName())) {
            final Product selectedProduct = dataModel.getSelectedProduct();
            if (selectedProduct != null && dataModel.isAutoZoomEnabled()) {
                zoomToProduct(selectedProduct);
            } else {
                repaint();
            }
        }
        if (WorldMapPaneDataModel.PROPERTY_ADDITIONAL_GEO_BOUNDARIES.equals(evt.getPropertyName())) {
            repaint();
        }
    }

    private void exchangeWorldMapLayer() {
        final List<Layer> children = getLayerCanvas().getLayer().getChildren();
        for (Layer child : children) {
            child.dispose();
        }
        children.clear();
        final Layer rootLayer = getLayerCanvas().getLayer();
        worldMapLayer = dataModel.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
        children.add(worldMapLayer);
        zoomAll();
    }

    private Rectangle2D cropToMaxModelBounds(Rectangle2D modelBounds) {
        final Rectangle2D maxModelBounds = worldMapLayer.getModelBounds();
        if (modelBounds.getWidth() >= maxModelBounds.getWidth() - 1 ||
            modelBounds.getHeight() >= maxModelBounds.getHeight() - 1) {
            modelBounds = maxModelBounds;
        }
        return modelBounds;
    }

    private void installLayerCanvasNavigation(LayerCanvas layerCanvas) {
        mouseHandler = new MouseHandler();
        layerCanvas.addMouseListener(mouseHandler);
        layerCanvas.addMouseMotionListener(mouseHandler);
        layerCanvas.addMouseWheelListener(mouseHandler);
    }

    private static boolean viewportIsInWorldMapBounds(double dx, double dy, LayerCanvas layerCanvas) {
        AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();

        double minX = layerCanvas.getMaxVisibleModelBounds().getMinX();
        double minY = layerCanvas.getMaxVisibleModelBounds().getMinY();
        double maxX = layerCanvas.getMaxVisibleModelBounds().getMaxX();
        double maxY = layerCanvas.getMaxVisibleModelBounds().getMaxY();

        final Point2D upperLeft = transform.transform(new Point2D.Double(minX, minY), null);
        final Point2D lowerRight = transform.transform(new Point2D.Double(maxX, maxY), null);
        /*
        * We need to give the borders a minimum width/height of 1 because otherwise the intersection
        * operation would not work
        */
        Rectangle2D northBorder = new Rectangle2D.Double(upperLeft.getX() + dx, upperLeft.getY() + dy,
                                                         lowerRight.getX() + dx - upperLeft.getX() + dx, 1);
        Rectangle2D southBorder = new Rectangle2D.Double(upperLeft.getX() + dx, lowerRight.getY() + dy,
                                                         lowerRight.getX() + dx - upperLeft.getX() + dx, 1);
        Rectangle2D westBorder = new Rectangle2D.Double(upperLeft.getX() + dx, lowerRight.getY() + dy, 1,
                                                        upperLeft.getY() + dy - lowerRight.getY() + dy);
        Rectangle2D eastBorder = new Rectangle2D.Double(lowerRight.getX() + dx, lowerRight.getY() + dy, 1,
                                                        upperLeft.getY() + dy - lowerRight.getY() + dy);
        return (!layerCanvas.getBounds().intersects(northBorder) &&
                !layerCanvas.getBounds().intersects(southBorder) &&
                !layerCanvas.getBounds().intersects(westBorder) &&
                !layerCanvas.getBounds().intersects(eastBorder));
    }

    private class ModelChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateUiState(evt);
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            panSupport.panStarted(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            panSupport.performPan(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            panSupport.panStopped(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!isEnabled()) {
                return;
            }
            double oldFactor = layerCanvas.getViewport().getZoomFactor();
            final int wheelRotation = e.getWheelRotation();
            final double newZoomFactor = layerCanvas.getViewport().getZoomFactor() * Math.pow(1.1, wheelRotation);
            final Rectangle viewBounds = layerCanvas.getViewport().getViewBounds();
            final Rectangle2D modelBounds = worldMapLayer.getModelBounds();
            final double minZoomFactor = Math.min(viewBounds.getWidth() / modelBounds.getWidth(),
                                                  viewBounds.getHeight() / modelBounds.getHeight());
            layerCanvas.getViewport().setZoomFactor(Math.max(newZoomFactor, minZoomFactor));

            if (layerCanvas.getViewport().getZoomFactor() > oldFactor
                || viewportIsInWorldMapBounds(0, 0, layerCanvas)) {
                fireScrolled();
                return;
            }
            layerCanvas.getViewport().setZoomFactor(oldFactor);
        }
    }


    private static class WorldMapLayerContext implements LayerContext {

        private final Layer rootLayer;

        private WorldMapLayerContext(Layer rootLayer) {
            this.rootLayer = rootLayer;
        }

        @Override
        public Object getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public Layer getRootLayer() {
            return rootLayer;
        }
    }

    private class ZoomAllAction extends AbstractAction {

        private ZoomAllAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/view-fullscreen.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isEnabled()) {
                zoomAll();
            }
        }
    }

    private class ZoomToSelectedAction extends AbstractAction {

        private ZoomToSelectedAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/ZoomTo24.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isEnabled()) {
                final Product selectedProduct = getSelectedProduct();
                zoomToProduct(selectedProduct);
            }
        }
    }

    public interface ZoomListener {

        void zoomed();
    }

    public interface PanSupport {

        void panStarted(MouseEvent event);

        void performPan(MouseEvent event);

        void panStopped(MouseEvent event);
    }

    protected static class DefaultPanSupport implements PanSupport {

        private Point p0;

        private final LayerCanvas layerCanvas;

        protected DefaultPanSupport(LayerCanvas layerCanvas) {
            this.layerCanvas = layerCanvas;
        }

        @Override
        public void panStarted(MouseEvent event) {
            p0 = event.getPoint();
        }

        @Override
        public void performPan(MouseEvent event) {
            final Point p = event.getPoint();
            final double dx = p.x - p0.x;
            final double dy = p.y - p0.y;

            if (viewportIsInWorldMapBounds(dx, dy, layerCanvas)) {
                layerCanvas.getViewport().moveViewDelta(dx, dy);
            }
            p0 = p;
        }

        @Override
        public void panStopped(MouseEvent event) {
        }
    }

    /**
     * Set the worldmap's scale.
     *
     * @param scale the scale.
     *
     * @deprecated since 4.10.1, use layer canvas for zooming instead
     */
    @Deprecated
    public void setScale(final float scale) {
        if (getScale() != scale && scale > 0) {
            final float oldValue = getScale();
            getLayerCanvas().getViewport().setZoomFactor(scale);
            firePropertyChange("scale", oldValue, scale);
        }
    }
}
