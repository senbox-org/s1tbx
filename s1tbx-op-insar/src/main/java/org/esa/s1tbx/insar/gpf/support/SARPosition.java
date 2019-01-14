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
package org.esa.s1tbx.insar.gpf.support;

import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.util.Maths;

/**
 * Compute azimuth and range indices in SAR image for a given target point on the Earth's surface.
 */
public class SARPosition {

    private final double firstLineTime;
    private final double lastLineTime; // in days
    private final double lineTimeInterval;
    private final double wavelength;
    private final double rangeSpacing;
    private final int sourceImageWidth;
    private final boolean srgrFlag;
    private final double nearEdgeSlantRange;
    private final boolean nearRangeOnLeft;
    private final OrbitStateVectors orbit;
    private final AbstractMetadata.SRGRCoefficientList[] srgrConvParams;

    private int x0, y0, w, h;

    public static class PositionData {
        public final PosVector earthPoint = new PosVector();
        public final PosVector sensorPos = new PosVector();
        public double azimuthIndex;
        public double rangeIndex;
        public double slantRange;
    }

    public SARPosition(
            double firstLineTime,
            double lastLineTime,
            double lineTimeInterval,
            double wavelength,
            double rangeSpacing,
            int sourceImageWidth,
            boolean srgrFlag,
            double nearEdgeSlantRange,
            boolean nearRangeOnLeft,
            OrbitStateVectors orbit,
            AbstractMetadata.SRGRCoefficientList[] srgrConvParams) {

        this.firstLineTime = firstLineTime;
        this.lastLineTime = lastLineTime;
        this.lineTimeInterval = lineTimeInterval;
        this.wavelength = wavelength;
        this.rangeSpacing = rangeSpacing;
        this.sourceImageWidth = sourceImageWidth;
        this.srgrFlag = srgrFlag;
        this.nearEdgeSlantRange = nearEdgeSlantRange;
        this.nearRangeOnLeft = nearRangeOnLeft;
        this.orbit = orbit;
        this.srgrConvParams = srgrConvParams;

    }

    public void setTileConstraints(int x0, int y0, int w, int h) {
        this.x0 = x0;
        this.y0 = y0;
        this.w = w;
        this.h = h;
    }

    public boolean getPosition(final PositionData data) {

        final double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(
                firstLineTime, lineTimeInterval, wavelength, data.earthPoint,
                orbit.sensorPosition, orbit.sensorVelocity);

        if (zeroDopplerTime == SARGeocoding.NonValidZeroDopplerTime) {
            return false;
        }

        data.slantRange = SARGeocoding.computeSlantRange(
                zeroDopplerTime, orbit, data.earthPoint, data.sensorPos);

        data.azimuthIndex = (zeroDopplerTime - firstLineTime) / lineTimeInterval;

        if (data.azimuthIndex < 0 || (h > 0 && !(data.azimuthIndex >= y0 - 1 && data.azimuthIndex <= y0 + h))) {
            return false;
        }

        if (!srgrFlag) {
            data.rangeIndex = (data.slantRange - nearEdgeSlantRange) / rangeSpacing;
        } else {
            data.rangeIndex = computeRangeIndex(zeroDopplerTime, data.slantRange);
        }

        if (!nearRangeOnLeft) {
            data.rangeIndex = sourceImageWidth - 1 - data.rangeIndex;
        }

        return data.rangeIndex >= 0 || (w > 0 && data.rangeIndex >= x0 - 1 && data.rangeIndex <= x0 + w);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     *
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange      The slant range in meters.
     * @return The range index.
     */
    private double computeRangeIndex(final double zeroDopplerTime, final double slantRange) {

        if (zeroDopplerTime < Math.min(firstLineTime, lastLineTime) ||
                zeroDopplerTime > Math.max(firstLineTime, lastLineTime)) {
            return -1.0;
        }

        if (srgrFlag) { // ground detected image

            double groundRange;

            if (srgrConvParams.length == 1) {
                groundRange = SARGeocoding.computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
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
            groundRange = SARGeocoding.computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
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
}
