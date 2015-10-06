/*
 * $Id: AlbersEqualAreaConicDescriptor.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2005 by Roman Gerlach (Friedrich-Schiller-University Jena, Germany)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * Method adopted from Snyder,J.P. (1987): Map Projections - A Working 
 * Manual. USGS Professional Paper 1395.
 */

package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.param.Parameter;

import java.awt.geom.Point2D;

/*** 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.AlbersEqualArea.Provider} instead.
 */
@Deprecated
public class AlbersEqualAreaConicDescriptor implements MapTransformDescriptor {

    /**
     * The type ID of this map descriptor
     */
    public final static String TYPE_ID = "Albers_Equal_Area_Conic";
    /**
     * The name of this map descriptor
     */
    public final static String NAME = "Albers Equal Area Conic";
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
    private final static int FALSE_EASTING_INDEX = 7;
    private final static int FALSE_NORTHING_INDEX = 8;

    public final static String[] PARAMETER_NAMES = new String[]{
        "semi_major",
        "semi_minor",
        "latitude_of_origin",
        "central_meridian",
        "latitude_of_intersection_1",
        "latitude_of_intersection_2",
        "scale_factor",
        "false_easting",
        "false_northing"
    };

    /**
     * The default parameters of this map descriptor
     * <p>
     * NOTE: These default values are specific to the SIBERIA-II project and
     * should be altered accordingly for other areas of interest.
     */
    public final static double[] PARAMETER_DEFAULT_VALUES = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(),
        Ellipsoid.WGS_84.getSemiMinor(),
        50.0,
        99.0,
        56.0,
        73.0,
        1.0,
        1000000.0,
        0.0
    };

    public final static String[] PARAMETER_LABELS = new String[]{
        "Semi major",
        "Semi minor",
        "Latitude of origin",
        "Central meridian",
        "Latitude of intersection 1",
        "Latitude of intersection 2",
        "Scale factor",
        "False easting",
        "False northing"
    };

    public final static String[] PARAMETER_UNITS = new String[]{
        "meter",
        "meter",
        "degree",
        "degree",
        "degree",
        "degree",
        "",
        "meter",
        "meter"
    };

    public AlbersEqualAreaConicDescriptor() {
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
        MapProjectionRegistry.registerProjection(
                new MapProjection(getName(), createTransform(null), getMapUnit(), false));
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
        return new AEAC(parameterValues);
    }

    /** 
     * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.MapProjection} instead.
     */
    @Deprecated
    public class AEAC extends CartographicMapTransform {

        private static final double eps10 = 1e-10;
        private double[] _parameterValues;
        private double _n = 0.0;
        private double _rho0 = 0.0;
        private double e;
        private double _c;
        private double es;
        private double _a;
        private double _lam0;


        /**
         * Constructs the MapTransform with given parameter set. The parameter are in indexed order: <ul> <li>0 - Semi
         * major axis</li> <li>1 - Semi minor axis</li> <li>2 - Latitude of origin</li> <li>3 - Latitude of intersection
         * 1</li> <li>4 - Latitude of intersection 2</li> <li>5 - Scale factor</li> </ul>
         *
         * @param parameterValues
         */
        public AEAC(double[] parameterValues) {
            super(0.0,
                  parameterValues[AlbersEqualAreaConicDescriptor.FALSE_EASTING_INDEX],
                  parameterValues[AlbersEqualAreaConicDescriptor.FALSE_NORTHING_INDEX],
                  1.0);

            _parameterValues = new double[parameterValues.length];
            System.arraycopy(parameterValues, 0, _parameterValues, 0, parameterValues.length);

            double phi0 = _parameterValues[AlbersEqualAreaConicDescriptor.LATITUDE_OF_ORIGIN_INDEX];
            double phi1 = _parameterValues[AlbersEqualAreaConicDescriptor.LATITUDE_OF_INTERSECTION_1_INDEX];
            double phi2 = _parameterValues[AlbersEqualAreaConicDescriptor.LATITUDE_OF_INTERSECTION_2_INDEX];
            double lam0 = _parameterValues[AlbersEqualAreaConicDescriptor.CENTRAL_MERIDIAN_INDEX];

            double _phi0 = Math.toRadians(phi0);
            double _phi1 = Math.toRadians(phi1);
            double _phi2 = Math.toRadians(phi2);
            _lam0 = Math.toRadians(lam0);


            if (Math.abs(_phi1 + _phi2) < eps10) {
                throw new IllegalArgumentException("Invalid parameter set.");
            }

            _a = _parameterValues[AlbersEqualAreaConicDescriptor.SEMI_MAJOR_INDEX];
            double b = _parameterValues[AlbersEqualAreaConicDescriptor.SEMI_MINOR_INDEX];
            es = 1.0 - (b * b) / (_a * _a);
            e = Math.sqrt(es);

            final double sinPhi0 = Math.sin(_phi0);
            final double sinPhi1 = Math.sin(_phi1);
            final double sinPhi2 = Math.sin(_phi2);

            double _q0 = (1.0 - es) * ((sinPhi0 / (1.0 - (es * sinPhi0 * sinPhi0))) - ((1.0 / (2 * e)) * Math.log(
                    (1.0 - (e * sinPhi0)) / (1.0 + (e * sinPhi0)))));
            double _q1 = (1.0 - es) * ((sinPhi1 / (1.0 - (es * sinPhi1 * sinPhi1))) - ((1.0 / (2 * e)) * Math.log(
                    (1.0 - (e * sinPhi1)) / (1.0 + (e * sinPhi1)))));
            double _q2 = (1.0 - es) * ((sinPhi2 / (1.0 - (es * sinPhi2 * sinPhi2))) - ((1.0 / (2 * e)) * Math.log(
                    (1.0 - (e * sinPhi2)) / (1.0 + (e * sinPhi2)))));
            double m1 = Math.cos(_phi1) / Math.sqrt(1.0 - (es * sinPhi1 * sinPhi1));
            double m2 = Math.cos(_phi2) / Math.sqrt(1.0 - (es * sinPhi2 * sinPhi2));

            boolean isSecant = (Math.abs(_phi1 - _phi2) >= eps10);
            if (isSecant) {
                _n = (Math.pow(m1, 2) - Math.pow(m2, 2)) / (_q2 - _q1);
            }

            _c = Math.pow(m1, 2) + (_n * _q1);

            if (Math.abs(Math.abs(_phi0) - (Math.PI / 2)) >= eps10) {
                _rho0 = (_a * (Math.sqrt(_c - (_n * _q0)))) / _n;
            }

        }

        /**
         * Returns the descriptor for this map transform.
         *
         * @return the descriptor, should never be <code>null</code>
         */
        public MapTransformDescriptor getDescriptor() {
            return AlbersEqualAreaConicDescriptor.this;
        }

        /**
         * Creates a deep clone of this <code>MapTransform</code>.
         *
         * @return a <code>MapTransform</code> clone
         */
        public MapTransform createDeepClone() {
            return new AEAC(_parameterValues);
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
         * @param phi      latitude of source location
         * @param lam      longitude of source location
         * @param mapPoint
         *
         * @return the map co-ordinate
         */
        @Override
        protected Point2D forward_impl(double phi, double lam, Point2D mapPoint) {
            final double _phi = Math.toRadians(phi);
            final double _lam = Math.toRadians(lam);
            double rho = 0;

            if (Math.abs(Math.abs(_phi) - (Math.PI / 2)) < eps10) {
                if ((_phi * _n) < 0.0) {
                    throw new IllegalArgumentException("Invalid parameter range");
                }
            } else {
                final double sinPhi = Math.sin(_phi);
                double _q = (1 - es) * ((sinPhi / (1 - (es * sinPhi * sinPhi))) - ((1 / (2 * e)) * Math.log(
                        (1 - e * sinPhi) / (1 + e * sinPhi))));
                rho = (_a * (Math.sqrt(_c - (_n * _q)))) / _n;
            }

            double theta = _n * (_lam - _lam0);
            double x = rho * Math.sin(theta);
            double y = _rho0 - (rho * Math.cos(theta));

            mapPoint.setLocation(x, y);
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
            double rho;
            double beta;
            double theta;
            double localX = x;
            double localY = y;
            double _q;
            double _phi = 0.0;
            double _lam = 0.0;

            rho = Math.sqrt(Math.pow(localX, 2) + Math.pow(_rho0 - localY, 2));
            if (rho != 0.0) {
                if (_n < 0.0) {
                    rho = -rho;
                    localX = -localX;
                    localY = -localY;
                }

                theta = Math.atan((localX) / (_rho0 - localY));

                _lam = _lam0 + (theta / _n);

                _q = (_c - ((Math.pow(rho, 2) * Math.pow(_n, 2)) / Math.pow(_a, 2))) / _n;

                final double e2 = e * e;
                final double e4 = e2 * e2;
                final double e6 = e2 * e2 * e2;

                beta = Math.asin(_q / (1.0 - ((1.0 - e2) / (2 * e)) * Math.log((1.0 - e) / (1.0 + e))));

                _phi = beta + ((e2 / 3) + (31 * (e4 / 180)) + (517 * e6 / 5040)) * Math.sin(2 * beta) + ((23 * e4 / 360) + (251 * e6 / 3780)) * Math.sin(
                        4 * beta) + (761 * e6 / 45360) * Math.sin(6 * beta);

            } else {
                _lam = 0.;
            }

            geoPoint.lat = Math.toDegrees(_phi);
            geoPoint.lon = Math.toDegrees(_lam);
            return geoPoint;
        }

        public double getSemiMinor() {
            return _parameterValues[SEMI_MINOR_INDEX];
        }

        public double getLatitudeOfOrigin() {
            return _parameterValues[LATITUDE_OF_ORIGIN_INDEX];
        }

        public double getStandardParallel1() {
            return _parameterValues[LATITUDE_OF_INTERSECTION_1_INDEX];
        }

        public double getStandardParallel2() {
            return _parameterValues[LATITUDE_OF_INTERSECTION_2_INDEX];
        }
    }

}
