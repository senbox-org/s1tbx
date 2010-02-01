package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glayer.PlacemarkLayerType;
import org.junit.After;
import org.junit.Before;

import java.awt.geom.AffineTransform;

public class PlacemarkLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    private Product product;

    public PlacemarkLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(PlacemarkLayerType.class));
    }

    @Before
    public void setup() {
        product = createTestProduct("Test", "Test");
        final Pin pin = createPin("Pin");
        product.getPinGroup().add(pin);

        getProductManager().addProduct(product);
    }

    @After
    public void tearDown() {
        getProductManager().removeProduct(product);
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue("product", product);
        configuration.setValue("placemarkDescriptor", PinDescriptor.INSTANCE);
        configuration.setValue("imageToModelTransform", new AffineTransform());
        return layerType.createLayer(null, configuration);
    }

    private Pin createPin(String name) {
        return new Pin(name, "", "", new PixelPos(), new GeoPos(), PlacemarkSymbol.createDefaultPinSymbol(), product.getGeoCoding());
    }
}
