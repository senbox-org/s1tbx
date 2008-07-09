/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import java.io.IOException;
import java.util.Random;

/**
 * Tests for cluster analysis algorithms. Not useful yet.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class FindClustersOpTest extends TestCase {

    public void testFindClusters() throws IOException {
//        final int clusterCount = 4;
//
//        final Product sourceProduct = createTestProduct();
//        final FindClustersOp op = new FindClustersOp(sourceProduct, clusterCount, 10);
//
//        final Product targetProduct = op.getTargetProduct();
//        assertEquals(clusterCount, targetProduct.getNumBands());
//
//        final Band[] probabilityBands = targetProduct.getBands();
//        final double[][] probabilities = new double[clusterCount][64];
//
//        for (int i = 0; i < clusterCount; ++i) {
//            probabilityBands[i].readPixels(0, 0, 8, 8, probabilities[i]);
//        }

//        assertEquals(0.0, probabilities[0][1], 0.0);
//        assertEquals(0.0, probabilities[1][1], 0.0);
//        assertEquals(0.0, probabilities[2][1], 0.0);
//        assertEquals(1.0, probabilities[3][1], 0.0);
//
//        assertEquals(0.0, probabilities[0][5], 0.0);
//        assertEquals(0.0, probabilities[1][5], 0.0);
//        assertEquals(1.0, probabilities[2][5], 0.0);
//        assertEquals(0.0, probabilities[3][5], 0.0);
//
//        assertEquals(0.0, probabilities[0][17], 0.0);
//        assertEquals(1.0, probabilities[1][17], 0.0);
//        assertEquals(0.0, probabilities[2][17], 0.0);
//        assertEquals(0.0, probabilities[3][17], 0.0);
//
//        assertEquals(1.0, probabilities[0][21], 0.0);
//        assertEquals(0.0, probabilities[1][21], 0.0);
//        assertEquals(0.0, probabilities[2][21], 0.0);
//        assertEquals(0.0, probabilities[3][21], 0.0);
    }

    private static Product createTestProduct() {
        final Product sourceProduct = new Product("F", "FT", 8, 8);

        final double[] values = {
                4, 4, 4, 1, 1, 1, 1, 1,
                4, 4, 4, 1, 1, 1, 1, 1,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
        };
        noisify(values);
        addSourceBand(sourceProduct, "feature", values);

        return sourceProduct;
    }

    private static Band addSourceBand(Product product, String name, double[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT64);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));
        band.setImage(new RasterDataNodeOpImage(band));

        return band;
    }

    private static void noisify(double[] values) {
        final Random random = new Random(5489);

        for (int i = 0; i < values.length; i++) {
            values[i] += 0.01 * random.nextGaussian();
        }
    }
}
