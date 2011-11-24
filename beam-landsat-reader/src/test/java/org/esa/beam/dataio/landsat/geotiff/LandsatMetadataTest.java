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

package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;


public class LandsatMetadataTest {

    @Test
    public void testReadMetadata_L5() throws IOException {
        InputStream stream = LandsatMetadataTest.class.getResourceAsStream("test_MTL.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        LandsatMetadata landsatMetadata = new LandsatMetadata(reader);

        MetadataElement element = landsatMetadata.getMetaDataElementRoot();
        assertNotNull(element);
        assertEquals("L1_METADATA_FILE", element.getName());
        MetadataElement[] childElements = element.getElements();
        assertEquals(8, childElements.length);
        MetadataElement firstChild = childElements[0];
        assertEquals("METADATA_FILE_INFO", firstChild.getName());
        assertEquals(0, firstChild.getElements().length);
        MetadataAttribute[] attributes = firstChild.getAttributes();
        assertEquals(9, attributes.length);
        MetadataAttribute originAttr = attributes[0];
        assertEquals("ORIGIN", originAttr.getName());
        assertEquals(ProductData.TYPESTRING_ASCII, originAttr.getData().getTypeString());
        assertEquals("Image courtesy of the U.S. Geological Survey", originAttr.getData().getElemString());

        assertTrue(landsatMetadata.isLandsatTM());
        assertFalse(landsatMetadata.isLandsatETM_Plus());

        ProductData.UTC cTime = landsatMetadata.getCenterTime();
        assertNotNull(cTime);
        assertEquals("14-SEP-2003 09:55:12.228000", cTime.format());
    }

    @Test
    public void testReadMetadata_L7() throws IOException {
        final InputStream stream = LandsatMetadataTest.class.getResourceAsStream("test_MTL_L7.txt");
        final InputStreamReader reader = new InputStreamReader(stream);
        final LandsatMetadata landsatMetadata = new LandsatMetadata(reader);

        final MetadataElement elementRoot = landsatMetadata.getMetaDataElementRoot();
        assertNotNull(elementRoot);
        assertEquals("L1_METADATA_FILE", elementRoot.getName());

        final MetadataElement[] childElements = elementRoot.getElements();
        assertEquals(8, childElements.length);

        final MetadataElement firstChild = childElements[0];
        assertEquals("METADATA_FILE_INFO", firstChild.getName());
        assertEquals(0, firstChild.getElements().length);

        final MetadataAttribute[] attributes = firstChild.getAttributes();
        assertEquals(9, attributes.length);
        final MetadataAttribute originAttr = attributes[0];
        assertEquals("ORIGIN", originAttr.getName());
        assertEquals(ProductData.TYPESTRING_ASCII, originAttr.getData().getTypeString());
        assertEquals("Image courtesy of the U.S. Geological Survey", originAttr.getData().getElemString());

        assertFalse(landsatMetadata.isLandsatTM());
        assertTrue(landsatMetadata.isLandsatETM_Plus());

        ProductData.UTC cTime = landsatMetadata.getCenterTime();
        assertNotNull(cTime);
        assertEquals("15-JAN-2006 08:45:02.587000", cTime.format());
    }
}
