package org.esa.beam.dataio.chris.internal;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Tests for class {@link SunPositionCalculator}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SunPositionCalculatorTest extends TestCase {

    /**
     * Test calculation of angular position of the Sun.
     * <p/>
     * Expected values are from the <a href="http://www.srrb.noaa.gov/highlights/sunrise/azel.html">NOAA
     * Solar Position Calculator</a>.
     *
     */
    public void testCalculate() {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

        calendar.set(2002, 6, 1, 12, 0, 0);
        SunPosition sunPosition = SunPositionCalculator.calculate(calendar, 55.0, 10.00);
        assertEquals(32.59, sunPosition.getZenithAngle(), 0.02);
        assertEquals(195.57, sunPosition.getAzimuthAngle(), 0.02);

        calendar.set(2004, 8, 1, 14, 0, 0);
        sunPosition = SunPositionCalculator.calculate(calendar, 55.0, 10.00);
        assertEquals(56.69, sunPosition.getZenithAngle(), 0.02);
        assertEquals(229.67, sunPosition.getAzimuthAngle(), 0.02);
    }
}
