/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.common;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MergeOpTest {

    @Test
    public void testMergeOp_includeAll() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 10, 10);

        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("B"));
        assertEquals("dummy1", mergedProduct.getName());
    }

    @Test
    public void testMergeOp_usedForRenaming() throws Exception {
        final Product master = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product second = new Product("dummy2", "mergeOpTest", 10, 10);

        master.addBand("A", ProductData.TYPE_FLOAT32);
        master.addBand("B", ProductData.TYPE_FLOAT32);
        master.addBand("C", ProductData.TYPE_FLOAT32);
        second.addBand("John", ProductData.TYPE_FLOAT32);

        final MergeOp.NodeDescriptor aDescriptor = new MergeOp.NodeDescriptor();
        aDescriptor.setName("A");
        aDescriptor.setNewName("Alvin");
        aDescriptor.setProductId("masterProduct");
        final MergeOp.NodeDescriptor bDescriptor = new MergeOp.NodeDescriptor();
        bDescriptor.setName("B");
        bDescriptor.setNewName("BigBird");
        bDescriptor.setProductId("masterProduct");
        final MergeOp.NodeDescriptor cDescriptor = new MergeOp.NodeDescriptor();
        cDescriptor.setName("C");
        cDescriptor.setNewName("Charlie");
        cDescriptor.setProductId("masterProduct");
        final MergeOp.NodeDescriptor johnDescriptor = new MergeOp.NodeDescriptor();
        johnDescriptor.setName("John");
        johnDescriptor.setNewName("Jane");
        johnDescriptor.setProductId("second");
        final MergeOp.NodeDescriptor[] nodeDescriptors = new MergeOp.NodeDescriptor[]{
                aDescriptor, bDescriptor, cDescriptor, johnDescriptor};

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", master);
        mergeOp.setSourceProduct("second", second);
        mergeOp.setParameter("includes", nodeDescriptors);
        final Product mergedProduct = mergeOp.getTargetProduct();

        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("Alvin"));
        assertTrue(mergedProduct.containsBand("BigBird"));
        assertTrue(mergedProduct.containsBand("Charlie"));
        assertTrue(mergedProduct.containsBand("Jane"));
    }

    @Test
    public void testMergeOp_includeByPattern() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 10, 10);

        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);
        productB.addBand("C", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final MergeOp.NodeDescriptor nodeDescriptor = new MergeOp.NodeDescriptor();
        nodeDescriptor.setNamePattern("B");
        nodeDescriptor.setProductId("dummy2");
        final MergeOp.NodeDescriptor[] nodeDescriptors = new MergeOp.NodeDescriptor[]{nodeDescriptor};
        mergeOp.setParameter("includes", nodeDescriptors);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("B"));
        assertTrue(!mergedProduct.containsBand("C"));
    }

    @Test
    public void testMergeOp_includeByNameAndRename() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 10, 10);

        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);
        productB.addBand("C", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final MergeOp.NodeDescriptor nodeDescriptor = new MergeOp.NodeDescriptor();
        nodeDescriptor.setName("B");
        nodeDescriptor.setNewName("Beeh");
        nodeDescriptor.setProductId("dummy2");
        final MergeOp.NodeDescriptor[] nodeDescriptors = new MergeOp.NodeDescriptor[]{nodeDescriptor};
        mergeOp.setParameter("includes", nodeDescriptors);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("Beeh"));
        assertTrue(!mergedProduct.containsBand("B"));
        assertTrue(!mergedProduct.containsBand("C"));
    }

    @Test
    public void testMergeOp_excludeBandsFromMaster() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);

        productA.addBand("A_rem_0", ProductData.TYPE_FLOAT32);
        productA.addBand("A_rem_1", ProductData.TYPE_FLOAT32);
        productA.addBand("A_rem_2", ProductData.TYPE_FLOAT32);
        productA.addBand("B", ProductData.TYPE_FLOAT32);
        productA.addBand("C", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", productA);
        final MergeOp.NodeDescriptor remDescriptor = new MergeOp.NodeDescriptor();
        remDescriptor.setNamePattern(".*rem.*");
        remDescriptor.setProductId("masterProduct");
        final MergeOp.NodeDescriptor bDescriptor = new MergeOp.NodeDescriptor();
        bDescriptor.setNamePattern("B");
        bDescriptor.setProductId("masterProduct");
        final MergeOp.NodeDescriptor[] excludeDescriptor = new MergeOp.NodeDescriptor[]{remDescriptor, bDescriptor};
        mergeOp.setParameter("excludes", excludeDescriptor);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(!mergedProduct.containsBand("A_rem_0"));
        assertTrue(!mergedProduct.containsBand("A_rem_1"));
        assertTrue(!mergedProduct.containsBand("A_rem_2"));
        assertTrue(!mergedProduct.containsBand("B"));
        assertTrue(mergedProduct.containsBand("C"));
    }

    @Test
    public void testMergeOp_includeAndExclude() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);

        productA.addBand("master", ProductData.TYPE_FLOAT32);
        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productA.addBand("A_rem_1", ProductData.TYPE_FLOAT32);
        productA.addBand("A_rem_2", ProductData.TYPE_FLOAT32);
        productA.addBand("B", ProductData.TYPE_FLOAT32);
        productA.addBand("C", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("masterProduct", productA);
        final MergeOp.NodeDescriptor inclRemDescriptor = new MergeOp.NodeDescriptor();
        inclRemDescriptor.setNamePattern(".*rem.*");
        inclRemDescriptor.setProductId("masterProduct");

        final MergeOp.NodeDescriptor exclRem2Descriptor = new MergeOp.NodeDescriptor();
        exclRem2Descriptor.setNamePattern("A_rem_2");
        exclRem2Descriptor.setProductId("masterProduct");

        final MergeOp.NodeDescriptor[] includeDescriptor = new MergeOp.NodeDescriptor[]{inclRemDescriptor};
        final MergeOp.NodeDescriptor[] excludeDescriptor = new MergeOp.NodeDescriptor[]{exclRem2Descriptor};
        mergeOp.setParameter("includes", includeDescriptor);
        mergeOp.setParameter("excludes", excludeDescriptor);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A_rem_1")); // included  by include descriptor
        assertTrue(!mergedProduct.containsBand("A_rem_2")); // excluded by exclude descriptor
        assertTrue(!mergedProduct.containsBand("A")); // excluded by not being included
        assertTrue(!mergedProduct.containsBand("B")); // excluded by not being included
        assertTrue(!mergedProduct.containsBand("C")); // excluded by not being included
    }

    @Test
    public void testValidateSourceProducts_Failing() throws Exception {
        final MergeOp mergeOp = new MergeOp();

        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 11, 11);

        mergeOp.setSourceProduct("masterProduct", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        try {
            mergeOp.getTargetProduct();
            fail();
        } catch (OperatorException e) {
            final String expectedErrorMessage = "Product .* is not compatible to master product";
            assertTrue("expected: '" + expectedErrorMessage + "', actual: '" + e.getMessage() + "'",
                       e.getMessage().replace(".", "").matches(expectedErrorMessage));
        }
    }
}
