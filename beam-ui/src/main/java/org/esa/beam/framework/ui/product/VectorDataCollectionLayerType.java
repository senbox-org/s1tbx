package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorData;

public class VectorDataCollectionLayerType extends CollectionLayer.Type {

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        Assert.notNull(ctx, "ctx");
        final ProductSceneImage sceneImage = (ProductSceneImage) ctx;
        final ProductNodeGroup<VectorData> vectorDataGroup = sceneImage.getRaster().getProduct().getVectorDataGroup();

        return new VectorDataCollectionLayer(this, vectorDataGroup, configuration);
    }
}
