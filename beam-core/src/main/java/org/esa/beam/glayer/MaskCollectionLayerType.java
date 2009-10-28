package org.esa.beam.glayer;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.datamodel.RasterDataNode;


public class MaskCollectionLayerType extends CollectionLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "Mask / ROI Layers";
    }

    @Override
    public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        return new MaskCollectionLayer(this, configuration);
    }

    @Override
    public ValueContainer createLayerConfig(LayerContext ctx) {
        final ValueContainer template = super.createLayerConfig(ctx);
        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);
        return template;
    }
}
