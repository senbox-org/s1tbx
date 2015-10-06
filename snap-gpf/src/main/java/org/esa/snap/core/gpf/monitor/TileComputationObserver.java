/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.gpf.monitor;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.common.WriteOp;

import java.util.logging.Logger;

/**
 * Gets notified once a new tile has been computed.
 * <p>
 * The framework uses observers as follows:
 * <ol>
 *     <li>{@link #start()} is called only once before any other method is called.</li>
 *     <li>{@link #tileComputed(TileComputationEvent)} is called for each tile computed by any GPF {@link Operator Operator}.</li>
 *     <li>{@link #stop()} is called after a {@link Product Product} has been
 *     fully written using the {@link WriteOp WriteOp} operator.</li>
 * </ol>
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class TileComputationObserver {
    private Logger logger;

    /**
     * @return A logger.
     */
    public final Logger getLogger() {
        return logger;
    }

    public final void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Starts observation of tile computation events.
     */
    public abstract void start();

    /**
     * Called each time a tile has been computed. This method is usually called asynchronously by multiple threads.
     * It should perform very fast.
     *
     * @param event The  tile computation event.
     */
    public abstract void tileComputed(TileComputationEvent event);

    /**
     * Stops observation of tile computation events.
     */
    public abstract void stop();
}
