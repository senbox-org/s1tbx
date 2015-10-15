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

package org.esa.snap.core.dataop.maptransf;

/**
 * The class <code>ISEAG</code> represents the <i>Integerised Sinusoidal Equal Area Grid</i>
 * used by MODIS and SeaWiFS Level-3 binning.
 * 
 * <p><i>Note that this class is not yet public API and may change in future releases.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 *
 * @deprecated since BEAM 4.7, no replacement.
 */
@Deprecated
public class ISEAG {

    /**
     * Represents a 1-based row/col position in the grid.
     */
    public final static class RC {
        public int row; // 1-relative
        public int col; // 1-relative

        public RC() {
        }

        public RC(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    /**
     * Represents a lat/lon position in the grid.
     */
    public final static class LL {
        public double lat;
        public double lon;

        public LL() {
        }

        public LL(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    // total bin numbers
    private final int totBins;
    // total number of rows for binning
    private final int numRows;
    // total number of rows for binning
    private final int[] numBin;
    // first bin no. of each row
    private final int[] baseBin;
    // center latitude of each row
    private final double[] latBin;
    private final double seamLon;

    private int old_row_a = 0;    /* 1-relative */
    private int old_row_b = 0;    /* 1-relative */

    /**
     * Given the total row number.
     */
    public ISEAG(int nrows) {
        seamLon = -180.0;      /*  this value should be passed in  */
        numRows = nrows;

        numBin = new int[numRows];
        baseBin = new int[numRows];
        latBin = new double[numRows];

        double radfac = Math.PI / 180.0;

        for (int i = 0; i < numRows; i++) {
            latBin[i] = (i + 0.5) * (180.0 / numRows) - 90.0;
            numBin[i] = (int) (Math.cos(latBin[i] * radfac) * (2.0 * numRows) + 0.5);
        }

        baseBin[0] = 1;

        for (int i = 1; i < numRows; i++) {
            baseBin[i] = baseBin[i - 1] + numBin[i - 1];
        }

        totBins = baseBin[numRows - 1] + numBin[numRows - 1] - 1;
    }

    /**
     * @return the total number of bins
     */
    public int getBinCount() {
        return totBins;
    }

    /**
     * @return the total number of rows
     */
    public int getRowCount() {
        return numRows;
    }

    /**
     * @param row 1-relative
     * @return the number of columns in the row
     */
    public int getColumnCount(int row) {
        return numBin[row - 1];
    }

    /**
     * Given a bin number, return the center lat/lon of that bin number heuristic and binary search algorithm is used.
     *
     * @param bin 1-relative
     */
    public void bin2ll(int bin, LL ll) {
        int row, rlow, rhi, rmid;

        if (old_row_a > 0 && baseBin[old_row_a - 1] <= bin && baseBin[old_row_a] > bin) {
            row = old_row_a;
        } else {
            if (bin < 1)
                bin = 1;             /* south pole */
            if (bin > totBins)
                bin = totBins;       /* north pole */

            /* binary search for row in range [1..numRows] */
            rlow = 1;            /* 1-relative */
            rhi = numRows;       /* 1-relative */
            while (true) {
                rmid = (rlow + rhi - 1) / 2;     /* 0-relative */
                if (baseBin[rmid] > bin)
                    rhi = rmid;
                else
                    rlow = rmid + 1;

                if (rlow == rhi) {
                    row = rlow;
                    break;
                }
            }
            old_row_a = row;
        }

        ll.lat = latBin[row - 1];
        ll.lon = 360.0 * (bin - baseBin[row - 1] + 0.5) / numBin[row - 1];
        ll.lon = ll.lon + seamLon;  /* note, lon returned here may be in 0 to 360 */
    }


    /**
     * Given the lat/lon, return the bin number.
     * @param ll ll.lon has to be in the range of -180.0 to 180.0
     * @return bin 1-relative
     */
    public int ll2bin(LL ll) {
        return ll2bin(ll.lat, ll.lon);
    }

    /**
     * Given the lat/lon, return the bin number.
     * @param lat
     * @param lon has to be in the range of -180.0 to 180.0
     * @return bin 1-relative
     */
    public int ll2bin(double lat, double lon) {
        int row, col;       /* 0-relative */
        row = (int) ((90.0 + lat) * numRows / 180.0);
        col = (int) (numBin[row] * (lon - seamLon) / 360.0);
        return baseBin[row] + col;
    }

    /**
     * Given the lat/lon, return the row, column.
     *
     * @param ll ll.lon has to be in the range of -180.0 to 180.0
     * @param rc 1-relative
     */
    public void ll2rc(LL ll, RC rc) {
        ll2rc(ll.lat, ll.lon, rc);
    }

    /**
     * Given the lat/lon, return the row, column.
     *
     * @param lat
     * @param lon has to be in the range of -180.0 to 180.0
     */
    public void ll2rc(double lat, double lon, RC rc) {
        int row = (int) ((90.0 + lat) * numRows / 180.0);
        if (row <0) row = 0;
        if (row >=numRows) row = numRows-1;
        int col = (int) (numBin[row] * (lon - seamLon) / 360.0);
        rc.row = row + 1;
        rc.col = col + 1;
    }

    /**
     * Given row/column, return lat/lon.
     *
     * @param rc 1-relative
     * @param ll
     */
    public void rc2ll(RC rc, LL ll) {
        rc2ll(rc.row, rc.col, ll);
    }

    /**
     * Given row/column, return lat/lon.
     *
     * @param row 1-relative
     * @param col 1-relative
     * @param ll
     */
    public void rc2ll(int row, int col, LL ll) {
        row--;
        if (row <0) row = 0;
        if (row >=numRows) row = numRows-1;
        ll.lat = latBin[row];
        ll.lon = seamLon + (360.0 * (col - 0.5) / numBin[row]);
    }


    /**
     * Given a row/column number, return the bin number (1-relative)
     */
    public int rc2bin(RC rc) {
        return rc2bin(rc.row, rc.col);
    }

    /**
     * Given a row/column number, return the bin number (1-relative)
     */
    public int rc2bin(int row, int col) {
        return baseBin[row - 1] + col - 1;
    }



    /**
     * Given a bin number, return the row and column (both are 1-relative) heuristic and binary search algorithm is used.
     *
     * @param bin 1-relative
     * @param rc 1-relative
     */
    public void bin2rc(int bin, RC rc) {

        int rlow, rhi, rmid;

        if (old_row_b > 0 && baseBin[old_row_b - 1] <= bin && baseBin[old_row_b] > bin) {
            rc.row = old_row_b;
        } else {
            if (bin < 1)
                bin = 1;             /* south pole */
            if (bin > totBins)
                bin = totBins;       /* north pole */

            /* binary search for row in range [1..numRows] */
            rlow = 1;            /* 1-relative */
            rhi = numRows;       /* 1-relative */
            while (true) {
                rmid = (rlow + rhi - 1) / 2;     /* 0-relative */
                if (baseBin[rmid] > bin)
                    rhi = rmid;
                else
                    rlow = rmid + 1;

                if (rlow == rhi) {
                    rc.row = rlow;
                    break;
                }
            }
            old_row_b = rc.row;
        }

        rc.col = bin - baseBin[rc.row - 1] + 1;
    }


    /**
     * Given a bin number, return the center lat/lon of that bin number no heuristic or binary search algorithm is used this routine is very slow due to the array reference (I think).
     */
    void old_bin2ll(int bin, LL ll) {
        int row = numRows - 1;

        while (bin < baseBin[row]) {
            row--;
        }

        ll.lat = latBin[row];
        ll.lon = 360.0 * (bin - baseBin[row] + 0.5) / numBin[row];
    }



/*  update version, much faster than using array reference */

//    int old_bin2ll
//            (bin, lat, lon)
//
//    int bin;
//    double *lat, *lon;
//    {
//        int row;
//        int *tmpptr;
//
//        row = numRows - 1;
//        tmpptr = baseBin + numRows - 1;
//
//        while (bin < *tmpptr--)
//        row--;
//
//        *lat = latBin[row];
//        *lon = 360.0 * (bin - baseBin[row] + 0.5) / numBin[row];
//    }
}
