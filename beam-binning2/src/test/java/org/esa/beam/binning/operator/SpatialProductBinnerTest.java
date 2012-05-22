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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.binning.*;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.support.BinningContextImpl;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpatialProductBinnerTest {
    @Test
    public void testThatProductMustHaveAGeoCoding() throws Exception {
        BinningContext ctx = createValidCtx();

        try {
            MySpatialBinConsumer mySpatialBinProcessor = new MySpatialBinConsumer();
            SpatialProductBinner.processProduct(new Product("p", "t", 32, 256), new SpatialBinner(ctx, mySpatialBinProcessor), 1, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testProcessProduct() throws Exception {

        BinningContext ctx = createValidCtx();
        Product product = new Product("p", "t", 32, 256);
        final TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{+40f, +40f, -40f, -40f});
        final TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{-80f, +80f, -80f, +80f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        product.setPreferredTileSize(32, 16);

        MySpatialBinConsumer mySpatialBinProcessor = new MySpatialBinConsumer();
        SpatialProductBinner.processProduct(product, new SpatialBinner(ctx, mySpatialBinProcessor), 1, ProgressMonitor.NULL);
        Assert.assertEquals(32 * 256, mySpatialBinProcessor.numObs);
    }

    @Test
    public void testGetSuperSamplingSteps() {
        float[] superSamplingSteps = SpatialProductBinner.getSuperSamplingSteps(1);
        assertNotNull(superSamplingSteps);
        assertEquals(1, superSamplingSteps.length);
        assertEquals(0.5f, superSamplingSteps[0], 0.0001);

        superSamplingSteps = SpatialProductBinner.getSuperSamplingSteps(2);
        assertNotNull(superSamplingSteps);
        assertEquals(2, superSamplingSteps.length);
        assertEquals(0.25f, superSamplingSteps[0], 0.0001);
        assertEquals(0.75f, superSamplingSteps[1], 0.0001);

        superSamplingSteps = SpatialProductBinner.getSuperSamplingSteps(3);
        assertNotNull(superSamplingSteps);
        assertEquals(3, superSamplingSteps.length);
        assertEquals(1f / 6, superSamplingSteps[0], 0.0001);
        assertEquals(3f / 6, superSamplingSteps[1], 0.0001);
        assertEquals(5f / 6, superSamplingSteps[2], 0.0001);
    }


    private static BinningContext createValidCtx() {
        VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.setMaskExpr("!invalid");
        variableContext.defineVariable("invalid", "0");
        variableContext.defineVariable("a", "2.4");
        variableContext.defineVariable("b", "1.8");

        PlanetaryGrid planetaryGrid = new SEAGrid(6);
        BinManager binManager = new BinManager(variableContext,
                                               new AggregatorAverage(variableContext, "a", null, null),
                                               new AggregatorAverage(variableContext, "b", null, null));

        return new BinningContextImpl(planetaryGrid, binManager);
    }

    private static class MySpatialBinConsumer implements SpatialBinConsumer {
        int numObs;

        @Override
        public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {
            // System.out.println("spatialBins = " + Arrays.toString(spatialBins.toArray()));
            for (SpatialBin spatialBin : spatialBins) {
                Assert.assertEquals(2.4f, spatialBin.getFeatureValues()[0], 0.01f);  // mean of a
                Assert.assertEquals(1.8f, spatialBin.getFeatureValues()[2], 0.01f);  // mean of b
                numObs += spatialBin.getNumObs();
            }
        }
    }
}
