package org.esa.snap.framework.datamodel;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Created by Sabine on 20.08.2015.
 */
public class RasterLineTimeCodingTest {

    private RasterLineTimeCoding timeCoding;

    @Before
    public void setUp() throws Exception {
        timeCoding = new RasterLineTimeCoding(new double[]{
                    ProductData.UTC.parse("2000-01-01 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2008-07-12 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2003-03-07 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2004-04-04 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("1995-02-02 12:00", "yyyy-MM-dd HH:mm").getMJD()
        }, 20, 5);
    }

    @Test
    public void testGetMJD() throws Exception {
        double mjd;
        final double validX = 12.5;

        mjd = timeCoding.getMJD(new PixelPos(validX, 0.5));
        assertEquals("0.5", "" + mjd);
        assertEquals("01-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 1.5));
        assertEquals("3115.5", "" + mjd);
        assertEquals("12-JUL-2008 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 2.5));
        assertEquals("1161.5", "" + mjd);
        assertEquals("07-MAR-2003 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 3.5));
        assertEquals("1555.5", "" + mjd);
        assertEquals("04-APR-2004 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 4.5));
        assertEquals("-1793.5", "" + mjd);
        assertEquals("02-FEB-1995 12:00:00.000000", new ProductData.UTC(mjd).format());

        final double validY = 2.5;
        final double invalidNegY = -0.1;
        final double invalidPosY = 5.1;
        final double invalidNegX = -0.1;
        final double invalidPosX = 20.1;
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidNegY)));
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidPosY)));
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(invalidNegX, validY)));
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(invalidPosX, validY)));
   }
}