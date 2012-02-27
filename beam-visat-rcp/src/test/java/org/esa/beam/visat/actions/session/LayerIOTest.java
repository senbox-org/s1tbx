/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.binding.dom.*;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LayerIOTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        final ProductManager pm = new ProductManager();

        final Product a = new Product("A", "AT", 17, 11);
        final Product b = new Product("B", "BT", 19, 67);

        a.addBand(new VirtualBand("a", ProductData.TYPE_INT32, 17, 11, "2"));
        b.addBand(new VirtualBand("b", ProductData.TYPE_INT32, 19, 67, "4"));

        pm.addProduct(a);
        pm.addProduct(b);

        final Converter<RasterDataNode, RasterRef> rasterConverter = new Converter<RasterDataNode, RasterRef>() {
            @Override
            public RasterRef getR(RasterDataNode raster) {
                return new RasterRef(raster.getProduct().getRefNo(), raster.getName());
            }

            @Override
            public RasterDataNode getT(RasterRef rasterRef) {
                return getProduct(rasterRef.refNo).getRasterDataNode(rasterRef.rasterName);
            }

            private Product getProduct(int refNo) {
                for (Product product : pm.getProducts()) {
                    if (refNo == product.getRefNo()) {
                        return product;
                    }
                }

                return null;
            }
        };

        DefaultContext.getInstance().setConverter(RasterDataNode.class, RasterRef.class, rasterConverter);
    }

    public void testWriteAndReadL() throws ValidationException, ConversionException {
        // 1. Create an L with factory mathod in T
        final T t = new T();
        final PropertyContainer configuration1 = t.createConfigurationTemplate();
        assertNotNull(configuration1);

        assertNull(configuration1.getValue("rasterRef"));
        assertEquals(true, configuration1.getValue("borderShown"));
        assertEquals(1.0, configuration1.getValue("borderWidth"));
        assertEquals(Color.YELLOW, configuration1.getValue("borderColor"));

        final Context context = DefaultContext.getInstance();
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
        final DefaultDomConverter dc = new DefaultDomConverter(PropertyContainer.class);
        final DomElement domElement1 = new DefaultDomElement("configuration");
        dc.convertValueToDom(configuration1, domElement1);

        final M m1 = new M("T", domElement1);
        final XStream xs = new XStream();
        xs.processAnnotations(M.class);
        xs.registerConverter(new DomElementXStreamConverter());
        xs.alias("configuration", DomElement.class, DefaultDomElement.class);
        xs.alias("configuration", DomElement.class, XppDomElement.class);
        xs.useAttributeFor(M.class, "typeName");

        final String xml = xs.toXML(m1);
        System.out.println(xml);

        // 3. Read configuration from XML
        final Object obj = xs.fromXML(xml);
        assertTrue(obj instanceof M);
        final M m2 = (M) obj;

        // 4. Restore layer
        final PropertyContainer configuration2 = t.createConfigurationTemplate();
        dc.convertDomToValue(m2.getConfiguration(), configuration2);

        final L l2 = t.createL(context, configuration2);
        assertNotNull(l2);
        assertNotNull(l2.getMultiLevelSource());

        System.out.println(xs.toXML(m2));
    }

    @XStreamAlias("layer")
    static class M {

        @XStreamAlias("type")
        private final String typeName;

        @XStreamConverter(DomElementXStreamConverter.class)
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

        final PropertyContainer configuration;

        L(MultiLevelSource multiLevelSource, PropertyContainer configuration) {
            this.multiLevelSource = multiLevelSource;
            this.configuration = configuration;
        }

        final PropertyContainer getConfiguration() {
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

        L createL(Context context, PropertyContainer configuration) throws ValidationException {
            for (final Property model : configuration.getProperties()) {
                model.validate(model.getValue());
            }
            final RasterRef rasterRef = (RasterRef) configuration.getValue("rasterRef");
            final Converter<RasterDataNode, RasterRef> converter = context.getConverter(RasterDataNode.class,
                                                                                        RasterRef.class);
            final RasterDataNode raster = converter.getT(rasterRef);
            final MultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);

            return new L(multiLevelSource, configuration);
        }

        PropertyContainer createConfigurationTemplate() {
            final PropertyContainer configuration = new PropertyContainer();

            final PropertyDescriptor rasterRefDescriptor = new PropertyDescriptor("rasterRef", RasterRef.class);
            rasterRefDescriptor.setNotNull(true);
            final PropertyDescriptor borderShownDescriptor = new PropertyDescriptor("borderShown", Boolean.class);
            borderShownDescriptor.setDefaultValue(DEFAULT_BORDER_SHOWN);
            final PropertyDescriptor borderWidthDescriptor = new PropertyDescriptor("borderWidth", Double.class);
            borderWidthDescriptor.setDefaultValue(DEFAULT_BORDER_WIDTH);
            final PropertyDescriptor borderColorDescriptor = new PropertyDescriptor("borderColor", Color.class);
            borderColorDescriptor.setDefaultValue(DEFAULT_BORDER_COLOR);

            configuration.addProperty(new Property(rasterRefDescriptor, new DefaultPropertyAccessor()));
            configuration.addProperty(new Property(borderShownDescriptor, new DefaultPropertyAccessor()));
            configuration.addProperty(new Property(borderWidthDescriptor, new DefaultPropertyAccessor()));
            configuration.addProperty(new Property(borderColorDescriptor, new DefaultPropertyAccessor()));

            configuration.setDefaultValues();
            return configuration;
        }
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

    private interface Converter<T, R> {

        R getR(T t);

        T getT(R r);
    }

    private interface Context {

        <T, R> Converter<T, R> getConverter(Class<T> typeT, Class<R> typeR);
    }

    private static class DefaultContext implements Context {

        private static final DefaultContext UNIQUE_INSTANCE = new DefaultContext();

        private static class Key<T, R> {

            final Class<T> typeT;
            final Class<R> typeR;

            private Key(Class<T> typeT, Class<R> typeR) {
                this.typeT = typeT;
                this.typeR = typeR;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Key && typeT == ((Key<?, ?>) obj).typeT && typeR == ((Key<?, ?>) obj).typeR;
            }

            @Override
            public int hashCode() {
                return (typeT.hashCode() << 16) | typeR.hashCode();
            }
        }

        private final Map<Key<?, ?>, Converter<?, ?>> converterMap;

        private DefaultContext() {
            converterMap = new HashMap<Key<?, ?>, Converter<?, ?>>();
        }

        static DefaultContext getInstance() {
            return UNIQUE_INSTANCE;
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public <T, R> Converter<T, R> getConverter(Class<T> typeT, Class<R> typeR) {
            return (Converter<T, R>) converterMap.get(new Key<T, R>(typeT, typeR));
        }

        public <T, R> void setConverter(Class<T> typeT, Class<R> typeR, Converter<T, ?> converter) {
            converterMap.put(new Key<T, R>(typeT, typeR), converter);
        }
    }

}
