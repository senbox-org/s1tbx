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

package org.esa.snap.binning;

import org.esa.snap.binning.support.SEAGrid;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author MarcoZ
 * @author Norman
 */
public class ReprojectorTest {

    static final int NAN = -1;
    private BinManager binManager = new BinManager();
    private PlanetaryGrid planetaryGrid;
    private NobsRaster raster;
    private int width;
    private Reprojector reprojector;

    @Before
    public void setUp() throws Exception {
        planetaryGrid = new SEAGrid(6);
        assertEquals(46, planetaryGrid.getNumBins());

        assertEquals(3, planetaryGrid.getNumCols(0));  //  0... 2 --> 0
        assertEquals(8, planetaryGrid.getNumCols(1));  //  3...10 --> 1
        assertEquals(12, planetaryGrid.getNumCols(2)); // 11...22 --> 2
        assertEquals(12, planetaryGrid.getNumCols(3)); // 23...34 --> 3
        assertEquals(8, planetaryGrid.getNumCols(4));  // 35...42 --> 4
        assertEquals(3, planetaryGrid.getNumCols(5));  // 43...45 --> 5

        width = 2 * planetaryGrid.getNumRows();
        int height = planetaryGrid.getNumRows();
        assertEquals(12, width);
        assertEquals(6, height);

        raster = new NobsRaster(new Rectangle(width, height));
        assertEquals("" +
                     "------------\n" +
                     "------------\n" +
                     "------------\n" +
                     "------------\n" +
                     "------------\n" +
                     "------------\n",
                     raster.toString());

        reprojector = new Reprojector(planetaryGrid, raster);
        reprojector.begin();
    }

