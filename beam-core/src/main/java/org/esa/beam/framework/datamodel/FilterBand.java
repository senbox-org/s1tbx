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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.Guardian;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Represents a band that generates its data by using another band as input and performs some kind of operation on this input.
 * <p/>
 * <p><i>Note that this class is not yet public API and may change in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class FilterBand extends Band {

    private RasterDataNode _source;

    protected FilterBand(String name, int dataType, int width, int height, RasterDataNode source) {
        super(name, dataType, width, height);
        Guardian.assertNotNull("source", source);
        _source = source;
        setSynthetic(true);
    }

    public RasterDataNode getSource() {
        return _source;
    }

    @Override
    public void dispose() {
        _source = null;
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for filtered band");
    }

}
