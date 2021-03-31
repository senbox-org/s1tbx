/*
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
package org.esa.s1tbx.io.gaofen3;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.SystemUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Jakob Grahn
 */
public class Gaofen3Geocoding extends AbstractGeoCoding {

    private final float _pixelOffsetX;
    private final float _pixelOffsetY;
    private final float _pixelSizeX;
    private final float _pixelSizeY;
    private Map<String, List<Double>> _rpcParameters;
    private String _rpcVersion = "RPC00B";
    double[] _centerGeoPos;
    private final Datum _datum = Datum.WGS_84;
    private final double _height = 0.0;

    public Gaofen3Geocoding(final float pixelOffsetX, final float pixelOffsetY,
                            final float pixelSizeX, final float pixelSizeY,
                            final Map<String, List<Double>> rpcParameters,
                            final double[] centerGeoPos
    ) {
        _pixelOffsetX = pixelOffsetX;
        _pixelOffsetY = pixelOffsetY;
        _pixelSizeX = pixelSizeX;
        _pixelSizeY = pixelSizeY;
        _rpcParameters = rpcParameters;
        _centerGeoPos = centerGeoPos;
    }

    public float getPixelOffsetX() {
        return _pixelOffsetX;
    }

    public float getPixelOffsetY() {
        return _pixelOffsetY;
    }

    public float getPixelSizeX() {
        return _pixelSizeX;
    }

    public float getPixelSizeY() {
        return _pixelSizeY;
    }

    public Map<String, List<Double>> getRpcParameters() {
        return _rpcParameters;
    }

    public double[] getCenterGeoPos() {
        return _centerGeoPos;
    }

    protected double[] geo2pixel(double lat, double lon){
        double latOffset = _rpcParameters.get("latOffset").get(0);
        double latScale = _rpcParameters.get("latScale").get(0);
        double longOffset = _rpcParameters.get("longOffset").get(0);
        double longScale = _rpcParameters.get("longScale").get(0);
        double heightOffset = _rpcParameters.get("heightOffset").get(0);
        double heightScale = _rpcParameters.get("heightScale").get(0);
        double sampOffset = _rpcParameters.get("sampOffset").get(0);
        double sampScale = _rpcParameters.get("sampScale").get(0);
        double lineOffset = _rpcParameters.get("lineOffset").get(0);
        double lineScale = _rpcParameters.get("lineScale").get(0);
        double p = (lat - latOffset)/latScale;
        double l = (lon - longOffset)/longScale;
        double h = (_height - heightOffset)/heightScale;
        double x = evalPoly(p, l, h, _rpcParameters.get("sampNumCoef"))/
                evalPoly(p, l, h, _rpcParameters.get("sampDenCoef"));
        double y = evalPoly(p, l, h, _rpcParameters.get("lineNumCoef"))/
                evalPoly(p, l, h, _rpcParameters.get("lineDenCoef"));
        double xPos = sampOffset + x*sampScale;
        double yPos = lineOffset + y*lineScale;
        return new double[] {xPos, yPos};
    }

    protected double[] pixel2geo(double x, double y){
        final MultivariateFunction func = a -> {
            final double[] xy = geo2pixel(a[0], a[1]);
            return Math.pow(Math.abs(x - xy[0]), 2) + Math.pow(Math.abs(y - xy[1]), 2);
        };
        final PowellOptimizer optim = new PowellOptimizer(1e-9, Math.ulp(1d));
        final PointValuePair result = optim.optimize(new MaxEval(1000),
                new ObjectiveFunction(func),
                GoalType.MINIMIZE,
                new InitialGuess(getCenterGeoPos()));
        return result.getPoint();
    }

    protected double evalPoly(double p, double l, double h, List<Double> c){
        double[] a = {1, l, p, h, l*p, l*h, p*h, Math.pow(l,2), Math.pow(p,2), Math.pow(h,2), p*l*h, Math.pow(l,3),
                l*Math.pow(p,2), l*Math.pow(h,2), p*Math.pow(l,2), Math.pow(p,3), p*Math.pow(h,2), h*Math.pow(l,2),
                h*Math.pow(p,2), Math.pow(h,3)};

        int[] idx;
        switch (_rpcVersion) {
            case "RPC00A":
                idx = new int[]{0,1,2,3,4,5,6,8,9,10,7,11,14,17,12,15,18,13,16,19};
                break;
            case "RPC00B":
                idx = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
                break;
            default:
                idx = new int[]{};
                SystemUtils.LOG.severe("RPC-version not understood.");
        }

        double out = 0.0;
        for (int i = 0; i < c.size(); ++i) {
            out += c.get(idx[i]) * a[i];
        }
        return out;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();
        final double[] xy = geo2pixel(geoPos.getLat(), geoPos.getLon());
        pixelPos.setLocation(
                (xy[0] - _pixelOffsetX) / _pixelSizeX,
                (xy[1] - _pixelOffsetY) / _pixelSizeY
        );
        return pixelPos;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the geographical position as lat/lon.
     */
    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            // TODO: 20.02.2020 SE fixed -- Marked GETGEOPOS why?
//            geoPos = new GeoPos(0.0f, 0.0f);
            // TODO: 20.02.2020 SE fixed -- Marked GETGEOPOS ... instead that way?
            geoPos = new GeoPos();
        }
        final double x = _pixelOffsetX + _pixelSizeX * pixelPos.x;
        final double y = _pixelOffsetY + _pixelSizeY * pixelPos.y;
        final double[] geo = pixel2geo(x, y);
        geoPos.setLocation(geo[0], geo[1]);
        return geoPos;

    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return _datum;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        if (subsetDef == null || subsetDef.isEntireProductSelected()) {
            destScene.setGeoCoding(clone());
            return true;
        }

        float pixelOffsetX = getPixelOffsetX();
        float pixelOffsetY = getPixelOffsetY();
        float pixelSizeX = getPixelSizeX();
        float pixelSizeY = getPixelSizeY();

        if (subsetDef.getRegion() != null) {
            pixelOffsetX += subsetDef.getRegion().getX() * pixelSizeX;
            pixelOffsetY += subsetDef.getRegion().getY() * pixelSizeY;
        }
        pixelSizeX *= subsetDef.getSubSamplingX();
        pixelSizeY *= subsetDef.getSubSamplingY();

        destScene.setGeoCoding(createCloneWithNewOffsetAndSize(pixelOffsetX, pixelOffsetY,
                pixelSizeX, pixelSizeY));
        return true;
    }

    @Override
    public boolean canClone() {
        return true;
    }

    @Override
    public GeoCoding clone() {
        return new org.esa.s1tbx.io.gaofen3.Gaofen3Geocoding(
                _pixelOffsetX, _pixelOffsetY, _pixelSizeX, _pixelSizeY, _rpcParameters, _centerGeoPos);
    }

    public org.esa.s1tbx.io.gaofen3.Gaofen3Geocoding createCloneWithNewOffsetAndSize(
            float pixelOffsetX, float pixelOffsetY, float pixelSizeX, float pixelSizeY) {
        return new org.esa.s1tbx.io.gaofen3.Gaofen3Geocoding(
                pixelOffsetX, pixelOffsetY, pixelSizeX, pixelSizeY, _rpcParameters, _centerGeoPos);
    }
}
