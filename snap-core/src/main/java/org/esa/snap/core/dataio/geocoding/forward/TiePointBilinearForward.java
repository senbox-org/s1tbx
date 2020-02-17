package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGrid;

public class TiePointBilinearForward extends TiePointForward {

    private TiePointGrid lonGrid;
    private TiePointGrid latGrid;
    private int sceneWidth;
    private int sceneHeight;

    public TiePointBilinearForward() {
        lonGrid = null;
        latGrid = null;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        int discontinuity = containsAntiMeridian ? TiePointGrid.DISCONT_AT_180 : TiePointGrid.DISCONT_NONE;

        lonGrid = new TiePointGrid("lon", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                RasterUtils.toFloat(geoRaster.getLongitudes()), discontinuity);

        latGrid = new TiePointGrid("lat", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                                   geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                                   geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                                   RasterUtils.toFloat(geoRaster.getLatitudes()));

        checkGrids(lonGrid, latGrid);

        this.sceneWidth = geoRaster.getSceneWidth();
        this.sceneHeight = geoRaster.getSceneHeight();
    }

    @Override
    public String getFactoryKey() {
        return ComponentFactory.FWD_TIE_POINT_BILINEAR;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        if (pixelPos.x < 0 || pixelPos.x > sceneWidth
                || pixelPos.y < 0 || pixelPos.y > sceneHeight) {
            geoPos.setInvalid();
        } else {
            geoPos.lat = latGrid.getPixelDouble(pixelPos.x, pixelPos.y);
            geoPos.lon = lonGrid.getPixelDouble(pixelPos.x, pixelPos.y);
        }
        return geoPos;
    }

    @Override
    public void dispose() {
        lonGrid = null;
        latGrid = null;
    }
}
