/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.PosVector;
import org.esa.nest.eo.Constants;
import org.esa.nest.eo.GeoUtils;
import org.esa.nest.util.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Skew correction for ALOS product
 * Reference: ALOS-PALSAR-FAQ-001, ESRIN Contract No.20700/07/I-OL, IDEAS QC PALSAR Team
 */

@OperatorMetadata(alias="ALOS-Deskewing",
                  category = "Geometry",
                  authors = "Jun Lu, Luis Veci",
                  copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
                  description="Deskewing ALOS product")
public class ALOSDeskewingOp extends Operator {

    public static final String PRODUCT_SUFFIX = "_DS";

    @SourceProduct(alias="source")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames = null;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private double absShift = 0;
    private double fracShift = 0.0;

    private AbstractMetadata.OrbitStateVector[] orbitStateVectors = null;
    private double firstLineTime = 0.0;
    private double lineTimeInterval = 0.0;
    private double lastLineTime = 0.0;
    private AbstractMetadata.DopplerCentroidCoefficientList[] dopplerCentroidCoefficientLists = null;
    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double slantRangeToFirstPixel = 0.0;
    private double radarWaveLength = 0.0;
    //private double[][] sensorPosition = null;
    //private double[][] sensorVelocity = null;
    private double[] timeArray = null;
    private double[] xPosArray = null;
    private double[] yPosArray = null;
    private double[] zPosArray = null;
    private double[] xVelArray = null;
    private double[] yVelArray = null;
    private double[] zVelArray = null;
    //private double[] targetVel = new double[3];

    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();

    private final static double AngularVelocity = getAngularVelocity();

    private boolean useMapreadyShiftOnly = false;
    private boolean useFAQShiftOnly = false;
    private boolean useBoth = true; // Note: Here "both" means that the shift for each pixel has two parts, one is the
                                     // shift computed by using MapReady method, the second part is a constant that is
                                     // the shift computed  for pixel (0,0) using FAQ method.

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (!useMapreadyShiftOnly && !useFAQShiftOnly && !useBoth) {
                throw new OperatorException("No method was selected for shift calculation");
            }

            getMetadata();

            computeSensorPositionsAndVelocities();

            // computeTargetVelocity();

            createTargetProduct();

            computeShift();

