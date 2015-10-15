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
import org.esa.snap.core.util.math.MathUtils;

import java.awt.geom.Point2D;
/**
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.LambertConformal1SP.Provider} instead.
 */
@Deprecated
public class LambertConformalConicDescriptor implements MapTransformDescriptor {

    /**
     * The type ID of this map descriptor
     */
    public final static String TYPE_ID = "Lambert_Conformal_Conic";
    /**
     * The name of this map descriptor
     */
    public final static String NAME = "Lambert Conformal Conic";
    /**
     * The map unit of this map descriptor
     */
    public final static String MAP_UNIT = "meter";

    private final static int SEMI_MAJOR_INDEX = 0;
    private final static int SEMI_MINOR_INDEX = 1;
    private final static int LATITUDE_OF_ORIGIN_INDEX = 2;
    private final static int CENTRAL_MERIDIAN_INDEX = 3;
    private final static int LATITUDE_OF_INTERSECTION_1_INDEX = 4;
    private final static int LATITUDE_OF_INTERSECTION_2_INDEX = 5;
    private final static int SCALE_FACTOR_INDEX = 6;

    public final static String[] PARAMETER_NAMES = new String[]{
            "semi_major",
            "semi_minor",
            "latitude_of_origin",
            "central_meridian",
            "latitude_of_intersection_1",
            "latitude_of_intersection_2",
            "scale_factor"
    };

    public final static double[] PARAMETER_DEFAULT_VALUES = new double[]{
            Ellipsoid.WGS_84.getSemiMajor(),
            Ellipsoid.WGS_84.getSemiMinor(),
            90.0,
            0.0,
            20.0,
            60.0,
            1.0
    };

    public final static String[] PARAMETER_LABELS = new String[]{
            "Semi major",
            "Semi minor",
            "Latitude of origin",
            "Central meridian",
            "Latitude of intersection 1",
            "Latitude of intersection 2",
            "Scale factor"
    };

    public final static String[] PARAMETER_UNITS = new String[]{
            "meter",
            "meter",
            "degree",
            "degree",
            "degree",
            "degree",
            ""
    };

    public LambertConformalConicDescriptor() {
    }


    /**
     * This method is called within the <code>{@link MapProjectionRegistry#registerDescriptor}</code> method after an
     * instance of this <code>MapTransformDescriptor</code> has been successfully registered. The method can and should
     * be used to register projections that are based on the type of <code>{@link MapTransform}</code> described by this
     * <code>MapTransformDescriptor</code>. Registering projection instances is done using the using the <code>{@link
     * MapProjectionRegistry#registerProjection}</code> method.
     * <p>
     * <p>
     * A typical implementation of this method would be:
     * <pre>
     * public void registerProjections() {
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-1", new
     * MyMapTransform(param_1)));
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-2", new
     * MyMapTransform(param_2)));
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-3", new
     * MyMapTransform(param_3)));
     *     ...
     * }
     * </pre>
     */
    public void registerProjections() {
        MapProjectionRegistry.registerProjection(new MapProjection(getName(), createTransform(null), false));
    }

    /**
     * Gets the unique type identifier for the map transformation, e.g. "Transverse_Mercator".
     */
    public String getTypeID() {
        return TYPE_ID;
    }

    /**
     * Gets a descriptive name for this map transformation descriptor.
     *
     * @see #NAME
     */
    public String getName() {
        return NAME;
    }

    /**
     * Gets the unit of the map.
     */
    public String getMapUnit() {
        return MAP_UNIT;
    }

    /**
     * Gets the list of parameters required to create an instance of the map transform.
     */
    public Parameter[] getParameters() {
        final Parameter[] parameters = new Parameter[PARAMETER_NAMES.length];

        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = new Parameter(PARAMETER_NAMES[i], PARAMETER_DEFAULT_VALUES[i]);
            parameters[i].getProperties().setLabel(PARAMETER_LABELS[i]);
            parameters[i].getProperties().setPhysicalUnit(PARAMETER_UNITS[i]);
        }

        return parameters;
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
     * Tests if a user interface is available. Returns <code>true</code> because a user interface is available for this
     * descriptor.
     *
     * @return always <code>true</code>
     */
    public boolean hasTransformUI() {
        return true;
    }

