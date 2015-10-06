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

package org.esa.snap.core.gpf.graph;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.TestOps;
import org.esa.snap.core.gpf.internal.OperatorProductReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@Ignore("Method GraphOp#setOperatorParameters(...) needs to be implemented.")
public class GraphOpTest {

    private final TestOps.Op1.Spi operatorSpi1 = new TestOps.Op1.Spi();
    private final TestOps.Op2.Spi operatorSpi2 = new TestOps.Op2.Spi();
    private OperatorSpi graphOpSpiOneNode;
    private OperatorSpi graphOpSpiTwoNodes;
    private OperatorSpi usesOtherGraphSpi;

    private static OperatorSpi createOp2GraphSpi() throws GraphException {
        String graphOpXml =
                "<graph id=\"Op2Graph\">\n" +
                        "  <version>1.0</version>\n" +

                        "  <header>\n" +
                        "    <target refid=\"node2\" />\n" +
                        "    <source name=\"toa\" description=\"AATSR L1b TOA\"/>\n" +
                        "    <parameter name=\"THR\" defaultValue=\"42.0\" type=\"double\"/>\n" +
                        "  </header>\n" +

                        "  <node id=\"node2\">\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input refid=\"toa\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold refid=\"THR\"/>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        return new GraphOp.Spi(graph) {};
    }

    private static OperatorSpi createSourcelessOp1Op2GraphSpi() throws GraphException {
        String graphOpXml =
                "<graph id=\"SourcelessOp1Op2Graph\">\n" +
                        "  <version>1.0</version>\n" +

                        "  <header>\n" +
                        "    <target refid=\"node2\" />\n" +
                        "    <parameter name=\"THR\" defaultValue=\"42.0\" type=\"double\"/>\n" +
                        "  </header>\n" +

                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "  </node>\n" +

                        "  <node id=\"node2\">\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input refid=\"node1\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold refid=\"THR\"/>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        return new GraphOp.Spi(graph) {};
    }

    private static OperatorSpi createUsesOtherGraphSpi() throws GraphException {
        String graphOpXml =
                "<graph id=\"UsesOtherGraph\">\n" +
                        "  <version>1.0</version>\n" +

                        "  <node id=\"node1\">\n" +
                        "    <operator>SourcelessOp1Op2Graph</operator>\n" +
                        "    <parameters>\n" +
                        "       <THR>33</THR>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        return new GraphOp.Spi(graph) {};
    }

    @Before
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi2);
        graphOpSpiOneNode = createOp2GraphSpi();
        graphOpSpiTwoNodes = createSourcelessOp1Op2GraphSpi();
        usesOtherGraphSpi = createUsesOtherGraphSpi();
        assertTrue(GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(graphOpSpiOneNode));
        assertTrue(GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(graphOpSpiTwoNodes));
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(usesOtherGraphSpi);
    }

    @After
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(graphOpSpiOneNode);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(graphOpSpiTwoNodes);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(usesOtherGraphSpi);
    }

    @Test
    public void testParameterValuesAreUsedFromHeader() throws Exception {

        Map<String, Product> sourceProducts = new HashMap<String, Product>(1);
        Product testProduct = new Product("p", "t", 1, 1);
        testProduct.addBand("Op1A", ProductData.TYPE_INT8);
        sourceProducts.put("toa", testProduct);

        Product targetProduct = GPF.createProduct("Op2Graph", new HashMap<String, Object>(), sourceProducts);
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (TestOps.Op2) operator;
        assertEquals(24.0, op2.threshold, 0.00001);
    }

    @Test
    public void testOneNodeDirectCall() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>(1);
        parameters.put("THR", 65.0);

        Map<String, Product> sourceProducts = new HashMap<String, Product>(1);
        Product testProduct = new Product("p", "t", 1, 1);
        testProduct.addBand("Op1A", ProductData.TYPE_INT8);
        sourceProducts.put("toa", testProduct);

        Product targetProduct = GPF.createProduct("Op2Graph", parameters, sourceProducts);
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (TestOps.Op2) operator;
        assertEquals(65.0, op2.threshold, 0.00001);
    }

    @Test
    public void testTwoNodesDirectCall() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>(1);
        parameters.put("THR", 66.0);

        Product targetProduct = GPF.createProduct("SourcelessOp1Op2Graph", parameters, Collections.EMPTY_MAP);
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (TestOps.Op2) operator;
        assertEquals(66.0, op2.threshold, 0.00001);
    }

    @Test
    public void testUsesOtherGraph() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>(1);
        parameters.put("THR", 67.0);

        Product targetProduct = GPF.createProduct("UsesOtherGraph", parameters, Collections.EMPTY_MAP);
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (TestOps.Op2) operator;
        assertEquals(67.0, op2.threshold, 0.00001);
    }

}
