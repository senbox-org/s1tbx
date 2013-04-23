/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.support;

import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.VariableContext;

/**
 * The binning context.
 *
 * @author Norman Fomferra
 */
public class BinningContextImpl implements BinningContext {

    private final PlanetaryGrid planetaryGrid;
    private final BinManager binManager;
    private final int superSampling;
    private CompositingType compositingType;

    public BinningContextImpl(PlanetaryGrid planetaryGrid, BinManager binManager, CompositingType compositingType,
                              int superSampling) {
        this.planetaryGrid = planetaryGrid;
        this.binManager = binManager;
        this.compositingType = compositingType;
        this.superSampling = superSampling;
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

}
