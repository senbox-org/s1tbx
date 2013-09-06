package org.jlinda.core;

/**
 * The <code>GeoPoint</code> class represents a geographical position measured in longitudes and latitudes.
 * <p>Adapted from org.esa.beam.framework.datamodel.GeoPoint(), refactoring to doubles.<p/>
 * @author Norman Fomferra, and adapted by Petar Marinkovic
 * @version $Revision$ $Date$
 */
public class GeoPoint {

    private static final double _MIN_PER_DEG = 60.0D;
    private static final double _SEC_PER_DEG = _MIN_PER_DEG * 60.0D;

    /**
     * The geographical latitude in decimal degree, valid range is -90 to +90.
     */
    public double lat;

    /**
     * The geographical longitude in decimal degree, valid range is -180 to +180.
     */
    public double lon;

    /**
     * Constructs a new geo-position with latitude and longitude set to zero.
     */
    public GeoPoint() {
    }

    /**
     * Constructs a new geo-position with latitude and longitude set to that of the given geo-position.
     *
     * @param geoPoint the  geo-position providing the latitude and longitude, must not be <code>null</code>
     */
    public GeoPoint(GeoPoint geoPoint) {
        this(geoPoint.lat, geoPoint.lon);
    }

    /**
     * Constructs a new geo-position with the given latitude and longitude values.
     *
     * @param lat the geographical latitude in decimal degree, valid range is -90 to +90
     * @param lon the geographical longitude in decimal degree, valid range is -180 to +180
     */
    public GeoPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Gets the latitude value.
     *
     * @return the geographical latitude in decimal degree
     */
    public double getLat() {
        return lat;
    }

    /**
     * Gets the longitude value.
     *
     * @return the geographical longitude in decimal degree
     */
    public double getLon() {
        return lon;
    }

    /**
     * Sets the geographical location of this point.
     *
     * @param lat the geographical latitude in decimal degree, valid range is -90 to +90
     * @param lon the geographical longitude in decimal degree, valid range is -180 to +180
     */
    public void setLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Tests whether or not this geo-position is valid.
     *
     * @return true, if so
     */
    public final boolean isValid() {
        return isLatValid(lat) && isLonValid(lon);
    }

    /**
     * Tests whether or not all given geo-positions are valid.
     *
     * @return true, if so
     */
    public static boolean areValid(GeoPoint[] a) {
        for (int i = 0; i < a.length; i++) {
            if (!a[i].isValid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the lat/lon fields so that {@link #isValid()} will return false.
     */
    public final void setInvalid() {
        lat = Double.NaN;
        lon = Double.NaN;
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     *
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof GeoPoint)) {
            return false;
        }
        GeoPoint other = (GeoPoint) obj;
        return other.lat == lat && other.lon == lon;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return (int) (Double.doubleToLongBits(lat) + Double.doubleToLongBits(lon));
    }

    /**
     * Returns a string representation of the object. In general, the <code>toString</code> method returns a string that
     * "textually represents" this object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[" + getLatString() + "," + getLonString() + "]";
    }

    /**
     * Normalizes this position so that its longitude is in the range -180 to +180 degree.
     */
    public void normalize() {
        lon = normalizeLon(lon);
    }

    /**
     * Normalizes the given longitude so that it is in the range -180 to +180 degree and returns it.
     * Note that -180 will remain as is, although -180 is equivalent to +180 degrees.
     *
     * @param lon the longitude in degree
     *
     * @return the normalized longitude in the range
     */
    public static double normalizeLon(double lon) {
        if (lon < -360d || lon > 360d) {
            lon %= 360d;
        }
        if (lon < -180d) {
            lon += 360d;
        } else if (lon > 180.0d) {
            lon -= 360d;
        }
        return lon;
    }

    /**
     * Returns a string representation of the latitude value.
     *
     * @return a string of the form DDD?[MM'[SS"]] [N|S].
     */
    public String getLatString() {
        return getLatString(lat);
    }

    /**
     * Returns a string representation of the latitude value.
     *
     * @return a string of the form DDD?[MM'[SS"]] [W|E].
     */
    public String getLonString() {
        return getLonString(lon);
    }

    /**
     * Returns a string representation of the given longitude value.
     *
     *
     * @param lat the geographical latitude in decimal degree
     *
     * @return a string of the form DDD?[MM'[SS"]] [N|S].
     */
    public static String getLatString(double lat) {
        if (isLatValid(lat)) {
            return getDegreeString(lat, false);
        } else {
            return "Inv N";
        }
    }

    /**
     * Returns a string representation of the given longitude value.
     *
     *
     *
     *
     * @param lon the geographical longitude in decimal degree
     *
     * @return a string of the form DDD?[MM'[SS"]] [W|E].
     */
    public static String getLonString(double lon) {
        if (isLonValid(lon)) {
            return getDegreeString(lon, true);
        } else {
            return "Inv E";
        }
    }


    /**
     * Creates a string representation of the given decimal degree value. The string returned has the format
     * DDD?[MM'[SS.sssss"]] [N|S|W|E].
     */
    private static String getDegreeString(double value, boolean longitudial) {

        int sign = (value == 0.0D) ? 0 : (value < 0.0D) ? -1 : 1;
        double rest = Math.abs(value);
        int degree = floorInt(rest);
        rest -= degree;
        int minutes = floorInt(_MIN_PER_DEG * rest);
        rest -= minutes / _MIN_PER_DEG;
        double seconds = (_SEC_PER_DEG * rest);
        rest -= seconds / _SEC_PER_DEG;
        if (seconds == 60) {
            seconds = 0;
            minutes++;
            if (minutes == 60) {
                minutes = 0;
                degree++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(degree);
        sb.append('\260');  // degree
        if (minutes != 0 || seconds != 0) {
            if (minutes < 10) {
                sb.append('0');
            }
            sb.append(minutes);
            sb.append('\'');
            if (seconds != 0) {
                if (seconds < 10) {
                    sb.append('0');
                }
                sb.append(seconds);
                sb.append('"');
            }
        }
        if (sign == -1) {
            sb.append(' ');
            if (longitudial) {
                sb.append('W');
            } else {
                sb.append('S');
            }
        } else if (sign == 1) {
            sb.append(' ');
            if (longitudial) {
                sb.append('E');
            } else {
                sb.append('N');
            }
        }

        return sb.toString();
    }


    private static boolean isLatValid(double lat) {
        return lat >= -90d && lat <= 90d;
    }

    private static boolean isLonValid(double lon) {
        return !Double.isNaN(lon) && !Double.isInfinite(lon);
    }

    private static int floorInt(final double value) {
        return (int) Math.floor(value);
    }


}
