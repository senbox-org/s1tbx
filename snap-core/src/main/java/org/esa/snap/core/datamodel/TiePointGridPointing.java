/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.Guardian;

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
            sd.azimuth = _saGrid.getPixelDouble(pixelPos.x, pixelPos.y);
            sd.zenith = _szGrid.getPixelDouble(pixelPos.x, pixelPos.y);
        }
        return sd;
    }

    public final AngularDirection getViewDir(PixelPos pixelPos, AngularDirection vd) {
        if (canGetViewDir()) {
            if (vd == null) {
                vd = new AngularDirection();
            }
            vd.azimuth = _vaGrid.getPixelDouble(pixelPos.x, pixelPos.y);
            vd.zenith = _vzGrid.getPixelDouble(pixelPos.x, pixelPos.y);
        }
        return vd;
    }

    public double getElevation(PixelPos pixelPos) {
        return canGetElevation() ? _elGrid.getPixelDouble(pixelPos.x, pixelPos.y) : 0.0f;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Pointing) {
            final Pointing other = (Pointing) obj;
            return getGeoCoding().equals(other.getGeoCoding());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getGeoCoding().hashCode();
    }
}
