package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.RsMathUtils;
import org.geotools.referencing.CRS;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.awt.geom.Rectangle2D;

/**
 * Calculates the size of an specified area in square meter. The size is computed considering the latitudinal
 * area correction.
 *
 * @author Marco Peters
 */
public class AreaCalculator {

    private final GeoCoding gc;
    private double earthRadius;

    /**
     * Initialise the calculator with an {@link GeoCoding}.
     * The earth radius ist retrieved from the ellipsoid from the underlying {@link GeoCoding#getMapCRS() map crs} of the geo-coding.
     * If this is not possible a default mean earth radius us used.
     *
     * @param gc the geo-coding
     */
    public AreaCalculator(GeoCoding gc) {
        this.gc = gc;
        Unit<Length> axisUnit = CRS.getEllipsoid(gc.getMapCRS()).getAxisUnit();
        initEarthRadius(gc, axisUnit);
    }

    /**
     * The earth radius used for the calculation
     *
     * @return the earth radius in meter
     */
    public double getEarthRadius() {
        return earthRadius;
    }

    /**
     * Calculates the size of the pixel specified by the given x,y coordinates. The unit of the size is always {@code meter}.
     *
     * @param x the x location of the pixel
     * @param y the y location of the pixel
     * @return the size in square meters
     */
    public double calculatePixelSize(int x, int y) {
        Rectangle2D geoRectangleForPixel = createGeoRectangleForPixel(x, y);
        return calculateRectangleSize(geoRectangleForPixel);
    }

    /**
     * Calculates the size of the area of the rectangle specified. The rectangle needs to be specified in
     * geo-graphical latitude/longitude coordinates
     * The unit of the size is always {@code meter}.
     *
     * @param rectangle rectangle of the area in latitude/longitude coordinates
     * @return the size in square meters
     */
    public double calculateRectangleSize(Rectangle2D rectangle) {
        double deltaLon = rectangle.getWidth();
        double deltaLat = rectangle.getHeight();
        double centerLat = rectangle.getCenterY();
        double a = earthRadius * Math.cos(centerLat * MathUtils.DTOR) * deltaLon * MathUtils.DTOR;
        double b = earthRadius * deltaLat * MathUtils.DTOR;

        return (a * b);
    }

    /**
     * Creates a rectangle for the given pixel (x,y) using the specified geo-coding.
     *
     * @param x the x location of the pixel
     * @param y the y location of the pixel
     */
    Rectangle2D createGeoRectangleForPixel(int x, int y) {
        Rectangle2D.Double rect = new Rectangle2D.Double();
        GeoPos geoPosUL = gc.getGeoPos(new PixelPos(x, y), null);
        GeoPos geoPosLR = gc.getGeoPos(new PixelPos(x + 1, y + 1), null);
        rect.setFrameFromDiagonal(geoPosUL.getLon(), geoPosUL.getLat(),
                                  geoPosLR.getLon(), geoPosLR.getLat());
        return rect;
    }

    private void initEarthRadius(GeoCoding gc, Unit<Length> axisUnit) {
        if (axisUnit.equals(SI.METER)) {
            earthRadius = CRS.getEllipsoid(gc.getMapCRS()).getSemiMajorAxis() * 1.0;
        } else if (axisUnit.equals(SI.KILOMETER)) {
            earthRadius = CRS.getEllipsoid(gc.getMapCRS()).getSemiMajorAxis() * 1000.0;
        } else {
            earthRadius = RsMathUtils.MEAN_EARTH_RADIUS;
        }
    }
}
