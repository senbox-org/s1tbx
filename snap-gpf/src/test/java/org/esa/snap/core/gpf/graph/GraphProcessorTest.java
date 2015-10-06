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
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.TestOps;
import org.esa.snap.core.util.jai.VerbousTileCache;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;

public class GraphProcessorTest extends TestCase {
    private OperatorSpi spi1;
    private OperatorSpi spi2;
    private OperatorSpi spi3;
    private TileCache jaiTileCache;
    private TileCache testTileCache;

    @Override
    protected void setUp() throws Exception {
        jaiTileCache = JAI.getDefaultInstance().getTileCache();
        testTileCache = new VerbousTileCache(jaiTileCache);
        JAI.getDefaultInstance().setTileCache(testTileCache);
        testTileCache.flush();

        TestOps.clearCalls();
        spi1 = new TestOps.Op1.Spi();
        spi2 = new TestOps.Op2.Spi();
        spi3 = new TestOps.Op3.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(spi1);
        registry.addOperatorSpi(spi2);
        registry.addOperatorSpi(spi3);
    }

    @Override
    protected void tearDown() throws Exception {
        testTileCache.flush();
        JAI.getDefaultInstance().setTileCache(jaiTileCache);
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(spi1);
        spiRegistry.removeOperatorSpi(spi2);
        spiRegistry.removeOperatorSpi(spi3);
    }


    @SuppressWarnings("null")
    public void testTwoOpsExecutionOrder() throws Exception {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");

        node2.addSource(new NodeSource("input", "node1"));

        graph.addNode(node1);
        graph.addNode(node2);

        GraphContext graphContext = new GraphContext(graph);
        Product chainOut = processor.executeGraph(graphContext, ProgressMonitor.NULL)[0];

        assertNotNull(chainOut);
        assertEquals("Op2Name", chainOut.getName());

        assertEquals("Op1;Op2;", TestOps.getCalls());
        TestOps.clearCalls();
    }

    ////////////////////////////////////////////////////////////////////////
    //            node1
    //            /  \
    //        node2   \
    //            \   /
    //            node3    <-- Target!
    //
    public void testThreeOpsExecutionOrder() throws Exception {

        VerbousTileCache.setVerbous(false);

        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product chainOut = graphContext.getOutputProducts()[0];

        assertNotNull(chainOut);
        assertEquals("Op3Name", chainOut.getName());

        processor.executeGraph(graphContext, ProgressMonitor.NULL);
        // - Op3 requires the two bands of Op2
        // - Op2 computes all bands
        // --> Op2 should only be called once
        assertEquals("Op1;Op2;Op3;", TestOps.getCalls());
        TestOps.clearCalls();

        VerbousTileCache.setVerbous(false);
    }

}
