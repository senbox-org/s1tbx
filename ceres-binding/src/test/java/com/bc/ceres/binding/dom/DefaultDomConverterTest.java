package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.PropertyDescriptor;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class DefaultDomConverterTest extends TestCase {

    private static final PropertyDescriptorFactory VALUE_DESCRIPTOR_FACTORY = new PropertyDescriptorFactory() {
        @Override
        public PropertyDescriptor createValueDescriptor(java.lang.reflect.Field field) {
            final PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), field.getType());
            final X xAnnotation = field.getAnnotation(X.class);
            if (xAnnotation != null) {
                descriptor.setAlias(xAnnotation.alias());
                descriptor.setItemAlias(xAnnotation.componentAlias());
                descriptor.setItemsInlined(xAnnotation.inlined());
            }
            return descriptor;
        }
    };

    public void testUnknownElement() throws ValidationException, ConversionException {
        final String xml = ""
                           + "<parameters>"
                           + "  <kornField>42</kornField>"
                           + "</parameters>";
        final Xpp3Dom dom = createDom(xml);
        final SimplePojo value = new SimplePojo();
        try {
            convertDomToValue(dom, value);
            fail("ConversionException expected");
        } catch (ConversionException e) {
        }
    }

    public void testDomToSimplePojo() throws ValidationException, ConversionException {
        final String xml = ""
                           + "<parameters>"
                           + "  <intField>42</intField>"
                           + "  <stringField>a string</stringField>"
                           + "  <doubleArrayField>1.2, 4.5, -0.034</doubleArrayField>"
                           + "  <fileField>C:/data/MER.N1</fileField>"
                           + "</parameters>";
        final Xpp3Dom dom = createDom(xml);
        final SimplePojo value = new SimplePojo();
        assertEquals(0, value.intField);
        assertEquals(null, value.stringField);
        assertNull(value.doubleArrayField);
        assertNull(value.fileField);
        convertDomToValue(dom, value);
        assertEquals(42, value.intField);
        assertEquals("a string", value.stringField);
        assertEquals(new File("C:/data/MER.N1"), value.fileField);
        assertNotNull(value.doubleArrayField);
        assertEquals(3, value.doubleArrayField.length);
        assertEquals(1.2, value.doubleArrayField[0], 1.e-10);
        assertEquals(4.5, value.doubleArrayField[1], 1.e-10);
        assertEquals(-0.034, value.doubleArrayField[2], 1.e-10);
    }

    public void testSimplePojoToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <fileField>" + new File("C:/data/dat.ini") + "</fileField>"
                                   + "  <intField>43</intField>"
                                   + "  <stringField>This is a test.</stringField>"
                                   + "  <doubleArrayField>0.1,0.2,-0.4</doubleArrayField>"
                                   + "</parameters>";
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = new double[]{0.1, 0.2, -0.4};
        value.fileField = new File("C:/data/dat.ini");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testWeirdPojoToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <finalWeird/>"
                                   + "  <weird/>"
                                   + "  <name>ernie</name>"
                                   + "</parameters>";
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        final WeirdPojo value = new WeirdPojo();
        value.weird = new Weird();
        value.name = "ernie";
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testDomToArrayPojo() {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <fileField>" + new File("C:/data/dat.ini") + "</fileField>"
                                   + "  <intField>43</intField>"
                                   + "  <stringField>This is a test.</stringField>"
                                   + "  <doubleArrayField>0.1,0.2,-0.4</doubleArrayField>"
                                   + "</parameters>";
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = new double[]{0.1, 0.2, -0.4};
        value.fileField = new File("C:/data/dat.ini");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testArrayPojoToDom() {
        ConverterRegistry.getInstance().setConverter(ArrayPojo.Member.class, new Converter<ArrayPojo.Member>() {
            @Override
            public Class<? extends ArrayPojo.Member> getValueType() {
                return ArrayPojo.Member.class;
            }

            @Override
            public ArrayPojo.Member parse(String text) throws ConversionException {
                return new ArrayPojo.Member(text);
            }

            @Override
            public String format(ArrayPojo.Member field) {
                return field.name;
            }
        });

        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <prince>bert</prince>"
                                   + "  <allies>bibo,mimi</allies>"
                                   + "</parameters>";

        final Xpp3Dom dom = new Xpp3Dom("parameters");
        final ArrayPojo arrayPojo = new ArrayPojo();
        arrayPojo.prince = new ArrayPojo.Member("bert");
        arrayPojo.allies = new ArrayPojo.Member[2];
        arrayPojo.allies[0] = new ArrayPojo.Member("bibo");
        arrayPojo.allies[1] = new ArrayPojo.Member("mimi");

        convertValueToDom(arrayPojo, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testDomToAnnotatedPojo() throws ValidationException, ConversionException {

        final String xml = ""
                           + "<parameters>"
                           + "  <targetBand>result</targetBand>"
                           + "  <targetBands>"
                           + "    <band>a</band>"
                           + "    <band>b</band>"
                           + "  </targetBands>"
                           + "</parameters>";
        final Xpp3Dom dom = createDom(xml);
        final AnnotatedPojo value = new AnnotatedPojo();
        assertNull(value.targetBandName);
        assertNull(value.targetBandNames);
        convertDomToValue(dom, value);
        assertEquals("result", value.targetBandName);
        assertNotNull(value.targetBandNames);
        assertEquals(2, value.targetBandNames.length);
        assertEquals("a", value.targetBandNames[0]);
        assertEquals("b", value.targetBandNames[1]);
    }

    public void testAnnotatedPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <targetBand>radiance_13</targetBand>"
                                   + "  <targetBands>"
                                   + "    <band>u</band>"
                                   + "    <band>v</band>"
                                   + "    <band>w</band>"
                                   + "  </targetBands>"
                                   + "</parameters>";
        final AnnotatedPojo value = new AnnotatedPojo();
        value.targetBandName = "radiance_13";
        value.targetBandNames = new String[]{"u", "v", "w"};
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testDomToComplexPojo() throws ValidationException, ConversionException {

        final String xmlString = ""
                                 + "<parameters>"
                                 + "  <simple>"
                                 + "    <intField>42</intField>"
                                 + "    <stringField>a string</stringField>"
                                 + "    <doubleArrayField>1.2, 4.5, -0.034</doubleArrayField>"
                                 + "  </simple>"
                                 + "  <annotatedPojo>"
                                 + "    <targetBand>result</targetBand>"
                                 + "    <targetBands>"
                                 + "      <band>a</band>"
                                 + "      <band>b</band>"
                                 + "    </targetBands>"
                                 + "  </annotatedPojo>"
                                 + "</parameters>";
        final Xpp3Dom dom = createDom(xmlString);
        final ComplexPojo value = new ComplexPojo();
        assertNull(value.simplePojo);
        assertNull(value.annotatedPojo);

        convertDomToValue(dom, value);

        assertEquals(42, value.simplePojo.intField);
        assertEquals("a string", value.simplePojo.stringField);
        assertNotNull(value.simplePojo.doubleArrayField);
        assertEquals(3, value.simplePojo.doubleArrayField.length);
        assertEquals(1.2, value.simplePojo.doubleArrayField[0], 1.e-10);
        assertEquals(4.5, value.simplePojo.doubleArrayField[1], 1.e-10);
        assertEquals(-0.034, value.simplePojo.doubleArrayField[2], 1.e-10);

        assertEquals("result", value.annotatedPojo.targetBandName);
        assertNotNull(value.annotatedPojo.targetBandNames);
        assertEquals(2, value.annotatedPojo.targetBandNames.length);
        assertEquals("a", value.annotatedPojo.targetBandNames[0]);
        assertEquals("b", value.annotatedPojo.targetBandNames[1]);

    }

    public void testComplexPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <simple>"
                                   + "    <intField>87</intField>"
                                   + "    <stringField>Test, test, test!</stringField>"
                                   + "    <doubleArrayField>0.5,1.0</doubleArrayField>"
                                   + "    <fileField/>"
                                   + "  </simple>"
                                   + "  <annotatedPojo>"
                                   + "    <targetBand>reflec_4</targetBand>"
                                   + "    <targetBands>"
                                   + "      <band>real</band>"
                                   + "      <band>imag</band>"
                                   + "    </targetBands>"
                                   + "  </annotatedPojo>"
                                   + "</parameters>";
        final ComplexPojo value = new ComplexPojo();
        value.simplePojo = new SimplePojo();
        value.simplePojo.intField = 87;
        value.simplePojo.stringField = "Test, test, test!";
        value.simplePojo.doubleArrayField = new double[]{0.5, 1.0};
        value.annotatedPojo = new AnnotatedPojo();
        value.annotatedPojo.targetBandName = "reflec_4";
        value.annotatedPojo.targetBandNames = new String[]{"real", "imag"};
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testDomToInlinedArrayPojo() throws ValidationException, ConversionException {

        final String xml = ""
                           + "<parameters>"
                           + "  <endmember>"
                           + "    <name>Land</name>"
                           + "    <size>4</size>"
                           + "    <wavelengths>820,830,840,850</wavelengths>"
                           + "    <radiances>220,230,240,250</radiances>"
                           + "  </endmember>"
                           + "  <defaultEndmember>"      // note the order!
                           + "    <name>Fallback</name>"
                           + "    <size>4</size>"
                           + "    <wavelengths>820,830,840,850</wavelengths>"
                           + "    <radiances>420,430,440,450</radiances>"
                           + "  </defaultEndmember>"
                           + "  <endmember>"
                           + "    <name>Water</name>"
                           + "    <size>4</size>"
                           + "    <wavelengths>820,830,840,850</wavelengths>"
                           + "    <radiances>620,630,640,650</radiances>"
                           + "  </endmember>"
                           + "  <endmember>"
                           + "    <name>Cloud</name>"
                           + "    <size>4</size>"
                           + "    <wavelengths>820,830,840,850</wavelengths>"
                           + "    <radiances>920,930,940,950</radiances>"
                           + "  </endmember>"
                           + "</parameters>";

        final Xpp3Dom dom = createDom(xml);
        final InlinedArrayPojo value = new InlinedArrayPojo();
        assertNull(value.defaultEndmember);
        assertNull(value.endmembers);

        convertDomToValue(dom, value);

        assertNotNull(value.defaultEndmember);
        assertEquals("Fallback", value.defaultEndmember.name);
        assertEquals(4, value.defaultEndmember.size);
        assertNotNull(value.defaultEndmember.wavelengths);
        assertNotNull(value.defaultEndmember.radiances);

        assertNotNull(value.endmembers);
        assertEquals(3, value.endmembers.length);

        assertEquals("Land", value.endmembers[0].name);
        assertEquals(4, value.endmembers[0].size);
        assertNotNull(value.endmembers[0].wavelengths);
        assertNotNull(value.endmembers[0].radiances);

        assertEquals("Water", value.endmembers[1].name);
        assertEquals(4, value.endmembers[1].size);
        assertNotNull(value.endmembers[1].wavelengths);
        assertNotNull(value.endmembers[1].radiances);

        assertEquals("Cloud", value.endmembers[2].name);
        assertEquals(4, value.endmembers[2].size);
        assertNotNull(value.endmembers[2].wavelengths);
        assertNotNull(value.endmembers[2].radiances);
    }

    public void testInlinedArrayPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <defaultEndmember>"
                                   + "    <name>Fallback</name>"
                                   + "    <size>4</size>"
                                   + "    <wavelengths>820.0,830.0,840.0,850.0</wavelengths>"
                                   + "    <radiances>420.0,430.0,440.0,450.0</radiances>"
                                   + "  </defaultEndmember>"
                                   + "  <endmember>"
                                   + "    <name>Land</name>"
                                   + "    <size>4</size>"
                                   + "    <wavelengths>820.0,830.0,840.0,850.0</wavelengths>"
                                   + "    <radiances>220.0,230.0,240.0,250.0</radiances>"
                                   + "  </endmember>"
                                   + "  <endmember>"
                                   + "    <name>Water</name>"
                                   + "    <size>4</size>"
                                   + "    <wavelengths>820.0,830.0,840.0,850.0</wavelengths>"
                                   + "    <radiances>620.0,630.0,640.0,650.0</radiances>"
                                   + "  </endmember>"
                                   + "  <endmember>"
                                   + "    <name>Cloud</name>"
                                   + "    <size>4</size>"
                                   + "    <wavelengths>820.0,830.0,840.0,850.0</wavelengths>"
                                   + "    <radiances>920.0,930.0,940.0,950.0</radiances>"
                                   + "  </endmember>"
                                   + "</parameters>";

        final InlinedArrayPojo value = new InlinedArrayPojo();
        value.defaultEndmember = new Endmember("Fallback", new double[]{820, 830, 840, 850},
                                               new double[]{420, 430, 440, 450});
        value.endmembers = new Endmember[3];
        value.endmembers[0] = new Endmember("Land", new double[]{820, 830, 840, 850}, new double[]{220, 230, 240, 250});
        value.endmembers[1] = new Endmember("Water", new double[]{820, 830, 840, 850},
                                            new double[]{620, 630, 640, 650});
        value.endmembers[2] = new Endmember("Cloud", new double[]{820, 830, 840, 850},
                                            new double[]{920, 930, 940, 950});

        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testInterfaceFieldsPojoToDom() {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <shape1 class=\"java.awt.Rectangle\" >"
                                   + "    <x>10</x>"
                                   + "    <y>10</y>"
                                   + "    <width>20</width>"
                                   + "    <height>25</height>"
                                   + "  </shape1>"
                                   + "  <shape2 class=\"java.awt.geom.Line2D$Float\" >"
                                   + "    <x1>0.0</x1>"
                                   + "    <y1>10.3</y1>"
                                   + "    <x2>15.7</x2>"
                                   + "    <y2>34.6</y2>"
                                   + "  </shape2>"
                                   + "</parameters>";
        final InterfaceFieldsPojo interfacePojo = new InterfaceFieldsPojo(new Rectangle(10, 10, 20, 25),
                                                                          new Line2D.Float(0.0f, 10.3f, 15.7f, 34.6f));
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(interfacePojo, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void testDomToInterfaceFieldsPojo() throws ValidationException, ConversionException {
        final String xml = ""
                           + "<parameters>"
                           + "  <shape1 class=\"java.awt.Rectangle\" >"
                           + "    <x>10</x>"
                           + "    <y>10</y>"
                           + "    <width>20</width>"
                           + "    <height>25</height>"
                           + "  </shape1>"
                           + "  <shape2 class=\"java.awt.geom.Line2D$Float\" >"
                           + "    <x1>0.0</x1>"
                           + "    <y1>10.3</y1>"
                           + "    <x2>15.7</x2>"
                           + "    <y2>34.6</y2>"
                           + "  </shape2>"
                           + "</parameters>";

        final Xpp3Dom dom = createDom(xml);
        final InterfaceFieldsPojo value = new InterfaceFieldsPojo(null, null);
        assertNull(value.shape1);
        assertNull(value.shape2);

        convertDomToValue(dom, value);

        assertNotNull(value.shape1);
        assertNotNull(value.shape1 instanceof Rectangle);
        final Rectangle shape1 = (Rectangle) value.shape1;
        assertEquals(10, shape1.x);
        assertEquals(10, shape1.x);
        assertEquals(20, shape1.width);
        assertEquals(25, shape1.height);

        assertNotNull(value.shape2);
        assertNotNull(value.shape2 instanceof Line2D.Float);
        final Line2D.Float shape2 = (Line2D.Float) value.shape2;
        assertEquals(0.0f, shape2.x1);
        assertEquals(10.3f, shape2.y1);
        assertEquals(15.7f, shape2.x2);
        assertEquals(34.6f, shape2.y2);
    }

    public void doNotTestMapFieldPojoToDom() {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <map class=\"java.util.HashMap\" >"
                                   + "    <entry>"
                                   + "      <key class=\"java.lang.String\">Bibo</key>"
                                   + "      <value class=\"java.awt.Rectangle\">"
                                   + "        <x>10</x>"
                                   + "        <y>10</y>"
                                   + "        <width>20</width>"
                                   + "        <height>25</height>"
                                   + "      </value>"
                                   + "    </entry>"
                                   + "    <entry>"
                                   + "      <key class=\"java.lang.Integer\">12345</key>"
                                   + "      <value class=\"java.awt.Color\">12,40,123</value>"
                                   + "    </entry>"
                                   + "  </map>"
                                   + "</parameters>";

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("Bibo", new Rectangle(10, 10, 20, 25));
        map.put(12345, new Color(12, 40, 123));
        final MapFieldPojo mapFieldPojo = new MapFieldPojo(map);
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(mapFieldPojo, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void doNotTestDomToMapFieldPojo() throws ValidationException, ConversionException {
        final String xml = ""
                           + "<parameters>"
                           + "  <map class=\"java.util.HashMap\" >"
                           + "    <entry>"
                           + "      <key class=\"java.lang.String\">Bibo</key>"
                           + "      <value class=\"java.awt.Rectangle\">"
                           + "        <x>10</x>"
                           + "        <y>10</y>"
                           + "        <width>20</width>"
                           + "        <height>25</height>"
                           + "      </value>"
                           + "    </entry>"
                           + "    <entry>"
                           + "      <key class=\"java.lang.Integer\">12345</key>"
                           + "      <value class=\"java.awt.Color\">12,40,123</value>"
                           + "    </entry>"
                           + "  </map>"
                           + "</parameters>";

        final Xpp3Dom dom = createDom(xml);
        final MapFieldPojo mapFieldPojo = new MapFieldPojo(null);
        convertDomToValue(dom, mapFieldPojo);

        final Map<?, ?> map = mapFieldPojo.map;
        assertTrue(map instanceof HashMap);
        assertEquals(2, map.size());
    }

    public void doNotTestCollectionFieldPojoToDom() {
        final String expectedXml = ""
                                   + "<parameters>"
                                   + "  <collection class=\"java.util.Stack\">"
                                   + "    <item class=\"java.awt.Rectangle\">"
                                   + "      <x>0</x>"
                                   + "      <y>0</y>"
                                   + "      <width>10</width>"
                                   + "      <height>10</height>"
                                   + "    </item>"
                                   + "    <item class=\"java.awt.geom.Line2D$Double\">"
                                   + "      <x1>0.0</x1>"
                                   + "      <y1>0.0</y1>"
                                   + "      <x2>10.0</x2>"
                                   + "      <y2>10.0</y2>"
                                   + "    </item>"
                                   + "    <item class=\"java.awt.geom.Arc2D$Double\">"
                                   + "      <type>1</type>"
                                   + "      <x>0.0</x>"
                                   + "      <y>0.0</y>"
                                   + "      <width>10.0</width>"
                                   + "      <height>10.0</height>"
                                   + "      <start>2.0</start>"
                                   + "      <extent>3.0</extent>"
                                   + "    </item>"
                                   + "  </collection>"
                                   + "</parameters>";
        Collection<Shape> stack = new Stack<Shape>();
        stack.add(new Rectangle(0, 0, 10, 10));
        stack.add(new Line2D.Double(0, 0, 10, 10));
        stack.add(new Arc2D.Double(0, 0, 10, 10, 2, 3, Arc2D.CHORD));
        final CollectionFieldPojo collectionFieldPojo = new CollectionFieldPojo(stack);
        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(collectionFieldPojo, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    public void doNotTestDomToCollectionFieldPojo() throws ValidationException, ConversionException {
        final String xml = ""
                           + "<parameters>"
                           + "  <collection class=\"java.util.Stack\">"
                           + "    <item class=\"java.awt.Rectangle\">"
                           + "      <x>0</x>"
                           + "      <y>0</y>"
                           + "      <width>10</width>"
                           + "      <height>10</height>"
                           + "    </item>"
                           + "    <item class=\"java.awt.geom.Line2D$Double\">"
                           + "      <x1>0.0</x1>"
                           + "      <y1>0.0</y1>"
                           + "      <x2>10.0</x2>"
                           + "      <y2>10.0</y2>"
                           + "    </item>"
                           + "    <item class=\"java.awt.geom.Arc2D$Double\">"
                           + "      <type>1</type>"
                           + "      <x>0.0</x>"
                           + "      <y>0.0</y>"
                           + "      <width>10.0</width>"
                           + "      <height>10.0</height>"
                           + "      <start>2.0</start>"
                           + "      <extent>3.0</extent>"
                           + "    </item>"
                           + "  </collection>"
                           + "</parameters>";

        final Xpp3Dom dom = createDom(xml);
        final CollectionFieldPojo collectionFieldPojo = new CollectionFieldPojo(null);
        convertDomToValue(dom, collectionFieldPojo);

        assertTrue(collectionFieldPojo.collection instanceof Stack);
        Stack stack = (Stack) collectionFieldPojo.collection;
        assertEquals(3, stack.size());
        assertTrue(stack.get(0) instanceof Rectangle);
        assertTrue(stack.get(1) instanceof Line2D.Double);
        assertTrue(stack.get(2) instanceof java.awt.geom.Arc2D.Double);
    }

    private Xpp3Dom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        return domWriter.getConfiguration();
    }

    public static void convertValueToDom(Object value, Xpp3Dom parentElement) {
        try {
            new DefaultDomConverter(value.getClass(), VALUE_DESCRIPTOR_FACTORY).convertValueToDom(value,
                                                                                                  new Xpp3DomElement(
                                                                                                          parentElement));
        } catch (ConversionException e) {
            e.printStackTrace();
        }
    }


    public static void convertDomToValue(Xpp3Dom parentElement, Object value) throws ConversionException,
                                                                                     ValidationException {

        new DefaultDomConverter(value.getClass(), VALUE_DESCRIPTOR_FACTORY).convertDomToValue(
                new Xpp3DomElement(parentElement), value);
    }

    public static class SimplePojo {

        File fileField;
        String stringField;
        int intField;
        double[] doubleArrayField;
    }

    public static class AnnotatedPojo {

        @X(alias = "targetBand")
        String targetBandName;

        @X(alias = "targetBands", componentAlias = "band")
        String[] targetBandNames;
    }

    public static class ComplexPojo {

        @X(alias = "simple")
        SimplePojo simplePojo;
        AnnotatedPojo annotatedPojo;
    }

    public static class ArrayPojo {

        Member prince;
        Member[] allies;

        public static class Member {

            private final String name;

            public Member(String name) {
                this.name = name;
            }
        }
    }

    public static class InlinedArrayPojo {

        Endmember defaultEndmember;

        @X(componentAlias = "endmember", inlined = true)
        Endmember[] endmembers;
    }

    public static class InterfaceFieldsPojo {

        Shape shape1;
        Shape shape2;

        public InterfaceFieldsPojo(Shape shape1, Shape shape2) {
            this.shape1 = shape1;
            this.shape2 = shape2;
        }
    }

    public static class MapFieldPojo {

        Map<?, ?> map;

        public MapFieldPojo(Map<?, ?> map) {
            this.map = map;
        }
    }

    public static class CollectionFieldPojo {

        Collection collection;

        public CollectionFieldPojo(Collection collection) {
            this.collection = collection;
        }
    }

    public static class WeirdPojo {

        final Weird finalWeird = new Weird();
        Weird weird;
        String name;
        transient Date timeStamp = new Date();
    }

    public static class Weird {

        static final Weird staticFinalWeird = new Weird();
        static Weird staticWeird = new Weird();
    }

    @Retention(value = RetentionPolicy.RUNTIME)
            @interface X {

        String alias() default "";

        String componentAlias() default "";

        boolean inlined() default false;
    }


    public static class Endmember {

        public Endmember() {
        }

        public Endmember(String name, double[] wavelengths, double[] radiances) {
            this.name = name;
            this.size = wavelengths.length;
            this.wavelengths = wavelengths;
            this.radiances = radiances;
        }

        String name;
        int size;
        double[] wavelengths;
        double[] radiances;
    }

    public static void assertEquals(Xpp3Dom expected, Xpp3Dom actual) {
        assertEquals("", expected, actual);
    }

    public static void assertEquals(String message, Xpp3Dom expected, Xpp3Dom actual) {
        message = message + "/" + expected.getName();
        assertEquals(message, expected.getName(), actual.getName());
        assertEquals(message, expected.getChildCount(), actual.getChildCount());
        if (expected.getChildCount() == 0) {
            assertEquals(message, expected.getValue(), actual.getValue());
        } else {
            assertTrue(message, expected.getValue() == null || expected.getValue().trim().isEmpty());
            assertTrue(message, actual.getValue() == null || actual.getValue().trim().isEmpty());
        }
        final String[] attributeNames = expected.getAttributeNames();
        for (String expectedAttrName : attributeNames) {
            final String expectedAttrValue = expected.getAttribute(expectedAttrName);
            final String actualAttrValue = actual.getAttribute(expectedAttrName);
            assertEquals(message, expectedAttrValue, actualAttrValue);
        }
        final Xpp3Dom[] expectedChildren = expected.getChildren();
        final Xpp3Dom[] actualChildren = actual.getChildren();
        for (Xpp3Dom expectedChild : expectedChildren) {
            boolean success = false;
            for (Xpp3Dom actualChild : actualChildren) {
                try {
                    assertEquals(message, expectedChild, actualChild);
                    success = true;
                    break;
                } catch (AssertionFailedError e) {
                    // try next one
                }
            }
            if (!success) {
                final Xpp3Dom actualChild = actual.getChild(expectedChild.getName());
                assertNotNull(message, actualChild);
                assertEquals(message, expectedChild, actualChild);
            }

        }
    }
}
