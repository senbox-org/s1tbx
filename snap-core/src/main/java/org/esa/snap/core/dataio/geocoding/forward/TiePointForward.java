package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.TiePointGrid;

abstract class TiePointForward implements ForwardCoding {

    void checkGrids(TiePointGrid lonGrid, TiePointGrid latGrid) {
        if (lonGrid.getGridWidth() != latGrid.getGridWidth() ||
                lonGrid.getGridHeight() != latGrid.getGridHeight() ||
                lonGrid.getOffsetX() != latGrid.getOffsetX() ||
                lonGrid.getOffsetY() != latGrid.getOffsetY() ||
                lonGrid.getSubSamplingX() != latGrid.getSubSamplingX() ||
                lonGrid.getSubSamplingY() != latGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("lonGrid is not compatible with latGrid");
        }
    }


    void checkGeoRaster(GeoRaster geoRaster) {
        if (geoRaster.getLongitudes().length != geoRaster.getLatitudes().length) {
            throw new IllegalArgumentException("lonGrid is not compatible with latGrid");
        }
    }
}
