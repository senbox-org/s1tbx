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

package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.TestOps.Op2;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;
import org.esa.beam.util.jai.VerbousTileCache;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GraphOpTest extends TestCase {

    private TestOps.Op1.Spi operatorSpi1 = new TestOps.Op1.Spi();
    private TestOps.Op2.Spi operatorSpi2 = new TestOps.Op2.Spi();
    private OperatorSpi graphOpSpiOneNode;
    private OperatorSpi graphOpSpiTwoNodes;

    private static OperatorSpi createGraphOpSpiOneNode() throws GraphException {
        String graphOpXml =
                "<graph id=\"myOneNodeGraph\">\n" +
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

        return new GraphOp.Spi(graph);
    }

    private static OperatorSpi createGraphOpSpiTwoNodes() throws GraphException {
        String graphOpXml =
                "<graph id=\"myOneNodeGraph\">\n" +
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

        return new GraphOp.Spi(graph);
    }

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi2);
        graphOpSpiOneNode = createGraphOpSpiOneNode();
        graphOpSpiTwoNodes = createGraphOpSpiTwoNodes();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(graphOpSpiOneNode);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(graphOpSpiTwoNodes);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(graphOpSpiOneNode);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(graphOpSpiTwoNodes);
    }

    public void testOneNodeDirectCall() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>(1);
        parameters.put("THR", 66.0);

        Map<String, Product> sourceProducts = new HashMap<String, Product>(1);
        Product testProduct = new Product("p", "t", 1, 1);
        testProduct.addBand("Op1A", ProductData.TYPE_INT8);
        sourceProducts.put("toa", testProduct);

        Operator op = graphOpSpiOneNode.createOperator(parameters, sourceProducts);

        assertNotNull(op);
        assertTrue(op instanceof GraphOp);
        Product actualSourceProduct = op.getSourceProduct("toa");
        assertNotNull(actualSourceProduct);
        assertSame(testProduct, actualSourceProduct);
        Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (Op2) operator;
        assertEquals(66.0, op2.threshold, 0.00001);
    }

    public void testTwoNodesDirectCall() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>(1);
        parameters.put("THR", 66.0);

        Operator op = graphOpSpiTwoNodes.createOperator(parameters, Collections.EMPTY_MAP);

        assertNotNull(op);
        assertTrue(op instanceof GraphOp);
        Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals("Op2Name", targetProduct.getName());
        OperatorProductReader operatorProductReader = (OperatorProductReader) targetProduct.getProductReader();
        Operator operator = operatorProductReader.getOperatorContext().getOperator();
        TestOps.Op2 op2 = (Op2) operator;
        assertEquals(66.0, op2.threshold, 0.00001);
    }

    public void notWorkingYetTestGraphEmbeded() throws Exception {

        VerbousTileCache.setVerbous(true);

        Graph graph = new Graph("graph");
        Node node1 = new Node("node1", graphOpSpiTwoNodes.getOperatorAlias());
        DefaultDomElement config = new DefaultDomElement("parameters");
        DefaultDomElement param = new DefaultDomElement("THR");
        param.setValue("33");
        config.addChild(param);
        node1.setConfiguration(config);
        graph.addNode(node1);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = null;
        try {
            graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException e) {
            fail(e.getMessage());
        }

        Product chainOut = graphContext.getOutputProducts()[0];

        assertNotNull(chainOut);
        assertEquals("Op2Name", chainOut.getName());

        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        // - Op3 requires the two bands of Op2
        // - Op2 computes all bands
        // --> Op2 should only be called once
        assertEquals("Op1;Op2;", TestOps.getCalls());
        TestOps.clearCalls();

        VerbousTileCache.setVerbous(false);

    }

}
