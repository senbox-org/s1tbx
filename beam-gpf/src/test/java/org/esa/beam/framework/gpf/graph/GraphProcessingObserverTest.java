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

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;

public class GraphProcessingObserverTest extends TestCase {
    private static OpMock.Spi opMockSpi = new OpMock.Spi();

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(opMockSpi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(opMockSpi);
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
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);

        assertEquals(6, observerMock.entries.size());
        assertEquals("graph [test-graph] started", observerMock.entries.get(0));
        assertEquals("tile java.awt.Rectangle[x=0,y=0,width=10,height=5] started", observerMock.entries.get(1));
        assertEquals("tile java.awt.Rectangle[x=0,y=0,width=10,height=5] stopped", observerMock.entries.get(2));
        assertEquals("tile java.awt.Rectangle[x=0,y=5,width=10,height=5] started", observerMock.entries.get(3));
        assertEquals("tile java.awt.Rectangle[x=0,y=5,width=10,height=5] stopped", observerMock.entries.get(4));
        assertEquals("graph [test-graph] stopped", observerMock.entries.get(5));
    }

    public static class OpMock extends Operator {
        @TargetProduct
        private Product product;
        
        @Override
        public void initialize() throws OperatorException {
            product = new Product(getClass().getSimpleName(), getClass().getSimpleName(), 10, 10);
            product.addBand("band_1", ProductData.TYPE_INT32);
            product.setPreferredTileSize(new Dimension(10, 5));
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {
            public Spi() {
                super(OpMock.class, "OpMock");
            }
        }
    }

    static class GraphProcessingObserverMock implements GraphProcessingObserver {
        ArrayList<String> entries = new ArrayList<String>();

        public void graphProcessingStarted(GraphContext graphContext) {
            ralla("graph [" + graphContext.getGraph().getId() + "] started");

        }

        private void ralla(String m) {
            //System.out.println("m = " + m);
            this.entries.add(m);
        }

        public void graphProcessingStopped(GraphContext graphContext) {
            ralla("graph [" + graphContext.getGraph().getId() + "] stopped");
        }

        public void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle) {
            ralla("tile " + tileRectangle + " started");
        }

        public void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle) {
            ralla("tile " + tileRectangle + " stopped");
        }
    }
}