    @Test
    public void testSubPixelRegion() throws Exception {
        TemporalBinRenderer raster = new NobsRaster(new Rectangle(2, 2, 6, 3));
        assertEquals("" +
                     "------\n" +
                     "------\n" +
                     "------\n",
                     raster.toString());

        Reprojector reprojector = new Reprojector(planetaryGrid, raster);
        reprojector.begin();

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < planetaryGrid.getNumBins(); i++) {
            bins.add(createTBin(i));
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "******\n" +
                     "******\n" +
                     "******\n",
                     raster.toString());


    }

    @Test
    public void testProcessBins_Full() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < planetaryGrid.getNumBins(); i++) {
            bins.add(createTBin(i));
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_Full_multipleParts() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        int numRows = planetaryGrid.getNumRows();
        int startBinIndex = 0;
        for (int row = 0; row < numRows; row++) {
            int numCols = planetaryGrid.getNumCols(row);
            for (int i = startBinIndex; i < startBinIndex + numCols; i++) {
                bins.add(createTBin(i));
            }
            startBinIndex += numCols;
            reprojector.processPart(bins.iterator());
            bins.clear();
        }
        reprojector.end();

        assertEquals("" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "************\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_Empty() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();

        reprojector.processPart(bins.iterator());
        reprojector.end();

        assertEquals("" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_Empty_multipleParts() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();

        reprojector.processPart(bins.iterator());
        reprojector.processPart(bins.iterator());
        reprojector.processPart(bins.iterator());
        reprojector.end();

        assertEquals("" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n",
                     raster.toString());
    }


    @Test
    public void testProcessBins_SomeLinesMissing() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < planetaryGrid.getNumBins(); i++) {
            if (!(planetaryGrid.getRowIndex(i) == 2 || planetaryGrid.getRowIndex(i) == 4)) {
                bins.add(createTBin(i));
            }
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "************\n" +
                     "************\n" +
                     "++++++++++++\n" +
                     "************\n" +
                     "++++++++++++\n" +
                     "************\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_SomeLinesMissing_multipleParts() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        int numRows = planetaryGrid.getNumRows();
        int startBinIndex = 0;
        for (int row = 0; row < numRows; row++) {
            int numCols = planetaryGrid.getNumCols(row);
            for (int i = startBinIndex; i < startBinIndex + numCols; i++) {
                if (!(planetaryGrid.getRowIndex(i) == 2 || planetaryGrid.getRowIndex(i) == 4)) {
                    bins.add(createTBin(i));
                }
            }
            startBinIndex += numCols;
            reprojector.processPart(bins.iterator());
            bins.clear();
        }
        reprojector.end();

        assertEquals("" +
                     "************\n" +
                     "************\n" +
                     "++++++++++++\n" +
                     "************\n" +
                     "++++++++++++\n" +
                     "************\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_Alternating() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < planetaryGrid.getNumBins(); i++) {
            bins.add(createTBin(i));
            i++;  // SKIP!!!
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "****++++****\n" +
                     "+**+**+**+**\n" +
                     "+*+*+*+*+*+*\n" +
                     "+*+*+*+*+*+*\n" +
                     "+**+**+**+**\n" +
                     "++++****++++\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_TopMissing() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 15; i < planetaryGrid.getNumBins(); i++) {  // from 15 on!!!
            bins.add(createTBin(i));
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "++++++++++++\n" +
                     "++++++++++++\n" +
                     "++++********\n" +
                     "************\n" +
                     "************\n" +
                     "************\n",
                     raster.toString());
    }

    @Test
    public void testProcessBins_BottomMissing() throws Exception {

        ArrayList<TemporalBin> bins = new ArrayList<TemporalBin>();
        for (int i = 0; i < 25; i++) {  // only up to 25!!!
            bins.add(createTBin(i));
        }

        reprojector.processPart(bins.iterator());
        reprojector.end();
        assertEquals("" +
                     "************\n" +
                     "************\n" +
                     "************\n" +
                     "**++++++++++\n" +
                     "++++++++++++\n" +
                     "++++++++++++\n",
                     raster.toString());
    }

    @Test
    public void testProcessRowWithBins_CompleteEquator() throws Exception {

        List<TemporalBin> binRow = Arrays.asList(createTBin(11),
                                                 createTBin(12),
                                                 createTBin(13),
                                                 createTBin(14),
                                                 createTBin(15),
                                                 createTBin(16),
                                                 createTBin(17),
                                                 createTBin(18),
                                                 createTBin(19),
                                                 createTBin(20),
                                                 createTBin(21),
                                                 createTBin(22)
        );

        int y = 2;
        reprojector.processPart(binRow.iterator());
        int[] nobsData = raster.nobsData;

        assertEquals(11, nobsData[y * width + 0]);
        assertEquals(12, nobsData[y * width + 1]);
        assertEquals(13, nobsData[y * width + 2]);
        assertEquals(14, nobsData[y * width + 3]);
        assertEquals(15, nobsData[y * width + 4]);
        assertEquals(16, nobsData[y * width + 5]);
        assertEquals(17, nobsData[y * width + 6]);
        assertEquals(18, nobsData[y * width + 7]);
        assertEquals(19, nobsData[y * width + 8]);
        assertEquals(20, nobsData[y * width + 9]);
        assertEquals(21, nobsData[y * width + 10]);
        assertEquals(22, nobsData[y * width + 11]);


    }

    @Test
    public void testProcessRowWithBins_IncompleteEquator() throws Exception {

        List<TemporalBin> binRow = Arrays.asList(//createTBin(11),
                                                 createTBin(12),
                                                 createTBin(13),
                                                 // createTBin(14),
                                                 // createTBin(15),
                                                 createTBin(16),
                                                 // createTBin(17),
                                                 createTBin(18),
                                                 createTBin(19),
                                                 createTBin(20),
                                                 createTBin(21)
                                                 // createTBin(22)
        );

        int y = 2;
        reprojector.processPart(binRow.iterator());
        int[] nobsData = raster.nobsData;

        assertEquals(NAN, nobsData[y * width + 0]);
        assertEquals(12, nobsData[y * width + 1]);
        assertEquals(13, nobsData[y * width + 2]);
        assertEquals(NAN, nobsData[y * width + 3]);
        assertEquals(NAN, nobsData[y * width + 4]);
        assertEquals(16, nobsData[y * width + 5]);
        assertEquals(NAN, nobsData[y * width + 6]);
        assertEquals(18, nobsData[y * width + 7]);
        assertEquals(19, nobsData[y * width + 8]);
        assertEquals(20, nobsData[y * width + 9]);
        assertEquals(21, nobsData[y * width + 10]);
        assertEquals(NAN, nobsData[y * width + 11]);


    }


    @Test
    public void testProcessRowWithBins_CompletePolar() throws Exception {

        List<TemporalBin> binRow = Arrays.asList(createTBin(0),
                                                 createTBin(1),
                                                 createTBin(2));

        int y = 0;
        reprojector.processPart(binRow.iterator());
        int[] nobsData = raster.nobsData;

        assertEquals(0, nobsData[y * width + 0]);
        assertEquals(0, nobsData[y * width + 1]);
        assertEquals(0, nobsData[y * width + 2]);
        assertEquals(0, nobsData[y * width + 3]);
        assertEquals(1, nobsData[y * width + 4]);
        assertEquals(1, nobsData[y * width + 5]);
        assertEquals(1, nobsData[y * width + 6]);
        assertEquals(1, nobsData[y * width + 7]);
        assertEquals(2, nobsData[y * width + 8]);
        assertEquals(2, nobsData[y * width + 9]);
        assertEquals(2, nobsData[y * width + 10]);
        assertEquals(2, nobsData[y * width + 11]);
    }

    @Test
    public void testProcessRowWithBins_IncompletePolar() throws Exception {

        List<TemporalBin> binRow = Arrays.asList(createTBin(0),
                                                 //createTBin(1),
                                                 createTBin(2));

        int y = 0;
        reprojector.processPart(binRow.iterator());
        int[] nobsData = raster.nobsData;

        assertEquals(0, nobsData[y * width + 0]);
        assertEquals(0, nobsData[y * width + 1]);
        assertEquals(0, nobsData[y * width + 2]);
        assertEquals(0, nobsData[y * width + 3]);
        assertEquals(NAN, nobsData[y * width + 4]);
        assertEquals(NAN, nobsData[y * width + 5]);
        assertEquals(NAN, nobsData[y * width + 6]);
        assertEquals(NAN, nobsData[y * width + 7]);
        assertEquals(2, nobsData[y * width + 8]);
        assertEquals(2, nobsData[y * width + 9]);
        assertEquals(2, nobsData[y * width + 10]);
        assertEquals(2, nobsData[y * width + 11]);
    }

    /*
     * Creates a test bin whose #obs = ID.
     */
    private TemporalBin createTBin(int idx) {
        TemporalBin temporalBin = binManager.createTemporalBin(idx);
        temporalBin.setNumObs(idx);
        return temporalBin;
    }

    private static class NobsRaster implements TemporalBinRenderer {

        private final int w;
        private final int h;
        private final int[] nobsData;
        private final Rectangle rasterRegion;

        private NobsRaster(Rectangle rasterRegion) {
            this.rasterRegion = rasterRegion;
            this.w = rasterRegion.width;
            this.h = rasterRegion.height;
            nobsData = new int[w * h];
            Arrays.fill(nobsData, -2);
        }

        @Override
        public Rectangle getRasterRegion() {
            return rasterRegion;
        }

        @Override
        public void begin() throws IOException {
        }

        @Override
        public void end() throws IOException {
        }

        @Override
        public void renderBin(int x, int y, TemporalBin temporalBin, Vector outputVector) throws IOException {
            nobsData[y * w + x] = temporalBin.getNumObs();
        }

        @Override
        public void renderMissingBin(int x, int y) throws IOException {
            nobsData[y * w + x] = -1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nobsData.length; i++) {
                int d = nobsData[i];
                if (d == -2) {
                    // Never visited
                    sb.append('-');
                } else if (d == -1) {
                    // Visited, but missing data
                    sb.append("+");
                } else {
                    // Visited, valid data
                    sb.append("*");
                }
                if ((i + 1) % w == 0) {
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
    }
}
