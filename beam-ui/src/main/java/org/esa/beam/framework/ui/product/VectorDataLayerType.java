package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.glayer.ProductLayerContext;

/**
 * @author Marco Peters
 * @author Ralf Quast
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class VectorDataLayerType extends LayerType {

    public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";
    public static final String VECTOR_DATA_LAYER_ID_PREFIX = "org.esa.beam.layers.vectorData";
    private static int id;

    public static Layer createLayer(VectorDataNode vectorDataNode) {
        final VectorDataLayerType layerType = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue(PROPERTY_NAME_VECTOR_DATA, vectorDataNode.getName());

        return layerType.createLayer(vectorDataNode, configuration);
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return ctx instanceof ProductLayerContext;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        Assert.notNull(ctx, "ctx");
        final ProductLayerContext plc = (ProductLayerContext) ctx;
        final String vectorDataName = (String) configuration.getValue(PROPERTY_NAME_VECTOR_DATA);
        final VectorDataNode vectorDataNode = plc.getProduct().getVectorDataGroup().get(vectorDataName);

        return createLayer(vectorDataNode, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer configuration = new PropertyContainer();
        configuration.addProperty(Property.create(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, String.class));
        return configuration;
    }

    private Layer createLayer(VectorDataNode vectorDataNode, PropertySet configuration) {
        final VectorDataLayer layer = new VectorDataLayer(this, vectorDataNode, configuration);
        layer.setId(VECTOR_DATA_LAYER_ID_PREFIX + (++id));

        return layer;
    }
}