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

import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;

/**
 * @deprecated since BEAM 4.7, no replacement.
 */
@Deprecated
public class Ellipsoids {

    // EPSG::7004
    public static final Ellipsoid BESSEL1841;
    // EPSG::7019
    public static final Ellipsoid GRS80;
    // EPSG::7043
    public static final Ellipsoid WGS72;
    // EPSG::7030
    public static final Ellipsoid WGS84;

    static {
        final DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);

        try {
            BESSEL1841 = factory.createEllipsoid("EPSG:7004");
            GRS80 = factory.createEllipsoid("EPSG:7019");
            WGS72 = factory.createEllipsoid("EPSG:7043");
            WGS84 = factory.createEllipsoid("EPSG:7030");
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
    }
}
