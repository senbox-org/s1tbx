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
package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.param.Parameter;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * A map transform which implements an affine transformation.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.transform.ProjectiveTransform.ProviderAffine} instead.
 */
@Deprecated
public class AffineTransformDescriptor implements MapTransformDescriptor {

    public static final String TYPE_ID = "Affine";
    public static final String NAME = TYPE_ID;
    public static final String MAP_UNIT = "degree";
    private static final String[] PARAMETER_NAMES = new String[]{
        "scale_x",
        "shear_y",
        "shear_x",
        "scale_y",
        "translate_x",
        "translate_y"
    };
    private static final double[] PARAMETER_DEFAULT_VALUES = new double[]{
        1,
        0,
        0,
        1,
        0,
        0
    };

    public void registerProjections() {
    }

    public AffineTransformDescriptor() {
    }

    public String getTypeID() {
        return TYPE_ID;
    }

    /**
     * Gets a descriptive name for this map transformation descriptor, e.g. "Transverse Mercator".
     */
    public String getName() {
        return NAME;
    }

    public String getMapUnit() {
        return MAP_UNIT;
    }

    /**
     * Gets the default parameter values for this map transform.
     */
    public double[] getParameterDefaultValues() {
    	final double[] values = new double[PARAMETER_DEFAULT_VALUES.length];
    	System.arraycopy(PARAMETER_DEFAULT_VALUES, 0, values, 0, values.length);
    	
        return values;
    }

    /**
     * Gets the list of parameters required to create an
     * instance of the map transformation.
     */
    public Parameter[] getParameters() {
    	final Parameter[] parameters = new Parameter[PARAMETER_NAMES.length];
        
    	for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = new Parameter(PARAMETER_NAMES[i], PARAMETER_DEFAULT_VALUES[i]);
        }
    	
        return parameters;
    }

    /**
     * Tests if a user interface is available. Returns <code>false</code> because a user interface is not available for
     * this descriptor.
     *
     * @return always <code>false</code>
     */
    public boolean hasTransformUI() {
        return false;
    }

    /**
     * Gets a user interface for editing the transformation properties of a map projection. Returns <code>null</code>
     * because a user interface is not available for this descriptor.
     *
     * @param transform ignored
     *
     * @return always <code>null</code>
     */
    public MapTransformUI getTransformUI(MapTransform transform) {
        return null;
    }

    public MapTransform createTransform(double[] parameterValues) {
        if (parameterValues == null) {
            parameterValues = getParameterDefaultValues();
        }
        return new AMT(parameterValues);
    }

    private class AMT implements MapTransform {

        private AffineTransform _forward;
        private AffineTransform _inverse;
        private final Point2D.Double _temp = new Point2D.Double();

        private AMT(double[] parameterValues) {
            _forward = new AffineTransform(parameterValues);
            try {
                _inverse = _forward.createInverse();
            } catch (NoninvertibleTransformException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        public MapTransformDescriptor getDescriptor() {
            return AffineTransformDescriptor.this;
        }

        public double[] getParameterValues() {
            double[] values = new double[6];
            _forward.getMatrix(values);
            return values;
        }

        /**
         * Forward project geographical co-ordinates into map co-ordinates.
         */
        public Point2D forward(GeoPos geoPoint, Point2D mapPoint) {
            if (mapPoint == null) {
                mapPoint = new Point2D.Float();
            }
            _temp.x = geoPoint.lon;
            _temp.y = geoPoint.lat;
            _forward.transform(_temp, mapPoint);
            return mapPoint;
        }

        /**
         * Inverse project map co-ordinates into geographical co-ordinates.
         */
        public GeoPos inverse(Point2D mapPoint, GeoPos geoPoint) {
            if (geoPoint == null) {
                geoPoint = new GeoPos();
            }
            _inverse.transform(mapPoint, _temp);
            geoPoint.lon = _temp.x;
            geoPoint.lat = _temp.y;
            return geoPoint;
        }

        public MapTransform createDeepClone() {
            double[] flatmatrix = new double[6];
            _forward.getMatrix(flatmatrix);
            return new AMT(flatmatrix);
        }
    }
}
