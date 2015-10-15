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

package org.esa.snap.core.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import javax.media.jai.BorderExtenderConstant;
import java.awt.Rectangle;
import java.awt.image.Raster;


public class AreaOperatorTest extends TestCase {

    public void testBasicOperatorStates() throws OperatorException {
        Product sourceProduct = new Product("foo", "grunt", 4, 4);
        Band band = sourceProduct.addBand("bar", ProductData.TYPE_FLOAT32);
        band.setRasterData(ProductData.createInstance(new float[]{
                1, 2, 3, 4,
                2, 3, 4, 5,
                3, 4, 5, 6,
                4, 5, 6, 7,
        }));

        final SomeAreaOp op = new SomeAreaOp();
        op.setSourceProduct(sourceProduct);
        op.bandName = band.getName();
        op.kernelSize = 3;

        Product targetProduct = op.getTargetProduct();
        Raster data = targetProduct.getBand(op.bandName).getSourceImage().getData();

        final float X = Float.NaN;
        float[] expectedSamples = new float[]{
                X, X, X, X,
                X, 1, 2, X,
                X, 2, 3, X,
                X, X, X, X,
        };

        for (int i = 0; i < expectedSamples.length; i++) {
            float expectedSample = expectedSamples[i];
            assertEquals(expectedSample, data.getSampleFloat(i % 4, i / 4, 0));
        }

    }

    private static class SomeAreaOp extends Operator {

        @SourceProduct
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;
        @Parameter
        String bandName;
        @Parameter(defaultValue = "3")
        int kernelSize;

        private Band sourceBand;
        private Band targetBand;

        @Override
        public void initialize() throws OperatorException {
            sourceBand = sourceProduct.getBand(bandName);
            targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            targetBand = targetProduct.addBand(bandName, sourceBand.getDataType());
        }

        @Override
        public void computeTile(Band band, Tile tile, ProgressMonitor pm) throws OperatorException {
            if (band == targetBand) {
                int border = kernelSize / 2;
                Rectangle sourceRegion = tile.getRectangle();
                sourceRegion.grow(border, border);
                Tile sourceTile = getSourceTile(sourceBand, sourceRegion, new BorderExtenderConstant(new double[]{Double.NaN}));
                for (int y = tile.getMinY(); y <= tile.getMaxY(); y++) {
                    for (int x = tile.getMinX(); x <= tile.getMaxX(); x++) {
                        tile.setSample(x, y, computeMin(sourceTile, x, y, border));
                    }
                }
            }
        }

        private double computeMin(Tile sourceTile, int x, int y, int border) {
            double min = Double.MAX_VALUE;
            for (int j = y - border; j <= y + border; j++) {
                for (int i = x - border; i <= x + border; i++) {
                    min = Math.min(min, sourceTile.getSampleDouble(i, j));
                }
            }
            return min;
        }
    }
}

