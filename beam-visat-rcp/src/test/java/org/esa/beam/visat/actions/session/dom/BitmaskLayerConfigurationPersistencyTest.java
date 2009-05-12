package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glayer.BitmaskLayerType;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;

import java.awt.Color;
import java.awt.geom.AffineTransform;

public class BitmaskLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    private BitmaskDef bitmaskDef;

    public BitmaskLayerConfigurationPersistencyTest() {
        super(LayerType.getLayerType(BitmaskLayerType.class.getName()));
    }

    @Before
    public void setUp() {
        bitmaskDef = new BitmaskDef("Invalid", "Invalid values", "V != 42", Color.ORANGE, 0.5f);
        final Product product = getProductManager().getProduct(0);
        product.addBitmaskDef(bitmaskDef);
    }

    @After
    public void tearDown() {
        final Product product = getProductManager().getProduct(0);
        product.removeBitmaskDef(bitmaskDef);
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final ValueContainer originalConfiguration = layerType.getConfigurationTemplate();
        assertNotNull(originalConfiguration);

        originalConfiguration.setValue("bitmaskDef", bitmaskDef);
        originalConfiguration.setValue("imageToModelTransform", new AffineTransform());
        originalConfiguration.setValue("product", bitmaskDef.getProduct());

        return layerType.createLayer(null, originalConfiguration);
    }

}
