package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.util.InterpolationContext;
import org.esa.snap.core.dataio.geocoding.util.InverseDistanceWeightingInterpolator;
import org.esa.snap.core.dataio.geocoding.util.XYInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.RsMathUtils;

public class PixelQuadTreeInverse implements InverseCoding {

    private static final double TO_DEG = 180.0 / Math.PI ;

    private final boolean fractionalAccuracy;
    private final XYInterpolator interpolator;

    private int rasterWidth;
    private int rasterHeight;
    private double epsilon;
    private double offsetX;
    private double offsetY;
    private boolean isCrossingMeridian;
    private double[] longitudes;
    private double[] latitudes;

    PixelQuadTreeInverse() {
        this(false);
    }

    public PixelQuadTreeInverse(boolean fractionalAccuracy) {
        this.fractionalAccuracy = fractionalAccuracy;
        if (fractionalAccuracy) {
            interpolator = new InverseDistanceWeightingInterpolator();
        } else {
            interpolator = null;
        }
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();

        final Result result = new Result();
        boolean pixelFound = quadTreeSearch(0,
                geoPos.lat, geoPos.lon,
                0, 0,
                rasterWidth, rasterHeight,
                result);

        if (pixelFound) {
            final GeoPos resultGeoPos = new GeoPos();
            getGeoPos(result.x, result.y, resultGeoPos);
            final double absLon = Math.abs(resultGeoPos.lon - geoPos.lon);
            final double absLat = Math.abs(resultGeoPos.lat - geoPos.lat);
            final double distance = Math.max(absLat, absLon);

            if (distance < epsilon) {
                if (fractionalAccuracy) {
                    final InterpolationContext context = InterpolationContext.extract(result.x, result.y, longitudes, latitudes, rasterWidth, rasterHeight);
                    //noinspection ConstantConditions
                    pixelPos = interpolator.interpolate(geoPos, pixelPos, context);
                    pixelPos.setLocation(pixelPos.x + offsetX, pixelPos.y + offsetY);
                } else {
                    pixelPos.setLocation(result.x + offsetX, result.y + offsetY);
                }
            }
        }

        return pixelPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        this.rasterWidth = geoRaster.getSceneWidth();
        this.rasterHeight = geoRaster.getSceneHeight();

        this.longitudes = geoRaster.getLongitudes();
        this.latitudes = geoRaster.getLatitudes();

        epsilon = getEpsilon(geoRaster.getRasterResolutionInKm());
        isCrossingMeridian = containsAntiMeridian;

        offsetX = geoRaster.getOffsetX();
        offsetY = geoRaster.getOffsetY();
    }

    @Override
    public void dispose() {
        longitudes = null;
        latitudes = null;
    }

    // package access for testing only tb 2019-12-16
    void getGeoPos(int pixelX, int pixelY, GeoPos geoPos) {
        final int index = pixelY * rasterWidth + pixelX;

        geoPos.setLocation(latitudes[index], longitudes[index]);
    }

    // package access for testing only tb 2019-12-16
    double getEpsilon(double resolutionInKm) {
//        final int x_center = rasterWidth / 2;
//        final int y_center = rasterHeight / 2;
//
//        final GeoPos geoPos_1 = new GeoPos();
//        final GeoPos geoPos_2 = new GeoPos();
//        getGeoPos(x_center, y_center, geoPos_1);
//        getGeoPos(x_center + 1, y_center + 1, geoPos_2);
//
//        final double deltaLat = Math.abs(geoPos_1.lat - geoPos_2.lat);
//        final double deltaLon = Math.abs(geoPos_1.lon - geoPos_2.lon);
//
//        return Math.max(deltaLat, deltaLon) / Math.sqrt(2);

        final double angle = 2.0 * Math.asin((resolutionInKm * 1000.0) / (2 * RsMathUtils.MEAN_EARTH_RADIUS));
        return TO_DEG * angle * 2.0;
    }

    // package access for testing only tb 2019-12-16
    static double getPositiveLonMin(double lon0, double lon1, double lon2, double lon3) {
        double lonMin = 180.0f;
        if (lon0 >= 0.0) {
            lonMin = lon0;
        }
        if (lon1 >= 0.0) {
            lonMin = Math.min(lon1, lonMin);
        }
        if (lon2 >= 0.0) {
            lonMin = Math.min(lon2, lonMin);
        }
        if (lon3 >= 0.0) {
            lonMin = Math.min(lon3, lonMin);
        }
        return lonMin;
    }

