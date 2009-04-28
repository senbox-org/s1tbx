package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.draw.AreaFigure;
import org.esa.beam.framework.draw.LineFigure;
import org.esa.beam.framework.draw.ShapeFigure;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.geom.Line2D;
import java.io.StringReader;
import java.util.Collections;

public class AbstractFigureDomConverterTest {

    @Test
    public void testLineFigure() throws ValidationException, ConversionException {
        String expectedXml = ""
                             + "  <figure class=\"org.esa.beam.framework.draw.LineFigure\">"
                             + "    <shape class =\"java.awt.geom.Line2D$Float\">"
                             + "      <x1>0.0</x1>"
                             + "      <y1>0.0</y1>"
                             + "      <x2>2.0</x2>"
                             + "      <y2>4.0</y2>"
                             + "    </shape>"
                             + "    <attributes class=\"java.util.HashMap\"/>"
                             + "  </figure>";
        final Xpp3Dom expectedDom = createDom(expectedXml);

        final Xpp3Dom dom = new Xpp3Dom("figure");

        final LineFigure figure = new LineFigure(new Line2D.Float(0, 0, 2, 4), Collections.EMPTY_MAP);
        final AbstractFigureDomConverter figureDomConverter = new AbstractFigureDomConverter();
        figureDomConverter.convertValueToDom(figure, new Xpp3DomElement(dom));
        System.out.println(new Xpp3DomElement(dom).toXml());

        assertDomEquals(expectedDom, dom);

        final LineFigure abstractFigure = (LineFigure) figureDomConverter.convertDomToValue(new Xpp3DomElement(dom),
                                                                                            null);

        assertEquals(abstractFigure.getClass(), figure.getClass());
        assertEquals(abstractFigure.getShape().getClass(), figure.getShape().getClass());
        assertEquals(abstractFigure.getShape().getBounds(), figure.getShape().getBounds());
        for (String key : figure.getAttributes().keySet()) {
            assertEquals(abstractFigure.getAttribute(key), figure.getAttribute(key));
        }
    }

    @Test
    public void testAreaFigure() throws ValidationException, ConversionException {
        String expectedXml = ""
                             + "  <figure class=\"org.esa.beam.framework.draw.AreaFigure\">"
                             + "    <shape class =\"java.awt.geom.Line2D$Float\">"
                             + "      <x1>0.0</x1>"
                             + "      <y1>0.0</y1>"
                             + "      <x2>2.0</x2>"
                             + "      <y2>4.0</y2>"
                             + "    </shape>"
                             + "    <attributes class=\"java.util.HashMap\"/>"
                             + "  </figure>";
        final Xpp3Dom expectedDom = createDom(expectedXml);

        final Xpp3Dom dom = new Xpp3Dom("figure");

        final AreaFigure figure = new AreaFigure(new Line2D.Float(0, 0, 2, 4), Collections.EMPTY_MAP);
        final AbstractFigureDomConverter figureDomConverter = new AbstractFigureDomConverter();
        figureDomConverter.convertValueToDom(figure, new Xpp3DomElement(dom));
        System.out.println(new Xpp3DomElement(dom).toXml());
        assertDomEquals(expectedDom, dom);

        final AreaFigure abstractFigure = (AreaFigure) figureDomConverter.convertDomToValue(new Xpp3DomElement(dom),
                                                                                            null);

        assertEquals(abstractFigure.getClass(), figure.getClass());
        assertEquals(abstractFigure.getShape().getClass(), figure.getShape().getClass());
        assertEquals(abstractFigure.getShape().getBounds(), figure.getShape().getBounds());
        for (String key : figure.getAttributes().keySet()) {
            assertEquals(abstractFigure.getAttribute(key), figure.getAttribute(key));
        }
    }

    @Test
    public void testShapeFigure() throws ValidationException, ConversionException {
        String expectedXml = ""
                             + "  <figure class=\"org.esa.beam.framework.draw.ShapeFigure\">"
                             + "    <shape class =\"java.awt.geom.Line2D$Float\">"
                             + "      <x1>0.0</x1>"
                             + "      <y1>0.0</y1>"
                             + "      <x2>2.0</x2>"
                             + "      <y2>4.0</y2>"
                             + "    </shape>"
                             + "    <oneDimensional>true</oneDimensional>"
                             + "    <attributes class=\"java.util.HashMap\"/>"
                             + "  </figure>";
        final Xpp3Dom expectedDom = createDom(expectedXml);

        final Xpp3Dom dom = new Xpp3Dom("figure");

        final ShapeFigure figure = new ShapeFigure(new Line2D.Float(0, 0, 2, 4), true, Collections.EMPTY_MAP);
        final AbstractFigureDomConverter figureDomConverter = new AbstractFigureDomConverter();
        figureDomConverter.convertValueToDom(figure, new Xpp3DomElement(dom));

        assertDomEquals(expectedDom, dom);

        final ShapeFigure abstractFigure = (ShapeFigure) figureDomConverter.convertDomToValue(new Xpp3DomElement(dom),
                                                                                              null);

        assertEquals(abstractFigure.getClass(), figure.getClass());
        assertEquals(abstractFigure.getShape().getClass(), figure.getShape().getClass());
        assertEquals(abstractFigure.getShape().getBounds(), figure.getShape().getBounds());
        assertEquals(abstractFigure.isOneDimensional(), figure.isOneDimensional());

        for (String key : figure.getAttributes().keySet()) {
            assertEquals(abstractFigure.getAttribute(key), figure.getAttribute(key));
        }
    }

