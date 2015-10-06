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

package org.esa.snap.core.image;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;

/**
 * Represent a level in a multi-resolution image pyramid.
 *
 * @author Norman Fomferra
 */
public class ResolutionLevel {
    public final static ResolutionLevel MAXRES = new ResolutionLevel(0, 1.0);

    private final int index;
    private final double scale;

    public static ResolutionLevel create(MultiLevelModel model, int level) {
        return new ResolutionLevel(level, model.getScale(level));
    }

    public ResolutionLevel(int index, double scale) {
        Assert.argument(index >= 0, "index >= 0");
        Assert.argument(scale >= 1.0, "scale >= 1.0");
        this.index = index;
        this.scale = scale;
    }

    public int getIndex() {
        return index;
    }

    public double getScale() {
        return scale;
    }
}
