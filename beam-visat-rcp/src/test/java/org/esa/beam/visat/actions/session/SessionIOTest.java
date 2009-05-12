package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementXStreamConverter;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import junit.framework.TestCase;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.SimpleInternationalString;
import org.geotools.xml.DocumentWriter;
import org.geotools.xml.schema.Element;
import org.geotools.xml.gml.GMLSchema;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.simple.SimpleFeature;

import javax.naming.OperationNotSupportedException;
import java.awt.Rectangle;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class SessionIOTest extends TestCase {
/* todo - May be useful for final implementation, otherwise remove
    public void testGML() {
        GeometryType geometryAT = new GeometryTypeImpl(new NameImpl("point"), Point.class, DefaultGeographicCRS.WGS84, false, false, null, null, new SimpleInternationalString("A pin geometry type!"));
        AttributeType labelAT = new AttributeTypeImpl(new NameImpl("name"), String.class, false, false, null, null, new SimpleInternationalString("A pin feature!"));
        GeometryDescriptor geometryAD = new GeometryDescriptorImpl(geometryAT,
                                                                   new NameImpl("point"), 1, 1, false, null);
        AttributeDescriptor labelAD = new AttributeDescriptorImpl(labelAT,
                                                                  new NameImpl("name"), 1, 1, false, null);
        List<AttributeDescriptor> schema = Arrays.asList(geometryAD, labelAD);
        SimpleFeatureType pinT = new SimpleFeatureTypeImpl(new NameImpl("pinT"), schema, geometryAD, false, null, null, new SimpleInternationalString("Fuck you!"));

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pinT);
        builder.set("point", new GeometryFactory().createPoint(new Coordinate(13.0, 53.2)));
        builder.set("name", "Pin 3");
        SimpleFeature feature = builder.buildFeature("a2");

        StringWriter writer = new StringWriter();
        try {
            DocumentWriter.writeFragment(feature, GMLSchema.getInstance(), writer, null);
        } catch (OperationNotSupportedException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        assertEquals("", writer.toString());
    }
*/    


/* todo - May be useful for final implementation, otherwise remove
    static {
        ExtensionManager.getInstance().register(ImageLayer.Type.class, new ExtensionFactory() {
            @Override
            public Object getExtension(Object object, Class<?> extensionType) {
                return new DomConverter() {
                    @Override
                    public Class<?> getValueType() {
                        return Map.class;
                    }

                    @Override
                    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                                   ValidationException {
                                                Map<String, Object> configuration = (Map<String, Object>) value;
                        if (configuration == null) {
                            configuration = new HashMap<String, Object>();
                        }

                        parentElement.getChild("multiLevelSourceType")
                        ExtensionManager.getInstance().get

                                                final DomConverter converter = mls.getExtension(DomConverter.class);
                        converter.convertValueToDom(mls, parentElement);
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void convertValueToDom(Object value, DomElement parentElement) {
                        Map<String, Object> configuration = (Map<String, Object>) value;
                        MultiLevelSource mls = (MultiLevelSource) configuration.get("multiLevelSource");
                        final DomConverter converter = mls.getExtension(DomConverter.class);
                        converter.convertValueToDom(mls, parentElement);
                    }
                };
            }

            @Override
            public Class<?>[] getExtensionTypes() {
                return new Class<?>[] {DomConverter.class};
            }
        });
    }
*/

    private interface LayerIO {

        void write(Layer layer, Writer writer, DomConverter domConverter);

        LayerMemento read(Reader reader);
    }

    public void testGetInstance() {
        assertNotNull(SessionIO.getInstance());
        assertSame(SessionIO.getInstance(), SessionIO.getInstance());
    }

    public void testIO() throws Exception {
        ExtensionManager.getInstance().register(LayerType.class, new GraticuleLayerIOFactory());

        final Session session1 = SessionTest.createTestSession();

        testSession(session1);
        final StringWriter writer = new StringWriter();
        SessionIO.getInstance().writeSession(session1, writer);
        final String xml = writer.toString();
        System.out.println("Session XML:\n" + xml);
        final StringReader reader = new StringReader(xml);
        final Session session2 = SessionIO.getInstance().readSession(reader);
        testSession(session2);
    }

    private void testSession(Session session) {
        assertEquals(Session.CURRENT_MODEL_VERSION, session.getModelVersion());

        assertEquals(2, session.getProductCount());
        testProductRef(session.getProductRef(0), 11, "out/DIMAP/X.dim");
        testProductRef(session.getProductRef(1), 15, "out/DIMAP/Y.dim");

        assertEquals(4, session.getViewCount());
        testViewRef(session.getViewRef(0), 0, ProductSceneView.class.getName(), new Rectangle(0, 0, 200, 100), 11, "A");
        testViewRef(session.getViewRef(1), 1, ProductSceneView.class.getName(), new Rectangle(200, 0, 200, 100), 15,
                    "C");
        testViewRef(session.getViewRef(2), 2, ProductSceneView.class.getName(), new Rectangle(0, 100, 200, 100), 11,
                    "B");
        testViewRef(session.getViewRef(3), 3, ProductSceneView.class.getName(), new Rectangle(200, 100, 200, 100), 15,
                    "D");

        assertEquals(3, session.getViewRef(3).getLayerCount());
        assertEquals("[15] D", session.getViewRef(3).getLayerRef(0).name);
        final Session.LayerRef graticuleLayerRef = session.getViewRef(3).getLayerRef(1);
        assertEquals("Graticule", graticuleLayerRef.name);
        assertNotNull(graticuleLayerRef.configuration);
        assertEquals(14, graticuleLayerRef.configuration.getChildCount());
        final Session.LayerRef bitmaskCollectionLayerRef = session.getViewRef(3).getLayerRef(2);
        assertEquals("Bitmask Collection", bitmaskCollectionLayerRef.name);
        assertNotNull(bitmaskCollectionLayerRef.configuration);
        assertEquals(2, bitmaskCollectionLayerRef.configuration.getChildCount());
    }

    private void testProductRef(Session.ProductRef productRef, int expectedId, String expectedRelFile) {
        assertEquals(expectedId, productRef.refNo);
        assertEquals(expectedRelFile, productRef.uri.toString());
    }

    private void testViewRef(Session.ViewRef viewRef, int expectedId, String expectedType, Rectangle expectedBounds,
                             int expectedProductId, String expectedProductNodeName) {
        assertEquals(expectedId, viewRef.id);
        assertEquals(expectedType, viewRef.type);
        assertEquals(expectedBounds, viewRef.bounds);
        assertEquals(expectedProductId, viewRef.productRefNo);
        assertEquals(expectedProductNodeName, viewRef.productNodeName);
    }

    static class GraticuleLayerIOFactory extends SingleTypeExtensionFactory<LayerType, LayerIO> {

        private GraticuleLayerIOFactory() {
            super(LayerIO.class, GraticuleLayerIO.class);
        }

        @Override
        protected LayerIO getExtensionImpl(LayerType layerType, Class<LayerIO> extensionType) throws Throwable {
            return new GraticuleLayerIO();
        }
    }

    static class GraticuleLayerIO implements LayerIO {

        private volatile XStream xs;

        @Override
        public void write(Layer layer, Writer writer, DomConverter domConverter) {
            initIO();

            final Xpp3DomElement configuration = new Xpp3DomElement("configuration");
            try {
                domConverter.convertValueToDom(layer, configuration);
            } catch (com.bc.ceres.binding.ConversionException e) {
                e.printStackTrace();
            }

            final LayerMemento memento = new LayerMemento(layer.getLayerType().getName(), configuration);
            xs.toXML(memento, writer);
        }

        @Override
        public LayerMemento read(Reader reader) {
            initIO();

            final Object obj = xs.fromXML(reader);
            assertTrue(obj instanceof LayerMemento);

            return (LayerMemento) obj;
        }

        private void initIO() {
            if (xs == null) {
                synchronized (this) {
                    if (xs == null) {
                        xs = new XStream();
                        xs.processAnnotations(LayerMemento.class);
                        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);
                        xs.useAttributeFor(LayerMemento.class, "typeName");
                    }
                }
            }
        }
    }

    @XStreamAlias("layer")
    static class LayerMemento {

        @XStreamAlias("type")
        private String typeName;

        @XStreamConverter(DomElementXStreamConverter.class)
        private DomElement configuration;

        LayerMemento(String typeName, DomElement configuration) {
            this.typeName = typeName;
            this.configuration = configuration;
        }

        DomElement getConfiguration() {
            return configuration;
        }
    }
}
