package org.jlinda.core;

import org.junit.*;

import static org.jlinda.core.Constants.DTOR;

public class EllipsoidTest {

    private Ellipsoid inputEll = new Ellipsoid();

    private final static double[] cr_GEO_expected = {51.9903894167,4.3896355000,41.670};
    private final static double[] cr_XYZ_expected = {3.92428342070434e+06, 3.01243077763538e+05, 5.00217775318444e+06};
    private final static double deltaXYZ = 1E-03; // up to mm
    private final static double deltaGEO = 1E-06; // up to msec
    private final static double N_expected = 6391431.77177463;


    @Ignore
    @Test
    public void testShowdata() throws Exception {
        inputEll.showdata();
    }


    @Test
    public void testXyz2ell() throws Exception {

        double[] phi_lambda_height = Ellipsoid.xyz2ell(new Point(cr_XYZ_expected));

        Assert.assertEquals(cr_GEO_expected[0], Math.toDegrees(phi_lambda_height[0]), deltaGEO);
        Assert.assertEquals(cr_GEO_expected[1], Math.toDegrees(phi_lambda_height[1]), deltaGEO);
        Assert.assertEquals(cr_GEO_expected[2], phi_lambda_height[2], deltaXYZ);


    }

    @Test
    public void testEll2xyz() throws Exception {

        Point cr4_XYZ_actual = Ellipsoid.ell2xyz(cr_GEO_expected[0] * DTOR, cr_GEO_expected[1] * DTOR, cr_GEO_expected[2]);

        for (int i = 0; i < cr_XYZ_expected.length; i++) {

            Assert.assertEquals(cr_XYZ_expected[i],cr4_XYZ_actual.toArray()[i], deltaXYZ);

        }

    }

    @Test
    public void testEll2xyz_ARRAYS() throws Exception {

        GeoPoint geoPoint_EXPECTED = new GeoPoint(cr_GEO_expected[0], cr_GEO_expected[1]);

        double[] xyz_ACTUAL = new double[3];
        Ellipsoid.ell2xyz(geoPoint_EXPECTED, cr_GEO_expected[2], xyz_ACTUAL);

        for (int i = 0; i < cr_XYZ_expected.length; i++) {
            Assert.assertEquals(cr_XYZ_expected[i], xyz_ACTUAL[i], deltaXYZ);
        }
    }

    @Test
    public void testXyz2Geo2Xyz() throws Exception {

        // Try to close to loop (xyz) -> (geo) -> (xyz)

        Point cr_XYZ_actual = Ellipsoid.ell2xyz(cr_GEO_expected[0]*DTOR, cr_GEO_expected[1] * DTOR, cr_GEO_expected[2]);

        double[] cr_GEO_actual = Ellipsoid.xyz2ell(cr_XYZ_actual);

        Point cr4_XYZ_actual_2 = Ellipsoid.ell2xyz(cr_GEO_actual);

        for (int i = 0; i < cr_XYZ_actual.toArray().length; i++) {
            Assert.assertEquals(cr_XYZ_actual.toArray()[i],cr4_XYZ_actual_2.toArray()[i], deltaXYZ);
        }

    }

}
