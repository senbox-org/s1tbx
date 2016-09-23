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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.BeamConstants;

import java.io.IOException;


public class TiePointGridTest extends AbstractRasterDataNodeTest {

    private final float eps = 1.e-10F;
    private final int gridWidth = 3;
    private final int gridHeight = 5;
    private final float[] tiePoints = new float[]{
            0.0F, 1.0F, 2.0F,
            1.0F, 2.0F, 3.0F,
            2.0F, 3.0F, 4.0F,
            3.0F, 4.0F, 5.0F,
            4.0F, 5.0F, 6.0F
    };

    @Override
    protected RasterDataNode createRasterDataNode() {
        return new TiePointGrid("default", 2, 2, 0, 0, 1, 1, new float[]{1f, 2f, 3f, 4f});
    }

    public void testConstructors() {
        try {
            new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 4, 2);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }
        try {
            new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 4, 2, tiePoints);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }
        try {
            new TiePointGrid(null, gridWidth, gridHeight, 0, 0, 4, 2, tiePoints);
            fail("IllegalArgumentException expected, name was null");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }
        try {
            new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 0, 2, tiePoints);
            fail("IllegalArgumentException expected, sunSamplingX was less than 1");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }
        try {
            new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 4, 0, tiePoints);
            fail("IllegalArgumentException expected, sunSamplingY was less than 1");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }
        try {
            new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 4, 2, null);
            fail("NullPointerException expected, tiePoints was null");
        } catch (NullPointerException e) {
            // IllegalArgumentException expected
        }
        try {
            new TiePointGrid("x", gridWidth, 6, 0, 0, 4, 2, tiePoints);
            fail("IllegalArgumentException expected, to few tiePoints");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }
    }

    public void testProperties() {
        TiePointGrid grid = new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 4, 2, tiePoints);

        assertEquals("x", grid.getName());
        assertEquals(3, grid.getGridWidth());
        assertEquals(5, grid.getGridHeight());
        assertEquals(9, grid.getRasterWidth());
        assertEquals(9, grid.getRasterHeight());
        assertEquals(0, grid.getOffsetX(), 1e-6F);
        assertEquals(0, grid.getOffsetY(), 1e-6F);
        assertEquals(4, grid.getSubSamplingX(), 1e-6F);
        assertEquals(2, grid.getSubSamplingY(), 1e-6F);
        assertEquals(TiePointGrid.DISCONT_NONE, grid.getDiscontinuity());

        assertNotNull(grid.getData());
        assertEquals(3 * 5, grid.getData().getNumElems());
        assertSame(grid.getData(), grid.getGridData());
        assertSame(grid.getGridData(), grid.getGridData());

        assertNotNull(grid.getRasterData());
        assertEquals(9 * 9, grid.getRasterData().getNumElems());
        assertSame(grid.getRasterData(), grid.getRasterData());

        assertNotNull(grid.getTiePoints());
        assertSame(grid.getTiePoints(), tiePoints);
        assertEquals(15, grid.getTiePoints().length);
        for (int i = 0; i < 15; i++) {
            assertEquals(tiePoints[i], grid.getTiePoints()[i], 1e-10F);
        }
    }

    public void testPropertiesNoSubSampling() {
        TiePointGrid grid = new TiePointGrid("x", gridWidth, gridHeight, 0, 0, 1, 1, tiePoints);

        assertEquals("x", grid.getName());
        assertEquals(3, grid.getGridWidth());
        assertEquals(5, grid.getGridHeight());
        assertEquals(3, grid.getRasterWidth());
        assertEquals(5, grid.getRasterHeight());
        assertEquals(0, grid.getOffsetX(), 1e-6F);
        assertEquals(0, grid.getOffsetY(), 1e-6F);
        assertEquals(1, grid.getSubSamplingX(), 1e-6F);
        assertEquals(1, grid.getSubSamplingY(), 1e-6F);
        assertEquals(TiePointGrid.DISCONT_NONE, grid.getDiscontinuity());


        assertNotNull(grid.getData());
        assertEquals(3 * 5, grid.getData().getNumElems());
        assertSame(grid.getData(), grid.getGridData());
        assertSame(grid.getGridData(), grid.getGridData());
        assertSame(grid.getData(), grid.getRasterData());
        assertSame(grid.getRasterData(), grid.getRasterData());
    }

    public void testInterpolation() {

        TiePointGrid grid = new TiePointGrid("x", gridWidth, gridHeight, 0.5f, 0.5f, 4, 2, tiePoints);

        float[][] interpolated = new float[][]{
                {0.0F, 0.5F, 1.0F, 1.5F, 2.0F},
                {0.5F, 1.0F, 1.5F, 2.0F, 2.5F},
                {1.0F, 1.5F, 2.0F, 2.5F, 3.0F},
                {1.5F, 2.0F, 2.5F, 3.0F, 3.5F},
                {2.0F, 2.5F, 3.0F, 3.5F, 4.0F},
                {2.5F, 3.0F, 3.5F, 4.0F, 4.5F},
                {3.0F, 3.5F, 4.0F, 4.5F, 5.0F},
                {3.5F, 4.0F, 4.5F, 5.0F, 5.5F},
                {4.0F, 4.5F, 5.0F, 5.5F, 6.0F}
        };

        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 4; i++) {
                int x = i * 4 / 2;
                int y = j * 2 / 2;
                assertEquals(interpolated[j][i], grid.getPixelFloat(x, y), eps);
            }
        }

        int rasterWidth = grid.getRasterWidth();
        ProductData rasterData = grid.getRasterData();
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 4; i++) {
                int x = i * 4 / 2;
                int y = j * 2 / 2;
                assertEquals(interpolated[j][i], rasterData.getElemFloatAt(rasterWidth * y + x), eps);
            }
        }
    }

    public void testExtrapolation() {

        TiePointGrid grid = new TiePointGrid("x", gridWidth, gridHeight, 0.5f, 0.5f, 4, 2, tiePoints);

        assertEquals(-0.75F, grid.getPixelFloat(-1, -1), eps);
        assertEquals(+0.50F, grid.getPixelFloat(4, -1), eps);
        assertEquals(+1.75F, grid.getPixelFloat(9, -1), eps);
        assertEquals(+1.75F, grid.getPixelFloat(-1, 4), eps);
        assertEquals(+3.00F, grid.getPixelFloat(4, 4), eps);
        assertEquals(+4.25F, grid.getPixelFloat(9, 4), eps);
        assertEquals(+4.25F, grid.getPixelFloat(-1, 9), eps);
        assertEquals(+5.50F, grid.getPixelFloat(4, 9), eps);
        assertEquals(+6.75F, grid.getPixelFloat(9, 9), eps);
    }

    public void testAccess_TiePointGrid() {
        TiePointGrid tiePointGrid = new TiePointGrid("x",
                                                     3, 3,
                                                     0.5f, 1.5f,
                                                     3, 5,
                                                     new float[]{
                                                             2, 4, 6,
                                                             12, 10, 8,
                                                             14, 16, 18
                                                     });

        // Test for the current tie pont positions.
        // The positions marked with ">xx<" at the raster below
        assertEquals(2, tiePointGrid.getPixelFloat(0.5f, 1.5f), 1e-5f);
        assertEquals(4, tiePointGrid.getPixelFloat(3.5f, 1.5f), 1e-5f);
        assertEquals(6, tiePointGrid.getPixelFloat(6.5f, 1.5f), 1e-5f);
        assertEquals(12, tiePointGrid.getPixelFloat(0.5f, 6.5f), 1e-5f);
        assertEquals(10, tiePointGrid.getPixelFloat(3.5f, 6.5f), 1e-5f);
        assertEquals(8, tiePointGrid.getPixelFloat(6.5f, 6.5f), 1e-5f);
        assertEquals(14, tiePointGrid.getPixelFloat(0.5f, 11.5f), 1e-5f);
        assertEquals(16, tiePointGrid.getPixelFloat(3.5f, 11.5f), 1e-5f);
        assertEquals(18, tiePointGrid.getPixelFloat(6.5f, 11.5f), 1e-5f);

        // Test for horizontal interpolation and extrapolation
        // The positions marked with "H" at the raster below
        assertEquals(1, tiePointGrid.getPixelFloat(-1f, 1.5f), 1e-5f);
        assertEquals(3, tiePointGrid.getPixelFloat(2f, 1.5f), 1e-5f);
        assertEquals(5, tiePointGrid.getPixelFloat(5f, 1.5f), 1e-5f);
        assertEquals(7, tiePointGrid.getPixelFloat(8f, 1.5f), 1e-5f);

        // Test for vertical interpolation and extrapolation
        // The positions marked with "V" at the raster below
        assertEquals(-3, tiePointGrid.getPixelFloat(0.5f, -1f), 1e-5f);
        assertEquals(7, tiePointGrid.getPixelFloat(0.5f, 4f), 1e-5f);
        assertEquals(13, tiePointGrid.getPixelFloat(0.5f, 9f), 1e-5f);
        assertEquals(15, tiePointGrid.getPixelFloat(0.5f, 14f), 1e-5f);

        // Test for diagonal interpolation between tie points
        // The positions marked with "D" at the raster below
        assertEquals(7, tiePointGrid.getPixelFloat(2f, 4f), 1e-5f);
        assertEquals(7, tiePointGrid.getPixelFloat(5f, 4f), 1e-5f);
        assertEquals(13, tiePointGrid.getPixelFloat(2f, 9f), 1e-5f);
        assertEquals(13, tiePointGrid.getPixelFloat(5f, 9f), 1e-5f);

        // Test for diagonal extrapolation outside tie points
        // The positions marked with "E" at the raster below
        assertEquals(-5, tiePointGrid.getPixelFloat(-1f, -1f), 1e-5f);
        assertEquals(7, tiePointGrid.getPixelFloat(8f, -1f), 1e-5f);
        assertEquals(13, tiePointGrid.getPixelFloat(-1f, 14f), 1e-5f);
        assertEquals(25, tiePointGrid.getPixelFloat(8f, 14f), 1e-5f);

//        -1   0    1    2    3    4    5    6    7    8
//        |    |    |    |    |    |    |    |    |    |
//        E----+--V-+----+----+----+----+----+----+----E--   -1
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    0
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    1
//        H    |> 2<|    H    |> 4<|    H    |> 6<|    H
//        +----+----+----+----+----+----+----+----+----+--    2
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    3
//        |    |    |    |    |    |    |    |    |    |
//        +----+--V-+----D----+----+----D----+----+----+--    4
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    5
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    6
//        |    |>12<|    |    |>10<|    |    |> 8<|    |
//        +----+----+----+----+----+----+----+----+----+--    7
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--    8
//        |    |    |    |    |    |    |    |    |    |
//        +----+--V-+----D----+----+----D----+----+----+--    9
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--   10
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--   11
//        |    |>14<|    |    |>16<|    |    |>18<|    |
//        +----+----+----+----+----+----+----+----+----+--   12
//        |    |    |    |    |    |    |    |    |    |
//        +----+----+----+----+----+----+----+----+----+--   13
//        |    |    |    |    |    |    |    |    |    |
//        E----+--V-+----+----+----+----+----+----+----E--   14
    }

    public void testAccess_MERIS_TiePointGrid() {
        String tpgName = "x";
        int gridWidth = 4;
        int gridHeight = 3;
        float offsetX = BeamConstants.MERIS_TIE_POINT_OFFSET_X;
        float offsetY = BeamConstants.MERIS_TIE_POINT_OFFSET_Y;
        int subSamplingX = 5;
        int subSamplingY = 2;
        float[] tiepoints = new float[]{
                00.0F, 05.0F, 10.0F, 15.0F,
                20.0F, 30.0F, 40.0F, 50.0F,
                -0.05F, -0.10F, -0.15F, -0.20F
        };
        TiePointGrid grid = new TiePointGrid(tpgName,
                                             gridWidth, gridHeight,
                                             offsetX, offsetY,
                                             subSamplingX, subSamplingY,
                                             tiepoints);

        float[][] valuesForIntAccess = new float[][]{
//            Line 1
                new float[]{0, 0, 0},
                new float[]{5, 0, 5},
                new float[]{6, 0, 6},
                new float[]{7, 0, 7},
                new float[]{8, 0, 8},
                new float[]{9, 0, 9},
                new float[]{10, 0, 10},
                new float[]{15, 0, 15},
//            Line 2
                new float[]{0, 1, 10},
                new float[]{5, 1, 17.5f},
                new float[]{6, 1, 19},
                new float[]{7, 1, 20.5f},
                new float[]{8, 1, 22},
                new float[]{9, 1, 23.5f},
                new float[]{10, 1, 25},
                new float[]{15, 1, 32.5f},
//            Line 3
                new float[]{0, 2, 20},
                new float[]{5, 2, 30},
                new float[]{6, 2, 32},
                new float[]{7, 2, 34},
                new float[]{8, 2, 36},
                new float[]{9, 2, 38},
                new float[]{10, 2, 40},
                new float[]{15, 2, 50},
//            Line 4
                new float[]{0, 3, 9.975f},
                new float[]{5, 3, 14.95f},
                new float[]{6, 3, 15.945f},
                new float[]{7, 3, 16.94f},
                new float[]{8, 3, 17.935f},
                new float[]{9, 3, 18.93f},
                new float[]{10, 3, 19.925f},
                new float[]{15, 3, 24.9f},
//            Line 5
                new float[]{0, 4, -0.05f},
                new float[]{5, 4, -0.10f},
                new float[]{6, 4, -0.11f},
                new float[]{7, 4, -0.12f},
                new float[]{8, 4, -0.13f},
                new float[]{9, 4, -0.14f},
                new float[]{10, 4, -0.15f},
                new float[]{15, 4, -0.20f}
        };

        for (int i = 0; i < valuesForIntAccess.length; i++) {
            float[] intAcces = valuesForIntAccess[i];
            final int x = (int) intAcces[0];
            final int y = (int) intAcces[1];
            final float expected = intAcces[2];
            final float actual = grid.getPixelFloat(x, y);
            assertEquals("loop counter i = " + i, expected, actual, 1e-5f);
        }
    }

    public void testAccess_AATSR_LOC_TiePointGrid() {
        String tpgName = "x";
        int gridWidth = BeamConstants.AATSR_LOC_TIE_POINT_GRID_WIDTH;
        int gridHeight = 2;
        float offsetX = BeamConstants.AATSR_LOC_TIE_POINT_OFFSET_X;
        float offsetY = BeamConstants.MERIS_TIE_POINT_OFFSET_Y;
        int subSamplingX = BeamConstants.AATSR_LOC_TIE_POINT_SUBSAMPLING_X;
        int subSamplingY = BeamConstants.AATSR_LOC_TIE_POINT_SUBSAMPLING_Y;
        float[] tiepoints = new float[]{
                01f, 02f, 03f, 04f, 05f, 06f, 07f, 08f, 09f, 10f, 11f,
                12f,
                13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f,

                00f, 01f, 02f, 03f, 04f, 05f, 06f, 07f, 08f, 09f, 10f,
                11f,
                12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f
        };
        TiePointGrid grid = new TiePointGrid(tpgName,
                                             gridWidth, gridHeight,
                                             offsetX, offsetY,
                                             subSamplingX, subSamplingY,
                                             tiepoints);

        float[][] valuesForIntAccess = new float[][]{
//            Line 0
                new float[]{0, 0, 1.78f},
                new float[]{5, 0, 1.98f},
                new float[]{6, 0, 2.02f},
                new float[]{30, 0, 2.98f},
                new float[]{31, 0, 3.02f},
                new float[]{55, 0, 3.98f},
                new float[]{56, 0, 4.02f},
                new float[]{80, 0, 4.98f},
                new float[]{81, 0, 5.02f},
                new float[]{105, 0, 5.98f},
                new float[]{106, 0, 6.02f},
                new float[]{130, 0, 6.98f},
                new float[]{131, 0, 7.02f},
                new float[]{155, 0, 7.98f},
                new float[]{156, 0, 8.02f},
                new float[]{180, 0, 8.98f},
                new float[]{181, 0, 9.02f},
                new float[]{205, 0, 9.98f},
                new float[]{206, 0, 10.02f},
                new float[]{230, 0, 10.98f},
                new float[]{231, 0, 11.02f},
                new float[]{255, 0, 11.98f},
                new float[]{256, 0, 12.02f},
                new float[]{280, 0, 12.98f},
                new float[]{281, 0, 13.02f},
                new float[]{305, 0, 13.98f},
                new float[]{306, 0, 14.02f},
                new float[]{330, 0, 14.98f},
                new float[]{331, 0, 15.02f},
                new float[]{355, 0, 15.98f},
                new float[]{356, 0, 16.02f},
                new float[]{380, 0, 16.98f},
                new float[]{381, 0, 17.02f},
                new float[]{405, 0, 17.98f},
                new float[]{406, 0, 18.02f},
                new float[]{430, 0, 18.98f},
                new float[]{431, 0, 19.02f},
                new float[]{455, 0, 19.98f},
                new float[]{456, 0, 20.02f},
                new float[]{480, 0, 20.98f},
                new float[]{481, 0, 21.02f},
                new float[]{505, 0, 21.98f},
                new float[]{506, 0, 22.02f},
                new float[]{511, 0, 22.22f},
//            Line 16
                new float[]{0, 16, 1.28f},
                new float[]{5, 16, 1.48f},
                new float[]{6, 16, 1.52f},
                new float[]{30, 16, 2.48f},
                new float[]{31, 16, 2.52f},
                new float[]{55, 16, 3.48f},
                new float[]{56, 16, 3.52f},
                new float[]{80, 16, 4.48f},
                new float[]{81, 16, 4.52f},
                new float[]{105, 16, 5.48f},
                new float[]{106, 16, 5.52f},
                new float[]{130, 16, 6.48f},
                new float[]{131, 16, 6.52f},
                new float[]{155, 16, 7.48f},
                new float[]{156, 16, 7.52f},
                new float[]{180, 16, 8.48f},
                new float[]{181, 16, 8.52f},
                new float[]{205, 16, 9.48f},
                new float[]{206, 16, 9.52f},
                new float[]{230, 16, 10.48f},
                new float[]{231, 16, 10.52f},
                new float[]{255, 16, 11.48f},
                new float[]{256, 16, 11.52f},
                new float[]{280, 16, 12.48f},
                new float[]{281, 16, 12.52f},
                new float[]{305, 16, 13.48f},
                new float[]{306, 16, 13.52f},
                new float[]{330, 16, 14.48f},
                new float[]{331, 16, 14.52f},
                new float[]{355, 16, 15.48f},
                new float[]{356, 16, 15.52f},
                new float[]{380, 16, 16.48f},
                new float[]{381, 16, 16.52f},
                new float[]{405, 16, 17.48f},
                new float[]{406, 16, 17.52f},
                new float[]{430, 16, 18.48f},
                new float[]{431, 16, 18.52f},
                new float[]{455, 16, 19.48f},
                new float[]{456, 16, 19.52f},
                new float[]{480, 16, 20.48f},
                new float[]{481, 16, 20.52f},
                new float[]{505, 16, 21.48f},
                new float[]{506, 16, 21.52f},
                new float[]{511, 16, 21.72f},
//            Line 32
                new float[]{0, 32, 0.78f},
                new float[]{5, 32, 0.98f},
                new float[]{6, 32, 1.02f},
                new float[]{30, 32, 1.98f},
                new float[]{31, 32, 2.02f},
                new float[]{55, 32, 2.98f},
                new float[]{56, 32, 3.02f},
                new float[]{80, 32, 3.98f},
                new float[]{81, 32, 4.02f},
                new float[]{105, 32, 4.98f},
                new float[]{106, 32, 5.02f},
                new float[]{130, 32, 5.98f},
                new float[]{131, 32, 6.02f},
                new float[]{155, 32, 6.98f},
                new float[]{156, 32, 7.02f},
                new float[]{180, 32, 7.98f},
                new float[]{181, 32, 8.02f},
                new float[]{205, 32, 8.98f},
                new float[]{206, 32, 9.02f},
                new float[]{230, 32, 9.98f},
                new float[]{231, 32, 10.02f},
                new float[]{255, 32, 10.98f},
                new float[]{256, 32, 11.02f},
                new float[]{280, 32, 11.98f},
                new float[]{281, 32, 12.02f},
                new float[]{305, 32, 12.98f},
                new float[]{306, 32, 13.02f},
                new float[]{330, 32, 13.98f},
                new float[]{331, 32, 14.02f},
                new float[]{355, 32, 14.98f},
                new float[]{356, 32, 15.02f},
                new float[]{380, 32, 15.98f},
                new float[]{381, 32, 16.02f},
                new float[]{405, 32, 16.98f},
                new float[]{406, 32, 17.02f},
                new float[]{430, 32, 17.98f},
                new float[]{431, 32, 18.02f},
                new float[]{455, 32, 18.98f},
                new float[]{456, 32, 19.02f},
                new float[]{480, 32, 19.98f},
                new float[]{481, 32, 20.02f},
                new float[]{505, 32, 20.98f},
                new float[]{506, 32, 21.02f},
                new float[]{511, 32, 21.22f},
        };

        for (int i = 0; i < valuesForIntAccess.length; i++) {
            float[] intAcces = valuesForIntAccess[i];
            final int x = (int) intAcces[0];
            final int y = (int) intAcces[1];
            final float expected = intAcces[2];
            final float actual = grid.getPixelFloat(x, y);
            assertEquals("loop counter i = " + i, expected, actual, 1e-5f);
        }
    }

    public void testReadAndGetPixelSimilarity() throws IOException {

        TiePointGrid grid;

        grid = new TiePointGrid("offset05",
                                3, 2,
                                0.5f, 0.5f, // Offsets set!
                                1f, 1f,
                                new float[]{
                                        1f, 2f, 3f,
                                        2f, 3f, 4f,
                                });

        float[] rline1 = grid.readPixels(0, 0, 3, 1, (float[]) null, ProgressMonitor.NULL);
        assertEquals(1f, rline1[0], 1e-6f);
        assertEquals(2f, rline1[1], 1e-6f);
        assertEquals(3f, rline1[2], 1e-6f);
        float[] gline1 = grid.getPixels(0, 0, 3, 1, (float[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < gline1.length; i++) {
            assertEquals(rline1[i], gline1[i], 1e-6f);
        }

        float[] rline2 = grid.readPixels(0, 1, 3, 1, (float[]) null, ProgressMonitor.NULL);
        assertEquals(2f, rline2[0], 1e-6f);
        assertEquals(3f, rline2[1], 1e-6f);
        assertEquals(4f, rline2[2], 1e-6f);
        float[] gline2 = grid.getPixels(0, 1, 3, 1, (float[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < gline2.length; i++) {
            assertEquals(rline2[i], gline2[i], 1e-6f);
        }

        grid = new TiePointGrid("offset00",
                                3, 2,
                                0.0f, 0.0f, // Zero now!
                                1f, 1f,
                                new float[]{
                                        1f, 2f, 3f,
                                        2f, 3f, 4f,
                                });

        rline1 = grid.readPixels(0, 0, 3, 1, (float[]) null, ProgressMonitor.NULL);
        assertEquals(2f, rline1[0], 1e-6f);
        assertEquals(3f, rline1[1], 1e-6f);
        assertEquals(4f, rline1[2], 1e-6f);
        gline1 = grid.getPixels(0, 0, 3, 1, (float[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < gline1.length; i++) {
            assertEquals(rline1[i], gline1[i], 1e-6f);
        }

        rline2 = grid.readPixels(0, 1, 3, 1, (float[]) null, ProgressMonitor.NULL);
        assertEquals(3f, rline2[0], 1e-6f);
        assertEquals(4f, rline2[1], 1e-6f);
        assertEquals(5f, rline2[2], 1e-6f);
        gline2 = grid.getPixels(0, 1, 3, 1, (float[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < gline2.length; i++) {
            assertEquals(rline2[i], gline2[i], 1e-6f);
        }
    }


    public static void testDiscontinuity() {
        final TiePointGrid tp0 = new TiePointGrid("tp0", 2, 2, 0, 0, 1, 1);
        assertEquals(TiePointGrid.DISCONT_NONE, tp0.getDiscontinuity());

        final TiePointGrid tp0_auto = new TiePointGrid("tp0", 2, 2, 0, 0, 1, 1);
        tp0_auto.setDiscontinuity(TiePointGrid.DISCONT_AUTO);
        ProductData data = tp0_auto.createCompatibleRasterData();
        data.setElems(new float[]{0, 20, 180, 150});
        tp0_auto.setData(data);
        assertEquals(TiePointGrid.DISCONT_AT_180, tp0_auto.getDiscontinuity());

        final TiePointGrid tp1 = new TiePointGrid("tp1", 2, 2, 0, 0, 1, 1, new float[]{0, 20, 180, 150}, true);
        assertEquals(TiePointGrid.DISCONT_AT_180, tp1.getDiscontinuity());

        final TiePointGrid tp2 = new TiePointGrid("tp2", 2, 2, 0, 0, 1, 1, new float[]{150, 0, -20, 20}, true);
        assertEquals(TiePointGrid.DISCONT_AT_180, tp2.getDiscontinuity());

        final TiePointGrid tp3 = new TiePointGrid("tp3", 2, 2, 0, 0, 1, 1, new float[]{0, 278, 180, 46}, true);
        assertEquals(TiePointGrid.DISCONT_AT_360, tp3.getDiscontinuity());

        final TiePointGrid tp4 = new TiePointGrid("tp4", 2, 2, 0, 0, 1, 1, new float[]{-4, 0, 278, 180}, true);
        assertEquals(TiePointGrid.DISCONT_AT_360, tp4.getDiscontinuity());
    }

    public void testClone() {
        TiePointGrid grid = new TiePointGrid("abc", 2, 2,
                                             0.1, 0.2,
                                             0.3, 0.4,
                                             new float[]{1.2f, 2.3f, 3.4f, 4.5f});
        grid.setDescription("Aha!");
        grid.setDiscontinuity(TiePointGrid.DISCONT_AT_180);

        TiePointGrid gridClone = grid.cloneTiePointGrid();
        assertEquals("abc", gridClone.getName());
        assertEquals("Aha!", gridClone.getDescription());
        assertEquals(TiePointGrid.DISCONT_AT_180, gridClone.getDiscontinuity());
        assertEquals(2, gridClone.getGridWidth());
        assertEquals(2, gridClone.getGridHeight());
        assertEquals(0.1, gridClone.getOffsetX());
        assertEquals(0.2, gridClone.getOffsetY());
        assertEquals(0.3, gridClone.getSubSamplingX());
        assertEquals(0.4, gridClone.getSubSamplingY());
        assertNotNull(gridClone.getData());
        assertEquals(true, gridClone.getData().getElems() instanceof float[]);
        float[] dataClone = (float[]) gridClone.getData().getElems();
        assertEquals(4, dataClone.length);
        assertEquals(1.2f, dataClone[0]);
        assertEquals(2.3f, dataClone[1]);
        assertEquals(3.4f, dataClone[2]);
        assertEquals(4.5f, dataClone[3]);
        assertNotSame(grid.getData().getElems(), dataClone);
    }
}
