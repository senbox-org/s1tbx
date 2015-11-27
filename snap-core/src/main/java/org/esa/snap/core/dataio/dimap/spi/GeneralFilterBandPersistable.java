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

import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.jdom.Element;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i>
 *
 * @author Marco Peters
 */
class GeneralFilterBandPersistable extends RasterDataNodePersistable {
    static final String VERSION_1_0 = "1.0";
    static final String VERSION_1_1 = "1.1";
    static final String VERSION_1_2 = "1.2";
    static final String ATTRIBUTE_BAND_TYPE = DimapProductConstants.ATTRIB_BAND_TYPE;
    static final String ATTRIBUTE_VERSION = "version";
    static final String GENERAL_FILTER_BAND_TYPE = "GeneralFilterBand";

    @Override
    public Object createObjectFromXml(Element element, Product product) {
        final Element filterBandInfo = element.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);

        GeneralFilterBand.OpType opType = parseOpType(filterBandInfo);
        if (opType == null) {
            SystemUtils.LOG.warning(String.format("BEAM-DIMAP problem in element '%s': missing or illegal value for element '%s'",
                                                  filterBandInfo.getName(), DimapProductConstants.TAG_FILTER_OP_TYPE));
            return null;
        }

        final Element kernelInfo = filterBandInfo.getChild(DimapProductConstants.TAG_FILTER_KERNEL);
        Kernel kernel;
        if (kernelInfo != null) {
            kernel = ConvolutionFilterBandPersistable.convertElementToKernel(kernelInfo);
        } else {
            Dimension size = parseSize(filterBandInfo);
            if (size == null || size.width <= 0|| size.height <= 0) {
                SystemUtils.LOG.warning(String.format("BEAM-DIMAP problem in element '%s': missing or illegal value for element '%s'",
                                                      filterBandInfo.getName(), DimapProductConstants.TAG_FILTER_SUB_WINDOW_SIZE));
                return null;
            }
            double[] data = new double[size.width * size.height];
            Arrays.fill(data, 1.0);
            kernel = new Kernel(size.width, size.height, data);
        }

        final String sourceName = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE);
        final RasterDataNode sourceNode = product.getRasterDataNode(sourceName);
        final String bandName = element.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME);

        // todo - read iterationCount
        final GeneralFilterBand gfb = new GeneralFilterBand(bandName, sourceNode, opType, kernel, 1);

        gfb.setDescription(element.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        gfb.setUnit(element.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        gfb.setSolarFlux(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)));
        gfb.setSpectralWavelength(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)));
        gfb.setSpectralBandwidth(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)));
        gfb.setScalingFactor(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)));
        gfb.setScalingOffset(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)));
        gfb.setLog10Scaled(Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        gfb.setNoDataValueUsed(Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        gfb.setNoDataValue(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)));

        setAncillaryRelations(element, gfb);
        setAncillaryVariables(element, gfb, product);
        setImageToModelTransform(element, gfb);
        return gfb;
    }

    private Dimension parseSize(Element filterBandInfo) {
        int width = 1;
        int height = 1;
        // Version 1.1
        String sizeText = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_SIZE);
        if (sizeText != null) {
            int size = Integer.parseInt(sizeText);
            width = size;
            height = size;
        } else {
            // Version 1.0
            String widthText = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH);
            if (widthText != null) {
                width = Integer.parseInt(widthText);
            }
            String heightText = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT);
            if (heightText != null) {
                height = Integer.parseInt(heightText);
            }
        }
        return new Dimension(width, height);
    }

    private GeneralFilterBand.OpType parseOpType(Element filterBandInfo) {
        GeneralFilterBand.OpType opType = null;
        String filterOpClassName = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_OPERATOR_CLASS_NAME);
        if (filterOpClassName != null) {
            // Compatibility with older BEAM-DIMAP <= v1.1
            int index = filterOpClassName.lastIndexOf('$');
            if (index > 0) {
                filterOpClassName = filterOpClassName.substring(index + 1);
            }
            switch (filterOpClassName) {
                case "Min":
                    opType = GeneralFilterBand.OpType.MIN;
                    break;
                case "Max":
                    opType = GeneralFilterBand.OpType.MAX;
                    break;
                case "Mean":
                    opType = GeneralFilterBand.OpType.MEAN;
                    break;
                case "Median":
                    opType = GeneralFilterBand.OpType.MEDIAN;
                    break;
                case "StdDev":
                    opType = GeneralFilterBand.OpType.STDDEV;
                    break;
            }
        } else {
            String filterOpTypeName = filterBandInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_OP_TYPE);
            if (filterOpTypeName != null) {
                opType = GeneralFilterBand.OpType.valueOf(filterOpTypeName);
            }
        }
        return opType;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        final GeneralFilterBand gfb = (GeneralFilterBand) object;
        final List<Element> contentList = new ArrayList<Element>(20);
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
        final List<Element> filterBandInfoList = new ArrayList<Element>(5);
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, gfb.getSource().getName()));
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_OP_TYPE, gfb.getOpType().toString()));
        filterBandInfoList.add(ConvolutionFilterBandPersistable.convertKernelToElement(gfb.getStructuringElement()));

        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        filterBandInfo.setAttribute(ATTRIBUTE_BAND_TYPE, GENERAL_FILTER_BAND_TYPE);
        filterBandInfo.setAttribute(ATTRIBUTE_VERSION, VERSION_1_2);
        filterBandInfo.addContent(filterBandInfoList);
        contentList.add(filterBandInfo);

        final Element root = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        root.setContent(contentList);
        addImageToModelTransformElement(root, gfb);
        addAncillaryElements(root, gfb);
        return root;
    }
}
