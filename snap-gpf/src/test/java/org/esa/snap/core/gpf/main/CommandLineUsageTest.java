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

package org.esa.snap.core.gpf.main;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommandLineUsageTest {
    final static FooOpSpi FOO_OP_SPI = new FooOpSpi();

    @BeforeClass
    public static void setupTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(FOO_OP_SPI);
    }

    @AfterClass
    public static void tearDownTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(FOO_OP_SPI);
    }

    @Test
    public void testOperatorUsageText() throws NoSuchFieldException {
        String usageText = CommandLineUsage.getUsageTextForOperator("FooOp");
        Assert.assertEquals("Usage:\n" +
                            "  gpt FooOp [options] \n" +
                            "\n" +
                            "\n" +
                            "Source Options:\n" +
                            "  -Sinput1=<file>    Sets source 'input1' to <filepath>.\n" +
                            "                     This is a mandatory source.\n" +
                            "  -Sinput2=<file>    Sets source 'input2' to <filepath>.\n" +
                            "                     This is a mandatory source.\n" +
                            "\n" +
                            "Parameter Options:\n" +
                            "  -Py=<string>    Sets parameter 'y' to <string>.\n" +
                            "  -Pz=<float>     Sets parameter 'z' to <float>.\n" +
                            "\n" +
                            "Graph XML Format:\n" +
                            "  <graph id=\"someGraphId\">\n" +
                            "    <version>1.0</version>\n" +
                            "    <node id=\"someNodeId\">\n" +
                            "      <operator>FooOp</operator>\n" +
                            "      <sources>\n" +
                            "        <input1>${input1}</input1>\n" +
                            "        <input2>${input2}</input2>\n" +
                            "      </sources>\n" +
                            "      <parameters>\n" +
                            "        <z>float</z>\n" +
                            "        <y>string</y>\n" +
                            "        <bar>\n" +
                            "          <c>float</c>\n" +
                            "          <b>string</b>\n" +
                            "        </bar>\n" +
                            "      </parameters>\n" +
                            "    </node>\n" +
                            "  </graph>\n",
                            usageText);
    }


    public static class FooOpSpi extends OperatorSpi {
        public FooOpSpi() {
            super(FooOp.class);
        }
    }


    static class FooOp extends Operator {
        @SourceProduct(alias = "input1")
        Product sourceProduct1;

        @SourceProduct(alias = "input2")
        Product sourceProduct2;

        @Parameter(alias = "z")
        float x;

        @Parameter
        String y;

        @Parameter
        Bar bar;

        @Parameter
        @Deprecated
        float baz;

        @Override
        public void initialize() throws OperatorException {
        }
    }

    static class Bar {

        @Parameter(alias = "c")
        float a;

        String b;

        final double notMe1 = 0.42;
        static String notMe2 ;
        transient int notMe3;
    }
}
