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
package org.esa.snap.core.dataop.dem;

import org.esa.snap.core.datamodel.AngularDirection;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Pointing;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.RsMathUtils;

/**
 * An experimental modification of the standard {@link Orthorectifier}.
 * <p><i>IMPORTANT NOTE: This class is not thread save. In order to use it safely, make sure to create a new instance of
 * this class for each orthorectifying thread.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class Orthorectifier2 extends Orthorectifier {

    public static final float DEGREE_EPS = 1.0f / (60.0f * 60.0f);
    public static final float DEGREE_EPS_SQR = DEGREE_EPS * DEGREE_EPS;

    public static final double RE = RsMathUtils.MEAN_EARTH_RADIUS;

    public Orthorectifier2(int sceneRasterWidth,
                           int sceneRasterHeight,
                           Pointing pointing,
                           ElevationModel elevationModel,
                           int maxIterationCount) {
        super(sceneRasterWidth, sceneRasterHeight, pointing, elevationModel, maxIterationCount);
    }

    /**
     * Returns the pixel co-ordinates as x,y for a given geographical position given as lat,lon.
     * <p>
     * Implements the prediction/correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 29, figure 23.
     * <p>
     * Scope of the prediction/correction algorithm is to retrieve the pixel x,y
     * that matches the given lat,lon by the direct location model f(x,y) = lat,lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        getPixelPosImpl(geoPos, pixelPos);
        return pixelPos;
    }

    private boolean getPixelPosImpl(final GeoPos geoPos, PixelPos pixelPos) {
        // Compute first guess pixel coordinate for given earth coordinate using source geo-coding
        pixelPos = getGeoCoding().getPixelPos(geoPos, pixelPos);
        if (!isPixelPosValid(pixelPos)) {
            return false;
        }

        final double phiTrue = MathUtils.DTOR * geoPos.lat;
        final double lamTrue = MathUtils.DTOR * geoPos.lon;

        final double reCosPhi = RE * Math.cos(phiTrue);

        // Compute elevation above true polar earth coordinate on earth ellipsoid (a,b)
        final double hTrue = getElevation(geoPos, pixelPos);

        final AngularDirection vd = new AngularDirection();
        final GeoPos gp = new GeoPos();

        final int maxIterationCount = getMaxIterationCount();
        for (int iter = 1; iter <= maxIterationCount; iter++) {

            // Compute vieving azimuth and zenith from source tie-points
            getPointing().getViewDir(pixelPos, vd);
            double thetaV = MathUtils.DTOR * vd.zenith;
            double phiV = MathUtils.DTOR * vd.azimuth;

            // Compute polar correction assuming flat earth surface and flat, constant elevation surface
            double t = hTrue * Math.tan(thetaV);
            double deltaPhi = t * Math.cos(phiV) / RE;
            double deltaLam = t * Math.sin(phiV) / reCosPhi;

            // Compute first guess of uncorrected polar earth coordinates
            double phi = phiTrue - deltaPhi;
            double lam = lamTrue - deltaLam;

            gp.lat = (float) (MathUtils.RTOD * phi);
            gp.lon = (float) (MathUtils.RTOD * lam);

            // Compute new pixel coordinates of intersection point
            double iOld = pixelPos.x;
            double jOld = pixelPos.y;
            getGeoCoding().getPixelPos(gp, pixelPos);
            double iNew = pixelPos.x;
            double jNew = pixelPos.y;

            // Compute distance between last and current pixel position
            double deltaI = (iNew - iOld);
            double deltaJ = (jNew - jOld);
            double d = deltaI * deltaI + deltaJ * deltaJ;
            if (d < PIXEL_EPS_SQR) {
                return true;
            }
        }

        return false;
    }
}
