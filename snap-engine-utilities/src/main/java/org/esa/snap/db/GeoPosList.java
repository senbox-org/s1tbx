package org.esa.snap.db;

import org.esa.beam.framework.datamodel.GeoPos;

/**
 * interface for AOI
 */
public interface GeoPosList {

    public void setPoints(final GeoPos[] selectionBox);

    public GeoPos[] getPoints();
}
