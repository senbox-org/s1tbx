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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.junit.Test;

import static org.junit.Assert.*;

public class OperatorSpiTest {

    @Test
    public void testNonAnnotatedOperator() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatedFooOp.class) {
        };
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertEquals(NonAnnotatedFooOp.class.getName(), operatorSpi.getOperatorDescriptor().getName());
        assertEquals("NonAnnotatedFooOp", operatorSpi.getOperatorDescriptor().getAlias());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getLabel());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getVersion());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getAuthors());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getDescription());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getCopyright());
        assertEquals(NonAnnotatedFooOp.class, operatorSpi.getOperatorDescriptor().getOperatorClass());
        assertNotNull(operatorSpi.getOperatorDescriptor().getSourceProductDescriptors());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getSourceProductsDescriptor());
        assertNotNull(operatorSpi.getOperatorDescriptor().getParameterDescriptors());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getTargetProductDescriptor());
        assertNotNull(operatorSpi.getOperatorDescriptor().getTargetPropertyDescriptors());

        assertEquals("NonAnnotatedFooOp", operatorSpi.getOperatorAlias());
    }

    @Test
    public void testAnnotatedOperator() {
        OperatorSpi operatorSpi = new OperatorSpi(AnnotatedFooOp.class) {
        };
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertEquals(AnnotatedFooOp.class.getName(), operatorSpi.getOperatorDescriptor().getName());
        assertEquals("fooop", operatorSpi.getOperatorDescriptor().getAlias());
        assertEquals("Foo Operator", operatorSpi.getOperatorDescriptor().getLabel());
        assertEquals("0.1", operatorSpi.getOperatorDescriptor().getVersion());
        assertEquals("F.Bar", operatorSpi.getOperatorDescriptor().getAuthors());
        assertEquals("This is a baz", operatorSpi.getOperatorDescriptor().getDescription());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getCopyright());
        assertEquals(AnnotatedFooOp.class, operatorSpi.getOperatorDescriptor().getOperatorClass());
        assertNotNull(operatorSpi.getOperatorDescriptor().getSourceProductDescriptors());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getSourceProductsDescriptor());
        assertNotNull(operatorSpi.getOperatorDescriptor().getParameterDescriptors());
        assertEquals(null, operatorSpi.getOperatorDescriptor().getTargetProductDescriptor());
        assertNotNull(operatorSpi.getOperatorDescriptor().getTargetPropertyDescriptors());

        assertEquals("fooop", operatorSpi.getOperatorAlias());
    }

    public static class NonAnnotatedFooOp extends Operator {

        @Override
        public void initialize() throws OperatorException {
        }
    }

    @OperatorMetadata(alias = "fooop", label="Foo Operator", version ="0.1", authors = "F.Bar", description = "This is a baz")
    public static class AnnotatedFooOp extends Operator {
        @SourceProduct
        Product input;

        Product auxdata;

        @Parameter
        double threshold;

        int maxCount;

        @Override
        public void initialize() throws OperatorException {
        }
    }
}
