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

    /**
     * An InverseCoding shall be instanced only by {@link ComponentFactory} using a {@link String} key.
     * Such an instance must be able to return the key, in order to persist the InverseCoding and recreate
     * such an instance via {@link ComponentFactory} if the {@link org.esa.snap.core.datamodel.Product} shall
     * be opened again.
     *
     * @return the key String used while instantiating via {@link ComponentFactory}
     */
    String getFactoryKey();

    void dispose();
}
