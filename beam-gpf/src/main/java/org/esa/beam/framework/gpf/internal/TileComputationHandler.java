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
package org.esa.beam.framework.gpf.internal;

import java.util.logging.Logger;

/**
 * Gets notified once a new tile has been computed.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class TileComputationHandler {
    private Logger logger;

    /**
     * @return A logger.
     */
    public final Logger getLogger() {
        return logger;
    }

    final void setLogger(Logger logger) {
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
