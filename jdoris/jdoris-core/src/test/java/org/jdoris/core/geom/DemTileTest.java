package org.jdoris.core.geom;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdoris.core.Orbit;
import org.jdoris.core.SLCImage;
import org.jdoris.core.Window;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.jdoris.core.io.DataReader.readFloatData;

/**
 * User: pmar@ppolabs.com
 * Date: 6/16/11
 * Time: 4:11 PM
 */
public class
        DemTileTest {

    static Logger logger = Logger.getLogger(DemTile.class.getName());
    private static final File masterResFile = new File("/d2/etna_test/demTest/master.res");

    private static final double DELTA_08 = 1e-08;

    static DemTile demTile;

    static SLCImage masterMeta;
    static Orbit masterOrbit;
    static Window masterTileWindow;

    public static Logger initLog() {
        String filePathToLog4JProperties = "log4j.properties";
        Logger logger = Logger.getLogger(TopoPhase.class);
        PropertyConfigurator.configure(filePathToLog4JProperties);
        return logger;
    }

    @Before
    public void setUp() throws Exception {

        initLog();

        double lat0 = 0.68067840827778847;
        double lon0 = 0.24434609527920614;
        int nLatPixels = 3601;
        int nLonPixels = 3601;
        double latitudeDelta = 1.4544410433280261e-05;
        double longitudeDelta = 1.4544410433280261e-05;
        long nodata = -32768;

        // initialize
        demTile = new DemTile(lat0,lon0,nLatPixels,nLonPixels,latitudeDelta,longitudeDelta,nodata);

        // initialize masterMeta
        masterMeta = new SLCImage();
        masterMeta.parseResFile(masterResFile);

        masterOrbit = new Orbit();
        masterOrbit.parseOrbit(masterResFile);
        masterOrbit.computeCoefficients(3);

        masterTileWindow = new Window(10000, 10127, 1500, 2011);

    }

    @Test
    public void testGetDEMCorners() throws Exception {

        demTile.computeGeoCorners(masterMeta, masterOrbit, masterTileWindow);

        double phiMin_EXPECTED = 0.65198531114095126;
        Assert.assertEquals(phiMin_EXPECTED, demTile.phiMin, DELTA_08);

        double phiMax_EXPECTED = 0.65803878531122906;
        Assert.assertEquals(phiMax_EXPECTED, demTile.phiMax, DELTA_08);

        double lambdaMin_EXPECTED = 0.2584835617746124;
        Assert.assertEquals(lambdaMin_EXPECTED, demTile.lambdaMin, DELTA_08);

        double lambdaMax_EXPECTED = 0.26619645946965714;
        Assert.assertEquals(lambdaMax_EXPECTED, demTile.lambdaMax, DELTA_08);


/*
        // TODO: fix this checks!
        int indexPhi0DEM_EXPECTED = 1556;
        Assert.assertEquals(indexPhi0DEM_EXPECTED, demTile.indexPhi0DEM, DELTA_08);

        int indexPhiNDEM_EXPECTED = 1973;
        Assert.assertEquals(indexPhiNDEM_EXPECTED, demTile.indexPhiNDEM, DELTA_08);

        int indexLambda0DEM_EXPECTED = 972;
        Assert.assertEquals(indexLambda0DEM_EXPECTED, demTile.indexLambda0DEM, DELTA_08);

        int indexLambdaNDEM_EXPECTED = 1503;
        Assert.assertEquals(indexLambdaNDEM_EXPECTED, demTile.indexLambdaNDEM, DELTA_08);
*/

    }

    @Test
    public void testDemStats() throws Exception {

        // load test data
        String testDataDir = "/d2/etna_test/demTest/";
        String bufferFileName;

        final int nRows = 418;
        final int nCols = 532;

        bufferFileName = testDataDir + "dem_full_input.r4.swap";
        float[][] demBuffer = readFloatData(bufferFileName, nRows, nCols).toArray2();

        double[][] demBufferDouble = new double[nRows][nCols];
        // convert to double
        for (int i = 0; i < demBuffer.length; i++) {
            for (int j = 0; j < demBuffer[0].length; j++) {
                demBufferDouble[i][j] = (double) demBuffer[i][j];
            }
        }

        Assert.assertFalse(demTile.statsComputed);

        demTile.setData(demBufferDouble);
        demTile.stats();

        Assert.assertTrue(demTile.statsComputed);

        double demMin_EXPECTED = -10;
        Assert.assertEquals(demMin_EXPECTED, demTile.minValue, DELTA_08);

        double demMax_EXPECTED = 2051;
        Assert.assertEquals(demMax_EXPECTED, demTile.maxValue, DELTA_08);

        double demMean_EXPECTED = 237.479116451416;
        Assert.assertEquals(demMean_EXPECTED, demTile.meanValue, DELTA_08);


    }
}