package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

class UTMAutomaticCrsProvider extends AbstractUTMCrsProvider {

    private static final String NAME = "UTM / WGS 84 (Automatic)";

    UTMAutomaticCrsProvider(GeodeticDatum wgs84Datum) {
        super(NAME, false, false, wgs84Datum);
    }

    @Override
    public ParameterValueGroup getParameter() {
        return ParameterGroup.EMPTY;
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameters,
                                            GeodeticDatum datum) throws FactoryException {
        int zoneIndex = getZoneIndex(referencePos.getLon());
        final boolean south = referencePos.getLat() < 0.0;
        ParameterValueGroup tmParameters = createTransverseMercatorParameters(zoneIndex, south, datum);
        final String projName = getProjectionName(zoneIndex, south);

        return createCrs(projName, new TransverseMercator.Provider(), tmParameters, datum);
    }
}
