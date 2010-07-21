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

package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.internal.OperatorClassDescriptor;

public class OperatorSpiTest extends TestCase {

    public void testNonAnnotatoedOpConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class) {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("NonAnnotatoedFooOp", operatorSpi.getOperatorAlias());
    }

    public void testNonAnnotatoedOpConstructionWithName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class, "foo") {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getOperatorAlias());
    }

    public void testAnnotatoedOpConstructionWithoutName() throws NoSuchFieldException {
        OperatorSpi operatorSpi = new OperatorSpi(AnnotatedFooOp.class) {
        };
        assertSame(AnnotatedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("FooFighters", operatorSpi.getOperatorAlias());

        OperatorClassDescriptor opDescriptor = new OperatorClassDescriptor(operatorSpi.getOperatorClass());
        assertNotNull(opDescriptor.getOperatorMetadata());
        assertNotNull(opDescriptor.getParameters());
        assertEquals(1, opDescriptor.getParameters().size());
        assertNotNull(opDescriptor.getParameters().get(AnnotatedFooOp.class.getDeclaredField("threshold")));
        assertNotNull(opDescriptor.getSourceProductMap());
        assertEquals(1, opDescriptor.getSourceProductMap().size());
        assertNotNull(opDescriptor.getSourceProductMap().get(AnnotatedFooOp.class.getDeclaredField("input")));
    }

    public static class NonAnnotatoedFooOp extends Operator {

        @Override
        public void initialize() throws OperatorException {
        }
    }

    @OperatorMetadata(alias = "FooFighters")
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
