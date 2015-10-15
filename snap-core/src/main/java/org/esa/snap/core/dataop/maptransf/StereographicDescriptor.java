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

import java.awt.geom.Point2D;


/**
 * The descriptor for a map transformation which implements the
 * Stereographic projection. The code is based on the proj4 map
 * projection program.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version 1.0
 * @see <a href="http://remotesensing.org/proj/">http://remotesensing.org/proj/</a>
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.Stereographic.Provider} instead.
 */
@Deprecated
public class StereographicDescriptor implements MapTransformDescriptor {

    public static final String TYPE_ID = "Stereographic";
    public static final String NAME = TYPE_ID;
    public static final String MAP_UNIT = "meter";

    private static final int SEMI_MAJOR_INDEX = 0;
    private static final int SEMI_MINOR_INDEX = 1;
    private static final int LATITUDE_OF_ORIGIN_INDEX = 2;
    private static final int CENTRAL_MERIDIAN_INDEX = 3;
    private static final int SCALE_FACTOR_INDEX = 4;
    private static final int FALSE_EASTING_INDEX = 5;
    private static final int FALSE_NORTHING_INDEX = 6;

    private static final String[] PARAMETER_NAMES = new String[]{
        "semi_major",
        "semi_minor",
        "latitude_of_origin",
        "central_meridian",
        "scale_factor",
        "false_easting",
        "false_northing"
    };

    private static final String[] PARAMETER_LABELS = new String[]{
        "Semi major",
        "Semi minor",
        "Latitude of origin",
        "Central meridian",
        "Scale factor",
        "False easting",
        "False northing",
    };

    private static final String[] PARAMETER_UNITS = new String[]{
        "meter",
        "meter",
        "degree",
        "degree",
        "",
        "meter",
        "meter"
    };

