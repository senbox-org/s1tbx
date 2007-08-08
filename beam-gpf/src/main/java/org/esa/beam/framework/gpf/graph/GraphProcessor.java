package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ParameterConverter;
import org.esa.beam.framework.gpf.internal.DefaultParameterConverter;
import org.esa.beam.framework.gpf.internal.OperatorContextInitializer;
import org.esa.beam.framework.gpf.internal.ParameterInjector;
import org.esa.beam.framework.gpf.internal.TileComputingStrategy;
import org.esa.beam.framework.gpf.support.RectangleIterator;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;

/**
 * The <code>GraphProcessor</code> is responsible for executing processing
 * graphs.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 4.1
 */
public class GraphProcessor {

    private List<GraphProcessingObserver> observerList;
    private Logger logger;


    /**
     * Creates a new instance og {@code GraphProcessor}.
     */
    public GraphProcessor() {
        observerList = new ArrayList<GraphProcessingObserver>();
        logger = Logger.getAnonymousLogger();
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets a logger.
     *
     * @param logger a logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds an observer to this graph propcessor. {@link GraphProcessingObserver}s are informed about
     * processing steps of the currently running processing graph.
     *
     * @param processingObserver the observer
     *
     * @see GraphProcessingObserver
     */
    public void addObserver(GraphProcessingObserver processingObserver) {
        observerList.add(processingObserver);
    }

    /**
     * Gets all observers currentyl attached to this {@code GraphProcessor}.
     *
     * @return the observers
     */
    public GraphProcessingObserver[] getObservers() {
        return observerList.toArray(new GraphProcessingObserver[observerList.size()]);
    }

    /**
     * Executes the given {@link Graph}.
     *
     * @param graph the {@link Graph}
     * @param pm    a progress monitor. Can be used to signal progress.
     *
     * @throws GraphException if any error occrues during execution
     */
    public void executeGraph(Graph graph, ProgressMonitor pm) throws GraphException {
        GraphContext graphContext;
        try {
            pm.beginTask("Executing processing graph", 100);
            graphContext = createGraphContext(graph, new SubProgressMonitor(pm, 10));
            executeGraphContext(graphContext, new SubProgressMonitor(pm, 90));
            disposeGraphContext(graphContext);
        } finally {
            pm.done();
        }
    }

    /**
     * Creates an {@link GraphContext} for the given {@link Graph}.
     *
     * @param graph the {@link Graph} to create the {@link GraphContext} for
     * @param pm    a progress monitor. Can be used to signal progress.
     *
     * @return the created {@link GraphContext}
     *
     * @throws GraphException if any error occrues during creation of the context, e.g. the graph is empty
     */
    public GraphContext createGraphContext(Graph graph, ProgressMonitor pm) throws GraphException {

        if (graph.getNodeCount() == 0) {
            throw new GraphException("Empty graph.");
        }

        try {
            pm.beginTask("Creating processing graph context", 100);
            GraphContext graphContext = new GraphContext(graph, logger);
            initNodeDependencies(graphContext);
            pm.worked(10);
            initOutput(graphContext, new SubProgressMonitor(pm, 90));
            return graphContext;
        } finally {
            pm.done();
        }
    }

    /**
     * Disposes the given {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to dispose
     */
    public void disposeGraphContext(GraphContext graphContext) {
        Deque<NodeContext> initNodeContextDeque = graphContext.getInitNodeContextDeque();
        while (!initNodeContextDeque.isEmpty()) {
            NodeContext nodeContext = initNodeContextDeque.pop();
            nodeContext.getOperator().dispose();
            nodeContext.getTargetProduct().dispose();
        }
    }

    /**
     * Executes the given {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to execute
     * @param pm           a progress monitor. Can be used to signal progress.
     *
     * @throws GraphException if any error occrues during execution
     */
    public void executeGraphContext(GraphContext graphContext, ProgressMonitor pm) throws GraphException {
        fireProcessingStarted(graphContext);

        NodeContext[] outputNodeContexts = graphContext.getOutputNodeContexts();
        // todo - further investigate this suspicious code
        Product targetProduct = outputNodeContexts[0].getTargetProduct();
        if (targetProduct == null) {
            throw new IllegalStateException("outputProduct == null");
        }
        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        RectangleIterator rectangleIterator = new RectangleIterator(graphContext.getPreferredTileSize(),
                                                                    rasterWidth, rasterHeight);

        pm.beginTask("Computing raster data...", rectangleIterator.getNumRectangles());
        try {
            while (rectangleIterator.hasNext()) {
                if (pm.isCanceled()) {
                    break;
                }
                Rectangle tileRectangle = rectangleIterator.next();
                fireTileStarted(graphContext, tileRectangle);
                for (NodeContext nodeContext : outputNodeContexts) {
                    TileComputingStrategy.computeAllBands(nodeContext, tileRectangle, pm);
                }
                fireTileStopped(graphContext, tileRectangle);
                pm.worked(1);
            }
        } catch (OperatorException e) {
            throw new GraphException(e.getMessage(), e);
        } finally {
            pm.done();
            fireProcessingStopped(graphContext);
        }
    }

    private void initNodeDependencies(GraphContext graphContext) throws GraphException {
        Graph graph = graphContext.getGraph();
        for (Node node : graph.getNodes()) {
            for (NodeSource source : node.getSources()) {
                Node sourceNode = graph.getNode(source.getSourceNodeId());
                if (sourceNode == null) {
                    throw new GraphException("Missing source. Node Id: " + node.getId()
                                             + " Source Id: " + source.getSourceNodeId());
                }
                graphContext.getNodeContext(sourceNode).incrementReferenceCount();
                source.setSourceNode(sourceNode);  // todo - use getNodeContext()
            }
        }
    }

    private void initOutput(GraphContext graphContext, ProgressMonitor pm) throws GraphException {
        final int outputCount = graphContext.getOutputCount();
        try {
            pm.beginTask("Creating output products", outputCount);
            for (Node node : graphContext.getGraph().getNodes()) {
                NodeContext nodeContext = graphContext.getNodeContext(node);
                if (nodeContext.isOutput()) {
                    initNodeContext(graphContext, nodeContext, new SubProgressMonitor(pm, 1));
                    graphContext.addOutputNodeContext(nodeContext);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void initNodeContext(GraphContext graphContext, final NodeContext nodeContext, ProgressMonitor pm) throws
                                                                                                               GraphException {
        try {
            NodeSource[] sources = nodeContext.getNode().getSources();
            pm.beginTask("Creating operator", sources.length + 4);

            if (nodeContext.isInitialized()) {
                return;
            }

            for (NodeSource source : sources) {
                NodeContext sourceNodeContext = graphContext.getNodeContext(source.getSourceNode());
                initNodeContext(graphContext, sourceNodeContext, new SubProgressMonitor(pm, 1));
                nodeContext.addSourceProduct(source.getName(), sourceNodeContext.getTargetProduct());
            }

            OperatorContextInitializer.initOperatorContext(nodeContext, new Xpp3DomParameterInjector(nodeContext), pm);

            // register nodeContext for correct disposal
            graphContext.getInitNodeContextDeque().addFirst(nodeContext);
        } catch (OperatorException e) {
            throw new GraphException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }


    private void fireProcessingStarted(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStarted(graphContext);
        }
    }

    private void fireProcessingStopped(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStopped(graphContext);
        }
    }

    private void fireTileStarted(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStarted(graphContext, rect);
        }

    }

    private void fireTileStopped(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStopped(graphContext, rect);
        }
    }


    private static class Xpp3DomParameterInjector implements ParameterInjector {

        private final NodeContext nodeContext;

        public Xpp3DomParameterInjector(NodeContext nodeContext) {
            this.nodeContext = nodeContext;
        }

        public void injectParameters(Operator operator) throws OperatorException {
            Xpp3Dom configuration = nodeContext.getNode().getConfiguration();
            if (configuration != null) {
                if (operator instanceof ParameterConverter) {
                    ParameterConverter converter = (ParameterConverter) operator;
                    try {
                        converter.setParameterValues(operator, configuration);
                    } catch (Throwable t) {
                        throw new OperatorException(t);
                    }
                } else {
                    DefaultParameterConverter defaultParameterConverter = new DefaultParameterConverter();
                    defaultParameterConverter.setParameterValues(operator, configuration);
                }
            }
        }
    }
}
