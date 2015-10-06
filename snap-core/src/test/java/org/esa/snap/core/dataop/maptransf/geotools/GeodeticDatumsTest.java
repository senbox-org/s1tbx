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

import junit.framework.TestCase;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;

public class GeodeticDatumsTest extends TestCase {

    public void testGeodeticDatums() throws FactoryException {
        final DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);

        assertSame(factory.createGeodeticDatum("EPSG:6655"), GeodeticDatums.ITRF97);
        assertSame(factory.createGeodeticDatum("EPSG:6322"), GeodeticDatums.WGS72);
        assertSame(factory.createGeodeticDatum("EPSG:6326"), GeodeticDatums.WGS84);

        assertSame(GeodeticDatums.ITRF97, GeodeticDatums.getGeodeticDatum(Datum.ITRF_97));
        assertSame(GeodeticDatums.WGS72, GeodeticDatums.getGeodeticDatum(Datum.WGS_72));
        assertSame(GeodeticDatums.WGS84, GeodeticDatums.getGeodeticDatum(Datum.WGS_84));
    }

}
