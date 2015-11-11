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

import com.bc.ceres.glevel.MultiLevelImage;
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

public class MaskPersistableTest {

    @Test
    public void createXmlFromObject() {
        final Mask mask = new Mask("myRange", 10, 10, Mask.RangeType.INSTANCE);
        mask.setDescription("Carefully defined range");
        mask.setImageColor(new Color(0, 255, 0, 128));
        mask.setImageTransparency(0.78);

        final RangeTypeMaskPersistable maskPersistable = new RangeTypeMaskPersistable();
        final Element element = maskPersistable.createXmlFromObject(mask);
        assertNotNull(element);
        assertEquals(TAG_MASK, element.getName());
        assertEquals(Mask.RangeType.TYPE_NAME, getAttributeString(element, ATTRIB_TYPE));

        final Element name = element.getChild(TAG_NAME);
        assertEquals("myRange", getAttributeString(name, ATTRIB_VALUE));
        final Element widthElem = element.getChild(TAG_MASK_RASTER_WIDTH);
        assertEquals(10, getAttributeInt(widthElem, ATTRIB_VALUE));
        final Element heightElem = element.getChild(TAG_MASK_RASTER_HEIGHT);
        assertEquals(10, getAttributeInt(heightElem, ATTRIB_VALUE));

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertEquals("Carefully defined range", getAttributeString(description, ATTRIB_VALUE));

        final Element color = element.getChild(TAG_COLOR);
        assertEquals(0, getAttributeInt(color, ATTRIB_RED));
        assertEquals(255, getAttributeInt(color, ATTRIB_GREEN));
        assertEquals(0, getAttributeInt(color, ATTRIB_BLUE));
        assertEquals(128, getAttributeInt(color, ATTRIB_ALPHA));

        final Element transparency = element.getChild(TAG_TRANSPARENCY);
        assertEquals(0.78, getAttributeDouble(transparency, ATTRIB_VALUE), 0.0);
    }

    @Test
    public void testXmlCreationWithoutMaskDescription() {
        final Mask mask = new Mask("name", 10, 10, new TestImageType());
        mask.setDescription(null);
        final DimapPersistable persistable = new TestMaskPersistable();
        final Element element = persistable.createXmlFromObject(mask);

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertNotNull(description);
        assertTrue(description.getAttribute(ATTRIB_VALUE).getValue().isEmpty());
    }

    @Test
    public void testMaskCreation() throws IOException, JDOMException {
        final DimapPersistable persistable = new TestMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("TestMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Product product = new Product("P", "T", 10, 10);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), product);

        assertNotNull(maskFromXml);
        assertEquals(TestImageType.class, maskFromXml.getImageType().getClass());
        assertEquals(10, maskFromXml.getRasterWidth());
        assertEquals(10, maskFromXml.getRasterHeight());
        assertEquals("Bibo", maskFromXml.getName());
        assertEquals("A big yellow bird is in the pixel.", maskFromXml.getDescription());
        assertEquals(0.7, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(17, 11, 67), maskFromXml.getImageColor());
    }

    @Test
    public void testMaskCreation_WithSize() throws IOException, JDOMException {
        final DimapPersistable persistable = new TestMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("TestMask_WithSize.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Product product = new Product("P", "T", 10, 10);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), product);

        assertNotNull(maskFromXml);
        assertEquals(TestImageType.class, maskFromXml.getImageType().getClass());
        assertEquals(30, maskFromXml.getRasterWidth());
        assertEquals(25, maskFromXml.getRasterHeight());
        assertEquals("Bibo", maskFromXml.getName());
        assertEquals("A big yellow bird is in the pixel.", maskFromXml.getDescription());
        assertEquals(0.7, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(17, 11, 67), maskFromXml.getImageColor());
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

    private class TestMaskPersistable extends MaskPersistable {

        @Override
        protected Mask.ImageType createImageType() {
            return new TestImageType();
        }

        @Override
        protected void configureMask(Mask mask, Element element) {
        }

        @Override
        protected void configureElement(Element root, Mask mask) {
        }
    }

    private static class TestImageType extends Mask.ImageType {

        public TestImageType() {
            super("type");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }
    }
}
