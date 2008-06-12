package org.esa.beam.dataio.dimap.spi;
/*
 * $Id: GeneralFilterBandPersistableTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import junit.framework.TestCase;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeneralFilterBand;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class GeneralFilterBandPersistableTest extends TestCase {

    private GeneralFilterBandPersistable _generalFilterBandPersistable;
    private static final double EPS = 1e-6;
    private Product _product;
    private Band _source;

    public void setUp() throws Exception {
        _generalFilterBandPersistable = new GeneralFilterBandPersistable();
        _product = new Product("p", "doesntMatter", 2, 2);
        _source = _product.addBand("anyBand", ProductData.TYPE_UINT16);

    }

    public void tearDown() throws Exception {
        _generalFilterBandPersistable = null;
        _product = null;
        _source = null;
    }


    public void testCreateObjectFromXml() {
        final Element xmlElement = createXmlElement();

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
        assertTrue(gfb.isNoDataValueUsed());
        assertEquals(-9999.0, gfb.getNoDataValue(), EPS);
        assertEquals(gfb.getSource().getName(), _source.getName());
        assertEquals(5, gfb.getSubWindowWidth());
        assertEquals(2, gfb.getSubWindowHeight());
        assertTrue(gfb.getOperator() instanceof GeneralFilterBand.Mean);
    }

    public void testCreateXmlFromObject() {
        final GeneralFilterBand gfb = new GeneralFilterBand("filteredBand", _source, 2, 2, GeneralFilterBand.MAX);
        gfb.setDescription("somehow explainig");
        gfb.setUnit("someUnit");
        _product.addBand(gfb);

        final Element xmlElement = _generalFilterBandPersistable.createXmlFromObject(gfb);

        assertNotNull(xmlElement);
        assertEquals(DimapProductConstants.TAG_SPECTRAL_BAND_INFO, xmlElement.getName());
        assertEquals(14, xmlElement.getChildren().size());
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_INDEX) != null);
        assertEquals(gfb.getProduct().getBandIndex(gfb.getName()), Integer.parseInt(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_NAME) != null);
        assertEquals(gfb.getName(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_DESCRIPTION) != null);
        assertEquals(gfb.getDescription(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_DATA_TYPE) != null);
        assertEquals(ProductData.getTypeString(gfb.getDataType()), xmlElement.getChildTextTrim(DimapProductConstants.TAG_DATA_TYPE));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_PHYSICAL_UNIT) != null);
        assertEquals(gfb.getUnit(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SOLAR_FLUX) != null);
        assertEquals(gfb.getSolarFlux(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_WAVELEN) != null);
        assertEquals(gfb.getSpectralWavelength(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BANDWIDTH) != null);
        assertEquals(gfb.getSpectralBandwidth(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_FACTOR) != null);
        assertEquals(gfb.getScalingFactor(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_OFFSET) != null);
        assertEquals(gfb.getScalingOffset(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_LOG_10) != null);
        assertEquals(gfb.isLog10Scaled(), Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE_USED) != null);
        assertEquals(gfb.isNoDataValueUsed(), Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE) != null);
        assertEquals(gfb.getNoDataValue(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)), EPS);

        final Element filterInfo = xmlElement.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
        assertNotNull(filterInfo);
        assertEquals("GeneralFilterBand", filterInfo.getAttributeValue("bandType"));
        assertEquals(4, filterInfo.getChildren().size());
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_SOURCE) != null);
        assertEquals(gfb.getSource().getName(), filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE));
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH) != null);
        assertEquals(gfb.getSubWindowWidth(), Integer.parseInt(filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH)));
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT) != null);
        assertEquals(gfb.getSubWindowHeight(), Integer.parseInt(filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT)));
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME) != null);
        assertEquals(gfb.getOperator().getClass().getName(), filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME));

    }

    public void testReadAndWrite() {
        final Element xmlElement = createXmlElement();

        final Object object = _generalFilterBandPersistable.createObjectFromXml(xmlElement, _product);
        _product.addBand((Band) object);

        final Element xmlFromObject = _generalFilterBandPersistable.createXmlFromObject(object);

        assertNotNull(xmlFromObject);
        final List expChildren = xmlElement.getChildren();
        final List actChildren = xmlFromObject.getChildren();
        assertEquals(expChildren.size(), actChildren.size());
        assertEqualElementList(expChildren, actChildren);
    }

    private static void assertEqualElementList(List expList, List actList) {
        for (int i = 0; i < expList.size(); i++) {
            final Element expElement = (Element) expList.get(i);
            final Element actElement = (Element) actList.get(i);
            if(expElement.getChildren().size() > 0) {
                assertEqualElementList(expElement.getChildren(), actElement.getChildren());
            } else {
                assertEquals(expElement.getName(), actElement.getName());
                assertEquals(expElement.getTextTrim(), actElement.getTextTrim());
            }
        }
    }

    private static Element createXmlElement() {
        final ArrayList contentList = new ArrayList();
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
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE, "-9999.0"));
        final ArrayList filterBandInfoList = new ArrayList();
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, "anyBand"));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH, "5"));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT, "2"));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME,
                                             "org.esa.beam.framework.datamodel.GeneralFilterBand$Mean"));
        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
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

}