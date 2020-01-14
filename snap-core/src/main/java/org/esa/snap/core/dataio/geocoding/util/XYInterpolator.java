package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface XYInterpolator {

    PixelPos interpolate(GeoPos geoPos, PixelPos pixelPos, InterpolationContext context);
}
