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
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.annotations.SourceProduct;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;

/**
 * Tests operators with {@link VirtualBand}s contained in source and target products.
 *
 * @author Norman
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class OpDealingWithVirtualBandTest extends TestCase {
    public void testUseVirtualBandInTargetProduct_SingleTile() throws IOException {
        testUseVirtualBandInTargetProduct(new UseVirtualBandInTargetProductOp_SingleTile(2, 3));
    }

    public void testUseVirtualBandInTargetProduct_TileStack() throws IOException {
        testUseVirtualBandInTargetProduct(new UseVirtualBandInTargetProductOp_TileStack(2, 3));
    }

    private void testUseVirtualBandInTargetProduct(Operator op) throws IOException {
        Product p = op.getTargetProduct();
        Band c = p.getBand("C");
        ProductData rc = c.createCompatibleRasterData();
        c.readRasterData(0, 0, 2, 3, rc, ProgressMonitor.NULL);
        assert_a_times_b_is_c(rc, 0, 0, 0);
        assert_a_times_b_is_c(rc, 1, 1, 0);
        assert_a_times_b_is_c(rc, 2, 0, 1);
        assert_a_times_b_is_c(rc, 3, 1, 1);
        assert_a_times_b_is_c(rc, 4, 0, 2);
        assert_a_times_b_is_c(rc, 5, 1, 2);

        AbcPullerOp op2 = new AbcPullerOp(op.getTargetProduct());
        Product p2 = op2.getTargetProduct();
        Band d = p2.getBand("D");
        ProductData rd = d.createCompatibleRasterData();
        d.readRasterData(0, 0, 2, 3, rd, ProgressMonitor.NULL);
        assert_a_plus_b_plus_c_is_d(rd, 0, 0, 0);
        assert_a_plus_b_plus_c_is_d(rd, 1, 1, 0);
        assert_a_plus_b_plus_c_is_d(rd, 2, 0, 1);
        assert_a_plus_b_plus_c_is_d(rd, 3, 1, 1);
        assert_a_plus_b_plus_c_is_d(rd, 4, 0, 2);
        assert_a_plus_b_plus_c_is_d(rd, 5, 1, 2);
    }

    private void assert_a_times_b_is_c(ProductData r, int i, int x, int y) {
        int a = 2 * x + 4 * y;
        int b = 3 * x + 5 * y;
        int c = a * b;
        assertEquals("i=" + i, c, r.getElemIntAt(i));
    }

    private void assert_a_plus_b_plus_c_is_d(ProductData r, int i, int x, int y) {
        int a = 2 * x + 4 * y;
        int b = 3 * x + 5 * y;
        int c = a * b;
        int d = a + b + c;
        assertEquals("i=" + i, d, r.getElemIntAt(i));
    }

    public static abstract class UseVirtualBandInTargetProductOp extends Operator {

        private final int w, h;

        protected UseVirtualBandInTargetProductOp(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("X", "Y", w, h);
            product.addBand(new Band("A", ProductData.TYPE_INT32, w, h));
            product.addBand(new Band("B", ProductData.TYPE_INT32, w, h));
            product.addBand(new VirtualBand("C", ProductData.TYPE_INT32, w, h, "A*B"));
            setTargetProduct(product);
        }
    }

    public static class UseVirtualBandInTargetProductOp_SingleTile extends UseVirtualBandInTargetProductOp {
        public UseVirtualBandInTargetProductOp_SingleTile(int w, int h) {
            super(w, h);
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            if (targetBand.getName().equals("A")) {
                for (Tile.Pos p : targetTile) {
                    int a = 2 * p.x + 4 * p.y;
                    targetTile.setSample(p.x, p.y, a);
                }
            } else if (targetBand.getName().equals("B")) {
                for (Tile.Pos p : targetTile) {
                    int b = 3 * p.x + 5 * p.y;
                    targetTile.setSample(p.x, p.y, b);
                }
            } else {
                fail(getClass().getName() + ": computeTile() illegally called for band " + targetBand.getName());
            }
        }
    }

    public static class UseVirtualBandInTargetProductOp_TileStack extends UseVirtualBandInTargetProductOp {
        public UseVirtualBandInTargetProductOp_TileStack(int w, int h) {
            super(w, h);
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
            Tile tileA = targetTiles.get(getTargetProduct().getBand("A"));
            Tile tileB = targetTiles.get(getTargetProduct().getBand("B"));
            Tile tileC = targetTiles.get(getTargetProduct().getBand("C"));

            assertNotNull(tileA);
            assertNotNull(tileB);
            assertNull(tileC);

            for (Tile.Pos p : tileA) {
                int a = 2 * p.x + 4 * p.y;
                tileA.setSample(p.x, p.y, a);
            }
            for (Tile.Pos p : tileB) {
                int b = 3 * p.x + 5 * p.y;
                tileB.setSample(p.x, p.y, b);
            }
        }
    }

    public static class AbcPullerOp extends Operator {

        @SourceProduct
        Product sourceProduct;

        public AbcPullerOp(Product sourceProduct) {
            this.sourceProduct = sourceProduct;
        }


        @Override
        public void initialize() throws OperatorException {
            int w = sourceProduct.getSceneRasterWidth();
            int h = sourceProduct.getSceneRasterHeight();
            Product product = new Product("X", "Y", w, h);
            product.addBand(new Band("D", ProductData.TYPE_INT32, w, h));
            setTargetProduct(product);
        }

        @Override
        public void computeTile(Band targetBand, Tile tileD, ProgressMonitor pm) throws OperatorException {
            Tile tileA = getSourceTile(sourceProduct.getBand("A"), tileD.getRectangle());
            Tile tileB = getSourceTile(sourceProduct.getBand("B"), tileD.getRectangle());
            Tile tileC = getSourceTile(sourceProduct.getBand("C"), tileD.getRectangle());
            for (Tile.Pos p : tileD) {
                int a = tileA.getSampleInt(p.x, p.y);
                int b = tileB.getSampleInt(p.x, p.y);
                int c = tileC.getSampleInt(p.x, p.y);
                tileD.setSample(p.x, p.y, a + b + c);
            }
        }
    }
}
