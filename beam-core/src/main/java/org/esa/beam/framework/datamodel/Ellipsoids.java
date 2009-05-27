package org.esa.beam.framework.datamodel;

import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.geotools.referencing.ReferencingFactoryFinder;

class Ellipsoids {

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
