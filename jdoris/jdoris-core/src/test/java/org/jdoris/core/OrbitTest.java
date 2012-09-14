package org.jdoris.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OrbitTest {

    static Logger logger = (Logger) LoggerFactory.getLogger(OrbitTest.class.getName());

    private static final File resFile = new File("/d2/unit_test_data/test_cr.res");
//    private static final File resFile = new File("test/test_cr.res");

    private static Orbit orbit_ACTUAL;

    private static final SLCImage slcimage = new SLCImage();

    private static final double[] crGEO_EXPECTED = new double[]{51.9903894167, 4.3896355000, 41.670};
    //    private static final double[] crXYZ_EXPECTED = {3.92428342070434e+06, 3.01243077763538e+05, 5.00217775318444e+06};
    private static final Point crXYZ_EXPECTED = new Point(3.92428342070434e+06, 3.01243077763538e+05, 5.00217775318444e+06);

    private static final Point pixelXYZ_EXPECTED = new Point(3924267.875114853, 301323.1099883879, 5002132.192287684);
    private static final Point pixel_EXPECTED = new Point(3615, 18094);
    private static final Point subPixel_EXPECTED = new Point(3614.52218889146, 18093.3381301418, crGEO_EXPECTED[2]); // for crGEO_EXPECTED

    private static final Point pixelTime_EXPECTED = new Point(0.002864458452552312, 36487.95443126317);

    private static final Point satellitePos_EXPECTED = new Point(4440791.472772267, 685252.6420443446, 5570675.151783929);
    private static final Point satelliteVel_EXPECTED = new Point(5902.30491148681, -1209.304721400825, -4545.640621144265);
    private static final Point satelliteAcc_EXPECTED = new Point(-4.968594175395303, -1.60033695761798, -6.056842169973311);

    private static final Point dSatCr_EXPECTED = crXYZ_EXPECTED.min(satellitePos_EXPECTED);

    // for doppler equations
    double eq1Doppler_EXPECTED = -18565.30647325516;
    double eq2Range_EXPECTED = -6403789.885986328;
    double eq3Ellipsoid_EXPECTED = 1.309374277047581e-05;

    // for radius of orbit and earth
    private double earthRadius_EXPECTED = 6364903.80517952;
    private double orbitRadius_EXPECTED = 7156998.09505499;

    // for resolution
    private double azimuthDelta_EXPECTED = 4.56746375292234;
    private double azimuthResolution_EXPECTED = 5.73506731122275;

    // state vectors
    private static final double[][] stateVectors_EXPECTED =
            {{36475.000, 4363915.965, 700783.482, 5629051.344},
                    {36479.000, 4387741.287, 696016.877, 5611135.366},
                    {36483.000, 4411488.063, 691224.377, 5593121.779},
                    {36487.000, 4435155.866, 686406.111, 5575010.894},
                    {36491.000, 4458744.274, 681562.209, 5556803.024},
                    {36495.000, 4482252.861, 676692.800, 5538498.487}};


    // poly degree
    private static final int poly_degree_EXPECTED = 4;

    // Expected coefficients x(t)
    private static final double[] orbitCoeff_X_EXPECTED =
            {4435155.8664, 59070.440744, -248.11341149, -1.1048176795, 0.00081383788597};

    // Expected coefficients y(t)
    private static final double[] orbitCoeff_Y_EXPECTED =
            {686406.11108, -12077.763886, -80.113368056, 0.33709490661, 0};

    // Expected coefficients z(t)
    private static final double[] orbitCoeff_Z_EXPECTED =
            {5575010.8939, -45398.575317, -303.07773437, 0.82183155403, 0.0056965888796};

    // deltas
    private static final double eps_01 = 2E-01;
    private static final double eps_03 = 1E-03;
    private static final double eps_04 = 1E-04;
    private static final double eps_05 = 1E-05;
    private static final double eps_06 = 1E-06;

    // expected CR values
    private static Point crSAR_EXPECTED = null;

    @BeforeClass
    public static void setUpTestData() throws Exception {

        logger.setLevel(Level.DEBUG);

        slcimage.parseResFile(resFile);

        orbit_ACTUAL = new Orbit();
        orbit_ACTUAL.parseOrbit(resFile);
        orbit_ACTUAL.computeCoefficients(poly_degree_EXPECTED);

    }

    @AfterClass
    public static void destroyTestData() throws Exception {
        System.gc();
    }


    @Test
    public void testGetNumStateVectors() throws Exception {
        Assert.assertEquals(orbit_ACTUAL.getNumStateVectors(), stateVectors_EXPECTED.length);
    }

    @Test
    public void testOrbitStateVectors() throws Exception {

        for (int i = 0; i < orbit_ACTUAL.getNumStateVectors(); i++) {
            Assert.assertEquals(orbit_ACTUAL.getTime()[i], stateVectors_EXPECTED[i][0], eps_06);
            Assert.assertEquals(orbit_ACTUAL.getData_X()[i], stateVectors_EXPECTED[i][1], eps_06);
            Assert.assertEquals(orbit_ACTUAL.getData_Y()[i], stateVectors_EXPECTED[i][2], eps_06);
            Assert.assertEquals(orbit_ACTUAL.getData_Z()[i], stateVectors_EXPECTED[i][3], eps_06);
        }

    }

    @Test
    public void testOrbitInterpolationFlag() throws Exception {
        Assert.assertEquals(true, orbit_ACTUAL.isInterpolated());
    }

    @Test
    public void testGetPolyDegree() throws Exception {
        Assert.assertEquals(poly_degree_EXPECTED, orbit_ACTUAL.getPoly_degree());
    }

    @Test
    public void testOrbitInterpolationCoeffs_X() throws Exception {
        Assert.assertArrayEquals(orbitCoeff_X_EXPECTED, orbit_ACTUAL.getCoeff_X(), eps_04);
    }

    @Test
    public void testOrbitInterpolationCoeffs_Y() throws Exception {
        Assert.assertArrayEquals(orbitCoeff_Y_EXPECTED, orbit_ACTUAL.getCoeff_Y(), eps_04);
    }

    @Test
    public void testOrbitInterpolationCoeffs_Z() throws Exception {
        Assert.assertArrayEquals(orbitCoeff_Z_EXPECTED, orbit_ACTUAL.getCoeff_Z(), eps_04);
    }

    @Test
    public void testGetXYZ() throws Exception {
        Assert.assertArrayEquals(satellitePos_EXPECTED.toArray(), orbit_ACTUAL.getXYZ(pixelTime_EXPECTED.y).toArray(), eps_06);
    }

    @Test
    public void testGetXYZDot() throws Exception {
        Assert.assertArrayEquals(satelliteVel_EXPECTED.toArray(), orbit_ACTUAL.getXYZDot(pixelTime_EXPECTED.y).toArray(), eps_06);
    }

    @Test
    public void testGetXYZDotDot() throws Exception {
        Assert.assertArrayEquals(satelliteAcc_EXPECTED.toArray(), orbit_ACTUAL.getXYZDotDot(pixelTime_EXPECTED.y).toArray(), eps_06);
    }

    @Test
    public void testEq1_Doppler() throws Exception {
        Assert.assertEquals(eq1Doppler_EXPECTED, orbit_ACTUAL.eq1_Doppler(satelliteVel_EXPECTED, dSatCr_EXPECTED), eps_06);
    }

    @Test
    public void testEq2_Range() throws Exception {
        Assert.assertEquals(eq2Range_EXPECTED, orbit_ACTUAL.eq2_Range(dSatCr_EXPECTED, pixelTime_EXPECTED.x), eps_03);
    }

    @Test
    public void testEq3_Ellipsoid() throws Exception {
        Assert.assertEquals(eq3Ellipsoid_EXPECTED, orbit_ACTUAL.eq3_Ellipsoid(crXYZ_EXPECTED, 0), eps_06);
    }

    // test geometrical conversions
    @Test
    public void testXyz2t() throws Exception {
        Point sarTime_ACTUAL = orbit_ACTUAL.xyz2t(pixelXYZ_EXPECTED, slcimage);
        Assert.assertEquals(pixelTime_EXPECTED.x, sarTime_ACTUAL.x, eps_06);
        Assert.assertEquals(pixelTime_EXPECTED.y, sarTime_ACTUAL.y, eps_06);

        System.out.println("sarTime_ACTUAL = " + slcimage.tr2pix(sarTime_ACTUAL.x));

    }

    @Test
    public void testXyz2lp() throws Exception {
        Point sarPosition_ACTUAL = orbit_ACTUAL.xyz2lp(pixelXYZ_EXPECTED, slcimage);

        Assert.assertEquals(pixel_EXPECTED.x, sarPosition_ACTUAL.x, eps_04);
        Assert.assertEquals(pixel_EXPECTED.y, sarPosition_ACTUAL.y, eps_04);
    }

    @Test
    public void testXyz2orb() throws Exception {
        Point satellitePos_ACTUAL = orbit_ACTUAL.xyz2orb(pixelXYZ_EXPECTED, slcimage);
        Assert.assertArrayEquals(satellitePos_EXPECTED.toArray(), satellitePos_ACTUAL.toArray(), eps_06);
    }


    @Test
    public void testLp2xyz() throws Exception {
        Point xyz_ACTUAL = orbit_ACTUAL.lp2xyz(pixel_EXPECTED.y, pixel_EXPECTED.x, slcimage);
        Assert.assertArrayEquals(pixelXYZ_EXPECTED.toArray(), xyz_ACTUAL.toArray(), eps_03);
    }

    @Test
    public void testPointByReference_lp2xyz() throws Exception {
        Point t1 = orbit_ACTUAL.lp2xyz(1, 1, slcimage);
        Point t2 = orbit_ACTUAL.lp2xyz(2, 2, slcimage);
        Assert.assertFalse(t1 == t2);

        Point t3 = orbit_ACTUAL.lp2xyz(new Point(1, 1), slcimage);
        Point t4 = orbit_ACTUAL.lp2xyz(new Point(2, 2), slcimage);
        Assert.assertFalse(t3 == t4);
    }

    @Test
    public void testPointByReference_lph2xyz() throws Exception {
        Point t1 = orbit_ACTUAL.lph2xyz(1, 1, 0, slcimage);
        Point t2 = orbit_ACTUAL.lph2xyz(1, 1, 1, slcimage);
        Assert.assertFalse(t1 == t2);
    }

    @Test
    public void testPointByReference_getXYZ() throws Exception {
        Point t1 = orbit_ACTUAL.getXYZ(1);
        Point t2 = orbit_ACTUAL.getXYZ(2);
        Assert.assertFalse(t1 == t2);
    }

    @Test
    public void testPointByReference_getXYZDot() throws Exception {
        Point t1 = orbit_ACTUAL.getXYZDot(1);
        Point t2 = orbit_ACTUAL.getXYZDot(2);
        Assert.assertFalse(t1 == t2);
    }

    @Test
    public void testPointByReference_getXYZDotDot() throws Exception {
        Point t1 = orbit_ACTUAL.getXYZDotDot(1);
        Point t2 = orbit_ACTUAL.getXYZDotDot(2);
        Assert.assertFalse(t1 == t2);
    }

    @Test
    public void testPointByReference_xyz2t() throws Exception {
        Point t1 = orbit_ACTUAL.xyz2t(crXYZ_EXPECTED, slcimage);
        Point t2 = orbit_ACTUAL.xyz2t(new Point(crXYZ_EXPECTED.x - 1, crXYZ_EXPECTED.y - 1, crXYZ_EXPECTED.z), slcimage);
        Assert.assertFalse(t1 == t2);
    }

    @Test
    public void crLoop_Run1() throws Exception {

        final double[] crGEO_EXPECTED_RADIANS = {crGEO_EXPECTED[0] * Constants.DTOR, crGEO_EXPECTED[1] * Constants.DTOR, crGEO_EXPECTED[2]};
        final Point xyz_ACTUAL = Ellipsoid.ell2xyz(crGEO_EXPECTED_RADIANS);
        final Point time_ACTUAL = orbit_ACTUAL.xyz2t(xyz_ACTUAL, slcimage);
        final double line_ACTUAL = slcimage.ta2line(time_ACTUAL.y);
        final double pixel_ACTUAL = slcimage.tr2pix(time_ACTUAL.x);
        final Point xyz_ACTUAL_2 = orbit_ACTUAL.lph2xyz(line_ACTUAL, pixel_ACTUAL, crGEO_EXPECTED[2], slcimage);

        Assert.assertArrayEquals(xyz_ACTUAL.toArray(), xyz_ACTUAL_2.toArray(), eps_03);

        double[] philamheight_ACTUAL = orbit_ACTUAL.lph2ell(line_ACTUAL, pixel_ACTUAL, crGEO_EXPECTED[2], slcimage);
        Assert.assertEquals(crGEO_EXPECTED_RADIANS[0], philamheight_ACTUAL[0], eps_06);
        Assert.assertEquals(crGEO_EXPECTED_RADIANS[1], philamheight_ACTUAL[1], eps_06);
        Assert.assertEquals(crGEO_EXPECTED_RADIANS[2], philamheight_ACTUAL[2], eps_03);


    }

    @Test
    public void testEll2lp() throws Exception {
        double[] phi_lam_height = new double[]{crGEO_EXPECTED[0] * Constants.DTOR, crGEO_EXPECTED[1] * Constants.DTOR, crGEO_EXPECTED[2]};
        final Point lp_ACTUAL = orbit_ACTUAL.ell2lp(phi_lam_height, slcimage);
        Assert.assertEquals(subPixel_EXPECTED.y,lp_ACTUAL.y,  eps_03);
        Assert.assertEquals(subPixel_EXPECTED.x,lp_ACTUAL.x,  eps_03);
    }

    @Test
    public void testLph2ell() throws Exception {
        final double[] crGEO_ACTUAL = orbit_ACTUAL.lph2ell(subPixel_EXPECTED.y, subPixel_EXPECTED.x, subPixel_EXPECTED.z, slcimage);
        Assert.assertEquals(crGEO_EXPECTED[0], crGEO_ACTUAL[0] * Constants.RTOD, eps_05);
        Assert.assertEquals(crGEO_EXPECTED[1], crGEO_ACTUAL[1] * Constants.RTOD, eps_05);
        Assert.assertEquals(crGEO_EXPECTED[2], crGEO_ACTUAL[2], eps_03);
    }

    @Test
    public void testComputeEarthRadius() throws Exception {
        final double earthRadius_ACTUAL = orbit_ACTUAL.computeEarthRadius(pixel_EXPECTED, slcimage);
        Assert.assertEquals(earthRadius_EXPECTED, earthRadius_ACTUAL, eps_03);
    }

    @Test
    public void testComputeOrbitRadius() throws Exception {
        final double orbitRadius_ACTUAL = orbit_ACTUAL.computeOrbitRadius(pixel_EXPECTED, slcimage);
        Assert.assertEquals(orbitRadius_EXPECTED, orbitRadius_ACTUAL, eps_03);
    }

    @Test
    public void testComputeAzimuthDelta() throws Exception {
        final double azimuthDelta_COMPUTED = orbit_ACTUAL.computeAzimuthDelta(pixel_EXPECTED, slcimage);
        Assert.assertEquals(azimuthDelta_EXPECTED, azimuthDelta_COMPUTED, eps_06);
    }

    @Test
    public void testComputeAzimuthResolution() throws Exception {
        final double azimuthResolution_COMPUTED = orbit_ACTUAL.computeAzimuthResolution(pixel_EXPECTED, slcimage);
        Assert.assertEquals(azimuthResolution_COMPUTED, azimuthResolution_EXPECTED, eps_06);
    }

}

