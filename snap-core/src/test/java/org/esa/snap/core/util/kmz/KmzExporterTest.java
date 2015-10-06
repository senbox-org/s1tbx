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

package org.esa.snap.core.util.kmz;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class KmzExporterTest {

    @Test
    public void testKmlHierarchy() throws ParserConfigurationException, IOException, SAXException {
        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlFolder kmlFolder1 = new KmlFolder("folder1", null);

        KmlDocument kmlDocument1_1 = new KmlDocument("document1_1", null);
        kmlFolder1.addChild(kmlDocument1_1);
        KmlGroundOverlay layer1_1_1 = new KmlGroundOverlay("layer1_1_1", layer, boundBox);
        kmlDocument1_1.addChild(layer1_1_1);
        KmlGroundOverlay layer1_1_2 = new KmlGroundOverlay("layer1_1_2", layer, boundBox);
        kmlDocument1_1.addChild(layer1_1_2);

        KmlGroundOverlay kmlLayer1_2 = new KmlGroundOverlay("layer1_2", layer, boundBox);
        kmlFolder1.addChild(kmlLayer1_2);

        KmlFolder kmlFFolder1_2 = new KmlFolder("folder1_3", null);
        kmlFolder1.addChild(kmlFFolder1_2);
        KmlGroundOverlay layer1_2_1 = new KmlGroundOverlay("layer1_3_1", layer, boundBox);
        kmlFFolder1_2.addChild(layer1_2_1);
        KmlGroundOverlay layer1_2_2 = new KmlGroundOverlay("layer1_3_2", layer, boundBox);
        kmlFFolder1_2.addChild(layer1_2_2);


        String xml = KmzExporter.createKml(kmlFolder1);

        final Document document = convertToDocument(xml);
        final Element root = document.getRootElement();
        Namespace nameSpace = root.getNamespace();
        assertEquals("http://earth.google.com/kml/2.0", nameSpace.getURI());
        final List children = root.getChildren();
        assertEquals(1, children.size());

        final Element folder1 = root.getChild("Folder", nameSpace);
        assertEquals(4, folder1.getChildren().size());
        final Element nameChild = folder1.getChild("name", nameSpace);
        assertEquals("folder1", nameChild.getValue());

        List overlays;

        final Element document1_1 = folder1.getChild("Document", nameSpace);
        assertEquals("document1_1", document1_1.getChildText("name", nameSpace));
        overlays = document1_1.getChildren("GroundOverlay", nameSpace);
        assertEquals(2, overlays.size());
        validateGroundOverlay("layer1_1_1", overlays.get(0), nameSpace);
        validateGroundOverlay("layer1_1_2", overlays.get(1), nameSpace);

        overlays = folder1.getChildren("GroundOverlay", nameSpace);
        assertEquals(1, overlays.size());
        validateGroundOverlay("layer1_2", overlays.get(0), nameSpace);

        Element folder1_3 = folder1.getChild("Folder", nameSpace);
        assertEquals("folder1_3", folder1_3.getChildText("name", nameSpace));
        overlays = folder1_3.getChildren("GroundOverlay", nameSpace);
        assertEquals(2, overlays.size());
        validateGroundOverlay("layer1_3_1", overlays.get(0), nameSpace);
        validateGroundOverlay("layer1_3_2", overlays.get(1), nameSpace);
    }

    @Test
    public void testKmlPlacemarks() throws IOException, SAXException, ParserConfigurationException {
        KmlDocument doc1 = new KmlDocument("pins", null);
        doc1.addChild(new KmlPlacemark("placemark 1", null, new Point2D.Double(80.0, 89.0)));
        doc1.addChild(new KmlPlacemark("placemark 2", null, new Point2D.Double(70.0, 70.0)));
        doc1.addChild(new KmlPlacemark("placemark 3", null, new Point2D.Double(60.0, 60.0)));
        doc1.addChild(new KmlPlacemark("placemark 4", null, new Point2D.Double(40.0, 50.0)));

        final String xml = KmzExporter.createKml(doc1);
        final Document document = convertToDocument(xml);

        assertNotNull(document);
        final Element root = document.getRootElement();
        assertNotNull(root);
        Namespace nameSpace = root.getNamespace();

        final Element doc = root.getChild("Document", nameSpace);
        assertNotNull(doc);

        final List placemarks = doc.getChildren("Placemark", nameSpace);
        assertEquals(4, placemarks.size());
        for (int i = 0; i < placemarks.size(); i++) {
            validatePlacemark((Element) placemarks.get(i), "placemark " + (i + 1), nameSpace);
        }
    }

    @Test
    public void testLegend() throws IOException, SAXException, ParserConfigurationException {
        final String xml = KmzExporter.createKml(new KmlScreenOverlay("Legend", new DummyTestOpImage(4, 10)));
        final Document document = convertToDocument(xml);
        assertNotNull(document);

        final Element root = document.getRootElement();
        assertNotNull(root);
        Namespace nameSpace = root.getNamespace();

        final Element screenOverlay = root.getChild("ScreenOverlay", nameSpace);
        assertNotNull(screenOverlay);
        assertEquals("Legend", screenOverlay.getChildText("name", nameSpace));
        final Element icon = screenOverlay.getChild("Icon", nameSpace);
        assertNotNull(icon);
        assertEquals("Legend.png", icon.getChildText("href", nameSpace));
        assertNotNull(screenOverlay.getChild("overlayXY", nameSpace));
        assertNotNull(screenOverlay.getChild("screenXY", nameSpace));

    }

    private void validatePlacemark(Element placemark, String s, Namespace nameSpace) {
        assertEquals(s, placemark.getChildText("name", nameSpace));
        final Element point = placemark.getChild("Point", nameSpace);
        assertNotNull(point);
        assertNotNull(point.getChild("coordinates", nameSpace));
    }

    private void validateGroundOverlay(String name, Object overlay, Namespace namespace) {
        Element groundOverlay = (Element) overlay;
        assertEquals("GroundOverlay", groundOverlay.getName());
        assertEquals(3, groundOverlay.getChildren().size());
        assertEquals(name, groundOverlay.getChildText("name", namespace));
        Element icon = groundOverlay.getChild("Icon", namespace);
        assertEquals(name + ".png", icon.getChildText("href", namespace));
        final Element latLonBox = groundOverlay.getChild("LatLonBox", namespace);
        assertNotNull(latLonBox);
        assertEquals("70.0", latLonBox.getChildText("north", namespace));
        assertEquals("30.0", latLonBox.getChildText("south", namespace));
        assertEquals("20.0", latLonBox.getChildText("east", namespace));
        assertEquals("0.0", latLonBox.getChildText("west", namespace));
    }

    // for debugging
    @SuppressWarnings({"UnusedDeclaration"})
    private void printDocument(Document document) {
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(false);
        prettyFormat.setOmitEncoding(true);
        prettyFormat.setOmitDeclaration(true);
        prettyFormat.setTextMode(Format.TextMode.NORMALIZE);

        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        final String xml = xmlOutputter.outputString(document);
        System.out.println(xml);
    }

    private Document convertToDocument(String xmlString) throws ParserConfigurationException, SAXException,
                                                                IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return new DOMBuilder().build(builder.parse(new ByteArrayInputStream(xmlString.getBytes())));
    }


}
