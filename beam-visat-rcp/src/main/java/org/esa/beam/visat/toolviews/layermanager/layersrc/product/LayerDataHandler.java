package org.esa.beam.visat.toolviews.layermanager.layersrc.product;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.util.Arrays;
import java.util.List;

class LayerDataHandler extends AbstractLayerListener implements ProductNodeListener {

    private final List<String> imageChangingProperties = Arrays.asList(RasterDataNode.PROPERTY_NAME_DATA,
                                                                       RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE,
                                                                       RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE_USED,
                                                                       RasterDataNode.PROPERTY_NAME_VALID_PIXEL_EXPRESSION,
                                                                       RasterDataNode.PROPERTY_NAME_IMAGE_INFO,
                                                                       VirtualBand.PROPERTY_NAME_EXPRESSION);

    private final RasterDataNode rasterDataNode;
    private final ImageLayer imageLayer;

    LayerDataHandler(RasterDataNode rasterDataNode, ImageLayer imageLayer) {
        this.rasterDataNode = rasterDataNode;
        this.imageLayer = imageLayer;
    }

    @Override
    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        for (Layer childLayer : childLayers) {
            if (childLayer == imageLayer) {
                final Product product = rasterDataNode.getProduct();
                if (product != null) {
                    product.removeProductNodeListener(this);
                }
            }
        }
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == rasterDataNode) {
            if (RasterDataNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                imageLayer.setName(rasterDataNode.getDisplayName());
            } else if (imageChangingProperties.contains(event.getPropertyName())) {
                BandImageMultiLevelSource bandImageSource = (BandImageMultiLevelSource) imageLayer.getMultiLevelSource();
                bandImageSource.setImageInfo(rasterDataNode.getImageInfo());
                imageLayer.regenerate();
            }
        }
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        if (event.getSourceNode() == rasterDataNode) {
            imageLayer.regenerate();
        }
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }
}
