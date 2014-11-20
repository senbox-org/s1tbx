package org.jlinda.core.utils;

import org.jlinda.core.Window;
import org.junit.Assert;
import org.junit.Test;

import static org.jlinda.core.io.DataReader.readDoubleData;

public class TriangleUtilsTest {

    @Test
    public void testGridDataLinear() throws Exception {

        // define params
        final double DELTA_06 = 1e-06;
        Window tileWin = new Window(10000, 10127, 1500, 2011);
        int mlL = 1;
        int mlP = 1;
        double r_az_ratio = 5.2487532186594095;
        int offset = 0;
        double NODATA = -32768;

        final int nRows = 418;
        final int nCols = 532;
        double[][] DEMline_buffer;  // dem_line_buffer.r4
        double[][] DEMpixel_buffer; // dem_pixel_buffer.r4
        double[][] input_buffer;    // dem_buffer.r4

        double[][] grd_EXPECTED;    // output_buffer.r4

        // load test data
        String testDataDir = "/d2/etna_test/demTest/";
        String bufferFileName;

        bufferFileName = testDataDir + "dem_line_buffer.r8.swap";
        DEMline_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "dem_pixel_buffer.r8.swap";
        DEMpixel_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "dem_buffer.r8.swap";
        input_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "output_buffer.r8.swap";
        grd_EXPECTED = readDoubleData(bufferFileName, 128, 512).toArray2();

        /* computation */

        /* grid input tile */
        long t0 = System.currentTimeMillis();
        double[][] grd_ACTUAL = TriangleUtils.gridDataLinear(DEMline_buffer, DEMpixel_buffer, input_buffer,
                tileWin, r_az_ratio,
                mlL, mlP,
                NODATA, offset);

        long t1 = System.currentTimeMillis();
        System.out.println("Data set gridded in " + (0.001 * (t1 - t0)) + " sec");


        /* assert result */
        for (int i = 0; i < grd_EXPECTED.length; i++) {
            double[] doubles = grd_EXPECTED[i];
            Assert.assertArrayEquals(doubles, grd_ACTUAL[i], DELTA_06);
        }

    }

}
