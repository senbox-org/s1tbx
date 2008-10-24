/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.glayer;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;


public class BitmaskCollectionLayer extends Layer {

    private RasterDataNode rasterDataNode;
    private final ProductNodeListener bitmaskDefListener;
    private final ProductNodeListener bitmaskOverlayInfoListener;
    private final AffineTransform i2mTransform;

    public BitmaskCollectionLayer(RasterDataNode rasterDataNode, AffineTransform i2mTransform) {
        this.rasterDataNode = rasterDataNode;
        this.i2mTransform = i2mTransform;
        setName("Bitmasks");
        final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();
        for (final BitmaskDef bitmaskDef : bitmaskDefs) {
            getChildren().add(createBitmaskLayer(bitmaskDef));
        }
        bitmaskDefListener = new BitmaskDefListener(this);
        getProduct().addProductNodeListener(bitmaskDefListener);
        bitmaskOverlayInfoListener = new BitmaskOverlayInfoListener(this);
        getProduct().addProductNodeListener(bitmaskOverlayInfoListener);
    }

    @Override
    public void disposeLayer() {
        if (rasterDataNode != null) {
            getProduct().removeProductNodeListener(bitmaskDefListener);
            getProduct().removeProductNodeListener(bitmaskOverlayInfoListener);
            rasterDataNode = null;
        }
    }

    private Product getProduct() {
        return rasterDataNode.getProduct();
    }

    private RasterDataNode getRaster() {
        return rasterDataNode;
    }

    private Layer createBitmaskLayer(final BitmaskDef bitmaskDef) {
        final Color color = bitmaskDef.getColor();
        final String expr = bitmaskDef.getExpr();

        final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(getProduct(), color, expr, false,
                i2mTransform);

        final Layer layer = new ImageLayer(multiLevelSource);
        layer.setName(bitmaskDef.getName());
        final BitmaskOverlayInfo overlayInfo = rasterDataNode.getBitmaskOverlayInfo();
        layer.setVisible(overlayInfo != null && overlayInfo.containsBitmaskDef(bitmaskDef));

        final Style style = new DefaultStyle();
        style.setOpacity(bitmaskDef.getAlpha());
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        layer.setStyle(style);

        return layer;
    }

    private class BitmaskDefListener implements ProductNodeListener {
        private final Layer bitmaskLayer;
        private final Map<BitmaskDef, Layer> layerMap;

        public BitmaskDefListener(Layer bitmaskLayer) {
            this.bitmaskLayer = bitmaskLayer;
            this.layerMap = new HashMap<BitmaskDef, Layer>();

            final Product product = getProduct();
            for (final Layer layer : bitmaskLayer.getChildren()) {
                layerMap.put(product.getBitmaskDef(layer.getName()), layer);
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef bitmaskDef = (BitmaskDef) sourceNode;
                final Layer layer = layerMap.remove(bitmaskDef);

                if (layer != null) {
                    final int index = bitmaskLayer.getChildren().indexOf(layer);
                    if (index != -1) {
                        final Layer newLayer = createBitmaskLayer(bitmaskDef);
                        final Layer oldLayer = bitmaskLayer.getChildren().remove(index);
                        bitmaskLayer.getChildren().add(index, newLayer);
                        layerMap.put(bitmaskDef, newLayer);
                        oldLayer.dispose();
                    }
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();

                for (int i = 0; i < bitmaskDefs.length; i++) {
                    if (sourceNode == bitmaskDefs[i]) {
                        final Layer layer = createBitmaskLayer(bitmaskDefs[i]);
                        bitmaskLayer.getChildren().add(i, layer);
                        layerMap.put(bitmaskDefs[i], layer);
                        break;
                    }
                }
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef bitmaskDef = (BitmaskDef) sourceNode;
                final Layer layer = layerMap.remove(bitmaskDef);
                if (layer != null) {
                    if (bitmaskLayer.getChildren().remove(layer)) {
                        layer.dispose();
                    }
                }
            }
        }
    }

    private class BitmaskOverlayInfoListener implements ProductNodeListener {
        private final Layer bitmaskLayer;

        private BitmaskOverlayInfoListener(Layer bitmaskLayer) {
            this.bitmaskLayer = bitmaskLayer;
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode == getRaster() &&
                    RasterDataNode.PROPERTY_NAME_BITMASK_OVERLAY_INFO.equals(event.getPropertyName())) {
                final BitmaskOverlayInfo overlayInfo = getRaster().getBitmaskOverlayInfo();
                final Product product = getProduct();

                for (final Layer layer : bitmaskLayer.getChildren()) {
                    layer.setVisible(overlayInfo.containsBitmaskDef(product.getBitmaskDef(layer.getName())));
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
        }
    }
}
