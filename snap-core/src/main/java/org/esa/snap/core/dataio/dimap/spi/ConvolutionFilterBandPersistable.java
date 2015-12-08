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
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i>
 *
 * @author Marco Peters
 */
class ConvolutionFilterBandPersistable extends RasterDataNodePersistable {

    @Override
    public Object createObjectFromXml(Element element, Product product) {
        final Element filterInfo = element.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
        final Element kernelInfo = filterInfo.getChild(DimapProductConstants.TAG_FILTER_KERNEL);
        final Kernel kernel = convertElementToKernel(kernelInfo);
        final String sourceName = filterInfo.getChildTextTrim(DimapProductConstants.TAG_FILTER_SOURCE);
        final String bandName = element.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME);
        final RasterDataNode sourceNode = product.getRasterDataNode(sourceName);
        // todo - read iterationCount
        final ConvolutionFilterBand cfb = new ConvolutionFilterBand(bandName, sourceNode, kernel, 1);
        cfb.setDescription(element.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION));
        cfb.setUnit(element.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT));
        cfb.setSolarFlux(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX)));
        cfb.setSpectralWavelength(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN)));
        cfb.setSpectralBandwidth(Float.parseFloat(element.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH)));
        cfb.setScalingFactor(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR)));
        cfb.setScalingOffset(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET)));
        cfb.setLog10Scaled(Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10)));
        cfb.setNoDataValueUsed(Boolean.parseBoolean(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED)));
        cfb.setNoDataValue(Double.parseDouble(element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE)));
        setAncillaryRelations(element, cfb);
        setAncillaryVariables(element, cfb, product);
        setImageToModelTransform(element, cfb);
        return cfb;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        final ConvolutionFilterBand cfb = (ConvolutionFilterBand) object;
        final List<Element> contentList = new ArrayList<>();
        contentList.add(createElement(DimapProductConstants.TAG_BAND_INDEX, String.valueOf(cfb.getProduct().getBandIndex(cfb.getName()))));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_NAME, cfb.getName()));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_DESCRIPTION, cfb.getDescription()));
        contentList.add(createElement(DimapProductConstants.TAG_DATA_TYPE, ProductData.getTypeString(cfb.getDataType())));
        contentList.add(createElement(DimapProductConstants.TAG_PHYSICAL_UNIT, cfb.getUnit()));
        contentList.add(createElement(DimapProductConstants.TAG_SOLAR_FLUX, String.valueOf(cfb.getSolarFlux())));
        contentList.add(createElement(DimapProductConstants.TAG_BAND_WAVELEN, String.valueOf(cfb.getSpectralWavelength())));
        contentList.add(createElement(DimapProductConstants.TAG_BANDWIDTH, String.valueOf(cfb.getSpectralBandwidth())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_FACTOR, String.valueOf(cfb.getScalingFactor())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_OFFSET, String.valueOf(cfb.getScalingOffset())));
        contentList.add(createElement(DimapProductConstants.TAG_SCALING_LOG_10, String.valueOf(cfb.isLog10Scaled())));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE_USED, String.valueOf(cfb.isNoDataValueUsed())));
        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE, String.valueOf(cfb.getNoDataValue())));

        final List<Element> filterBandInfoList = new ArrayList<>();
        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, cfb.getSource().getName()));
        filterBandInfoList.add(convertKernelToElement(cfb.getKernel()));

        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        filterBandInfo.setAttribute("bandType", "ConvolutionFilterBand");
        filterBandInfo.addContent(filterBandInfoList);
        contentList.add(filterBandInfo);

        final Element root = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        root.setContent(contentList);
        addAncillaryElements(root, cfb);
        addImageToModelTransformElement(root, cfb);
        return root;

    }

    static Kernel convertElementToKernel(Element kernelInfo) {
        final String kernelDataString = kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_DATA);
        final double[] data = StringUtils.toDoubleArray(kernelDataString, ",");

        int width = Integer.parseInt(kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_WIDTH));
        int height = Integer.parseInt(kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_HEIGHT));

        String xOriginText = kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_X_ORIGIN);
        int xOrigin = (width - 1) / 2;
        if (xOriginText != null) {
            xOrigin = Integer.parseInt(xOriginText);
        }

        String yOriginText = kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_Y_ORIGIN);
        int yOrigin = (height - 1) / 2;
        if (yOriginText != null) {
            yOrigin = Integer.parseInt(yOriginText);
        }

        String factorText = kernelInfo.getChildTextTrim(DimapProductConstants.TAG_KERNEL_FACTOR);
        double factor = 1;
        if (factorText != null) {
            factor = Double.parseDouble(factorText);
        }

        return new Kernel(width, height, xOrigin, yOrigin, factor, data);
    }

    static Element convertKernelToElement(Kernel kernel) {
        final List<Element> filterKernelList = new ArrayList<>();
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_WIDTH, String.valueOf(kernel.getWidth())));
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_HEIGHT, String.valueOf(kernel.getHeight())));
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_X_ORIGIN, String.valueOf(kernel.getXOrigin())));
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_Y_ORIGIN, String.valueOf(kernel.getYOrigin())));
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_FACTOR, String.valueOf(kernel.getFactor())));
        filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_DATA, toCsv(kernel.getKernelData(null))));
        final Element filterKernel = new Element(DimapProductConstants.TAG_FILTER_KERNEL);
        filterKernel.addContent(filterKernelList);
        return filterKernel;
    }

    static String toCsv(double[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            double v = data[i];
            if (i > 0) {
                sb.append(',');
            }
            if (v == (int) v) {
                sb.append((int) v);
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