            updateTargetProductMetadata();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.equals("ALOS")) {
            throw new OperatorException("The deskewing operator is for ALOS PALSAR products only");
        }

        orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        if (orbitStateVectors == null) {
            throw new OperatorException("Invalid Obit State Vectors");
        } else if (orbitStateVectors.length < 2) {
            throw new OperatorException("Not enough orbit state vectors");
        }

        firstLineTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();

        lastLineTime = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD();

        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day

        dopplerCentroidCoefficientLists = AbstractMetadata.getDopplerCentroidCoefficients(absRoot);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);

        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);

        slantRangeToFirstPixel = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.slant_range_to_first_pixel);

        radarWaveLength = OperatorUtils.getRadarFrequency(absRoot);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, false, false);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        final double fd = getDopplerFrequency(0);

        final stateVector v = getOrbitStateVector(firstLineTime);

        final double vel = Math.sqrt(v.xVel*v.xVel + v.yVel*v.yVel + v.zVel*v.zVel);

        final double newSlantRangeToFirstPixel = FastMath.cos(FastMath.asin(fd*radarWaveLength/(2.0*vel)))*slantRangeToFirstPixel;

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.slant_range_to_first_pixel, newSlantRangeToFirstPixel);
    }

    private void computeSensorPositionsAndVelocities() {

        final int numVectorsUsed = Math.min(orbitStateVectors.length, 5);
        timeArray = new double[numVectorsUsed];
        xPosArray = new double[numVectorsUsed];
        yPosArray = new double[numVectorsUsed];
        zPosArray = new double[numVectorsUsed];
        xVelArray = new double[numVectorsUsed];
        yVelArray = new double[numVectorsUsed];
        zVelArray = new double[numVectorsUsed];
        // sensorPosition = new double[sourceImageHeight][3]; // xPos, yPos, zPos
        // sensorVelocity = new double[sourceImageHeight][3]; // xVel, yVel, zVel

        final int numVectors = orbitStateVectors.length;
        int k;
        for (k = 0; k < numVectors; k++) {
            if (orbitStateVectors[k].time_mjd >= firstLineTime) {
                break;
            }
        }

        final int j0 = Math.max(k-3, 0);
        final int j1 = Math.min(j0 + numVectorsUsed, numVectors);
        for (int j = j0; j < j1; j++) {
            timeArray[j-j0] = orbitStateVectors[j].time_mjd;
            xPosArray[j-j0] = orbitStateVectors[j].x_pos; // m
            yPosArray[j-j0] = orbitStateVectors[j].y_pos; // m
            zPosArray[j-j0] = orbitStateVectors[j].z_pos; // m
            xVelArray[j-j0] = orbitStateVectors[j].x_vel; // m/s
            yVelArray[j-j0] = orbitStateVectors[j].y_vel; // m/s
            zVelArray[j-j0] = orbitStateVectors[j].z_vel; // m/s
        }

        // Lagrange polynomial interpolation
        /*
        for (int i = 0; i < sourceImageHeight; i++) {
            final double time = firstLineTime + i*lineTimeInterval; // zero Doppler time (in days) for each range line
            if (time > lastLineTime) {
                System.out.println();
            }
            sensorPosition[i][0] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, time);
            sensorPosition[i][1] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, time);
            sensorPosition[i][2] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, time);
            sensorVelocity[i][0] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xVelArray, time);
            sensorVelocity[i][1] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yVelArray, time);
            sensorVelocity[i][2] = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zVelArray, time);
        }
        */
    }
    /*
    private void computeTargetVelocity() throws Exception {

        final double angVel = getAngularVelocity();
        final double lat = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.first_near_lat);
        final double lon = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.first_near_long);
        final GeoPos geoPos = new GeoPos((float)lat, (float)lon);
        final double[] targetPos = new double[3];
        GeoUtils.geo2xyz(geoPos, targetPos);

        targetVel[0] = -angVel*targetPos[1];
        targetVel[1] = angVel*targetPos[0];
        targetVel[2] = 0.0;
    }
    */

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw  = targetRectangle.width;
            final int th  = targetRectangle.height;
            final int tyMax = ty0 + th;
            final int txMax = tx0 + tw;
            //System.out.println("x0 = " + tx0 + ", y0 = " + ty0 + ", w = " + tw + ", h = " + th);

            final int maxShift = (int)computeMaxShift(txMax, ty0);

            final Rectangle sourceRectangle = getSourceRectangle(tx0, ty0, tw, th, maxShift);
            final int sx0 = sourceRectangle.x;
            final int sy0 = sourceRectangle.y;
            final int sw  = sourceRectangle.width;
            final int sh  = sourceRectangle.height;
            final int syMax = sy0 + sh;
            final int sxMax = sx0 + sw;

            final Set<Band> keySet = targetTiles.keySet();
            double totalShift;
            for(Band targetBand : keySet) {

                final Tile targetTile = targetTiles.get(targetBand);
                final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName())[0];
                final Tile sourceTile = getSourceTile(sourceProduct.getBand(srcBandName), sourceRectangle);
                final ProductData trgDataBuffer = targetTile.getDataBuffer();
                final ProductData srcDataBuffer = sourceTile.getDataBuffer();
                final TileIndex srcIndex = new TileIndex(sourceTile);

                for (int y = sy0; y < syMax; y++) {
                    srcIndex.calculateStride(y);
                    final stateVector v = getOrbitStateVector(firstLineTime + y*lineTimeInterval);
                    for (int x = sx0; x < sxMax; x++) {

                        if (useMapreadyShiftOnly) {
                            totalShift = FastMath.round(fracShift*x);
                        } else if (useFAQShiftOnly) {
                            totalShift = computeShift(v, x);
                        } else if (useBoth) {
                            double faqShift = computeShift(v, x);
                            double fraction = FastMath.round(fracShift*x);
                            totalShift = faqShift + fraction;
                            //totalShift = absShift + FastMath.round(fracShift*x);
                            //System.out.println(faqShift);
                        } else {
                            throw new OperatorException("No method was selected for shift calculation");
                        }

                        final int newy = y + (int)totalShift;
                        if (newy >= ty0 && newy < tyMax) {
                            final int trgIdx = targetTile.getDataBufferIndex(x, newy);
                            trgDataBuffer.setElemFloatAt(trgIdx, srcDataBuffer.getElemFloatAt(srcIndex.getIndex(x)));
                        }
                    }
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private double computeMaxShift(final int txMax, final int ty0) throws Exception {

        if (useMapreadyShiftOnly) {
            return FastMath.round(txMax*fracShift);
        } else {
            final stateVector v = getOrbitStateVector(firstLineTime + ty0*lineTimeInterval);
            return computeShift(v, txMax) + FastMath.round(txMax*fracShift);
        }
    }

    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th, final int maxShift) {

        final int sx0 = tx0;
        final int sw = tw;

        int sy0, syMax;
        if (maxShift > 0) {
            sy0 = Math.max(ty0 - maxShift, 0);
            syMax = ty0 + th - 1;
        } else if (maxShift < 0) {
            sy0 = ty0;
            syMax = Math.min(ty0 + th - 1 - maxShift, sourceImageHeight - 1);
        } else { // maxShift == 0
            sy0 = ty0;
            syMax = ty0 + th - 1;
        }

        final int sh = syMax - sy0 + 1;
        return new Rectangle(sx0, sy0, sw, sh);
    }

    private double computeShift(final stateVector v, final int x) throws Exception {

        final double slr = slantRangeToFirstPixel + x*rangeSpacing;
        final double fd = getDopplerFrequency(x);
        final double vel = Math.sqrt(v.xVel*v.xVel + v.yVel*v.yVel + v.zVel*v.zVel);
        return slr*fd*radarWaveLength/(2.0*vel*azimuthSpacing);
    }

    /**
     * Compute deskewing shift for pixel at (0,0).
     * @throws Exception The exceptions.
     */
    private void computeShift() throws Exception {

        final stateVector v = getOrbitStateVector(firstLineTime);
        final double slr = slantRangeToFirstPixel + 0*rangeSpacing;
        final double fd = getDopplerFrequency(0);

        // absolute shift
        final double vel = Math.sqrt(v.xVel*v.xVel + v.yVel*v.yVel + v.zVel*v.zVel);
        absShift = FastMath.round(slr*fd*radarWaveLength/(2.0*vel*azimuthSpacing));

        // fractional shift
        final double[] lookYaw = new double[2];
        computeLookYawAngles(v, slr, fd, lookYaw);
        fracShift = FastMath.sin(lookYaw[0])*FastMath.sin(lookYaw[1]);
    }

    /**
     * Get orbit state vector for given time.
     * @param time The given time.
     * @return Orbit state vector.
     */
    private stateVector getOrbitStateVector(final double time) {

        final double[] weight = MathUtils.lagrangeWeight(timeArray, time);

        final PosVector pos = new PosVector();
        MathUtils.lagrangeInterpolatingPolynomial(xPosArray, yPosArray, zPosArray, weight, pos);

        final PosVector vel = new PosVector();
        MathUtils.lagrangeInterpolatingPolynomial(xVelArray, yVelArray, zVelArray, weight, vel);

        return new stateVector(time, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
    }

    /**
     * Compute orbit state vector for given time using interpolation of two given vectors.
     * @param v1 First given vector.
     * @param v2 Second given vector.
     * @param time The given time.
     * @return The interpolated vector.
     */
    private static stateVector vectorInterpolation(final AbstractMetadata.OrbitStateVector v1,
                                            final AbstractMetadata.OrbitStateVector v2,
                                            final double time) {

        final double[] pos1 = {v1.x_pos, v1.y_pos, v1.z_pos};
        final double[] vel1 = {v1.x_vel, v1.y_vel, v1.z_vel};
        final double[] pos2 = {v2.x_pos, v2.y_pos, v2.z_pos};
        final double[] vel2 = {v2.x_vel, v2.y_vel, v2.z_vel};

        final double t = time;
        final double t1 = v1.time.getMJD();
        final double t2 = v2.time.getMJD();
        final double dt = t2 - t1;
        final double alpha = (t - t1)/dt;
        final double alpha2 = alpha*alpha;
        final double alpha3 = alpha2*alpha;

        final double[] pos = new double[3];
        final double[] vel = new double[3];
        double a0, a1, a2, a3;
        for (int i = 0; i < 3; i++) {
            a0 = pos1[i];
            a1 = vel1[i]*dt;
            a2 = -3*pos1[i] + 3*pos2[i] - 2*vel1[i]*dt - vel2[i]*dt;
            a3 = 2*pos1[i] - 2*pos2[i] + vel1[i]*dt + vel2[i]*dt;
            pos[i] = a0 + a1*alpha + a2*alpha2 + a3*alpha3;
            vel[i] = (a1 + 2*a2*alpha + 3*a3*alpha2)/dt;
        }

        return new stateVector(time, pos[0], pos[1], pos[2], vel[0], vel[1], vel[2]);
    }

    private double getDopplerFrequency(final int x) {

        return dopplerCentroidCoefficientLists[0].coefficients[0] +
               dopplerCentroidCoefficientLists[0].coefficients[1]*x +
               dopplerCentroidCoefficientLists[0].coefficients[2]*x*x;
    }

    private void computeLookYawAngles(final stateVector v, final double slant, final double dopp, double[] lookYaw) {

        int iterations = 0, max_iter = 10000;
        double yaw = 0, deltaAz;
        final double[] look = {0};
        double dopGuess, deltaDop, prevDeltaDop = -9999999;
    	final double[] vRel = new double[3];

        final double lambda = radarWaveLength;

        while(true) {

            double relativeVelocity;

	    	boolean succeed = getLook(v, slant, yaw, look);
            if (!succeed) {
		    	break;
		    }

		    dopGuess = getDoppler(v, look[0], yaw, vRel, radarWaveLength);

		    deltaDop = dopp - dopGuess;
		    relativeVelocity = Math.sqrt(vRel[0]*vRel[0] + vRel[1]*vRel[1] + vRel[2]*vRel[2]);
		    deltaAz = deltaDop*(lambda/(2.0*relativeVelocity));
            if (Math.abs(deltaDop + prevDeltaDop) < 0.000001) {
			    deltaAz /= 2.0;
		    }

		    if (Math.abs(deltaAz*slant) < 0.1) {
			    break;
		    }

		    yaw += deltaAz;

		    if (++iterations > max_iter) {
			    break;
            }

		    prevDeltaDop = deltaDop;
	    }
        lookYaw[0] = look[0];
	    lookYaw[1] = yaw;
    }

    private boolean getLook(final stateVector v, final double slant, final double yaw, final double[] plook) {

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if(geoCoding == null) {
            throw new OperatorException("GeoCoding is null");
        }

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        final double earthRadius = computeEarthRadius(geoPos);

	    final double ht = Math.sqrt(v.xPos*v.xPos + v.yPos*v.yPos + v.zPos*v.zPos);

	    double look = FastMath.acos((ht*ht + slant*slant - earthRadius*earthRadius)/(2.0*slant*ht));

	    for (int iter = 0; iter < 100; iter++) {

		    double delta_range = slant - calcRange(v, look, yaw);
		    if (Math.abs(delta_range) < 0.1) {
			    plook[0] = look;
			    return true;
            } else {
                double sininc = (ht/earthRadius)*FastMath.sin(look);
                double taninc = sininc/Math.sqrt(1 - sininc*sininc);
                look += delta_range/(slant*taninc);
            }
	    }

	    return false;
    }

    private static double calcRange(final stateVector v, final double look, final double yaw) {

        final double[][] rM = new double[3][3];
        getRotationMatrix(v, rM);

        final double cosyaw = FastMath.cos(yaw);
        final double x =  FastMath.sin(yaw);
        final double y = -FastMath.sin(look)* cosyaw;
        final double z = -FastMath.cos(look)* cosyaw;

        final double rx = rM[0][0]*x + rM[1][0]*y + rM[2][0]*z;
        final double ry = rM[0][1]*x + rM[1][1]*y + rM[2][1]*z;
        final double rz = rM[0][2]*x + rM[1][2]*y + rM[2][2]*z;

        final double re = GeoUtils.WGS84.a;
        final double rp = re - re/GeoUtils.WGS84.b;
        final double re2 = re*re;
        final double rp2 = rp*rp;
        final double a = (rx*rx + ry*ry)/re2 + rz*rz/rp2;
        final double b = 2.0*((v.xPos*rx + v.yPos*ry)/re2 + v.zPos*rz/rp2);
        final double c = (v.xPos*v.xPos + v.yPos*v.yPos)/re2 + v.zPos*v.zPos/rp2 - 1.0;

        final double d = (b*b - 4.0*a*c);
        if (d < 0) {
            return -1.0;
        }

        final double sqrtD = Math.sqrt(d);
        final double ans1 = (-b + sqrtD)/(2.0*a);
        final double ans2 = (-b - sqrtD)/(2.0*a);

        return Math.min(ans1, ans2);
    }

    private static void getRotationMatrix(final stateVector v, double[][] rM){

        final double[] ax = new double[3];
        final double[] ay = new double[3];
	    final double[] az = {v.xPos, v.yPos, v.zPos};
	    final double[] vl = {v.xVel, v.yVel, v.zVel};

	    MathUtils.normalizeVector(az);
        MathUtils.normalizeVector(vl);

        crossProduct(az, vl, ay);
        crossProduct(ay, az, ax);

        for (int i = 0; i < 3; i++) {
            rM[0][i] = ax[i];
            rM[1][i] = ay[i];
            rM[2][i] = az[i];
        }
    }

    private static void crossProduct(final double[] a, final double[] b, final double[] c) {

        c[0] = a[1]*b[2] - a[2]*b[1];
        c[1] = a[2]*b[0] - a[0]*b[2];
        c[2] = a[0]*b[1] - a[1]*b[0];
    }

    private static double getDoppler(final stateVector v, final double look, final double yaw, final double[] relVel,
                                     final double lambda) {

        final double spx = v.xPos, spy = v.yPos, spz = v.zPos;
        final double svx = v.xVel, svy = v.yVel, svz = v.zVel;

        final double x =  FastMath.sin(yaw);
        final double y = -FastMath.sin(look)*FastMath.cos(yaw);
        final double z = -FastMath.cos(look)*FastMath.cos(yaw);

        final double[][] rM = new double[3][3];
        getRotationMatrix(v, rM);

        double rpx = rM[0][0]*x + rM[1][0]*y + rM[2][0]*z;
        double rpy = rM[0][1]*x + rM[1][1]*y + rM[2][1]*z;
        double rpz = rM[0][2]*x + rM[1][2]*y + rM[2][2]*z;

        final double range = calcRange(v, look, yaw);

        rpx *= range;
        rpy *= range;
        rpz *= range;

        final double tpx = rpx + spx;
        final double tpy = rpy + spy;
        final double tpz = rpz + spz;

        final double tvx = -AngularVelocity*tpy;
        final double tvy = AngularVelocity*tpx;
        final double tvz = 0.0;

        final double rvx = tvx - svx;
        final double rvy = tvy - svy;
        final double rvz = tvz - svz;

        relVel[0] = rvx;
        relVel[1] = rvy;
        relVel[2] = rvz;

        return -2.0/(lambda*range)*(rpx*rvx + rpy*rvy + rpz*rvz);
    }

    private static double getAngularVelocity() {
        final double dayLength = 24.0*60.0*60.0;
        return (366.225/365.225)*2*Math.PI/dayLength;
    }

    /**
     * Compute Earth radius for pixel at (0,0).
     * @param geoPos lat lon position
     * @return The Earth radius.
     */
    private static double computeEarthRadius(final GeoPos geoPos) {

        final double lat = geoPos.lat;
        final double re = Constants.semiMajorAxis;
        final double rp = Constants.semiMinorAxis;
        return (re*rp) / Math.sqrt(rp*rp*FastMath.cos(lat)*FastMath.cos(lat) + re*re*FastMath.sin(lat)*FastMath.sin(lat));
    }

    public static class stateVector {
        public double xPos;
        public double yPos;
        public double zPos;
        public double xVel;
        public double yVel;
        public double zVel;
        public double time;
        public stateVector(final double time, final double xPos, final double yPos, final double zPos,
                           final double xVel, final double yVel, final double zVel) {
            this.time = time;
            this.xPos = xPos;
            this.yPos = yPos;
            this.zPos = zPos;
            this.xVel = xVel;
            this.yVel = yVel;
            this.zVel = zVel;
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ALOSDeskewingOp.class);
            this.setOperatorUI(ALOSDeskewingUI.class);
        }
    }
}
