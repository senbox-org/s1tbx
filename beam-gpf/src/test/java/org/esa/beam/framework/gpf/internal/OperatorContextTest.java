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

package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import static org.junit.Assert.*;
import org.junit.Test;

@SuppressWarnings({"PackageVisibleField"})
public class OperatorContextTest {

    @Test
    public void testSourceProductAnnotations() {
        final TestOperator testOp = new TestOperator();
        final OperatorContext opContext = new OperatorContext(testOp);

        final Product productP1Bibo = new Product("p1-bibo", "T", 10, 10);
        final Product productP2Bert = new Product("p2-bert", "T", 10, 10);
        final Product productUnnamed = new Product("unnamed", "T", 10, 10);
        Product[] products = new Product[]{
                new Product("John Doe", "T", 10, 10),
                new Product("Jane Doe", "T", 10, 10)
        };
        opContext.setSourceProducts(products);
        opContext.setSourceProduct("bibo", productP1Bibo);
        opContext.setSourceProduct("p2", productP2Bert);
        opContext.setSourceProduct("sourceProduct", productUnnamed);

        opContext.getTargetProduct();

        assertNotNull(testOp.p1);
        assertSame(testOp.p1, productP1Bibo);
        assertNotNull(testOp.p2);
        assertSame(testOp.p2, productP2Bert);
        assertNotNull(testOp.products);
        assertEquals(3, testOp.products.length);

    }

    private static class TestOperator extends Operator {

        @SourceProduct(alias = "bibo")
        Product p1;

        @SourceProduct(alias = "bert")
        Product p2;

        @SourceProducts()
        Product[] products;

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("target", "T", 10, 10);
        }
    }
}
