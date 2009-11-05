package org.esa.beam.gpf.common.reproject.ui.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
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
    public CoordinateReferenceSystem getCRS(Product product, ParameterValueGroup parameters,
                                            GeodeticDatum datum) throws FactoryException {
        if (product == null) {
            return null;
        }
        final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(product);
        int zoneIndex = getZoneIndex(centerGeoPos.getLon());
        final boolean south = centerGeoPos.getLat() < 0.0;
        ParameterValueGroup tmParameters = createTransverseMercatorParameters(zoneIndex, south, datum);
        final String projName = getProjectionName(zoneIndex, south);

        return createCrs(projName, new TransverseMercator.Provider(), tmParameters, datum);
    }
}
