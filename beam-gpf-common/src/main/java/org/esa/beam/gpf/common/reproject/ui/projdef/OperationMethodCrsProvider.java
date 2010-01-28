package org.esa.beam.gpf.common.reproject.ui.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.parameter.Parameter;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.DefiningConversion;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;

import java.util.HashMap;
import java.util.List;

class OperationMethodCrsProvider extends AbstractCrsProvider {

    private final OperationMethod delegate;

    OperationMethodCrsProvider(OperationMethod method) {
        super(method.getName().getCode().replace("_", " "), true, true, null);
        this.delegate = method;
    }

    @Override
    public ParameterValueGroup getParameter() {
        // this call does not initialises optional values and so they are missed in our user interface
//        return delegate.getParameters().createValue();
        final ParameterDescriptorGroup descriptorGroup = delegate.getParameters();
        final List<GeneralParameterDescriptor> parameterDescriptors = descriptorGroup.descriptors();
        GeneralParameterValue[] parameterValues = new GeneralParameterValue[parameterDescriptors.size()];
        for (int i = 0, parameterDescriptorsSize = parameterDescriptors.size(); i < parameterDescriptorsSize; i++) {
            GeneralParameterDescriptor parameterDescriptor = parameterDescriptors.get(i);
            parameterValues[i] = parameterDescriptor.createValue();
            if (parameterValues[i] instanceof Parameter) {
                Parameter p = (Parameter) parameterValues[i];
                if (p.getValue() == null) {
                    final ParameterDescriptor descriptor = p.getDescriptor();
                    p.setValue(descriptor.getMinimumValue());
                }
            }
        }
        return new ParameterGroup(descriptorGroup, parameterValues);
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameters,
                                            GeodeticDatum datum) throws FactoryException {
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);

        final Conversion conversion = new DefiningConversion(AbstractIdentifiedObject.getProperties(delegate),
                                                             delegate, parameters);

        final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
        baseCrsProperties.put("name", datum.getName().getCode());
        GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties,
                                                               datum,
                                                               DefaultEllipsoidalCS.GEODETIC_2D);

        final HashMap<String, Object> projProperties = new HashMap<String, Object>();
        projProperties.put("name", getName() + " / " + datum.getName().getCode());
        return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
    }
}
