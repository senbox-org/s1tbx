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

package org.esa.snap.watermask.operator;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 * @author Ralf Quast
 */
@SuppressWarnings({"deprecation"})
public class WatermaskOpTest {

    private Product sourceProduct;
    private Map<String,Object> parameters;

    @Before
    public void setUp() throws Exception {
        sourceProduct = new Product("dummy", "type", 1, 1);
        sourceProduct.setSceneGeoCoding(new MyGeoCoding());
        parameters = new HashMap<>();
    }

    @Test
    public void testWithSubsampling() throws Exception {
        parameters.put("subSamplingFactorX", 10);
        parameters.put("subSamplingFactorY", 10);
        Product lwProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(WatermaskOp.class), parameters, sourceProduct);
        Band band = lwProduct.getBand("land_water_fraction");
        byte sample = (byte) band.getSourceImage().getData().getSample(0, 0, 0);
        assertEquals(25, sample);
    }

    @Test
    public void testWithoutSubsampling() throws Exception {
        Product lwProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(WatermaskOp.class), parameters, sourceProduct);
        Band band = lwProduct.getBand("land_water_fraction");
        byte sample = (byte) band.getSourceImage().getData().getSample(0, 0, 0);
        assertEquals(0, sample);
    }

    private static class MyGeoCoding implements GeoCoding {

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return null;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {

            /*
                Pixel values and pixel size directly derived from GlobCover map
             */

            double pixelSize = 0.002777777777777778;
            geoPos.setLocation((float) (66.40278 + (pixelPos.y - 0.5) * pixelSize), (float) (28.063889 + (pixelPos.x - 0.5) * pixelSize));
            return geoPos;
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
