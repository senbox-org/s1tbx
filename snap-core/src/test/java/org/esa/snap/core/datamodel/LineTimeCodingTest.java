package org.esa.snap.core.datamodel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Sabine on 20.08.2015.
 */
public class LineTimeCodingTest {

    @Test
    public void testGetMJD_Scattered() throws Exception {
        final LineTimeCoding timeCoding = new LineTimeCoding(new double[]{
                    ProductData.UTC.parse("2000-01-01 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2008-07-12 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2003-03-07 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2004-04-04 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("1995-02-02 12:00", "yyyy-MM-dd HH:mm").getMJD()
        });

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

        final double invalidNegY = 0.0 - 0.000000000000001;
        final double invalidPosY = 5.0 + 0.000000000000001;
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidNegY)));
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidPosY)));
    }

    @Test
    public void testGetMJD_Continuous() throws Exception {
        final LineTimeCoding timeCoding = new LineTimeCoding(
                    5,
                    ProductData.UTC.parse("2000-01-01 12:00", "yyyy-MM-dd HH:mm").getMJD(),
                    ProductData.UTC.parse("2000-01-05 12:00", "yyyy-MM-dd HH:mm").getMJD()
        );

        double mjd;
        final double validX = 12.5;

        mjd = timeCoding.getMJD(new PixelPos(validX, 0.5));
        assertEquals("0.5", "" + mjd);
        assertEquals("01-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 1.5));
        assertEquals("1.5", "" + mjd);
        assertEquals("02-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 2.5));
        assertEquals("2.5", "" + mjd);
        assertEquals("03-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 3.5));
        assertEquals("3.5", "" + mjd);
        assertEquals("04-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        mjd = timeCoding.getMJD(new PixelPos(validX, 4.5));
        assertEquals("4.5", "" + mjd);
        assertEquals("05-JAN-2000 12:00:00.000000", new ProductData.UTC(mjd).format());

        final double invalidNegY = 0.0 - 0.000000000000001;
        final double invalidPosY = 5.0 + 0.000000000000001;
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidNegY)));
        assertEquals("NaN", "" + timeCoding.getMJD(new PixelPos(validX, invalidPosY)));
    }
}