/*
 * $Id: TiePointGridPointing.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Guardian;

/**
 * A {@link Pointing} which uses tie-point grids to compute the geometry for a given pixel position.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public final class TiePointGridPointing implements Pointing {

    private final GeoCoding _geoCoding;
    private final TiePointGrid _szGrid;
    private final TiePointGrid _saGrid;
    private final TiePointGrid _vzGrid;
    private final TiePointGrid _vaGrid;
    private final TiePointGrid _elGrid;

    /**
     * Constructs a new pointing based on tie-point grids.
     *
     * @param geoCoding the geo-coding, must not be null
     * @param szGrid    the sun zenith tie-point grid, can be null
     * @param saGrid    the sun azimuth tie-point grid, can be null
     * @param vzGrid    the viewing zenith tie-point grid, can be null
     * @param vaGrid    the viewing azimuth tie-point grid, can be null
     * @param elGrid    the elevation tie-point grid, can be null
     */
    public TiePointGridPointing(GeoCoding geoCoding,
                                TiePointGrid szGrid,
                                TiePointGrid saGrid,
                                TiePointGrid vzGrid,
                                TiePointGrid vaGrid,
                                TiePointGrid elGrid) {
        Guardian.assertNotNull("geoCoding", geoCoding);
        _geoCoding = geoCoding;
        _szGrid = szGrid;
        _saGrid = saGrid;
        _vzGrid = vzGrid;
        _vaGrid = vaGrid;
        _elGrid = elGrid;
    }

    public final GeoCoding getGeoCoding() {
        return _geoCoding;
    }

    public final AngularDirection getSunDir(PixelPos pixelPos, AngularDirection sd) {
        if (canGetSunDir()) {
            if (sd == null) {
                sd = new AngularDirection();
            }
            sd.azimuth = _saGrid.getPixelFloat(pixelPos.x, pixelPos.y);
            sd.zenith = _szGrid.getPixelFloat(pixelPos.x, pixelPos.y);
        }
        return sd;
    }

    public final AngularDirection getViewDir(PixelPos pixelPos, AngularDirection vd) {
        if (canGetViewDir()) {
            if (vd == null) {
                vd = new AngularDirection();
            }
            vd.azimuth = _vaGrid.getPixelFloat(pixelPos.x, pixelPos.y);
            vd.zenith = _vzGrid.getPixelFloat(pixelPos.x, pixelPos.y);
        }
        return vd;
    }

    public float getElevation(PixelPos pixelPos) {
        return canGetElevation() ? _elGrid.getPixelFloat(pixelPos.x, pixelPos.y) : 0.0f;
    }

    public final boolean canGetElevation() {
        return _elGrid != null;
    }

    public final boolean canGetSunDir() {
        return _szGrid != null && _saGrid != null;
    }

    public final boolean canGetViewDir() {
        return _vzGrid != null && _vaGrid != null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Pointing) {
            final Pointing other = (Pointing) obj;
            return getGeoCoding().equals(other.getGeoCoding());
        }
        return false;
    }

    public int hashCode() {
        return getGeoCoding().hashCode();
    }
}
