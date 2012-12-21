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
package org.esa.nest.dataio.netcdf;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 * Represents a 2D raster dimension.
 *
 * @author Norman Fomferra
 */
public class NcRasterDim {

    private final Dimension dimX;
    private final Dimension dimY;

    public NcRasterDim(Dimension dimX, Dimension dimY) {
        this.dimX = dimX;
        this.dimY = dimY;
    }

    public Dimension getDimX() {
        return dimX;
    }

    public Dimension getDimY() {
        return dimY;
    }

    public boolean isTypicalRasterDim() {
        if(dimX.getName() == null || dimY.getName() == null)
            return false;
        return (dimX.getName().equalsIgnoreCase("lon") && dimY.getName().equalsIgnoreCase("lat")) ||
               (dimX.getName().equalsIgnoreCase("longitude") && dimY.getName().equalsIgnoreCase("latitude")) ||
               (dimX.getName().equalsIgnoreCase("ni") && dimY.getName().equalsIgnoreCase("nj")) ||
               (dimX.getName().equalsIgnoreCase("x") && dimY.getName().equalsIgnoreCase("y"));
    }

    public boolean fitsTo(final Variable varX, final Variable varY) {
        return varX.getRank() == 1 &&
               varY.getRank() == 1 &&
               varX.getDimension(0).getLength() == dimX.getLength() &&
               varY.getDimension(0).getLength() == dimY.getLength();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NcRasterDim) {
            final NcRasterDim other = (NcRasterDim) obj;
            return dimX.equals(other.dimX) &&
                   dimY.equals(other.dimY);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return dimX.hashCode() +
               dimY.hashCode();
    }
}