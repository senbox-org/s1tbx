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
package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.geom.AffineTransform;
import java.util.List;


/**
 * @deprecated since BEAM 4.7, replaced by MaskCollectionLayerType
 */
@Deprecated
public class BitmaskCollectionLayer extends CollectionLayer {


    private final ProductNodeListener bitmaskDefListener;

    private RasterDataNode rasterDataNode;

    public BitmaskCollectionLayer(Type layerType, PropertySet configuration) {
        super(layerType, configuration, "Bitmasks");
        this.rasterDataNode = (RasterDataNode) configuration.getValue(Type.PROPERTY_NAME_RASTER);
        bitmaskDefListener = new BitmaskDefListener(this);
        getProduct().addProductNodeListener(bitmaskDefListener);
    }

    @Override
    public void disposeLayer() {
        if (rasterDataNode != null) {
            getProduct().removeProductNodeListener(bitmaskDefListener);
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
        return BitmaskLayerType.createBitmaskLayer(getRaster(), bitmaskDef, null);
    }

    public static class BitmaskDefListener implements ProductNodeListener {

        private final BitmaskCollectionLayer bitmaskCollectionLayer;

        private BitmaskDefListener(BitmaskCollectionLayer bitmaskCollectionLayer) {
            this.bitmaskCollectionLayer = bitmaskCollectionLayer;
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef bitmaskDef = (BitmaskDef) sourceNode;
                final Layer oldLayer = getLayerForBitmask(bitmaskDef);

                if (oldLayer != null) {
                    final int index = bitmaskCollectionLayer.getChildren().indexOf(oldLayer);
                    bitmaskCollectionLayer.getChildren().remove(oldLayer);
                    final Layer newLayer = bitmaskCollectionLayer.createBitmaskLayer(bitmaskDef);
                    bitmaskCollectionLayer.getChildren().add(index, newLayer);
                    oldLayer.dispose();
                }
            } else if (sourceNode == bitmaskCollectionLayer.getRaster() &&
                       RasterDataNode.PROPERTY_NAME_BITMASK_OVERLAY_INFO.equals(event.getPropertyName())) {
                final BitmaskOverlayInfo overlayInfo = bitmaskCollectionLayer.getRaster().getBitmaskOverlayInfo();
                final Product product = bitmaskCollectionLayer.getProduct();

                for (final Layer layer : bitmaskCollectionLayer.getChildren()) {
                    layer.setVisible(overlayInfo.containsBitmaskDef(product.getBitmaskDef(layer.getName())));
                }
            }
        }

        private Layer getLayerForBitmask(BitmaskDef bitmaskDef) {
            final List<Layer> list = bitmaskCollectionLayer.getChildren();
            for (Layer layer : list) {
                final Object value = layer.getConfiguration().getValue(BitmaskLayerType.PROPERTY_NAME_BITMASK_DEF);
                if (bitmaskDef.equals(value)) {
                    return layer;
                }
            }
            return null;
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef[] bitmaskDefs = bitmaskCollectionLayer.getProduct().getBitmaskDefs();

                for (int i = 0; i < bitmaskDefs.length; i++) {
                    if (sourceNode == bitmaskDefs[i]) {
                        final Layer layer = bitmaskCollectionLayer.createBitmaskLayer(bitmaskDefs[i]);
                        bitmaskCollectionLayer.getChildren().add(i, layer);
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
                final Layer layer = getLayerForBitmask(bitmaskDef);
                if (layer != null) {
                    if (bitmaskCollectionLayer.getChildren().remove(layer)) {
                        layer.dispose();
                    }
                }
            }
        }
    }

    public static class Type extends CollectionLayer.Type {

        public static final String BITMASK_LAYER_ID = "org.esa.beam.layers.bitmask";

        public static final String PROPERTY_NAME_RASTER = "raster";
        /**
         * @deprecated since BEAM 4.7, no replacement; kept for compatibility of sessions
         */
        @Deprecated
        private static final String PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM = "imageToModelTransform";

        private static final String TYPE_NAME = "BitmaskCollectionLayerType";
        private static final String[] ALIASES = {"org.esa.beam.glayer.BitmaskCollectionLayer$Type"};

        @Override
        public String getName() {
            return TYPE_NAME;
        }
        
        @Override
        public String[] getAliases() {
            return ALIASES;
        }
        
        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            final BitmaskCollectionLayer bitmaskCollectionLayer = new BitmaskCollectionLayer(this, configuration);
            bitmaskCollectionLayer.setId(BITMASK_LAYER_ID);
            return bitmaskCollectionLayer;
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            final PropertySet prototype = super.createLayerConfig(ctx);

            final Property rasterProperty = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
            rasterProperty.getDescriptor().setNotNull(true);
            prototype.addProperty(rasterProperty);

            final Property i2mProperty = Property.create(PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, AffineTransform.class);
            i2mProperty.getDescriptor().setTransient(true);
            prototype.addProperty(i2mProperty);

            return prototype;

        }
    }
}