    @Test
    public void testPlacemarkSymbol() throws ValidationException, ConversionException {
        final PlacemarkSymbol figure = PlacemarkSymbol.createDefaultGcpSymbol();
        String expectedXml = ""
                             + "<figure class=\"org.esa.beam.framework.datamodel.PlacemarkSymbol\">"
                             + "<name>GCP-Symbol</name>"
                             + "<shape class=\"java.awt.Rectangle\">"
                             + "  <x>0</x>"
                             + "  <y>0</y>"
                             + "  <width>23</width>"
                             + "  <height>23</height>"
                             + "</shape>"
                             + "<attributes class=\"java.util.HashMap\">"
                             + "  <entry>"
                             + "    <key class=\"java.lang.String\">placemarkSymbolName</key>"
                             + "    <value class=\"java.lang.String\">GCP-Symbol</value>"
                             + "  </entry>"
                             + "  <entry>"
                             + "    <key class=\"java.lang.String\">REF_POINT</key>"
                             + "    <value class=\"org.esa.beam.framework.datamodel.PixelPos\">"
                             + "      <x>11.5</x>"
                             + "      <y>11.5</y>"
                             + "    </value>"
                             + "  </entry>"
                             + "</attributes>"
                             + "<iconLocation>" + figure.getIcon().getDescription() + "</iconLocation>"
                             + "</figure>";
        final Xpp3Dom expectedDom = createDom(expectedXml);

        final Xpp3Dom dom = new Xpp3Dom("figure");

        final AbstractFigureDomConverter figureDomConverter = new AbstractFigureDomConverter();
        figureDomConverter.convertValueToDom(figure, new Xpp3DomElement(dom));
        assertDomEquals(expectedDom, dom);

        final PlacemarkSymbol abstractFigure = (PlacemarkSymbol) figureDomConverter.convertDomToValue(
                new Xpp3DomElement(dom), null);

        assertEquals(abstractFigure.getClass(), figure.getClass());
        assertEquals(abstractFigure.getShape().getClass(), figure.getShape().getClass());
        assertEquals(abstractFigure.getShape().getBounds(), figure.getShape().getBounds());
        for (String key : figure.getAttributes().keySet()) {
            assertEquals(abstractFigure.getAttribute(key), figure.getAttribute(key));
        }
        assertTrue(abstractFigure.getIcon() != null);
    }


    private Xpp3Dom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        return domWriter.getConfiguration();
    }


    private static void assertDomEquals(Xpp3Dom expected, Xpp3Dom actual) {
        assertDomEquals("", expected, actual);
    }

    private static void assertDomEquals(String message, Xpp3Dom expected, Xpp3Dom actual) {
        message = message + "/" + expected.getName();
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getChildCount(), actual.getChildCount());
        if (expected.getChildCount() == 0) {
            assertEquals(expected.getValue(), actual.getValue());
        } else {
            assertTrue(message, expected.getValue() == null || expected.getValue().trim().isEmpty());
            assertTrue(message, actual.getValue() == null || actual.getValue().trim().isEmpty());
        }
        final String[] attributeNames = expected.getAttributeNames();
        for (String expectedAttrName : attributeNames) {
            final String expectedAttrValue = expected.getAttribute(expectedAttrName);
            final String actualAttrValue = actual.getAttribute(expectedAttrName);
            assertEquals(expectedAttrValue, actualAttrValue);
        }
        final Xpp3Dom[] expectedChildren = expected.getChildren();
        final Xpp3Dom[] actualChildren = actual.getChildren();
        for (Xpp3Dom expectedChild : expectedChildren) {
            boolean success = false;
            for (Xpp3Dom actualChild : actualChildren) {
                try {
                    assertDomEquals(message, expectedChild, actualChild);
                    success = true;
                    break;
                } catch (Throwable ignored) {
                    // try next one
                }
            }
            if (!success) {
                final Xpp3Dom actualChild = actual.getChild(expectedChild.getName());
                assertNotNull(message, actualChild);
                assertDomEquals(message, expectedChild, actualChild);
            }

        }
    }

}
