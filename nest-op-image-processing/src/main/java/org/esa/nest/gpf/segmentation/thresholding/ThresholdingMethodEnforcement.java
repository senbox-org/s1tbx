/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.segmentation.thresholding;

import com.bc.ceres.core.ProgressMonitor;
import ij.process.ByteProcessor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Tile;

import java.util.Map;

/**
 * ThresholdingMethodEnforcement the interface for all the thresholding methods.
 *
 * @author Emanuela Boros
 * @since October 2012
 */
public interface ThresholdingMethodEnforcement {

    /**
     *
     * Compute a Thresholding Method
     *
     * @param sourceBand The source band.
     * @param sourceRaster The source tile for the band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param x0 X coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     * @param pm A progress monitor which should be used to determine
     * computation cancellation requests.
     * @param method The thresholding method that will be applied.
     * @param paramMap The parameters list for every thresholding method.
     */
    public abstract ByteProcessor computeThresholdingOperator(final Band sourceBand,
            final Tile sourceRaster, final Tile targetTile,
            final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm, Map<String, Object> paramMap);

    /**
     * Get the thresholded method
     *
     * @param imageProcessor The processor for the image to be thresholded.
     * @return The thresholded image.
     */
    public abstract ByteProcessor getThresholdedImage(ByteProcessor imageProcessor);
}
