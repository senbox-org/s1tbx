package org.esa.beam.framework.dataop.maptransf.geotools;

import junit.framework.TestCase;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.esa.beam.framework.dataop.maptransf.geotools.Ellipsoids;

public class EllipsoidsTest extends TestCase {

    public void testEllipsoids() throws FactoryException {
        final DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);

        assertSame(factory.createEllipsoid("EPSG:7004"), Ellipsoids.BESSEL1841);
        assertSame(factory.createEllipsoid("EPSG:7019"), Ellipsoids.GRS80);
        assertSame(factory.createEllipsoid("EPSG:7043"), Ellipsoids.WGS72);
        assertSame(factory.createEllipsoid("EPSG:7030"), Ellipsoids.WGS84);
    }

}
