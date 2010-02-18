package org.esa.beam.dataio.chris.internal;

import static java.lang.Math.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Class for calculating the angular position of the Sun.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SunPositionCalculator {

    /**
     * Based on formulae developed by Flandern & Pulkkinen (1979, Astrophys. J. Suppl. Ser. 41, 391)
     * and simplified by Paul Schlyter (http://www.stjarnhimlen.se/).
     * <p/>
     * The angular position of the Sun is calculated with an accuracy of 1-2 arcmin.
     *
     * @param calendar the time of observation
     * @param lat      the geographical latitude of the observer (degrees)
     * @param lon      the geographical longitude of the observer (degrees)
     *
     * @return the zenith and azimuth angles of the Sun (degrees)
     */
    public static SunPosition calculate(final Calendar calendar, final double lat, final double lon) {
        final Calendar utc = toUTC(calendar);

        final int year = utc.get(Calendar.YEAR);
        final int month = utc.get(Calendar.MONTH) + 1;
        final int date = utc.get(Calendar.DATE);
        final int hour = utc.get(Calendar.HOUR_OF_DAY);
        final int minute = utc.get(Calendar.MINUTE);
        final int second = utc.get(Calendar.SECOND);
        final double h = hour + minute / 60.0 + second / 3600.0;

        // fractional day, 0.0 corresponds to January 1, 2000 00:00:00 UTC
        final double d = h / 24.0 + (367 * year - 7 * (year + (month + 9) / 12) / 4 + 275 * month / 9 + date - 730530);

        // longitude of perihelion (rad)
        final double w = toRadians(282.9404) + toRadians(4.70935E-5) * d;
        // eccentricity (rad)
        final double e = 0.016709 - 1.151E-9 * d;
        // mean anomaly (rad)
        final double M = toRadians(356.0470) + toRadians(0.9856002585) * d;

        // ecliptical obliquity (rad)
        final double o = toRadians(23.4393) - toRadians(3.563E-7) * d;

        // eccentric anomaly (rad)
        final double E = M + e * sin(M) * (1.0 + e * cos(M));
        // true anomaly (rad)
        final double v = atan2(sqrt(1.0 - e * e) * sin(E), cos(E) - e);
        // true longitude (rad)
        final double l = v + w;

        // Cartesian geocentric coordinates
        final double x = cos(l);
        final double y = sin(l) * cos(o);
        final double z = sin(l) * sin(o);

        // right ascension (rad)
        final double alpha = atan2(y, x);
        // declination (rad)
        final double delta = atan2(z, sqrt(x * x + y * y));
        // local siderial time
        final double lst = M + w + PI + h * toRadians(15.0) + toRadians(lon);
        // local hour angle (rad)
        final double lha = lst - alpha;
        // latitude (rad)
        final double phi = toRadians(lat);
        // altitude angle (rad)
        final double alt = asin(sin(phi) * sin(delta) + cos(phi) * cos(delta) * cos(lha));

        final double xaa = cos(lha) * cos(delta) * sin(phi) - sin(delta) * cos(phi);
        final double yaa = sin(lha) * cos(delta);

        // azimuth angle (rad)
        final double saa = atan2(yaa, xaa) + PI;
        // zenith angle (rad)
        final double sza = PI / 2.0 - alt;

        return new SunPosition(Math.toDegrees(sza), Math.toDegrees(saa));
    }

    private static Calendar toUTC(Calendar calendar) {
        final Calendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(calendar.getTimeInMillis());

        return utc;
    }

}
