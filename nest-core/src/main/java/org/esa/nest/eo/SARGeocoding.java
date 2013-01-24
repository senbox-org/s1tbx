package org.esa.nest.eo;

import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.PosVector;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.util.MathUtils;

/**
 * Common SAR utilities for Geocoding
 */
public class SARGeocoding {

    public static final double NonValidZeroDopplerTime = -99999.0;
    public static final double NonValidIncidenceAngle = -99999.0;

    public static boolean isNearRangeOnLeft(final TiePointGrid incidenceAngle, final int sourceImageWidth) {
        final double incidenceAngleToFirstPixel = incidenceAngle.getPixelDouble(0, 0);
        final double incidenceAngleToLastPixel = incidenceAngle.getPixelDouble(sourceImageWidth-1, 0);
        return (incidenceAngleToFirstPixel < incidenceAngleToLastPixel);
    }

    /**
     * Compute sensor position and velocity for each range line from the orbit state vectors using
     * cubic WARP polynomial.
     * @param orbitStateVectors The orbit state vectors.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param sensorPosition Sensor positions for all range lines.
     * @param sensorVelocity Sensor velocities for all range lines.
     * @param firstLineUTC The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param sourceImageHeight The source image height.
     */
    public static void computeSensorPositionsAndVelocities(AbstractMetadata.OrbitStateVector[] orbitStateVectors,
                                                           double[] timeArray, double[] xPosArray,
                                                           double[] yPosArray, double[] zPosArray,
                                                           double[][] sensorPosition, double[][] sensorVelocity,
                                                           double firstLineUTC, double lineTimeInterval,
                                                           int sourceImageHeight) {

        final int numVectors = orbitStateVectors.length;
        final int numVectorsUsed = timeArray.length;
        final int d = numVectors / numVectorsUsed;

        final double[] xVelArray = new double[numVectorsUsed];
        final double[] yVelArray = new double[numVectorsUsed];
        final double[] zVelArray = new double[numVectorsUsed];

        for (int i = 0; i < numVectorsUsed; i++) {
            timeArray[i] = orbitStateVectors[i*d].time_mjd;
            xPosArray[i] = orbitStateVectors[i*d].x_pos; // m
            yPosArray[i] = orbitStateVectors[i*d].y_pos; // m
            zPosArray[i] = orbitStateVectors[i*d].z_pos; // m
            xVelArray[i] = orbitStateVectors[i*d].x_vel; // m/s
            yVelArray[i] = orbitStateVectors[i*d].y_vel; // m/s
            zVelArray[i] = orbitStateVectors[i*d].z_vel; // m/s
        }

        final PosVector pos = new PosVector();

        // Lagrange polynomial interpolation
        for (int i = 0; i < sourceImageHeight; i++) {
            final double time = firstLineUTC + i*lineTimeInterval; // zero Doppler time (in days) for each range line

            final double[] weight = MathUtils.lagrangeWeight(timeArray, time);
            MathUtils.lagrangeInterpolatingPolynomial(xPosArray, yPosArray, zPosArray, weight, pos);

            sensorPosition[i][0] = pos.x;
            sensorPosition[i][1] = pos.y;
            sensorPosition[i][2] = pos.z;

            MathUtils.lagrangeInterpolatingPolynomial(xVelArray, yVelArray, zVelArray, weight, pos);

            sensorVelocity[i][0] = pos.x;
            sensorVelocity[i][1] = pos.y;
            sensorVelocity[i][2] = pos.z;
        }
    }

