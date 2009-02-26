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
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;


public class MapTransformTest extends TestCase {

    public void testTransform() throws FactoryException {

    }

    static class CrsGeoCoding implements GeoCoding {
        private static final CoordinateReferenceSystem GEO = DefaultGeographicCRS.WGS84;

        private final MathTransform toGeo;
        private final MathTransform toCrs;
        private final ReferencedEnvelope envelope;

        CrsGeoCoding(ReferencedEnvelope envelope) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            MathTransform toGeo;
            MathTransform toCrs;

            try {
                toGeo = CRS.findMathTransform(crs, GEO);
            } catch (FactoryException e) {
                toGeo = null;
            }
            try {
                toCrs = CRS.findMathTransform(GEO, crs);
            } catch (FactoryException e) {
                toCrs = null;
            }

            this.toGeo = toGeo;
            this.toCrs = toCrs;
            this.envelope = new ReferencedEnvelope(envelope);
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            final ReferencedEnvelope geoEnvelope;
            try {
                geoEnvelope = envelope.transform(GEO, true);
            } catch (TransformException e) {
                // some coordinates cannot be transformed
                return false;
            } catch (FactoryException e) {
                // transform cannot determined
                return false;
            }

            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return toCrs != null;
        }

        @Override
        public boolean canGetGeoPos() {
            return toGeo != null;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return null;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return null;
        }

        @Override
        public Datum getDatum() {
            return Datum.WGS_84;
        }

        @Override
        public void dispose() {
        }
    }
}
