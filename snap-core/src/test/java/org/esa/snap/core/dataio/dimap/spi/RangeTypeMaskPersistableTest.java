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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.*;
import static org.junit.Assert.*;

public class RangeTypeMaskPersistableTest {


    /*
    <Mask type="Range">
      <NAME value="myRange" />
      <DESCRIPTION value="Carefully defined range" />
      <COLOR red="0" green="255" blue="0" alpha="128" />
      <TRANSPARENCY value="0.78" />
      <MINIMUM value="0.35" />
      <MAXIMUM value="0.76" />
      <RASTER value="reflectance_13" />
    </Mask>
    */
    @Test
    public void createXmlFromObject() {
        final Mask mask = new Mask("myRange", 10, 10, Mask.RangeType.INSTANCE);
        mask.setDescription("Carefully defined range");
        mask.setImageColor(new Color(0, 255, 0, 128));
        mask.setImageTransparency(0.78);
        final PropertyContainer config = mask.getImageConfig();
        config.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, 0.35);
        config.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, 0.76);
        config.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, "reflectance_13");

        final RangeTypeMaskPersistable maskPersistable = new RangeTypeMaskPersistable();
        final Element element = maskPersistable.createXmlFromObject(mask);
        assertNotNull(element);
        assertEquals(TAG_MASK, element.getName());
        assertEquals(Mask.RangeType.TYPE_NAME, getAttributeString(element, ATTRIB_TYPE));

        final Element name = element.getChild(TAG_NAME);
        assertEquals("myRange", getAttributeString(name, ATTRIB_VALUE));

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertEquals("Carefully defined range", getAttributeString(description, ATTRIB_VALUE));

        final Element color = element.getChild(TAG_COLOR);
        assertEquals(0, getAttributeInt(color, ATTRIB_RED));
        assertEquals(255, getAttributeInt(color, ATTRIB_GREEN));
        assertEquals(0, getAttributeInt(color, ATTRIB_BLUE));
        assertEquals(128, getAttributeInt(color, ATTRIB_ALPHA));

        final Element transparency = element.getChild(TAG_TRANSPARENCY);
        assertEquals(0.78, getAttributeDouble(transparency, ATTRIB_VALUE), 0.0);


        final Element minimum = element.getChild(TAG_MINIMUM);
        assertEquals(0.35, getAttributeDouble(minimum, ATTRIB_VALUE), 0.0);
        final Element maximum = element.getChild(TAG_MAXIMUM);
        assertEquals(0.76, getAttributeDouble(maximum, ATTRIB_VALUE), 0.0);
        final Element raster = element.getChild(TAG_RASTER);
        assertEquals("reflectance_13", getAttributeString(raster, ATTRIB_VALUE));
    }

    @Test
    public void createMaskFromXml() throws IOException, JDOMException {
        final DimapPersistable persistable = new RangeTypeMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("RangeMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Product product = new Product("P", "T", 10, 10);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), product);

        assertNotNull(maskFromXml);
        assertEquals(Mask.RangeType.class, maskFromXml.getImageType().getClass());
        assertEquals("myRange", maskFromXml.getName());
        assertEquals("Carefully defined range", maskFromXml.getDescription());
        assertEquals(0.78, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(0, 255, 0, 128), maskFromXml.getImageColor());

        assertEquals(0.35, maskFromXml.getImageConfig().getValue(Mask.RangeType.PROPERTY_NAME_MINIMUM), 1.0e-6);
        assertEquals(0.76, maskFromXml.getImageConfig().getValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM), 1.0e-6);
        assertEquals("reflectance_13", maskFromXml.getImageConfig().getValue(Mask.RangeType.PROPERTY_NAME_RASTER));
    }

    private int getAttributeInt(Element element, String attribName) {
        return Integer.parseInt(element.getAttribute(attribName).getValue());
    }

    private double getAttributeDouble(Element element, String attribName) {
        return Double.parseDouble(element.getAttribute(attribName).getValue());
    }

    private String getAttributeString(Element element, String attribName) {
        return element.getAttribute(attribName).getValue();
    }
}
