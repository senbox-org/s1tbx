package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.dataio.geocoding.Discontinuity;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.RsMathUtils;

import java.util.ArrayList;

public class RasterUtils {

    private static final double MEAN_EARTH_RADIUS_KM = RsMathUtils.MEAN_EARTH_RADIUS * 0.001;
    private static double STEP_THRESH = 180.0;

    // this one is for backward compatibility - I guess it can be removed during the merge operation with SNAP core tb 2019-10-24
    // @todo discuss with Marco and Norman
    static Discontinuity calculateDiscontinuity(float[] longitudes) {
        float max = Float.MIN_VALUE;

        for (float value : longitudes) {
            if (value > max) {
                max = value;
            }
        }

        if (max > 180.0) {
            return Discontinuity.AT_360;
        } else {
            return Discontinuity.AT_180;
        }
    }

    /**
     * Checks if the longitude data raster contains the anti-meridian. Runs along the raster borders and if a
     * longitude-delta larger than 180 deg is detected, an anti-meridian crossing is contained in the data.
     *
     * @param longitudes the longitude raster
     * @param width      the data width in pixels
     * @return whether the data contains the anti-meridian or not
     */
    public static boolean containsAntiMeridian(double[] longitudes, int width) {
        final int height = longitudes.length / width;

        // top
        for (int x = 1; x < width; x++) {
            final double step = Math.abs(longitudes[x] - longitudes[x - 1]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // left
        int lineOffset;
        for (int y = 1; y < height; y++) {
            final double step = Math.abs(longitudes[y * width] - longitudes[(y - 1) * width]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // bottom
        lineOffset = (height - 1) * width;
        for (int x = 1; x < width; x++) {
            final double step = Math.abs(longitudes[lineOffset + x] - longitudes[lineOffset + x - 1]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        // right
        for (int y = 1; y < height; y++) {
            lineOffset = width - 1;
            final double step = Math.abs(longitudes[y * width + lineOffset] - longitudes[(y - 1) * width + lineOffset]);
            if (step > STEP_THRESH) {
                return true;
            }
        }

        return false;
    }

    public static PixelPos[] getPoleLocations(GeoRaster geoRaster) {
        final double deltaToPole = getLatDeltaToPole(geoRaster.getRasterResolutionInKm());
        double maxLat = 90.0 - deltaToPole;
        double minLat = -90.0 + deltaToPole;

        final ArrayList<PixelPos> poleCandidates = findPoleCandidates(geoRaster, maxLat, minLat);
        if (poleCandidates.size() == 0) {
            return new PixelPos[0];
        }

        final ArrayList<PixelPos> consolidatedPoleLocations = new ArrayList<>();
        final double[] lons = geoRaster.getLongitudes();
        final int width = geoRaster.getRasterWidth();
        for (final PixelPos candidate : poleCandidates) {
            final double[] deltas = new double[8];
            final int x = (int) candidate.x;
            final int y = (int) candidate.y;

            // walk clockwise around the pole and calculate the 8 longitude deltas
            deltas[0] = Math.abs(lons[(y) * width + (x - 1)] - lons[(y - 1) * width + (x - 1)]);
            deltas[1] = Math.abs(lons[(y + 1) * width + (x - 1)] - lons[(y) * width + (x - 1)]);
            deltas[2] = Math.abs(lons[(y + 1) * width + (x)] - lons[(y + 1) * width + (x - 1)]);
            deltas[3] = Math.abs(lons[(y + 1) * width + (x + 1)] - lons[(y + 1) * width + (x)]);
            deltas[4] = Math.abs(lons[(y) * width + (x + 1)] - lons[(y + 1) * width + (x + 1)]);
            deltas[5] = Math.abs(lons[(y - 1) * width + (x + 1)] - lons[(y) * width + (x + 1)]);
            deltas[6] = Math.abs(lons[(y - 1) * width + (x)] - lons[(y - 1) * width + (x + 1)]);
            deltas[7] = Math.abs(lons[(y - 1) * width + (x - 1)] - lons[(y - 1) * width + (x)]);

            int numMeridianCrossings = 0;
            for (final double delta : deltas) {
                if (delta > STEP_THRESH) {
                    ++numMeridianCrossings;
                }
            }
            if (numMeridianCrossings % 2 == 1) {
                consolidatedPoleLocations.add(candidate);
            }
        }

        return consolidatedPoleLocations.toArray(new PixelPos[0]);
    }

    /**
     * Returns the latitude difference to the poles (i.e. the lenght of the circle segment) given the desired distance
     * in kilometers.
     *
     * @param distanceInKm the distance in kilometers
     * @return the latitude delta in degrees
     */
    static double getLatDeltaToPole(double distanceInKm) {
        return ((distanceInKm * 180.0) / (Math.PI * MEAN_EARTH_RADIUS_KM));
    }

    public static float[] toFloat(double[] doubles) {
        final float[] floats = new float[doubles.length];

        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }

        return floats;
    }

    public static double[] toDouble(float[] floats) {
        final double[] doubles = new double[floats.length];

        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }

        return doubles;
    }

    // returns al (x/y) positions that have a latitude above the defined pole-angle threshold
    private static ArrayList<PixelPos> findPoleCandidates(GeoRaster geoRaster, double maxLat, double minLat) {
        final ArrayList<PixelPos> poleCandidates = new ArrayList<>();

        final double[] latitudes = geoRaster.getLatitudes();
        final int rasterWidth = geoRaster.getRasterWidth();
        for (int y = 0; y < geoRaster.getRasterHeight(); y++) {
            final int lineOffset = y * rasterWidth;
            for (int x = 0; x < rasterWidth; x++) {
                final double lat = latitudes[lineOffset + x];
                if ((lat >= maxLat) || (lat <= minLat)) {
                    poleCandidates.add(new PixelPos(x, y));
                }
            }
        }
        return poleCandidates;
    }
}
