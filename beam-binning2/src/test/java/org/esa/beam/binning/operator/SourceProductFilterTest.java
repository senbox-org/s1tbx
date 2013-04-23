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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SourceProductFilterTest {

    @Test
    public void testAcceptProductWithProperGeoCoding() throws Exception {
        final ProductFilter filter = new SourceProductFilter(null, null);
        final Product product = new Product("P", "T", 10, 10);

        product.setGeoCoding(new MockGeoCoding(true, false));

        assertTrue(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(true, true));

        assertTrue(filter.accept(product));
    }

    @Test
    public void testRejectProductWithoutProperGeoCoding() throws Exception {
        final ProductFilter filter = new SourceProductFilter(null, null);
        final Product product = new Product("P", "T", 10, 10);

        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(false, false));

        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(false, true));

        assertFalse(filter.accept(product));

        product.setGeoCoding(new MockGeoCoding(true, false));

        assertTrue(filter.accept(product));
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
}
