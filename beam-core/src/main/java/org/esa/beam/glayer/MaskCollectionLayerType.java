package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.RasterDataNode;


public class MaskCollectionLayerType extends CollectionLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "Mask / ROI Layers";
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
        return new MaskCollectionLayer(this, raster, configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer template = super.createLayerConfig(ctx);
        template.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);
        return template;
    }
}
