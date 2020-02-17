package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ComponentFactory;
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

        final int x0 = (int) Math.floor(pixelPos.getX());
        final int y0 = (int) Math.floor(pixelPos.getY());
        if (x0 < 0 || x0 >= sceneWidth || y0 < 0 || y0 >= sceneHeight) {
            return geoPos;
        }

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
    public String getFactoryKey() {
        return ComponentFactory.FWD_PIXEL;
    }

    @Override
    public void dispose() {
        longitudes = null;
        latitudes = null;
    }
}
