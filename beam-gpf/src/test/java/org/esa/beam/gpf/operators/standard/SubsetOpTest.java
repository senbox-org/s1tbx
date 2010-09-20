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
package org.esa.beam.gpf.operators.standard;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.Arrays;

public class SubsetOpTest extends TestCase {


    public void testConstructorUsage() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final String[] bandNames = {"radiance_1", "radiance_3"};

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(bandNames);

        assertSame(sp, op.getSourceProduct());
        assertNotSame(bandNames, op.getBandNames());

        Product tp = op.getTargetProduct();

        assertEquals(2, tp.getNumBands());
        assertNotNull(tp.getBand("radiance_1"));
        assertNull(tp.getBand("radiance_2"));
        assertNotNull(tp.getBand("radiance_3"));
    }

    public void testCopyMetadata() throws Exception {
        final Product sp = createTestProduct(100, 100);
        addMetadata(sp);
        final String[] bandNames = {"radiance_1", "radiance_3"};

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(bandNames);
        op.setCopyMetadata(true);

        assertSame(sp, op.getSourceProduct());
        assertNotSame(bandNames, op.getBandNames());

        Product tp = op.getTargetProduct();

        assertEquals(2, tp.getNumBands());
        assertNotNull(tp.getBand("radiance_1"));
        assertNull(tp.getBand("radiance_2"));
        assertNotNull(tp.getBand("radiance_3"));

        final MetadataElement root = tp.getMetadataRoot();
        assertNotNull(root);
        final MetadataAttribute attribRoot = root.getAttribute("attribRoot");
        assertNotNull(attribRoot);
        assertEquals("rootValue", attribRoot.getData().getElemString());
        assertTrue(root.containsElement("meta1"));
        final MetadataAttribute attrib1 = root.getElement("meta1").getAttribute("attrib1");
        assertNotNull(attrib1);
        assertEquals("value", attrib1.getData().getElemString());
        final MetadataElement meta2 = root.getElement("meta2");
        assertNotNull(meta2);
        final MetadataElement meta2_1 = meta2.getElement("meta2_1");
        assertNotNull(meta2_1);
        final MetadataAttribute attrib2_1 = meta2_1.getAttribute("attrib2_1");
        assertEquals("meta2_1_value", attrib2_1.getData().getElemString());
    }

    private void addMetadata(Product sp) {
        final MetadataElement meta1 = new MetadataElement("meta1");
        meta1.addAttribute(new MetadataAttribute("attrib1", ProductData.createInstance("value"), true));
        final MetadataElement meta2 = new MetadataElement("meta2");
        final MetadataElement meta2_1 = new MetadataElement("meta2_1");
        meta2_1.addAttribute(new MetadataAttribute("attrib2_1", ProductData.createInstance("meta2_1_value"), true));
        meta2.addElement(meta2_1);

        final MetadataElement metadataRoot = sp.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("attribRoot", ProductData.createInstance("rootValue"), true));
        metadataRoot.addElement(meta1);
        metadataRoot.addElement(meta2);
    }

    private Product createTestProduct(int w, int h) {
        Product testProduct = new Product("p", "t", w, h);

        Band band1 = testProduct.addBand("radiance_1", ProductData.TYPE_INT32);
        int[] intValues = new int[w * h];
        Arrays.fill(intValues, 1);
        band1.setData(ProductData.createInstance(intValues));

        Band band2 = testProduct.addBand("radiance_2", ProductData.TYPE_FLOAT32);
        float[] floatValues = new float[w * h];
        Arrays.fill(floatValues, 2.5f);
        band2.setData(ProductData.createInstance(floatValues));

        Band band3 = testProduct.addBand("radiance_3", ProductData.TYPE_INT16);
        band3.setScalingFactor(0.5);
        short[] shortValues = new short[w * h];
        Arrays.fill(shortValues, (short) 6);
        band3.setData(ProductData.createInstance(shortValues));

        return testProduct;
    }
}