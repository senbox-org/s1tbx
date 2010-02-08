package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

class WGS84CrsProvider extends AbstractCrsProvider {

    private static final String NAME = "Geographic Lat/Lon (WGS 84)";

    WGS84CrsProvider(GeodeticDatum wgs84Datum) {
        super(NAME, false, false, wgs84Datum);
    }

    @Override
    public ParameterValueGroup getParameter() {
        return ParameterGroup.EMPTY;
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameter,
                                            GeodeticDatum datum) throws FactoryException {
        return DefaultGeographicCRS.WGS84;
    }
}
