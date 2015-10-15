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

package org.esa.snap.core.gpf;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.io.IOException;


public class InheritedOperatorTest extends TestCase {


    public void testBasicOperatorStates() throws OperatorException, IOException {
        Product sourceProduct = new Product("test", "test", 10, 10);

        DerivedOp op = new DerivedOp();
        op.setSourceProduct(sourceProduct);
        op.setParameter("canExplode", true);

        Product targetProduct = op.getTargetProduct();
        assertSame(targetProduct, sourceProduct);

        assertEquals(true, op.canExplode);
    }

    private static class BaseOp extends Operator {
        @SourceProduct
        Product sourceProduct;
        @TargetProduct
        Product targetProduct;
        @Parameter
        boolean canExplode;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = sourceProduct;
        }
    }

    private static class DerivedOp extends BaseOp {
        @Override
        public void initialize() throws OperatorException {
            super.initialize();
        }
    }
}
