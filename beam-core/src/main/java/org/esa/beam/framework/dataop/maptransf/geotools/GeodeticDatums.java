package org.esa.beam.framework.dataop.maptransf.geotools;

import org.esa.beam.framework.dataop.maptransf.Datum;
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
