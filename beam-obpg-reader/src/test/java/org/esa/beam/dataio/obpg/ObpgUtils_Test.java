/*
 * $Id$
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
package org.esa.beam.dataio.obpg;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;

import org.esa.beam.dataio.obpg.ObpgUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.MetadataElement;

import ucar.nc2.Attribute;

public class ObpgUtils_Test extends TestCase {

    public void testGetInputFile_UnsupportetSource() {
        try {
            ObpgUtils.getInputFile(new Integer(3));
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertTrue(e.getMessage().startsWith("unsupported input source:"));
        }
    }

    public void testGetInputFile_FileSource() {
        final File file = ObpgUtils.getInputFile(new File("someFile"));

        assertNotNull(file);
        assertEquals("someFile", file.getPath());
    }

    public void testGetInputFile_StringSource() {
        final File file = ObpgUtils.getInputFile("someOtherFile");

        assertNotNull(file);
        assertEquals("someOtherFile", file.getPath());
    }

    public void testAddGlobalMetadata_ok() throws ProductIOException {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        final Product product = new Product("name", "type", 50, 60);

        final ObpgUtils obpgUtils = new ObpgUtils();
        obpgUtils.addGlobalMetadata(product, globalAttributes);

        final MetadataElement globalElement = product.getMetadataRoot().getElement("Global_Attributes");
        assertNotNull(globalElement);
        assertEquals(0, globalElement.getNumElements());
        assertEquals(4, globalElement.getNumAttributes());
        assertNotNull(globalElement.getAttribute(ObpgUtils.KEY_NAME));
        assertNotNull(globalElement.getAttribute(ObpgUtils.KEY_TYPE));
        assertNotNull(globalElement.getAttribute(ObpgUtils.KEY_WIDTH));
        assertNotNull(globalElement.getAttribute(ObpgUtils.KEY_HEIGHT));
    }

    public void testConvertToFlagmask() {
        final ObpgUtils obpgUtils = new ObpgUtils();
        assertEquals(0x00000000, obpgUtils.convertToFlagMask("any_name"));
        assertEquals(0x00000001, obpgUtils.convertToFlagMask("f01_name"));
        assertEquals(0x00000002, obpgUtils.convertToFlagMask("f02_name"));
        assertEquals(0x00000004, obpgUtils.convertToFlagMask("f03_name"));
        assertEquals(0x00000008, obpgUtils.convertToFlagMask("f04_name"));
        assertEquals(0x00000010, obpgUtils.convertToFlagMask("f05_name"));
        assertEquals(0x00000020, obpgUtils.convertToFlagMask("f06_name"));
        assertEquals(0x00000040, obpgUtils.convertToFlagMask("f07_name"));
        assertEquals(0x00000080, obpgUtils.convertToFlagMask("f08_name"));
        assertEquals(0x00000100, obpgUtils.convertToFlagMask("f09_name"));
        assertEquals(0x00000200, obpgUtils.convertToFlagMask("f10_name"));
        assertEquals(0x00000400, obpgUtils.convertToFlagMask("f11_name"));
        assertEquals(0x00000800, obpgUtils.convertToFlagMask("f12_name"));
        assertEquals(0x00001000, obpgUtils.convertToFlagMask("f13_name"));
        assertEquals(0x00002000, obpgUtils.convertToFlagMask("f14_name"));
        assertEquals(0x00004000, obpgUtils.convertToFlagMask("f15_name"));
        assertEquals(0x00008000, obpgUtils.convertToFlagMask("f16_name"));
        assertEquals(0x00010000, obpgUtils.convertToFlagMask("f17_name"));
        assertEquals(0x00020000, obpgUtils.convertToFlagMask("f18_name"));
        assertEquals(0x00040000, obpgUtils.convertToFlagMask("f19_name"));
        assertEquals(0x00080000, obpgUtils.convertToFlagMask("f20_name"));
        assertEquals(0x00100000, obpgUtils.convertToFlagMask("f21_name"));
        assertEquals(0x00200000, obpgUtils.convertToFlagMask("f22_name"));
        assertEquals(0x00400000, obpgUtils.convertToFlagMask("f23_name"));
        assertEquals(0x00800000, obpgUtils.convertToFlagMask("f24_name"));
        assertEquals(0x01000000, obpgUtils.convertToFlagMask("f25_name"));
        assertEquals(0x02000000, obpgUtils.convertToFlagMask("f26_name"));
        assertEquals(0x04000000, obpgUtils.convertToFlagMask("f27_name"));
        assertEquals(0x08000000, obpgUtils.convertToFlagMask("f28_name"));
        assertEquals(0x10000000, obpgUtils.convertToFlagMask("f29_name"));
        assertEquals(0x20000000, obpgUtils.convertToFlagMask("f30_name"));
        assertEquals(0x40000000, obpgUtils.convertToFlagMask("f31_name"));
        assertEquals(0x80000000, obpgUtils.convertToFlagMask("f32_name"));
    }
}
