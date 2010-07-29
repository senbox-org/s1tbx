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
package org.esa.beam.dataio.netcdf;

import junit.framework.TestCase;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;


public class FormatNameTest extends TestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testFormatnameInModuleXmlIsTheSameAsInConstants() throws IOException {
        final Enumeration<URL> urlEnumeration = ClassLoader.getSystemResources("module.xml");
        InputStream stream = null;
        while (urlEnumeration.hasMoreElements()) {
            final URL url = urlEnumeration.nextElement();
            System.out.println("url = " + url);
            if (url.toString().contains("beam-netcdf")) {
                stream = new FileInputStream(url.getFile());
                System.out.println("  SELECTED");
                break;
            }
        }

        assertNotNull(stream);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(stream);
            final DOMBuilder domBuilder = new DOMBuilder();
            final org.jdom.Document jDoc = domBuilder.build(document);
            final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(jDoc, System.out);

            final NodeList byTagName = document.getElementsByTagName("formatName");
            assertEquals(3, byTagName.getLength());
            final Node node = byTagName.item(0);
            assertNotNull(node);
            assertEquals(Constants.FORMAT_NAME, node.getTextContent());
        } catch (Exception e) {
            fail("should not come here");
        }
    }
}
