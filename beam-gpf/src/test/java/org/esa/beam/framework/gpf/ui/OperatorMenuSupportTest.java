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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OperatorMenuSupportTest {

    private static TestOpSpi testOpSpi;

    @BeforeClass
    public static void beforeClass() {
        testOpSpi = new TestOpSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(testOpSpi);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(testOpSpi);
    }


    @Test
    public void testStoreAndLoadParameter() throws IOException, ValidationException, ConversionException {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        final PropertySet container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Tester",
                                                                                                          parameterMap);
        container.setValue("paramDouble", 0.42);
        container.setValue("paramString", "A String!");
        container.setValue("paramComplex", new Complex(25));
        final OperatorMenuSupport support = new OperatorMenuSupport(null, TestOp.class, container, "");
        final DomElement domElement = support.toDomElement();

        assertEquals("parameters", domElement.getName());
        assertEquals(3, domElement.getChildCount());
        assertNotNull(domElement.getChild("paramDouble"));
        assertEquals("0.42", domElement.getChild("paramDouble").getValue());
        assertNotNull(domElement.getChild("paramString"));
        assertEquals("A String!", domElement.getChild("paramString").getValue());
        assertNotNull(domElement.getChild("paramComplex"));
        assertNotNull(domElement.getChild("paramComplex").getChild("complexInt"));
        assertEquals("25", domElement.getChild("paramComplex").getChild("complexInt").getValue());

        // change container
        container.setValue("paramDouble", 23.67);
        container.setValue("paramString", "Another String");
        container.setValue("paramComplex", new Complex(17));

        support.fromDomElement(domElement);
        assertEquals(0.42, support.getParameters().getValue("paramDouble"));
        assertEquals("A String!", support.getParameters().getValue("paramString"));
        assertEquals(new Complex(25), support.getParameters().getValue("paramComplex"));
    }

    @Test
    public void testOperatorDescription() throws Exception {
        final OperatorMenuSupport support = new OperatorMenuSupport(null, TestOp.class, null, "");

        assertEquals("Tester", support.getOperatorName());

        String operatorDescription = support.getOperatorDescription();
        assertFalse(operatorDescription.isEmpty());
        assertTrue(operatorDescription.contains("<tr><td><b>Full name:</b></td><td><code>org.esa.beam.framework.gpf.ui.OperatorMenuSupportTest$TestOp</code></td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Authors:</b></td><td>Nobody</td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Version:</b></td><td>42</td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Purpose:</b></td><td>This is very stupid operator.</td></tr>"));
    }

    @OperatorMetadata(alias = "Tester",
                      authors = "Nobody",
                      version = "42",
                      description = "This is very stupid operator.")
    public class TestOp extends Operator {

        @Parameter
        private double paramDouble;

        @Parameter
        private String paramString;

        @Parameter
        private Complex paramComplex;

        @Override
        public void initialize() throws OperatorException {
        }

    }

    public static class TestOpSpi extends OperatorSpi {

        protected TestOpSpi() {
            super(TestOp.class);
        }
    }

    public static class Complex {

        public Complex() {
            this(-1);
        }

        private Complex(int complexInt) {
            this.complexInt = complexInt;
        }

        @Parameter
        private int complexInt;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Complex complex = (Complex) o;

            if (complexInt != complex.complexInt) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return complexInt;
        }
    }
}
