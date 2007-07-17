package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;

import java.awt.*;
import java.util.ArrayList;

public class GraphProcessingObserverTest extends TestCase {
    private static OpMock.Spi opMockSpi = new OpMock.Spi();

    @Override
    protected void setUp() throws Exception {
        OperatorSpiRegistry.getInstance().addOperatorSpi(opMockSpi);
    }

    @Override
    protected void tearDown() throws Exception {
        OperatorSpiRegistry.getInstance().removeOperatorSpi(opMockSpi);
    }

    public void testAddingOberserverToChain() throws GraphException {

        GraphProcessor processor = new GraphProcessor();


        GraphProcessingObserver[] observers = processor.getObservers();
        assertNotNull(observers);
        assertEquals(0, observers.length);

        GraphProcessingObserverMock observerMock = new GraphProcessingObserverMock();
        processor.addObserver(observerMock);

        observers = processor.getObservers();
        assertNotNull(observers);
        assertEquals(1, observers.length);
        assertSame(observerMock, observers[0]);

        Graph graph = new Graph("test-graph");
        graph.addNode(new Node("a", OpMock.Spi.class.getName()));

        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        graphContext.setPreferredTileSize(new Dimension(10, 5));
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);

        assertEquals(6, observerMock.entries.size());
        assertEquals("graph [test-graph] started", observerMock.entries.get(0));
        assertEquals("tile java.awt.Rectangle[x=0,y=0,width=10,height=5] started", observerMock.entries.get(1));
        assertEquals("tile java.awt.Rectangle[x=0,y=0,width=10,height=5] stopped", observerMock.entries.get(2));
        assertEquals("tile java.awt.Rectangle[x=0,y=5,width=10,height=5] started", observerMock.entries.get(3));
        assertEquals("tile java.awt.Rectangle[x=0,y=5,width=10,height=5] stopped", observerMock.entries.get(4));
        assertEquals("graph [test-graph] stopped", observerMock.entries.get(5));
    }

    public static class OpMock extends AbstractOperator {

        public OpMock(OperatorSpi spi) {
            super(spi);
        }

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            Product product = new Product(getSpi().getName(), getSpi().getName(), 10, 10);
            product.addBand("band_1", ProductData.TYPE_INT32);
            return product;
        }

        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends AbstractOperatorSpi {
            public Spi() {
                super(OpMock.class, "OpMock");
            }
        }
    }

    static class GraphProcessingObserverMock implements GraphProcessingObserver {
        ArrayList<String> entries = new ArrayList<String>();

        public void graphProcessingStarted(GraphContext graphContext) {
            this.entries.add("graph [" + graphContext.getGraph().getId() + "] started");

        }

        public void graphProcessingStopped(GraphContext graphContext) {
            this.entries.add("graph [" + graphContext.getGraph().getId() + "] stopped");
        }

        public void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle) {
            this.entries.add("tile " + tileRectangle + " started");
        }

        public void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle) {
            this.entries.add("tile " + tileRectangle + " stopped");
        }
    }
}
