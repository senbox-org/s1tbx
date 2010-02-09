package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.glayer.ProductLayerContext;

public class VectorDataCollectionLayerType extends CollectionLayer.Type {

    private static final String TYPE_NAME = "VectorDataCollectionLayerType";
    private static final String[] ALIASES = {"org.esa.beam.framework.ui.product.VectorDataCollectionLayerType"};

    @Override
    public String getName() {
        return TYPE_NAME;
    }
    
    @Override
    public String[] getAliases() {
        return ALIASES;
    }
    
    @Override
    public boolean isValidFor(LayerContext ctx) {
        return ctx instanceof ProductLayerContext;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        Assert.notNull(ctx, "ctx");
        final ProductLayerContext plc = (ProductLayerContext) ctx;
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = plc.getProduct().getVectorDataGroup();

        return new VectorDataCollectionLayer(this, vectorDataGroup, configuration);
    }
}
