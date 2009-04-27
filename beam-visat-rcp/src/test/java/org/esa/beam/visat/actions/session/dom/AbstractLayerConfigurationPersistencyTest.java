package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Array;

public abstract class AbstractLayerConfigurationPersistencyTest {

    private static ProductManager productManager;
    private final LayerType layerType;

    protected AbstractLayerConfigurationPersistencyTest(LayerType layerType) {
        this.layerType = layerType;
    }

    @BeforeClass
    public static void setupClass() {
        Product product = new Product("P", "T", 10, 10);
        product.setFileLocation(new File("out/P.dim"));
        Band band = new VirtualBand("V", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(band);
        productManager = new ProductManager();
        productManager.addProduct(product);
    }

    public static ProductManager getProductManager() {
        return productManager;
    }

    @Test
    public void testLayerConfigurationPersistency() throws Exception {
        final Layer layer = createLayer(layerType);

        final SessionDomConverter domConverter = new SessionDomConverter(getProductManager());
        final DomElement originalDomElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(layer.getConfiguration(), originalDomElement);
        System.out.println(originalDomElement.toXml());

        final ValueContainer restoredConfiguration = (ValueContainer) domConverter.convertDomToValue(originalDomElement,
                                                                                                     layerType.getConfigurationTemplate());
        compareConfigurations(layer.getConfiguration(), restoredConfiguration);
        final DomElement restoredDomElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(restoredConfiguration, restoredDomElement);
        System.out.println(restoredDomElement.toXml());
    }

    protected abstract Layer createLayer(LayerType layerType) throws Exception;

    private static void compareConfigurations(ValueContainer originalConfiguration,
                                              ValueContainer restoredConfiguration) {
        for (final ValueModel originalModel : originalConfiguration.getModels()) {
            final ValueDescriptor originalDescriptor = originalModel.getDescriptor();
            final ValueModel restoredModel = restoredConfiguration.getModel(originalDescriptor.getName());

            org.junit.Assert.assertNotNull(restoredModel);
            assertSame(originalDescriptor.getName(), restoredModel.getDescriptor().getName());
            assertSame(originalDescriptor.getType(), restoredModel.getDescriptor().getType());

            if (originalModel.getDescriptor().isTransient()) {
                assertEquals(originalModel.getDescriptor().isTransient(),
                             restoredModel.getDescriptor().isTransient());
            } else {
                final Object originalValue = originalModel.getValue();
                final Object restoredValue = restoredModel.getValue();
                assertSame(originalValue.getClass(), restoredValue.getClass());
                if (originalValue.getClass().isArray()) {
                    assertEquals(Array.getLength(originalValue), Array.getLength(restoredValue));
                }
            }
        }
    }

}
