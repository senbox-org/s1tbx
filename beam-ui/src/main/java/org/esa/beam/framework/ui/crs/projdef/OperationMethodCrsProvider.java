/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.DefaultOperationMethod;
import org.geotools.referencing.operation.DefiningConversion;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;

import java.util.HashMap;

class OperationMethodCrsProvider extends AbstractCrsProvider {

    final OperationMethod delegate;

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
        // in some cases, depending on the parameters set, the effective transformation can be different
        // from the transformation given by the OperationMethod.
        // So we create a new one
        final MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final MathTransform transform = mtFactory.createParameterizedTransform(parameters);
        final DefaultOperationMethod operationMethod = new DefaultOperationMethod(transform);

        final Conversion conversion = new DefiningConversion(AbstractIdentifiedObject.getProperties(operationMethod),
                                                             operationMethod, transform);

        final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
        baseCrsProperties.put("name", datum.getName().getCode());
        GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties,
                                                               datum,
                                                               DefaultEllipsoidalCS.GEODETIC_2D);

        final HashMap<String, Object> projProperties = new HashMap<String, Object>();
        projProperties.put("name", conversion.getName().getCode() + " / " + datum.getName().getCode());
        return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
    }
}
