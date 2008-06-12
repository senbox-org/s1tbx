/*
 * $Id: GeneralFilterBandPersistable.java,v 1.1.1.1 2006/09/11 08:16:44 norman Exp $
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
package org.esa.beam.dataio.dimap.spi;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.GeneralFilterBand;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.jdom.Element;

import java.util.ArrayList;

/**
 * Created by Marco Peters.
 *
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
class GeneralFilterBandPersistable implements DimapPersistable {


    public Object createObjectFromXml(Element element, Product product) {
        final Element filterBandInfo = element.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
        final int subWindowWidth = Integer.parseInt(
                filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH));
        final int subWindowHeight = Integer.parseInt(
                filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT));
        final GeneralFilterBand.Operator operator = GeneralFilterBand.createOperator(
                filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME));

        final String sourceName = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE);
        final RasterDataNode sourceNode = product.getRasterDataNode(sourceName);
        final String bandName = element.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME);
        final GeneralFilterBand gfb = new GeneralFilterBand(bandName, sourceNode,
                                                            subWindowWidth, subWindowHeight, operator);

        gfb.setDescription(element.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        gfb.setUnit(element.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        gfb.setSolarFlux(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)));
        gfb.setSpectralWavelength(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)));
        gfb.setSpectralBandwidth(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)));
        gfb.setScalingFactor(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)));
        gfb.setScalingOffset(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)));
        gfb.setLog10Scaled(Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        gfb.setNoDataValueUsed(
                Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        gfb.setNoDataValue(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)));

        return gfb;
    }

    public Element createXmlFromObject(Object object) {
        final GeneralFilterBand gfb = (GeneralFilterBand) object;
        final ArrayList contentList = new ArrayList();
        contentList.add(createElement(DimapProductConstants.TAG_BAND_INDEX, String.valueOf(gfb.getProduct().getBandIndex(gfb.getName()))));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_NAME, gfb.getName()));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_DESCRIPTION, gfb.getDescription()));
        contentList.add(createElement(DimapProductConstants.TAG_DATA_TYPE, ProductData.getTypeString(gfb.getDataType())));
        contentList.add(createElement(DimapProductConstants.TAG_PHYSICAL_UNIT, gfb.getUnit()));
        contentList.add(createElement(DimapProductConstants.TAG_SOLAR_FLUX, String.valueOf(gfb.getSolarFlux())));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_WAVELEN, String.valueOf(gfb.getSpectralWavelength())));
        contentList.add(createElement(DimapProductConstants.TAG_BANDWIDTH, String.valueOf(gfb.getSpectralBandwidth())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_FACTOR, String.valueOf(gfb.getScalingFactor())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_OFFSET, String.valueOf(gfb.getScalingOffset())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_LOG_10, String.valueOf(gfb.isLog10Scaled())));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE_USED, String.valueOf(gfb.isNoDataValueUsed())));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE, String.valueOf(gfb.getNoDataValue())));
        final ArrayList filterBandInfoList = new ArrayList();
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, gfb.getSource().getName()));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH, String.valueOf(gfb.getSubWindowWidth())));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT, String.valueOf(gfb.getSubWindowHeight())));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME, gfb.getOperator().getClass().getName()));

        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        filterBandInfo.setAttribute("bandType", "GeneralFilterBand");
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
