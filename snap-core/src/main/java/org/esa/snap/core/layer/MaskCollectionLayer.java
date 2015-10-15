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
package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class MaskCollectionLayer extends CollectionLayer {

    public static final String ID = MaskCollectionLayer.class.getName();

    private final ProductNodeListener maskPNL;
    private RasterDataNode raster;

    public MaskCollectionLayer(MaskCollectionLayerType layerType,
                               RasterDataNode raster,
                               PropertySet configuration) {
        super(layerType, configuration, "Masks");
        Assert.notNull(raster, "raster");
        this.raster = raster;
        this.maskPNL = new MaskPNL();
        setId(ID);
        getProduct().addProductNodeListener(maskPNL);
        addListener(new VisibilityLL());
    }

    @Override
    public void disposeLayer() {
        if (raster != null) {
            raster.getProduct().removeProductNodeListener(maskPNL);
            raster = null;
        }
    }

    private Product getProduct() {
        return raster.getProduct();
    }

    private RasterDataNode getRaster() {
        return raster;
    }

    private Layer createLayer(final Mask mask) {
        return MaskLayerType.createLayer(getRaster(), mask);
    }

    private ImageLayer getMaskLayer(final Mask mask) {
        LayerFilter layerFilter = layer -> (layer instanceof  ImageLayer &&
                mask == layer.getConfiguration().getValue(MaskLayerType.PROPERTY_NAME_MASK));
        return (ImageLayer) LayerUtils.getChildLayer(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
    }

    private synchronized void updateChildren() {

        // Collect all current mask layers
        LayerFilter layerFilter = layer -> {
            PropertySet conf = layer.getConfiguration();
            return conf.isPropertyDefined(MaskLayerType.PROPERTY_NAME_MASK) && conf.getValue(MaskLayerType.PROPERTY_NAME_MASK) != null;
        };
        List<Layer> maskLayers = LayerUtils.getChildLayers(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
        HashMap<Mask, Layer> currentLayers = new HashMap<>();
        for (Layer maskLayer : maskLayers) {
            Mask mask = maskLayer.getConfiguration().getValue(MaskLayerType.PROPERTY_NAME_MASK);
            currentLayers.put(mask, maskLayer);
        }

        // Allign mask layers with available masks
        if (raster != null && getProduct() != null) {
            Mask[] availableMasks = getProduct().getMaskGroup().toArray(new Mask[0]);
            HashSet<Layer> unusedLayers = new HashSet<>(maskLayers);
            for (Mask availableMask : availableMasks) {
                //todo add all mask layers as soon as the masks have been scaled to fit the raster
                if (raster.getRasterSize().equals(availableMask.getRasterSize())) {
                    Layer layer = currentLayers.get(availableMask);
                    if (layer != null) {
                        unusedLayers.remove(layer);
                    } else {
                        layer = createLayer(availableMask);
                        getChildren().add(layer);
                    }
                    layer.setVisible(raster.getOverlayMaskGroup().contains(availableMask));
                }
            }

            // Remove unused layers
            for (Layer layer : unusedLayers) {
                layer.dispose();
                Layer layerParent = layer.getParent();
                if (layerParent != null) {
                    layerParent.getChildren().remove(layer);
                }
            }
        }
    }

    public class MaskPNL implements ProductNodeListener {

        @Override
        public synchronized void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Mask) {
                final Mask mask = (Mask) sourceNode;
                final ImageLayer maskLayer = getMaskLayer(mask);
                if (maskLayer != null) {
                    if (event.getPropertyName().equals("name")) {
                        maskLayer.setName(mask.getName());
                    } else {
                        maskLayer.regenerate();
                    }
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Mask) {
                nodeChanged(event);
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Mask) {
                updateChildren();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Mask) {
                updateChildren();
            }
        }
    }

    private class VisibilityLL extends AbstractLayerListener {
        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            if ("visible".equals(event.getPropertyName())) {
                final Object value = layer.getConfiguration().getValue("mask");
                if (value instanceof Mask) {
                    Mask mask = (Mask) value;
                    final ProductNodeGroup<Mask> overlayMaskGroup = getRaster().getOverlayMaskGroup();
                    if (layer.isVisible()) {
                        if (!overlayMaskGroup.contains(mask)) {
                            overlayMaskGroup.add(mask);
                        }
                    } else {
                        overlayMaskGroup.remove(mask);
                    }
                }
            }
        }
    }
}
