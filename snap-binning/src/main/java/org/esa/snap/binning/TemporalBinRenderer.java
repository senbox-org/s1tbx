/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.snap.binning;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * Renders temporal bins to a rectangular output raster. Used by {@link Reprojector}.
 * <p>
 * Bin renderers can render either maximum rasters of the size
 * <pre>
 *     PlanetaryGrid grid = ctx.getPlanetaryGrid();
 *     int rasterWidth = 2 * grid.getNumRows();
 *     int rasterHeight = grid.getNumRows();
 * </pre>
 * or just render a sub-region returned by {@link #getRasterRegion()}.
 *
 * @author Norman Fomferra
 * @see Reprojector
 */
public interface TemporalBinRenderer {

    /**
     * @return The raster sub-region that this renderer will render the bins into.
     */
    Rectangle getRasterRegion();

    /**
     * Called once before the rendering of bins begins.
     *
     * @throws IOException If an I/O error occurred.
     */
    void begin() throws IOException;

    /**
     * Called once after the rendering of bins ends.
     *
     * @throws IOException If an I/O error occurred.
     */
    void end() throws IOException;

    /**
     * Renders a temporal bin and its statistical output features into the raster at pixel position (x,y).
     * Called for each (x,y) where there is data, with increasing X and increasing Y, X varies faster.
     *
     * @param x            The current raster pixel X coordinate
     * @param y            The current raster pixel Y coordinate
     * @param temporalBin  the current temporal bin
     * @param outputVector the current output vector
     * @throws IOException If an I/O error occurred.
     */
    void renderBin(int x, int y, TemporalBin temporalBin, Vector outputVector) throws IOException;

    /**
     * Renders a missing temporal bin.
     * Called for each (x,y) where there is no data, with increasing X and increasing Y, X varies faster.
     *
     * @param x current pixel X coordinate
     * @param y current pixel Y coordinate
     * @throws IOException If an I/O error occurred.
     */
    void renderMissingBin(int x, int y) throws IOException;
}
