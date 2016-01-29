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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class GeoCodingMathTransformTest {

    @Test
    public void testIt() throws FactoryException, TransformException {

        AffineTransform at = new AffineTransform();
        at.translate(-40, +10);
        assertEquals(new Point(-40, 10), at.transform(new Point(0, 0), null));

        GeoCoding geoCoding = new AffineGeoCoding(at);
        assertEquals(new GeoPos(10, -40), geoCoding.getGeoPos(new PixelPos(0, 0), new GeoPos()));
        assertEquals(new PixelPos(0, 0), geoCoding.getPixelPos(new GeoPos(10, -40), new PixelPos()));

        GeographicCRS geoCRS = DefaultGeographicCRS.WGS84;
        SingleCRS gridCRS = new DefaultDerivedCRS("xyz",
                                                  geoCRS,
                                                  new GeoCodingMathTransform(geoCoding),
                                                  DefaultCartesianCS.GRID);

        assertEquals(geoCRS.getDatum(), gridCRS.getDatum());

        MathTransform transform = CRS.findMathTransform(gridCRS, geoCRS);
        assertNotNull(transform);

        DirectPosition position = transform.transform(new GeneralDirectPosition(0, 0), null);
        assertNotNull(position);
        assertEquals(new GeneralDirectPosition(-40, 10), position);

        //assertEquals();
        position = transform.transform(new DirectPosition2D(gridCRS, 1, 1), null);
        assertEquals(new GeneralDirectPosition(-39, 11), position);

        transform = CRS.findMathTransform(gridCRS, gridCRS);
        assertNotNull(transform);
    }

    private static class AffineGeoCoding extends AbstractGeoCoding {

        private final AffineTransform at;

        private AffineGeoCoding(AffineTransform at) {
            this.at = at;
        }

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return true;
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            final Point2D point2D;
            try {
                point2D = at.inverseTransform(new Point2D.Double(geoPos.lon, geoPos.lat), null);
            } catch (NoninvertibleTransformException e) {
                throw new IllegalStateException(e);
            }
            pixelPos.x = (float) point2D.getX();
            pixelPos.y = (float) point2D.getY();
            return pixelPos;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            final Point2D point2D = at.transform(new Point2D.Double(pixelPos.x, pixelPos.y), null);
            geoPos.lon = (float) point2D.getX();
            geoPos.lat = (float) point2D.getY();
            return geoPos;
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
