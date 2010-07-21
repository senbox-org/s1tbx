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
package org.esa.beam.examples.maptransf;

import java.awt.geom.Point2D;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.param.Parameter;

/**
 * The <code>ExampleProjectionPlugIn</code> class is an example for a custom map-transformation. After you have compiled
 * this class, put the resulting class files along with their package path into VISAT's extensions folder:
 * <p/>
 * <pre>
 * $BEAM_INSTALL_DIR$/
 *   extensions/
 *     org/
 *       esa/
 *         beam/
 *           examples/
 *             maptransf/
 *               ExampleProjectionPlugIn.class
 *               ExampleProjectionPlugIn$1.class
 *               ExampleProjectionPlugIn$MT.class
 * </pre>
 * <p/>
 * After restarting VISAT, the map projection tool will have some new map-projections with the name "Example Projection
 * n".
 * <p/>
 * <p>The <code>ExampleProjectionPlugIn</code> class is used to describe the map-transformation and is a factory class
 * for instances of the actual map-transformation it provides. In this example the inner class <code>{@link MT}</code>
 * provides this implementation of the map-transformation algorithm.
 *
 * @see #createTransform(double[])
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.MathTransformProvider} instead.
 */
@Deprecated
public class ExampleProjectionPlugIn implements MapTransformDescriptor {

    /**
     * Important: public(!) no-args-constructor
     */
    public ExampleProjectionPlugIn() {
    }

    /**
     * This method is called within the <code>{@link org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry#registerDescriptor}</code>
     * method after an instance of this <code>MapTransformDescriptor</code> has been successfully registered.
     */
    public void registerProjections() {
        MapProjectionRegistry.registerProjection(new MapProjection("Example Projection 1", new MT(1.0, 1.0)));
        MapProjectionRegistry.registerProjection(new MapProjection("Example Projection 2", new MT(1.0, 0.5)));
        MapProjectionRegistry.registerProjection(new MapProjection("Example Projection 3", new MT(0.5, 1.0)));
    }

    /**
     * Gets the unique type identifier for the map transformation, e.g. "Transverse_Mercator".
     */
    public String getTypeID() {
        return "Example";
    }

    /**
     * Gets a descriptive name for this map transformation descriptor, e.g. "Transverse Mercator".
     */
    public String getName() {
        return "Example";
    }

    /**
     * Gets the unit of the map, e.g. "degree" or "meter".
     */
    public String getMapUnit() {
        return "pixel";
    }

    /**
     * Gets the default parameter values for this map transform.
     */
    public double[] getParameterDefaultValues() {
        return new double[]{
            1.0,
            1.0
        };
    }

    /**
     * Gets the list of parameters required to create an instance of the map transform.
     */
    public Parameter[] getParameters() {
        final double[] defaultParameterValues = getParameterDefaultValues();
        return new Parameter[]{
            new Parameter("scaleX", defaultParameterValues[0]),
            new Parameter("scaleY", defaultParameterValues[1])};
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

    /**
     * Creates an instance of the map transform for the given parameter values.
     */
    public MapTransform createTransform(double[] parameterValues) {
        if (parameterValues == null) {
            parameterValues = getParameterDefaultValues();
        }
        return new MT(parameterValues[0], parameterValues[1]);
    }

    /**
     * The actual implementation of the map-transformation. It performs a simple (and stupid) transformation in order to
     * keep things simple. It scales and flips the X- and Y-axes of an image and thus it has two parameters,
     * <code>scaleX</code> and <code>scaleY</code>.
     */
    public class MT implements MapTransform {

        private final double _scaleX;
        private final double _scaleY;

        /**
         * Construct a new map-transformation
         *
         * @param scaleX scaling factor for the X-axis
         * @param scaleY scaling factor for the Y-axis
         */
        public MT(double scaleX, double scaleY) {
            _scaleX = scaleX;
            _scaleY = scaleY;
        }

        /**
         * Returns the descriptor for this map transform.
         *
         * @return the descriptor, should never be <code>null</code>
         */
        public MapTransformDescriptor getDescriptor() {
            return ExampleProjectionPlugIn.this;
        }

        /**
         * Returns the array of parameter values. The order in which the parameters are returned must exactly match the
         * order in which the corresponding {@link org.esa.beam.framework.param.Parameter} array is returned by the
         * <code>{@link org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor#getParameters()}</code> method.
         */
        public double[] getParameterValues() {
            return new double[]{_scaleX, _scaleY};
        }

        /**
         * Forward project geographical co-ordinates into map co-ordinates.
         */
        public Point2D forward(GeoPos geoPoint, Point2D mapPoint) {
            if (mapPoint == null) {
                mapPoint = new Point2D.Double();
            }
            final double y = _scaleY * geoPoint.getLon();
            final double x = _scaleX * geoPoint.getLat();
            mapPoint.setLocation(x, y);
            return mapPoint;
        }

        /**
         * Inverse project map co-ordinates into geographical co-ordinates.
         */
        public GeoPos inverse(Point2D mapPoint, GeoPos geoPoint) {
            if (geoPoint == null) {
                geoPoint = new GeoPos();
            }
            final double lat = mapPoint.getX() / _scaleX;
            final double lon = mapPoint.getY() / _scaleY;
            geoPoint.setLocation((float) lat, (float) lon);
            return geoPoint;
        }

        /**
         * Creates a deep clone of this <code>MapTransform</code>.
         *
         * @return a <code>MapTransform</code> clone
         */
        public MapTransform createDeepClone() {
            return new MT(_scaleX, _scaleY);
        }
    }
}