    /**
     * Gets a user interface for editing the transformation properties of a map projection. Never returns
     * <code>null</code> because a user interface is available for this descriptor.
     *
     * @param transform the transformation which provides the default properties for the UI.
     *
     * @return the transformation UI, never null
     */
    public MapTransformUI getTransformUI(MapTransform transform) {
        return new DefaultMapTransformUI(transform);
    }

    /**
     * Creates an instance of the map transform for the given parameter values.
     */
    public MapTransform createTransform(double[] parameterValues) {
        if (parameterValues == null) {
            parameterValues = getParameterDefaultValues();
        }
        return new LCCT(parameterValues);
    }

    /** 
     * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.MapProjection} instead.
     */
    @Deprecated
    public class LCCT extends CartographicMapTransform {

        private static final double _epsilon = 1e-10;
        private double[] _parameterValues;
        private double _n;
        private double _invN;
        private double _rho0;
        private double _e; //@todo se - excentizitaet
        private double _c;
        private double _invC;
        private double _k0;
        private double _invK0;

        /**
         * Constructs the MapTransform with given parameter set. The parameter are in indexed order: <ul> <li>0 - Semi
         * major axis</li> <li>1 - Semi minor axis</li> <li>2 - Latitude of origin</li> <li>3 - Latitude of intersection
         * 1</li> <li>4 - Latitude of intersection 2</li> <li>5 - Scale factor</li> </ul>
         *
         * @param parameterValues
         */
        public LCCT(double[] parameterValues) {
            super(parameterValues[LambertConformalConicDescriptor.CENTRAL_MERIDIAN_INDEX],
                  0.0, 0.0,
                  parameterValues[LambertConformalConicDescriptor.SEMI_MAJOR_INDEX]);

            _parameterValues = new double[parameterValues.length];
            System.arraycopy(parameterValues, 0, _parameterValues, 0, parameterValues.length);
            double temp;
            double phi0 = MathUtils.DTOR * _parameterValues[LambertConformalConicDescriptor.LATITUDE_OF_ORIGIN_INDEX];
            double phi1 = MathUtils.DTOR * _parameterValues[LambertConformalConicDescriptor.LATITUDE_OF_INTERSECTION_1_INDEX];
            double phi2 = MathUtils.DTOR * _parameterValues[LambertConformalConicDescriptor.LATITUDE_OF_INTERSECTION_2_INDEX];
            if (Math.abs(phi1 + phi2) < _epsilon) {
                throw new IllegalArgumentException("Invalid parameter set.");
            }

            double sinPhi = Math.sin(phi1);
            _n = sinPhi;
            double cosPhi = Math.cos(phi1);

            boolean isSecant = (Math.abs(phi1 - phi2) >= _epsilon);

            double a = _parameterValues[LambertConformalConicDescriptor.SEMI_MAJOR_INDEX]; // a
            double b = _parameterValues[LambertConformalConicDescriptor.SEMI_MINOR_INDEX]; // b
            double es = 1.0 - (b * b) / (a * a);
            _e = Math.sqrt(es);
            double m1 = MapTransformUtils.msfn(sinPhi, cosPhi, es);
            double ml1 = MapTransformUtils.tsfn(phi1, sinPhi, _e);

            if (isSecant) {
                temp = MapTransformUtils.msfn(Math.sin(phi2), Math.cos(phi2), es);
                _n = Math.log(m1 / temp);
                temp = MapTransformUtils.tsfn(phi2, Math.sin(phi2), _e);
                _n = _n / Math.log(ml1 / temp);
            }

            _invN = 1.0 / _n;
            _c = m1 * Math.pow(ml1, -_n) * _invN;
            _invC = 1.0 / _c;
            _rho0 = 0.0;

            if (Math.abs(Math.abs(phi0) - MathUtils.HALFPI) >= _epsilon) {
                temp = MapTransformUtils.tsfn(phi0, Math.sin(phi0), _e);
                _rho0 = _c * Math.pow(temp, _n);
            }

            _k0 = _parameterValues[LambertConformalConicDescriptor.SCALE_FACTOR_INDEX];
            _invK0 = 1.0 / _k0;
        }

