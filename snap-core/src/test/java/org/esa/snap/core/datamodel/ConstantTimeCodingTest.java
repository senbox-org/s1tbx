package org.esa.snap.core.datamodel;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * Created by Sabine on 21.08.2015.
 */
public class ConstantTimeCodingTest {

    private double constantTime;

    @Before
    public void setUp() throws Exception {
        constantTime = ProductData.UTC.parse("2003-03-24 12:00", "yyyy-MM-dd HH:mm").getMJD();

    }

    @Test
    public void testCenterTime() throws ParseException {
        final ConstantTimeCoding constantTimeCoding = new ConstantTimeCoding(constantTime);

        final ProductData.UTC centerUTC = new ProductData.UTC(constantTimeCoding.getMJD(new PixelPos(2, 2)));
        assertEquals("24-MAR-2003 12:00:00.000000", centerUTC.format());
    }

    @Test
    public void testCenterTime_ReverseOrder() throws ParseException {
        final ConstantTimeCoding constantTimeCoding = new ConstantTimeCoding(constantTime);

        final ProductData.UTC centerUTC = new ProductData.UTC(constantTimeCoding.getMJD(new PixelPos(2, 2)));
        assertEquals("24-MAR-2003 12:00:00.000000", centerUTC.format());
    }

    @Test
    public void testCanGetPixelPos() throws Exception {
        final ConstantTimeCoding constantTimeCoding = new ConstantTimeCoding(constantTime);
        assertEquals(true, constantTimeCoding.canGetPixelPos());
    }

    @Test
    public void testGetPixelPos() throws Exception {
        final ConstantTimeCoding constantTimeCoding = new ConstantTimeCoding(constantTime);

        double mjd = constantTimeCoding.getMJD(new PixelPos(0, 0));
        assertEquals(new PixelPos(0, 0), constantTimeCoding.getPixelPos(mjd, null));
    }
}