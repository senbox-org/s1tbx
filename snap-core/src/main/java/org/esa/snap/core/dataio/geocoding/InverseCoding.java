package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface InverseCoding {

    /**
     * Returns the pixel coordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon in the coordinate system determined by underlying CRS
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the pixel co-ordinates as x/y
     */
    PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos);

    void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations);

    void dispose();
}
