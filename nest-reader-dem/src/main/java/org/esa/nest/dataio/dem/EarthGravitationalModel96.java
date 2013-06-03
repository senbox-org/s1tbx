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
package org.esa.nest.dataio.dem;

import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.util.MathUtils;
import org.esa.nest.util.Settings;

import java.io.*;
import java.util.StringTokenizer;

/**
 *               "WW15MGH.GRD"
 *
 * This file contains 1038961 point values in grid form.  The first row of the file is the "header" of the file
 * and shows the south, north, west, and east limits of the file followed by the grid spacing in n-s and e-w.
 * All values in the "header" are in DECIMAL DEGREES.
 *
 * The geoid undulation grid is computed at 15 arc minute spacings in north/south and east/west with the new
 * "EGM96" spherical harmonic potential coefficient set complete to degree and order 360 and a geoid height
 * correction value computed from a set of spherical harmonic coefficients ("CORRCOEF"), also to degree and
 * order 360.  The file is arranged from north to south, west to east (i.e., the data after the header is
 * the north most latitude band and is ordered from west to east).
 *
 * The coverage of this file is:
 *
 *                90.00 N  +------------------+
 *                         |                  |
 *                         | 15' spacing N/S  |
 *                         |                  |
 *                         |                  |
 *                         | 15' spacing E/W  |
 *                         |                  |
 *               -90.00 N  +------------------+
 *                        0.00 E           360.00 E
 */

public final class EarthGravitationalModel96 {

    private static final String NAME = "ww15mgh_b.grd";
    private static final int NUM_LATS = 721; // 180*4 + 1  (cover 90 degree to -90 degree)
    private static final int NUM_LONS = 1441; // 360*4 + 1 (cover 0 degree to 360 degree)
    private static final int NUM_CHAR_PER_NORMAL_LINE = 74;
    private static final int NUM_CHAR_PER_SHORT_LINE = 11;
    private static final int NUM_CHAR_PER_EMPTY_LINE = 1;
    private static final int BLOCK_HEIGHT = 20;
    private static final int NUM_OF_BLOCKS_PER_LAT = 9;

    private static final int MAX_LATS = NUM_LATS - 1;
    private static final int MAX_LONS = NUM_LONS - 1;

    private final float[][] egm = new float[NUM_LATS][NUM_LONS];
    private static EarthGravitationalModel96 theInstance = null;

    public static EarthGravitationalModel96 instance() {
        if(theInstance == null) {
            theInstance = new EarthGravitationalModel96();
        }
        return theInstance;
    }

    private EarthGravitationalModel96() {

        // get absolute file path
        final String filePath = Settings.instance().get("AuxData/egm96AuxDataPath");
        final String fileName = filePath + File.separator + NAME;

        // get reader
        FileInputStream stream;
        try {
            stream = new FileInputStream(fileName);
        } catch(FileNotFoundException e) {
            throw new OperatorException("File not found: " + fileName);
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // read data from file and save them in 2-D array
        String line = "";
        StringTokenizer st;
        int rowIdx = 0;
        int colIdx = 0;
        try {

            final int numLatLinesToSkip = 0;
            final int numCharInHeader = NUM_CHAR_PER_NORMAL_LINE + NUM_CHAR_PER_EMPTY_LINE;
            final int numCharInEachLatLine = NUM_OF_BLOCKS_PER_LAT * BLOCK_HEIGHT * NUM_CHAR_PER_NORMAL_LINE +
                                             (NUM_OF_BLOCKS_PER_LAT + 1) * NUM_CHAR_PER_EMPTY_LINE +
                                             NUM_CHAR_PER_SHORT_LINE;

            final int totalCharToSkip = numCharInHeader + numCharInEachLatLine * numLatLinesToSkip;
            reader.skip(totalCharToSkip);

            // get the lat lines from 90 deg to -90 deg 45 min
            final int numLinesInEachLatLine = NUM_OF_BLOCKS_PER_LAT * (BLOCK_HEIGHT + 1) + 2;
            final int numLinesToRead = NUM_LATS * numLinesInEachLatLine;
            int linesRead = 0;
            for (int i = 0; i < numLinesToRead - 1; i++) { // -1 because the last line read from file is null

                line = reader.readLine();
                linesRead++;
                if (!line.equals("")) {
                    st = new StringTokenizer(line);
                    final int numCols = st.countTokens();
                    for (int j = 0; j < numCols; j++) {
                        egm[rowIdx][colIdx] = Float.parseFloat(st.nextToken());
                        colIdx++;
                    }
                }

                if (linesRead % numLinesInEachLatLine == 0) {
                    rowIdx++;
                    colIdx = 0;
                }
            }

            reader.close();
            stream.close();

        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    public double getEGM(final double lat, final double lon) {

        final double r = (90 - lat) / 0.25;
        final double c = (lon < 0? lon + 360 : lon)/ 0.25;

        final double[][] v = new double[4][4];
        final int r0 = FastMath.max(((int)r-1), 0);
        int c0 = FastMath.max(((int)c-1), 0);

        int ci1 = c0 + 1;
        int ci2 = c0 + 2;
        int ci3 = c0 + 3;
        if(ci3 > MAX_LONS) {
            c0  = FastMath.min(c0  , MAX_LONS);
            ci1 = FastMath.min(ci1, MAX_LONS);
            ci2 = FastMath.min(ci2, MAX_LONS);
            ci3 = FastMath.min(ci3, MAX_LONS);
        }

        for (int i = 0; i < 4; i++) {
            final int ri = r0+i > MAX_LATS ? FastMath.min(r0 + i, MAX_LATS) : r0+i;

            //unrolled loop
            v[i][0] = egm[ri][c0];
            v[i][1] = egm[ri][ci1];
            v[i][2] = egm[ri][ci2];
            v[i][3] = egm[ri][ci3];
        }

        return MathUtils.interpolationBiCubic(v, c - (c0+1), r - (r0+1));
    }
}