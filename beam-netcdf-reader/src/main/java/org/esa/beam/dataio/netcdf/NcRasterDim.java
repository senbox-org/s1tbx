package org.esa.beam.dataio.netcdf;

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
        return (dimX.getName().equalsIgnoreCase("lon") && dimY.getName().equalsIgnoreCase("lat")) ||
               (dimX.getName().equalsIgnoreCase("longitude") && dimY.getName().equalsIgnoreCase("latitude")) ||
               (dimX.getName().equalsIgnoreCase("ni") && dimY.getName().equalsIgnoreCase("nj")) ||
               (dimX.getName().equalsIgnoreCase("x") && dimY.getName().equalsIgnoreCase("y"));
    }

    public boolean fitsTo(final Variable varX, final Variable varY) {
        return varX.getRank() == 1 &&
               varY.getRank() == 1 &&
               varX.getDimension(0).getLength() == getDimX().getLength() &&
               varY.getDimension(0).getLength() == getDimY().getLength();
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