        /**
         * Returns the descriptor for this map transform.
         *
         * @return the descriptor, should never be <code>null</code>
         */
        public MapTransformDescriptor getDescriptor() {
            return LambertConformalConicDescriptor.this;
        }

        /**
         * Creates a deep clone of this <code>MapTransform</code>.
         *
         * @return a <code>MapTransform</code> clone
         */
        public MapTransform createDeepClone() {
            return new LCCT(_parameterValues);
        }

        /**
         * Returns the array of parameter values. The order in which the parameters are returned must exactly match the
         * order in which the corresponding {@link Parameter} array is returned by the <code>{@link
         * MapTransformDescriptor#getParameters()}</code> method.
         */
        public double[] getParameterValues() {
            final double[] values = new double[_parameterValues.length];
            System.arraycopy(_parameterValues, 0, values, 0, values.length);

            return values;
        }

        /**
         * Worker method to be overridden by derived class. Performs the pure transformation. Prescaling, northing,
         * easting etc is calculated in this class.
         *
         * @param lat      latitude of source location
         * @param lon      longitude of source location
         * @param mapPoint
         *
         * @return the map co-ordinate
         */
        @Override
        protected Point2D forward_impl(double lat, double lon, Point2D mapPoint) {
            final double phi = MathUtils.DTOR * lat;
            double lam = MathUtils.DTOR * lon;
            double rho;

            if (Math.abs(Math.abs(phi) - MathUtils.HALFPI) < _epsilon) {
                if ((phi * _n) < 0.0) {
                    throw new IllegalArgumentException("Invalid parameter range");
                }
                rho = 0.0;
            } else {
                double temp = MapTransformUtils.tsfn(phi, Math.sin(phi), _e);
                rho = _c * Math.pow(temp, _n);
            }

            lam = lam * _n;
            double tempX = _k0 * (rho * Math.sin(lam));
            double tempY = _k0 * (_rho0 - rho * Math.cos(lam));

            mapPoint.setLocation(tempX, tempY);
            return mapPoint;
        }

        /**
         * Worker method to be overridden by derived class. Performs the pure transformation. Prescaling, northing,
         * easting etc is calculated in this class.
         * <p>Should be overridden in order to delegate to <code>{@link #inverse_impl(double, double,
         * GeoPos)}</code> if transformation is performed is in 64-bit accuracy.
         * Override <code>{@link #inverse_impl(double, double, GeoPos)}</code> instead
         * in order to perform the actual transformation.
         *
         * @param geoPoint
         * @param x        map x coordinate
         * @param y        map y coordinate
         *
         * @return the geodetic co-ordinate
         */
        @Override
        protected GeoPos inverse_impl(double x, double y, GeoPos geoPoint) {
            double localX = x * _invK0;
            double localY = y * _invK0;
            double rho;
            double temp;

            localY = _rho0 - localY;
            rho = Math.sqrt(localX * localX + localY * localY);
            if (rho != 0.0) {
                if (_n < 0.f) {
                    rho = -rho;
                    localX = -localX;
                    localY = -localY;
                }
                temp = Math.pow(rho * _invC, _invN);
                geoPoint.lat = MapTransformUtils.phi2(temp, _e);
                geoPoint.lon = Math.atan2(localX, localY) * _invN;
            } else {
                geoPoint.lat = 0.f;
                if (_n > 0) {
                    geoPoint.lon = (float) MathUtils.HALFPI;
                } else {
                    geoPoint.lon = (float) -MathUtils.HALFPI;
                }
            }

            geoPoint.lat = geoPoint.lat * MathUtils.RTOD_F;
            geoPoint.lon = geoPoint.lon * MathUtils.RTOD_F;
            return geoPoint;
        }

        public double getSemiMinor() {
            return _parameterValues[SEMI_MINOR_INDEX];
        }

        public double getLatitudeOfOrigin() {
            return _parameterValues[LATITUDE_OF_ORIGIN_INDEX];
        }

        public double getScaleFactor() {
            return _parameterValues[SCALE_FACTOR_INDEX];
        }

        public double getStandardParallel1() {
            return _parameterValues[LATITUDE_OF_INTERSECTION_1_INDEX];
        }

        public double getStandardParallel2() {
            return _parameterValues[LATITUDE_OF_INTERSECTION_2_INDEX];
        }
    }

}
