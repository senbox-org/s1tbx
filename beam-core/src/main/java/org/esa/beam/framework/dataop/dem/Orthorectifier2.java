/*
 * $Id: Orthorectifier2.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.RsMathUtils;

/**
 * An experimental modification of the standard {@link Orthorectifier}.
 * <p/>
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
     * <p/>
     * Implements the prediction/correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 29, figure 23.
     * <p/>
     * Scope of the prediction/correction algorithm is to retrieve the pixel x,y
     * that matches the given lat,lon by the direct location model f(x,y) = lat,lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
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

        double phiTrue = MathUtils.DTOR * geoPos.lat;
        double lamTrue = MathUtils.DTOR * geoPos.lon;

        final double reCosPhi = RE * Math.cos(phiTrue);

        // Compute elevation above true polar earth coordinate on earth ellipsoid (a,b)
        final double hTrue = getElevation(geoPos, pixelPos);

        final int maxIterationCount = getMaxIterationCount();
        for (int iter = 1; iter <= maxIterationCount; iter++) {

            // Compute vieving azimuth and zenith from source tie-points
            getPointing().getViewDir(pixelPos, _vg);
            double thetaV = MathUtils.DTOR * _vg.zenith;
            double phiV = MathUtils.DTOR * _vg.azimuth;

            // Compute polar correction assuming flat earth surface and flat, constant elevation surface
            double t = hTrue * Math.tan(thetaV);
            double deltaPhi = t * Math.cos(phiV) / RE;
            double deltaLam = t * Math.sin(phiV) / reCosPhi;

            // Compute first guess of uncorrected polar earth coordinates
            double phi = phiTrue - deltaPhi;
            double lam = lamTrue - deltaLam;

            _gp.lat = (float) (MathUtils.RTOD * phi);
            _gp.lon = (float) (MathUtils.RTOD * lam);

            // Compute new pixel coordinates of intersection point
            float iOld = pixelPos.x;
            float jOld = pixelPos.y;
            getGeoCoding().getPixelPos(_gp, pixelPos);
            float iNew = pixelPos.x;
            float jNew = pixelPos.y;

            // Compute distance between last and current pixel position
            float deltaI = (iNew - iOld);
            float deltaJ = (jNew - jOld);
            float d = deltaI * deltaI + deltaJ * deltaJ;
            if (d < PIXEL_EPS_SQR) {
                return true;
            }
        }

        return false;
    }
}
