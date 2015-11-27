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

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.jdom.Element;

import java.awt.Color;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.*;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public abstract class MaskPersistable extends RasterDataNodePersistable {

    @Override
    public final Mask createObjectFromXml(Element element, Product product) {
        final String name = getChildAttributeValue(element, TAG_NAME, ATTRIB_VALUE);
        final int width;
        final int height;
        if (element.getChild(TAG_MASK_RASTER_WIDTH) != null && element.getChild(TAG_MASK_RASTER_HEIGHT) != null) {
            width = Integer.parseInt(getChildAttributeValue(element, TAG_MASK_RASTER_WIDTH, ATTRIB_VALUE));
            height = Integer.parseInt(getChildAttributeValue(element, TAG_MASK_RASTER_HEIGHT, ATTRIB_VALUE));
        } else {
            width = product.getSceneRasterWidth();
            height = product.getSceneRasterHeight();
        }
        final Mask mask = new Mask(name, width, height, createImageType());
        mask.setDescription(getChildAttributeValue(element, TAG_DESCRIPTION, ATTRIB_VALUE));
        mask.setImageTransparency(Double.parseDouble(getChildAttributeValue(element, TAG_TRANSPARENCY, ATTRIB_VALUE)));
        setImageColor(element, mask);
        setImageToModelTransform(element, mask);
        setAncillaryRelations(element, mask);
        setAncillaryVariables(element, mask, product);
        configureMask(mask, element);
        return mask;
    }

    private void setImageColor(Element element, Mask mask) {
        final int r = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_RED));
        final int g = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_GREEN));
        final int b = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_BLUE));
        final int a = Integer.parseInt(getChildAttributeValue(element, TAG_COLOR, ATTRIB_ALPHA));
        mask.setImageColor(new Color(r, g, b, a));
    }

    @Override
    public final Element createXmlFromObject(Object object) {
        final Mask mask = (Mask) object;
        final Element root = new Element(TAG_MASK);
        root.setAttribute(ATTRIB_TYPE, mask.getImageType().getName());
        root.addContent(createValueAttributeElement(TAG_NAME, mask.getName()));
        root.addContent(createValueAttributeElement(TAG_MASK_RASTER_WIDTH, String.valueOf(mask.getRasterWidth())));
        root.addContent(createValueAttributeElement(TAG_MASK_RASTER_HEIGHT, String.valueOf(mask.getRasterHeight())));
        root.addContent(createValueAttributeElement(TAG_DESCRIPTION, mask.getDescription()));
        addAncillaryElements(root, mask);
        addImageConfigElements(root, mask);
        addImageToModelTransformElement(root, mask);
        configureElement(root, mask);
        return root;
    }

    private void addImageConfigElements(Element root, Mask mask) {
        final Element colorElement = new Element(TAG_COLOR);
        final PropertyContainer config = mask.getImageConfig();
        final Color color = config.getValue(Mask.ImageType.PROPERTY_NAME_COLOR);
        colorElement.setAttribute(ATTRIB_RED, String.valueOf(color.getRed()));
        colorElement.setAttribute(ATTRIB_GREEN, String.valueOf(color.getGreen()));
        colorElement.setAttribute(ATTRIB_BLUE, String.valueOf(color.getBlue()));
        colorElement.setAttribute(ATTRIB_ALPHA, String.valueOf(color.getAlpha()));
        root.addContent(colorElement);
        Object transparencyValue = config.getValue(Mask.ImageType.PROPERTY_NAME_TRANSPARENCY);
        final String transparency = String.valueOf(transparencyValue);
        root.addContent(createValueAttributeElement(TAG_TRANSPARENCY, transparency));
    }

    protected abstract Mask.ImageType createImageType();

    protected abstract void configureMask(Mask mask, Element element);

    protected abstract void configureElement(Element root, Mask mask);

    protected static String getChildAttributeValue(Element element, String childName, String attributeName) {
        return element.getChild(childName).getAttribute(attributeName).getValue();
    }
}
