/*
 * $Id: WorldMapPane.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
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
package org.esa.beam.framework.ui;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * This class displays a world map specified by the {@link WorldMapPaneModel}.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public final class WorldMapPane extends JPanel {

    private LayerCanvas layerCanvas;
    private Layer worldMapLayer;
    private final WorldMapPaneModel model;

    public WorldMapPane(WorldMapPaneModel model) {
        this.model = model;
        layerCanvas = new LayerCanvas();
        layerCanvas.getModel().getViewport().setModelYAxisDown(false);
        installLayerCanvasNavigation(layerCanvas);
        layerCanvas.addOverlay(new BoundaryOverlay());
        final Layer rootLayer = layerCanvas.getLayer();

        final Dimension dimension = new Dimension(400, 200);
        final Viewport viewport = layerCanvas.getViewport();
        viewport.setViewBounds(new Rectangle(dimension));

        setPreferredSize(dimension);
        setSize(dimension);
        setLayout(new BorderLayout());
        add(layerCanvas, BorderLayout.CENTER);

        model.addModelChangeListener(new ModelChangeListener());

        worldMapLayer = model.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
        layerCanvas.getLayer().getChildren().add(worldMapLayer);
        layerCanvas.getViewport().zoom(worldMapLayer.getModelBounds());
    }

    public Product getSelectedProduct() {
        return model.getSelectedProduct();
    }

    public Product[] getProducts() {
        return model.getProducts();
    }

    public float getScale() {
        return new Float(layerCanvas.getViewport().getZoomFactor());
    }

    public void setScale(final float scale) {
        if (getScale() != scale) {
            final float oldValue = getScale();
            layerCanvas.getViewport().setZoomFactor(scale);
            firePropertyChange("scale", oldValue, scale);
        }
    }

    public void zoomToProduct(Product product) {
        if (product.getGeoCoding() == null) {
            return;
        }
        final GeneralPath[] generalPaths = getGeoBoundaryPaths(product);
        Rectangle2D modelArea = new Rectangle2D.Double();
        final Viewport viewport = layerCanvas.getViewport();
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
    }

    private void updateUiState(PropertyChangeEvent evt) {
        if (WorldMapPaneModel.PROPERTY_LAYER.equals(evt.getPropertyName())) {
            exchangeWorldMapLayer();
        }
        if (WorldMapPaneModel.PROPERTY_PRODUCTS.equals(evt.getPropertyName())) {
            repaint();
        }
        if (WorldMapPaneModel.PROPERTY_SELECTED_PRODUCT.equals(evt.getPropertyName())) {
            final Product selectedProduct = model.getSelectedProduct();
            if (selectedProduct != null) {
                zoomToProduct(selectedProduct);
            }
        }
        if (WorldMapPaneModel.PROPERTY_ADDITIONAL_GEO_BOUNDARIES.equals(evt.getPropertyName())) {
            repaint();
        }
    }


    private void exchangeWorldMapLayer() {
        final List<Layer> children = layerCanvas.getLayer().getChildren();
        for (Layer child : children) {
            child.dispose();
        }
        children.clear();
        final Layer rootLayer = layerCanvas.getLayer();
        worldMapLayer = model.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
        children.add(worldMapLayer);
        layerCanvas.getViewport().zoom(worldMapLayer.getModelBounds());
    }

    private Rectangle2D cropToMaxModelBounds(Rectangle2D modelBounds) {
        final Rectangle2D maxModelBounds = worldMapLayer.getModelBounds();
        if (modelBounds.getWidth() >= maxModelBounds.getWidth() - 1 ||
            modelBounds.getHeight() >= maxModelBounds.getHeight() - 1) {
            modelBounds = maxModelBounds;
        }
        return modelBounds;
    }


    private GeneralPath[] getGeoBoundaryPaths(Product product) {
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        return ProductUtils.createGeoBoundaryPaths(product, null, step);
    }

    private PixelPos getProductCenter(final Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        PixelPos centerPos = null;
        if (geoCoding != null) {
            final float pixelX = (float) Math.floor(0.5f * product.getSceneRasterWidth()) + 0.5f;
            final float pixelY = (float) Math.floor(0.5f * product.getSceneRasterHeight()) + 0.5f;
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(pixelX, pixelY), null);
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            final Point2D point2D = transform.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), null);
            centerPos = new PixelPos((float) point2D.getX(), (float) point2D.getY());
        }
        return centerPos;
    }

    private static void installLayerCanvasNavigation(LayerCanvas layerCanvas) {
        final MouseHandler mouseHandler = new MouseHandler(layerCanvas);
        layerCanvas.addMouseListener(mouseHandler);
        layerCanvas.addMouseMotionListener(mouseHandler);
        layerCanvas.addMouseWheelListener(mouseHandler);
    }

    /**
     * @param image is ignored
     *
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    @SuppressWarnings({"UnusedDeclaration"})
    public WorldMapPane(final java.awt.image.BufferedImage image) {
        this(new DefaultWorldMapPaneModel());
    }

    /**
     * @param bufferedImage is ignored
     *
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    @SuppressWarnings({"UnusedDeclaration"})
    public void setWorldMapImage(java.awt.image.BufferedImage bufferedImage) {
    }

    /**
     * @return the dimension of this {@link WorldMapPane}
     *
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    public Dimension getCurrentimageSize() {
        return getSize();
    }

    /**
     * @param product the selected product
     *
     * @deprecated since BEAM 4.7, use the {@link WorldMapPaneModel} model provided
     *             to this {@link WorldMapPane} via the constructor.
     */
    @Deprecated
    public void setSelectedProduct(final Product product) {
        final Product oldSelectedProduct = model.getSelectedProduct();
        if (oldSelectedProduct != product) {
            model.setSelectedProduct(product);
            firePropertyChange("product", oldSelectedProduct, product);
        }
    }

    /**
     * @param products the products
     *
     * @deprecated since BEAM 4.7, use the {@link WorldMapPaneModel#setProducts(Product[])} model provided
     *             to this {@link WorldMapPane} via the constructor.
     */
    @Deprecated
    public void setProducts(final Product[] products) {
        final Product[] oldProducts = model.getProducts();
        if (oldProducts != products) {
            model.setProducts(products);
            firePropertyChange("products", oldProducts, products);
        }
    }

    /**
     * @param geoBoundaries additional geo-boundaries
     *
     * @deprecated since BEAM 4.7, use the {@link WorldMapPaneModel#setAdditionalGeoBoundaries(GeoPos[][])} model
     *             provided to this {@link WorldMapPane} via the constructor.
     */
    @Deprecated
    public void setPathesToDisplay(final GeoPos[][] geoBoundaries) {
        final GeoPos[][] oldAdditionalGeoBoundaries = model.getAdditionalGeoBoundaries();
        if (oldAdditionalGeoBoundaries != geoBoundaries) {
            model.setAdditionalGeoBoundaries(geoBoundaries);
            firePropertyChange("extraGeoBoundaries", oldAdditionalGeoBoundaries, oldAdditionalGeoBoundaries);
        }
    }

    /**
     * @return the center {@link PixelPos pixel position} of the
     *         currently selected product
     *
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    public PixelPos getCurrentProductCenter() {
        if (model.getSelectedProduct() == null) {
            return null;
        }
        return getProductCenter(model.getSelectedProduct());
    }

    private class ModelChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateUiState(evt);
        }
    }

    public static class MouseHandler extends MouseInputAdapter {

        private LayerCanvas layerCanvas;
        private Point p0;

        private MouseHandler(LayerCanvas layerCanvas) {
            this.layerCanvas = layerCanvas;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            p0 = e.getPoint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final Point p = e.getPoint();
            final double dx = p.x - p0.x;
            final double dy = p.y - p0.y;
            layerCanvas.getViewport().moveViewDelta(dx, dy);
            p0 = p;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            final int wheelRotation = e.getWheelRotation();
            final double newZoomFactor = layerCanvas.getViewport().getZoomFactor() * Math.pow(1.1, wheelRotation);
            layerCanvas.getViewport().setZoomFactor(newZoomFactor);
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

    private class BoundaryOverlay implements LayerCanvas.Overlay {

        @Override
        public void paintOverlay(LayerCanvas canvas, Graphics2D graphics) {
            for (final GeoPos[] extraGeoBoundary : model.getAdditionalGeoBoundaries()) {
                drawGeoBoundary(graphics, extraGeoBoundary, false, null, null);
            }

            final Product selectedProduct = model.getSelectedProduct();
            for (final Product product : model.getProducts()) {
                if (selectedProduct != product) {
                    drawProduct(graphics, product, false);
                }
            }

            if (selectedProduct != null) {
                drawProduct(graphics, selectedProduct, true);
            }

        }

        private void drawProduct(final Graphics2D g2d, final Product product, final boolean isCurrent) {
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding == null) {
                return;
            }

            GeneralPath[] boundaryPaths = getGeoBoundaryPaths(product);
            final String text = String.valueOf(product.getRefNo());
            final PixelPos textCenter = getProductCenter(product);
            drawGeoBoundary(g2d, boundaryPaths, isCurrent, text, textCenter);
        }

        private void drawGeoBoundary(final Graphics2D g2d, final GeneralPath[] boundaryPaths, final boolean isCurrent,
                                     final String text, final PixelPos textCenter) {
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            for (GeneralPath boundaryPath : boundaryPaths) {
                boundaryPath.transform(transform);
                drawPath(isCurrent, g2d, boundaryPath, 0.0f);
            }

            drawText(g2d, text, textCenter, 0.0f);

        }

        private void drawGeoBoundary(final Graphics2D g2d, final GeoPos[] geoBoundary, final boolean isCurrent,
                                     final String text, final PixelPos textCenter) {
            final GeneralPath gp = convertToPixelPath(geoBoundary);
            drawPath(isCurrent, g2d, gp, 0.0f);
            drawText(g2d, text, textCenter, 0.0f);
        }

        private void drawPath(final boolean isCurrent, Graphics2D g2d, final GeneralPath gp, final float offsetX) {
            g2d = prepareGraphics2D(offsetX, g2d);
            if (isCurrent) {
                g2d.setColor(new Color(255, 200, 200, 70));
            } else {
                g2d.setColor(new Color(255, 255, 255, 70));
            }
            g2d.fill(gp);
            if (isCurrent) {
                g2d.setColor(new Color(255, 0, 0));
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.draw(gp);
        }

        private GeneralPath convertToPixelPath(final GeoPos[] geoBoundary) {
            final GeneralPath gp = new GeneralPath();
            for (int i = 0; i < geoBoundary.length; i++) {
                final GeoPos geoPos = geoBoundary[i];
                final AffineTransform m2vTransform = layerCanvas.getViewport().getModelToViewTransform();
                final Point2D viewPos = m2vTransform.transform(new PixelPos.Double(geoPos.lon, geoPos.lat), null);
                if (i == 0) {
                    gp.moveTo(viewPos.getX(), viewPos.getY());
                } else {
                    gp.lineTo(viewPos.getX(), viewPos.getY());
                }
            }
            gp.closePath();
            return gp;
        }

        private void drawText(Graphics2D g2d, final String text, final PixelPos textCenter, final float offsetX) {
            if (text == null || textCenter == null) {
                return;
            }
            g2d = prepareGraphics2D(offsetX, g2d);
            final FontMetrics fontMetrics = g2d.getFontMetrics();
            final Color color = g2d.getColor();
            g2d.setColor(Color.black);

            g2d.drawString(text,
                           textCenter.x - fontMetrics.stringWidth(text) / 2.0f,
                           textCenter.y + fontMetrics.getAscent() / 2.0f);
            g2d.setColor(color);
        }

        private Graphics2D prepareGraphics2D(final float offsetX, Graphics2D g2d) {
            if (offsetX != 0.0f) {
                g2d = (Graphics2D) g2d.create();
                final AffineTransform transform = g2d.getTransform();
                final AffineTransform offsetTrans = new AffineTransform();
                offsetTrans.setToTranslation(+offsetX, 0);
                transform.concatenate(offsetTrans);
                g2d.setTransform(transform);
            }
            return g2d;
        }

    }

}
