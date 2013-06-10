package org.jlinda.core.utils;

import org.esa.beam.framework.datamodel.GeoPos;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.junit.*;


import java.io.File;

/**
 * User: pmar@ppolabs.com
 * Date: 1/31/12
 * Time: 2:14 AM
 */
public class GeoUtilsTest {

    //    private static final File resFile = new File("/d2/delft_cr_asar.res");
    private static final File resFile = new File("/d2/test.processing/delft.cr/test_cr.res");
    private static Orbit orbit_ACTUAL;
    private static int poly_degree_EXPECTED = 3;

    private static final SLCImage slcimage = new SLCImage();
    // deltas
    private static final double eps_01 = 2E-01;
    private static final double eps_03 = 1E-03;
    private static final double eps_04 = 1E-04;
    private static final double eps_06 = 1E-06;

/*
    private static final Window window = new Window(0, 100, 0, 100);
    private static final float[] height = new float[4];

    private static final double lonMin_ACTUAL = 5.70366629695993;
    private static final double lonMax_ACTUAL = 5.73990330852526;
    private static final double latMin_ACTUAL = 52.4675582362898;
    private static final double latMax_ACTUAL = 52.4762412008187;
*/

//    private static final Window window = new Window(156, 1111, 651, 2222);
    private static final Window window = new Window(156, 1111, 651, 2222);
    private static final float[] height = new float[4];

    private static final double latMin_ACTUAL = 52.4644731467372;
    private static final double latMax_ACTUAL = 52.5687935392713;

    private static final double lonMin_ACTUAL = 5.01270839895334;
    private static final double lonMax_ACTUAL = 5.51647029873252;


    @BeforeClass
    public static void setUpTestData() throws Exception {

        slcimage.parseResFile(resFile);

        orbit_ACTUAL = new Orbit();
        orbit_ACTUAL.parseOrbit(resFile);
        orbit_ACTUAL.computeCoefficients(poly_degree_EXPECTED);

    }

    @AfterClass
    public static void destroyTestData() throws Exception {
        System.gc();
    }

    @Ignore
    @Test
    public void testComputeCorners() throws Exception {

        long start = System.currentTimeMillis();
        GeoPos[] corners = GeoUtils.computeCorners(slcimage, orbit_ACTUAL, window, height);
        long stop = System.currentTimeMillis();

        System.out.println("time = " + (double)(stop - start)/1000);

        // latitude
        Assert.assertEquals(corners[0].getLat(), latMax_ACTUAL, eps_04);
        Assert.assertEquals(corners[1].getLat(), latMin_ACTUAL, eps_04);

        // longitude
        Assert.assertEquals(corners[0].getLon(), lonMin_ACTUAL, eps_04);
        Assert.assertEquals(corners[1].getLon(), lonMax_ACTUAL, eps_04);

    }

}
