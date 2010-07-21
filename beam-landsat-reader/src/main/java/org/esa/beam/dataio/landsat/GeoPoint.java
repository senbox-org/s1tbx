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

package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.LandsatConstants.Points;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The class <code>GeoPoint</code> is used to store the data of the geo points
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public class GeoPoint {

    private final Points geoPointID;
    private String geodicLongitude;
    private String geodicLatitude;
    private double easting;
    private double northing;
    private boolean northernHemisphere;
    private int locationY;
    private int locationX;

    /**
     * @param point the ID
     */
    public GeoPoint(final Points point) {
        this.geoPointID = point;
        locationX = -1;
        locationY = -1;
    }

    /**
     * @return easting Value of the geo point
     */
    public final double getEasting() {
        return easting;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setEasting(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                        NumberFormatException,
                                                                                                        IOException {
        this.easting = Float.parseFloat(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size));
    }

    /**
     * @return Latitude value in degree and with hemisphere information
     */
    public final String getGeodicLatitude() {
        return GeoPos.getLatString(latlon2Decimal(geodicLatitude));
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setGeodicLatitude(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                               IOException {
        geodicLatitude = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
        setHemisphere(geodicLatitude);
    }

    /**
     * TODO
     *
     * @return longitude value in degree and with hemisphere information
     */
    public final String getGeodicLongitude() {
        return GeoPos.getLonString(latlon2Decimal(geodicLongitude));
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setGeodicLongitude(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                                IOException {
        geodicLongitude = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    /**
     * @return northingValue
     */
    public final double getNorthing() {
        return northing;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setNorthing(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                         NumberFormatException,
                                                                                                         IOException {
        this.northing = Float.parseFloat(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return unique ID for a GeoPoint
     */
    public final Points getGeoPointID() {
        return geoPointID;
    }

    /**
     * sets the value of the hemisphere saved in the longitude/latitude string
     *
     * @param longlat LANDSAT latitude longitude value
     */
    private void setHemisphere(String longlat) {
        northernHemisphere = longlat.contains("N") || longlat.contains("n");
    }

    /**
     * @return the name of the geo point
     */
    @Override
    public final String toString() {
        return geoPointID.toString();
    }

    /**
     * @return <code>true</code> if the image is located on the northern hemisphere
     *         <code>false</code> if the image is located on the southern hemisphere
     */
    public final boolean isNorthernHemisphere() {
        return northernHemisphere;
    }

    /**
     * @param latlon latitude or longitude LANDSAT TM String
     *
     * @return converted decimal degree value
     *         convert the from lat long format ddmmss.ffff to dd.ff decimal
     *         (dd + mm/60 +ss/3600) to Decimal degrees (dd.ff)
     *         dd = whole degrees, mm = minutes, ss = seconds
     *         dd.ff = dd + mm/60 + ss/3600
     *         Example: 30 degrees 15 minutes 22 seconds = 30 + 15/60 + 22/3600 = 30.2561
     */
    private static float latlon2Decimal(final String latlon) {

        Guardian.assertNotNullOrEmpty("latlon", latlon);
        int signFaktor = 1;
        if (isSouth(latlon) || isWest(latlon)) {
            signFaktor = -1;
        }
        double latlonValue = 0;
        final Pattern p = Pattern.compile(
                "[wWnNsSEe]");  // deletes 'W' 'E' from longitude String and 'N' 'S' from lat String

        final String data[] = p.split(latlon);
        if (data.length == 1) {
            latlonValue = Double.parseDouble(data[0]);
        }
        final int dd = (int) (latlonValue / 10000); //extract degrees
        final int mm = (int) ((latlonValue - (dd * 10000)) / 100); //extract minutes
        final double ssffff = latlonValue - (dd * 10000 + mm * 100); //extract seconds
        return (signFaktor * (dd + (float) mm / LandsatConstants.CONVERT_MINUTE_DEGREE + (float) ssffff / LandsatConstants.CONVERT_SECOND_DEGREE)); //convert to a degree decimal number
    }

    private static boolean isWest(String latlon) {
        return latlon.contains("W") || latlon.contains("w");
    }

    private static boolean isSouth(String latlon) {
        return latlon.contains("S") || latlon.contains("s");
    }

    /**
     * x Value of the corresponding pixel of this GeoPoint
     *
     * @param y the y position
     */
    public void sePixelY(int y) {
        locationY = y;
    }

    /**
     * y Value of the corresponding pixel of this GeoPoint
     *
     * @return the y value of the pixel, -1 if not initalised
     */
    public int getPixelY() {
        return locationY;
    }

    /**
     * x Value of the corresponding pixel of this GeoPoint
     *
     * @param x the x position
     */
    public void setPixelX(int x) {
        locationX = x;

    }

    /**
     * x Value of the corresponding pixel of this GeoPoint
     *
     * @return the x value of the pixel, -1 if not initalised
     */
    public int getPixelX() {
        return locationX;
    }


}
