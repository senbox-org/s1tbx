package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;

public class SourceProductAnnotationValidationTest extends TestCase {

    private OperatorSpi wrongTypeOpSpi;
    private OperatorSpi wrongBandsOpSpi;
    private OperatorSpi goodOpSpi;
    private OperatorSpi consumerOpSpi;
    private OperatorSpi optionalConsumerOpSpi;
    private OperatorSpi aliasConsumerOpSpi;

    @Override
    protected void setUp() throws Exception {
        wrongTypeOpSpi = new WrongTypeOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(wrongTypeOpSpi);
        wrongBandsOpSpi = new WrongBandsOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(wrongBandsOpSpi);
        goodOpSpi = new GoodOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(goodOpSpi);
        consumerOpSpi = new ConsumerOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(consumerOpSpi);
        aliasConsumerOpSpi = new ConsumerWithAliasSourceOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(aliasConsumerOpSpi);
        optionalConsumerOpSpi = new OptionalConsumerOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(optionalConsumerOpSpi);

    }

    @Override
    protected void tearDown() {
        OperatorSpiRegistry.getInstance().removeOperatorSpi(wrongTypeOpSpi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(wrongBandsOpSpi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(goodOpSpi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(consumerOpSpi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(aliasConsumerOpSpi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(optionalConsumerOpSpi);
    }

    public void testForWrongType() {
        Graph graph = new Graph("graph");

        Node wrongTypeNode = new Node("WrongType", wrongTypeOpSpi.getName());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getName());
        consumerNode.addSource(new NodeSource("input1", "WrongType"));
        graph.addNode(wrongTypeNode);
        graph.addNode(consumerNode);

        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected caused by wrong type of source product");
        } catch (GraphException ge) {
        }
    }

    public void testForWrongBands() {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getName());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getName());
        consumerNode.addSource(new NodeSource("input1", "WrongBands"));
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, caused by not present bands");
        } catch (GraphException ge) {
        }
    }

    public void testOptionalAndWrongProductIsGiven() {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getName());
        Node consumerNode = new Node("OptionalConsumer", optionalConsumerOpSpi.getName());
        consumerNode.addSource(new NodeSource("input1", "WrongBands"));
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, caused by not present bands, even it is optional");
        } catch (GraphException ge) {
        }
    }

    public void testOptionalAndWrongProductIsNotGiven() throws GraphException {
        Graph graph = new Graph("graph");

        Node wrongBandsNode = new Node("WrongBands", wrongBandsOpSpi.getName());
        Node consumerNode = new Node("OptionalConsumer", optionalConsumerOpSpi.getName());
        graph.addNode(wrongBandsNode);
        graph.addNode(consumerNode);

        new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
    }

    public void testNotInitialzedInputResultsInException() {
        Graph graph = new Graph("graph");

        Node goodNode = new Node("Good", goodOpSpi.getName());
        Node consumerNode = new Node("Consumer", consumerOpSpi.getName());
        graph.addNode(goodNode);
        graph.addNode(consumerNode);

        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, because input1 is not initialized");
        } catch (GraphException ge) {
        }
    }

    public void testSourceProductWithAlias() throws GraphException {
        Graph graph = new Graph("graph");

        Node goodNode = new Node("Good", goodOpSpi.getName());
        Node consumerNode = new Node("AliasConsumer", aliasConsumerOpSpi.getName());
        consumerNode.addSource(new NodeSource("alias", "Good"));
        graph.addNode(goodNode);
        graph.addNode(consumerNode);

        GraphContext graphContext = new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        NodeContext consumerNodeContext = graphContext.getNodeContext(consumerNode);
        assertSame(((ConsumerWithAliasSourceOperator) consumerNodeContext.getOperator()).input1,
                   consumerNodeContext.getSourceProduct("alias"));
    }

    public static class WrongTypeOperator extends AbstractOperator {

        @TargetProduct
        Product output;

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            return new Product("Wrong", "WrongType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(WrongTypeOperator.class, "WrongTypeOperator");
            }

        }
    }

    public static class WrongBandsOperator extends AbstractOperator {

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            Product product = new Product("WrongBands", "GoodType", 1, 1);
            product.addBand("x", ProductData.TYPE_INT8);
            product.addBand("y", ProductData.TYPE_INT8);
            return product;
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(WrongBandsOperator.class, "WrongBandsOperator");
            }
        }
    }

    public static class GoodOperator extends AbstractOperator {

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            Product product = new Product("Good", "GoodType", 1, 1);
            product.addBand("a", ProductData.TYPE_INT8);
            product.addBand("b", ProductData.TYPE_INT8);
            return product;
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(GoodOperator.class, "GoodOperator");
            }
        }
    }

    public static class ConsumerOperator extends AbstractOperator {

        @SourceProduct(type = "GoodType", bands = {"a", "b"})
        Product input1;

        @TargetProduct
        Product output;

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            return new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(ConsumerOperator.class, "ConsumerOperator");
            }
        }
    }

    public static class OptionalConsumerOperator extends AbstractOperator {

        @SourceProduct(optional = true, type = "Optional", bands = {"c", "d"})
        Product input1;

        @TargetProduct
        Product output;

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            return new Product("output", "outputType", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(OptionalConsumerOperator.class, "OptionalConsumerOperator");
            }

        }
    }

    public static class ConsumerWithAliasSourceOperator extends AbstractOperator {

        @SourceProduct(alias = "alias")
        Product input1;

        @TargetProduct
        Product output;

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            return new Product("output", "outputType", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(ConsumerWithAliasSourceOperator.class, "ConsumerWithAliasSourceOperator");
            }
        }
    }
}
