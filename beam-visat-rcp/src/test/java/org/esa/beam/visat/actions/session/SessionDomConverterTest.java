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
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glayer.BitmaskLayerType;
import org.esa.beam.glayer.RasterImageLayerType;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Array;

public class SessionDomConverterTest {

    private static Band band;
    private static Session.SessionAccessor sessionAccessor;
    private static ExtensionFactory extensionFactory;
    private static BitmaskDef bitmaskDef;
    private static Product product;

    @BeforeClass
    public static void setupClass() {
        product = new Product("P", "T", 10, 10);
        product.setFileLocation(new File("out/P.dim"));
        band = new VirtualBand("V", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(band);
        bitmaskDef = new BitmaskDef("Invalid", "Invalid values", "V != 42", Color.ORANGE, 0.5f);
        product.addBitmaskDef(bitmaskDef);

        sessionAccessor = new Session.SessionAccessor(new Product[]{product});
    }

    @Test
    public void testWriteReadImageLayer() throws ValidationException, ConversionException {
        final RasterImageLayerType layerType = (RasterImageLayerType) LayerType.getLayerType(
                RasterImageLayerType.class.getName());
        final ValueContainer originalConfiguration = layerType.getConfigurationTemplate();
        originalConfiguration.setValue(RasterImageLayerType.PROPERTY_NAME_RASTERS, new RasterDataNode[]{band});
        final Layer layer = layerType.createLayer(null, originalConfiguration);

        final SessionDomConverter domConverter = new SessionDomConverter();
        domConverter.setSessionAccessor(sessionAccessor);

        DomElement elem = new DefaultDomElement("testConfig");
        domConverter.convertValueToDom(layer.getConfiguration(), elem);

        System.out.println(elem.toXml());

        final ValueContainer restoredConfiguration = (ValueContainer) domConverter.convertDomToValue(elem,
                                                                                                     layerType.getConfigurationTemplate());
        for (ValueModel outModel : originalConfiguration.getModels()) {
            final ValueDescriptor outDescriptor = outModel.getDescriptor();
            final ValueModel inModel = restoredConfiguration.getModel(outDescriptor.getName());
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

    public void testWriteReadBitmaskLayer() throws ValidationException, ConversionException {
        final BitmaskLayerType layerType = (BitmaskLayerType) LayerType.getLayerType(
                BitmaskLayerType.class.getName());
        assertNotNull(layerType);

        final ValueContainer originalConfiguration = layerType.getConfigurationTemplate();
        assertNotNull(originalConfiguration);

        originalConfiguration.setValue("bitmask.bitmaskDef", bitmaskDef);
        originalConfiguration.setValue("bitmask.product", product);

        final Layer layer = layerType.createLayer(null, originalConfiguration);

        final SessionDomConverter domConverter = new SessionDomConverter();
        domConverter.registerDomConverter(BitmaskDef.class, new BitmaskDefDomConverter());
        domConverter.registerDomConverter(Product.class, new ProductDomConverter());

        domConverter.setSessionAccessor(sessionAccessor);

        final DomElement domElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(layer.getConfiguration(), domElement);

        System.out.println(domElement.toXml());

        final ValueContainer restoredConfiguration = (ValueContainer) domConverter.convertDomToValue(domElement,
                                                                                                     layerType.getConfigurationTemplate());

        for (ValueModel originalModel : originalConfiguration.getModels()) {
            final ValueDescriptor originalDescriptor = originalModel.getDescriptor();
            final ValueModel restoredModel = restoredConfiguration.getModel(originalDescriptor.getName());

            assertNotNull(restoredModel);
            assertSame(originalDescriptor.getName(), restoredModel.getDescriptor().getName());
            assertSame(originalDescriptor.getType(), restoredModel.getDescriptor().getType());

            if (originalModel.getDescriptor().isTransient()) {
                assertEquals(originalModel.getDescriptor().isTransient(), restoredModel.getDescriptor().isTransient());
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

    private static class BitmaskDefDomConverter extends AbstractSingleTypeDomConverter<BitmaskDef> {

        public BitmaskDefDomConverter() {
            super(BitmaskDef.class);
        }

        @Override
        public BitmaskDef convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                           ValidationException {
            return null; // TODO: implement
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) {
            // TODO: implement
        }
    }

    private static class ProductDomConverter extends AbstractSingleTypeDomConverter<Product> {

        public ProductDomConverter() {
            super(Product.class);
        }

        @Override
        public Product convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                        ValidationException {
            return null; // TODO: implement
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) {
            // TODO: implement
        }
    }
}
