package org.esa.beam.framework.gpf.graph;

import java.awt.Rectangle;

/**
 * This interface can be implemented and added to the {@link GraphProcessor} to get informed
 * about processing steps of a graph.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @see GraphProcessor#addObserver(GraphProcessingObserver)
 * @since 4.1
 */
public interface GraphProcessingObserver {

    /**
     * It is invoked when the graph starts its processing.
     *
     * @param graphContext the graph context being processed
     */
    void graphProcessingStarted(GraphContext graphContext);

    /**
     * It is invoked when the graph stops its processing.
     *
     * @param graphContext the graph context being processed
     */
    void graphProcessingStopped(GraphContext graphContext);

    /**
     * It is invoked when the processing of the rectangle starts.
     *
     * @param graphContext  the graph context being processed
     * @param tileRectangle the rectangle currently processed
     */
    void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle);

    /**
     * It is invoked when the processing of the rectangle is done.
     *
     * @param graphContext  the graph context being processed
     * @param tileRectangle the rectangle currently processed
     */
    void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle);
}
