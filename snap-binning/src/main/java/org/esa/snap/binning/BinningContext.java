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

import com.vividsolutions.jts.geom.Geometry;

/**
 * The binning context.
 *
 * @author Norman Fomferra
 */
public interface BinningContext {

    /**
     * @return The variable context. Defines numbering of variables involved in the binning.
     */
    VariableContext getVariableContext();

    /**
     * @return The definition of the binning grid.
     */
    PlanetaryGrid getPlanetaryGrid();

    /**
     * @return The bin manager which is used to perform compute bin operations.
     */
    BinManager getBinManager();

    /**
     * @return The compositing type which is used to during the binning.
     */
    CompositingType getCompositingType();

    /**
     * @return The super-sampling of source pixels. May be {@code null}, if not used.
     */
    Integer getSuperSampling();

    /**
     * @return The maximum distance on earth in meter for a sub-pixel coordinate compared to the center of the macro-pixel. A value <=0 disables this check.
     */
    Integer getMaxDistanceOnEarth();

    /**
     * @return The definition of a "spatial data-day", or more generally, a spatial data-period used for the binning.
     * May be {@code null}, if not used.
     * @since BEAM 5
     */
    DataPeriod getDataPeriod();

    /**
     * @return The region that should be considered for binning.  May be {@code null}, if not used.
     * @since BEAM 5
     */
    Geometry getRegion();
}
