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
package org.esa.nest.gpf.features.local.matching;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Tile;

/**
 * TemplateMatcherEnforcement the interface for all the matching methods.
 *
 * @author Emanuela Boros
 * @since October 2012
 */
public interface TemplateMatcherEnforcement {

    /**
     * Compute the matching score between an image and a template tile
     *
     * @param sourceBand The source band.
     * @param templateTile The source template tile for the band.
     * @param x coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param y Y coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     *
     * @return the matching score between the image and the template
     */
    public float computeMatchScore(
            final Band sourceBand, final Tile templateTile,
            final int x, final int y, SARTemplateMatcher.Mode mode);
}
