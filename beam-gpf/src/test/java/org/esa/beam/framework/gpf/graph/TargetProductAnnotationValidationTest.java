package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class TargetProductAnnotationValidationTest extends TestCase {


    private OperatorSpi notInitTargetProductOpSPI;

    @Override
    protected void setUp() throws Exception {
        notInitTargetProductOpSPI = new NotInitOutputOperator.Spi();
        OperatorSpiRegistry.getInstance().addOperatorSpi(notInitTargetProductOpSPI);

    }

    @Override
    protected void tearDown() {
        OperatorSpiRegistry.getInstance().removeOperatorSpi(notInitTargetProductOpSPI);
    }

    public void testTargetProductIsSetByAnnotation() throws GraphException {
        Graph graph = new Graph("graph");

        Node node = new Node("OutputNotSet", notInitTargetProductOpSPI.getName());
        graph.addNode(node);

        GraphContext graphContext = new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        NodeContext nodeContext = graphContext.getNodeContext(node);
        NotInitOutputOperator notInitOutputOperator = (NotInitOutputOperator) nodeContext.getOperator();
        assertNotNull("Output of operator is null", notInitOutputOperator.output);
        assertSame(nodeContext.getTargetProduct(), notInitOutputOperator.output);
    }


    public static class NotInitOutputOperator extends AbstractOperator {

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
                super(NotInitOutputOperator.class, "NotInitOutputOperator");
            }
        }
    }


}
