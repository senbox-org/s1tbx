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

package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.Node;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CommandLineToolMultiSourceGraphTest {

    private static final TestOps.Op5.Spi OP5_SPI = new TestOps.Op5.Spi();
    private GraphCommandLineContext context;
    private CommandLineTool clTool;

    @BeforeClass
    public static void setupTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP5_SPI);
    }

    @BeforeClass
    public static void tearDownTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP5_SPI);
    }

    @Before
    public void setUp() throws Exception {
        context = new GraphCommandLineContext();
        clTool = new CommandLineTool(context);
    }

    @Test
    public void testGraphWithTwoSources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("ReadProduct$0", "ernie.dim");
        map.put("ReadProduct$1", "idefix.dim");
        testGraph(new String[]{"graph.xml", "ernie.dim", "idefix.dim"},
                  4,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteProduct$node1", "target.dim", "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWithWith3Sources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("ReadProduct$0", "ernie.dim");
        map.put("ReadProduct$1", "idefix.dim");
        map.put("ReadProduct$2", "obelix.dim");
        testGraph(new String[]{"graph.xml", "ernie.dim", "idefix.dim", "obelix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteProduct$node1", "target.dim", "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWith2SourcesAndOneNamedSource() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("ReadProduct$0", "vincent.dim");
        map.put("ReadProduct$1", "ernie.dim");
        map.put("ReadProduct$2", "idefix.dim");

        testGraph(new String[]{"graph.xml", "-SVincent=vincent.dim", "ernie.dim", "idefix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteProduct$node1",
                  "target.dim",
                  "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWithOnlyNamedSources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("ReadProduct$0", "vincent.dim");
        map.put("ReadProduct$1", "ernie.dim");
        map.put("ReadProduct$2", "idefix.dim");

        testGraph(new String[]{"graph.xml", "-SVincent=vincent.dim", "-Sernie=ernie.dim", "-Sidefix=idefix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteProduct$node1",
                  "target.dim",
                  "BEAM-DIMAP"
        );
    }


    private void testGraph(String[] args,
                           int expectedNodeCount,
                           String expectedLog,
                           Map<String, String> expectedSourceNodeIdFilePathMap,
                           String expectedTargetNodeId,
                           String expectedTargetFilepath,
                           String expectedTargetFormat) throws Exception {
        clTool.run(args);

        assertEquals(expectedLog, context.logString);

        Graph executedGraph = context.executedGraph;
        assertNotNull(executedGraph);
        assertEquals(expectedNodeCount, executedGraph.getNodeCount());

        for (Map.Entry<String, String> entry : expectedSourceNodeIdFilePathMap.entrySet()) {
            String expectedSourceFilePath = entry.getValue();
            if (expectedSourceFilePath != null) {
                Node generatedReaderNode1 = executedGraph.getNode(entry.getKey());
                assertNotNull(generatedReaderNode1);
                assertEquals(expectedSourceFilePath,
                             generatedReaderNode1.getConfiguration().getChild("file").getValue());
            }

        }
        Node generatedWriterNode = executedGraph.getNode(expectedTargetNodeId);
        assertNotNull(generatedWriterNode);
        assertEquals("node1", generatedWriterNode.getSource(0).getSourceNodeId());

        DomElement parameters = generatedWriterNode.getConfiguration();
        assertNotNull(parameters);
        assertNotNull(expectedTargetFilepath, parameters.getChild("file").getValue());
        assertNotNull(expectedTargetFormat, parameters.getChild("formatName").getValue());
    }


    private static class GraphCommandLineContext implements CommandLineContext {

        private String logString;
        private int readProductCounter;
        private int writeProductCounter;
        private Graph executedGraph;

        private GraphCommandLineContext() {
            logString = "";
        }

        @Override
        public Product readProduct(String productFilepath) throws IOException {
            logString += "s" + readProductCounter + "=" + productFilepath + ";";
            readProductCounter++;
            return new Product("P", "T", 10, 10);
        }

        @Override
        public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
            logString += "t" + writeProductCounter + "=" + filePath + ";";
            writeProductCounter++;
        }

        @Override
        public Graph readGraph(String filepath, Map<String, String> templateVariables) throws IOException, GraphException {

            logString += "g=" + filepath + ";";

            String xml =
                    "<graph id=\"chain1\">" +
                            "<version>1.0</version>\n" +
                            "<node id=\"node1\">" +
                            "  <operator>org.esa.beam.framework.gpf.TestOps$Op5$Spi</operator>\n" +
                            "  <sources>\n" +
                            "    <sourceProducts>${sourceProducts}</sourceProducts>\n" +
                            "  </sources>\n" +
                            "</node>" +
                            "</graph>";

            return GraphIO.read(new StringReader(xml), templateVariables);
        }

        @Override
        public void executeGraph(Graph graph) throws GraphException {
            logString += "e=" + graph.getId() + ";";
            executedGraph = graph;
        }


        @Override
        public Product createOpProduct(String opName, Map<String, Object> parameters,
                                       Map<String, Product> sourceProducts) throws OperatorException {
            fail("did not expect to come here");
            return null;
        }

        @Override
        public Map<String, String> readParametersFile(String filePath, Map<String, String> templateVariables) throws IOException {
            return Collections.emptyMap();
        }

        @Override
        public void print(String m) {
        }
    }
}