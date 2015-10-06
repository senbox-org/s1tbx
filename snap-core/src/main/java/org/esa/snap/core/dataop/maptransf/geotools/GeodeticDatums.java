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

package org.esa.snap.core.dataop.maptransf.geotools;

import org.esa.snap.core.dataop.maptransf.Datum;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated since BEAM 4.7, no replacement.
 */
@Deprecated
public class GeodeticDatums {

    // EPSG::6655
    public static final GeodeticDatum ITRF97;
    // EPSG::6322
    public static final GeodeticDatum WGS72;
    // EPSG::6326
    public static final GeodeticDatum WGS84;

    private static final Map<Datum, GeodeticDatum> geodeticDatumMap;

    static {
        final DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);

        try {
            ITRF97 = factory.createGeodeticDatum("EPSG:6655");
            WGS72 = factory.createGeodeticDatum("EPSG:6322");
            WGS84 = factory.createGeodeticDatum("EPSG:6326");
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }

        geodeticDatumMap = new HashMap<Datum, GeodeticDatum>(3);
        geodeticDatumMap.put(Datum.ITRF_97, ITRF97);
        geodeticDatumMap.put(Datum.WGS_72, WGS72);
        geodeticDatumMap.put(Datum.WGS_84, WGS84);
    }

    public static GeodeticDatum getGeodeticDatum(Datum datum) {
        return geodeticDatumMap.get(datum);
    }
}
