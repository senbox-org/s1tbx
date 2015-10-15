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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.DataPeriod;
import org.esa.snap.binning.support.SpatialDataPeriod;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.junit.Assert;

import java.text.ParseException;

/**
 * @author Thomas Storm
 */
public class TestUtils {

    public static final float EASTERN_LON = 10.0F;
    public static final float WESTERN_LON = 0.0F;
    public static final int SCENE_RASTER_HEIGHT = 347;
    public static double T0;
    public static double H = 1 / 24.;

    static {
        try {
            T0 = ProductData.UTC.parse("10-Jan-2010 12:00:00").getMJD();
        } catch (ParseException e) {
            // ignore
        }
    }

    static Product createProduct(DataPeriod dataPeriod, DataPeriod.Membership firstPeriod, DataPeriod.Membership lastPeriod) {
        Product product = new Product("name", "type", 100, SCENE_RASTER_HEIGHT);
        product.setSceneGeoCoding(new MockGeoCoding());

        switch (firstPeriod) {
            case PREVIOUS_PERIODS: {
                // for first pixel in previous period: create a product that starts at latest at T0 + 10 * H
                double productStartTime = T0 - 12 * H;
                product.setStartTime(new ProductData.UTC(productStartTime));
                Assert.assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, dataPeriod.getObservationMembership(EASTERN_LON, productStartTime));
                break;
            }
            case CURRENT_PERIOD: {
                // for first pixel in current period: create a product that crosses -180° after T0 + 10 * H and before T0 + (10 + 24) * H
                double productStartTime = T0 + 15 * H;
                product.setStartTime(new ProductData.UTC(productStartTime));
                Assert.assertEquals(DataPeriod.Membership.CURRENT_PERIOD, dataPeriod.getObservationMembership(EASTERN_LON, productStartTime));
                break;
            }
            case SUBSEQUENT_PERIODS: {
                // for first pixel in current period: create a product that crosses -180° after T0 + (10 + 24) * H
                double productStartTime = T0 + (10 + 25) * H;
                product.setStartTime(new ProductData.UTC(productStartTime));
                Assert.assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, dataPeriod.getObservationMembership(EASTERN_LON, productStartTime));
                break;
            }
        }

        switch (lastPeriod) {
            case PREVIOUS_PERIODS: {
                // for last pixel in previous period: create a product that ends at latest at T0 + 10 * H
                double productEndTime = T0 - 12 * H;
                product.setEndTime(new ProductData.UTC(productEndTime));
                Assert.assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, dataPeriod.getObservationMembership(WESTERN_LON, productEndTime));
                break;
            }
            case CURRENT_PERIOD: {
                // for last pixel in current period: create a product that ends after T0 + 10 * H and before T0 + (10 + 24) * H
                double productEndTime = T0 + 15 * H;
                product.setEndTime(new ProductData.UTC(productEndTime));
                Assert.assertEquals(DataPeriod.Membership.CURRENT_PERIOD, dataPeriod.getObservationMembership(WESTERN_LON, productEndTime));
                break;
            }
            case SUBSEQUENT_PERIODS: {
                // for last pixel in current period: create a product that ends after T0 + (10 + 24) * H
                double productEndTime = T0 + (10 + 25) * H;
                product.setEndTime(new ProductData.UTC(productEndTime));
                Assert.assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, dataPeriod.getObservationMembership(WESTERN_LON, productEndTime));
                break;
            }
        }
        return product;
    }

    public static DataPeriod createSpatialDataPeriod() {
        return new SpatialDataPeriod(TestUtils.T0, 1, 10.0);
    }

    private static class MockGeoCoding extends AbstractGeoCoding {

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (pixelPos.y == 0) {
                return new GeoPos(EASTERN_LON, 0.0F);
            } else if (pixelPos.y == SCENE_RASTER_HEIGHT - 1) {
                return new GeoPos(WESTERN_LON, 0.0F);
            }
            throw new IllegalStateException("Unneeded pixel position");
        }

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            throw new IllegalStateException("Not implemented on purpose");
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            throw new IllegalStateException("Not implemented on purpose");
        }

        @Override
        public boolean canGetPixelPos() {
            throw new IllegalStateException("Not implemented on purpose");
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            throw new IllegalStateException("Not implemented on purpose");
        }

        @Override
        public Datum getDatum() {
            throw new IllegalStateException("Not implemented on purpose");
        }

        @Override
        public void dispose() {
            // nothing to do
        }
    }
}
