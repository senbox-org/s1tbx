/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.geospike;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

public class MapTransformTest extends TestCase {

    public void testTransform() throws FactoryException {
    }

    static class GeoToolsMapTransform implements MapTransform {
        private final MapProjection mapProjection;

        GeoToolsMapTransform(ParameterValueGroup parameters) {
            mapProjection = createMapProjection(parameters);
        }

        @Override
        public MapTransformDescriptor getDescriptor() {
            // todo - implement
            return null;
        }

        @Override
        public double[] getParameterValues() {
            // todo - implement
            return new double[0];
        }

        @Override
        public Point2D forward(GeoPos geoPoint, Point2D mapPoint) {
            try {
                return mapProjection.transform(toPoint2D(geoPoint), mapPoint);
            } catch (ProjectionException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Override
        public GeoPos inverse(Point2D mapPoint, GeoPos geoPoint) {
            if (geoPoint == null) {
                geoPoint = new GeoPos();
            }
            final Point2D.Float point;
            try {
                point = (Point2D.Float) mapProjection.inverse().transform(mapPoint, toPoint2D(geoPoint));
                geoPoint.setLocation(point.y, point.x);
            } catch (TransformException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            return geoPoint;
        }

        @Override
        public MapTransform createDeepClone() {
            return new GeoToolsMapTransform(mapProjection.getParameterValues());
        }

        private static MapProjection createMapProjection(ParameterValueGroup parameterValues) {
            final MathTransformFactory transformFactory = new DefaultMathTransformFactory();
            final MathTransform transform;

            try {
                transform = transformFactory.createParameterizedTransform(parameterValues);
            } catch (FactoryException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            return (MapProjection) transform;
        }

        private static Point2D.Float toPoint2D(GeoPos geoPoint) {
            return new Point2D.Float(geoPoint.lon, geoPoint.lat);
        }
    }
}
