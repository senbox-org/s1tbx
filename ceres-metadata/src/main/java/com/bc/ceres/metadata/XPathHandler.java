/*
* Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.metadata;

import com.bc.ceres.resource.ReaderResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Handles XPath expressions in velocity template files. The expressions parse an input xml file.
 *
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class XPathHandler {

    /**
     * Run a XPath query.
     * Call with $xpath.run("XPath expression", $source-XML) in the velocity template.
     * e.g. $xpath.run("//creationDate", $metadata)
     *
     * @param xpath    The XPath expression
     * @param document Either an instance of {@link ReaderResource}, {@link Element} or a raw xml {@link String}.
     * @return The demanded information from the XML document.
     */
    public String run(String xpath, Object document) {
        try {
            final Document doc = transformToDocument(document);
            return XPathFactory.newInstance().newXPath().evaluate(xpath, doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run a XPath query.
     * Call with $xpath.run("XPath expression", $source-XML) in the velocity template.
     * e.g. $xpath.run("//creationDate", $metadata)
     *
     * @param xpath    The XPath expression
     * @param document Either an instance of {@link ReaderResource}, {@link Element} or a raw xml {@link String}.
     * @return The whole XML snippet, which starts with the tag selected in the xpath expression.
     */
    public String extractXml(String xpath, Object document) throws XPathExpressionException {
        try {
            final Document doc = transformToDocument(document);
            final Node node = (Node) XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODE);

            Document newXmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Node copyNode = newXmlDocument.importNode(node, true);
            newXmlDocument.appendChild(copyNode);
            return printXmlDocument(newXmlDocument);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Document transformToDocument(Object document) {
        try {
            String docString;
            if (document instanceof ReaderResource) {
                docString = ((ReaderResource) document).getContent();
            } else if (document instanceof String) {
                docString = (String) document;
            } else if (document instanceof Element) { //used?
                DOMSource domSource = new DOMSource((Element) document);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                Properties properties = new Properties();
                properties.setProperty(OutputKeys.METHOD, "xml");
                properties.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperties(properties);
                transformer.transform(domSource, result);
                docString = writer.toString();
            } else {
                return null;
            }

            InputStream is = new ByteArrayInputStream(docString.getBytes());
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String printXmlDocument(Document document) {
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) document.getImplementation();
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        String string = lsSerializer.writeToString(document);
        return string.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n", "");
    }
}