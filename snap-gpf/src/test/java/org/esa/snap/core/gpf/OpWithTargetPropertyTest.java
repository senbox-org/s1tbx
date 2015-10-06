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
import org.esa.snap.core.gpf.annotations.TargetProperty;


public class OpWithTargetPropertyTest extends TestCase {

    public void testTargetPropertyIsComputedDuringOpInitialisation() {
        final Operator op = new PropertyOp();
        final Object targetProperty = op.getTargetProperty("theAnswer");

        assertNotNull(targetProperty);
        assertEquals(42, targetProperty);
    }

    public void testWrongPropertyOp() {
        final Operator op = new IllegalPropertyOp();
        try {
            op.getTargetProperty("theAnswer");
            fail("duplicated property name error expected");
        } catch (OperatorException e) {
        }
    }

    public static class PropertyOp extends Operator {

        @TargetProperty
        int theAnswer;

        @Override
        public void initialize() throws OperatorException {
            theAnswer = 42;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

    public static class IllegalPropertyOp extends Operator {

        @TargetProperty
        int theAnswer;

        @TargetProperty(alias = "theAnswer")
        double anotherAnswer;

        @Override
        public void initialize() throws OperatorException {
            theAnswer = 42;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

}
