package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementConverter;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.MessageFormat;

public class LayerIOTest extends TestCase {

    private Context context;

    public static interface Context {

        public Product getProduct(int refNo);
    }

    public static class RasterRef {

        private int refNo;
        private String rasterName;

        public RasterRef() {
        }

        public RasterRef(int refNo, String rasterName) {
            this.refNo = refNo;
            this.rasterName = rasterName;
        }
    }

    @Override
    protected void setUp() throws Exception {
        final ProductManager pm = new ProductManager();

        final Product a = new Product("A", "AT", 17, 11);
        final Product b = new Product("B", "BT", 19, 67);

        a.addBand(new VirtualBand("a", ProductData.TYPE_INT32, 17, 11, "2"));
        b.addBand(new VirtualBand("b", ProductData.TYPE_INT32, 19, 67, "4"));

        pm.addProduct(a);
        pm.addProduct(b);

        context = new Context() {
            @Override
            public Product getProduct(int refNo) {
                for (Product product : pm.getProducts()) {
                    if (refNo == product.getRefNo()) {
                        return product;
                    }
                }

                return null;
            }
        };
    }

    public void testWriteAndReadL() throws ValidationException, ConversionException {
        // 1. Create an L with factory mathod in T
        final T t = new T();
        final ValueContainer configuration1 = t.createDefaultConfiguration();
        assertNotNull(configuration1);

        assertNull(configuration1.getValue("rasterRef"));
        assertEquals(true, configuration1.getValue("borderShown"));
        assertEquals(1.0, configuration1.getValue("borderWidth"));
        assertEquals(Color.YELLOW, configuration1.getValue("borderColor"));

        try {
            t.createL(context, configuration1);
            fail();
        } catch (ValidationException expected) {
       }

        configuration1.setValue("rasterRef", new RasterRef(1, "a"));
        assertNotNull(configuration1.getValue("rasterRef"));

        final L l1 = t.createL(context, configuration1);
        assertNotNull(l1);
        assertNotNull(l1.getMultiLevelSource());

        // 2. Write configuration to XML
        final ValueContainerConverter dc = new ValueContainerConverter();
        final DomElement domElement1 = Xpp3DomElement.createDomElement("configuration");
        dc.convertValueContainerToDom(configuration1, domElement1);

        final M m1 = new M("T", domElement1);
        final XStream xs = new XStream();
        xs.processAnnotations(M.class);
        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);
        xs.useAttributeFor(M.class, "typeName");

        final String xml = xs.toXML(m1);
        System.out.println(xml);

