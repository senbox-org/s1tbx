package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.VectorData;

/**
 * @author Marco Peters
 * @author Ralf Quast
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class VectorDataLayerType extends LayerType {

    public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";
    public static final String VECTOR_DATA_LAYER_ID_PREFIX = "org.esa.beam.layers.vectorData";
    public static int id;

    public static Layer createLayer(VectorData vectorData) {
        final VectorDataLayerType layerType = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
        final PropertyContainer configuration = layerType.createLayerConfig(null);
        configuration.setValue(PROPERTY_NAME_VECTOR_DATA, vectorData.getName());

        return layerType.createLayer(vectorData, configuration);
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        Assert.notNull(ctx, "ctx");
        final ProductSceneView sceneView = (ProductSceneView) ctx;
        final String vectorDataName = (String) configuration.getValue(PROPERTY_NAME_VECTOR_DATA);
        final VectorData vectorData = sceneView.getRaster().getProduct().getVectorDataGroup().get(vectorDataName);

        return createLayer(vectorData, configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer configuration = new PropertyContainer();
        configuration.addProperty(Property.create(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, String.class));
        return configuration;
    }

    private Layer createLayer(VectorData vectorData, PropertyContainer configuration) {
        final VectorDataLayer layer = new VectorDataLayer(this, vectorData, configuration);
        layer.setId(VECTOR_DATA_LAYER_ID_PREFIX + (++id));

        return layer;
    }
}