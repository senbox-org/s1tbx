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
package org.esa.snap.core.datamodel;

import junit.framework.TestCase;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.placemark.PlacemarkIO;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.XmlWriter;
import org.jdom.Element;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PlacemarkTest extends TestCase {

    private static final String _NODE_ADDED = "nodeAdded";
    private static final String _NODE_CHANGED = "nodeChanged";
    private static final String _NODE_DATA_CHANGED = "ndc";
    private static final String _NODE_REMOVED = "nodeRemoved";
    private static final String _ls = SystemUtils.LS;

    private Product product;
    private List<String> eventTypes;
    private List<ProductNodeEvent> events;

    @Override
    public void setUp() {
        product = new Product("product", "t", 10, 10);
        eventTypes = new ArrayList<String>();
        events = new ArrayList<ProductNodeEvent>();
        product.addProductNodeListener(new ProductNodeListener() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (event.getSource() instanceof Placemark) {
                    eventTypes.add(_NODE_CHANGED);
                    events.add(event);
                }
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                if (event.getSource() instanceof Placemark) {
                    eventTypes.add(_NODE_DATA_CHANGED);
                    events.add(event);
                }
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                if (event.getSource() instanceof Placemark) {
                    eventTypes.add(_NODE_ADDED);
                    events.add(event);
                }
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                if (event.getSource() instanceof Placemark) {
                    eventTypes.add(_NODE_REMOVED);
                    events.add(event);
                }
            }
        });
    }


    public void testPinEvents() {
        final Placemark placemark1 = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "pinName", "pinLabel", "", null, new GeoPos(),
                                                                    product.getSceneGeoCoding());

        assertEquals(0, product.getPinGroup().getNodeCount());
        assertEquals(0, events.size());
        assertEquals(0, eventTypes.size());

        product.getPinGroup().add(placemark1);
        assertEquals(1, product.getPinGroup().getNodeCount());
        assertEquals(1, events.size());
        assertEquals(1, eventTypes.size());

        placemark1.setDescription("descPin1");
        assertEquals(1, product.getPinGroup().getNodeCount());
        assertEquals(2, events.size());
        assertEquals(2, eventTypes.size());

        placemark1.setGeoPos(new GeoPos(4, 4));
        assertEquals(1, product.getPinGroup().getNodeCount());
        assertEquals(4, events.size());
        assertEquals(4, eventTypes.size());

        placemark1.setStyleCss("fill:#CAFEBABE");
        assertEquals(1, product.getPinGroup().getNodeCount());
        assertEquals(5, events.size());
        assertEquals(5, eventTypes.size());

        product.getPinGroup().remove(placemark1);
        assertEquals(0, product.getPinGroup().getNodeCount());
        assertEquals(6, events.size());
        assertEquals(6, eventTypes.size());

        final String[] expectedEventTypes = new String[]{
                _NODE_ADDED,
                _NODE_CHANGED,
                _NODE_CHANGED,
                _NODE_CHANGED,
                _NODE_CHANGED,
                _NODE_REMOVED
        };
        final String[] currentEventTypes = eventTypes.toArray(new String[eventTypes.size()]);
        for (int i = 0; i < currentEventTypes.length; i++) {
            assertEquals("event number: " + i, expectedEventTypes[i], currentEventTypes[i]);
        }

        final String[] expectedPropertyNames = new String[]{
                null,
                ProductNode.PROPERTY_NAME_DESCRIPTION,
                Placemark.PROPERTY_NAME_PIXELPOS,
                Placemark.PROPERTY_NAME_GEOPOS,
                Placemark.PROPERTY_NAME_STYLE_CSS,
                null
        };
        final ProductNodeEvent[] currentProductNodeEvents = events.toArray(new ProductNodeEvent[events.size()]);
        for (int i = 0; i < currentProductNodeEvents.length; i++) {
            final ProductNodeEvent currentProductNodeEvent = currentProductNodeEvents[i];
            assertEquals("event number: " + i, placemark1, currentProductNodeEvent.getSourceNode());
            assertEquals("event number: " + i, expectedPropertyNames[i], currentProductNodeEvent.getPropertyName());
        }
    }

    public void testWriteXML_XmlWriterIsNull() {
        Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "pinName", "pinLabel", "", null, new GeoPos(),
                                                             product.getSceneGeoCoding());

        try {
            PlacemarkIO.writeXML(placemark, null, 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected IllegalArgumentException
        } catch (Exception e) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testWriteXML_IndentIsSmallerThanZero() {
        Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "pinName", "pinLabel", "", null, new GeoPos(), product.getSceneGeoCoding());

        try {
            int indent = -1;
            PlacemarkIO.writeXML(placemark, new XmlWriter(new StringWriter(), false), indent);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected IllegalArgumentException
        } catch (Exception e) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testWriteXML_DifferentValidIndent() {
        Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "pinName", "pinLabel", "", null, new GeoPos(4f, 87f),
                                                             product.getSceneGeoCoding());
        placemark.setDescription("pinDescription");

        StringWriter stringWriter = new StringWriter();
        PlacemarkIO.writeXML(placemark, new XmlWriter(stringWriter, false), 0);
        String expected = "" +
                "<Placemark name=\"pinName\">" + _ls +
                "    <LABEL>pinLabel</LABEL>" + _ls +
                "    <DESCRIPTION>pinDescription</DESCRIPTION>" + _ls +
                "    <LATITUDE>4.0</LATITUDE>" + _ls +
                "    <LONGITUDE>87.0</LONGITUDE>" + _ls +
                "</Placemark>" + _ls;
        assertEquals(expected, stringWriter.toString());

        stringWriter = new StringWriter();
        PlacemarkIO.writeXML(placemark, new XmlWriter(stringWriter, false), 3);
        expected = "" +
                "            <Placemark name=\"pinName\">" + _ls +
                "                <LABEL>pinLabel</LABEL>" + _ls +
                "                <DESCRIPTION>pinDescription</DESCRIPTION>" + _ls +
                "                <LATITUDE>4.0</LATITUDE>" + _ls +
                "                <LONGITUDE>87.0</LONGITUDE>" + _ls +
                "            </Placemark>" + _ls;
        assertEquals(expected, stringWriter.toString());
    }

    public void testCreatePin_FromJDOMElement() {
        final String pinName = "pin14";
        final String pinDesc = "descr";
        final double pinLat = 5.7;
        final double pinLon = 23.4;

        try {
            PlacemarkIO.createPlacemark(null, null, null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // OK
        }

        Element pinElem = new Element(DimapProductConstants.TAG_PLACEMARK);

        try {
            PlacemarkIO.createPlacemark(pinElem, PinDescriptor.getInstance(), null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        pinElem.setAttribute(DimapProductConstants.ATTRIB_NAME, pinName);

        try {
            PlacemarkIO.createPlacemark(pinElem, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        final Element latElem = new Element(DimapProductConstants.TAG_PLACEMARK_LATITUDE);
        latElem.setText(String.valueOf(pinLat));
        pinElem.addContent(latElem);

        try {
            PlacemarkIO.createPlacemark(pinElem, null, null);
            fail("IllegalArgumentException expected");
        } catch (Exception e) {
            // OK
        }

        final Element lonElem = new Element(DimapProductConstants.TAG_PLACEMARK_LONGITUDE);
        lonElem.setText(String.valueOf(pinLon));
        pinElem.addContent(lonElem);

        Placemark placemark = PlacemarkIO.createPlacemark(pinElem, PinDescriptor.getInstance(), null);
        assertNotNull("pin must be not null", placemark);
        assertEquals(pinName, placemark.getName());
        assertEquals("", placemark.getDescription());
        assertEquals(pinLat, placemark.getGeoPos().lat, 1e-15f);
        assertEquals(pinLon, placemark.getGeoPos().lon, 1e-15f);

        final Element descElem = new Element(DimapProductConstants.TAG_PLACEMARK_DESCRIPTION);
        descElem.setText(pinDesc);
        pinElem.addContent(descElem);

        placemark = PlacemarkIO.createPlacemark(pinElem, PinDescriptor.getInstance(), null);
        assertNotNull("pin must be not null", placemark);
        assertEquals(pinName, placemark.getName());
        assertEquals(pinDesc, placemark.getDescription());
        assertEquals(pinLat, placemark.getGeoPos().lat, 1e-15f);
        assertEquals(pinLon, placemark.getGeoPos().lon, 1e-15f);

        final Element fillElem = new Element(PlacemarkIO.TAG_FILL_COLOR);
        Element colorElem = new Element(DimapProductConstants.TAG_COLOR);
        colorElem.setAttribute(DimapProductConstants.ATTRIB_RED, "255");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_GREEN, "0");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_BLUE, "0");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_ALPHA, "255");
        fillElem.addContent(colorElem);
        pinElem.addContent(fillElem);

        final Element outlineElem = new Element(PlacemarkIO.TAG_OUTLINE_COLOR);
        colorElem = new Element(DimapProductConstants.TAG_COLOR);
        colorElem.setAttribute(DimapProductConstants.ATTRIB_RED, "0");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_GREEN, "0");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_BLUE, "255");
        colorElem.setAttribute(DimapProductConstants.ATTRIB_ALPHA, "255");
        outlineElem.addContent(colorElem);
        pinElem.addContent(outlineElem);

        placemark = PlacemarkIO.createPlacemark(pinElem, PinDescriptor.getInstance(), null);
        assertNotNull("pin must be not null", placemark);
        assertEquals(pinName, placemark.getName());
        assertEquals(pinDesc, placemark.getDescription());
        assertEquals(pinLat, placemark.getGeoPos().lat, 1e-15f);
        assertEquals(pinLon, placemark.getGeoPos().lon, 1e-15f);
        assertEquals("fill:255,0,0;stroke:0,0,255", placemark.getStyleCss());
    }

    public void testLabelSettings() {
        Placemark p = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "rallamann", "rallamann", "", null, new GeoPos(), product.getSceneGeoCoding());
        assertEquals("rallamann", p.getName());
        assertEquals("rallamann", p.getLabel());

        p.setLabel("schanteri");
        assertEquals("rallamann", p.getName());
        assertEquals("schanteri", p.getLabel());

        p.setLabel(null);
        assertEquals("", p.getLabel());

        p.setLabel("");
        assertEquals("", p.getLabel());

    }
}
