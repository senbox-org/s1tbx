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
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;

public class EllipsoidsTest extends TestCase {

    public void testEllipsoids() throws FactoryException {
        final DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);

        assertSame(factory.createEllipsoid("EPSG:7004"), Ellipsoids.BESSEL1841);
        assertSame(factory.createEllipsoid("EPSG:7019"), Ellipsoids.GRS80);
        assertSame(factory.createEllipsoid("EPSG:7043"), Ellipsoids.WGS72);
        assertSame(factory.createEllipsoid("EPSG:7030"), Ellipsoids.WGS84);
    }

}
