/*
 * Copyright (C) 2014 Catalysts GmbH (www.catalysts.cc)
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
import org.opengis.referencing.operation.ConicProjection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

import java.awt.geom.Point2D;

/**
 * The azimuthal equidistant map projection.
 */
public class AzimuthalEquidistant extends MapProjection {

    private static final long serialVersionUID = 2015823327782243331L;

    public static final class Provider extends AbstractProvider {

        private static final long serialVersionUID = -6627017123664912788L;

        /**
         * The parameters group.
         * Central meridian is lambda0, the longitude value of the center
         * Latitude of origin is phi0, the latitude value of the center
         */
        static final ParameterDescriptorGroup PARAMETERS = createDescriptorGroup(new NamedIdentifier[]{
                new NamedIdentifier(Citations.EPSG, "Azimuthal Equidistant"),
                new NamedIdentifier(Citations.OGC, "Azimuthal_Equidistant"),
                new NamedIdentifier(Citations.GEOTIFF, "CT_AzimuthalEquidistant"),
                new NamedIdentifier(Citations.GEOTOOLS, "CT_AzimuthalEquidistant"),
                new NamedIdentifier(Citations.ESRI, "Azimuthal Equidistant"),
        }, new ParameterDescriptor[]{
                SEMI_MAJOR, SEMI_MINOR,
                LATITUDE_OF_ORIGIN, CENTRAL_MERIDIAN,
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
            return ConicProjection.class;
        }


        /**
         * Create a new map projection based on the parameters.
         */
        @Override
        public MathTransform createMathTransform(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            return new AzimuthalEquidistant(parameters);
        }
    }

    /**
     * Possible mode types
     */
    private static final int NORMAL = 0, NORTH_POLE = 1, SOUTH_POLE = 2;

    /**
     * Latitude tolerance value
     */
    private static final double EPSILON_LATITUDE = 1E-10;

    private final int mode;
    private final double sinPhi0;
    private final double cosPhi0;

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     * @throws org.opengis.parameter.ParameterNotFoundException if a mandatory parameter is missing.
     */
    protected AzimuthalEquidistant(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        super(parameters);

        // check if the latitude is -90 or 90 degrees and set the particular mode
        if (Math.abs(Math.abs(latitudeOfOrigin) - (Math.PI / 2)) < EPSILON_LATITUDE) {
            mode = latitudeOfOrigin < 0.0 ? SOUTH_POLE : NORTH_POLE;
        } else {
            mode = NORMAL;
        }

        // pre-calculate the sine and cosine value of the latitude of origin
        sinPhi0 = Math.sin(latitudeOfOrigin);
        cosPhi0 = Math.cos(latitudeOfOrigin);
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
    protected Point2D transformNormalized(double lambda, double phi, final Point2D ptDst) throws ProjectionException {

        // Note: the given lambda is already subtracted by lambda0!
        // transformation equations taken from mathworld.wolfram.com/AzimuthalEquidistantProjection.html

        final double sinPhi = Math.sin(phi);
        final double cosPhi = Math.cos(phi);

        final double sinLam = Math.sin(lambda);
        final double cosLam = Math.cos(lambda);

        // calculate c, the angular distance from the center
        final double cosC = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosLam;
        final double c = Math.acos(cosC);

        final double mapX;
        final double mapY;

        if (c == 0.0) {
            // special case: center
            mapX = 0.0;
            mapY = 0.0;
        } else {
            final double kPrime = c / Math.sin(c);
            mapX = kPrime * cosPhi * sinLam;
            mapY = kPrime * (cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosLam);
        }

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

        // Note: the lambda which will be returned will be automatically increased by lambda0
        // transformation equations taken from mathworld.wolfram.com/AzimuthalEquidistantProjection.html

        // calculate c, the angular distance from the center
        final double c = Math.sqrt(x * x + y * y);

        final double phi;
        final double lambda;

        if (c == 0.0) {
            // special case: center
            phi = 0.0;
            lambda = 0.0;
        } else {
            final double sinC = Math.sin(c);
            final double cosC = Math.cos(c);

            phi = Math.asin(cosC * sinPhi0 + y * sinC * cosPhi0 / c);

            switch (mode) {
                case NORTH_POLE:
                    lambda = Math.atan2(-x, y);
                    break;
                case SOUTH_POLE:
                    lambda = Math.atan2(x, y);
                    break;
                default:
                    lambda = Math.atan2(x * sinC, (c * cosPhi0 * cosC - y * sinPhi0 * sinC));
            }
        }

        if (ptDst != null) {
            ptDst.setLocation(lambda, phi);
            return ptDst;
        }
        return new Point2D.Double(lambda, phi);
    }

}