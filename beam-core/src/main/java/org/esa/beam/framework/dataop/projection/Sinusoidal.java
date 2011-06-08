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

package org.esa.beam.framework.dataop.projection;


import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

import java.awt.geom.Point2D;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

/**
 * The sinusoidal projection is a pseudocylindrical equal-area map projection.
 *
 * @since 4.8
 */
public class Sinusoidal extends MapProjection {

    private static final double HALF_PI = PI/2;

    public static final class Provider extends AbstractProvider {

        /**
         * The parameters group.
         */
        static final ParameterDescriptorGroup PARAMETERS = createDescriptorGroup(new NamedIdentifier[]{
                new NamedIdentifier(Citations.OGC, "Sinusoidal"),
                new NamedIdentifier(Citations.GEOTOOLS, "CT_Sinusoidal"),
                new NamedIdentifier(Citations.ESRI, "Sinusoidal"),
        }, new ParameterDescriptor[]{
                SEMI_MAJOR, SEMI_MINOR,
                CENTRAL_MERIDIAN, SCALE_FACTOR,
                FALSE_EASTING, FALSE_NORTHING
        });

        /**
         * Constructs a new provider.
         */
        public Provider() {
            super(PARAMETERS);
        }

        /**
         * Returns the operation type for this map projection.
         */
        @Override
        public Class<? extends Projection> getOperationType() {
            return PseudoCylindricalProjection.class;
        }


        /**
         * Create a new map projection based on the parameters.
         */
        @Override
        public MathTransform createMathTransform(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            return new Sinusoidal(parameters);
        }
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     *
     * @throws org.opengis.parameter.ParameterNotFoundException
     *          if a mandatory parameter is missing.
     */
    protected Sinusoidal(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        super(parameters);
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Provider.PARAMETERS;
    }

    /**
     * Transforms the specified (<var>&lambda;</var>,<var>&phi;</var>) coordinates
     * (units in radians) and stores the result in {@code ptDst} (linear distance
     * on a unit sphere).
     */
    @Override
    protected Point2D transformNormalized(double x, double y, final Point2D ptDst) throws ProjectionException {
        double mapX = x * cos(y);
        double mapY = y;

        if (ptDst != null) {
            ptDst.setLocation(mapX, mapY);
            return ptDst;
        }
        return new Point2D.Double(mapX, mapY);
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code ptDst}.
     */
    @Override
    protected Point2D inverseTransformNormalized(double x, double y, final Point2D ptDst)
            throws ProjectionException {
        double lon = x / cos(y);
        // if the value for lon or lat exceed the valid range,
        // set the to NaN to indicate an invalid phi or lambda.
        if (lon < -PI) {
            lon = Double.NaN;
        }
        if (lon > +PI) {
            lon = Double.NaN;
        }
        double lat = y;
        if (lat < -HALF_PI) {
            lon = 0;
            lat = Double.NaN;
        }
        if (lat > +HALF_PI) {
            lon = 0;
            lat = Double.NaN;
        }
        if (ptDst != null) {
            ptDst.setLocation(lon, lat);
            return ptDst;
        }
        return new Point2D.Double(lon, lat);
    }

}