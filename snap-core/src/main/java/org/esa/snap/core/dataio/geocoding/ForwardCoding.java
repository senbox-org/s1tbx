package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface ForwardCoding {

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's coordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the geographical position as lat/lon in the coordinate system determined by the underlying CRS
     */
    GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos);

    /**
     * Initializes the ForwardCoding. Passes in the geo_raster and allows initializing.
     *
     * @param geoRaster            the geoRaster
     * @param containsAntiMeridian whether the data crosses the anti-meridian or not
     * @param poleLocations        locations of the poles - o3 a leght zero array if no poles are contained in the data
     */
    void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations);

    /**
     * A ForwardCoding shall be instanced only by {@link ComponentFactory} using a {@link String} key.
     * Such an instance must be able to return the key, in order to persist the ForwardCoding and recreate
     * such an instance via {@link ComponentFactory} if the {@link org.esa.snap.core.datamodel.Product} shall
     * be opened again.
     *
     * @return the key String used while instantiating via {@link ComponentFactory}
     */
    String getFactoryKey();

    void dispose();
}
