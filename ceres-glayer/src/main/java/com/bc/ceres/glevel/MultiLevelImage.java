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

package com.bc.ceres.glevel;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Vector;

/**
 * A {@link javax.media.jai.PlanarImage PlanarImage} which can act as a {@link MultiLevelSource}.
 * The image data provided by this image corresponds to the level zero image of the
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class MultiLevelImage extends PlanarImage implements MultiLevelSource {

    /**
     * Constructs a new {@code MultiLevelImage}.
     * Calls the
     *
     * @param layout     The layout of this image or null.
     * @param sources    The immediate sources of this image or null.
     * @param properties A Map containing the properties of this image or null.
     */
    protected MultiLevelImage(ImageLayout layout, Vector sources, Map properties) {
        super(layout, sources, properties);
    }

    /////////////////////////////////////////////////////////////////////////
    // PlanarImage interface

    @Override
    public final Raster getTile(int x, int y) {
        return getImage(0).getTile(x, y);
    }

    @Override
    public final Raster getData() {
        return getImage(0).getData();
    }

    @Override
    public final Raster getData(Rectangle rect) {
        return getImage(0).getData(rect);
    }

    @Override
    public final WritableRaster copyData(WritableRaster raster) {
        return getImage(0).copyData(raster);
    }

    /**
     * Provides a hint that an image will no longer be accessed from a
     * reference in user space.  The results are equivalent to those
     * that occur when the program loses its last reference to this
     * image, the garbage collector discovers this, and finalize is
     * called.  This can be used as a hint in situations where waiting
     * for garbage collection would be overly conservative.
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.
     * <p>Overrides shall call {@code super.dispose()} in a final step.
     */
    @Override
    public void dispose() {
        super.dispose();
    }
}