    /**
     * Compute zero Doppler time for given erath point.
     * @param firstLineUTC The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength The ragar wavelength.
     * @param earthPoint The earth point in xyz cooordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @return The zero Doppler time in days if it is found, -1 otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getEarthPointZeroDopplerTime(final double firstLineUTC,
                                                      final double lineTimeInterval, final double wavelength,
                                                      final double[] earthPoint, final double[][] sensorPosition,
                                                      final double[][] sensorVelocity) throws OperatorException {

        // binary search is used in finding the zero doppler time
        int lowerBound = 0;
        int upperBound = sensorPosition.length - 1;
        double lowerBoundFreq = getDopplerFrequency(
                lowerBound, earthPoint, sensorPosition, sensorVelocity, wavelength);
        double upperBoundFreq = getDopplerFrequency(
                upperBound, earthPoint, sensorPosition, sensorVelocity, wavelength);

        if (Double.compare(lowerBoundFreq, 0.0) == 0) {
            return firstLineUTC + lowerBound*lineTimeInterval;
        } else if (Double.compare(upperBoundFreq, 0.0) == 0) {
            return firstLineUTC + upperBound*lineTimeInterval;
        } else if (lowerBoundFreq*upperBoundFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        // start binary search
        double midFreq;
        while(upperBound - lowerBound > 1) {

            final int mid = (int)((lowerBound + upperBound)/2.0);
            midFreq = sensorVelocity[mid][0]*(earthPoint[0] - sensorPosition[mid][0]) +
                    sensorVelocity[mid][1]*(earthPoint[1] - sensorPosition[mid][1]) +
                    sensorVelocity[mid][2]*(earthPoint[2] - sensorPosition[mid][2]);

            if (midFreq*lowerBoundFreq > 0.0) {
                lowerBound = mid;
                lowerBoundFreq = midFreq;
            } else if (midFreq*upperBoundFreq > 0.0) {
                upperBound = mid;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return firstLineUTC + mid*lineTimeInterval;
            }
        }

        final double y0 = lowerBound - lowerBoundFreq*(upperBound - lowerBound)/(upperBoundFreq - lowerBoundFreq);
        return firstLineUTC + y0*lineTimeInterval;
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     * @param y The index for given range line.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @param wavelength The ragar wavelength.
     * @return The Doppler frequency in Hz.
     */
    private static double getDopplerFrequency(
            final int y, final double[] earthPoint, final double[][] sensorPosition,
            final double[][] sensorVelocity, final double wavelength) {

        final double xDiff = earthPoint[0] - sensorPosition[y][0];
        final double yDiff = earthPoint[1] - sensorPosition[y][1];
        final double zDiff = earthPoint[2] - sensorPosition[y][2];
        final double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

        return 2.0 * (sensorVelocity[y][0]*xDiff + sensorVelocity[y][1]*yDiff + sensorVelocity[y][2]*zDiff) / (distance*wavelength);
    }

