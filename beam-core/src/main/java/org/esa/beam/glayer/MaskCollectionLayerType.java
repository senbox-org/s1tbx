package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.RasterDataNode;


public class MaskCollectionLayerType extends CollectionLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";

    private static final String TYPE_NAME = "MaskCollectionLayerType";
    private static final String[] ALIASES = {"org.esa.beam.glayer.MaskCollectionLayerType"};

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
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
        return new MaskCollectionLayer(this, raster, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet template = super.createLayerConfig(ctx);
        template.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);
        return template;
    }
}
