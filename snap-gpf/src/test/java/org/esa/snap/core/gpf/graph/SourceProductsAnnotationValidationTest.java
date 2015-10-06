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

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;

public class SourceProductsAnnotationValidationTest extends TestCase {

    private OperatorSpi someOpSpi;
    private OperatorSpi twoSourcesOpSpi;
    private OperatorSpi anySourcesOpSpi;
    private OptSourcesOp.Spi optSourcesOpSpi;

    @Override
    protected void setUp() throws Exception {
        someOpSpi = new InputOp.Spi();
        twoSourcesOpSpi = new TwoSourcesOp.Spi();
        anySourcesOpSpi = new AnySourcesOp.Spi();
        optSourcesOpSpi = new OptSourcesOp.Spi();

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(someOpSpi);
        registry.addOperatorSpi(twoSourcesOpSpi);
        registry.addOperatorSpi(anySourcesOpSpi);
        registry.addOperatorSpi(optSourcesOpSpi);
    }

    @Override
    protected void tearDown() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.removeOperatorSpi(someOpSpi);
        registry.removeOperatorSpi(twoSourcesOpSpi);
        registry.removeOperatorSpi(anySourcesOpSpi);
        registry.removeOperatorSpi(optSourcesOpSpi);
    }

    public void testTwoSourcesOp() {
        final String opName = "TwoSourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphContext(graph);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        try {
            new GraphContext(graph);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy1", "input1"));
        outputNode.addSource(new NodeSource("dummy2", "input2"));
        try {
            new GraphContext(graph);
        } catch (GraphException ge) {
            fail("GraphException not expected, exactly 2 sources given. Error: " + ge.getMessage());
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy1", "input1"));
        outputNode.addSource(new NodeSource("dummy2", "input2"));
        outputNode.addSource(new NodeSource("dummy3", "input3"));
        try {
            new GraphContext(graph);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }
    }

    public void testAnySourcesOp() {
        final String opName = "AnySourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphContext(graph);
            fail("GraphException expected, at least one source expected");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy1", "input1"));
        outputNode.addSource(new NodeSource("dummy2", "input2"));
        outputNode.addSource(new NodeSource("dummy3", "input3"));
        try {
            new GraphContext(graph);
        } catch (GraphException ge) {
            fail("GraphException not expected, any number of sources allowed. Error: " + ge.getMessage());
        }
    }

    public void testOptSourcesOp() {
        final String opName = "OptSourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphContext(graph);
        } catch (GraphException ge) {
            fail("GraphException not expected, sources not checked. Error: " + ge.getMessage());
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy1", "input1"));
        outputNode.addSource(new NodeSource("dummy2", "input2"));
        outputNode.addSource(new NodeSource("dummy3", "input3"));
        try {
            new GraphContext(graph);
        } catch (GraphException ge) {
            fail("GraphException not expected, sources not checked. Error: " + ge.getMessage());
        }
    }


    private Graph createTestGraph(String opName) {
        Graph graph = new Graph("graph");
        Node input1Node = new Node("input1", "InputOp");
        Node input2Node = new Node("input2", "InputOp");
        Node input3Node = new Node("input3", "InputOp");
        Node outputNode = new Node("output", opName);
        graph.addNode(input1Node);
        graph.addNode(input2Node);
        graph.addNode(input3Node);
        graph.addNode(outputNode);
        return graph;
    }

    @OperatorMetadata(alias = "InputOp")
    public static class InputOp extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("input", "inputType", 1, 1);
            targetProduct.addBand("a", ProductData.TYPE_INT8);
            targetProduct.addBand("b", ProductData.TYPE_INT8);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(InputOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "TwoSourcesOp")
    public static class TwoSourcesOp extends Operator {

        @SourceProducts(count = 2)
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(TwoSourcesOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "AnySourcesOp")
    public static class AnySourcesOp extends Operator {

        @SourceProducts(count = -1)
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(AnySourcesOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "OptSourcesOp")
    public static class OptSourcesOp extends Operator {

        @SourceProducts
        // count=0
                Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(OptSourcesOp.class);
            }
        }
    }
}
