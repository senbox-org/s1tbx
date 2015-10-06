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
