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

import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The class <code>GeometricData</code> is used to store the data for the
 * geolocation of the data
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class GeometricData {

    private String mapProjection;
    private String ellipsoid;
    private double semiMajorAxis;
    private double semiMinorAxis;
    private int mapZoneNumber;
    private int projectionNumber;
    private double[] projectionParameter;
    private List<GeoPoint> geoPoints;
    private int horizontalOffset;
    private float sunElevationAngles;
    private float sunAzimuthAngles;
    private float lookAngle;

    /**
     * constructs a GeometricData object with all information needed for geolocation
     */
    public GeometricData() {
        geoPoints = new ArrayList<GeoPoint>();
    }

    /**
     * @return cornergeopoints a collection of all geopoints of the image corners
     */

    public final List getImagePoints() {
        return geoPoints;
    }

    /**
     * @param geoPoint adds a corner geopoint to the dynamic array
     */

    public final void addGeoPoint(final GeoPoint geoPoint) {
        Guardian.assertNotNull("geoPoints", geoPoints);
        if (!geoPoints.contains(geoPoint)) {
            geoPoints.add(geoPoint);
        }
    }

    /**
     * @return ellisoid the ellipsoid value of the landsat TM scene
     */
    public final String getEllipsoid() {
        return ellipsoid;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws IOException
     */

    public final void setEllipsoid(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                          IOException {
        ellipsoid = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim();
    }

    /**
     * @return horizontalOffsetValue
     */
    public final int getHorizontalOffset() {
        return horizontalOffset;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setHorizontalOffset(final int offset, final int size, LandsatImageInputStream inputStream)
            throws
            NumberFormatException,
            IOException {
        horizontalOffset = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return mapProjectionValue
     */
    public final String getMapProjection() {
        return mapProjection;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setMapProjection(final int offset, final int size, final LandsatImageInputStream inputStream)
            throws
            IOException {
        mapProjection = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim();
    }

    /**
     * @return mapZoneNumberValue
     */
    public final int getMapZoneNumber() {
        return mapZoneNumber;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setMapZoneNumber(final int offset, final int size, LandsatImageInputStream inputStream)
            throws
            NumberFormatException,
            IOException {
        mapZoneNumber = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return orientation angle in degree
     */
    public final float getLookAngle() {
        return lookAngle;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setLookAngle(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                NumberFormatException,
                                                                                                                IOException {
        lookAngle = Float.parseFloat(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return SemiMajorAxis
     */
    public final double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setSemiMajorAxis(final int offset, final int size, LandsatImageInputStream inputStream)
            throws
            IOException {
        semiMajorAxis = Double.parseDouble(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return semiMinorAxis
     */
    public final double getSemiMinorAxis() {
        return semiMinorAxis;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setSemiMinorAxis(final int offset, final int size, LandsatImageInputStream inputStream)
            throws
            IOException {
        semiMinorAxis = Double.parseDouble(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return sunAzimuthAnglesValue
     */
    public final float getSunAzimuthAngle() {
        return sunAzimuthAngles;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setSunAzimuthAngles(final int offset, final int size, final LandsatImageInputStream inputStream)
            throws
            NumberFormatException,
            IOException {
        sunAzimuthAngles = Float.parseFloat(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return projectionParameter
     */
    public final double[] getProjectionParameter() {
        return projectionParameter;
    }

    /**
     * @param offset      - entry point at the inputstream
     * @param size        - the size of the data
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setProjectionParameter(final int offset, final int size,
                                             final LandsatImageInputStream inputStream)
            throws
            IOException {
        projectionParameter = parseProjectionParameter(offset, size, inputStream);
    }

    /**
     * gets the sun elevation angle of the image in the center of the images
     *
     * @return sunElevation
     */
    public final float getSunElevationAngle() {
        return sunElevationAngles;
    }

    /**
     * @return projectNumberValue
     */
    public final int getProjectionNumber() {
        return projectionNumber;
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setProjectionNumber(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                                 NumberFormatException,
                                                                                                                 IOException {
        projectionNumber = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @param offset      entry point at the inputstream
     * @param size        the size of the data
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setSunElevationAngles(final int offset, final int size,
                                            final LandsatImageInputStream inputStream) throws
                                                                                       NumberFormatException,
                                                                                       IOException {
        sunElevationAngles = Float.parseFloat(
                (LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim()));
    }

    /**
     * Gets the geo-point at the given ID.
     *
     * @param geoPointID Id to identify the desired geo-point
     *
     * @return geoPoint
     */
    public final GeoPoint getGeoPointAt(LandsatConstants.Points geoPointID) {

        for (Iterator iter = geoPoints.iterator(); iter.hasNext();) {
            GeoPoint geoPoint = (GeoPoint) iter.next();
            if (geoPoint.getGeoPointID() == geoPointID) {
                return geoPoint;
            }
        }

        throw new IllegalArgumentException("invalid geoPointID");
    }


    /**
     * parse data value from inputstream
     *
     * @param offset
     * @param size
     * @param inputStream
     *
     * @return double[] parameters projection depended parameters (max. 15 values) reads the projection parameter
     *
     * @throws IOException
     */
    private static double[] parseProjectionParameter(final int offset, final int size,
                                                     LandsatImageInputStream inputStream)
            throws
            IOException {

        List<Double> parameterList = new ArrayList<Double>();
        StringTokenizer token = new StringTokenizer(LandsatUtils
                .getValueFromLandsatFile(inputStream, offset, size));

        double checkValue;
        while (token.hasMoreElements()) {
            checkValue = Double.parseDouble(token.nextToken().substring(0, 16));
            if (checkValue != 0.0) {
                parameterList.add(new Double(checkValue));
            }
        }
        double[] parameter = new double[parameterList.size()];
        for (int i = 0; i < parameterList.size(); i++) {

            Double projpara = parameterList.get(i);
            parameter[i] = projpara.doubleValue();
        }
        return parameter;
    }
}
