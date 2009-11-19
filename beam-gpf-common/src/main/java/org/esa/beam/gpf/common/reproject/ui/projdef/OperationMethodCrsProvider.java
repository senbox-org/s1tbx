package org.esa.beam.gpf.common.reproject.ui.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;

import java.util.HashMap;

class OperationMethodCrsProvider extends AbstractCrsProvider {

    private final OperationMethod delegate;

    OperationMethodCrsProvider(OperationMethod method) {
        super(method.getName().getCode().replace("_", " "), true, true, null);
        this.delegate = method;
    }

    @Override
    public ParameterValueGroup getParameter() {
        return delegate.getParameters().createValue();
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameters,
                                            GeodeticDatum datum) throws FactoryException {
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
        final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);

        final HashMap<String, Object> projProperties = new HashMap<String, Object>();
        projProperties.put("name", getName() + " / " + datum.getName().getCode());
        final Conversion conversion = coFactory.createDefiningConversion(projProperties, delegate, parameters);
        final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
        baseCrsProperties.put("name", datum.getName().getCode());
        final GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties, datum,
                                                                     DefaultEllipsoidalCS.GEODETIC_2D);
        return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
    }
}
