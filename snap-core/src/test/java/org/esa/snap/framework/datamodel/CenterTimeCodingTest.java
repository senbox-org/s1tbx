package org.esa.snap.framework.datamodel;

import static org.junit.Assert.*;

import org.junit.*;

import java.text.ParseException;

/**
 * Created by Sabine on 21.08.2015.
 */
public class CenterTimeCodingTest {

    private double firstMJD;
    private double secondMJD;

    @Before
    public void setUp() throws Exception {
        firstMJD = ProductData.UTC.parse("2003-03-23 12:00", "yyyy-MM-dd HH:mm").getMJD();
        secondMJD = ProductData.UTC.parse("2003-03-25 12:00", "yyyy-MM-dd HH:mm").getMJD();
    }

    @Test
    public void testCenterTime() throws ParseException {
        final CenterTimeCoding centerTimeCoding = new CenterTimeCoding(firstMJD, secondMJD);

        final ProductData.UTC centerUTC = new ProductData.UTC(centerTimeCoding.getMJD(new PixelPos(2, 2)));
        assertEquals("24-MAR-2003 12:00:00.000000", centerUTC.format());
    }

    @Test
    public void testCenterTime_ReverseOrder() throws ParseException {
        final CenterTimeCoding centerTimeCoding = new CenterTimeCoding(secondMJD, firstMJD);

        final ProductData.UTC centerUTC = new ProductData.UTC(centerTimeCoding.getMJD(new PixelPos(2, 2)));
        assertEquals("24-MAR-2003 12:00:00.000000", centerUTC.format());
    }
}