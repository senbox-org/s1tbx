/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.DefaultPropertySetDescriptor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;

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

import static org.junit.Assert.*;

public class DefaultDomConverterTest {

    private static final PropertyDescriptorFactory PROPERTY_DESCRIPTOR_FACTORY = new PropertyDescriptorFactory() {
        @Override
        public PropertyDescriptor createValueDescriptor(java.lang.reflect.Field field) {
            final PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), field.getType());
            final X xAnnotation = field.getAnnotation(X.class);
            if (xAnnotation != null) {
                descriptor.setAlias(xAnnotation.alias());
                descriptor.setItemAlias(xAnnotation.itemAlias());
                if (xAnnotation.defaultValue() != null && !xAnnotation.defaultValue().isEmpty()) {
                    try {
                        descriptor.setDefaultConverter();
                        descriptor.setDefaultValue(descriptor.getConverter().parse(xAnnotation.defaultValue()));
                    } catch (ConversionException e) {
                        throw new IllegalStateException("Failed to convert default value.", e);
                    }
                }
                if (xAnnotation.domConverter() != DomConverter.class) {
                    DomConverter domConverter;
                    try {
                        domConverter = xAnnotation.domConverter().newInstance();
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to create domConverter.", t);
                    }
                    descriptor.setDomConverter(domConverter);
                }
            }
            return descriptor;
        }
    };

    @Test
    public void testUnknownElement() throws Exception {
        final String xml = ""
                           + "<parameters>"
                           + "  <kornField>42</kornField>"
                           + "</parameters>";
        final XppDom dom = createDom(xml);
        final SimplePojo value = new SimplePojo();
        try {
            convertDomToValue(dom, value);
            fail("ConversionException expected");
        } catch (ConversionException ignored) {
        }
    }

    @Test
    public void testDomToSimplePojo() throws Exception {
        final String xml = ""
                           + "<parameters>"
                           + "  <fileField>C:/data/MER.N1</fileField>"
                           + "  <stringField>a string</stringField>"
                           + "  <intField>42</intField>"
                           + "  <doubleArrayField>1.2, 4.5, -0.034</doubleArrayField>"
                           + "</parameters>";
        final XppDom dom = createDom(xml);
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
        assertEquals(1.2, value.doubleArrayField[0], 1.0e-10);
        assertEquals(4.5, value.doubleArrayField[1], 1.0e-10);
        assertEquals(-0.034, value.doubleArrayField[2], 1.0e-10);
    }

    @Test
    public void testSimplePojoToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <fileField>" + new File("C:/data/dat.ini") + "</fileField>\n"
                                   + "  <stringField>This is a test.</stringField>\n"
                                   + "  <intField>43</intField>\n"
                                   + "  <doubleArrayField>0.1,0.2,-0.4</doubleArrayField>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = new double[]{0.1, 0.2, -0.4};
        value.fileField = new File("C:/data/dat.ini");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testSimplePojoWithNullArrayToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <fileField>" + new File("C:/data/dat.ini") + "</fileField>\n"
                                   + "  <stringField>This is a test.</stringField>\n"
                                   + "  <intField>43</intField>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = null;
        value.fileField = new File("C:/data/dat.ini");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testSimplePojoWithNullObjectToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <stringField>This is a test.</stringField>\n"
                                   + "  <intField>43</intField>\n"
                                   + "  <doubleArrayField>0.1,0.2,-0.4</doubleArrayField>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = new double[]{0.1, 0.2, -0.4};
        value.fileField = null;
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testArrayInitIfAlreadyInitialized() throws Exception {
        final String parameters = ""
                                  + "<parameters>\n"
                                  + "  <targetBands>\n"
                                  + "    <band>band_a</band>\n"
                                  + "    <band>band_b</band>\n"
                                  + "    <band>band_c</band>\n"
                                  + "  </targetBands>\n"
                                  + "  <targetBand>notImportant</targetBand>\n"
                                  + "  <defaultBandName>ignored</defaultBandName>\n"
                                  + "</parameters>";
        final XppDom dom = createDom(parameters);
        AnnotatedPojo annotatedPojo = new AnnotatedPojo();
        annotatedPojo.targetBandNames = new String[]{"band_1","band_2"};
        convertDomToValue(dom, annotatedPojo);
        assertArrayEquals(new String[]{"band_a", "band_b", "band_c"}, annotatedPojo.targetBandNames);
    }

    @Test
    public void testArrayWithDomConverterPojoToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <allies>\n"
                                   + "    <member>\n"
                                   + "      <name>bibo</name>\n"
                                   + "    </member>\n"
                                   + "    <member>\n"
                                   + "      <name>mimi</name>\n"
                                   + "    </member>\n"
                                   + "  </allies>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final ArrayWithDomConverterPojo value = new ArrayWithDomConverterPojo();
        value.allies = new Member[2];
        value.allies[0] = new Member("bibo");
        value.allies[1] = new Member("mimi");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testArrayWithDomConverterPojoToDom_NullValue() throws ValidationException, ConversionException {
        final String expectedXml = "<parameters/>";
        final XppDom dom = new XppDom("parameters");
        final ArrayWithDomConverterPojo value = new ArrayWithDomConverterPojo();
        value.allies = null;
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }


    @Test
    public void testWeirdPojoToDom() throws ValidationException, ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <weird/>\n"
                                   + "  <name>ernie</name>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final WeirdPojo value = new WeirdPojo();
        value.weird = new Weird();
        value.name = "ernie";
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToArrayPojo() throws ConversionException {
        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <fileField>" + new File("C:/data/dat.ini") + "</fileField>\n"
                                   + "  <stringField>This is a test.</stringField>\n"
                                   + "  <intField>43</intField>\n"
                                   + "  <doubleArrayField>0.1,0.2,-0.4</doubleArrayField>\n"
                                   + "</parameters>";
        final XppDom dom = new XppDom("parameters");
        final SimplePojo value = new SimplePojo();
        value.intField = 43;
        value.stringField = "This is a test.";
        value.doubleArrayField = new double[]{0.1, 0.2, -0.4};
        value.fileField = new File("C:/data/dat.ini");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testArrayPojoToDom() throws ConversionException {
        ConverterRegistry.getInstance().setConverter(Member.class, new Converter<Member>() {
            @Override
            public Class<? extends Member> getValueType() {
                return Member.class;
            }

            @Override
            public Member parse(String text) throws ConversionException {
                return new Member(text);
            }

            @Override
            public String format(Member field) {
                return field.name;
            }
        });

        final String expectedXml = "<parameters>\n" +
                                   "  <prince>bert</prince>\n" +
                                   "  <allies>bibo,mimi</allies>\n" +
                                   "</parameters>";

        final XppDom dom = new XppDom("parameters");
        final ArrayPojo arrayPojo = new ArrayPojo();
        arrayPojo.prince = new Member("bert");
        arrayPojo.allies = new Member[2];
        arrayPojo.allies[0] = new Member("bibo");
        arrayPojo.allies[1] = new Member("mimi");

        convertValueToDom(arrayPojo, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToAnnotatedPojo() throws Exception {

        final String xml = ""
                           + "<parameters>"
                           + "  <targetBand>result</targetBand>"
                           + "  <targetBands>"
                           + "    <band>a</band>"
                           + "    <band>b</band>"
                           + "  </targetBands>"
                           + "</parameters>";
        final XppDom dom = createDom(xml);
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

    @Test
    public void testAnnotatedPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = "<parameters>\n" +
                                   "  <targetBand>radiance_13</targetBand>\n" +
                                   "  <defaultBandName>radiance_12</defaultBandName>\n" +
                                   "  <targetBands>\n" +
                                   "    <band>u</band>\n" +
                                   "    <band>v</band>\n" +
                                   "    <band>w</band>\n" +
                                   "  </targetBands>\n" +
                                   "</parameters>";
        final AnnotatedPojo value = new AnnotatedPojo();
        value.targetBandName = "radiance_13";
        value.defaultBandName = "radiance_12";
        value.targetBandNames = new String[]{"u", "v", "w"};
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToEnumPojo() throws Exception {

        final String xml = ""
                           + "<parameters>"
                           + "  <character>KERMIT</character>"
                           + "</parameters>";
        final XppDom dom = createDom(xml);
        final EnumPojo value = new EnumPojo();
        assertNull(value.character);
        convertDomToValue(dom, value);
        assertEquals(EnumPojo.Muppet.KERMIT, value.character);
    }

    @Test
    public void testEnumPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = ""
                                   + "<parameters>\n"
                                   + "  <character>MISS_PIGGY</character>\n"
                                   + "</parameters>";
        final EnumPojo value = new EnumPojo();
        value.character = DefaultDomConverterTest.EnumPojo.Muppet.MISS_PIGGY;
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToComplexPojo() throws Exception {

        final String xmlString = ""
                                 + "<parameters>"
                                 + "  <simple>"
                                 + "    <stringField>a string</stringField>"
                                 + "    <intField>42</intField>"
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
        final XppDom dom = createDom(xmlString);
        final ComplexPojo value = new ComplexPojo();
        assertNull(value.simplePojo);
        assertNull(value.annotatedPojo);

        convertDomToValue(dom, value);

        assertEquals(42, value.simplePojo.intField);
        assertEquals("a string", value.simplePojo.stringField);
        assertNotNull(value.simplePojo.doubleArrayField);
        assertEquals(3, value.simplePojo.doubleArrayField.length);
        assertEquals(1.2, value.simplePojo.doubleArrayField[0], 1.0e-10);
        assertEquals(4.5, value.simplePojo.doubleArrayField[1], 1.0e-10);
        assertEquals(-0.034, value.simplePojo.doubleArrayField[2], 1.0e-10);

        assertEquals("result", value.annotatedPojo.targetBandName);
        assertEquals("reflec_13", value.annotatedPojo.defaultBandName);
        assertNotNull(value.annotatedPojo.targetBandNames);
        assertEquals(2, value.annotatedPojo.targetBandNames.length);
        assertEquals("a", value.annotatedPojo.targetBandNames[0]);
        assertEquals("b", value.annotatedPojo.targetBandNames[1]);

    }

    @Test
    public void testComplexPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = "<parameters>\n" +
                                   "  <simple>\n" +
                                   "    <stringField>Test, test, test!</stringField>\n" +
                                   "    <intField>87</intField>\n" +
                                   "    <doubleArrayField>0.5,1.0</doubleArrayField>\n" +
                                   "  </simple>\n" +
                                   "  <annotatedPojo>\n" +
                                   "    <targetBand>reflec_4</targetBand>\n" +
                                   "    <defaultBandName>reflec_27</defaultBandName>\n" +
                                   "    <targetBands>\n" +
                                   "      <band>real</band>\n" +
                                   "      <band>imag</band>\n" +
                                   "    </targetBands>\n" +
                                   "  </annotatedPojo>\n" +
                                   "</parameters>";
        final ComplexPojo value = new ComplexPojo();
        value.simplePojo = new SimplePojo();
        value.simplePojo.intField = 87;
        value.simplePojo.stringField = "Test, test, test!";
        value.simplePojo.doubleArrayField = new double[]{0.5, 1.0};
        value.annotatedPojo = new AnnotatedPojo();
        value.annotatedPojo.targetBandName = "reflec_4";
        value.annotatedPojo.defaultBandName = "reflec_27";
        value.annotatedPojo.targetBandNames = new String[]{"real", "imag"};
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToNamedItemArrayPojo() throws Exception {

        final String xml = ""
                           + "<parameters>"
                           + "  <endmembers>"
                           + "    <endmember>"
                           + "      <name>Land</name>"
                           + "      <size>4</size>"
                           + "      <wavelengths>820,830,840,850</wavelengths>"
                           + "      <radiances>220,230,240,250</radiances>"
                           + "    </endmember>"
                           + "    <endmember>"
                           + "      <name>Water</name>"
                           + "      <size>4</size>"
                           + "      <wavelengths>820,830,840,850</wavelengths>"
                           + "      <radiances>620,630,640,650</radiances>"
                           + "    </endmember>"
                           + "    <endmember>"
                           + "      <name>Cloud</name>"
                           + "      <size>4</size>"
                           + "      <wavelengths>820,830,840,850</wavelengths>"
                           + "      <radiances>920,930,940,950</radiances>"
                           + "    </endmember>"
                           + "  </endmembers>"
                           + "  <defaultEndmember>"      // note the order!
                           + "    <name>Fallback</name>"
                           + "    <size>4</size>"
                           + "    <wavelengths>820,830,840,850</wavelengths>"
                           + "    <radiances>420,430,440,450</radiances>"
                           + "  </defaultEndmember>"
                           + "</parameters>";

        final XppDom dom = createDom(xml);
        final NamedItemArrayPojo value = new NamedItemArrayPojo();
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

    @Test
    public void testNamedItemArrayPojoToDom() throws ValidationException, ConversionException {

        final String expectedXml = "" +
                                   "<parameters>\n" +
                                   "  <defaultEndmember>\n" +
                                   "    <name>Fallback</name>\n" +
                                   "    <size>4</size>\n" +
                                   "    <wavelengths>820.0,830.0,840.0,850.0</wavelengths>\n" +
                                   "    <radiances>420.0,430.0,440.0,450.0</radiances>\n" +
                                   "  </defaultEndmember>\n" +
                                   "  <endmembers>\n" +
                                   "    <endmember>\n" +
                                   "      <name>Land</name>\n" +
                                   "      <size>4</size>\n" +
                                   "      <wavelengths>820.0,830.0,840.0,850.0</wavelengths>\n" +
                                   "      <radiances>220.0,230.0,240.0,250.0</radiances>\n" +
                                   "    </endmember>\n" +
                                   "    <endmember>\n" +
                                   "      <name>Water</name>\n" +
                                   "      <size>4</size>\n" +
                                   "      <wavelengths>820.0,830.0,840.0,850.0</wavelengths>\n" +
                                   "      <radiances>620.0,630.0,640.0,650.0</radiances>\n" +
                                   "    </endmember>\n" +
                                   "    <endmember>\n" +
                                   "      <name>Cloud</name>\n" +
                                   "      <size>4</size>\n" +
                                   "      <wavelengths>820.0,830.0,840.0,850.0</wavelengths>\n" +
                                   "      <radiances>920.0,930.0,940.0,950.0</radiances>\n" +
                                   "    </endmember>\n" +
                                   "  </endmembers>\n" +
                                   "</parameters>";

        final NamedItemArrayPojo value = new NamedItemArrayPojo();
        value.defaultEndmember = new Endmember("Fallback", new double[]{820, 830, 840, 850},
                                               new double[]{420, 430, 440, 450});
        value.endmembers = new Endmember[3];
        value.endmembers[0] = new Endmember("Land", new double[]{820, 830, 840, 850}, new double[]{220, 230, 240, 250});
        value.endmembers[1] = new Endmember("Water", new double[]{820, 830, 840, 850},
                                            new double[]{620, 630, 640, 650});
        value.endmembers[2] = new Endmember("Cloud", new double[]{820, 830, 840, 850},
                                            new double[]{920, 930, 940, 950});

        final XppDom dom = new XppDom("parameters");
        convertValueToDom(value, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testInterfaceFieldsPojoToDom() throws ConversionException {
        final String expectedXml = "<parameters>\n" +
                                   "  <shape1 class=\"java.awt.Rectangle\">\n" +
                                   "    <x>10</x>\n" +
                                   "    <y>10</y>\n" +
                                   "    <width>20</width>\n" +
                                   "    <height>25</height>\n" +
                                   "  </shape1>\n" +
                                   "  <shape2 class=\"java.awt.geom.Line2D$Float\">\n" +
                                   "    <x1>0.0</x1>\n" +
                                   "    <y1>10.3</y1>\n" +
                                   "    <x2>15.7</x2>\n" +
                                   "    <y2>34.6</y2>\n" +
                                   "  </shape2>\n" +
                                   "</parameters>";
        final InterfaceFieldsPojo interfacePojo = new InterfaceFieldsPojo(new Rectangle(10, 10, 20, 25),
                                                                          new Line2D.Float(0.0f, 10.3f, 15.7f, 34.6f));
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(interfacePojo, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    @Test
    public void testDomToInterfaceFieldsPojo() throws Exception {
        final String xml = "<parameters>\n" +
                           "  <shape1 class=\"java.awt.Rectangle\">\n" +
                           "    <x>10</x>\n" +
                           "    <y>10</y>\n" +
                           "    <width>20</width>\n" +
                           "    <height>25</height>\n" +
                           "  </shape1>\n" +
                           "  <shape2 class=\"java.awt.geom.Line2D$Float\">\n" +
                           "    <x1>0.0</x1>\n" +
                           "    <y1>10.3</y1>\n" +
                           "    <x2>15.7</x2>\n" +
                           "    <y2>34.6</y2>\n" +
                           "  </shape2>\n" +
                           "</parameters>";

        final XppDom dom = createDom(xml);
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
        assertEquals(0.0f, shape2.x1, 1e-5);
        assertEquals(10.3f, shape2.y1, 1e-5);
        assertEquals(15.7f, shape2.x2, 1e-5);
        assertEquals(34.6f, shape2.y2, 1e-5);
    }

    public void doNotTestMapFieldPojoToDom() throws ConversionException {
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
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(mapFieldPojo, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    public void doNotTestDomToMapFieldPojo() throws Exception {
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

        final XppDom dom = createDom(xml);
        final MapFieldPojo mapFieldPojo = new MapFieldPojo(null);
        convertDomToValue(dom, mapFieldPojo);

        final Map<?, ?> map = mapFieldPojo.map;
        assertTrue(map instanceof HashMap);
        assertEquals(2, map.size());
    }

    public void doNotTestCollectionFieldPojoToDom() throws ConversionException {
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
        final XppDom dom = new XppDom("parameters");
        convertValueToDom(collectionFieldPojo, dom);
        assertEquals(expectedXml, getXml(dom));
    }

    public void doNotTestDomToCollectionFieldPojo() throws Exception {
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

        final XppDom dom = createDom(xml);
        final CollectionFieldPojo collectionFieldPojo = new CollectionFieldPojo(null);
        convertDomToValue(dom, collectionFieldPojo);

        assertTrue(collectionFieldPojo.collection instanceof Stack);
        Stack stack = (Stack) collectionFieldPojo.collection;
        assertEquals(3, stack.size());
        assertTrue(stack.get(0) instanceof Rectangle);
        assertTrue(stack.get(1) instanceof Line2D.Double);
        assertTrue(stack.get(2) instanceof java.awt.geom.Arc2D.Double);
    }

    private XppDom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml), new MXParser()), domWriter);
        return domWriter.getConfiguration();
    }

    public static String getXml(XppDom dom) {
        XppDomElement domElement = new XppDomElement(dom);
        return domElement.toXml();
    }

    public static void convertValueToDom(Object value, XppDom parentElement) throws ConversionException {
        DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), PROPERTY_DESCRIPTOR_FACTORY);
        XppDomElement domElement = new XppDomElement(parentElement);
        domConverter.convertValueToDom(value, domElement);
    }


    public static void convertDomToValue(XppDom parentElement, Object value) throws Exception {
        DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), PROPERTY_DESCRIPTOR_FACTORY);
        XppDomElement domElement = new XppDomElement(parentElement);
        domConverter.convertDomToValue(domElement, value);
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

        @X(defaultValue = "reflec_13")
        String defaultBandName;

        @X(alias = "targetBands", itemAlias = "band")
        String[] targetBandNames;
    }

    public static class ComplexPojo {

        @X(alias = "simple")
        SimplePojo simplePojo;
        AnnotatedPojo annotatedPojo;
    }

    public static class MemberDomConverter implements DomConverter {

        private final DefaultDomConverter memberConverter = new DefaultDomConverter(Member.class);

        @Override
        public Class<?> getValueType() {
            return Member.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                ValidationException {
            DomElement[] children = parentElement.getChildren("member");
            Member[] members = new Member[children.length];
            for (int i = 0; i < children.length; i++) {
                members[i] = (Member) memberConverter.convertDomToValue(children[i], null);
            }
            return members;
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            Member[] members = (Member[]) value;
            for (Member member : members) {
                DomElement aggregator = parentElement.createChild("member");
                memberConverter.convertValueToDom(member, aggregator);
            }
        }
    }

    public static class ArrayWithDomConverterPojo {

        @X(domConverter = MemberDomConverter.class)
        Member[] allies;
    }

    public static class ArrayPojo {

        Member prince;
        Member[] allies;

    }

    public static class Member {

        private String name;

        public Member(String name) {
            this.name = name;
        }
    }

    public static class EnumPojo {

        private enum Muppet {
            KERMIT,
            ANIMAL,
            MISS_PIGGY
        }

        Muppet character;
    }

    public static class NamedItemArrayPojo {

        Endmember defaultEndmember;

        @X(itemAlias = "endmember")
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

        String itemAlias() default "";

        String defaultValue() default "";

        Class<? extends DomConverter> domConverter() default DomConverter.class;
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

}
