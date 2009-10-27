package org.esa.beam.glayer;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.geom.AffineTransform;


public class MaskCollectionLayerType extends CollectionLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "Mask / ROI Layers";
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new MaskCollectionLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = super.getConfigurationTemplate();
        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);
        return template;
    }
}
