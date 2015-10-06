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

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.TestOps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.fail;

public class GraphIOTest {

    private TestOps.Op1.Spi operatorSpi1 = new TestOps.Op1.Spi();
    private TestOps.Op2.Spi operatorSpi2 = new TestOps.Op2.Spi();
    private TestOps.Op3.Spi operatorSpi3 = new TestOps.Op3.Spi();
    private TestOps.Op4.Spi operatorSpi4 = new TestOps.Op4.Spi();

    @Before
    public void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi3);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi4);
    }

    @After
    public void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi3);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi4);
    }

    @Test
    public void testEmptyChain() throws Exception {
        Graph chain1 = new Graph("myEmptyChain");

        Graph chain2 = doIO(chain1);

        Node[] nodes = chain2.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.length);

        assertEquals(chain1.getId(), chain2.getId());
    }

    @Test
    public void testOneNodeChain() throws Exception {
        Graph graph1 = new Graph("myOneNodeGraph");
        Node node1 = new Node("node1", "Op1");
        graph1.addNode(node1);

        Graph chain2 = doIO(graph1);

        Node[] nodes = chain2.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.length);
        assertNotNull(nodes[0]);

        assertEquals(graph1.getId(), chain2.getId());
        Node node1Copy = chain2.getNode("node1");
        assertSame(nodes[0], node1Copy);
        assertEquals("Op1", node1Copy.getOperatorName());
    }

    @Test
    public void testWriteToXML() throws Exception {
        Graph graph1 = new Graph("myOneNodeGraph");
        Node node1 = new Node("node1", "Op1");
        graph1.addNode(node1);

        StringWriter writer = new StringWriter();
        GraphIO.write(graph1, writer);
        String actualXML = writer.toString();
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <version>1.0</version>\n" +
                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources/>\n" +
                        "  </node>\n" +
                        "</graph>";
        assertEquals(expectedXML, actualXML);
    }

    @Test
    public void testWriteToXMLWithApplicationData() throws Exception {
        Graph graph1 = new Graph("myOneNodeGraph");
        Node node1 = new Node("node1", "Op1");
        graph1.addNode(node1);

        XppDom xpp3Dom = new XppDom("");
        XppDom font = new XppDom("font");
        font.setValue("big");
        xpp3Dom.addChild(font);
        graph1.setAppData("foo", xpp3Dom);

        XppDom xpp3Dom2 = new XppDom("");
        XppDom colour = new XppDom("colour");
        colour.setValue("red");
        xpp3Dom2.addChild(colour);
        graph1.setAppData("baz", xpp3Dom2);

        StringWriter writer = new StringWriter();
        GraphIO.write(graph1, writer);
        String actualXML = writer.toString();
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <version>1.0</version>\n" +
                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources/>\n" +
                        "  </node>\n" +
                        "  <applicationData id=\"foo\">\n" +
                        "    <font>big</font>\n" +
                        "  </applicationData>\n" +
                        "  <applicationData id=\"baz\">\n" +
                        "    <colour>red</colour>\n" +
                        "  </applicationData>\n" +
                        "</graph>";
        assertEquals(expectedXML, actualXML);

    }

    @Test
    public void testReadXMLWithoutVersion() throws Exception {
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources/>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(expectedXML);
        Exception caught = null;
        try {
            GraphIO.read(reader);
            fail("version should be checked");
        } catch (GraphException e) {
            caught = e;
        }
        assertNotNull(caught);
        assertEquals(GraphException.class, caught.getClass());
    }

    @Test
    public void testReadFromXML() throws Exception {
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <version>1.0</version>\n" +
                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources/>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(expectedXML);
        Graph graph = GraphIO.read(reader);

        Node[] nodes = graph.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.length);
        assertNotNull(nodes[0]);

        assertEquals("myOneNodeGraph", graph.getId());
        Node node1 = graph.getNode("node1");
        assertNotNull(node1);
        assertEquals("Op1", node1.getOperatorName());
    }

    @Test
    public void testReadFromXMLWithAppData() throws Exception {
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <version>1.0</version>\n" +
                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources/>\n" +
                        "  </node>\n" +

                        " <applicationData id=\"foo\">\n" +
                        "    <font>Big</font>\n" +
                        "    <colour>red</colour>\n" +
                        " </applicationData>\n" +
                        " <applicationData id=\"bar\">\n" +
                        "    <textmode>true</textmode>\n" +
                        " </applicationData>\n" +

                        "</graph>";
        StringReader reader = new StringReader(expectedXML);
        Graph graph = GraphIO.read(reader);

        XppDom fooData = graph.getApplicationData("foo");
        assertNotNull(fooData);
        assertEquals(2, fooData.getChildCount());
        assertEquals("Big", fooData.getChild("font").getValue());
        assertEquals("red", fooData.getChild("colour").getValue());

        XppDom barData = graph.getApplicationData("bar");
        assertNotNull(barData);
        assertEquals(1, barData.getChildCount());
        assertEquals("true", barData.getChild("textmode").getValue());

    }

    @Test
    public void testReadFromXMLWithHeader() throws Exception {
        String expectedXML =
                "<graph id=\"myOneNodeGraph\">\n" +
                        "  <version>1.0</version>\n" +

                        "  <header>\n" +
                        "    <target refid=\"node1\" />\n" +

                        "    <source name=\"input1\" optional=\"true\" description=\"AATSR L1b TOA\"/>\n" +  // -Sinput1=FILE_PATH
                        "    <source name=\"input2\" description=\"CHRIS/proba\">C:\\data\\x.dim</source>\n" +  // -Sinput2=FILE_PATH

                        "    <parameter name=\"ignore\" defaultValue=\"true\" type=\"boolean\"/>\n" + // -PignoreSign=false
                        "    <parameter name=\"regex\" description=\"a regular expression\" type=\"String\"/>\n" +
                        "    <parameter name=\"threshold\" type=\"double\" interval=\"(0,1]\"/>\n" +
                        "    <parameter name=\"ernie\" type=\"int\" valueSet=\"2,4,6,8\"/>\n" +
                        "  </header>\n" +

                        "  <node id=\"node1\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "    <sources>\n" +
                        "      <toa refid=\"input1\"/>\n" +
                        "      <chris refid=\"input2\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <ignoreSign refid=\"ignore\"/>\n" +
                        "       <expression refid=\"regex\"/>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";
        StringReader reader = new StringReader(expectedXML);
        Graph graph = GraphIO.read(reader);

        Header header = graph.getHeader();
        assertNotNull(header);
        assertEquals("node1", header.getTarget().getNodeId());
        List<HeaderSource> sources = header.getSources();
        assertNotNull(sources);
        assertEquals(2, sources.size());
        assertEquals("input1", sources.get(0).getName());
        assertEquals(true, sources.get(0).isOptional());
        assertEquals("AATSR L1b TOA", sources.get(0).getDescription());
        assertEquals("input2", sources.get(1).getName());
        assertEquals(false, sources.get(1).isOptional());
        assertEquals("C:\\data\\x.dim", sources.get(1).getLocation());

        List<HeaderParameter> parameters = header.getParameters();
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("ignore", parameters.get(0).getName());
        assertEquals("true", parameters.get(0).getDefaultValue());
        assertEquals("boolean", parameters.get(0).getType());
        assertEquals("regex", parameters.get(1).getName());
        assertEquals("a regular expression", parameters.get(1).getDescription());
        assertEquals("String", parameters.get(1).getType());
        assertEquals("threshold", parameters.get(2).getName());
        assertEquals("(0,1]", parameters.get(2).getInterval());
        assertEquals("double", parameters.get(2).getType());
        assertEquals("ernie", parameters.get(3).getName());

        final String[] expected = {"2", "4", "6", "8"};
        assertEquals(expected.length, parameters.get(3).getValueSet().length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], parameters.get(3).getValueSet()[i]);
        }
        assertEquals("int", parameters.get(3).getType());

        Node[] nodes = graph.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.length);
        assertNotNull(nodes[0]);

        assertEquals("myOneNodeGraph", graph.getId());
        Node node1 = graph.getNode("node1");
        assertNotNull(node1);
        assertEquals("Op1", node1.getOperatorName());

    }

    @Test
    public void testTwoNodeChain() throws Exception {
        Graph graph1 = new Graph("myTwoNodeGraph");
        Node node1 = new Node("node1", "Op1");

        Node node2 = new Node("node2", "Op2");

        node2.addSource(new NodeSource("key1", "node1"));
        graph1.addNode(node1);
        graph1.addNode(node2);

        Graph graph2 = doIO(graph1);
        assertEquals(graph1.getId(), graph2.getId());

        Node[] nodes = graph2.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.length);
        assertNotNull(nodes[0]);
        assertNotNull(nodes[1]);

        Node node1Copy = graph2.getNode("node1");
        Node node2Copy = graph2.getNode("node2");
        assertSame(nodes[0], node1Copy);
        assertSame(nodes[1], node2Copy);

        NodeSource[] sources1 = node1Copy.getSources();
        assertNotNull(sources1);
        assertEquals(0, sources1.length);

        NodeSource[] sources2 = node2Copy.getSources();
        assertNotNull(sources2);
        assertEquals(1, sources2.length);
        NodeSource source2 = node2Copy.getSource(0);
        assertSame(sources2[0], source2);

        assertEquals("Op1", node1Copy.getOperatorName());
        assertEquals("Op2", node2Copy.getOperatorName());
        assertEquals("node1", source2.getSourceNodeId());
    }

    @Test
    public void testReadXml() throws Exception {
        String xml =
                "<graph id=\"foo\">\n" +
                        "<version>1.0</version>\n" +
                        "  <node id=\"grunt\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "  </node>\n" +
                        "  <node id=\"baz\">\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input refid=\"grunt\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold>0.86</threshold>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "  <node id=\"bar\">\n" +
                        "    <operator>Op3</operator>\n" +
                        "    <sources>\n" +
                        "      <input1 refid=\"grunt\"/>\n" +
                        "      <input2 refid=\"baz\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <ignoreSign>true</ignoreSign>\n" +
                        "       <expression>A+B</expression>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";

        Graph graph = GraphIO.read(new StringReader(xml));
        assertEquals("foo", graph.getId());

        Node[] nodes = graph.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.length);
        assertNotNull(nodes[0]);
        assertNotNull(nodes[1]);

        Node node1 = graph.getNode("grunt");
        Node node2 = graph.getNode("baz");
        Node node3 = graph.getNode("bar");
        assertSame(nodes[0], node1);
        assertSame(nodes[1], node2);
        assertSame(nodes[2], node3);

        assertEquals("Op1", node1.getOperatorName());
        assertEquals("Op2", node2.getOperatorName());
        assertEquals("Op3", node3.getOperatorName());

        NodeSource[] sources1 = node1.getSources();
        assertNotNull(sources1);
        assertEquals(0, sources1.length);

        NodeSource[] sources2 = node2.getSources();
        assertNotNull(sources2);
        assertEquals(1, sources2.length);
        assertEquals("grunt", sources2[0].getSourceNodeId());

        NodeSource[] sources3 = node3.getSources();
        assertNotNull(sources3);
        assertEquals(2, sources3.length);
        assertEquals("grunt", sources3[0].getSourceNodeId());
        assertEquals("baz", sources3[1].getSourceNodeId());

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = null;
        try {
            graphContext = new GraphContext(graph);
        } catch (GraphException e) {
            fail(e.getMessage());
        }

        NodeContext nodeContext2 = graphContext.getNodeContext(node2);
        TestOps.Op2 op2 = (TestOps.Op2) nodeContext2.getOperator();
        assertEquals(0.86, op2.threshold, 1.e-10);

        NodeContext nodeContext3 = graphContext.getNodeContext(node3);
        TestOps.Op3 op3 = (TestOps.Op3) nodeContext3.getOperator();
        assertEquals(true, op3.ignoreSign);
        assertEquals("A+B", op3.expression);
    }

    private Graph doIO(Graph chain1) throws Exception {
        StringWriter writer = new StringWriter();
        GraphIO.write(chain1, writer);
        String xml = writer.toString();
//         System.out.println("xml = " + xml);
        StringReader reader = new StringReader(xml);
        return GraphIO.read(reader);
    }

    @Test
    public void testGraphWithReference() throws Exception {
        String xml =
                "<graph id=\"foo\">\n" +
                        "<version>1.0</version>\n" +
                        "  <node id=\"bert\">\n" +
                        "    <operator>Op4</operator>\n" +
                        "  </node>\n" +
                        "  <node id=\"baz\">\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input refid=\"bert\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold refid=\"bert.pi\"/>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";

        Graph graph = GraphIO.read(new StringReader(xml));

        final Node bertNode = graph.getNode("bert");
        assertEquals("Op4", bertNode.getOperatorName());
        final Node bazNode = graph.getNode("baz");
        assertEquals("Op2", bazNode.getOperatorName());

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = null;
        try {
            graphContext = new GraphContext(graph);
        } catch (GraphException e) {
            fail(e.getMessage());
        }

        NodeContext bertNodeContext = graphContext.getNodeContext(bertNode);
        TestOps.Op4 op4 = (TestOps.Op4) bertNodeContext.getOperator();
        final Object targetProperty = op4.getTargetProperty("pi");
        assertNotNull(targetProperty);
        assertEquals(3.142, (Double) targetProperty, 0.0);

        NodeContext bazNodeContext = graphContext.getNodeContext(bazNode);
        TestOps.Op2 op2 = (TestOps.Op2) bazNodeContext.getOperator();
        assertEquals(3.142, op2.threshold, 0.0);
    }

    @Test
    public void testGraphWithReferenceWithoutSourceProduct() throws Exception {
        String xml =
                "<graph id=\"foo\">\n" +
                        "<version>1.0</version>\n" +
                        "  <node id=\"grunt\">\n" +
                        "    <operator>Op1</operator>\n" +
                        "  </node>\n" +
                        "  <node id=\"bert\">\n" +
                        "    <operator>Op4</operator>\n" +
                        "  </node>\n" +
                        "  <node id=\"baz\">\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input refid=\"grunt\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold refid=\"bert.pi\"/>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";

        Graph graph = GraphIO.read(new StringReader(xml));

        final Node gruntNode = graph.getNode("grunt");
        assertEquals("Op1", gruntNode.getOperatorName());
        final Node bazNode = graph.getNode("baz");
        assertEquals("Op2", bazNode.getOperatorName());
        final Node bertNode = graph.getNode("bert");
        assertEquals("Op4", bertNode.getOperatorName());

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = null;
        try {
            graphContext = new GraphContext(graph);
        } catch (GraphException e) {
            fail(e.getMessage());
        }

        NodeContext bertNodeContext = graphContext.getNodeContext(bertNode);
        TestOps.Op4 op4 = (TestOps.Op4) bertNodeContext.getOperator();
        final Object targetProperty = op4.getTargetProperty("pi");
        assertNotNull(targetProperty);
        assertEquals(3.142, (Double) targetProperty, 0.0);

        NodeContext bazNodeContext = graphContext.getNodeContext(bazNode);
        TestOps.Op2 op2 = (TestOps.Op2) bazNodeContext.getOperator();
        assertEquals(3.142, op2.threshold, 0.0);
    }

    @Test
    public void testAddSourceProductsVariable() {
        Map<String, String> sourceProductNamesMap = new HashMap<>();
        sourceProductNamesMap.put("sourceProduct", "no1");
        sourceProductNamesMap.put("sourceProduct1", "no2");
        sourceProductNamesMap.put("sourceProduct2", "no3");
        sourceProductNamesMap.put("sourceProduct3", "no4");

        Map<String, String> sourceProductNamesMap2 = new HashMap<>();
        sourceProductNamesMap2.put("sourceProduct1", "no2");
        sourceProductNamesMap2.put("sourceProduct", "no1");
        sourceProductNamesMap2.put("sourceProduct3", "no4");
        sourceProductNamesMap2.put("sourceProduct1", "no2");
        sourceProductNamesMap2.put("sourceProduct2", "no3");
        sourceProductNamesMap2.put("sourceProduct.2", "no3");
        sourceProductNamesMap2.put("sourceProduct3", "no4");
        sourceProductNamesMap2.put("sourceProduct.3", "no4");

        GraphIO.addSourceProductsVariable(sourceProductNamesMap);
        GraphIO.addSourceProductsVariable(sourceProductNamesMap2);

        assertEquals(true, sourceProductNamesMap.containsKey("sourceProducts"));
        assertEquals(true, sourceProductNamesMap2.containsKey("sourceProducts"));
        assertEquals("no2,no3,no4", sourceProductNamesMap.get("sourceProducts"));
        assertEquals("no2,no3,no4", sourceProductNamesMap2.get("sourceProducts"));
    }

}
