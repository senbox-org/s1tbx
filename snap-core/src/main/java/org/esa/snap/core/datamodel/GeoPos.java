/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.math.MathUtils;

/**
 * The <code>GeoPos</code> class represents a geographical position measured in longitudes and latitudes.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class GeoPos {

    private static final double MIN_PER_DEG = 60.0;
    private static final double SEC_PER_DEG = MIN_PER_DEG * 60.0;

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
    public GeoPos() {
    }

    /**
     * Constructs a new geo-position with latitude and longitude set to that of the given geo-position.
     *
     * @param geoPos the  geo-position providing the latitude and longitude, must not be <code>null</code>
     */
    public GeoPos(GeoPos geoPos) {
        this(geoPos.lat, geoPos.lon);
    }

    /**
     * Constructs a new geo-position with the given latitude and longitude values.
     *
     * @param lat the geographical latitude in decimal degree, valid range is -90 to +90
     * @param lon the geographical longitude in decimal degree, valid range is -180 to +180
     */
    public GeoPos(double lat, double lon) {
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
    public static boolean areValid(GeoPos[] gepPositions) {
        for (GeoPos geoPos : gepPositions) {
            if (!geoPos.isValid()) {
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
        if (!(obj instanceof GeoPos)) {
            return false;
        }
        GeoPos other = (GeoPos) obj;
        return other.lat == lat && other.lon == lon;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return (int)(Double.doubleToLongBits(lat) + Double.doubleToLongBits(lon));
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
        if (lon < -360f || lon > 360f) {
            lon %= 360f;
        }
        if (lon < -180f) {
            lon += 360f;
        } else if (lon > 180.0f) {
            lon -= 360f;
        }
        return lon;
    }

    /**
     * Returns a string representation of the latitude value.
     *
     * @return a string of the form DDD°[MM'[SS"]] [N|S].
     */
    public String getLatString() {
        return getLatString(lat);
    }

    /**
     * Returns a string representation of the latitude value.
     *
     * @return a string of the form DDD°[MM'[SS"]] [W|E].
     */
    public String getLonString() {
        return getLonString(lon);
    }

    /**
     * Returns a string representation of the given longitude value.
     *
     * @param lat the geographical latitude in decimal degree
     *
     * @return a string of the form DDD°[MM'[SS"]] [N|S].
     */
    public static String getLatString(double lat) {
        if (isLatValid(lat)) {
            return getDegreeString(lat, false);
        } else {
            return "Inv N (" + lat + ")";
        }
    }

    /**
     * Returns a string representation of the given longitude value.
     *
     * @param lon the geographical longitude in decimal degree
     *
     * @return a string of the form DDD°[MM'[SS"]] [W|E].
     */
    public static String getLonString(double lon) {
        if (isLonValid(lon)) {
            return getDegreeString(lon, true);
        } else {
            return "Inv E (" + lon + ")";
        }
    }


    /**
     * Creates a string representation of the given decimal degree value. The string returned has the format
     * DDD°[MM'[SS"]] [N|S|W|E].
     */
    private static String getDegreeString(double value, boolean longitudial) {

        int sign = (value == 0.0F) ? 0 : (value < 0.0F) ? -1 : 1;
        double rest = Math.abs(value);
        int degree = MathUtils.floorInt(rest);
        rest -= degree;
        int minutes = MathUtils.floorInt(MIN_PER_DEG * rest);
        rest -= minutes / MIN_PER_DEG;
        int seconds = (int)Math.round(SEC_PER_DEG * rest);
        rest -= seconds / SEC_PER_DEG;
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
        sb.append("°");
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
        return lat >= -90.0 && lat <= 90.0;
    }

    private static boolean isLonValid(double lon) {
        return !Double.isNaN(lon) && !Double.isInfinite(lon);
    }

}