    public static final double[] PARAMETER_DEFAULT_VALUES = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi major axis (meter)
        Ellipsoid.WGS_84.getSemiMinor(), // semi minor axis (meter)
        90.0, // central parallel (degree)
        0.0, // central meridian (degree)
        1.0, // scale factor
        0.0, // false_easting (meter)
        0.0  // false_northing (meter)
    };

    private static final double[] UPS_NORTH_PARAMETER_VALUES = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi major axis (meter)
        Ellipsoid.WGS_84.getSemiMinor(), // semi minor axis (meter)
        90.0, // central parallel (degree)
        0.0, // central meridian (degree)
        0.994, // scale factor
        2.0e6, // false_easting (meter)
        2.0e6  // false_northing (meter)
    };

    private static final double[] UPS_SOUTH_PARAMETER_VALUES = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi major axis (meter)
        Ellipsoid.WGS_84.getSemiMinor(), // semi minor axis (meter)
        -90.0, // central parallel (degree)
        0.0, // central meridian (degree)
        0.994, // scale factor
        2.0e6, // false_easting (meter)
        2.0e6  // false_northing (meter)
    };

    public static final String UPS_NORTH_NAME = "Universal Polar Stereographic North";
    public static final String UPS_SOUTH_NAME = "Universal Polar Stereographic South";


    public StereographicDescriptor() {
    }


    /**
     * This method is called within the <code>{@link MapProjectionRegistry#registerDescriptor}</code>
     * method after an instance of this <code>MapTransformDescriptor</code> has been successfully registered.
     * <p>
     */
    @Override
    public void registerProjections() {
        MapProjectionRegistry.registerProjection(new MapProjection(getName(),
                                                                   createTransform(PARAMETER_DEFAULT_VALUES), false));
        MapProjectionRegistry.registerProjection(new MapProjection(UPS_NORTH_NAME,
                                                                   createTransform(UPS_NORTH_PARAMETER_VALUES), true));
        MapProjectionRegistry.registerProjection(new MapProjection(UPS_SOUTH_NAME,
                                                                   createTransform(UPS_SOUTH_PARAMETER_VALUES), true));
    }


    /**
     * Retrieves the type identifier for this transform.
     */
    @Override
    public String getTypeID() {
        return TYPE_ID;
    }


    /**
     * Gets a descriptive name for this map transformation descriptor.
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
     * Gets the list of parameters required to create an
     * instance of the map transformation.
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
    public MapTransformUI getTransformUI(final MapTransform transform) {
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
        return new ST(parameterValues);
    }


    /**
     * Implements the stereographic transformation as defined in the
     * proj4 cartographic projection package. The optional latitude
     * of true scale parameter which provides an alternative way to
     * specify the scaling has been omitted for convenience.
     *
     * @author Marco Peters
     * @author Ralf Quast
     * @version 1.0
     * @see <a href="http://remotesensing.org/proj/">http://remotesensing.org/proj/</a>
     * 
     * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.MapProjection} instead.
     */
    @Deprecated
    public class ST extends CartographicMapTransform {

        // Numerical constants used by map transformations
        private static final double CONV = 1.0e-10;
        private static final double EPS10 = 1.0e-10;
        private static final double HALFPI = 0.5 * Math.PI;
        private static final int NITER = 8;
        // number of iterations for inverse transformation

        // Projection modes
        private static final int S_POLE = 0;
        // south polar projection
        private static final int N_POLE = 1;
        // north polar projection
        private static final int OBLIQ = 2;
        // oblique projection
        private static final int EQUIT = 3;
        // equatorial projection
                
        // Projection parameters
        private final double[] _parameterValues;

        // Numerical expressions precomputed during initialization
        final private int _mode;
        final private double _e;
        // eccentricity
        private double _akm1;
        private double _sinX1;
        private double _cosX1;

        public ST(final double[] parameterValues) {
            super(parameterValues[CENTRAL_MERIDIAN_INDEX],
                  parameterValues[FALSE_EASTING_INDEX],
                  parameterValues[FALSE_NORTHING_INDEX],
                  parameterValues[SEMI_MAJOR_INDEX]);

            final double b = parameterValues[SEMI_MINOR_INDEX];

            _e = Math.sqrt(1.0 - (b * b) / (_a * _a));

            final double phi0 = Math.toRadians(parameterValues[LATITUDE_OF_ORIGIN_INDEX]);
            final double k0 = parameterValues[SCALE_FACTOR_INDEX];
            final double absphi0 = Math.abs(phi0);

            if (Math.abs(absphi0 - HALFPI) < EPS10) {
                _mode = phi0 < 0.0 ? S_POLE : N_POLE;
            } else {
                _mode = absphi0 > EPS10 ? OBLIQ : EQUIT;
            }

            switch (_mode) {
            case N_POLE:
            case S_POLE:
                _akm1 = 2.0 * k0 / Math.sqrt(Math.pow(1.0 + _e, 1.0 + _e) * Math.pow(1.0 - _e, 1.0 - _e));
                break;
            case EQUIT:
                _akm1 = 2.0 * k0;
                break;
            case OBLIQ:
                double t = Math.sin(phi0);
                final double X = 2.0 * Math.atan(MapTransformUtils.ssfn(phi0, t, _e)) - HALFPI;
                t *= _e;
                _akm1 = 2.0 * k0 * Math.cos(phi0) / Math.sqrt(1.0 - t * t);
                _sinX1 = Math.sin(X);
                _cosX1 = Math.cos(X);
                break;
            }

            _parameterValues = new double[parameterValues.length];
            System.arraycopy(parameterValues, 0, _parameterValues, 0, _parameterValues.length);
        }

        @Override
        public MapTransformDescriptor getDescriptor() {
            return StereographicDescriptor.this;
        }


        @Override
        public double[] getParameterValues() {
            final double[] values = new double[_parameterValues.length];
            System.arraycopy(_parameterValues, 0, values, 0, values.length);

            return values;
        }


        /**
         * Forward project geographical coordinates into map coordinates.
         */
        @Override
        public Point2D forward_impl(final double lat, final double lon, Point2D mapPoint) {
            final double phi = Math.toRadians(lat);
            final double lam = Math.toRadians(lon);
            final double coslam = Math.cos(lam);
            final double sinlam = Math.sin(lam);
            final double sinphi = Math.sin(phi);

            double sinX = 0.0;
            double cosX = 0.0;
            double A = 0.0;

            if (_mode == OBLIQ || _mode == EQUIT) {
                final double X = 2.0 * Math.atan(MapTransformUtils.ssfn(phi, sinphi, _e)) - HALFPI;

                sinX = Math.sin(X);
                cosX = Math.cos(X);
            }

            if (mapPoint == null) {
                mapPoint = new Point2D.Double();
            }

            switch (_mode) {
            case OBLIQ:
                A = _akm1 / (_cosX1 * (1.0 + _sinX1 * sinX + _cosX1 * cosX * coslam));
                mapPoint.setLocation(A * cosX, A * (_cosX1 * sinX - _sinX1 * cosX * coslam));
                break;
            case EQUIT:
                A = 2.0 * _akm1 / (1.0 + cosX * coslam);
                mapPoint.setLocation(A * cosX, A * sinX);
                break;
            case S_POLE:
                A = _akm1 * MapTransformUtils.tsfn(-phi, -sinphi, _e);
                mapPoint.setLocation(A, A * coslam);
                break;
            case N_POLE:
                A = _akm1 * MapTransformUtils.tsfn(phi, sinphi, _e);
                mapPoint.setLocation(A, -A * coslam);
                break;
            }
            mapPoint.setLocation(mapPoint.getX() * sinlam, mapPoint.getY());

            return mapPoint;
        }

        /**
         * Inverse project map coordinates into geographical coordinates.
         */
        @Override
        public GeoPos inverse_impl(double x, double y, GeoPos geoPoint) {
            final double rho = Math.sqrt(x * x + y * y);

            double sinphi;
            double tp = 0.0, phi_l = 0.0, halfe = 0.0, halfpi = 0.0;

            switch (_mode) {
            case OBLIQ:
            case EQUIT:
                tp = 2.0 * Math.atan2(rho * _cosX1, _akm1);
                final double cosphi = Math.cos(tp);
                sinphi = Math.sin(tp);

                if (rho == 0.0) {
                    phi_l = Math.asin(cosphi * _sinX1);
                } else {
                    phi_l = Math.asin(cosphi * _sinX1 + (y * sinphi * _cosX1 / rho));
                }

                tp = Math.tan(0.5 * (HALFPI + phi_l));
                x *= sinphi;
                y = rho * _cosX1 * cosphi - y * _sinX1 * sinphi;
                halfpi = HALFPI;
                halfe = 0.5 * _e;
                break;
            case N_POLE:
                y = -y;
            case S_POLE:
                tp = -rho / _akm1;
                phi_l = HALFPI - 2.0 * Math.atan(tp);
                halfpi = -HALFPI;
                halfe = -0.5 * _e;
                break;
            }

            double phi = 0.0;
            double lam = 0.0;

            for (int i = NITER; i > 0; i--) {
                sinphi = _e * Math.sin(phi_l);
                phi = 2.0 * Math.atan(tp * Math.pow((1.0 + sinphi) / (1.0 - sinphi), halfe)) - halfpi;

                if (Math.abs(phi_l - phi) < CONV) {
                    if (_mode == S_POLE) {
                        phi = -phi;
                    }
                    lam = (x == 0.0 && y == 0.0) ? 0.0 : Math.atan2(x, y);
                    break;
                }
                phi_l = phi;
            }

            if (geoPoint == null) {
                geoPoint = new GeoPos();
            }
            geoPoint.setLocation(Math.toDegrees(phi), Math.toDegrees(lam));

            return geoPoint;
        }

        @Override
        public MapTransform createDeepClone() {
            return new ST(_parameterValues);
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

        public boolean isPolar() {
            return _mode == N_POLE || _mode == S_POLE;
        }

        public boolean isOblique() {
            return _mode == OBLIQ;
        }
    }
}
