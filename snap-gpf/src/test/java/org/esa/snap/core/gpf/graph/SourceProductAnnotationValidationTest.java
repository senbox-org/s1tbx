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
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

public class SourceProductAnnotationValidationTest extends TestCase {

    private OperatorSpi wrongTypeOpSpi;
    private OperatorSpi wrongBandsOpSpi;
    private OperatorSpi goodOpSpi;
    private OperatorSpi consumerOpSpi;
    private OperatorSpi optionalConsumerOpSpi;
    private OperatorSpi aliasConsumerOpSpi;

    @Override
    protected void setUp() throws Exception {
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        wrongTypeOpSpi = new WrongTypeOperator.Spi();
        spiRegistry.addOperatorSpi(wrongTypeOpSpi);
        wrongBandsOpSpi = new WrongBandsOperator.Spi();
        spiRegistry.addOperatorSpi(wrongBandsOpSpi);
        goodOpSpi = new GoodOperator.Spi();
        spiRegistry.addOperatorSpi(goodOpSpi);
        consumerOpSpi = new ConsumerOperator.Spi();
        spiRegistry.addOperatorSpi(consumerOpSpi);
        aliasConsumerOpSpi = new ConsumerWithAliasSourceOperator.Spi();
        spiRegistry.addOperatorSpi(aliasConsumerOpSpi);
        optionalConsumerOpSpi = new OptionalConsumerOperator.Spi();
        spiRegistry.addOperatorSpi(optionalConsumerOpSpi);

    }

    @Override
    protected void tearDown() {
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(wrongTypeOpSpi);
        spiRegistry.removeOperatorSpi(wrongBandsOpSpi);
        spiRegistry.removeOperatorSpi(goodOpSpi);
        spiRegistry.removeOperatorSpi(consumerOpSpi);
        spiRegistry.removeOperatorSpi(aliasConsumerOpSpi);
        spiRegistry.removeOperatorSpi(optionalConsumerOpSpi);
    }

    public void testForWrongType() {
        Graph graph = new Graph("graph");

        Node wrongTypeNode = new Node("WrongType", wrongTypeOpSpi.getOperatorAlias());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getOperatorAlias());
        consumerNode.addSource(new NodeSource("input1", "WrongType"));
        graph.addNode(wrongTypeNode);
        graph.addNode(consumerNode);

        try {
            new GraphContext(graph);
            fail("GraphException expected caused by wrong type of source product");
        } catch (GraphException ignored) {
        }
    }

    public void testForWrongBands() {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getOperatorAlias());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getOperatorAlias());
        consumerNode.addSource(new NodeSource("input1", "WrongBands"));
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        try {
            new GraphContext(graph);
            fail("GraphException expected, caused by missing bands");
        } catch (GraphException ignored) {
        }
    }

    public void testOptionalAndWrongProductIsGiven() {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getOperatorAlias());
        Node consumerNode = new Node("OptionalConsumer", optionalConsumerOpSpi.getOperatorAlias());
        consumerNode.addSource(new NodeSource("input1", "WrongBands"));
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        try {
            new GraphContext(graph);
            fail("GraphException expected, caused by missing bands, even if optional");
        } catch (GraphException ignored) {
        }
    }

    public void testOptionalAndWrongProductIsNotGiven() throws GraphException {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getOperatorAlias());
        Node consumerNode = new Node("OptionalConsumer", optionalConsumerOpSpi.getOperatorAlias());
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        new GraphContext(graph);
    }

    public void testNotInitialzedInputResultsInException() {
        Graph graph = new Graph("graph");

        Node goodNode = new Node("Good", goodOpSpi.getOperatorAlias());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getOperatorAlias());
        graph.addNode(goodNode);
        graph.addNode(consumerNode);

        try {
            new GraphContext(graph);
            fail("GraphException expected, because input1 is not initialized");
        } catch (GraphException ignored) {
        }
    }

    public void testSourceProductWithAlias() throws GraphException {
        Graph graph = new Graph("graph");

        Node goodNode = new Node("Good", goodOpSpi.getOperatorAlias());
        Node consumerNode = new Node("AliasConsumer", aliasConsumerOpSpi.getOperatorAlias());
        consumerNode.addSource(new NodeSource("alias", "Good"));
        graph.addNode(goodNode);
        graph.addNode(consumerNode);

        GraphContext graphContext = new GraphContext(graph);
        NodeContext consumerNodeContext = graphContext.getNodeContext(consumerNode);
        assertSame(((ConsumerWithAliasSourceOperator) consumerNodeContext.getOperator()).input1,
                   consumerNodeContext.getSourceProduct("alias"));
    }

    @OperatorMetadata(alias="WrongTypeOperator")
    public static class WrongTypeOperator extends Operator {

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("Wrong", "WrongType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(WrongTypeOperator.class);
            }

        }
    }

    @OperatorMetadata(alias="WrongBandsOperator")
    public static class WrongBandsOperator extends Operator {
        @TargetProduct
        private Product targetProduct;
        
        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("WrongBands", "GoodType", 1, 1);
            targetProduct.addBand("x", ProductData.TYPE_INT8);
            targetProduct.addBand("y", ProductData.TYPE_INT8);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(WrongBandsOperator.class);
            }
        }
    }

    @OperatorMetadata(alias="GoodOperator")
    public static class GoodOperator extends Operator {
        @TargetProduct
        private Product targetProduct;
        
        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("Good", "GoodType", 1, 1);
            targetProduct.addBand("a", ProductData.TYPE_INT8);
            targetProduct.addBand("b", ProductData.TYPE_INT8);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(GoodOperator.class);
            }
        }
    }

    @OperatorMetadata(alias="ConsumerOperator")
    public static class ConsumerOperator extends Operator {

        @SourceProduct(type = "GoodType", bands = {"a", "b"})
        Product input1;

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
                super(ConsumerOperator.class);
            }
        }
    }

    @OperatorMetadata(alias="OptionalConsumerOperator")
    public static class OptionalConsumerOperator extends Operator {

        @SourceProduct(optional = true, type = "Optional", bands = {"c", "d"})
        Product input1;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(OptionalConsumerOperator.class);
            }

        }
    }

    @OperatorMetadata(alias="ConsumerWithAliasSourceOperator")
    public static class ConsumerWithAliasSourceOperator extends Operator {

        @SourceProduct(alias = "alias")
        Product input1;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(ConsumerWithAliasSourceOperator.class);
            }
        }
    }
}
