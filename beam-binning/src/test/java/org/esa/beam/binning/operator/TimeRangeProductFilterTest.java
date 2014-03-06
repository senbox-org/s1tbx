/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.junit.Assert.*;

public class TimeRangeProductFilterTest {

    @Test
    public void testAcceptProductWithProperGeoCoding() throws Exception {
        final ProductFilter filter = new TimeRangeProductFilter(null, null);
        final Product product = new Product("P", "T", 10, 10);

        product.setGeoCoding(new MockGeoCoding(true, false));
        assertTrue(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(true, true));
        assertTrue(filter.accept(product));
    }

    @Test
    public void testRejectProductWithoutProperGeoCoding() throws Exception {
        final ProductFilter filter = new TimeRangeProductFilter(null, null);
        final Product product = new Product("P", "T", 10, 10);

        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(false, false));
        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(false, true));
        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(true, false));
        assertTrue(filter.accept(product));
    }

    @Test
    public void testAcceptProductWithStartAndEndTime() throws Exception {
        final ProductFilter filter = new TimeRangeProductFilter(ProductData.UTC.parse("02-MAY-2013 00:00:00"),
                                                                ProductData.UTC.parse("02-MAY-2013 23:59:59"));
        final Product product = new Product("P", "T", 10, 10);
        product.setGeoCoding(new MockGeoCoding(true, true));

        // product's start and end times are both before range
        product.setStartTime(ProductData.UTC.parse("01-MAY-2013 15:10:00"));
        product.setEndTime(ProductData.UTC.parse("01-MAY-2013 15:40:00"));
        assertFalse(filter.accept(product));

        // product's start and end times are both within range
        product.setStartTime(ProductData.UTC.parse("02-MAY-2013 15:10:00"));
        product.setEndTime(ProductData.UTC.parse("02-MAY-2013 15:40:00"));
        assertTrue(filter.accept(product));

        // product's start and end times are both after range
        product.setStartTime(ProductData.UTC.parse("03-MAY-2013 15:10:00"));
        product.setEndTime(ProductData.UTC.parse("03-MAY-2013 15:40:00"));
        assertFalse(filter.accept(product));

        // todo - investigate into this strange behaviour (nf)

        // product's start time is inside range
        product.setStartTime(ProductData.UTC.parse("02-MAY-2013 23:30:00"));
        product.setEndTime(ProductData.UTC.parse("03-MAY-2013 00:30:00"));
        assertFalse(filter.accept(product));
        // However, Norman would expect: assertTrue(filter.accept(product));

        // product's end time is inside range
        product.setStartTime(ProductData.UTC.parse("01-MAY-2013 23:30:00"));
        product.setEndTime(ProductData.UTC.parse("02-MAY-2013 00:30:00"));
        assertFalse(filter.accept(product));
        // However, Norman would expect: assertTrue(filter.accept(product));

        // product's start time equals range start time
        product.setStartTime(ProductData.UTC.parse("02-MAY-2013 00:00:00"));
        product.setEndTime(ProductData.UTC.parse("02-MAY-2013 01:00:00"));
        assertFalse(filter.accept(product));
        // However, Norman would expect: assertTrue(filter.accept(product));

        // product's end time equals range end time
        product.setStartTime(ProductData.UTC.parse("02-MAY-2013 23:00:00"));
        product.setEndTime(ProductData.UTC.parse("02-MAY-2013 23:59:59"));
        assertFalse(filter.accept(product));
        // However, Norman would expect: assertTrue(filter.accept(product));
    }


    private static class MockGeoCoding implements GeoCoding {

        private boolean canGetGeoPos;
        private boolean canGetPixelPos;

        private MockGeoCoding(boolean canGetGeoPos, boolean canGetPixelPos) {
            this.canGetGeoPos = canGetGeoPos;
            this.canGetPixelPos = canGetPixelPos;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return canGetPixelPos;
        }

        @Override
        public boolean canGetGeoPos() {
            return canGetGeoPos;
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
            return null;
        }

        @Override
        public void dispose() {
        }

        @Override
        public CoordinateReferenceSystem getImageCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getMapCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getGeoCRS() {
            return null;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return null;
        }
    }

    /**
     * This test was only introduced to measure the performance of
     * point in polygon test. No production code is executed.
     */
    @Test
    @Ignore
    public void testPointInPolygonPerformance() throws Exception {
        final GeometryFactory geometryFactory = new GeometryFactory();
        Polygon box1 = geometryFactory.createPolygon(geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(0.1, 0.1),
                new Coordinate(0.8, 0.1),
                new Coordinate(0.8, 0.8),
                new Coordinate(0.1, 0.8),
                new Coordinate(0.1, 0.1),
        }), null);
        Polygon box2 = geometryFactory.createPolygon(geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(0.11, 0.12),
                new Coordinate(0.85, 0.11),
                new Coordinate(0.84, 0.83),
                new Coordinate(0.19, 0.87),
                new Coordinate(0.16, 0.19),
                new Coordinate(0.11, 0.12),
        }), null);

        final int N = 14000000; // 14E6 ~ one MERIS orbit

        double n2m = 1.0 / (1000 * 1000);

        PIP emptyPip = new PIP() {
            @Override
            public boolean isPIP(double x, double y, Geometry polygon) {
                return false;
            }
        };

        PIP realPip = new PIP() {
            @Override
            public boolean isPIP(double x, double y, Geometry polygon) {
                return polygon.contains(geometryFactory.createPoint(new Coordinate(x, y)));
            }
        };

        long dt1 = runPip(box1, N, emptyPip);
        System.out.println("dt(fake) = " + dt1 * n2m + " ms");

        long dt2 = runPip(box1, N, realPip);
        System.out.println("dt(box1) = " + dt2 * n2m + " ms");

        long dt3 = runPip(box2, N, realPip);
        System.out.println("dt(box2) = " + dt3 * n2m + " ms");

        System.out.println("box1: dt = " + (dt2 - dt1) * n2m + " ms");
        System.out.println("box2: dt = " + (dt3 - dt1) * n2m + " ms");
    }


    private static long runPip(Polygon polygon, int n, PIP pip) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            pip.isPIP(Math.random(), Math.random(), polygon);
        }
        long t1 = System.nanoTime();
        return t1 - t0;
    }

    interface PIP {
        boolean isPIP(double x, double y, Geometry polygon);
    }
}
