package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.bc.ceres.binding.dom.DomElementConverter;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import junit.framework.TestCase;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SessionIOTest extends TestCase {

    private static interface LayerIO {

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
        testProductRef(session.getProductRef(0), 11, new File("testdata/out/DIMAP/X.dim"));
        testProductRef(session.getProductRef(1), 15, new File("testdata/out/DIMAP/Y.dim"));

        assertEquals(4, session.getViewCount());
        testViewRef(session.getViewRef(0), 0, ProductSceneView.class.getName(), new Rectangle(0, 0, 200, 100), 11, "A");
        testViewRef(session.getViewRef(1), 1, ProductSceneView.class.getName(), new Rectangle(200, 0, 200, 100), 15,
                    "C");
        testViewRef(session.getViewRef(2), 2, ProductSceneView.class.getName(), new Rectangle(0, 100, 200, 100), 11,
                    "B");
        testViewRef(session.getViewRef(3), 3, ProductSceneView.class.getName(), new Rectangle(200, 100, 200, 100), 15,
                    "D");

        assertEquals(2, session.getViewRef(3).getLayerCount());
        assertEquals("[15] D", session.getViewRef(3).getLayerRef(0).name);
        assertEquals("Graticule", session.getViewRef(3).getLayerRef(1).name);
    }

    private void testProductRef(Session.ProductRef productRef, int expectedId, File expectedFile) {
        assertEquals(expectedId, productRef.id);
        assertEquals(expectedFile, productRef.file);
    }

    private void testViewRef(Session.ViewRef viewRef, int expectedId, String expectedType, Rectangle expectedBounds,
                             int expectedProductId, String expectedProductNodeName) {
        assertEquals(expectedId, viewRef.id);
        assertEquals(expectedType, viewRef.type);
        assertEquals(expectedBounds, viewRef.bounds);
        assertEquals(expectedProductId, viewRef.productId);
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

    }

    @XStreamAlias("layer")
    static class XC {
        @XStreamConverter(DomElementConverter.class)
        DomElement configuration;
    }

    static class LC {
        boolean visible;
        double transparency;

        Color bgPaint;
        Color fgPaint;
        boolean showBorder;
        Font labelFont;
    }

    public void testLayerConfigurationIO() throws ValidationException, ConversionException {
        // init xstream
        final XStream xs = new XStream();
        xs.processAnnotations(XC.class);
        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);

        final ClassFieldDescriptorFactory valueDescriptorFactory = new ClassFieldDescriptorFactory() {
            @Override
            public ValueDescriptor createValueDescriptor(Field field) {
                return new ValueDescriptor(field.getName(), field.getType());
            }
        };
        final DefaultDomConverter domConverter = new DefaultDomConverter(LC.class, valueDescriptorFactory);
        final LC lc1 = new LC();
        lc1.visible = true;
        lc1.transparency = 0.5;
        lc1.bgPaint = Color.BLACK;
        lc1.fgPaint = Color.GREEN;
        lc1.showBorder = true;
        lc1.labelFont = new Font("helvetica", Font.ITALIC, 11);

        final Xpp3DomElement configuration = Xpp3DomElement.createDomElement("configuration");
        domConverter.convertValueToDom(lc1, configuration);

        final XC xc1 = new XC();
        xc1.configuration = configuration;
        final String xcXml = xs.toXML(xc1);
        System.out.println(xcXml);

        final Object o = xs.fromXML(xcXml);
        assertTrue(o instanceof XC);
        final XC xc2 = (XC) o;
        final LC lc2 = new LC();
        domConverter.convertDomToValue(xc2.configuration, lc2);
        assertEquals(true, lc2.visible);
        assertEquals(0.5, lc2.transparency, 0.0);
        assertEquals(Color.BLACK, lc2.bgPaint);
        assertEquals(Color.GREEN, lc2.fgPaint);
        assertEquals(true, lc2.showBorder);
        assertEquals(new Font("helvetica", Font.ITALIC, 11), lc2.labelFont);
    }

    public void testMyLayerCreation() {
        // Layer configuration test
        final LayerType type = new MyLayer.Type();
        final LayerContext ctx = new LayerContext() {
            @Override
            public Object getCoordinateReferenceSystem() {
                return null;
            }

            @Override
            public Layer getRootLayer() {
                return null;
            }
        };
        final Map<String, Object> configuration = type.createConfiguration(ctx, null);
        assertNotNull(configuration);
        assertEquals(1, configuration.size());

        final Layer layer = type.createLayer(ctx, configuration);
        assertTrue(layer instanceof MyLayer);
        final MyLayer myLayer = (MyLayer) layer;
        assertEquals(Color.BLACK, myLayer.getPaint());
    }

    private static class MyLayer extends Layer {

        private static final Type LAYER_TYPE = new Type();

        private Color paint;

        MyLayer(Map<String, Object> configuration) {
            super(LAYER_TYPE);
            final ValueContainer vc1 = ValueContainer.createMapBacked(configuration);
            final ValueContainer vc2 = ValueContainer.createObjectBacked(this);

            for (ValueModel vcm2 : vc2.getModels()) {
                final ValueModel vcm1 = vc1.getModel(vcm2.getDescriptor().getName());
                if (vcm1 != null) {
                    try {
                        vcm2.setValue(vcm1.getValue());
                    } catch (ValidationException e) {
                        // ignore
                    }
                }
            }
        }

        Color getPaint() {
            return paint;
        }

        public static class Type extends LayerType {

            @Override
            public String getName() {
                return "My Layer";
            }

            @Override
            public boolean isValidFor(LayerContext ctx) {
                return true;
            }

            @Override
            public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
                final HashMap<String, Object> configuration = new HashMap<String, Object>();
                final ValueContainer vc = ValueContainer.createMapBacked(configuration, MyLayer.class);
                final ValueModel model = vc.getModel("paint");

                try {
                    model.setValue(Color.BLACK);
                } catch (ValidationException e) {
                    // ignore
                }

                return configuration;
            }

            @Override
            public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
                return new MyLayer(configuration);
            }
        }
    }
}
