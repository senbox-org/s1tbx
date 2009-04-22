package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glayer.RasterImageLayerType;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Array;

public class SessionDomConverterTest {

    private static Band band;
    private static Session.SessionAccessor sessionAccessor;
    private static ExtensionFactory extensionFactory;

    @BeforeClass
    public static void setupClass() {
        extensionFactory = new TestExtensionFactory();
        ExtensionManager.getInstance().register(LayerType.class, extensionFactory);

        final Product product = new Product("P", "T", 10, 10);
        product.setFileLocation(new File("out/P.dim"));
        band = new VirtualBand("V", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(band);
        sessionAccessor = new Session.SessionAccessor(new Product[]{product});
    }

    @AfterClass
    public static void tearDownClass() {
        ExtensionManager.getInstance().unregister(LayerType.class, extensionFactory);
    }

    @Test
    public void testSomething() throws ValidationException, ConversionException {
        final RasterImageLayerType layerType = (RasterImageLayerType) LayerType.getLayerType(
                RasterImageLayerType.class.getName());
        final ValueContainer outConf = layerType.getConfigurationTemplate();
        outConf.setValue(RasterImageLayerType.PROPERTY_NAME_RASTERS, new RasterDataNode[]{band});
        final Layer layer = layerType.createLayer(null, outConf);

        final SessionDomConverter domConverter = layerType.getExtension(SessionDomConverter.class);
        assertNotNull("No SessionDomConverter found", domConverter);
        domConverter.setSessionAccessor(sessionAccessor);

        DomElement elem = new DefaultDomElement("testConfig");
        domConverter.convertValueToDom(layer.getConfiguration(), elem);

        System.out.println(elem.toXml());

        final ValueContainer inConf = (ValueContainer) domConverter.convertDomToValue(elem,
                                                                                      layerType.getConfigurationTemplate());
        for (ValueModel outModel : outConf.getModels()) {
            final ValueDescriptor outDescriptor = outModel.getDescriptor();
            final ValueModel inModel = inConf.getModel(outDescriptor.getName());
            assertNotNull(inModel);
            assertEquals(outDescriptor.getName(), inModel.getDescriptor().getName());
            assertEquals(outDescriptor.getType(), inModel.getDescriptor().getType());
            if (outModel.getDescriptor().isTransient()) {
                assertEquals(outModel.getDescriptor().isTransient(), inModel.getDescriptor().isTransient());
            } else {
                final Object outValue = outModel.getValue();
                final Object inValue = inModel.getValue();
                assertSame(outValue.getClass(), inValue.getClass());
                if (outValue.getClass().isArray()) {
                    assertEquals(Array.getLength(outValue), Array.getLength(inValue));
                }
            }
        }
    }

    private static class TestExtensionFactory implements ExtensionFactory {

        @Override
        public Object getExtension(Object object, Class<?> extensionType) {
            return new SessionDomConverter();
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{SessionDomConverter.class};
        }
    }
}
