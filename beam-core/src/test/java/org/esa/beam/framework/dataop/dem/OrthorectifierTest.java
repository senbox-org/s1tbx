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
package org.esa.beam.framework.dataop.dem;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.AngularDirection;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;

public class OrthorectifierTest extends TestCase {

    static final int SCENE_WIDTH = 100;
    static final int SCENE_HEIGHT = 100;
    static final int MAX_ITERATION_COUNT = 50;


    public void testThatOrthorectifierIsWellFormed() {

        final Orthorectifier gcDEM = createOrthorectifier();
        final GeoCoding gcWGS = gcDEM.getGeoCoding();
        final Pointing pointing = gcDEM.getPointing();

        assertSame(gcWGS, gcDEM.getGeoCoding());
        assertNull(gcDEM.getElevationModel());
        assertEquals(50, gcDEM.getMaxIterationCount());
        assertTrue(gcDEM.canGetGeoPos());
        assertTrue(gcDEM.canGetPixelPos());

        assertEquals(0.0f, pointing.getElevation(new PixelPos(0, 0)), 1e-5f);
        assertEquals(3000.0, pointing.getElevation(new PixelPos(0, 50)), 1e-5f);
        assertEquals(6000.0, pointing.getElevation(new PixelPos(0, 100)), 1e-5f);
        assertEquals(3000.0, pointing.getElevation(new PixelPos(100, 50)), 1e-5f);

        Assert.assertEquals(new GeoPos(10, 0), gcWGS.getGeoPos(new PixelPos(0, 0), null));
        assertEquals(new GeoPos(0, 0), gcWGS.getGeoPos(new PixelPos(0, 100), null));
        assertEquals(new GeoPos(10, 10), gcWGS.getGeoPos(new PixelPos(100, 0), null));
        assertEquals(new GeoPos(0, 10), gcWGS.getGeoPos(new PixelPos(100, 100), null));

        assertEquals(new GeoPos(5, 0), gcWGS.getGeoPos(new PixelPos(0, 50), null));
        assertEquals(new GeoPos(5, 5), gcWGS.getGeoPos(new PixelPos(50, 50), null));
        assertEquals(new GeoPos(10, 5), gcWGS.getGeoPos(new PixelPos(50, 0), null));
    }

    public void testThatPredictionCorrectionScopeIsAchieved() {
        final Orthorectifier o = createOrthorectifier();
        final PixelPos pixelPos = o.getPixelPos(new GeoPos(5.0f, 5.0f), null);
        assertEquals(50.0f, pixelPos.x, 1.0e-6);
        assertTrue(pixelPos.y > 50.0f);
        GeoPos geoPosFalse = o.getGeoCoding().getGeoPos(pixelPos, null);
        assertEquals(5.0f, geoPosFalse.lon, 1.0e-6);
        assertTrue(geoPosFalse.lat < 5.0f);
        GeoPos geoPosTrue = o.getGeoPos(pixelPos, null);
        assertEquals(5.0f, geoPosTrue.lon, 1.0e-6);
        assertTrue(geoPosTrue.lat > geoPosFalse.lat);
    }

    Orthorectifier createOrthorectifier() {
        return new Orthorectifier(SCENE_WIDTH,
                                  SCENE_HEIGHT,
                                  new PointingMock(new GeoCodingMock()),
                                  null,
                                  MAX_ITERATION_COUNT);
    }

    static class PointingMock implements Pointing {

        private GeoCodingMock _geoCoding;

        public PointingMock(GeoCodingMock geoCoding) {
            _geoCoding = geoCoding;
        }

        public GeoCoding getGeoCoding() {
            return _geoCoding;
        }

        public AngularDirection getSunDir(PixelPos pixelPos, AngularDirection angularDirection) {
            return null;
        }

        public AngularDirection getViewDir(PixelPos pixelPos, AngularDirection angularDirection) {
            if (angularDirection == null) {
                angularDirection = new AngularDirection();
            }
            angularDirection.azimuth = 0;
            angularDirection.zenith = 45;
            return angularDirection;
        }

        public float getElevation(PixelPos pixelPos) {
            return 6000 * pixelPos.y / SCENE_HEIGHT;
        }

        public boolean canGetGeoPos() {
            return true;
        }

        public boolean canGetElevation() {
            return true;
        }

        public boolean canGetSunDir() {
            return true;
        }

        public boolean canGetViewDir() {
            return true;
        }
    }

    static class GeoCodingMock implements GeoCoding {

        @Override
        public AffineTransform getImageToModelTransform() {
            return null;
        }

        public boolean canGetGeoPos() {
            return true;
        }

        public boolean canGetPixelPos() {
            return true;
        }

        public void dispose() {
        }

        @Override
        public CoordinateReferenceSystem getBaseCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getMapCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getImageCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getModelCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getGeoCRS() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return null;
        }

        public Datum getDatum() {
            return Datum.WGS_84;
        }

        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            final float lat = 10.0f * (1 - pixelPos.y / SCENE_HEIGHT);
            final float lon = 10.0f * (pixelPos.x / SCENE_WIDTH);
            geoPos.setLocation(lat, lon);
            return geoPos;
        }

        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            final float x = SCENE_WIDTH * (geoPos.lon / 10.0f);
            final float y = SCENE_HEIGHT * (1.0f - geoPos.lat / 10.0f);
            pixelPos.setLocation(x, y);
            return pixelPos;
        }

        public boolean isCrossingMeridianAt180() {
            return false;
        }
    }
}
