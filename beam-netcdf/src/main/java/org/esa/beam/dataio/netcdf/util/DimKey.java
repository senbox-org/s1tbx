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

package org.esa.beam.dataio.netcdf.util;

import com.bc.ceres.core.Assert;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 * Wraps a NetCDF dimension array so that it can be used as key.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
public class DimKey {

    private final Dimension[] dims;

    public DimKey(Dimension... dims) {
        Assert.argument(dims.length >= 1, "dims.length >= 1");
        for (Dimension dim : dims) {
            Assert.notNull(dim, "dim");
        }
        this.dims = dims;
    }

    public int getRank() {
        return dims.length;
    }

    public Dimension getDimensionX() {
        return getDimension(getRank() - 1);
    }

    public Dimension getDimensionY() {
        return getDimension(getRank() - 2);
    }

    public Dimension getDimension(int index) {
        return dims[index];
    }

    public boolean isTypicalRasterDim() {
        return (matchesXYDimNames("lon", "lat") ||
                matchesXYDimNames("long", "lat") ||
                matchesXYDimNames("longitude", "latitude") ||
                matchesXYDimNames("ni", "nj") ||
                matchesXYDimNames("x", "y"));
    }

    // Move to GeocodingUtils

    public boolean fitsTo(final Variable varX, final Variable varY) {
        return varX.getRank() == 1 &&
                varY.getRank() == 1 &&
                varX.getDimension(0).getLength() == getDimensionX().getLength() &&
                varY.getDimension(0).getLength() == getDimensionY().getLength();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DimKey dimKey = (DimKey) o;
        return getDimensionY().getLength() == dimKey.getDimensionY().getLength()
                && getDimensionX().getLength() == dimKey.getDimensionX().getLength();
    }

    @Override
    public int hashCode() {
        return 31 * getDimensionY().getLength() + getDimensionX().getLength();
    }

    private boolean matchesXYDimNames(final String xName, final String yName) {
        if (getDimensionX().getName() != null && getDimensionY().getName() != null) {
            return getDimensionX().getName().equalsIgnoreCase(xName)
                    && getDimensionY().getName().equalsIgnoreCase(yName);
        } else {
            return false;
        }
    }
}
