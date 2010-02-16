package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

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
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(notInitTargetProductOpSPI);

    }

    @Override
    protected void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(notInitTargetProductOpSPI);
    }

    public void testTargetProductIsSetByAnnotation() throws GraphException {
        Graph graph = new Graph("graph");

        Node node = new Node("OutputNotSet", notInitTargetProductOpSPI.getOperatorAlias());
        graph.addNode(node);

        GraphContext graphContext = new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        NodeContext nodeContext = graphContext.getNodeContext(node);
        NotInitOutputOperator notInitOutputOperator = (NotInitOutputOperator) nodeContext.getOperator();
        assertNotNull("Output of operator is null", notInitOutputOperator.output);
        assertSame(nodeContext.getTargetProduct(), notInitOutputOperator.output);
    }


    public static class NotInitOutputOperator extends Operator {

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
                super(NotInitOutputOperator.class, "NotInitOutputOperator");
            }
        }
    }


}
