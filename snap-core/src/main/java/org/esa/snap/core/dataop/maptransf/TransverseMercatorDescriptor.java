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
 * The descriptor for a map transformation which implements the Transverse Mercator transformation. The code basis for
 * this transformation has taken from the proj4 map projection API (http://remotesensing.org/proj/).
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.TransverseMercator.Provider} instead.
 */
@Deprecated
public class TransverseMercatorDescriptor implements MapTransformDescriptor {

    public static final String TYPE_ID = "Transverse_Mercator";
    public static final String NAME = "Transverse Mercator";
    public static final String MAP_UNIT = "meter";

    public final static String[] PARAMETER_NAMES = new String[]{
        "semi_major",
        "semi_minor",
        "latitude_of_origin",
        "central_meridian",
        "scale_factor",
        "false_easting",
        "false_northing"
    };

    private static final double[] PARAMETER_DEFAULT_VALUES = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi_major (meters)
        Ellipsoid.WGS_84.getSemiMinor(), // semi_minor (meters)
        0.0, // latitude_of_origin (degree)
        0.0, // central_meridian (degree)
        0.9996, //  scaling_factor(no unit)
        0.0, // false_easting (meters)
        0.0      // false_northing (meters)
    };

    public final static String[] PARAMETER_LABELS = new String[]{
        "Semi major",
        "Semi minor",
        "Latitude of origin",
        "Central meridian",
        "Scale factor",
        "False easting",
        "False northing",
    };

    public final static String[] PARAMETER_UNITS = new String[]{
        "meter",
        "meter",
        "degree",
        "degree",
        "",
        "meter",
        "meter"
    };


    private static final int SEMI_MAJOR_INDEX = 0;
    private static final int SEMI_MINOR_INDEX = 1;
    private static final int LATITUDE_OF_ORIGIN_INDEX = 2;
    private static final int CENTRAL_MERIDIAN_INDEX = 3;
    private static final int SCALE_FACTOR_INDEX = 4;
    private static final int FALSE_EASING_INDEX = 5;
    private static final int FALSE_NORTHING_INDEX = 6;

    public TransverseMercatorDescriptor() {
    }

    /**
     * This method is called within the <code>{@link MapProjectionRegistry#registerDescriptor}</code>
     * method after an instance of this <code>MapTransformDescriptor</code> has been successfully registered.
     * <p>
     * The method delegates the call to <code>{@link UTM#registerProjections}</code>
     * in order to register all frequently used UTM projections.
     */
    @Override
    public void registerProjections() {
        UTM.registerProjections();
        MapProjectionRegistry.registerProjection(new MapProjection(getName(), createTransform(null), false));
    }

    /**
     * Retrieves the type identifier for this transform.
     */
    @Override
    public String getTypeID() {
        return TYPE_ID;
    }

    /**
     * Gets a descriptive name for this map transformation descriptor, e.g. "Transverse Mercator".
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Retrieves the unit of this transform.
     */
    @Override
    public String getMapUnit() {
        return MAP_UNIT;
    }

    /**
     * Gets the default parameter values for this map transform.
     */
    @Override
    public double[] getParameterDefaultValues() {
    	final double[] values = new double[PARAMETER_DEFAULT_VALUES.length];
    	System.arraycopy(PARAMETER_DEFAULT_VALUES, 0, values, 0, values.length);
    	
        return values;
    }

    /**
     * Gets the parameter vector for this transform.
     */
    @Override
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
     * Tests if a user interface is available. Returns <code>true</code> because a user interface is available for this
     * descriptor.
     *
     * @return always <code>true</code>
     */
    @Override
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
    @Override
    public MapTransformUI getTransformUI(MapTransform transform) {
        return new DefaultMapTransformUI(transform);
    }


    /**
     * Creates the associated transform.
     *
     * @param parameterValues the parameters needed by the transform
     */
    @Override
    public MapTransform createTransform(double[] parameterValues) {
        if (parameterValues == null) {
            parameterValues = PARAMETER_DEFAULT_VALUES;
        }
        return new TMT(parameterValues);
    }

    /**
     * Implements the transverse mercator map-transformation.
     * 
     * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.MapProjection} instead.
     */
    @Deprecated
    public class TMT extends CartographicMapTransform {

        private static final double _epsilon = 1.0e-10;
        private static final double _fc1 = 1.0;
        private static final double _fc2 = 0.5;
        private static final double _fc3 = 0.16666666666666666666;
        private static final double _fc4 = 0.08333333333333333333;
        private static final double _fc5 = 0.05;
        private static final double _fc6 = 0.03333333333333333333;
        private static final double _fc7 = 0.02380952380952380952;
        private static final double _fc8 = 0.01785714285714285714;
        private static final double _halfPi = 0.5 * Math.PI;

        private final double[] _parameterValues;
        private final double _es;
        private final double _esp;
        private final double _k0;
        private final double _invK0;
        private final double _ml0;
        private final double[] _en;

        public TMT(double[] parameterValues) {
            super(parameterValues[CENTRAL_MERIDIAN_INDEX], // lam0
                  parameterValues[FALSE_EASING_INDEX], // x0
                  parameterValues[FALSE_NORTHING_INDEX], // y0
                  parameterValues[SEMI_MAJOR_INDEX]);  // a
            double a = parameterValues[SEMI_MAJOR_INDEX]; // a
            double b = parameterValues[SEMI_MINOR_INDEX]; // b
            _k0 = parameterValues[SCALE_FACTOR_INDEX]; // k0
            _invK0 = 1.0 / _k0;
            double phi0 = parameterValues[LATITUDE_OF_ORIGIN_INDEX]; // phi0

            _es = 1.0 - (b * b) / (a * a);
            _esp = _es / (1.0 - _es);

            _en = MapTransformUtils.getLengthParams(_es);

            phi0 = MathUtils.DTOR * phi0;
            _ml0 = MapTransformUtils.meridLength(phi0, Math.sin(phi0), Math.cos(phi0), _en);

            _parameterValues = new double[parameterValues.length];
            System.arraycopy(parameterValues, 0, _parameterValues, 0, parameterValues.length);
        }

        @Override
        public MapTransformDescriptor getDescriptor() {
            return TransverseMercatorDescriptor.this;
        }

        @Override
        public double[] getParameterValues() {
        	final double[] values = new double[_parameterValues.length];
        	System.arraycopy(_parameterValues, 0, values, 0, values.length);
        	
            return values;
        }

        /**
         * Forward project geographical co-ordinates into map co-ordinates.
         */
        @Override
        public Point2D forward_impl(double lat, double lon, Point2D mapPoint) {
            double phi = MathUtils.DTOR * lat;
            double lam = MathUtils.DTOR * lon;
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);
            double t = 0.0;
            double a1;
            double a1s;
            double n;
            double ml;

            if (Math.abs(cosPhi) > _epsilon) {
                t = sinPhi / cosPhi;
                t *= t;
            }

            a1 = cosPhi * lam;
            a1s = a1 * a1;
            a1 /= Math.sqrt(1.0 - _es * sinPhi * sinPhi);
            n = _esp * cosPhi * cosPhi;
            double tempX = _k0 * a1 * (_fc1 + _fc3 * a1s * (1.0 - t + n + _fc5 * a1s *
                    (5.0 + t * (t - 18.0) + n * (14.0 - 58.0 * t) + _fc7 * a1s *
                            (61.0 + t * (t * (179.0 - t) - 479.0)))));
            ml = MapTransformUtils.meridLength(phi, sinPhi, cosPhi, _en);
            double tempY = _k0 * (ml - _ml0 + sinPhi * a1 * lam * _fc2 *
                    (1.0 + _fc4 * a1s * (5.0 - t + n * (9.0 + 4.0 * n) + _fc6 * a1s *
                            (61.0 + t * (t - 58.0) + n * (270.0 - 330.0 * t) + _fc8 * a1s *
                                    (1385.0 + t * (t * (543.0 - t) - 3111.0))))));

            mapPoint.setLocation(tempX, tempY);
            return mapPoint;
        }

        /**
         * Inverse project map co-ordinates into geographical co-ordinates.
         */
        @Override
        public GeoPos inverse_impl(double x, double y, GeoPos geoPoint) {
            double _tempX = MapTransformUtils.invMeridLength(_ml0 + y * _invK0, _es, _en);

            double _tempY;
            if (Math.abs(_tempX) >= _halfPi) {
                if (y < 0) {
                    _tempX = -_halfPi;
                } else {
                    _tempX = _halfPi;
                }
                _tempY = 0.0;
            } else {
                double sinPhi = Math.sin(_tempX);
                double cosPhi = Math.cos(_tempX);
                double t = 0.0;
                double n;
                double d;
                double ds;
                double con;

                if (Math.abs(cosPhi) > _epsilon) {
                    t = sinPhi / cosPhi;
                }
                n = _esp * cosPhi * cosPhi;
                con = 1.0 - _es * sinPhi * sinPhi;
                d = x * Math.sqrt(con) * _invK0;
                con *= t;
                t *= t;
                ds = d * d;
                _tempX -= (con * ds / (1.0 - _es)) * _fc2 * (1.0 - ds * _fc4 *
                                                                   (5.0 + t * (3.0 - 9.0 * n) - ds * _fc6 * (61.0 + t *
                                                                                                                    (90.0 - 252.0 * n + 45.0 * t) + 46.0 * n - ds * _fc8 *
                                                                                                                                                               (1385.0 + t * (3633.0 + t * (4095.0 + 1574.0 * t))))));
                _tempY = d * (_fc1 - ds * _fc3 * (1.0 + 2.0 * t + n - ds * _fc5 *
                                                                      (5.0 + t * (28.0 + 24.0 * t + 8.0 * n) + 6.0 * n - ds * _fc7 *
                                                                                                                         (61.0 + t * (662.0 + t * (1320.0 + 720 * t))))));
                _tempY /= cosPhi;
            }
            geoPoint.setLocation(MathUtils.RTOD * _tempX, MathUtils.RTOD * _tempY);
            return geoPoint;
        }

        @Override
        public MapTransform createDeepClone() {
            return new TMT(_parameterValues);
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
    }
}

