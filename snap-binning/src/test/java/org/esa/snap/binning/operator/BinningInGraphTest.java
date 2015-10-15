package org.esa.snap.binning.operator;

import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * @author Norman Fomferra
 */
public class BinningInGraphTest {

    @Test
    public void testIt() throws Exception {
        InputStream stream = BinningInGraphTest.class.getResourceAsStream("BinningInGraphTest.xml");
        Graph graph = GraphIO.read(new InputStreamReader(stream));

        Assert.assertNotNull(graph);
        Assert.assertEquals(1, graph.getNodeCount());
        Assert.assertEquals("subsetNodeId", graph.getNode(0).getId());
        Assert.assertEquals("subsetNodeId", graph.getNode(0).getId());
    }
}
