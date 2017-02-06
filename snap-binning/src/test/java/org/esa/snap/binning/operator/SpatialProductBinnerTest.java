/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.DataPeriod;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.SpatialBinConsumer;
import org.esa.snap.binning.SpatialBinner;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.support.BinningContextImpl;
import org.esa.snap.binning.support.SEAGrid;
import org.esa.snap.binning.support.VariableContextImpl;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class SpatialProductBinnerTest {

    @Test
    public void testThatProductMustHaveAGeoCoding() throws Exception {
        BinningContext ctx = createValidCtx(1, null);

        try {
            MySpatialBinConsumer mySpatialBinProcessor = new MySpatialBinConsumer();
            SpatialProductBinner.processProduct(new Product("p", "t", 32, 256),
                                                new SpatialBinner(ctx, mySpatialBinProcessor),
                                                new HashMap<Product, List<Band>>(), null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testProcessProduct() throws Exception {

        BinningContext ctx = createValidCtx(1, null);
        Product product = createProduct();

        MySpatialBinConsumer mySpatialBinProcessor = new MySpatialBinConsumer();
        SpatialProductBinner.processProduct(product, new SpatialBinner(ctx, mySpatialBinProcessor),
                                            new HashMap<Product, List<Band>>(), ProgressMonitor.NULL);
        Assert.assertEquals(32 * 256, mySpatialBinProcessor.numObs);
    }

    @Test
    public void testProcessProductWithDataPeriod() throws Exception {

        DataPeriod testDataPeriod = new DataPeriod() {
            @Override
            public Membership getObservationMembership(double lon, double time) {
                return Membership.SUBSEQUENT_PERIODS;
            }
        };
        BinningContext ctx = createValidCtx(1, testDataPeriod);
        Product product = createProduct();

        MySpatialBinConsumer mySpatialBinProcessor = new MySpatialBinConsumer();
        SpatialProductBinner.processProduct(product, new SpatialBinner(ctx, mySpatialBinProcessor),
                                            new HashMap<Product, List<Band>>(), ProgressMonitor.NULL);
        Assert.assertEquals(0, mySpatialBinProcessor.numObs); // all are pixels are rejected
    }

    @Test
    public void testProcessProductWithSuperSampling() throws Exception {
        int superSampling = 3;
        BinningContext ctx = createValidCtx(superSampling, null);
        Product product = createProduct();

        MySpatialBinConsumer mySpatialBinConsumer = new MySpatialBinConsumer();
        SpatialProductBinner.processProduct(product, new SpatialBinner(ctx, mySpatialBinConsumer),
                                            new HashMap<Product, List<Band>>(), ProgressMonitor.NULL);
        Assert.assertEquals(32 * 256 * superSampling * superSampling, mySpatialBinConsumer.numObs);
    }

    @Test
    public void testAddedBands() throws Exception {

        BinningContext ctx = createValidCtx(1, null);
        Product product = createProduct();

        HashMap<Product, List<Band>> productBandListMap = new HashMap<Product, List<Band>>();
        SpatialProductBinner.processProduct(product, new SpatialBinner(ctx, new MySpatialBinConsumer()),
                                            productBandListMap, ProgressMonitor.NULL);

        assertEquals(1, productBandListMap.size());
        List<Band> bandList = productBandListMap.get(product);
        assertNotNull(bandList);
        VariableContext variableContext = ctx.getVariableContext();
        assertEquals(variableContext.getVariableCount(), bandList.size());
        for (int i = 0; i < bandList.size(); i++) {
            assertEquals(variableContext.getVariableName(i), bandList.get(i).getName());
        }

    }

    @Test
    public void testProcessProductWithMask() throws Exception {

        BinningContext ctx = createValidCtx(1, null);
        VariableContextImpl variableContext = (VariableContextImpl) ctx.getVariableContext();
        variableContext.setMaskExpr("floor(X) % 2");
        Product product = createProduct();

        MySpatialBinConsumer mySpatialBinConsumer = new MySpatialBinConsumer();
        SpatialBinner spatialBinner = new SpatialBinner(ctx, mySpatialBinConsumer);
        long numObservations = SpatialProductBinner.processProduct(product, spatialBinner,
                                                                   new HashMap<Product, List<Band>>(),
                                                                   ProgressMonitor.NULL);
        assertEquals(32 / 2 * 256, numObservations);
        assertEquals(numObservations, mySpatialBinConsumer.numObs);
    }

    @Test
    public void testProcessProductWithMaskAndSuperSampling() throws Exception {
        int superSampling = 3;
        BinningContext ctx = createValidCtx(superSampling, null);
        VariableContextImpl variableContext = (VariableContextImpl) ctx.getVariableContext();
        variableContext.setMaskExpr("floor(X) % 2");
        Product product = createProduct();

        MySpatialBinConsumer mySpatialBinConsumer = new MySpatialBinConsumer();
        SpatialBinner spatialBinner = new SpatialBinner(ctx, mySpatialBinConsumer);
        long numObservations = SpatialProductBinner.processProduct(product, spatialBinner,
                                                                   new HashMap<Product, List<Band>>(),
                                                                   ProgressMonitor.NULL);
        assertEquals(32 / 2 * 256 * superSampling * superSampling, numObservations);
        assertEquals(numObservations, mySpatialBinConsumer.numObs);
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


    private static Product createProduct() throws Exception {
        Product product = new Product("p", "t", 32, 256);
        final TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{+40f, +40f, -40f, -40f});
        final TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{-80f, +80f, -80f, +80f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        product.setPreferredTileSize(32, 16);
        product.setStartTime(ProductData.UTC.parse("2003-01-01", "yyyy-MM-dd"));
        product.setEndTime(ProductData.UTC.parse("2003-01-02", "yyyy-MM-dd"));
        return product;
    }

    private static BinningContext createValidCtx(int superSampling, DataPeriod dataPeriod) {
        VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.setMaskExpr("!invalid");
        variableContext.defineVariable("invalid", "0");
        variableContext.defineVariable("a", "2.4");
        variableContext.defineVariable("b", "1.8");

        PlanetaryGrid planetaryGrid = new SEAGrid(6);
        BinManager binManager = new BinManager(variableContext,
                                               new AggregatorAverage(variableContext, "a", 0.0),
                                               new AggregatorAverage(variableContext, "b", 0.0));

        return new BinningContextImpl(planetaryGrid, binManager, CompositingType.BINNING, superSampling, -1, dataPeriod, null);
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
