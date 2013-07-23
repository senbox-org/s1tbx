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

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;


public class LandsatLegacyMetadataTest {

    @Test
    public void testReadMetadata_L5() throws IOException {
        InputStream stream = LandsatLegacyMetadataTest.class.getResourceAsStream("test_MTL.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        LandsatLegacyMetadata landsatMetadata = new LandsatLegacyMetadata(reader);

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

        assertEquals("Landsat5_TM_L1T", landsatMetadata.getProductType());

        ProductData.UTC cTime = landsatMetadata.getCenterTime();
        assertNotNull(cTime);
        assertEquals("14-SEP-2003 09:55:12.228000", cTime.format());

        final Dimension panchromaticDim = landsatMetadata.getPanchromaticDim();
        assertNull(panchromaticDim);    // no panchromatic band in Landsat5 tb 2011-11-25

        final Dimension thermalDim = landsatMetadata.getThermalDim();
        assertNotNull(thermalDim);
        assertEquals(7461, thermalDim.getHeight(), 1e-8);
        assertEquals(8401, thermalDim.getWidth(), 1e-8);

        final Dimension reflectanceDim = landsatMetadata.getReflectanceDim();
        assertNotNull(reflectanceDim);
        assertEquals(7461, reflectanceDim.getHeight(), 1e-8);
        assertEquals(8401, reflectanceDim.getWidth(), 1e-8);
    }

    @Test
    public void testReadMetadata_L7() throws IOException {
        final InputStream stream = LandsatLegacyMetadataTest.class.getResourceAsStream("test_MTL_L7.txt");
        final InputStreamReader reader = new InputStreamReader(stream);
        final LandsatLegacyMetadata landsatMetadata = new LandsatLegacyMetadata(reader);

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

        assertEquals("Landsat7_ETM+_L1T", landsatMetadata.getProductType());

        ProductData.UTC cTime = landsatMetadata.getCenterTime();
        assertNotNull(cTime);
        assertEquals("15-JAN-2006 08:45:02.587000", cTime.format());

        final Dimension panchromaticDim = landsatMetadata.getPanchromaticDim();
        assertNotNull(panchromaticDim);
        assertEquals(14001, panchromaticDim.getHeight(), 1e-8);
        assertEquals(15541, panchromaticDim.getWidth(), 1e-8);

        final Dimension thermalDim = landsatMetadata.getThermalDim();
        assertNotNull(thermalDim);
        assertEquals(7001, thermalDim.getHeight(), 1e-8);
        assertEquals(7771, thermalDim.getWidth(), 1e-8);

        final Dimension reflectanceDim = landsatMetadata.getReflectanceDim();
        assertNotNull(reflectanceDim);
        assertEquals(7001, reflectanceDim.getHeight(), 1e-8);
        assertEquals(7771, reflectanceDim.getWidth(), 1e-8);
    }
}
