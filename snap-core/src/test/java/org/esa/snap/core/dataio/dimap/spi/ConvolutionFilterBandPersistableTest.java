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
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ArrayUtils;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class ConvolutionFilterBandPersistableTest extends TestCase {

    private ConvolutionFilterBandPersistable _convolutionFilterBandPersistable;
    private static final double EPS = 1e-6;
    private Product _product;
    private Band _source;

    @Override
    public void setUp() throws Exception {
        _convolutionFilterBandPersistable = new ConvolutionFilterBandPersistable();
        _product = new Product("p", "doesntMatter", 2, 2);
        _source = _product.addBand("anyBand", ProductData.TYPE_UINT16);
    }

    @Override
    public void tearDown() throws Exception {
        _convolutionFilterBandPersistable = null;
        _product = null;
        _source = null;
    }

    public void testCreateObjectFromXml() {
        final Element xmlElement = createXmlElement();

        final Object object = _convolutionFilterBandPersistable.createObjectFromXml(xmlElement, _product);
        _product.addBand((Band)object);

        assertNotNull(object);
        assertTrue(object instanceof ConvolutionFilterBand);
        final ConvolutionFilterBand cfb = (ConvolutionFilterBand) object;
        assertEquals(-1, cfb.getSpectralBandIndex());
        assertEquals(1, _product.getBandIndex(cfb.getName()));
        assertEquals("aBand", cfb.getName());
        assertEquals("this is a band", cfb.getDescription());
        assertEquals(ProductData.TYPE_FLOAT32, cfb.getDataType());
        assertEquals("l", cfb.getUnit());
        assertEquals(0.0, cfb.getSolarFlux(), EPS);
        assertEquals(0.0, cfb.getSpectralWavelength(), EPS);
        assertEquals(0.0, cfb.getSpectralBandwidth(), EPS);
        assertEquals(1.0, cfb.getScalingFactor(), EPS);
        assertEquals(0.0, cfb.getScalingOffset(), EPS);
        assertFalse(cfb.isLog10Scaled());
        assertTrue(cfb.isNoDataValueUsed());
        assertEquals(cfb.getSource().getName(), _source.getName());
        assertEquals(3, cfb.getKernel().getWidth());
        assertEquals(3, cfb.getKernel().getHeight());
        assertEquals(1.7, cfb.getKernel().getFactor(), EPS);
        assertTrue(ArrayUtils.equalArrays(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0},
                                          cfb.getKernel().getKernelData(null), EPS));

    }

    public void testCreateXmlFromObject() {
        final double[] kernelData = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0};
        final ConvolutionFilterBand cfb = new ConvolutionFilterBand("filteredBand", _source,
                                                                    new Kernel(3, 3, 1.7, kernelData), 1);
        cfb.setDescription("somehow explainig");
        cfb.setUnit("someUnit");
        _product.addBand(cfb);

        final Element xmlElement = _convolutionFilterBandPersistable.createXmlFromObject(cfb);

        assertNotNull(xmlElement);
        assertEquals(DimapProductConstants.TAG_SPECTRAL_BAND_INFO, xmlElement.getName());
        assertEquals(14, xmlElement.getChildren().size());
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_INDEX) != null);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_NAME) != null);
        assertEquals(cfb.getName(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_DESCRIPTION) != null);
        assertEquals(cfb.getDescription(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_DATA_TYPE) != null);
        assertEquals(ProductData.getTypeString(cfb.getDataType()), xmlElement.getChildTextTrim(DimapProductConstants.TAG_DATA_TYPE));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_PHYSICAL_UNIT) != null);
        assertEquals(cfb.getUnit(), xmlElement.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SOLAR_FLUX) != null);
        assertEquals(cfb.getSolarFlux(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BAND_WAVELEN) != null);
        assertEquals(cfb.getSpectralWavelength(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_BANDWIDTH) != null);
        assertEquals(cfb.getSpectralBandwidth(), Float.parseFloat(xmlElement.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_FACTOR) != null);
        assertEquals(cfb.getScalingFactor(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_OFFSET) != null);
        assertEquals(cfb.getScalingOffset(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)), EPS);
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_SCALING_LOG_10) != null);
        assertEquals(cfb.isLog10Scaled(), Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE_USED) != null);
        assertEquals(cfb.isNoDataValueUsed(), Boolean.parseBoolean(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        assertTrue(xmlElement.getChild(DimapProductConstants.TAG_NO_DATA_VALUE) != null);
        assertEquals(cfb.getNoDataValue(), Double.parseDouble(xmlElement.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)), EPS);

        final Element filterInfo = xmlElement.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
        assertNotNull(filterInfo);
        assertEquals("ConvolutionFilterBand", filterInfo.getAttributeValue("bandType"));
        assertEquals(2, filterInfo.getChildren().size());
        assertTrue(filterInfo.getChild(DimapProductConstants.TAG_FILTER_SOURCE) != null);
        assertEquals(cfb.getSource().getName(), filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE));

        final Kernel kernel = cfb.getKernel();
        final Element kernelInfo = filterInfo.getChild(DimapProductConstants.TAG_FILTER_KERNEL);
        assertNotNull(kernelInfo);
        assertEquals(6, kernelInfo.getChildren().size());
        assertTrue(kernelInfo.getChild(DimapProductConstants.TAG_KERNEL_WIDTH) != null);
        assertEquals(kernel.getWidth(), Integer.parseInt(kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_WIDTH)));
        assertTrue(kernelInfo.getChild(DimapProductConstants.TAG_KERNEL_HEIGHT) != null);
        assertEquals(kernel.getHeight(), Integer.parseInt(kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_HEIGHT)));
        assertTrue(kernelInfo.getChild(DimapProductConstants.TAG_KERNEL_FACTOR) != null);
        assertEquals(kernel.getFactor(), Double.parseDouble(kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_FACTOR)), EPS);
        assertTrue(kernelInfo.getChild(DimapProductConstants.TAG_KERNEL_DATA) != null);
        assertEquals(ConvolutionFilterBandPersistable.toCsv(kernel.getKernelData(null)), kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_DATA));
    }

    public void testReadAndWrite() {
        final Element xmlElement = createXmlElement();

        final Object object = _convolutionFilterBandPersistable.createObjectFromXml(xmlElement, _product);
        _product.addBand((Band) object);

        final Element xmlFromObject = _convolutionFilterBandPersistable.createXmlFromObject(object);

        assertNotNull(xmlFromObject);
        final List expChildren = xmlElement.getChildren();
        final List actChildren = xmlFromObject.getChildren();
        assertEquals(expChildren.size(), actChildren.size());
        assertEqualElement(xmlElement, xmlFromObject);
    }

    private static void assertEqualElement(Element expElement, Element actElement) {
        if (!expElement.getChildren().isEmpty()) {
            final List expList = expElement.getChildren();
            for (Object expElem : expList) {
                final Element expSubElement = (Element) expElem;
                final Element actSubElement = actElement.getChild(expSubElement.getName());
                assertEqualElement(expSubElement, actSubElement);
            }
        } else {
            assertEquals(expElement.getName(), actElement.getName());
            assertEquals(expElement.getTextTrim(), actElement.getTextTrim());
        }
    }

    private Element createXmlElement() {
        final List<Element> contentList = new ArrayList<Element>(16);
        contentList.add(createElement(DimapProductConstants.TAG_BAND_INDEX, "1"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_NAME, "aBand"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_DESCRIPTION, "this is a band"));
        contentList.add(createElement(DimapProductConstants.TAG_DATA_TYPE, "float32"));
        contentList.add(createElement(DimapProductConstants.TAG_PHYSICAL_UNIT, "l"));
        contentList.add(createElement(DimapProductConstants.TAG_SOLAR_FLUX, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_WAVELEN, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_BANDWIDTH, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_FACTOR, "1.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_OFFSET, "0.0"));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_LOG_10, "false"));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE_USED, "true"));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE, Double.toString(_source.getGeophysicalNoDataValue())));

        final List<Element> filterBandInfoList = new ArrayList<Element>(3);
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, "anyBand"));

        final List<Element> kernelInfoList = new ArrayList<Element>(8);
        kernelInfoList.add(createElement(DimapProductConstants.TAG_KERNEL_WIDTH, "3"));
        kernelInfoList.add(createElement(DimapProductConstants.TAG_KERNEL_HEIGHT, "3"));
        kernelInfoList.add(createElement(DimapProductConstants.TAG_KERNEL_FACTOR, "1.7"));
        kernelInfoList.add(createElement(DimapProductConstants.TAG_KERNEL_DATA,
                                         ConvolutionFilterBandPersistable.toCsv(
                                                 new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0})));

        final Element filterKernel = new Element(DimapProductConstants.TAG_FILTER_KERNEL);
        filterKernel.setContent(kernelInfoList);
        filterBandInfoList.add(filterKernel);

        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        filterBandInfo.setContent(filterBandInfoList);
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
