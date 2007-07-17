package org.esa.beam.framework.gpf.graph;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;

import com.bc.ceres.core.ProgressMonitor;

public class GraphCallSequenceTest extends TestCase {

    private static List<String> callRecordList;
    private N1Spi n1Spi;
    private N2Spi n2Spi;
    private N3Spi n3Spi;
    private N4Spi n4Spi;
    private N5Spi n5Spi;
    private N6Spi n6Spi;
	private long minimumTileSize;

    @Override
    protected void setUp() throws Exception {
    	minimumTileSize = GPF.getDefaultInstance().getTileCache().getMinimumTileSize();
    	GPF.getDefaultInstance().getTileCache().setMinimumTileSize(1);
        n1Spi = new N1Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n1Spi);
        n2Spi = new N2Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n2Spi);
        n3Spi = new N3Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n3Spi);
        n4Spi = new N4Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n4Spi);
        n5Spi = new N5Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n5Spi);
        n6Spi = new N6Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(n6Spi);

        callRecordList = new ArrayList<String>();
    }

    @Override
    protected void tearDown() throws Exception {
    	GPF.getDefaultInstance().getTileCache().setMinimumTileSize(minimumTileSize);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n1Spi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n2Spi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n3Spi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n4Spi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n5Spi);
        OperatorSpiRegistry.getInstance().removeOperatorSpi(n6Spi);
        callRecordList.clear();
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /
    //                N2
    //
    //
    public void testTwoNodeTraversion() throws GraphException {

        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");

        node2.addSource(new NodeSource("input", "N1"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        Product[] targetProducts = graphContext.getOutputProducts();

        assertNotNull(targetProducts);
        assertEquals(1, targetProducts.length);
        assertNotNull(targetProducts[0]);
        assertEquals("N2", targetProducts[0].getName());

        processor.disposeGraphContext(graphContext);

        String[] expectedRecordStrings = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };

        assertEquals(expectedRecordStrings.length, callRecordList.size());

        for (int i = 0; i < expectedRecordStrings.length; i++) {
            assertEquals(expectedRecordStrings[i], callRecordList.get(i).toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /
    //                N2
    //               /
    //              N3
    //
    //
    public void testThreeNodeTraversion() throws GraphException {
        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N2"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        Product[] targetProducts = graphContext.getOutputProducts();

        assertNotNull(targetProducts);
        assertEquals(1, targetProducts.length);
        assertNotNull(targetProducts[0]);
        assertEquals("N3", targetProducts[0].getName());

        processor.disposeGraphContext(graphContext);

        String[] expectedRecordStrings = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N3:Operator.computeBand",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N3:Operator.dispose",
                "N3:Product.dispose",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };

        assertEquals(expectedRecordStrings.length, callRecordList.size());

        for (int i = 0; i < expectedRecordStrings.length; i++) {
            assertEquals(expectedRecordStrings[i], callRecordList.get(i).toString());
        }
    }


    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /  \
    //                 N2   N3
    //               /  \
    //              N4   N5
    //
    //
    public void testSingleSources3Ouputs() throws GraphException {
        GPF.getDefaultInstance().getTileCache().clean();

        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");
        Node node4 = new Node("N4", "N4");
        Node node5 = new Node("N5", "N5");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N1"));
        node4.addSource(new NodeSource("input", "N2"));
        node5.addSource(new NodeSource("input", "N2"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node5);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        Product[] outputProducts = graphContext.getOutputProducts();

        assertNotNull(outputProducts);
        assertEquals(3, outputProducts.length);
        assertNotNull(outputProducts[0]);
        assertNotNull(outputProducts[1]);
        assertNotNull(outputProducts[2]);
        // Nodes shall be processed in the order they are defined!
        assertEquals("N3", outputProducts[0].getName());
        assertEquals("N4", outputProducts[1].getName());
        assertEquals("N5", outputProducts[2].getName());

        processor.disposeGraphContext(graphContext);

        String[] expectedRecords = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N4:Operator.initialize",
                "N4:Product.construct",
                "N5:Operator.initialize",
                "N5:Product.construct",
                "N3:Operator.computeBand",
                "N1:Operator.computeBand",
                "N4:Operator.computeBand",
                "N2:Operator.computeBand",
                //"N1:Operator.computeBand",  is cached!
                "N5:Operator.computeBand",
                //"N2:Operator.computeBand",  is cached!
                //"N1:Operator.computeBand",  is cached!
                "N5:Operator.dispose",
                "N5:Product.dispose",
                "N4:Operator.dispose",
                "N4:Product.dispose",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N3:Operator.dispose",
                "N3:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };
        assertEquals(expectedRecords.length, callRecordList.size());

        for (int i = 0; i < expectedRecords.length; i++) {
            assertEquals(expectedRecords[i], callRecordList.get(i).toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /  \
    //                 N2   N3
    //               /  \  /
    //              N4   N6      <-- N6 has 2 sources!
    //
    //
    public void test2Sources1Ouput() throws GraphException {
        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");
        Node node4 = new Node("N4", "N4");
        Node node6 = new Node("N6", "N6");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N1"));
        node4.addSource(new NodeSource("input", "N2"));
        node6.addSource(new NodeSource("input1", "N2"));
        node6.addSource(new NodeSource("input2", "N3"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node6);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        Product[] outputProducts = graphContext.getOutputProducts();

        assertNotNull(outputProducts);
        assertEquals(2, outputProducts.length);
        assertNotNull(outputProducts[0]);
        assertNotNull(outputProducts[1]);
        // Nodes shall be processed in the order they are defined!
        assertEquals("N4", outputProducts[0].getName());
        assertEquals("N6", outputProducts[1].getName());

        processor.disposeGraphContext(graphContext);

        String[] expectedRecords = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N4:Operator.initialize",
                "N4:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N6:Operator.initialize",
                "N6:Product.construct",
                "N4:Operator.computeBand",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N6:Operator.computeBand",
                // "N2:Operator.computeBand",  is cached!
                // "N1:Operator.computeBand",  is cached!
                "N3:Operator.computeBand",
                // "N1:Operator.computeBand",  is cached!
                "N6:Operator.dispose",
                "N6:Product.dispose",
                "N3:Operator.dispose",
                "N3:Product.dispose",
                "N4:Operator.dispose",
                "N4:Product.dispose",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };
        assertEquals(expectedRecords.length, callRecordList.size());

        for (int i = 0; i < expectedRecords.length; i++) {
            assertEquals(expectedRecords[i], callRecordList.get(i).toString());
        }
    }

    public static class RecordingOp extends AbstractOperator {

        @TargetProduct
        private Product targetProduct;

        public RecordingOp(OperatorSpi spi) {
            super(spi);
        }

        @Override
        public Product initialize(ProgressMonitor pm) throws OperatorException {
            recordCall(getSpi().getName(), "Operator.initialize");
            return new RecordingProduct((NodeContext) getContext());
        }

        @Override
        public void dispose() {
            recordCall(getSpi().getName(), "Operator.dispose");
        }
    }

    public static class NoSourceOp extends RecordingOp {

        @TargetProduct
        private Product targetProduct;

        public NoSourceOp(OperatorSpi spi) {
            super(spi);
        }

        // todo - add tests that verify correct computing output
        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) throws
                OperatorException {
            recordCall(getSpi().getName(), "Operator.computeBand");

            Rectangle r = targetRaster.getRectangle();
            float offset = r.y * targetProduct.getSceneRasterWidth() + r.x;

            float[] targetElems = (float[]) targetRaster.getDataBuffer().getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = offset + i;
            }

            targetRaster.getDataBuffer().setElems(targetElems);

        }

        @Override
        public void dispose() {
            recordCall(getSpi().getName(), "Operator.dispose");
        }
    }

    public static class SingleSourceOp extends NoSourceOp {

        @SourceProduct(alias = "input")
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;

        public SingleSourceOp(OperatorSpi spi) {
            super(spi);
        }

        // todo - add tests that verify correct computing output
        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) throws
                OperatorException {
            recordCall(getSpi().getName(), "Operator.computeBand");

            Raster sourceRaster = getRaster(sourceProduct.getBandAt(0),
            		targetRaster.getRectangle());

            float[] sourceElems = (float[]) sourceRaster.getDataBuffer().getElems();
            float[] targetElems = (float[]) targetRaster.getDataBuffer().getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = 0.1f * sourceElems[i];
            }

            targetRaster.getDataBuffer().setElems(targetElems);
        }
    }

    public static class DualSourceOp extends NoSourceOp {

        @SourceProduct(alias = "input1")
        private Product sourceProduct1;
        @SourceProduct(alias = "input2")
        private Product sourceProduct2;
        @TargetProduct
        private Product targetProduct;

        public DualSourceOp(OperatorSpi spi) {
            super(spi);
        }

        // todo - add tests that verify correct computing output
        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
            recordCall(getSpi().getName(), "Operator.computeBand");

            Raster sourceRaster1 = getRaster(sourceProduct1.getBandAt(0),
            		targetRaster.getRectangle());

            Raster sourceRaster2 = getRaster(sourceProduct2.getBandAt(0),
            		targetRaster.getRectangle());

            float[] source1Elems = (float[]) sourceRaster1.getDataBuffer().getElems();
            float[] source2Elems = (float[]) sourceRaster2.getDataBuffer().getElems();
            float[] targetElems = (float[]) targetRaster.getDataBuffer().getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = 0.1f * (source1Elems[i] + source2Elems[i]);
            }

            targetRaster.getDataBuffer().setElems(targetElems);
        }
    }

    public static abstract class NSpi extends AbstractOperatorSpi {

        protected NSpi(Class<? extends Operator> operatorClass, String name) {
            super(operatorClass, name);
        }
    }

    public static class N1Spi extends NSpi {

        public N1Spi() {
            super(NoSourceOp.class, "N1");
        }
    }

    public static class N2Spi extends NSpi {

        public N2Spi() {
            super(SingleSourceOp.class, "N2");
        }
    }

    public static class N3Spi extends NSpi {

        public N3Spi() {
            super(SingleSourceOp.class, "N3");
        }
    }

    public static class N4Spi extends NSpi {

        public N4Spi() {
            super(SingleSourceOp.class, "N4");
        }

    }

    public static class N5Spi extends NSpi {

        public N5Spi() {
            super(SingleSourceOp.class, "N5");
        }
    }

    public static class N6Spi extends NSpi {

        public N6Spi() {
            super(DualSourceOp.class, "N6");
        }
    }

    public static class RecordingProduct extends Product {

        public RecordingProduct(NodeContext context) {
            super(context.getNode().getId(), context.getNode().getOperatorName(), 1, 1);
            addBand("band_0", ProductData.TYPE_FLOAT32);
            setProductReader(new OperatorProductReader(context));
            recordCall(getName(), "Product.construct");
        }

        @Override
        public void dispose() {
            recordCall(getName(), "Product.dispose");
            super.dispose();
        }
    }

    private  static void recordCall(String name, String name1) {
        callRecordList.add(name + ":" + name1);
    }
}
