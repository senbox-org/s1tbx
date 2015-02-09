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

/**
 * The Mollweide projection is a pseudocylindrical map projection.
 * <p/>
 * It is used primarily where accurate representation of area takes precedence over shape,
 * for instance small maps depicting global distributions.
 *
 * @since 4.8
 */
public class Mollweide extends MapProjection {

    private static final int MAX_ITER = 10;
    private static final double TOLERANCE = 1.0e-7;

    private final double cx;
    private final double cy;
    private final double cp;

    public static final class Provider extends AbstractProvider {

        /**
         * The parameters group.
         */
        static final ParameterDescriptorGroup PARAMETERS = createDescriptorGroup(new NamedIdentifier[]{
                new NamedIdentifier(Citations.OGC, "Mollweide"),
                new NamedIdentifier(Citations.GEOTOOLS, "Mollweide"),
                new NamedIdentifier(Citations.ESRI, "Mollweide")
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
            return new Mollweide(parameters);
        }
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     *
     * @throws org.opengis.parameter.ParameterNotFoundException if a mandatory parameter is missing.
     */
    protected Mollweide(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        // Fetch parameters
        super(parameters);

        double p = Math.PI / 2;
        double p2 = p + p;

        double sp = Math.sin(p);
        double r = Math.sqrt(Math.PI * 2.0 * sp / (p2 + Math.sin(p2)));
        cx = 2.0 * r / Math.PI;
        cy = r / sp;
        cp = p2 + Math.sin(p2);
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

        double k = cp * Math.sin(y);
        int i;
        for (i = MAX_ITER; i != 0; i--) {
            double v = (y + Math.sin(y) - k) / (1.0 + Math.cos(y));
            y -= v;
            if (Math.abs(v) < TOLERANCE) {
                break;
            }
        }
        if (i == 0) {
            y = (y < 0.0) ? -Math.PI / 2 : Math.PI / 2;
        } else {
            y *= 0.5;
        }
        double mapX = cx * x * Math.cos(y);
        double mapY = cy * Math.sin(y);

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
        double lat = Math.asin(y / cy);
        double lon = x / (cx * Math.cos(lat));
        lat += lat;
        lat = Math.asin((lat + Math.sin(lat)) / cp);
        if (ptDst != null) {
            ptDst.setLocation(lon, lat);
            return ptDst;
        }
        return new Point2D.Double(lon, lat);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mollweide)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Mollweide mollweide = (Mollweide) o;

        if (Double.compare(mollweide.cp, cp) != 0) {
            return false;
        }
        if (Double.compare(mollweide.cx, cx) != 0) {
            return false;
        }
        return Double.compare(mollweide.cy, cy) == 0;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = cx != +0.0d ? Double.doubleToLongBits(cx) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = cy != +0.0d ? Double.doubleToLongBits(cy) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = cp != +0.0d ? Double.doubleToLongBits(cp) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}