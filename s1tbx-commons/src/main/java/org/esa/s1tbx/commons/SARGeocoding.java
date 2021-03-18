/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.commons;

import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.eo.LocalGeometry;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.util.Maths;

/**
 * Common SAR utilities for Geocoding
 */
public final class SARGeocoding {

    public static final double NonValidZeroDopplerTime = -99999.0;
    public static final double NonValidIncidenceAngle = -99999.0;

    public static final String USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";
    public static final String USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM = "Use local incidence angle from DEM";
    public static final String USE_INCIDENCE_ANGLE_FROM_ELLIPSOID = "Use incidence angle from Ellipsoid";


    public static boolean isNearRangeOnLeft(final TiePointGrid incidenceAngle, final int sourceImageWidth) {
        // for products without incidence angle tpg just assume left facing
        if (incidenceAngle == null) return true;

        final double incidenceAngleToFirstPixel = incidenceAngle.getPixelDouble(0, 0);
        final double incidenceAngleToLastPixel = incidenceAngle.getPixelDouble(sourceImageWidth - 1, 0);
        return (incidenceAngleToFirstPixel <= incidenceAngleToLastPixel);
    }

    /**
     * Compute zero Doppler time for given earth point using bisection method.
     *
     * @param firstLineUTC     The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength       The radar wavelength.
     * @param earthPoint       The earth point in xyz coordinate.
     * @param sensorPosition   Array of sensor positions for all range lines.
     * @param sensorVelocity   Array of sensor velocities for all range lines.
     * @return The zero Doppler time in days if it is found, -1 otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getEarthPointZeroDopplerTime(final double firstLineUTC,
                                                      final double lineTimeInterval, final double wavelength,
                                                      final PosVector earthPoint, final PosVector[] sensorPosition,
                                                      final PosVector[] sensorVelocity) throws OperatorException {

        // binary search is used in finding the zero doppler time
        int lowerBound = 0;
        int upperBound = sensorPosition.length - 1;
        double lowerBoundFreq = getDopplerFrequency(
                earthPoint, sensorPosition[lowerBound], sensorVelocity[lowerBound], wavelength);
        double upperBoundFreq = getDopplerFrequency(
                earthPoint, sensorPosition[upperBound], sensorVelocity[upperBound], wavelength);

        if (Math.abs(lowerBoundFreq) < 1.0) {
            return firstLineUTC + lowerBound * lineTimeInterval;
        } else if (Math.abs(upperBoundFreq) < 1.0) {
            return firstLineUTC + upperBound * lineTimeInterval;
        } else if (lowerBoundFreq * upperBoundFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        // start binary search
        double midFreq;
        while (upperBound - lowerBound > 1) {

            final int mid = (int) ((lowerBound + upperBound) / 2.0);
            midFreq = sensorVelocity[mid].x * (earthPoint.x - sensorPosition[mid].x) +
                    sensorVelocity[mid].y * (earthPoint.y - sensorPosition[mid].y) +
                    sensorVelocity[mid].z * (earthPoint.z - sensorPosition[mid].z);

            if (midFreq * lowerBoundFreq > 0.0) {
                lowerBound = mid;
                lowerBoundFreq = midFreq;
            } else if (midFreq * upperBoundFreq > 0.0) {
                upperBound = mid;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return firstLineUTC + mid * lineTimeInterval;
            }
        }

        final double y0 = lowerBound - lowerBoundFreq * (upperBound - lowerBound) / (upperBoundFreq - lowerBoundFreq);
        return firstLineUTC + y0 * lineTimeInterval;
    }

    public static double getEarthPointZeroDopplerTimeNewton(
            final double lineTimeInterval, final double wavelength,
            final PosVector earthPoint, final OrbitStateVectors orbit) throws OperatorException {

        final int numOrbitVec = orbit.orbitStateVectors.length;

        final double firstVecFreq = getDopplerFrequency(earthPoint, orbit.orbitStateVectors[0], wavelength);
        final double firstVecTime = orbit.orbitStateVectors[0].time_mjd;

        final double lastVecFreq = getDopplerFrequency(earthPoint, orbit.orbitStateVectors[numOrbitVec - 1], wavelength);
        final double lastVecTime = orbit.orbitStateVectors[numOrbitVec - 1].time_mjd;

        if (firstVecFreq == 0.0) {
            return firstVecTime;
        } else if (lastVecFreq == 0.0) {
            return lastVecTime;
        } else if (firstVecFreq * lastVecFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        double oldTime, oldFreq;
        double newTime = (firstVecTime + lastVecTime) / 2.0, oldFreqDel;

        OrbitStateVectors.PositionVelocity pv = orbit.getPositionVelocity(newTime);
        double newFreq = getDopplerFrequency(earthPoint, pv.position, pv.velocity, wavelength);

        double d;
        int numIter = 0;
        while (Math.abs(newFreq) > 0.001 && numIter <= 10) {
            oldTime = newTime;
            oldFreq = newFreq;

            pv = orbit.getPositionVelocity(oldTime + lineTimeInterval);
            oldFreqDel = getDopplerFrequency(earthPoint, pv.position, pv.velocity, wavelength);

            d = (oldFreqDel - oldFreq) / lineTimeInterval;

            newTime = oldTime - oldFreq / d;
            if (newTime < 0.0) {
                newTime = 0.0;
            } else if (newTime > lastVecTime) {
                newTime = lastVecTime;
            }

            pv = orbit.getPositionVelocity(newTime);
            newFreq = getDopplerFrequency(earthPoint, pv.position, pv.velocity, wavelength);
            numIter++;
        }

        return newTime;
    }

    /**
     * Compute zero Doppler time for given point with the product orbit state vectors using bisection method.
     *
     * @param lineTimeInterval The line time interval.
     * @param wavelength       The radar wavelength.
     * @param earthPoint       The earth point in xyz coordinate.
     * @param orbit            The object holding orbit state vectors.
     * @return The zero Doppler time in days if it is found, NonValidZeroDopplerTime otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getZeroDopplerTime(final double lineTimeInterval,
                                            final double wavelength, final PosVector earthPoint,
                                            final OrbitStateVectors orbit) throws OperatorException {

        // loop through all orbit state vectors to find the adjacent two vectors
        final int numOrbitVec = orbit.orbitStateVectors.length;
        double firstVecTime = 0.0;
        double secondVecTime = 0.0;
        double firstVecFreq = 0.0;
        double secondVecFreq = 0.0;

        for (int i = 0; i < numOrbitVec; i++) {
            final OrbitStateVector orb = orbit.orbitStateVectors[i];

            final double currentFreq = getDopplerFrequency(earthPoint, orb, wavelength);
            if (i == 0 || firstVecFreq * currentFreq > 0) {
                firstVecTime = orb.time_mjd;
                firstVecFreq = currentFreq;
            } else {
                secondVecTime = orb.time_mjd;
                secondVecFreq = currentFreq;
                break;
            }
        }

        if (firstVecFreq * secondVecFreq >= 0.0) {
            return NonValidZeroDopplerTime;
        }

        // find the exact time using Doppler frequency and bisection method
        double lowerBoundTime = firstVecTime;
        double upperBoundTime = secondVecTime;
        double lowerBoundFreq = firstVecFreq;
        double upperBoundFreq = secondVecFreq;
        double midTime, midFreq;
        double diffTime = Math.abs(upperBoundTime - lowerBoundTime);
        final double absLineTimeInterval = Math.abs(lineTimeInterval);

        final int totalIterations = (int)(diffTime/ absLineTimeInterval) + 1;
        int numIterations = 0;
        while (diffTime > absLineTimeInterval && numIterations <= totalIterations) {

            midTime = (upperBoundTime + lowerBoundTime) / 2.0;
            OrbitStateVectors.PositionVelocity pv = orbit.getPositionVelocity(midTime);
            midFreq = getDopplerFrequency(earthPoint, pv.position, pv.velocity, wavelength);

            if (midFreq * lowerBoundFreq > 0.0) {
                lowerBoundTime = midTime;
                lowerBoundFreq = midFreq;
            } else if (midFreq * upperBoundFreq > 0.0) {
                upperBoundTime = midTime;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return midTime;
            }

            diffTime = Math.abs(upperBoundTime - lowerBoundTime);
            numIterations++;
        }

        /*
        midTime = (upperBoundTime + lowerBoundTime) / 2.0;
        orbit.getPositionVelocity(midTime, sensorPosition, sensorVelocity);
        midFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
        final double[] freqArray = {lowerBoundFreq, midFreq, upperBoundFreq};
        final double[][] tmp = {{1, -1, 1}, {1,0,0}, {1,1,1}};
        final Matrix A = new Matrix(tmp);
        final double[] c = Maths.polyFit(A, freqArray);
        double t1 = (-c[1] + Math.sqrt(c[1]*c[1] - 4.0*c[0]*c[2]))/ (2.0*c[2]);
        double t2 = (-c[1] - Math.sqrt(c[1]*c[1] - 4.0*c[0]*c[2]))/ (2.0*c[2]);
        if (t1 >= -1 && t1 <= 1) {
            return 0.5*(1 - t1)*lowerBoundTime + 0.5*(1 + t1)*upperBoundTime;//return t1;
        } else {
            return 0.5*(1 - t2)*lowerBoundTime + 0.5*(1 + t2)*upperBoundTime;//return t2;
        }*/

