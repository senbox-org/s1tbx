package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public class PixelForward implements ForwardCoding {

    private int sceneWidth;
    private int sceneHeight;
    private double[] longitudes;
    private double[] latitudes;

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (!pixelPos.isValid()) {
            return geoPos;
        }

        final double x = pixelPos.getX();
        final double y = pixelPos.getY();
        if (x < 0 || x > sceneWidth || y < 0 || y > sceneHeight) {
            return geoPos;
        }

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);

        final int index = y0 * sceneWidth + x0;
        geoPos.setLocation(latitudes[index], longitudes[index]);
        return geoPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        sceneWidth = geoRaster.getSceneWidth();
        sceneHeight = geoRaster.getSceneHeight();
        longitudes = geoRaster.getLongitudes();
        latitudes = geoRaster.getLatitudes();
    }

    @Override
    public void dispose() {
        longitudes = null;
        latitudes = null;
    }
}