// @todo 3 nf/nf - implement ellipsoid transformation
// The following Gauss-Kr�ger projections can't be used because we currently do not support
// WGS84 --> BESSEL ellipsoid transformation.
//
//        MapTransform transform;
//
//        double[] params = new double[]{Ellipsoid.BESSEL.getSemiMajor(), // semi_major
//                                       Ellipsoid.BESSEL.getSemiMinor(), // semi_minor
//                                       0.0, // latitude_of_origin (not used)
//                                       6.0, // central_meridian
//                                       1.0, // scale_factor
//                                       2500000.0, // false_easting
//                                       0.0        // false_northing
//        };
//        transform = MapTransformFactory.createTransform("Transverse_Mercator", params);
//        registerProjection(new MapProjection("Gauss-Kr�ger Zone 2", transform));
//
//        params = new double[]{Ellipsoid.BESSEL.getSemiMajor(), // semi_major
//                              Ellipsoid.BESSEL.getSemiMinor(), // semi_minor
//                              0.0, // latitude_of_origin (not used)
//                              9.0, // central_meridian
//                              1.0, // scale_factor
//                              3500000.0, // false_easting
//                              0.0        // false_northing
//        };
//        transform = MapTransformFactory.createTransform("Transverse_Mercator", params);
//        registerProjection(new MapProjection("Gauss-Kr�ger Zone 3", transform));
//
//        params = new double[]{Ellipsoid.BESSEL.getSemiMajor(), // semi_major
//                              Ellipsoid.BESSEL.getSemiMinor(), // semi_minor
//                              0.0,  // latitude_of_origin (not used)
//                              12.0, // central_meridian
//                              1.0,  // scale_factor
//                              4500000.0, // false_easting
//                              0.0        // false_northing
//        };
//        transform = MapTransformFactory.createTransform("Transverse_Mercator", params);
//        registerProjection(new MapProjection("Gauss-Kr�ger Zone 4", transform));
//
//        params = new double[]{Ellipsoid.BESSEL.getSemiMajor(), // semi major
//                              Ellipsoid.BESSEL.getSemiMinor(), // semi minor
//                              0.0,  // latitude_of_origin (not used)
//                              15.0, // central_meridian
//                              1.0,  // scale
//                              5500000.0, // false_easting
//                              0.0        // false_northing
//        };
//        transform = MapTransformFactory.createTransform("Transverse_Mercator", params);
//        registerProjection(new MapProjection("Gauss-Kr�ger Zone 5", transform));
