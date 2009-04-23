package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
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
        final DomElement domElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(layer.getConfiguration(), domElement);

        System.out.println(domElement.toXml());

        final ValueContainer restoredConfiguration = (ValueContainer) domConverter.convertDomToValue(domElement,
                                                                                                     layerType.getConfigurationTemplate());
        compareConfigurations(layer.getConfiguration(), restoredConfiguration);
    }

    protected abstract Layer createLayer(LayerType layerType) throws Exception;

    private static void compareConfigurations(ValueContainer originalConfiguration,
                                              ValueContainer restoredConfiguration) {
        for (ValueModel originalModel : originalConfiguration.getModels()) {
            final ValueDescriptor originalDescriptor = originalModel.getDescriptor();
            final ValueModel restoredModel = restoredConfiguration.getModel(originalDescriptor.getName());

            org.junit.Assert.assertNotNull(restoredModel);
            junit.framework.Assert.assertSame(originalDescriptor.getName(), restoredModel.getDescriptor().getName());
            junit.framework.Assert.assertSame(originalDescriptor.getType(), restoredModel.getDescriptor().getType());

            if (originalModel.getDescriptor().isTransient()) {
                junit.framework.Assert.assertEquals(originalModel.getDescriptor().isTransient(),
                                                    restoredModel.getDescriptor().isTransient());
            } else {
                final Object originalValue = originalModel.getValue();
                final Object restoredValue = restoredModel.getValue();
                junit.framework.Assert.assertSame(originalValue.getClass(), restoredValue.getClass());
                if (originalValue.getClass().isArray()) {
                    junit.framework.Assert.assertEquals(Array.getLength(originalValue), Array.getLength(restoredValue));
                }
            }
        }
    }

}
