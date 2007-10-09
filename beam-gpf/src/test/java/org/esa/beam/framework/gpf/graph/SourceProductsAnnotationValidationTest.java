package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class SourceProductsAnnotationValidationTest extends TestCase {

    private OperatorSpi goodOpSpi;
    private OperatorSpi multiProductsConsumerOpSpi;
    private OperatorSpi multiProductsOptionalConsumerOpSpi;

    @Override
    protected void setUp() throws Exception {
        goodOpSpi = new GoodOperator.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(goodOpSpi);
        multiProductsConsumerOpSpi = new MultipleProductsConsumerOperator.Spi();
        registry.addOperatorSpi(multiProductsConsumerOpSpi);
        multiProductsOptionalConsumerOpSpi = new MultipleProductsOptionalConsumerOperator.Spi();
        registry.addOperatorSpi(multiProductsOptionalConsumerOpSpi);
    }

    @Override
    protected void tearDown() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.removeOperatorSpi(goodOpSpi);
        registry.removeOperatorSpi(multiProductsConsumerOpSpi);
        registry.removeOperatorSpi(multiProductsOptionalConsumerOpSpi);
    }

    public void testWithNoGivenProducts() {
        Graph graph = new Graph("graph");

        Node good1Node = new Node("Good1", goodOpSpi.getName());
        Node good2Node = new Node("Good2", goodOpSpi.getName());
        Node consumerNode = new Node("MultiConsumer", multiProductsConsumerOpSpi.getName());
        graph.addNode(good1Node);
        graph.addNode(good2Node);
        graph.addNode(consumerNode);

        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, cause no products are given");
        } catch (GraphException ge) {
        }
    }

    public void testOptionalWithGivenProducts() throws GraphException {
        Graph graph = new Graph("graph");

        Node good1Node = new Node("Good1", goodOpSpi.getName());
        Node good2Node = new Node("Good2", goodOpSpi.getName());
        Node consumerNode = new Node("MultiConsumer", multiProductsOptionalConsumerOpSpi.getName());
        consumerNode.addSource(new NodeSource("inputs", "Good1"));
        consumerNode.addSource(new NodeSource("inputs", "Good2"));
        graph.addNode(good1Node);
        graph.addNode(good2Node);
        graph.addNode(consumerNode);

        GraphContext graphContext = new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        Product[] sourceProducts = graphContext.getNodeContext(consumerNode).getSourceProducts();
        assertNotNull(sourceProducts);
        assertEquals(2, sourceProducts.length);
        assertSame(graphContext.getNodeContext(good1Node).getTargetProduct(), sourceProducts[0]);
        assertSame(graphContext.getNodeContext(good2Node).getTargetProduct(), sourceProducts[1]);
    }

    public void testOptionalWithNoGivenProducts() throws GraphException {
        Graph graph = new Graph("graph");

        Node good1Node = new Node("Good1", goodOpSpi.getName());
        Node good2Node = new Node("Good2", goodOpSpi.getName());
        Node consumerNode = new Node("MultiConsumer", multiProductsOptionalConsumerOpSpi.getName());
        graph.addNode(good1Node);
        graph.addNode(good2Node);
        graph.addNode(consumerNode);

        GraphContext graphContext = new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        Product[] sourceProducts = graphContext.getNodeContext(consumerNode).getSourceProducts();
        assertNotNull(sourceProducts);
        assertEquals(0, sourceProducts.length);
    }

    public static class GoodOperator extends Operator {

        @Override
        public Product initialize() throws OperatorException {
            Product product = new Product("Good", "GoodType", 1, 1);
            product.addBand("a", ProductData.TYPE_INT8);
            product.addBand("b", ProductData.TYPE_INT8);
            return product;
        }

        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(GoodOperator.class, "GoodOperator");
            }

        }
    }

    public static class MultipleProductsOptionalConsumerOperator extends Operator {

        @SourceProducts(optional = true)
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public Product initialize() throws OperatorException {
            return new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(MultipleProductsOptionalConsumerOperator.class, "MultipleProductsOptionalConsumerOperator");
            }

        }
    }

    public static class MultipleProductsConsumerOperator extends Operator {

        @SourceProducts
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public Product initialize() throws OperatorException {
            return new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {

            public Spi() {
                super(MultipleProductsConsumerOperator.class, "MultipleProductsConsumerOperator");
            }

        }
    }
}
