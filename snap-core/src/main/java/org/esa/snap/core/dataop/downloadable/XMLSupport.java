/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.dataop.downloadable;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jan 23, 2008
 * Time: 2:45:16 PM
 * To change this template use File | Settings | File Templates.
 */
public final class XMLSupport {

    public static void SaveXML(final Document doc, final String filePath) throws IOException {

        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try (final FileWriter writer = new FileWriter(filePath)){

            outputter.output(doc, writer);
            writer.close();
        }
    }

    public static Document LoadXML(final InputStream inputStream) throws IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document w3cDocument = null;

        final DOMBuilder domBuilder = new DOMBuilder();

        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            w3cDocument = builder.parse(inputStream);

            return domBuilder.build(w3cDocument);
        } catch (MalformedURLException e) {
            final String msg = "Cannot parse xml file path : " + e.getMessage() +
                    '\n' + inputStream +
                    "\n\nPlease check the characters being used and if your operating system locale is set correctly";
            SystemUtils.LOG.severe(msg);
            throw new IOException(msg);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Path to xml is not valid: " + e.getMessage());
            throw e;
        } catch (SAXException | ParserConfigurationException e) {
            SystemUtils.LOG.severe("cannot parse xml : " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    public static Document LoadXML(final String filePath) throws IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document w3cDocument = null;

        final DOMBuilder domBuilder = new DOMBuilder();

        try {
            // handle spaces in the path
            final String path = filePath.replaceAll(" ", "%20");
            final DocumentBuilder builder = factory.newDocumentBuilder();
            w3cDocument = builder.parse(filePath);

            return domBuilder.build(w3cDocument);
        } catch (MalformedURLException e) {
            final String msg = "Cannot parse xml file path : " + e.getMessage() +
                    '\n' + filePath +
                    "\n\nPlease check the characters being used and if your operating system locale is set correctly";
            SystemUtils.LOG.severe(msg);
            throw new IOException(msg);
        } catch (IOException e) {
            //System.out.println("Path to xml is not valid: " + e.getMessage());
            throw e;
        } catch (SAXException | ParserConfigurationException e) {
            SystemUtils.LOG.severe("cannot parse xml : " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    public static Document LoadXMLFromResource(final String filePath, final Class theClass) throws IOException {

        final java.net.URL resURL = theClass.getClassLoader().getResource(filePath);
        if (resURL != null)
            return LoadXML(resURL.toString());
        return null;
    }


    public static void metadataElementToDOMElement(final MetadataElement metadataElem, final Element domElem) {

        final MetadataElement[] metaElements = metadataElem.getElements();
        for (MetadataElement childMetaElem : metaElements) {
            final Element childDomElem = new Element(childMetaElem.getName());
            metadataElementToDOMElement(childMetaElem, childDomElem);
            domElem.addContent(childDomElem);
        }

        final MetadataAttribute[] metaAttributes = metadataElem.getAttributes();
        for (MetadataAttribute childMetaAttrib : metaAttributes) {
            final Element childDomElem = new Element("attrib");
            setAttribute(childDomElem, "name", childMetaAttrib.getName());
            setAttribute(childDomElem, "value", childMetaAttrib.getData().getElemString());
            if ((childMetaAttrib.getUnit() != null && childMetaAttrib.getUnit().equalsIgnoreCase("utc")) ||
                    childMetaAttrib.getData() instanceof ProductData.UTC)
                setAttribute(childDomElem, "type", String.valueOf(ProductData.TYPE_UTC));
            else if (childMetaAttrib.getData() instanceof ProductData.ASCII)
                setAttribute(childDomElem, "type", String.valueOf(ProductData.TYPE_ASCII));
            else
                setAttribute(childDomElem, "type", String.valueOf(childMetaAttrib.getDataType()));
            setAttribute(childDomElem, "unit", childMetaAttrib.getUnit());
            setAttribute(childDomElem, "desc", childMetaAttrib.getDescription());
            domElem.addContent(childDomElem);
        }
    }

    private static void setAttribute(final Element childDomElem, final String tag, final String val) {
        if (val != null)
            childDomElem.setAttribute(tag, val);
    }

    private static void domElementToMetadataElement(final Element domElem, final MetadataElement metadataElem) {

        final List<Content> children = domElem.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;
                final List<Content> grandChildren = child.getContent();
                if (!grandChildren.isEmpty()) {
                    final MetadataElement newElem = new MetadataElement(child.getName());
                    domElementToMetadataElement(child, newElem);
                    metadataElem.addElement(newElem);
                }

                if (child.getName().equals("attrib")) {
                    addAttribute(metadataElem, child);
                }
            }
        }
    }

    // todo incomplete
    private static void addAttribute(final MetadataElement root, final Element domElem) {

        final Attribute nameAttrib = domElem.getAttribute("name");
        final Attribute valueAttrib = domElem.getAttribute("value");
        final Attribute typeAttrib = domElem.getAttribute("type");
        final Attribute unitAttrib = domElem.getAttribute("unit");
        final Attribute descAttrib = domElem.getAttribute("desc");

        if (nameAttrib == null || valueAttrib == null)
            return;

        final MetadataAttribute attribute = new MetadataAttribute(nameAttrib.getName(), ProductData.TYPE_ASCII, 1);
        attribute.getData().setElems(valueAttrib.getValue());

        if (unitAttrib != null)
            attribute.setUnit(unitAttrib.getValue());
        if (descAttrib != null)
            attribute.setDescription(descAttrib.getValue());

        root.addAttribute(attribute);
    }

    public static String getAttrib(final Element elem, final String tag) {
        final Attribute attrib = elem.getAttribute(tag);
        if (attrib != null)
            return attrib.getValue();
        return "";
    }

    public static Element getElement(final Element root, final String name) throws IOException {
        final List<Content> children = root.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element elem = (Element) aChild;
                if (elem.getName().equalsIgnoreCase(name))
                    return elem;
            }
        }
        throw new IOException("Element " + name + " not found");
    }

    public static Text getElementText(final Element root) throws IOException {
        final List<Content> children = root.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Text) {
                return (Text) aChild;
            }
        }
        throw new IOException("Element Text not found");
    }

    public static String[] getStringList(final Element elem) {
        final List<String> array = new ArrayList<>();
        final List<Content> contentList = elem.getContent();
        for (Object o : contentList) {
            if (o instanceof Element) {
                array.add(((Element) o).getName());
            }
        }
        return array.toArray(new String[array.size()]);
    }
}
