/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
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

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
abstract class AbstractUTMCrsProvider extends AbstractCrsProvider {

    static final int MIN_UTM_ZONE = 1;
    static final int MAX_UTM_ZONE = 60;

    AbstractUTMCrsProvider(String name, boolean hasParameters, boolean datumChangable,
                                  GeodeticDatum defaultDatum) {
        super(name, hasParameters, datumChangable, defaultDatum);
    }

    static String getProjectionName(int zoneIndex, boolean south) {
        return "UTM Zone " + zoneIndex + (south ? ", South" : "");
    }

    static int getZoneIndex(float longitude) {
        final float zoneIndex = ((longitude + 180.0f) / 6.0f - 0.5f) + 1;
        return MathUtils.roundAndCrop(zoneIndex, MIN_UTM_ZONE, MAX_UTM_ZONE);
    }

    private static void setValue(ParameterValueGroup values, ParameterDescriptor<Double> descriptor, double value) {
        values.parameter(descriptor.getName().getCode()).setValue(value);
    }

    protected static double getCentralMeridian(int zoneIndex) {
        return (zoneIndex - 0.5) * 6.0 - 180.0;
    }


    ParameterValueGroup createTransverseMercatorParameters(int zoneIndex, boolean south,
                                                                     GeodeticDatum datum) {
        ParameterDescriptorGroup tmParameters = new TransverseMercator.Provider().getParameters();
        ParameterValueGroup tmValues = tmParameters.createValue();

        setValue(tmValues, MapProjection.AbstractProvider.SEMI_MAJOR, datum.getEllipsoid().getSemiMajorAxis());
        setValue(tmValues, MapProjection.AbstractProvider.SEMI_MINOR, datum.getEllipsoid().getSemiMinorAxis());
        setValue(tmValues, MapProjection.AbstractProvider.LATITUDE_OF_ORIGIN, 0.0);
        setValue(tmValues, MapProjection.AbstractProvider.CENTRAL_MERIDIAN, getCentralMeridian(zoneIndex));
        setValue(tmValues, MapProjection.AbstractProvider.SCALE_FACTOR, 0.9996);
        setValue(tmValues, MapProjection.AbstractProvider.FALSE_EASTING, 500000.0);
        setValue(tmValues, MapProjection.AbstractProvider.FALSE_NORTHING, south ? 10000000.0 : 0.0);
        return tmValues;
    }

    CoordinateReferenceSystem createCrs(String crsName, OperationMethod method,
                                                  ParameterValueGroup parameters,
                                                  GeodeticDatum datum) throws FactoryException {
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
        final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);
        final HashMap<String, Object> projProperties = new HashMap<String, Object>();
        projProperties.put("name", crsName + " / " + datum.getName().getCode());
        final Conversion conversion = coFactory.createDefiningConversion(projProperties,
                                                                         method,
                                                                         parameters);
        final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
        baseCrsProperties.put("name", datum.getName().getCode());
        final GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties, datum,
                                                                     DefaultEllipsoidalCS.GEODETIC_2D);
        return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
    }
}
