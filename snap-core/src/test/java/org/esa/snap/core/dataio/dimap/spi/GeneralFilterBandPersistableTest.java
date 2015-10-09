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

package org.esa.snap.core.dataio.dimap.spi;

import junit.framework.TestCase;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class GeneralFilterBandPersistableTest extends TestCase {

    private GeneralFilterBandPersistable _generalFilterBandPersistable;
    private static final double EPS = 1e-6;
    private Product _product;
    private Band _source;

    @Override
    public void setUp() throws Exception {
        _generalFilterBandPersistable = new GeneralFilterBandPersistable();
        _product = new Product("p", "doesntMatter", 2, 2);
        _source = _product.addBand("anyBand", ProductData.TYPE_UINT16);

    }

    @Override
    public void tearDown() throws Exception {
        _generalFilterBandPersistable = null;
        _product = null;
        _source = null;
    }

    public void testCreateObjectFromXml_Version_1_0() {
        Element xmlElement = createXmlElement(GeneralFilterBandPersistable.VERSION_1_0);
        assertCreateRightGeneralFilterBand(xmlElement);
    }

    public void testCreateObjectFromXml_Version_1_1() {
        final Element xmlElement = createXmlElement(GeneralFilterBandPersistable.VERSION_1_1);
        assertCreateRightGeneralFilterBand(xmlElement);
    }

    public void testCreateXmlFromObject() {
        final GeneralFilterBand gfb = new GeneralFilterBand("filteredBand", _source, GeneralFilterBand.OpType.MAX, new Kernel(2, 2, new double[2 * 2]), 1);
        gfb.setDescription("somehow explainig");
        gfb.setUnit("someUnit");
        _product.addBand(gfb);

        final Element xmlElement = _generalFilterBandPersistable.createXmlFromObject(gfb);

        assertNotNull(xmlElement);
        assertEquals(DimapProductConstants.TAG_SPECTRAL_BAND_INFO, xmlElement.getName());
        assertEquals(14, xmlElement.getChildren().size());
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_INDEX) != null);
        assertEquals(gfb.getProduct().getBandIndex(gfb.getName()),
                     Integer.parseInt(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_NAME) != null);
        assertEquals(gfb.getName(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_DESCRIPTION) != null);
        assertEquals(gfb.getDescription(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_DATA_TYPE) != null);
        assertEquals(ProductData.getTypeString(gfb.getDataType()),
                     xmlElement.getChildTextTrim(DimapProductConstants.TAG_DATA_TYPE));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_PHYSICAL_UNIT) != null);
        assertEquals(gfb.getUnit(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SOLAR_FLUX) != null);
        assertEquals(gfb.getSolarFlux(),
                     Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_WAVELEN) != null);
        assertEquals(gfb.getSpectralWavelength(),
                     Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BANDWIDTH) != null);
        assertEquals(gfb.getSpectralBandwidth(),
                     Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_FACTOR) != null);
        assertEquals(gfb.getScalingFactor(),
                     Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_OFFSET) != null);
        assertEquals(gfb.getScalingOffset(),
                     Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_LOG_10) != null);
        assertEquals(gfb.isLog10Scaled(),
                     Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE_USED) != null);
        assertEquals(gfb.isNoDataValueUsed(),
                     Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE) != null);
        assertEquals(gfb.getNoDataValue(),
                     Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)), EPS);

        final Element filterInfo = xmlElement.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
        assertNotNull(filterInfo);
        assertEquals(GeneralFilterBandPersistable.GENERAL_FILTER_BAND_TYPE, filterInfo.getAttributeValue(
                GeneralFilterBandPersistable.ATTRIBUTE_BAND_TYPE));
        assertEquals(GeneralFilterBandPersistable.VERSION_1_2, filterInfo.getAttributeValue(
                GeneralFilterBandPersistable.ATTRIBUTE_VERSION));
        assertEquals(3, filterInfo.getChildren().size());
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_SOURCE) != null);
        assertEquals(gfb.getSource().getName(), filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE));
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_KERNEL) != null);
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_OP_TYPE) != null);
        assertEquals(gfb.getOpType().toString(),
                     filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_OP_TYPE));
    }

    public void testReadAndWrite() {
        final Element xmlElement = createXmlElement(GeneralFilterBandPersistable.VERSION_1_2);

        final Object object = _generalFilterBandPersistable.createObjectFromXml(xmlElement, _product);
        _product.addBand((Band) object);

        final Element xmlFromObject = _generalFilterBandPersistable.createXmlFromObject(object);

        assertNotNull(xmlFromObject);
        final List expChildren = xmlElement.getChildren();
        final List actChildren = xmlFromObject.getChildren();
        assertEquals(expChildren.size(), actChildren.size());
        assertEqualElement(xmlElement, xmlFromObject);
    }

    private void assertCreateRightGeneralFilterBand(Element xmlElement) {
        final Object object = _generalFilterBandPersistable.createObjectFromXml(xmlElement, _product);
        _product.addBand((Band) object);

        assertNotNull(object);
        assertTrue(object instanceof GeneralFilterBand);
        final GeneralFilterBand gfb = (GeneralFilterBand) object;
        assertEquals(1, _product.getBandIndex(gfb.getName()));
        assertEquals(-1, gfb.getSpectralBandIndex());
        assertEquals("filtered_coffee", gfb.getName());
        assertEquals("with milk & sugar", gfb.getDescription());
        assertEquals(ProductData.TYPE_FLOAT32, gfb.getDataType());
        assertEquals("l", gfb.getUnit());
        assertEquals(0.0, gfb.getSolarFlux(), EPS);
        assertEquals(0.0, gfb.getSpectralWavelength(), EPS);
        assertEquals(0.0, gfb.getSpectralBandwidth(), EPS);
        assertEquals(1.0, gfb.getScalingFactor(), EPS);
        assertEquals(0.0, gfb.getScalingOffset(), EPS);
        assertFalse(gfb.isLog10Scaled());
        assertEquals(gfb.getSource().getName(), _source.getName());
        assertEquals(5, gfb.getStructuringElement().getWidth());
        assertEquals(5, gfb.getStructuringElement().getHeight());
        assertEquals(gfb.getOpType(), GeneralFilterBand.OpType.MEAN);
    }


    private static void assertEqualElement(Element expElement, Element actElement) {
        assertNotNull(expElement);
        assertNotNull(actElement);
        if (!expElement.getChildren().isEmpty()) {
            final List expList = expElement.getChildren();
            for (Object expElem : expList) {
                final Element expSubElement = (Element) expElem;
                final Element actSubElement = actElement.getChild(expSubElement.getName());
                assertNotNull(String.format("missing %s", expSubElement), actSubElement);
                assertEqualElement(expSubElement, actSubElement);
            }
        } else {
            assertEquals(expElement.getName(), actElement.getName());
            assertEquals(expElement.getTextTrim(), actElement.getTextTrim());
        }
    }

    private Element createXmlElement(final String version) {
        final List<Element> contentList = new ArrayList<Element>(16);
        contentList.add(createElement(DimapProductConstants.TAG_BAND_INDEX, "1"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_NAME, "filtered_coffee"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_DESCRIPTION, "with milk & sugar"));
        contentList.add(createElement(DimapProductConstants.TAG_DATA_TYPE, "float32"));
        contentList.add(createElement(DimapProductConstants.TAG_PHYSICAL_UNIT, "l"));
        contentList.add(createElement(DimapProductConstants.TAG_SOLAR_FLUX, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_WAVELEN, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_BANDWIDTH, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_FACTOR, "1.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_OFFSET, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_LOG_10, "false"));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE_USED, "true"));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE,
                                      String.valueOf(_source.getGeophysicalNoDataValue())));
        final List<Element> filterBandInfoList = new ArrayList<Element>(5);
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, "anyBand"));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_OP_TYPE, "MEAN"));
        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        filterBandInfo.setAttribute(GeneralFilterBandPersistable.ATTRIBUTE_BAND_TYPE,
                                    GeneralFilterBandPersistable.GENERAL_FILTER_BAND_TYPE);
        if (GeneralFilterBandPersistable.VERSION_1_0.equals(version)) {
            // Version 1.0
            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH, "5"));
            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT, "5"));
        } else if (GeneralFilterBandPersistable.VERSION_1_1.equals(version)) {
            // Version 1.1
            filterBandInfo.setAttribute(GeneralFilterBandPersistable.ATTRIBUTE_VERSION, version);
            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_SIZE, "5"));
        } else {
            // Version 1.2
            filterBandInfo.setAttribute(GeneralFilterBandPersistable.ATTRIBUTE_VERSION, version);
            final List<Element> filterKernelList = new ArrayList<Element>(5);
            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_WIDTH, "5"));
            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_HEIGHT, "5"));
            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_X_ORIGIN, "2"));
            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_Y_ORIGIN, "2"));
            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_DATA,"" +
                                                       "0,0,0,0,0," +
                                                       "0,0,0,0,0," +
                                                       "0,0,0,0,0," +
                                                       "0,0,0,0,0," +
                                                       "0,0,0,0,0"
            ));
            final Element kernelElement = new Element(DimapProductConstants.TAG_FILTER_KERNEL);
            kernelElement.addContent(filterKernelList);
            filterBandInfoList.add(kernelElement);
        }
        filterBandInfo.addContent(filterBandInfoList);
        contentList.add(filterBandInfo);

        final Element root = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        root.setContent(contentList);
        return root;
    }

    private static Element createElement(String tagName, String text) {
        final Element elem = new Element(tagName);
        elem.setText(text);
        return elem;
    }

    private static Element createElement(String tagName, boolean[] se) {
        final Element elem = new Element(tagName);
        StringBuilder text = new StringBuilder();
        for (boolean b : se) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(b ? "1" : "0");
        }
        elem.setText(text.toString());
        return elem;
    }
}
