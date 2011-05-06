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

package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.gpf.GPF;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class OperatorMenuTest {

    private static OperatorParameterSupportTest.TestOpSpi testOpSpi;

    @BeforeClass
    public static void beforeClass() {
        testOpSpi = new OperatorParameterSupportTest.TestOpSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(testOpSpi);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(testOpSpi);
    }


    @Test
    public void testOperatorDescription() throws Exception {
        final OperatorMenu support = new OperatorMenu(null, OperatorParameterSupportTest.TestOp.class, null, "");

        assertEquals("Tester", support.getOperatorName());

        String operatorDescription = support.getOperatorDescription();
        assertFalse(operatorDescription.isEmpty());
        assertTrue(operatorDescription.contains("<tr><td><b>Full name:</b></td><td><code>org.esa.beam.framework.gpf.ui.OperatorParameterSupportTest$TestOp</code></td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Authors:</b></td><td>Nobody</td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Version:</b></td><td>42</td></tr>"));
        assertTrue(operatorDescription.contains("<tr><td><b>Purpose:</b></td><td>This is very stupid operator.</td></tr>"));
    }
}
