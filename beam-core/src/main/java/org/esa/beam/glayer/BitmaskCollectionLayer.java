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

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;


public class BitmaskCollectionLayer extends CollectionLayer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    private final ProductNodeListener bitmaskDefListener;
    private final ProductNodeListener bitmaskOverlayInfoListener;
    private final AffineTransform i2mTransform;

    private RasterDataNode rasterDataNode;

    public BitmaskCollectionLayer(RasterDataNode rasterDataNode, AffineTransform i2mTransform) {
        this(LAYER_TYPE, rasterDataNode, i2mTransform);
    }

    protected BitmaskCollectionLayer(Type type, RasterDataNode rasterDataNode, AffineTransform i2mTransform) {
        super(type, "Bitmask collection");
        this.rasterDataNode = rasterDataNode;
        this.i2mTransform = i2mTransform;
        setName("Bitmasks");
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
        final LayerType type = LayerType.getLayerType(BitmaskLayerType.class.getName());
        final ValueContainer configuration = type.getConfigurationTemplate();
        try {
            configuration.setValue(BitmaskLayerType.PROPERTY_BITMASKDEF, bitmaskDef);
            configuration.setValue(BitmaskLayerType.PROPERTY_PRODUCT, getProduct());
            configuration.setValue(BitmaskLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2mTransform);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
        final Layer layer = type.createLayer(null, configuration);
        final BitmaskOverlayInfo overlayInfo = getRaster().getBitmaskOverlayInfo();
        layer.setVisible(overlayInfo != null && overlayInfo.containsBitmaskDef(bitmaskDef));

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

    public static class Type extends CollectionLayer.Type {

        public static final String BITMASK_LAYER_ID = "org.esa.beam.layers.bitmask";

        public static final String PROPERTY_RASTER = "bitmaskCollection.raster";
        public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "bitmaskCollection.i2mTransform";

        @Override
        public String getName() {
            return "Bitmask Collection Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            RasterDataNode rasterDataNode = (RasterDataNode) configuration.getValue(PROPERTY_RASTER);
            AffineTransform i2m = (AffineTransform) configuration.getValue(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
            final BitmaskCollectionLayer bitmaskCollectionLayer = new BitmaskCollectionLayer(rasterDataNode, i2m);
            bitmaskCollectionLayer.setId(BITMASK_LAYER_ID);
            final BitmaskDef[] bitmaskDefs = rasterDataNode.getProduct().getBitmaskDefs();
            final LayerType bitmaskLayerType = LayerType.getLayerType(BitmaskLayerType.class.getName());
            for (final BitmaskDef bitmaskDef : bitmaskDefs) {
                final ValueContainer template = bitmaskLayerType.getConfigurationTemplate();
                try {
                    template.setValue(BitmaskLayerType.PROPERTY_BITMASKDEF, bitmaskDef);
                    template.setValue(BitmaskLayerType.PROPERTY_PRODUCT, rasterDataNode.getProduct());
                    template.setValue(BitmaskLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2m);
                } catch (ValidationException e) {
                    throw new IllegalArgumentException(e);
                }

                final Layer layer = bitmaskLayerType.createLayer(ctx, template);
                final BitmaskOverlayInfo overlayInfo = rasterDataNode.getBitmaskOverlayInfo();
                layer.setVisible(overlayInfo != null && overlayInfo.containsBitmaskDef(bitmaskDef));
                bitmaskCollectionLayer.getChildren().add(layer);
            }

            return bitmaskCollectionLayer;
        }

        @Override
        public ValueContainer getConfigurationTemplate() {
            final ValueContainer template = super.getConfigurationTemplate();

            template.addModel(createDefaultValueModel(PROPERTY_RASTER, RasterDataNode.class));
            template.getDescriptor(PROPERTY_RASTER).setNotNull(true);

            template.addModel(createDefaultValueModel(PROPERTY_IMAGE_TO_MODEL_TRANSFORM, AffineTransform.class));
            template.getDescriptor(PROPERTY_IMAGE_TO_MODEL_TRANSFORM).setNotNull(true);

            return template;

        }
    }
}
