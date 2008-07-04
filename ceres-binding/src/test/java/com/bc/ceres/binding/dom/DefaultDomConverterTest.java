package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

public class DefaultDomConverterTest extends TestCase {

    private static final ClassFieldDescriptorFactory VALUE_DESCRIPTOR_FACTORY = new ClassFieldDescriptorFactory() {
        public ValueDescriptor createValueDescriptor(Field field) {
            final ValueDescriptor descriptor = new ValueDescriptor(field.getName(), field.getType());
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

    public void testUnmarshalSimplePojo() throws ValidationException, ConversionException {
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

    public void testMarshalSimplePojo() throws ValidationException, ConversionException {
        final String expectedXml = ""
                + "<parameters>"
                + "  <fileField>"+new File("C:/data/dat.ini") + "</fileField>"
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


    public void testUnmarshalAnnotatedPojo() throws ValidationException, ConversionException {

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

    public void testMarshalAnnotatedPojo() throws ValidationException, ConversionException {

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

    public void testUnmarshalComplexPojo() throws ValidationException, ConversionException {

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

    public void testMarshalComplexPojo() throws ValidationException, ConversionException {

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

    public void testUnmarshalInlinedArrayPojo() throws ValidationException, ConversionException {

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

    public void testMarshalInlinedArrayPojo() throws ValidationException, ConversionException {

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
        value.defaultEndmember = new Endmember("Fallback", new double[]{820, 830, 840, 850}, new double[]{420, 430, 440, 450});
        value.endmembers = new Endmember[3];
        value.endmembers[0] = new Endmember("Land", new double[]{820, 830, 840, 850}, new double[]{220, 230, 240, 250});
        value.endmembers[1] = new Endmember("Water", new double[]{820, 830, 840, 850}, new double[]{620, 630, 640, 650});
        value.endmembers[2] = new Endmember("Cloud", new double[]{820, 830, 840, 850}, new double[]{920, 930, 940, 950});

        final Xpp3Dom dom = new Xpp3Dom("parameters");
        convertValueToDom(value, dom);
        assertEquals(createDom(expectedXml), dom);
    }

    private Xpp3Dom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        return domWriter.getConfiguration();
    }

    public static void convertValueToDom(Object value, Xpp3Dom parentElement) {
        new DefaultDomConverter(value.getClass(), VALUE_DESCRIPTOR_FACTORY).convertValueToDom(value, new Xpp3DomElement(parentElement));
    }


    public static void convertDomToValue(Xpp3Dom parentElement, Object value) throws ConversionException, ValidationException {

        new DefaultDomConverter(value.getClass(), VALUE_DESCRIPTOR_FACTORY).convertDomToValue(new Xpp3DomElement(parentElement), value);
    }

    public static class Xpp3DomElement implements DomElement {
        private final Xpp3Dom xpp3Dom;

        public Xpp3DomElement(Xpp3Dom xpp3Dom) {
            this.xpp3Dom = xpp3Dom;
        }

        public Xpp3Dom getXpp3Dom() {
            return xpp3Dom;
        }

        public void setAttribute(String name, String value) {
            xpp3Dom.setAttribute(name, value);
        }

        public DomElement createChild(String name) {
            final Xpp3DomElement child = new Xpp3DomElement(new Xpp3Dom(name));
            addChild(child);
            return child;
        }

        public void addChild(DomElement childElement) {
            xpp3Dom.addChild(((Xpp3DomElement)childElement).getXpp3Dom());
        }

        public DomElement getChild(int index) {
            return new Xpp3DomElement(xpp3Dom.getChild(index));
        }

        public int getChildCount() {
            return xpp3Dom.getChildCount();
        }

        public void setValue(String value) {
            xpp3Dom.setValue(value);
        }

        public String getName() {
            return xpp3Dom.getName();
        }

        public String getValue() {
            return xpp3Dom.getValue();
        }

        public String getAttribute(String attributeName) {
            return xpp3Dom.getAttribute(attributeName);
        }

        public String[] getAttributeNames() {
            return xpp3Dom.getAttributeNames();
        }

        public DomElement getParent() {
            return new Xpp3DomElement(xpp3Dom.getParent());
        }

        public DomElement getChild(String elementName) {
            return new Xpp3DomElement(xpp3Dom.getChild(elementName));
        }

        public DomElement[] getChildren() {
            final Xpp3Dom[] xppChildren = xpp3Dom.getChildren();
            return createXpp3DomElementArray(xppChildren);
        }

        public DomElement[] getChildren(String elementName) {
            final Xpp3Dom[] xppChildren = xpp3Dom.getChildren(elementName);
            return createXpp3DomElementArray(xppChildren);
        }

        public String toXml() {
            final StringWriter writer = new StringWriter(8 * 1024);
            new HierarchicalStreamCopier().copy(new XppDomReader(xpp3Dom), new PrettyPrintWriter(writer));
            return writer.toString();
        }

        private Xpp3DomElement[] createXpp3DomElementArray(Xpp3Dom[] xppChildren) {
            final Xpp3DomElement[] domElements = new Xpp3DomElement[xppChildren.length];
            for (int i = 0; i < xppChildren.length; i++) {
                domElements[i] = new Xpp3DomElement(xppChildren[i]);
            }
            return domElements;
        }
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

    public static class InlinedArrayPojo {
        Endmember defaultEndmember;
        @X(componentAlias = "endmember", inlined = true)
        Endmember[] endmembers;
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