    /**
     * Compute slant range distance for given earth point and given time.
     * @param time The given time in days.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPos The sensor position.
     * @return The slant range distance in meters.
     */
    public static double computeSlantRange(final double time, final double[] timeArray, final double[] xPosArray,
                                           final double[] yPosArray, final double[] zPosArray,
                                           final double[] earthPoint, final double[] sensorPos) {

        final double[] weight = MathUtils.lagrangeWeight(timeArray, time);
        final PosVector pos = new PosVector();
        MathUtils.lagrangeInterpolatingPolynomial(xPosArray, yPosArray, zPosArray, weight, pos);
        sensorPos[0] = pos.x;
        sensorPos[1] = pos.y;
        sensorPos[2] = pos.z;

        final double xDiff = sensorPos[0] - earthPoint[0];
        final double yDiff = sensorPos[1] - earthPoint[1];
        final double zDiff = sensorPos[2] - earthPoint[2];

        return Math.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange The slant range in meters.
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

            double groundRange = 0.0;

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
                    (srgrConvParams[idx+1].timeMJD - srgrConvParams[idx].timeMJD);
            for (int i = 0; i < srgrCoefficients.length; i++) {
                srgrCoefficients[i] = MathUtils.interpolationLinear(srgrConvParams[idx].coefficients[i],
                        srgrConvParams[idx+1].coefficients[i], mu);
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
     * @param sourceImageWidth The source image width.
     * @param rangeSpacing The range spacing.
     * @param slantRange The salnt range in meters.
     * @param srgrCoeff The SRGR coefficients for converting ground range to slant range.
     *                  Here it is assumed that the polinomial is given by
     *                  c0 + c1*x + c2*x^2 + ... + cn*x^n, where {c0, c1, ..., cn} are the SRGR coefficients.
     * @param ground_range_origin The ground range origin.
     * @return The ground range in meters.
     */
    public static double computeGroundRange(final int sourceImageWidth, final double rangeSpacing,
                                            final double slantRange, final double[] srgrCoeff,
                                            final double ground_range_origin) {

        // binary search is used in finding the ground range for given slant range
        double lowerBound = ground_range_origin;
        double upperBound = ground_range_origin + sourceImageWidth*rangeSpacing;
        final double lowerBoundSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(lowerBound, srgrCoeff);
        final double upperBoundSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(upperBound, srgrCoeff);

        if (slantRange < lowerBoundSlantRange || slantRange > upperBoundSlantRange) {
            return -1.0;
        }

        // start binary search
        double midSlantRange;
        while(upperBound - lowerBound > 0.0) {

            final double mid = (lowerBound + upperBound)/2.0;
            midSlantRange = org.esa.nest.util.MathUtils.computePolynomialValue(mid, srgrCoeff);
            if (Math.abs(midSlantRange - slantRange) < 0.1) {
                return mid;
            } else if (midSlantRange < slantRange) {
                lowerBound = mid;
            } else if (midSlantRange > slantRange) {
                upperBound = mid;
            }
        }

        return -1.0;
    }

    /**
     * Compute projected local incidence angle (in degree).
     * @param lg Object holding local geometry information.
     * @param saveLocalIncidenceAngle Boolean flag indicating saving local incidence angle.
     * @param saveProjectedLocalIncidenceAngle Boolean flag indicating saving projected local incidence angle.
     * @param saveSigmaNought Boolean flag indicating applying radiometric calibration.
     * @param x0 The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0 The y coordinate of the pixel at the upper left corner of current tile.
     * @param x The x coordinate of the current pixel.
     * @param y The y coordinate of the current pixel.
     * @param localDEM The local DEM.
     * @param localIncidenceAngles The local incidence angle and projected local incidence angle.
     */
    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final float demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final float[][] localDEM, final double[] localIncidenceAngles) {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.

        for (int i = 0; i < 3; i++) {
            final int yy = y-y0+i;
            for (int j = 0; j < 3; j++) {
                if (localDEM[yy][x-x0+j] == demNoDataValue) {
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

        final double[] rightPoint = new double[3];
        final double[] leftPoint = new double[3];
        final double[] upPoint = new double[3];
        final double[] downPoint = new double[3];

        GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
        GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
        GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
        GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

        final double[] a = {rightPoint[0] - leftPoint[0], rightPoint[1] - leftPoint[1], rightPoint[2] - leftPoint[2]};
        final double[] b = {downPoint[0] - upPoint[0], downPoint[1] - upPoint[1], downPoint[2] - upPoint[2]};
        final double[] c = {lg.centrePoint[0], lg.centrePoint[1], lg.centrePoint[2]};

        final double[] n = {a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]}; // ground plane normal
        MathUtils.normalizeVector(n);
        if (MathUtils.innerProduct(n, c) < 0) {
            n[0] = -n[0];
            n[1] = -n[1];
            n[2] = -n[2];
        }

        final double[] s = {lg.sensorPos[0] - lg.centrePoint[0],
                lg.sensorPos[1] - lg.centrePoint[1],
                lg.sensorPos[2] - lg.centrePoint[2]};
        MathUtils.normalizeVector(s);

        final double nsInnerProduct = MathUtils.innerProduct(n, s);

        if (saveLocalIncidenceAngle) { // local incidence angle
            localIncidenceAngles[0] = FastMath.acos(nsInnerProduct) * org.esa.beam.util.math.MathUtils.RTOD;
        }

        if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
            final double[] m = {s[1]*c[2] - s[2]*c[1], s[2]*c[0] - s[0]*c[2], s[0]*c[1] - s[1]*c[0]}; // range plane normal
            MathUtils.normalizeVector(m);
            final double mnInnerProduct = MathUtils.innerProduct(m, n);
            final double[] n1 = {n[0] - m[0]*mnInnerProduct, n[1] - m[1]*mnInnerProduct, n[2] - m[2]*mnInnerProduct};
            MathUtils.normalizeVector(n1);
            localIncidenceAngles[1] = FastMath.acos(MathUtils.innerProduct(n1, s)) * org.esa.beam.util.math.MathUtils.RTOD;
        }
    }

    /**
     * Get azimuth pixel spacing (in m).
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
            return rangeSpacing/FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct));
        }
    }

    /**
     * Compute pixel spacing (in m).
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
            return Math.min(rangeSpacing/FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct)), azimuthSpacing);
        }
    }

    /**
     * Get incidence angle at centre range pixel (in radian).
     * @param srcProduct The source product.
     * @throws OperatorException The exceptions.
     * @return The incidence angle.
     */
    private static double getIncidenceAngleAtCentreRangePixel(final Product srcProduct) throws OperatorException {

        final int sourceImageWidth = srcProduct.getSceneRasterWidth();
        final int sourceImageHeight = srcProduct.getSceneRasterHeight();
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
        if(incidenceAngle == null) {
            throw new OperatorException("incidence_angle tie point grid not found in product");
        }
        return incidenceAngle.getPixelFloat((float)x, (float)y)*org.esa.beam.util.math.MathUtils.DTOR;
    }

    /**
     * Compute pixel spacing in degrees.
     * @param pixelSpacingInMeter Pixel spacing in meters.
     * @return The pixel spacing in degrees.
     */
    public static double getPixelSpacingInDegree(final double pixelSpacingInMeter) {
        return pixelSpacingInMeter / Constants.semiMajorAxis * org.esa.beam.util.math.MathUtils.RTOD;
//        return pixelSpacingInMeter / Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.RTOD;
    }

    /**
     * Compute pixel spacing in meters.
     * @param pixelSpacingInDegree Pixel spacing in degrees.
     * @return The pixel spacing in meters.
     */
    public static double getPixelSpacingInMeter(final double pixelSpacingInDegree) {
        return pixelSpacingInDegree * Constants.semiMinorAxis * org.esa.beam.util.math.MathUtils.DTOR;
//        return pixelSpacingInDegree * Constants.MeanEarthRadius * org.esa.beam.util.math.MathUtils.DTOR;
    }

    public static boolean isValidCell(final double rangeIndex, final double azimuthIndex,
                                      final double lat, final double lon,
                                      final TiePointGrid latitude, final TiePointGrid longitude,
                                      final int srcMaxRange, final int srcMaxAzimuth, final double[] sensorPos) {

        if (rangeIndex < 0.0 || rangeIndex >= srcMaxRange || azimuthIndex < 0.0 || azimuthIndex >= srcMaxAzimuth) {
            return  false;
        }

        final GeoPos sensorGeoPos = new GeoPos();
        GeoUtils.xyz2geo(sensorPos, sensorGeoPos, GeoUtils.EarthModel.WGS84);
        final double delLatMax = Math.abs(lat - sensorGeoPos.lat);
        double delLonMax;
        if (lon < 0 && sensorGeoPos.lon > 0) {
            delLonMax = Math.min(Math.abs(360 + lon - sensorGeoPos.lon), sensorGeoPos.lon - lon);
        } else if (lon > 0 && sensorGeoPos.lon < 0) {
            delLonMax = Math.min(Math.abs(360 + sensorGeoPos.lon - lon), lon - sensorGeoPos.lon);
        } else {
            delLonMax = Math.abs(lon - sensorGeoPos.lon);
        }

        final double delLat = Math.abs(lat - latitude.getPixelFloat((float)rangeIndex, (float)azimuthIndex));
        final double srcLon = longitude.getPixelFloat((float)rangeIndex, (float)azimuthIndex);
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
                                        final boolean nearRangeOnLeft, final TiePointGrid latitude,
                                        final TiePointGrid longitude) {

        final MetadataElement lookDirectionElem = new MetadataElement(name+index);

        int xHead, xTail, y;
        if (num == 1) {
            y = sourceImageHeight/2;
        } else if (num > 1) {
            y = (index - 1)*sourceImageHeight / (num - 1);
        } else {
            throw new OperatorException("Invalid number of look directions");
        }

        final double time = firstLineUTC + y*lineTimeInterval;
        lookDirectionElem.setAttributeUTC("time", new ProductData.UTC(time));

        if (nearRangeOnLeft) {
            xHead = sourceImageWidth - 1;
            xTail = 0;
        } else {
            xHead = 0;
            xTail = sourceImageWidth - 1;
        }
        lookDirectionElem.setAttributeDouble("head_lat", latitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("head_lon", longitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("tail_lat", latitude.getPixelDouble(xTail, y));
        lookDirectionElem.setAttributeDouble("tail_lon", longitude.getPixelDouble(xTail, y));
        lookDirectionListElem.addElement(lookDirectionElem);
    }
}
