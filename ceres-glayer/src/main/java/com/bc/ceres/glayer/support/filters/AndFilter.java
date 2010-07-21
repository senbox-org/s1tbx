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

package com.bc.ceres.glayer.support.filters;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.core.Assert;

public class AndFilter implements LayerFilter {
    private final LayerFilter arg1;
    private final LayerFilter arg2;

    public AndFilter(LayerFilter arg1, LayerFilter arg2) {
        Assert.notNull(arg1, "arg1");
        Assert.notNull(arg2, "arg2");
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public LayerFilter getArg1() {
        return arg1;
    }

    public LayerFilter getArg2() {
        return arg2;
    }

    public boolean accept(Layer layer) {
        return arg1.accept(layer) && arg2.accept(layer);
    }
}