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

        final double x = pixelPos.getX();
        final double y = pixelPos.getY();

        if (x < 0 || x > sceneWidth || y < 0 || y > sceneHeight) {
            return geoPos;
        }

        int xf = (int) Math.floor(x);
        int yf = (int) Math.floor(y);
        if (xf == sceneWidth) {
            xf--;
        }
        if (yf == sceneHeight) {
            yf--;
        }
        final int index = yf * sceneWidth + xf;
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