    // package access for testing only tb 2019-12-16
    static double getNegativeLonMax(double lon0, double lon1, double lon2, double lon3) {
        double lonMax = -180.0f;
        if (lon0 < 0.0f) {
            lonMax = lon0;
        }
        if (lon1 < 0.0f) {
            lonMax = Math.max(lon1, lonMax);
        }
        if (lon2 < 0.0f) {
            lonMax = Math.max(lon2, lonMax);
        }
        if (lon3 < 0.0f) {
            lonMax = Math.max(lon3, lonMax);
        }
        return lonMax;
    }

    // package access for testing only tb 2019-12-16
    static double sq(final double dx, final double dy) {
        return dx * dx + dy * dy;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private boolean quadTreeSearch(final int depth,
                                   final double lat,
                                   final double lon,
                                   final int x, final int y,
                                   final int w, final int h,
                                   final Result result) {

        if (w < 2 || h < 2) {
            return false;
        }

        final int x_1 = x;
        final int x_2 = x_1 + w - 1;

        final int y_1 = y;
        final int y_2 = y_1 + h - 1;

        final GeoPos geoPos = new GeoPos();
        getGeoPos(x_1, y_1, geoPos);
        final double lat_0 = geoPos.lat;
        final double lon_0 = geoPos.lon;
        getGeoPos(x_1, y_2, geoPos);
        final double lat_1 = geoPos.lat;
        final double lon_1 = geoPos.lon;
        getGeoPos(x_2, y_1, geoPos);
        final double lat_2 = geoPos.lat;
        final double lon_2 = geoPos.lon;
        getGeoPos(x_2, y_2, geoPos);
        final double lat_3 = geoPos.lat;
        final double lon_3 = geoPos.lon;

        final double latMin = Math.min(lat_0, Math.min(lat_1, Math.min(lat_2, lat_3))) - epsilon;
        final double latMax = Math.max(lat_0, Math.max(lat_1, Math.max(lat_2, lat_3))) + epsilon;

        double lonMin;
        double lonMax;
        if (isCrossingMeridian) {
            final double signumLon = Math.signum(lon);
            if (signumLon > 0f) {
                // position is in a region with positive longitudes, so cut negative longitudes from quad area
                lonMax = 180.0f;
                lonMin = getPositiveLonMin(lon_0, lon_1, lon_2, lon_3);
            } else {
                // position is in a region with negative longitudes, so cut positive longitudes from quad area
                lonMin = -180.0f;
                lonMax = getNegativeLonMax(lon_0, lon_1, lon_2, lon_3);
            }
        } else {
            lonMin = Math.min(lon_0, Math.min(lon_1, Math.min(lon_2, lon_3))) - epsilon;
            lonMax = Math.max(lon_0, Math.max(lon_1, Math.max(lon_2, lon_3))) + epsilon;
        }

        final boolean definitelyOutside = lat < latMin || lat > latMax || lon < lonMin || lon > lonMax;
        if (definitelyOutside) {
            return false;
        }

        boolean pixelFound = false;
        if (w == 2 && h == 2) {
            final double f = Math.cos(lat * MathUtils.DTOR);
            if (result.update(x_1, y_1, sq(lat - lat_0, f * (lon - lon_0)))) {
                pixelFound = true;
            }
            if (result.update(x_1, y_2, sq(lat - lat_1, f * (lon - lon_1)))) {
                pixelFound = true;
            }
            if (result.update(x_2, y_1, sq(lat - lat_2, f * (lon - lon_2)))) {
                pixelFound = true;
            }
            if (result.update(x_2, y_2, sq(lat - lat_3, f * (lon - lon_3)))) {
                pixelFound = true;
            }
        } else {
            pixelFound = quadTreeRecursion(depth, lat, lon, x_1, y_1, w, h, result);
        }


        return pixelFound;
    }

    private boolean quadTreeRecursion(final int depth,
                                      final double lat, final double lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {

        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }
        final boolean b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }
}
