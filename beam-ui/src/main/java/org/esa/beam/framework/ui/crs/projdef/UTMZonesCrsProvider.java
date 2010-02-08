package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

import javax.measure.unit.Unit;

class UTMZonesCrsProvider extends AbstractUTMCrsProvider {

    private static final String NAME = "UTM Zone";
    private static final Citation BEAM = Citations.fromName("BEAM");
    private static final String NORTH_HEMISPHERE = "North";
    private static final String SOUTH_HEMISPHERE = "South";
    private static final String ZONE_NAME = "zone";
    private static final String HEMISPHERE_NAME = "hemisphere";

    private static final ParameterDescriptor[] DESCRIPTORS = new ParameterDescriptor[]{
            new DefaultParameterDescriptor<Integer>(BEAM, ZONE_NAME, Integer.class, null, 1,
                                                    MIN_UTM_ZONE, MAX_UTM_ZONE, Unit.ONE, true),
            new DefaultParameterDescriptor<String>(HEMISPHERE_NAME, String.class,
                                                   new String[]{NORTH_HEMISPHERE, SOUTH_HEMISPHERE},
                                                   NORTH_HEMISPHERE)
    };

    private static final ParameterDescriptorGroup UTM_PARAMETERS = new DefaultParameterDescriptorGroup(NAME,
                                                                                                       DESCRIPTORS);

    UTMZonesCrsProvider(GeodeticDatum wgs84Datum) {
        super(NAME, true, true, wgs84Datum);
    }

    @Override
    public ParameterValueGroup getParameter() {
        return UTM_PARAMETERS.createValue();
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameters,
                                            GeodeticDatum datum) throws FactoryException {

        int zoneIndex = parameters.parameter(ZONE_NAME).intValue();
        String hemisphere1 = parameters.parameter(HEMISPHERE_NAME).stringValue();
        boolean south = (SOUTH_HEMISPHERE.equals(hemisphere1));

        ParameterValueGroup tmParameters = createTransverseMercatorParameters(zoneIndex, south, datum);

        return createCrs(getProjectionName(zoneIndex, south), new TransverseMercator.Provider(), tmParameters, datum);

    }

}
