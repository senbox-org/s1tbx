package org.esa.beam.framework.gpf.graph;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.*;

/**
 * Default implementation for {@link org.esa.beam.framework.gpf.internal.OperatorContext}.
 */
class NodeContext {

    private final GraphContext graphContext;
    private final Node node;
    private Operator operator;
    private int referenceCount;

    public NodeContext(GraphContext graphContext, Node node) throws GraphException {
        this.graphContext = graphContext;
        this.node = node;

        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) {
            throw new GraphException("Operator SPI not found for operator '" + node.getOperatorName() + "'");
        }

        try {
            this.operator = operatorSpi.createOperator();
        } catch (OperatorException e) {
            throw new GraphException("Failed to create inmstance of operator'" + node.getOperatorName() + "'");
        }

        this.operator.setLogger(graphContext.getLogger());
    }

    public GraphContext getGraphContext() {
        return graphContext;
    }

    public Node getNode() {
        return node;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean isOutput() {
        return referenceCount == 0;
    }

    void incrementReferenceCount() {
        this.referenceCount++;
    }

    public Product getTargetProduct() {
        return operator.getTargetProduct();
    }

    public boolean canComputeTileStack() {
        return operator.canComputeTileStack();
    }

    public boolean isInitialized() {
        return operator.isInitialized();
    }

    public void addSourceProduct(String id, Product sourceProduct) {
        operator.addSourceProduct(id, sourceProduct);
    }

    public void setParameters(Xpp3Dom configuration) {
        operator.setParameters(configuration);
    }

    public Product getSourceProduct(String id) {
        return operator.getSourceProduct(id);
    }

    public Product[] getSourceProducts() {
        return operator.getSourceProducts();
    }
}
