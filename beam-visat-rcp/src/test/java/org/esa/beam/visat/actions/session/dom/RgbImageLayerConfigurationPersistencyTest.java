package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glayer.RgbImageLayerType;

import java.awt.geom.AffineTransform;

public class RgbImageLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public RgbImageLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(RgbImageLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertyContainer configuration = layerType.createLayerConfig(null);

        final Product product = createTestProduct("Test", "TEST");
        addVirtualBand(product, "a", ProductData.TYPE_INT32, "17");
        addVirtualBand(product, "b", ProductData.TYPE_INT32, "11");
        addVirtualBand(product, "c", ProductData.TYPE_INT32, "67");
        getProductManager().addProduct(product);
        configuration.setValue("product", product);
        configuration.setValue("expressionR", "a + b");
        configuration.setValue("expressionG", "b + c");
        configuration.setValue("expressionB", "a - c");
        configuration.setValue("imageToModelTransform", new AffineTransform());
        return layerType.createLayer(null, configuration);
    }
}
