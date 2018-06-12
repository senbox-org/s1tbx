package org.esa.snap.core.util.grid.isin;



class IsinForward {

    private static final double EPS_CNVT = 0.01;    // Doubles must be within this of an integer to be valid
    private static final double EPS_SPHERE = 1.0e-10;  // Minimum sphere radius
    private static final long NROW_MAX = 360 * 3600;  // Maximum number of rows (zones)

    private static final double TWOPI = 2.0 * Math.PI;
    private static final double TWOPI_INV = 1.0 / (2.0 * Math.PI);
    private static final double HALFPI = 0.5 * Math.PI;

    double false_east;
    double false_north;
    double sphere;
    double sphere_inv;
    double ang_size_inv;
    long nrow;
    long nrow_half;
    double lon_cen_mer;
    double ref_lon;
    double col_dist;
    double col_dist_inv;
    int ijustify;
    Isin_row[] row;

    void init(double radius, double centerLon, double falseEasting, double falseNorth, double dzone, double djustify) {
        long nzone = (long) (dzone + EPS_CNVT);
        if (Math.abs(dzone - nzone) > EPS_CNVT) {
            throw new RuntimeException("bad parameter; nzone not near an integer value");
        }

        if ((nzone % 2) != 0) {
            throw new RuntimeException("bad parameter; nzone not multiple of two");
        }

        if (djustify < -EPS_CNVT || djustify > (2.0 + EPS_CNVT)) {
            throw new RuntimeException("bad parameter; ijustify out of range");
        }

        final int ijustify = (int) (djustify + EPS_CNVT);
        if (Math.abs(djustify - ijustify) > EPS_CNVT) {
            throw new RuntimeException("bad parameter; ijustify not near an integer value");
        }

        if (radius < EPS_SPHERE) {
            throw new RuntimeException("bad parameter; sphere radius too small");
        }

        if (centerLon < -TWOPI || centerLon > TWOPI) {
            throw new RuntimeException("bad parameter; center longitude invalid");
        }

        if (nzone < 2 || nzone > NROW_MAX) {
            throw new RuntimeException("bad parameter; number of zones out of range");
        }

        double lon_cen_mer = centerLon;
        if (lon_cen_mer < -Math.PI) {
            lon_cen_mer += TWOPI;
        } else if (lon_cen_mer > Math.PI) {
            lon_cen_mer -= TWOPI;
        }

        this.false_east = falseEasting;
        this.false_north = falseNorth;
        this.sphere = radius;
        this.sphere_inv = 1.0 / radius;
        this.ang_size_inv = nzone / Math.PI;
        this.nrow = nzone;
        this.nrow_half = nzone / 2;
        this.lon_cen_mer = lon_cen_mer;
        this.ref_lon = lon_cen_mer - Math.PI;
        this.ijustify = ijustify;

        this.row = new Isin_row[(int) nrow_half];
        for (int irow = 0; irow < nrow_half; irow++) {
            final Isin_row currentRow = new Isin_row();

            // Calculate latitude at center of row
            final double clat = HALFPI * (1.0 - (irow + 0.5) / nrow_half);

            // Calculate number of columns per row
            if (ijustify < 2)
                currentRow.ncol = (long) ((2.0 * Math.cos(clat) * nrow) + 0.5);
            else { // make the number of columns even
                currentRow.ncol = (long) ((Math.cos(clat) * nrow) + 0.5);
                currentRow.ncol *= 2;
            }

            // Must have at least one column
            if (currentRow.ncol < 1) {
                currentRow.ncol = 1;
            }

            // Save the inverse of the number of columns
            currentRow.ncol_inv = 1.0 / ((double) currentRow.ncol);

            /* Calculate the column number of the column whose left edge touches the
             * central meridian */
            if (ijustify == 1) {
                currentRow.icol_cen = (currentRow.ncol + 1) / 2;
            } else {
                currentRow.icol_cen = currentRow.ncol / 2;
            }

            row[irow] = currentRow;
        }

        // Get the number of columns at the equator
        final int equatorRow = (int) (nrow_half - 1);
        final long ncol_cen = row[equatorRow].ncol;

        /* Calculate the distance at the equator between
         * the centers of two columns (and the inverse) */

        col_dist = (TWOPI * sphere) / ncol_cen;
        col_dist_inv = ncol_cen / (TWOPI * sphere);

    }

    IsinPoint transform(IsinPoint point) {
        final double lon = point.getX();
        final double lat = point.getY();

        final double y = false_north + (lat * sphere);

        // integer row number
        final double row = (HALFPI - lat) * ang_size_inv;
        long irow = (long) row;
        if (irow >= nrow_half) {
            irow = (nrow - 1) - irow;
        }
        if (irow < 0) {
            irow = 0;
        }

        // Fractional longitude
        double flon = (lon - ref_lon) * TWOPI_INV;
        if (flon < 0.0) {
            flon += (1 - (long) flon);
        }
        if (flon > 1.0) {
            flon -= (long) flon;
        }

        // Column number (relative to center)
        final double col = (this.row[(int) irow].ncol * flon) - this.row[(int) irow].icol_cen;
        final double x = false_east + (col_dist * col);

        return new IsinPoint(x, y);
    }
}
