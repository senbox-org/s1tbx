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

package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.FormatStrategy;
import org.esa.snap.pixex.PixExOp;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class ScatterPlotDecoratingStrategyTest {

    private static final long PRODUCT_ID_0 = 0;
    private static final long PRODUCT_ID_1 = 1;
    private ScatterPlotDecoratingStrategy strategy;
    private PixExOp.VariableCombination[] variableCombinations;

    @Before
    public void setUp() throws Exception {
        Measurement[] originalMeasurements = new Measurement[]{
                new Measurement(0, "someName", -1, -1, -1, null, null, new Object[]{6, 3.0},
                                new String[]{"original_sst", "original_tsm"}, true),
                new Measurement(1, "someOtherName", -1, -1, -1, null, null, new Object[]{8, 2.0},
                                new String[]{"original_sst", "original_tsm"}, true)
        };
        variableCombinations = new PixExOp.VariableCombination[2];
        variableCombinations[0] = new PixExOp.VariableCombination();
        variableCombinations[0].originalVariableName = "original_sst";
        variableCombinations[0].productVariableName = "product_sst";

        variableCombinations[1] = new PixExOp.VariableCombination();
        variableCombinations[1].originalVariableName = "original_tsm";
        variableCombinations[1].productVariableName = "product_tsm";

        strategy = new ScatterPlotDecoratingStrategy(originalMeasurements, new NullStrategy(), variableCombinations,
                                                     new PixExRasterNamesFactory(true, true, true, null),
                                                     new ProductRegistry() {
                                                         @Override
                                                         public long getProductId(Product product) throws IOException {
                                                             return product.getName().equals("newProduct") ? 0 : 1;
                                                         }

                                                         @Override
                                                         public void close() {
                                                         }
                                                     }, null, "test");

    }

    @Test
    public void testCreateScatterPlotsForSingleProduct() throws Exception {
        Measurement[] productMeasurements = new Measurement[]{
                new Measurement(0, "someName", PRODUCT_ID_0, -1, -1, null, null, new Object[]{7, 4.0}, true),
                new Measurement(1, "someOtherName", PRODUCT_ID_0, -1, -1, null, null, new Object[]{9, 3.0}, true)
        };

        assertTrue(strategy.plotMaps.isEmpty());

        final Product product = createProduct("newProduct");
        strategy.writeHeader(null, product);
        strategy.writeMeasurements(product, null, productMeasurements);

        assertEquals(1, strategy.plotMaps.size());
        final int scatterPlotCountForProduct = strategy.plotMaps.get(PRODUCT_ID_0).size();
        assertEquals(2, scatterPlotCountForProduct);

        JFreeChart sstPlot = strategy.plotMaps.get(PRODUCT_ID_0).get(variableCombinations[0]);
        assertEquals("original_sst", sstPlot.getXYPlot().getDomainAxis().getLabel());
        assertEquals("product_sst", sstPlot.getXYPlot().getRangeAxis().getLabel());
        assertEquals("Scatter plot of 'original_sst' and 'product_sst' for product 'newProduct'",
                     sstPlot.getTitle().getText());

        XYDataset sstDataset = sstPlot.getXYPlot().getDataset();
        assertNotNull(sstDataset);
        assertEquals(1, sstDataset.getSeriesCount());

        int seriesIndex = 0;

        assertEquals(2, sstDataset.getItemCount(0));

        assertEquals(6, sstDataset.getX(seriesIndex, 0));
        assertEquals(7, sstDataset.getY(seriesIndex, 0));

        assertEquals(8, sstDataset.getX(seriesIndex, 1));
        assertEquals(9, sstDataset.getY(seriesIndex, 1));

        JFreeChart tsmPlot = strategy.plotMaps.get(PRODUCT_ID_0).get(variableCombinations[1]);
        assertEquals("original_tsm", tsmPlot.getXYPlot().getDomainAxis().getLabel());
        assertEquals("product_tsm", tsmPlot.getXYPlot().getRangeAxis().getLabel());
        assertEquals("Scatter plot of 'original_tsm' and 'product_tsm' for product 'newProduct'",
                     tsmPlot.getTitle().getText());

        XYDataset tsmDataset = tsmPlot.getXYPlot().getDataset();
        assertNotNull(tsmDataset);
        assertEquals(1, tsmDataset.getSeriesCount());

        assertEquals(2, tsmDataset.getItemCount(0));

        assertEquals(2.0, tsmDataset.getX(seriesIndex, 0));
        assertEquals(3.0, tsmDataset.getY(seriesIndex, 0));

        assertEquals(3.0, tsmDataset.getX(seriesIndex, 1));
        assertEquals(4.0, tsmDataset.getY(seriesIndex, 1));
    }

    @Test
    public void testCreateScatterPlotsForMultipleProducts() throws Exception {
        Measurement[] productMeasurements = new Measurement[]{
                new Measurement(0, "someName", PRODUCT_ID_0, -1, -1, null, null, new Object[]{7, 4.0}, true),
                new Measurement(1, "someOtherName", PRODUCT_ID_1, -1, -1, null, null, new Object[]{9, 3.0}, true)
        };

        assertTrue(strategy.plotMaps.isEmpty());

        strategy.updateRasterNamesMaps(createProduct("newProduct"));
        strategy.writeMeasurements(createProduct("newProduct_1"), null, productMeasurements);

        assertEquals(2, strategy.plotMaps.size());
        final int scatterPlotCountForFirstProduct = strategy.plotMaps.get(PRODUCT_ID_0).size();
        assertEquals(2, scatterPlotCountForFirstProduct);

        final int scatterPlotCountForSecondProduct = strategy.plotMaps.get(PRODUCT_ID_1).size();
        assertEquals(2, scatterPlotCountForSecondProduct);
    }

    @Test
    public void testFillRasterNamesIndicesMap() {
        Product product = createProduct("newProduct");
        strategy.updateRasterNamesMaps(product);

        Map<String, Integer> rasterIndices = strategy.rasterNamesIndices.get(PRODUCT_ID_0);
        assertNotNull(rasterIndices);
        assertEquals(2, rasterIndices.size());
        assertEquals(0, (int) rasterIndices.get("product_sst"));
        assertEquals(1, (int) rasterIndices.get("product_tsm"));
    }

    private static Product createProduct(String productName) {
        Product product = new Product(productName, "type", 10, 10);
        product.addBand("product_sst", ProductData.TYPE_INT32);
        product.addBand("product_tsm", ProductData.TYPE_FLOAT32);
        return product;
    }

    private class NullStrategy implements FormatStrategy {

        @Override
        public void writeHeader(PrintWriter writer, Product product) {
        }

        @Override
        public void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements) {
        }

        @Override
        public void finish() {
        }
    }
}
