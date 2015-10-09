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

import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.jdom.Attribute;
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

public class BandMathMaskPersistableTest {

    /*
    <Mask type="Math">
      <NAME value="Bibo" />
      <DESCRIPTION value="A big yellow bird is in the pixel." />
      <COLOR red="17" green="11" blue="67" alpha="255" />
      <TRANSPARENCY value="0.7" />
      <EXPRESSION value="false" />
    </Mask>
    */
    @Test
    public void testXmlCreation() {
        Mask mask = Mask.BandMathsType.create("Bibo", "A big yellow bird is in the pixel.", 10, 10,
                                             "false", new Color(17, 11, 67), 0.7);

        final DimapPersistable persistable = new BandMathsMaskPersistable();

        final Element element = persistable.createXmlFromObject(mask);

        assertNotNull(element);
        assertEquals(TAG_MASK, element.getName());

        final Attribute type = element.getAttribute(ATTRIB_TYPE);
        assertNotNull(type);
        assertEquals(Mask.BandMathsType.TYPE_NAME, type.getValue());

        final Element name = element.getChild(TAG_NAME);
        assertNotNull(name);
        assertEquals("Bibo", name.getAttribute("value").getValue());

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertNotNull(description);
        assertEquals("A big yellow bird is in the pixel.", description.getAttribute(ATTRIB_VALUE).getValue());

        final Element color = element.getChild(TAG_COLOR);
        assertNotNull(color);
        assertEquals(17, getAttributeInt(color, ATTRIB_RED));
        assertEquals(11, getAttributeInt(color, ATTRIB_GREEN));
        assertEquals(67, getAttributeInt(color, ATTRIB_BLUE));
        assertEquals(255, getAttributeInt(color, ATTRIB_ALPHA));

        final Element transparency = element.getChild(TAG_TRANSPARENCY);
        assertEquals(0.7, getAttributeDouble(transparency, ATTRIB_VALUE), 0.0);

        final Element expression = element.getChild(TAG_EXPRESSION);
        assertNotNull(expression);
        assertEquals("false", expression.getAttribute(ATTRIB_VALUE).getValue());
    }


    @Test
    public void testMaskCreation() throws IOException, JDOMException {
        final DimapPersistable persistable = new BandMathsMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("BandMathMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Product product = new Product("P", "T", 10, 10);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), product);

        assertNotNull(maskFromXml);
        assertEquals(Mask.BandMathsType.class, maskFromXml.getImageType().getClass());
        assertEquals("Bibo", maskFromXml.getName());
        assertEquals("A big yellow bird is in the pixel.", maskFromXml.getDescription());
        assertEquals(0.7, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(17, 11, 67), maskFromXml.getImageColor());


        assertEquals("false", maskFromXml.getImageConfig().getValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION));
    }

    private int getAttributeInt(Element element, String attribName) {
        return Integer.parseInt(element.getAttribute(attribName).getValue());
    }

    private double getAttributeDouble(Element element, String attribName) {
        return Double.parseDouble(element.getAttribute(attribName).getValue());
    }

}
