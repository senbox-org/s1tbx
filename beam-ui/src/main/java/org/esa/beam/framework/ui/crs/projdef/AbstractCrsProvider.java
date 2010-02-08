package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

abstract class AbstractCrsProvider {

    private final String name;
    private final boolean hasParameters;
    private final boolean isDatumChangable;
    private final GeodeticDatum defaultDatum;

    AbstractCrsProvider(String name, boolean hasParameters,
                boolean datumChangable, GeodeticDatum defaultDatum) {
        this.name = name;
        this.hasParameters = hasParameters;
        isDatumChangable = datumChangable;
        this.defaultDatum = defaultDatum;
    }

    String getName() {
        return name;
    }

    boolean hasParameters() {
        return hasParameters;
    }

    boolean isDatumChangable() {
        return isDatumChangable;
    }

    GeodeticDatum getDefaultDatum() {
        return defaultDatum;
    }

    abstract ParameterValueGroup getParameter();

    abstract CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameter,
                                              GeodeticDatum datum) throws FactoryException;
}