        // 3. Read configuration from XML
        xs.registerConverter(new DomElementConverter());
        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);
        final Object obj = xs.fromXML(xml);
        assertTrue(obj instanceof M);
        final M m2 = (M) obj;

        // 4. Restore layer
        final ValueContainer configuration2 = t.createDefaultConfiguration();
        dc.convertDomToValueContainer(m2.getConfiguration(), configuration2);

        final L l2 = t.createL(context, configuration2);
        assertNotNull(l2);
        assertNotNull(l2.getMultiLevelSource());

        System.out.println(xs.toXML(m2));
    }

    @XStreamAlias("layer")
    static class M {

        @XStreamAlias("type")
        private final String typeName;

        @XStreamConverter(DomElementConverter.class)
        private final DomElement configuration;

        M(String typeName, DomElement configuration) {
            this.typeName = typeName;
            this.configuration = configuration;
        }

        DomElement getConfiguration() {
            return configuration;
        }
    }

    private static class L extends ExtensibleObject {

        private MultiLevelSource multiLevelSource;
        private transient MultiLevelRenderer multiLevelRenderer;

        final ValueContainer configuration;

        L(MultiLevelSource multiLevelSource, ValueContainer configuration) {
            this.multiLevelSource = multiLevelSource;
            this.configuration = configuration;
        }

        final ValueContainer getConfiguration() {
            return configuration;
        }

        public MultiLevelSource getMultiLevelSource() {
            return multiLevelSource;
        }
    }

    private static class T {

        private static final boolean DEFAULT_BORDER_SHOWN = true;
        private static final double DEFAULT_BORDER_WIDTH = 1.0;
        private static final Color DEFAULT_BORDER_COLOR = Color.YELLOW;

        L createL(Context context, ValueContainer configuration) throws ValidationException {
            for (final ValueModel model : configuration.getModels()) {
                model.validate(model.getValue());
            }
            final RasterRef rasterRef = (RasterRef) configuration.getValue("rasterRef");
            final Product product = context.getProduct(rasterRef.refNo);
            final RasterDataNode raster = product.getRasterDataNode(rasterRef.rasterName);
            final MultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(raster,
                                                                                       ProgressMonitor.NULL);

            return new L(multiLevelSource, configuration);
        }

        ValueContainer createDefaultConfiguration() {
            final ValueContainer configuration = new ValueContainer();

            final ValueDescriptor rasterRefDescriptor = new ValueDescriptor("rasterRef", RasterRef.class);
            rasterRefDescriptor.setNotNull(true);
            final ValueDescriptor borderShownDescriptor = new ValueDescriptor("borderShown", Boolean.class);
            borderShownDescriptor.setDefaultValue(DEFAULT_BORDER_SHOWN);
            final ValueDescriptor borderWidthDescriptor = new ValueDescriptor("borderWidth", Double.class);
            borderWidthDescriptor.setDefaultValue(DEFAULT_BORDER_WIDTH);
            final ValueDescriptor borderColorDescriptor = new ValueDescriptor("borderColor", Color.class);
            borderColorDescriptor.setDefaultValue(DEFAULT_BORDER_COLOR);

            configuration.addModel(new ValueModel(rasterRefDescriptor, new DefaultValueAccessor()));
            configuration.addModel(new ValueModel(borderShownDescriptor, new DefaultValueAccessor()));
            configuration.addModel(new ValueModel(borderWidthDescriptor, new DefaultValueAccessor()));
            configuration.addModel(new ValueModel(borderColorDescriptor, new DefaultValueAccessor()));

            try {
                configuration.setDefaultValues();
            } catch (ValidationException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            return configuration;
        }
    }

    static class TestPojo {

        boolean visible = true;
        double transparency = 0.5;
        Color textColor = new Color(17, 11, 67);

        RasterRef rasterRef = new RasterRef(1, "a");
    }

    public void testDomConverter() {
        final TestPojo testPojo = new TestPojo();
        final ValueContainer vc = ValueContainer.createObjectBacked(testPojo);

        final ValueContainerConverter dc = new ValueContainerConverter();
        final DomElement domElement = Xpp3DomElement.createDomElement("configuration");

        dc.convertValueContainerToDom(vc, domElement);

        final String xml = domElement.toXml();
        System.out.println(xml);
    }

    static class ValueContainerConverter {

        private final ClassFieldDescriptorFactory descriptorFactory = new ClassFieldDescriptorFactory() {
            @Override
            public ValueDescriptor createValueDescriptor(Field field) {
                return new ValueDescriptor(field.getName(), field.getType());
            }
        };

        public void convertDomToValueContainer(DomElement parentElement, ValueContainer valueContainer) throws
                                                                                                        ConversionException,
                                                                                                        ValidationException {
            for (final DomElement childElement : parentElement.getChildren()) {
                final String childElementName = childElement.getName();
                final ValueModel valueModel = valueContainer.getModel(childElementName);
                // TODO: inlined array elements
                if (valueModel == null) {
                    throw new ConversionException(String.format("Illegal element '%s'.", childElementName));
                }

                final Object childValue;
                final ValueDescriptor descriptor = valueModel.getDescriptor();
                final DomConverter domConverter = descriptor.getDomConverter();
                if (domConverter != null) {
                    childValue = domConverter.convertDomToValue(childElement, null);
                } else {
                    final Class<?> valueType = descriptor.getType();
                    Converter<?> converter = descriptor.getConverter();
                    if (converter == null) {
                        converter = ConverterRegistry.getInstance().getConverter(valueType);   
                    }
                    childValue = domToValue(childElement, converter, valueType);
                }

                valueModel.setValue(childValue);
            }
        }

        public void convertValueContainerToDom(ValueContainer valueContainer, DomElement parentElement) {
            for (ValueModel valueModel : valueContainer.getModels()) {
                final ValueDescriptor descriptor = valueModel.getDescriptor();
                final Object value = valueModel.getValue();
                final DomConverter domConverter = descriptor.getDomConverter();

                if (domConverter != null) {
                    final DomElement childElement = parentElement.createChild(descriptor.getName());
                    domConverter.convertValueToDom(value, childElement);

                    continue;
                }

                if (descriptor.getType().isArray()) {
                    // TODO: arrays
                    continue;
                }

                Converter<?> converter = descriptor.getConverter();
                if (converter == null) {
                    converter = ConverterRegistry.getInstance().getConverter(descriptor.getType());
                }
                final DomElement childElement = parentElement.createChild(descriptor.getName());
                valueToDom(value, converter, childElement);
            }
        }

        private Object domToValue(DomElement domElement, Converter<?> converter, Class<?> valueType)
                throws ConversionException, ValidationException {
            final Object childValue;

            if (converter != null) {
                final String text = domElement.getValue();
                if (text != null) {
                    try {
                        childValue = converter.parse(text);
                    } catch (ConversionException e) {
                        throw new ConversionException(MessageFormat.format(
                                "In a member of ''{0}'': {1}", domElement.getName(), e.getMessage()), e);
                    }
                } else {
                    childValue = null;
                }
            } else {
                final DomConverter childConverter = getChildConverter(domElement, valueType);
                try {
                    childValue = childConverter.convertDomToValue(domElement, null);
                } catch (ValidationException e) {
                    throw new ValidationException(MessageFormat.format(
                            "In a member of ''{0}'': {1}", domElement.getName(), e.getMessage()), e);
                } catch (ConversionException e) {
                    throw new ConversionException(MessageFormat.format(
                            "In a member of ''{0}'': {1}", domElement.getName(), e.getMessage()), e);
                }
            }

            return childValue;
        }

        private void valueToDom(Object value, Converter converter, DomElement childElement) {
            if (converter != null) {
                final String text = converter.format(value);
                if (text != null && !text.isEmpty()) {
                    childElement.setValue(text);
                }
            } else if (value != null) {
                final ValueContainer valueContainer = createValueContainer(value);
                convertValueContainerToDom(valueContainer, childElement);
            }
        }

        private ValueContainer createValueContainer(Object obj) {
            return ValueContainer.createObjectBacked(obj, descriptorFactory);
        }

        private DomConverter getChildConverter(DomElement element, Class<?> valueType) {
            return new DefaultDomConverter(valueType, descriptorFactory);
        }
    }
}
