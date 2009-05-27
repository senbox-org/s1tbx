package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.framework.dataop.maptransf.Datum;
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
