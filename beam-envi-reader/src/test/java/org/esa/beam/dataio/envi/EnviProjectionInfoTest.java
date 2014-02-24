package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

import java.util.Arrays;

public class EnviProjectionInfoTest extends TestCase {

    public void testSetGetProjectionNumber() {
        final int number_1 = 33;
        final int number_2 = 27;

        info.setProjectionNumber(number_1);
        assertEquals(number_1, info.getProjectionNumber());

        info.setProjectionNumber(number_2);
        assertEquals(number_2, info.getProjectionNumber());
    }

    public void testSetGetParameter() {
        final double[] parameter = new double[]{1.9, 2.5, 3.1};

        info.setParameter(parameter);
        assertTrue(Arrays.equals(parameter, info.getParameter()));
    }

    public void testSetGetDatum() {
        final String datum_1 = "12.05.1972";
        final String datum_2 = "DerSchlimmeEllipsoid";

        info.setDatum(datum_1);
        assertEquals(datum_1, info.getDatum());

        info.setDatum(datum_2);
        assertEquals(datum_2, info.getDatum());
    }

    public void testSetGetName() {
        final String name_1 = "Ruben";
        final String name_2 = "Ramon";

        info.setName(name_1);
        assertEquals(name_1, info.getName());

        info.setName(name_2);
        assertEquals(name_2, info.getName());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private EnviProjectionInfo info;

    protected void setUp() {
        info = new EnviProjectionInfo();
    }
}
