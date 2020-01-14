package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.core.util.math.SphericalDistance;

public class InverseDistanceWeightingInterpolator implements XYInterpolator {

    private static final double MIN_DISTANCE = 0.001;   // which is one millimeter tb 2020-01-10

    public PixelPos interpolate(GeoPos geoPos, PixelPos pixelPos, InterpolationContext context) {
        final SphericalDistance distance = new SphericalDistance(geoPos.lon, geoPos.lat);
        final double[] distances = new double[4];

        for (int i = 0; i < 4; i++) {
            distances[i] = distance.distance(context.lons[i], context.lats[i]) * RsMathUtils.MEAN_EARTH_RADIUS;
            if (distances[i] < MIN_DISTANCE) {
                return new PixelPos(context.x[i], context.y[i]);
            } else {
                distances[i] = 1.0 / distances[i];
            }
        }

        double inv_sum = 0.0;
        double x_sum = 0.0;
        double y_sum = 0.0;
        for (int i = 0; i < 4; i++) {
            inv_sum += distances[i];
            x_sum += context.x[i] * distances[i];
            y_sum += context.y[i] * distances[i];
        }
        inv_sum = 1.0 / inv_sum;

        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.x = x_sum * inv_sum;
        pixelPos.y = y_sum * inv_sum;
        return pixelPos;
    }
}