        return lowerBoundTime - lowerBoundFreq * (upperBoundTime - lowerBoundTime) / (upperBoundFreq - lowerBoundFreq);
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     *
     * @param earthPoint     The earth point in xyz coordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @param wavelength     The radar wavelength.
     * @return The Doppler frequency in Hz.
     */
    private static double getDopplerFrequency(
            final PosVector earthPoint, final PosVector sensorPosition,
            final PosVector sensorVelocity, final double wavelength) {

        final double xDiff = earthPoint.x - sensorPosition.x;
        final double yDiff = earthPoint.y - sensorPosition.y;
        final double zDiff = earthPoint.z - sensorPosition.z;
        final double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

        return 2.0 * (sensorVelocity.x * xDiff + sensorVelocity.y * yDiff + sensorVelocity.z * zDiff) / (distance * wavelength);
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     *
     * @param earthPoint     The earth point in xyz coordinate.
     * @param orbit          OrbitStateVector
     * @param wavelength     The radar wavelength.
     * @return The Doppler frequency in Hz.
     */
    private static double getDopplerFrequency(
            final PosVector earthPoint, final OrbitStateVector orbit, final double wavelength) {

        final double xDiff = earthPoint.x - orbit.x_pos;
        final double yDiff = earthPoint.y - orbit.y_pos;
        final double zDiff = earthPoint.z - orbit.z_pos;
        final double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

        return 2.0 * (orbit.x_vel * xDiff + orbit.y_vel * yDiff + orbit.z_vel * zDiff) / (distance * wavelength);
    }

    /**
     * Compute slant range distance for given earth point and given time.
     *
     * @param time       The given time in days.
     * @param orbit      The orbit.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPos  The sensor position.
     * @return The slant range distance in meters.
     */
    public static double computeSlantRange(
            final double time, final OrbitStateVectors orbit, final PosVector earthPoint, final PosVector sensorPos) {

        orbit.getPosition(time, sensorPos);

        final double xDiff = sensorPos.x - earthPoint.x;
        final double yDiff = sensorPos.y - earthPoint.y;
        final double zDiff = sensorPos.z - earthPoint.z;

        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     *
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange      The slant range in meters.
     * @return The range index.
     */
    public static double computeRangeIndex(
            final boolean srgrFlag, final int sourceImageWidth, final double firstLineUTC, final double lastLineUTC,
            final double rangeSpacing, final double zeroDopplerTime, final double slantRange,
            final double nearEdgeSlantRange, final AbstractMetadata.SRGRCoefficientList[] srgrConvParams) {

        if (zeroDopplerTime < Math.min(firstLineUTC, lastLineUTC) ||
                zeroDopplerTime > Math.max(firstLineUTC, lastLineUTC)) {
            return -1.0;
        }

        if (srgrFlag) { // ground detected image

            double groundRange;

            if (srgrConvParams.length == 1) {
                groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                        srgrConvParams[0].coefficients, srgrConvParams[0].ground_range_origin);
                if (groundRange < 0.0) {
                    return -1.0;
                } else {
                    return (groundRange - srgrConvParams[0].ground_range_origin) / rangeSpacing;
                }
            }

            int idx = 0;
            for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
                idx = i;
            }

            final double[] srgrCoefficients = new double[srgrConvParams[idx].coefficients.length];
            if (idx == srgrConvParams.length - 1) {
                idx--;
            }

            final double mu = (zeroDopplerTime - srgrConvParams[idx].timeMJD) /
                    (srgrConvParams[idx + 1].timeMJD - srgrConvParams[idx].timeMJD);
            for (int i = 0; i < srgrCoefficients.length; i++) {
                srgrCoefficients[i] = Maths.interpolationLinear(srgrConvParams[idx].coefficients[i],
                        srgrConvParams[idx + 1].coefficients[i], mu);
            }
            groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                    srgrCoefficients, srgrConvParams[idx].ground_range_origin);
            if (groundRange < 0.0) {
                return -1.0;
            } else {
                return (groundRange - srgrConvParams[idx].ground_range_origin) / rangeSpacing;
            }

        } else { // slant range image

            return (slantRange - nearEdgeSlantRange) / rangeSpacing;
        }
    }

    public static double computeExtendedRangeIndex(
            final boolean srgrFlag, final int sourceImageWidth, final double firstLineUTC, final double lastLineUTC,
            final double rangeSpacing, final double zeroDopplerTime, final double slantRange,
            final double nearEdgeSlantRange, final AbstractMetadata.SRGRCoefficientList[] srgrConvParams) {

        if (zeroDopplerTime < Math.min(firstLineUTC, lastLineUTC) ||
                zeroDopplerTime > Math.max(firstLineUTC, lastLineUTC)) {
            return -1.0;
        }

        if (srgrFlag) { // ground detected image

            double groundRange;

            if (srgrConvParams.length == 1 || zeroDopplerTime < srgrConvParams[0].timeMJD) {
                groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                        srgrConvParams[0].coefficients, srgrConvParams[0].ground_range_origin);
                if (groundRange < 0.0) {
                    return -1.0;
                } else {
                    return (groundRange - srgrConvParams[0].ground_range_origin) / rangeSpacing;
                }
            }

            if (zeroDopplerTime > srgrConvParams[srgrConvParams.length - 1].timeMJD) {
                groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                        srgrConvParams[srgrConvParams.length - 1].coefficients,
                        srgrConvParams[srgrConvParams.length - 1].ground_range_origin);
                if (groundRange < 0.0) {
                    return -1.0;
                } else {
                    return (groundRange - srgrConvParams[srgrConvParams.length - 1].ground_range_origin) / rangeSpacing;
                }
            }

            int idx = 0;
            for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
                idx = i;
            }

            final double[] srgrCoefficients = new double[srgrConvParams[idx].coefficients.length];
            if (idx == srgrConvParams.length - 1) {
                idx--;
            }

            final double mu = (zeroDopplerTime - srgrConvParams[idx].timeMJD) /
                    (srgrConvParams[idx + 1].timeMJD - srgrConvParams[idx].timeMJD);
            for (int i = 0; i < srgrCoefficients.length; i++) {
                srgrCoefficients[i] = Maths.interpolationLinear(srgrConvParams[idx].coefficients[i],
                        srgrConvParams[idx + 1].coefficients[i], mu);
            }
            groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                    srgrCoefficients, srgrConvParams[idx].ground_range_origin);
            if (groundRange < 0.0) {
                return -1.0;
            } else {
                return (groundRange - srgrConvParams[idx].ground_range_origin) / rangeSpacing;
            }

        } else { // slant range image

            return (slantRange - nearEdgeSlantRange) / rangeSpacing;
        }
    }

    /**
     * Compute ground range for given slant range.
     *
     * @param sourceImageWidth    The source image width.
     * @param rangeSpacing        The range spacing.
     * @param slantRange          The slant range in meters.
     * @param srgrCoeff           The SRGR coefficients for converting ground range to slant range.
     *                            Here it is assumed that the polynomial is given by
     *                            c0 + c1*x + c2*x^2 + ... + cn*x^n, where {c0, c1, ..., cn} are the SRGR coefficients.
     * @param ground_range_origin The ground range origin.
     * @return The ground range in meters.
     */
    public static double computeGroundRange(final int sourceImageWidth, final double rangeSpacing,
                                            final double slantRange, final double[] srgrCoeff,
                                            final double ground_range_origin) {

        // binary search is used in finding the ground range for given slant range
        double lowerBound = ground_range_origin;
        final double lowerBoundSlantRange = Maths.computePolynomialValue(lowerBound, srgrCoeff);
        if (slantRange < lowerBoundSlantRange) {
            return -1.0;
        }

        double upperBound = ground_range_origin + sourceImageWidth * rangeSpacing;
        final double upperBoundSlantRange = Maths.computePolynomialValue(upperBound, srgrCoeff);
        if (slantRange > upperBoundSlantRange) {
            return -1.0;
        }

        // start binary search
        double midSlantRange;
        while (upperBound - lowerBound > 0.0) {

            final double mid = (lowerBound + upperBound) / 2.0;
            midSlantRange = Maths.computePolynomialValue(mid, srgrCoeff);
            if (midSlantRange < slantRange) {
                lowerBound = mid;
            } else if (midSlantRange > slantRange) {
                upperBound = mid;
            } else {
                final double a = midSlantRange - slantRange;
                if ((a > 0 && a < 0.1) || (a <= 0.0D && 0.0D - a < 0.1)) {
                    return mid;
                }
            }
        }

        return -1.0;
    }

    /**
     * Compute projected local incidence angle (in degree).
     *
     * @param lg                               Object holding local geometry information.
     * @param saveLocalIncidenceAngle          Boolean flag indicating saving local incidence angle.
     * @param saveProjectedLocalIncidenceAngle Boolean flag indicating saving projected local incidence angle.
     * @param saveSigmaNought                  Boolean flag indicating applying radiometric calibration.
     * @param x0                               The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0                               The y coordinate of the pixel at the upper left corner of current tile.
     * @param x                                The x coordinate of the current pixel.
     * @param y                                The y coordinate of the current pixel.
     * @param localDEM                         The local DEM.
     * @param localIncidenceAngles             The local incidence angle and projected local incidence angle.
     */
    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final Double demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final double[][] localDEM, final double[] localIncidenceAngles) {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.

        for (int i = 0; i < 3; i++) {
            final int yy = y - y0 + i;
            for (int j = 0; j < 3; j++) {
                if (demNoDataValue.equals(localDEM[yy][x - x0 + j])) {
                    return;
                }
            }
        }

        final int yy = y - y0;
        final int xx = x - x0;
        final double rightPointHeight = (localDEM[yy][xx + 2] +
                localDEM[yy + 1][xx + 2] +
                localDEM[yy + 2][xx + 2]) / 3.0;

        final double leftPointHeight = (localDEM[yy][xx] +
                localDEM[yy + 1][xx] +
                localDEM[yy + 2][xx]) / 3.0;

        final double upPointHeight = (localDEM[yy][xx] +
                localDEM[yy][xx + 1] +
                localDEM[yy][xx + 2]) / 3.0;

        final double downPointHeight = (localDEM[yy + 2][xx] +
                localDEM[yy + 2][xx + 1] +
                localDEM[yy + 2][xx + 2]) / 3.0;

        final PosVector rightPoint = new PosVector();
        final PosVector leftPoint = new PosVector();
        final PosVector upPoint = new PosVector();
        final PosVector downPoint = new PosVector();

        GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
        GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
        GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
        GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

        final PosVector a = new PosVector(rightPoint.x - leftPoint.x, rightPoint.y - leftPoint.y, rightPoint.z - leftPoint.z);
        final PosVector b = new PosVector(downPoint.x - upPoint.x, downPoint.y - upPoint.y, downPoint.z - upPoint.z);
        final PosVector c = new PosVector(lg.centrePoint.x, lg.centrePoint.y, lg.centrePoint.z);

        final PosVector n = new PosVector(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x); // ground plane normal

        Maths.normalizeVector(n);
        if (Maths.innerProduct(n, c) < 0) {
            n.x = -n.x;
            n.y = -n.y;
            n.z = -n.z;
        }

        final PosVector s = new PosVector(
                lg.sensorPos.x - lg.centrePoint.x,
                lg.sensorPos.y - lg.centrePoint.y,
                lg.sensorPos.z - lg.centrePoint.z);
        Maths.normalizeVector(s);

        if (saveLocalIncidenceAngle) { // local incidence angle
            final double nsInnerProduct = Maths.innerProduct(n, s);
            localIncidenceAngles[0] = FastMath.acos(nsInnerProduct) * Constants.RTOD;
        }

        if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
            final PosVector m = new PosVector(s.y * c.z - s.z * c.y, s.z * c.x - s.x * c.z, s.x * c.y - s.y * c.x); // range plane normal
            Maths.normalizeVector(m);
            final double mnInnerProduct = Maths.innerProduct(m, n);
            final PosVector n1 = new PosVector(n.x - m.x * mnInnerProduct, n.y - m.y * mnInnerProduct, n.z - m.z * mnInnerProduct);
            Maths.normalizeVector(n1);
            localIncidenceAngles[1] = FastMath.acos(Maths.innerProduct(n1, s)) * Constants.RTOD;
        }
    }

    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final double demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final double[][] localDEM, final double[] localIncidenceAngles,
            final TileGeoreferencing tileGeoRef, ElevationModel dem) throws Exception {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.

        final int yy = y - y0;
        final int xx = x - x0;
        final int maxX = localDEM[0].length - 1;
        final int maxY = localDEM.length - 1;
        final int numN = 3;
        final GeoPos geo = new GeoPos();
        Double alt;

        double rightPointHeight = 0, leftPointHeight = 0, upPointHeight = 0, downPointHeight = 0;

        int cnt = 0;
        for (int n = 0; n < numN; ++n) {
            if (xx + n > maxX) {
                tileGeoRef.getGeoPos(xx + n, yy, geo);
                alt = dem.getElevation(geo);
            } else {
                alt = localDEM[yy][xx + n];
            }
            if (!alt.equals(demNoDataValue)) {
                rightPointHeight += alt;
                ++cnt;
            }
        }
        if (cnt == 0) return;
        rightPointHeight /= (double) cnt;

        cnt = 0;
        for (int n = 0; n < numN; ++n) {
            if (xx - n < 0) {
                tileGeoRef.getGeoPos(xx - n, yy, geo);
                alt = dem.getElevation(geo);
            } else {
                alt = localDEM[yy][xx - n];
            }
            if (!alt.equals(demNoDataValue)) {
                leftPointHeight += alt;
                ++cnt;
            }
        }
        if (cnt == 0) return;
        leftPointHeight /= (double) cnt;

        cnt = 0;
        for (int n = 0; n < numN; ++n) {
            if (yy - n < 0) {
                tileGeoRef.getGeoPos(xx, yy - n, geo);
                alt = dem.getElevation(geo);
            } else {
                alt = localDEM[yy - n][xx];
            }
            if (!alt.equals(demNoDataValue)) {
                upPointHeight += alt;
                ++cnt;
            }
        }
        if (cnt == 0) return;
        upPointHeight /= (double) cnt;

        cnt = 0;
        for (int n = 0; n < numN; ++n) {
            if (yy + n > maxY) {
                tileGeoRef.getGeoPos(xx, yy + n, geo);
                alt = dem.getElevation(geo);
            } else {
                alt = localDEM[yy + n][xx];
            }
            if (!alt.equals(demNoDataValue)) {
                downPointHeight += alt;
                ++cnt;
            }
        }
        if (cnt == 0) return;
        downPointHeight /= (double) cnt;

        final PosVector rightPoint = new PosVector();
        final PosVector leftPoint = new PosVector();
        final PosVector upPoint = new PosVector();
        final PosVector downPoint = new PosVector();
        final PosVector centrePoint = new PosVector();

        GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
        GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
        GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
        GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

        tileGeoRef.getGeoPos(xx, yy, geo);
        final double centerHeight = localDEM[yy][xx];
        GeoUtils.geo2xyzWGS84(geo.getLat(), geo.lon, centerHeight, centrePoint);

        final PosVector a = new PosVector(rightPoint.x - leftPoint.x, rightPoint.y - leftPoint.y, rightPoint.z - leftPoint.z);
        final PosVector b = new PosVector(downPoint.x - upPoint.x, downPoint.y - upPoint.y, downPoint.z - upPoint.z);
        //final PosVector c = new PosVector(lg.centrePoint.x, lg.centrePoint.y, lg.centrePoint.z);
        final PosVector c = new PosVector(centrePoint.x, centrePoint.y, centrePoint.z);

        final PosVector n = new PosVector(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x); // ground plane normal

        Maths.normalizeVector(n);
        if (Maths.innerProduct(n, c) < 0) {
            n.x = -n.x;
            n.y = -n.y;
            n.z = -n.z;
        }

        final PosVector s = new PosVector(
                lg.sensorPos.x - centrePoint.x,
                lg.sensorPos.y - centrePoint.y,
                lg.sensorPos.z - centrePoint.z);
        Maths.normalizeVector(s);

        if (saveLocalIncidenceAngle) { // local incidence angle
            final double nsInnerProduct = Maths.innerProduct(n, s);
            localIncidenceAngles[0] = FastMath.acos(nsInnerProduct) * Constants.RTOD;
        }

        if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
            final PosVector m = new PosVector(s.y * c.z - s.z * c.y, s.z * c.x - s.x * c.z, s.x * c.y - s.y * c.x); // range plane normal
            Maths.normalizeVector(m);
            final double mnInnerProduct = Maths.innerProduct(m, n);
            final PosVector n1 = new PosVector(n.x - m.x * mnInnerProduct, n.y - m.y * mnInnerProduct, n.z - m.z * mnInnerProduct);
            Maths.normalizeVector(n1);
            localIncidenceAngles[1] = FastMath.acos(Maths.innerProduct(n1, s)) * Constants.RTOD;
        }
    }

    /**
     * Get azimuth pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The azimuth pixel spacing.
     * @throws Exception The exception.
     */
    public static double getAzimuthPixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        return AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
    }

    /**
     * Get range pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The range pixel spacing.
     * @throws Exception The exception.
     */
    public static double getRangePixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return rangeSpacing;
        } else {
            return rangeSpacing / FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct));
        }
    }

    /**
     * Compute pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The pixel spacing.
     * @throws Exception The exception.
     */
    public static double getPixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final double azimuthSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return Math.min(rangeSpacing, azimuthSpacing);
        } else {
            return Math.min(rangeSpacing / FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct)), azimuthSpacing);
        }
    }

    /**
     * Get incidence angle at centre range pixel (in radian).
     *
     * @param srcProduct The source product.
     * @return The incidence angle.
     * @throws OperatorException The exceptions.
     */
    private static double getIncidenceAngleAtCentreRangePixel(final Product srcProduct) throws OperatorException {

        final int sourceImageWidth = srcProduct.getSceneRasterWidth();
        final int sourceImageHeight = srcProduct.getSceneRasterHeight();
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
        if (incidenceAngle == null) {
            throw new OperatorException("incidence_angle tie point grid not found in product");
        }
        return incidenceAngle.getPixelDouble(x, y) * Constants.DTOR;
    }

    /**
     * Compute pixel spacing in degrees.
     *
     * @param pixelSpacingInMeter Pixel spacing in meters.
     * @return The pixel spacing in degrees.
     */
    public static double getPixelSpacingInDegree(final double pixelSpacingInMeter) {
        return pixelSpacingInMeter / Constants.semiMajorAxis * Constants.RTOD;
//        return pixelSpacingInMeter / Constants.MeanEarthRadius * Constants.RTOD;
    }

    /**
     * Compute pixel spacing in meters.
     *
     * @param pixelSpacingInDegree Pixel spacing in degrees.
     * @return The pixel spacing in meters.
     */
    public static double getPixelSpacingInMeter(final double pixelSpacingInDegree) {
        return pixelSpacingInDegree * Constants.semiMinorAxis * Constants.DTOR;
//        return pixelSpacingInDegree * Constants.MeanEarthRadius * Constants.DTOR;
    }

    public static boolean isValidCell(final double rangeIndex, final double azimuthIndex,
                                      final double lat, final double lon, final int diffLat,
                                      final GeoCoding geoCoding,
                                      final int srcMaxRange, final int srcMaxAzimuth, final PosVector sensorPos) {

        if (rangeIndex < 0.0 || rangeIndex >= srcMaxRange || azimuthIndex < 0.0 || azimuthIndex >= srcMaxAzimuth) {
            return false;
        }

        // the rest is only needed for very long images such as GM, WSM or assembled slices
        if(diffLat < 5) {
            return true;
        }

        final GeoPos sensorGeoPos = new GeoPos();
        GeoUtils.xyz2geo(sensorPos.toArray(), sensorGeoPos, GeoUtils.EarthModel.WGS84);
        final double delLatMax = Math.abs(lat - sensorGeoPos.lat);
        double delLonMax;
        if (lon < 0 && sensorGeoPos.lon > 0) {
            delLonMax = Math.min(Math.abs(360 + lon - sensorGeoPos.lon), sensorGeoPos.lon - lon);
        } else if (lon > 0 && sensorGeoPos.lon < 0) {
            delLonMax = Math.min(Math.abs(360 + sensorGeoPos.lon - lon), lon - sensorGeoPos.lon);
        } else {
            delLonMax = Math.abs(lon - sensorGeoPos.lon);
        }

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(rangeIndex, azimuthIndex), null);
        final double delLat = Math.abs(lat - geoPos.lat);
        final double srcLon = geoPos.lon;

        double delLon;
        if (lon < 0 && srcLon > 0) {
            delLon = Math.min(Math.abs(360 + lon - srcLon), srcLon - lon);
        } else if (lon > 0 && srcLon < 0) {
            delLon = Math.min(Math.abs(360 + srcLon - lon), lon - srcLon);
        } else {
            delLon = Math.abs(lon - srcLon);
        }

        return (delLat + delLon <= delLatMax + delLonMax);
    }

    public static void addLookDirection(final String name, final MetadataElement lookDirectionListElem, final int index,
                                        final int num, final int sourceImageWidth, final int sourceImageHeight,
                                        final double firstLineUTC, final double lineTimeInterval,
                                        final boolean nearRangeOnLeft, final GeoCoding geoCoding) {

        final MetadataElement lookDirectionElem = new MetadataElement(name + index);

        int xHead, xTail, y;
        if (num == 1) {
            y = sourceImageHeight / 2;
        } else if (num > 1) {
            y = (index - 1) * sourceImageHeight / (num - 1);
        } else {
            throw new OperatorException("Invalid number of look directions");
        }

        final double time = firstLineUTC + y * lineTimeInterval;
        lookDirectionElem.setAttributeUTC("time", new ProductData.UTC(time));

        if (nearRangeOnLeft) {
            xHead = sourceImageWidth - 1;
            xTail = 0;
        } else {
            xHead = 0;
            xTail = sourceImageWidth - 1;
        }

        final GeoPos geoPosHead = geoCoding.getGeoPos(new PixelPos(xHead, y), null);
        final GeoPos geoPosTail = geoCoding.getGeoPos(new PixelPos(xTail, y), null);
        lookDirectionElem.setAttributeDouble("head_lat", geoPosHead.lat);
        lookDirectionElem.setAttributeDouble("head_lon", geoPosHead.lon);
        lookDirectionElem.setAttributeDouble("tail_lat", geoPosTail.lat);
        lookDirectionElem.setAttributeDouble("tail_lon", geoPosTail.lon);
        lookDirectionListElem.addElement(lookDirectionElem);
    }
}
