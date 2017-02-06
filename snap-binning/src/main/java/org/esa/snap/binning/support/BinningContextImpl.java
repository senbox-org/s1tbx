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

package org.esa.snap.binning.support;

import com.bc.ceres.core.Assert;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.DataPeriod;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.VariableContext;

/**
 * The binning context.
 *
 * @author Norman Fomferra
 */
public class BinningContextImpl implements BinningContext {

    private final PlanetaryGrid planetaryGrid;
    private final BinManager binManager;
    private final int superSampling;
    private final int maxDistanceOnEarth;
    private final CompositingType compositingType;
    private final DataPeriod dataPeriod;
    private final Geometry region;

    @Deprecated
    public BinningContextImpl(PlanetaryGrid planetaryGrid, BinManager binManager, CompositingType compositingType,
                              int superSampling) {
        this(planetaryGrid, binManager, compositingType, superSampling, -1, null, null);
    }

    @Deprecated
    public BinningContextImpl(PlanetaryGrid planetaryGrid, BinManager binManager, CompositingType compositingType,
                              int superSampling, DataPeriod dataPeriod) {
        this(planetaryGrid, binManager, compositingType, superSampling, -1, dataPeriod, null);
    }

    @Deprecated
    public BinningContextImpl(PlanetaryGrid planetaryGrid, BinManager binManager, CompositingType compositingType,
                              int superSampling, DataPeriod dataPeriod, Geometry region) {
        this(planetaryGrid, binManager, compositingType, superSampling, -1, dataPeriod, region);
    }

    public BinningContextImpl(PlanetaryGrid planetaryGrid, BinManager binManager, CompositingType compositingType,
                              int superSampling, int maxDistanceOnEarth, DataPeriod dataPeriod, Geometry region) {
        Assert.notNull(planetaryGrid, "planetaryGrid");
        Assert.notNull(binManager, "binManager");
        Assert.notNull(compositingType, "compositingType");
        this.planetaryGrid = planetaryGrid;
        this.binManager = binManager;
        this.compositingType = compositingType;
        this.superSampling = superSampling;
        this.maxDistanceOnEarth = maxDistanceOnEarth;
        this.dataPeriod = dataPeriod;
        this.region = region;
    }

    @Override
    public VariableContext getVariableContext() {
        return getBinManager().getVariableContext();
    }

    @Override
    public PlanetaryGrid getPlanetaryGrid() {
        return planetaryGrid;
    }

    @Override
    public BinManager getBinManager() {
        return binManager;
    }

    @Override
    public CompositingType getCompositingType() {
        return compositingType;
    }

    @Override
    public Integer getSuperSampling() {
        return superSampling;
    }

    @Override
    public Integer getMaxDistanceOnEarth() {
        return maxDistanceOnEarth;
    }

    @Override
    public DataPeriod getDataPeriod() {
        return dataPeriod;
    }

    @Override
    public Geometry getRegion() {
        return region;
    }
}
